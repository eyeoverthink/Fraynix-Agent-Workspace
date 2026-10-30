'use strict';
function bodyReport(move,release,capture){
 if(!move||move.status!=='completed'||!Number.isFinite(move.targetErrorM)||release?.status!=='completed'||release.after?.grip?.held!==false||move.runId!==release.runId||capture?.packet?.world?.runId!==release.runId||capture.packet.world.tick!==release.endTick)throw Error('Native body report needs matching completed motion, release and final frame receipts');
 return 'Body move '+move.outcome+'. Measured target error '+move.targetErrorM.toPrecision(6)+' m (tolerance '+move.toleranceM+' m). Release position ['+release.after.position.map(n=>n.toFixed(6)).join(', ')+'] m. Native constraint removed. Run '+release.runId+'. Frame SHA-256 '+capture.pngSha256+'.';
}
module.exports={bodyReport};
