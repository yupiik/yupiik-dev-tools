const { spawn } = require('node:child_process');
const path = require('path');
const fs = require('fs');
const os = require('os');
const { app, BrowserWindow } = require('electron');
const setupJava = require('./java.setup');

let $yupiikDevTool = {};

const startServer = async () => { // todo: support a graalvm version which would be lighter?
  const javaExec = await setupJava();
  console.log(`[INFO] Using java=${javaExec}`);

  const base = path.join(__dirname, 'target');
  const sep = process.platform === 'win32' ? ';' : ':';
  const dependency = path.join(base, 'dependency');
  const libs = fs.readdirSync(dependency)
    .filter(it => it.endsWith('.jar'))
    .map(it => path.join(dependency, it))
    .join(sep);
  const args = [
    '-Dserver-tomcat-port=0',
    '-Djava.util.logging.manager=io.yupiik.logging.jul.YupiikLogManager',
    '-classpath',
    `${base}/classes${sep}${libs}`,
    'org.apache.openwebbeans.se.CDILauncher',
    '--openwebbeans.main',
    'uShipTomcatAwait'
  ];
  return new Promise((done, failed) => {
    let port = -1;
    const java = spawn(javaExec.toString(), args);
    process.on('exit', () => {
      try {
        java.kill();
      } catch (e) {
        // no-op
      }
    });
    java.stdout.on('data', (data) => {
      const string = data.toString();
      if (string.indexOf('[INFO][org.apache.webbeans.lifecycle.StandaloneLifeCycle] OpenWebBeans Container has started, it took') > 0) {
        done({ java, port });
      } else if (string.indexOf('[INFO][org.apache.coyote.http11.Http11NioProtocol] Starting ProtocolHandler ["http-nio-auto-1-') > 0) {
        const from = string.indexOf('http-nio-auto-1-');
        port = +string.substring(from + 'http-nio-auto-1-'.length, string.indexOf('"', from));
      }
      console.log(`[SERVER][stdout] ${string.trim()}`);
    });
    java.stderr.on('data', (data) => {
      failed(data);
      console.error(`[SERVER][stderr]  ${data.toString().trim()}`);
    });
  });
};

const createWindow = () => {
  if (!$yupiikDevTool.java) {
    $yupiikDevTool.java = startServer();
  }

  const defaultOptions = {
    width: 800,
    height: 600,
    autoHideMenuBar: true,
  };

  $yupiikDevTool.java
    .then(({ port }) => {
      // inject the backend url with a preload script which will define a window variable "yupiikDevToolPreloadConfig", see useJsonrpc.js
      const preload = path.join(os.tmpdir(), 'yupiik-dev-tools.preload.js');
      fs.writeFileSync(preload, `require('electron').contextBridge.exposeInMainWorld('yupiikDevToolPreloadConfig', { base: "http://localhost:${port}/jsonrpc" });`);
      $yupiikDevTool.preload = preload;

      const win = new BrowserWindow({
        ...defaultOptions,
        webPreferences: {
          preload,
        },
      });

      // to debug frontend enable next line:
      // win.webContents.openDevTools();

      return win.loadFile('dist/index.html');
    })
    .catch(() => new BrowserWindow(defaultOptions).loadFile('dist/index.html'));
};

app.whenReady().then(async () => {
  await createWindow();

  app.on('activate', async () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      await createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    if ($yupiikDevTool) {
      if ($yupiikDevTool.java) {
        $yupiikDevTool.java
          .then(p => {
            try {
              p.java.kill();
            } catch (e) {
              // no-op
            }
          });
      }
      if ($yupiikDevTool.preload && fs.existsSync($yupiikDevTool.preload)) {
        fs.rmSync($yupiikDevTool.preload);
      }
    }
    app.quit();
  }
});
