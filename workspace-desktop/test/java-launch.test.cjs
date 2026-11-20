
'use strict';

const assert = require('node:assert/strict');
const {test} = require('node:test');
const {javaExecutable, gameJvmFlags} = require('../java-launch.cjs');

test('Java launch preserves platform paths and executable names', () => {
  assert.equal(javaExecutable({JAVA_HOME:'C:\\Program Files\\Temurin'}, 'win32'),
    'C:\\Program Files\\Temurin\\bin\\java.exe');
  assert.equal(javaExecutable({JAVA_HOME:'/Library/Java/Temurin 21/Contents/Home'}, 'darwin'),
    '/Library/Java/Temurin 21/Contents/Home/bin/java');
  assert.equal(javaExecutable({JAVA_HOME:'/opt/jdk-21'}, 'linux'), '/opt/jdk-21/bin/java');
  assert.equal(javaExecutable({}, 'win32'), 'java.exe');
  assert.equal(javaExecutable({}, 'darwin'), 'java');
  assert.equal(javaExecutable({}, 'linux'), 'java');
});

test('only the macOS game JVM needs the Cocoa first-thread flag', () => {
  assert.deepEqual(gameJvmFlags('darwin'), ['-XstartOnFirstThread']);
  assert.deepEqual(gameJvmFlags('linux'), []);
  assert.deepEqual(gameJvmFlags('win32'), []);
});
