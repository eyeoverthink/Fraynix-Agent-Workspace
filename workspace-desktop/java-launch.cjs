
'use strict';

const path = require('node:path');

function javaExecutable(env = process.env, platform = process.platform) {
  const name = platform === 'win32' ? 'java.exe' : 'java';
  const paths = platform === 'win32' ? path.win32 : path.posix;
  return env.JAVA_HOME ? paths.join(env.JAVA_HOME, 'bin', name) : name;
}

function gameJvmFlags(platform = process.platform) {
  return platform === 'darwin' ? ['-XstartOnFirstThread'] : [];
}

module.exports = {javaExecutable, gameJvmFlags};
