const formatName = (prefix, name) => {
    let out = '';
    let up = true;
    for (let i = 0; i < name.length; i += 1) {
        const c = name.charAt(i);
        if (up) {
            out += c.toUpperCase();
            up = false;
        } else if (c === '-') {
            up = true;
            out += ' ';
        } else {
            out += c;
        }
    }
    return out.substring(0, 1).toUpperCase() + out.substring(1);
};

const createMenuItems = methods => {
    const byGroup = methods
        .filter(it => it.tags && it.tags.length > 0) // root_label and command_prefix summary values in current setup
        .map(method => ({
            method,
            $metadata: method.tags.reduce((a, i) => ({ ...a, [i.summary]: i.name }), {}),
        }))
        .reduce((agg, item) => ({
            ...agg,
            [item.$metadata.root_label]: [
                ...(agg[item.$metadata.root_label] || []),
                item,
            ],
        }), {});

    return [
        {
            key: 'home',
            label: 'Yupiik Dev Tools',
        },
        ...Object
            .keys(byGroup)
            .sort()
            .map(label => ({
                key: label,
                label,
                icon: undefined, // TBD
                children: byGroup[label].map(item => ({
                    key: item.method.name,
                    label: formatName(item.$metadata.command_prefix.length, item.method.name),
                })),
            })),
    ];
}

export default data => {
    const { methods, components: { schemas } } = data || { methods: [], components: { schemas: {} } };

    return {
        menuItems: createMenuItems(methods),
        indexedMethods: methods.reduce((a, i) => ({ ...a, [i.name]: i }), {}),
        indexedSchemas: Object
            .keys(schemas)
            .filter(name => !name.startsWith('io_yupiik_uship_jsonrpc_core_openrpc_') &&
                name !== 'io_yupiik_uship_backbone_johnzon_jsonschema_Schema' &&
                !name.startsWith('io-yupiik-tools-dev-jsonrpc-internal'))
            .reduce((a, name) => ({ ...a, [name]: schemas[name] }), {}),
    };
};
