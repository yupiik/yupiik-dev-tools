import Form from '@rjsf/antd';
import * as antd from 'antd';
import get from 'lodash/get';
import React, { createElement, useEffect, useMemo, useState } from 'react';
import { useJsonRpc } from '../../hooks/useJsonRpc';
import css from './Command.module.scss';
import { parametersToJsonSchema, parametersToUiSchema } from './openrpc-jsonschema.service';

const { Alert, Skeleton } = antd;

const doEval = (spec, data) => {
    if (spec.$eval) {
        return get(data, spec.$eval);
    }
    return spec;
};

const renderCustomUi = (spec, data) => {
    if (typeof spec === 'string') {
        return spec;
    }
    if (spec.type && spec.props) {
        const component = spec.type.startsWith('antd.') ? antd[spec.type.substring('antd.'.length)] : spec.type;
        return createElement(
            component,
            !spec.props ? undefined : Object.keys(spec.props)
                .reduce((a, i) => ({ ...a, [i]: doEval(spec.props[i], data) }), {}),
            !spec.children ? undefined : spec.children
                .map(it => doEval(it, { ...data, $item: it }))
                .map(it => renderCustomUi(it, { ...data, $item: it })));
    }
    return (<ObjectRenderer data={spec} />);
};

const ObjectRenderer = ({ data }) => (
    <div>
        <pre>
            <code>{typeof data === 'string' ? data : JSON.stringify(data, null, 2)}</code>
        </pre>
    </div>
);

const JsonResult = ({
    request,
}) => {
    const [loading, error, data] = useJsonRpc(request);
    if (loading) {
        return (
            <Skeleton />
        );
    }
    if (error) {
        return (
            <Alert
                type='error'
                message={error.message || JSON.stringify(error)}
            />
        )
    }

    if (data && data.ui && data.data) {
        return renderCustomUi(data.ui, data.data);
    }
    return (
        <ObjectRenderer data={data} />
    )
};

const Result = ({
    submitted,
    method,
    params,
}) => {
    if (!submitted) {
        return (
            <div>Click on <em>Execute</em> to see the result.</div>
        );
    }
    return (
        <JsonResult
            request={{
                jsonrpc: '2.0',
                method: method.name,
                params,
            }}
        />
    );
};

// NOTE: as of today, the strictmode detects the Form uses UNSAFE_componentWillReceiveProps
//       and triggers "Warning: Using UNSAFE_componentWillReceiveProps in strict mode is not recommended and may indicate bugs in your code."
//       -> https://github.com/rjsf-team/react-jsonschema-form/issues/1794
export default ({
    method,
    schemas,
}) => {
    const [formData, setFormData] = useState(null);
    const [submitted, setSubmitted] = useState(false);
    const { jsonSchema, uiSchema } = useMemo(() => ({
        jsonSchema: parametersToJsonSchema({ method, schemas }),
        uiSchema: parametersToUiSchema({ method }),
    }), [method, schemas]);

    useEffect(() => {
        setFormData(undefined);
        setSubmitted(false);
    }, [method]);

    return (
        <div className={css.command}>
            <div>
                <Form
                    schema={jsonSchema}
                    uiSchema={uiSchema}
                    formData={formData}
                    onChange={e => {
                        setSubmitted(false);
                        setFormData(e.formData);
                    }}
                    onSubmit={() => setSubmitted(true)}
                >
                    <button type="submit">Execute</button>
                </Form>
            </div>

            <div className={css.result}>
                <Result
                    submitted={submitted}
                    method={method}
                    params={formData}
                />
            </div>
        </div>
    );
};
