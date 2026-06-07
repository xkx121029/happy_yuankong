const fs = require('fs-extra');
const path = require('path');

const electronPath = require('electron');

async function buildPortable() {
    const distDir = path.join(__dirname, 'dist');
    const portableDir = path.join(__dirname, 'dist-portable');
    const electronRoot = path.dirname(electronPath);

    console.log('Cleaning portable directory...');
    await fs.remove(portableDir);
    await fs.ensureDir(portableDir);

    console.log('Copying Electron...');
    await fs.copy(electronRoot, portableDir, {
        filter: (src) => {
            const name = path.basename(src);
            return name !== 'locales' && !name.endsWith('.pdb');
        }
    });

    console.log('Copying app resources...');
    const appDir = path.join(portableDir, 'resources', 'app');
    await fs.ensureDir(appDir);
    await fs.copy(distDir, appDir);

    console.log('Creating package.json...');
    const packageJson = {
        name: 'p2p-remote',
        version: '1.0.0',
        main: 'index.html'
    };
    await fs.writeJson(path.join(appDir, 'package.json'), packageJson);

    console.log('Portable build completed!');
    console.log(`Output: ${portableDir}`);
}

buildPortable().catch(console.error);
