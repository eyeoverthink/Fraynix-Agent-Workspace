'use strict';

function guardClosedConsole(...streams) {
    for (const stream of streams) stream.on('error', error => {
        // A Windows launcher or test harness may go away while Electron is still closing.
        if (error.code !== 'EPIPE') throw error;
    });
}

module.exports = {guardClosedConsole};
