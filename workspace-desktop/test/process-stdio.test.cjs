'use strict';

const assert = require('node:assert/strict');
const {Writable} = require('node:stream');
const {test} = require('node:test');
const {guardClosedConsole} = require('../process-stdio.cjs');

const failure = code => Object.assign(new Error(code),{code});

test('logging to a closed pipe finishes without an unhandled error', async () => {
    const stream = new Writable({write(_, encoding, done) { done(failure('EPIPE')); }});
    guardClosedConsole(stream);
    const closed = new Promise(resolve => stream.once('close',resolve));
    stream.write('closing');
    await closed;
    assert(stream.destroyed);
});

test('other output errors retain their original identity', () => {
    const stream = new Writable({write(_, encoding, done) { done(); }});
    guardClosedConsole(stream);
    const error = failure('EIO');
    assert.throws(() => stream.emit('error',error),received=>received===error);
});
