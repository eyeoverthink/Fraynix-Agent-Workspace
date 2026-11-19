package fraymus.workspace;

import org.json.JSONObject;

/** Shared approval and handoff ordering for registered pairs. Keep existing journal keys readable. */
enum ReviewedChain {
    RAMP("rampTraceChain", "rampTraceChainApproved", "ownedRampMission", "ownedWorkflowId"),
    RELAY("relayClearanceChain", "relayClearanceChainApproved", "ownedGameTask115", "relayDashboard");

    static final String HANDOFF = "chainHandoff";
    final String key, approval, upstream, downstream;

    ReviewedChain(String key, String approval, String upstream, String downstream) {
        this.key = key;
        this.approval = approval;
        this.upstream = upstream;
        this.downstream = downstream;
    }

    static ReviewedChain of(JSONObject task) {
        if (task.has(RAMP.key) && task.has(RELAY.key)) throw new IllegalStateException("A task must have one reviewed chain");
        if (task.has(RAMP.key)) return RAMP;
        return task.has(RELAY.key) ? RELAY : null;
    }

    static void verifyJournal(JSONObject task) {
        var type = of(task);
        if (type == null) return;
        if (!task.has(ChainRecommendation.KEY)) {
            if (type == RAMP) RampTraceChain.verifyPlan(task);
            else RelayClearanceChain.verifyPlan(task);
        }
        var plan = task.getJSONObject(type.key);
        int approvals = 0;
        JSONObject witnessed = null;
        String dashboardReceipt = null;
        for (Object raw : task.getJSONArray("history")) {
            var event = (JSONObject) raw;
            switch (event.getString("kind")) {
                case "chain-confirmed" -> {
                    if (!plan.similar(event.getJSONObject("detail"))) throw new IllegalStateException("The chain approval changed");
                    approvals++;
                }
                case "chain-handoff-verified" -> {
                    if (approvals != 1 || witnessed != null) throw new IllegalStateException("The verified handoff is out of order");
                    witnessed = event.getJSONObject("detail");
                }
                case "surface-action" -> {
                    var detail = event.getJSONObject("detail");
                    if (type == RELAY && RelayClearanceChain.CHECK.equals(detail.optString("tool"))) {
                        if (witnessed == null || dashboardReceipt != null)
                            throw new IllegalStateException("The dashboard check is out of order");
                        dashboardReceipt = detail.getString("resultSha256");
                    }
                }
                default -> { }
            }
        }
        var context = task.getJSONObject("context044");
        var handoff = context.optJSONObject(HANDOFF);
        if (approvals != (task.optBoolean(type.approval) ? 1 : 0)
                || (handoff == null ? witnessed != null : !handoff.similar(witnessed))
                || (context.has(type.upstream) && approvals != 1)
                || (context.has(type.downstream) && handoff == null)) {
            throw new IllegalStateException("The saved chain approval or handoff changed");
        }
        if (type == RAMP && context.has(type.downstream)
                && !context.getString(type.downstream).equals(RampTraceChain.workflowId(task.getString("id")))) {
            throw new IllegalStateException("The saved summary identity changed");
        }
        if (type == RELAY) {
            var receipt = context.optJSONObject(RelayClearanceChain.RECEIPT);
            if (receipt == null ? dashboardReceipt != null:
                    !context.has(type.downstream) || !WorldFrames.hash(receipt).equals(dashboardReceipt)) {
                throw new IllegalStateException("The saved dashboard receipt changed");
            }
        }
    }
}
