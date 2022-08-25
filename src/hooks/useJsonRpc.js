import { useEffect, useState } from 'react';

export const DEFAULT_JSONRPC_HEADERS = {
    'accept': 'application/json;charset=utf-8',
    'content-type': 'application/json;charset=utf-8',
    'x-app-source': 'yupiik-dev-tools',
};

const { base } = window.yupiikDevToolPreloadConfig || { base: '/jsonrpc' };

export const useJsonRpc = (
    payload,
    path = base,
    headers = DEFAULT_JSONRPC_HEADERS,
    unwrap = true
) => {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState();
    const [data, setData] = useState();

    useEffect(() => {
        async function doLoad(signal) {
            setLoading(true);
            try {
                const response = await fetch(path, {
                    method: 'POST',
                    headers,
                    body: JSON.stringify(payload),
                    signal,
                });

                if (response.status !== 200) {
                    setError({
                        message: `Invalid HTTP status: ${response.status}`,
                    });
                    return true;
                }

                const json = await response.json();
                if (!unwrap) {
                    setData(json);
                    setError(undefined);
                    return true;
                }

                if (Array.isArray(json)) {
                    const errors = json.filter(it => it.error).map(it => it.error);
                    if (errors.length > 0) {
                        setError({
                            errors,
                            message: errors.map(e => e.message || e.code).join(',\n'),
                        });
                    } else {
                        setData(json.map(it => it.result));
                        setError(undefined);
                    }
                }

                if (json.error) {
                    setError(json.error);
                } else {
                    setData(json.result);
                    setError(undefined);
                }
            } catch (e) {
                setError({
                    message: e.message || JSON.stringify(e),
                });
            } finally {
                setLoading(false);
            }
            return true;
        };

        const aborter = new AbortController();
        let done = false;
        doLoad(aborter.signal)
            // portable finally
            .then(() => { done = true })
            .catch(() => { done = true });
        return () => {
            if (!done) {
                aborter.abort();
            }
        }
    }, [
        payload,
        path,
        headers,
        unwrap,
    ]);

    return [loading, error, data];
};
