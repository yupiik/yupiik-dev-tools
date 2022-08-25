const openrpc = require('./openrpc.json');

const MOCKS = {
    'base64-encode': JSON.stringify({
        jsonrpc: '2.0',
        result: 'the base64 result',
    }),
    'base64-decode': JSON.stringify({
        jsonrpc: '2.0',
        error: {
            code: 0,
            message: 'Invalid base64',
        },
    }),
    openrpc: JSON.stringify(openrpc),
    error: JSON.stringify({
        jsonrpc: '2.0',
        error: {
            code: 0,
            message: 'unknown JSON-RPC method',
        },
    }),
};

exports.JsonRpcServer = function JsonRpcServer(req, res, next) {
    if (req.url !== '/jsonrpc' || req.method !== 'POST') {
        next();
        return;
    }

    req.setEncoding('utf8');

    let rawBody = '';
    req.on('data', (chunk) => { rawBody += chunk });
    req.on('end', () => {
        const json = JSON.parse(rawBody);
        res.statusCode = 200;
        res.setHeader('content-type', 'application/json');

        const response = MOCKS[json.method];
        if (response && typeof response === 'function') {
            res.end(response(json.params || {}));
        } else if (response) {
            res.end(response);
        } else {
            res.end(MOCKS.error);
        }
    });
};
