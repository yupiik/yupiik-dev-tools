import Form from '@rjsf/antd';
import { Alert, Skeleton } from 'antd';
import React, { useEffect, useMemo, useState } from 'react';
import { useJsonRpc } from '../../hooks/useJsonRpc';
import css from './Command.module.scss';
import { parametersToJsonSchema, parametersToUiSchema } from './openrpc-jsonschema.service';

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
    return (
        <div>
            <pre>
                <code>{JSON.stringify(data, null, 2)}</code>
            </pre>
        </div>
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
