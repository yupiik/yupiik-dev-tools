const { start } = require('live-server')
const { build, analyzeMetafile } = require('esbuild')
const ScssModulesPlugin = require('esbuild-scss-modules-plugin').ScssModulesPlugin;
const fs = require('fs-extra')
const JsonRpcServer = require('./src/mock/jsonrpc').JsonRpcServer;

const isDev = process.env.NODE_ENV !== 'production'
const output = process.env.BUILDER_OUTPUT || 'dist'
const metafile = process.env.BUILDER_METAFILE === 'true'
const verboseMetafile = process.env.BUILDER_METAFILE_VERBOSE === 'true'
const analyzeJson = process.env.BUILDER_METAFILE_JSON === 'true'

const serverParams = { // https://www.npmjs.com/package/live-server#usage-from-node
    port: 8181,
    root: 'dist',
    open: true,
    middleware: [JsonRpcServer],
};

const buildParams = {
    color: true,
    entryPoints: [process.env.BUILDER_ENTRYPOINT || 'src/index.jsx'],
    loader: { '.js': 'jsx' },
    outdir: output,
    minify: !isDev,
    format: 'cjs',
    platform: 'browser',
    bundle: true,
    sourcemap: true,
    logLevel: 'info',
    incremental: true,
    watch: isDev,
    legalComments: process.env.BUILDER_ENTRYPOINT_LEGAL_COMMENT,
    metafile,
    plugins: [
        ScssModulesPlugin({
            inject: true,
            minify: true,
        }),
    ]
};

!(async () => {
    fs.removeSync(output);
    if (output === 'dist') {
        fs.copySync('public', 'dist');
    }

    const result = await build(buildParams);

    if (analyzeJson) { // BUILDER_METAFILE_JSON=true BUILDER_METAFILE=true NODE_ENV=production node builder.js > /tmp/esbuild.json
        console.log(JSON.stringify(result.metafile));
    } else if (metafile) { // BUILDER_METAFILE=true NODE_ENV=production node builder.js
        console.log(await analyzeMetafile(result.metafile, { verbose: verboseMetafile }));
    }

    if (isDev) {
        start(serverParams);
    } else {
        process.exit(0);
    }
})();
