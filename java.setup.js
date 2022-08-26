const https = require('https');
const path = require('path');
const fs = require('fs');
const os = require('os');
const unzipper = require('unzipper');

const findPlatform = () => {
    switch (os.platform) {
        case 'win32':
            return 'win';
        case 'darwin':
            return 'macosx';
        default:
            return 'linux';
    }
};

const unzip = (zip, target) => {
    console.log(`[INFO] Unzipping '${zip}' to '${target}'`);
    return new Promise((resolve, reject) => {
        try {
            fs.createReadStream(zip)
                .pipe(unzipper.Extract({ path: target }))
                .on('error', e => reject(e))
                .on('close', () => resolve(target));
        } catch (e) {
            console.error(e);
            reject(e);
        }
    });
};

const findExistingJava = (target, bin) => {
    const java = path.join(target, bin);
    if (fs.existsSync(java)) {
        console.log(`[INFO] java found at '${java}'`);
        return java;
    }

    // check if there is one more level (unzip of the distro, we don't denormalize it)
    const children = fs.readdirSync(target, { withFileTypes: true })
        .filter(it => it.isDirectory() && it.name.startsWith('zulu'))
        .map(it => it.name);
    if (children.length > 0) {
        const java = path.join(target, children[0], bin);
        if (fs.existsSync(java)) {
            console.log(`[INFO] java found at '${java}'`);
            return java;
        }
    }

    console.error(`[ERROR] No java found in ${target}`);
    return undefined;
}

const findOrInstallJava = async () => {
    const bin = `bin/java${process.platform === 'win32' ? '.exe' : ''}`;

    const javaHome = process.env.JAVA_HOME;
    if (javaHome) { // if contextually ok just use it
        const release = path.join(javaHome, 'release');
        if (fs.existsSync(release)) {
            const content = fs.readFileSync(release, 'utf-8');
            const start = content.indexOf('JAVA_VERSION="');
            if (start > 0) {
                const end = content.indexOf('"', start + 'JAVA_VERSION="'.length + 1);
                if (end > 0) {
                    const version = content.substring(start + 'JAVA_VERSION="'.length, end).trim().split('\.');
                    if (+version[0] >= 17) {
                        const java = path.join(javaHome, bin);
                        if (fs.existsSync(java)) {
                            return java;
                        }
                    }
                }
            }
        }
    }

    const target = process.env.YUPIIK_DEV_TOOLS_JAVA_INSTALLATION_DIRECTORY || path.join(__dirname, 'java_distribution');
    if (fs.existsSync(target)) {
        const java = findExistingJava(target, bin);
        if (java) {
            return java;
        }

        fs.rmSync(target, { recursive: true, force: true });
    }


    const download = `https://cdn.azul.com/zulu/bin/zulu17.36.13-ca-jdk17.0.4-${findPlatform()}_x64.zip`;
    console.log(`[INFO] No JAVA_HOME matching java 17 requirement, downloading '${download}' to '${target}'`);
    const zip = `${target}.zip`;
    await new Promise((resolve, reject) => https.get(download, response => {
        const fileStream = fs.createWriteStream(zip);
        response.pipe(fileStream);
        fileStream.on('finish', resolve);
    }).on('error', err => {
        fs.unlink(zip);
        reject(err.message);
    }));

    await unzip(zip, target);
    const exec = findExistingJava(target, bin);

    console.log(`[INFO] setting permissions to java`);
    fs.chmodSync(exec, 0770);

    return exec;
};

module.exports = findOrInstallJava;
