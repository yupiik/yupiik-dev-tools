export const parametersToJsonSchema = ({
    method,
    // schemas,
}) => {
    const noParam = !method.params;
    return {
        title: method.name,
        description: method.description, // todo: enable adoc/md?
        type: 'object',
        properties: noParam ?
            {} :
            method.params.reduce((agg, param) => ({
                ...agg,
                [param.name]: {
                    description: param.description,
                    ...param.schema, // todo: resolve thanks schemas if needed
                },
            }), {}),
        required: noParam ? [] :
            method.params
                .filter(it => it.required)
                .map(it => it.name),
    };
};

export const parametersToUiSchema = ({
    method,
}) => ({
    'ui:order': !method.params ?
        undefined :
        method.params.map(it => it.name), // keep definition order
    ...(!method.params ?
        {} :
        method.params
            .filter(param => param.ui && param.ui.widget)
            .reduce((a, i) => ({
                ...a,
                [i.name]: {
                    'ui:widget': i.ui.widget,
                },
            }), {})),
});
