// One thinking loop for the Brain.
// A goal is compiled with the operator table the mind currently holds, run on FrayVM,
// and compared with an independent evaluator. A mismatch rewrites that table.
// The rewrite is kept only when the goal, fresh probes, and every earlier case agree.
// Bind uses the live FlyGNN slice objects. A collision retires the placement rule.

import { assemble, runProgram } from "./frayvm.js";
import { FlyGNNv10, metaCompileFrayVmSource } from "./flyGnnConnectome.js";

const OPCODES = ["ADD", "SUB", "MUL", "DIV"];

export function createMind() {
  return {
    version: 1,
    opcodes: { "+": "ADD", "-": "ADD", "*": "ADD", "/": "ADD" },
    placement: "overwrite-zero",
    fossils: [],
    cases: [],
    facts: {},
    binds: [],
    activeProgram: ""
  };
}

export function normalizeMind(saved) {
  const fresh = createMind();
  if (!saved || typeof saved !== "object") return fresh;
  const opcodes = { ...fresh.opcodes };
  for (const op of ["+", "-", "*", "/"]) {
    if (OPCODES.includes(saved.opcodes?.[op])) opcodes[op] = saved.opcodes[op];
  }
  return {
    ...fresh,
    opcodes,
    placement: saved.placement === "next-free" ? "next-free" : "overwrite-zero",
    fossils: Array.isArray(saved.fossils) ? saved.fossils.slice(0, 12) : [],
    cases: Array.isArray(saved.cases) ? saved.cases.slice(0, 32) : [],
    facts: saved.facts && typeof saved.facts === "object" ? { ...saved.facts } : {},
    binds: Array.isArray(saved.binds) ? saved.binds.slice(0, 40) : [],
    activeProgram: typeof saved.activeProgram === "string" ? saved.activeProgram : ""
  };
}

function cloneMind(mind) {
  return normalizeMind(mind);
}

function runText(text) {
  const source = String(text || "").trim();
  const program = assemble(source.includes("HALT") ? source : `${source}\nHALT`);
  const ran = runProgram(program);
  return { value: ran.top, steps: ran.steps, source: source.includes("HALT") ? source : `${source}\nHALT` };
}

function tokenize(input) {
  const source = String(input || "").replace(/\s+/g, "");
  if (!source) return null;
  const tokens = [];
  for (let i = 0; i < source.length; i += 1) {
    const char = source[i];
    if (char >= "0" && char <= "9") {
      let j = i + 1;
      while (j < source.length && source[j] >= "0" && source[j] <= "9") j += 1;
      tokens.push({ kind: "num", n: Number(source.slice(i, j)) });
      i = j - 1;
      continue;
    }
    if ("+-*/()".includes(char)) {
      tokens.push({ kind: "sym", v: char });
      continue;
    }
    return null;
  }
  return tokens;
}

function parseExpr(input) {
  const tokens = tokenize(input);
  if (!tokens) return null;
  let at = 0;
  const peek = () => tokens[at];
  const take = () => tokens[at++];

  function primary() {
    const token = peek();
    if (!token) return null;
    if (token.kind === "num") {
      take();
      return { t: "num", n: token.n };
    }
    if (token.kind === "sym" && token.v === "(") {
      take();
      const inner = expr();
      if (!inner || peek()?.v !== ")") return null;
      take();
      return inner;
    }
    return null;
  }

  function unary() {
    if (peek()?.kind === "sym" && peek().v === "-") {
      take();
      const inner = unary();
      if (!inner) return null;
      return { t: "neg", x: inner };
    }
    return primary();
  }

  function term() {
    let left = unary();
    if (!left) return null;
    while (peek()?.kind === "sym" && (peek().v === "*" || peek().v === "/")) {
      const op = take().v;
      const right = unary();
      if (!right) return null;
      left = { t: "bin", op, l: left, r: right };
    }
    return left;
  }

  function expr() {
    let left = term();
    if (!left) return null;
    while (peek()?.kind === "sym" && (peek().v === "+" || peek().v === "-")) {
      const op = take().v;
      const right = term();
      if (!right) return null;
      left = { t: "bin", op, l: left, r: right };
    }
    return left;
  }

  const ast = expr();
  if (!ast || at !== tokens.length) return null;
  return ast;
}

function evalAst(ast) {
  if (!ast) return null;
  if (ast.t === "num") return ast.n | 0;
  if (ast.t === "neg") {
    const inner = evalAst(ast.x);
    return inner === null ? null : (-inner) | 0;
  }
  const left = evalAst(ast.l);
  const right = evalAst(ast.r);
  if (left === null || right === null) return null;
  if (ast.op === "+") return (left + right) | 0;
  if (ast.op === "-") return (left - right) | 0;
  if (ast.op === "*") return Math.imul(left, right);
  if (ast.op === "/") return right === 0 ? null : (left / right) | 0;
  return null;
}

function emit(ast, opcodes) {
  if (ast.t === "num") return [`PUSH_IMM ${ast.n}`];
  if (ast.t === "neg") {
    const inner = emit(ast.x, opcodes);
    return inner ? [...inner, "NEG"] : null;
  }
  const code = opcodes[ast.op];
  if (!code) return null;
  const left = emit(ast.l, opcodes);
  const right = emit(ast.r, opcodes);
  if (!left || !right) return null;
  return [...left, ...right, code];
}

function operatorsIn(ast, found = new Set()) {
  if (!ast) return found;
  if (ast.t === "bin") {
    found.add(ast.op);
    operatorsIn(ast.l, found);
    operatorsIn(ast.r, found);
  } else if (ast.t === "neg") operatorsIn(ast.x, found);
  return found;
}

function numberCount(ast) {
  if (!ast) return 0;
  if (ast.t === "num") return 1;
  if (ast.t === "neg") return numberCount(ast.x);
  return numberCount(ast.l) + numberCount(ast.r);
}

function withNumbers(ast, nums) {
  let cursor = 0;
  const walk = (node) => {
    if (node.t === "num") return { ...node, n: nums[cursor++] };
    if (node.t === "neg") return { ...node, x: walk(node.x) };
    return { ...node, l: walk(node.l), r: walk(node.r) };
  };
  return walk(ast);
}

const PROBE_ROWS = [
  [2, 3, 5, 8, 9, 4],
  [4, 1, 6, 7, 2, 3],
  [9, 4, 3, 8, 1, 5],
  [8, 5, 2, 6, 7, 9]
];

function agrees(opcodes, ast, expected) {
  const lines = emit(ast, opcodes);
  if (!lines || expected === null) return false;
  try {
    return runText(lines.join("\n")).value === expected;
  } catch {
    return false;
  }
}

function holdsForProbes(opcodes, ast) {
  const count = numberCount(ast);
  if (!count) return false;
  let checked = 0;
  for (const row of PROBE_ROWS) {
    const probe = withNumbers(ast, row.slice(0, count));
    const expected = evalAst(probe);
    if (expected === null) continue;
    if (!agrees(opcodes, probe, expected)) return false;
    checked += 1;
  }
  return checked >= 3;
}

function holdsForCases(opcodes, cases) {
  for (const item of cases) {
    const ast = parseExpr(item.expr);
    if (!ast || !agrees(opcodes, ast, item.expected)) return false;
  }
  return true;
}

function reviseOpcodes(opcodes, ast, expected, cases) {
  const ops = [...operatorsIn(ast)];
  if (!ops.length || ops.length > 3) return null;
  let best = null;
  const assign = (index, table) => {
    if (index === ops.length) {
      if (!agrees(table, ast, expected)) return;
      if (!holdsForProbes(table, ast)) return;
      if (!holdsForCases(table, cases)) return;
      const distance = ops.reduce((sum, op) => sum + (table[op] === opcodes[op] ? 0 : 1), 0);
      if (!best || distance < best.distance) best = { opcodes: table, distance };
      return;
    }
    for (const code of OPCODES) assign(index + 1, { ...table, [ops[index]]: code });
  };
  assign(0, { ...opcodes });
  return best;
}

function compress(source, expected) {
  let folded = source;
  try {
    folded = metaCompileFrayVmSource(source, "SAFE_FULL");
  } catch {
    return { source, kept: false };
  }
  try {
    if (runText(folded).value === expected) return { source: folded, kept: true };
  } catch {
    /* The shorter program is discarded when it no longer matches. */
  }
  return { source, kept: false };
}

function finish(mind, verdict, steps, extra = {}) {
  mind.journal = undefined;
  return { mind, verdict, steps, ...extra };
}

function remember(mind, goal) {
  const match = goal.match(/^remember\s+([A-Za-z][A-Za-z0-9_]*)\s*=\s*(\S(?:.*\S)?)\s*$/i);
  if (!match) return null;
  const key = match[1].toLowerCase();
  const value = match[2];
  const next = cloneMind(mind);
  next.facts[key] = value;
  const read = next.facts[key];
  const steps = [
    { phase: "store", detail: `Wrote fact ${key}.` },
    { phase: "read", detail: `Read back ${key} = ${read}.` }
  ];
  if (read !== value) {
    steps.push({ phase: "reject", detail: "The fact did not read back." });
    return finish(mind, "FAIL", steps);
  }
  steps.push({ phase: "commit", detail: "The fact is in the mind." });
  return finish(next, "PASS", steps, { got: read, expected: value });
}

function recall(mind, goal) {
  const match = goal.match(/^recall\s+([A-Za-z][A-Za-z0-9_]*)\s*$/i);
  if (!match) return null;
  const key = match[1].toLowerCase();
  const value = mind.facts?.[key];
  if (value === undefined) {
    return finish(cloneMind(mind), "FAIL", [
      { phase: "read", detail: `No fact named ${key}.` },
      { phase: "reject", detail: "A missing fact is not a pass." }
    ]);
  }
  return finish(cloneMind(mind), "PASS", [
    { phase: "read", detail: `${key} = ${value}.` }
  ], { got: value, expected: value });
}

function placeBinds(policy, binds) {
  const tiles = new Map();
  const tile = (id) => {
    if (!tiles.has(id)) tiles.set(id, new FlyGNNv10(42 + id * 137, 1000));
    return tiles.get(id);
  };
  const assigned = [];
  for (const bind of binds) {
    let slot = { tile: 0, slice: 0 };
    if (policy === "next-free") {
      const used = new Set(assigned.map((item) => `${item.tile}:${item.slice}`));
      let tileId = 0;
      let slice = 0;
      while (used.has(`${tileId}:${slice}`)) {
        slice += 1;
        if (slice > 13) {
          slice = 0;
          tileId += 1;
          if (tileId > 6) return null;
        }
      }
      slot = { tile: tileId, slice };
    }
    tile(slot.tile).trainConceptSlice(slot.slice, bind.name, bind.value, true);
    assigned.push({ name: bind.name, value: bind.value, tile: slot.tile, slice: slot.slice });
  }
  for (const item of assigned) {
    const read = tile(item.tile).readConceptSlice(item.slice, true);
    if (read.boundConceptId !== item.name || read.boundTargetVal !== item.value) return null;
  }
  return assigned;
}

function bindConcept(mind, goal) {
  const match = goal.match(/^bind\s+([A-Za-z][A-Za-z0-9_]*)\s+(-?\d+)\s*$/i);
  if (!match) return null;
  const name = match[1].toLowerCase();
  const value = Number(match[2]);
  const prior = (mind.binds || []).filter((item) => item.name !== name);
  const requested = [...prior.map((item) => ({ name: item.name, value: item.value })), { name, value }];
  const steps = [{ phase: "bind", detail: `Place ${name} = ${value} with rule ${mind.placement}.` }];
  let placed = placeBinds(mind.placement, requested);
  let placement = mind.placement;
  if (!placed) {
    steps.push({ phase: "miss", detail: "Read-back collided with a concept already stored." });
    if (placement !== "next-free") {
      const rescued = placeBinds("next-free", requested);
      if (rescued) {
        placement = "next-free";
        placed = rescued;
        steps.push({ phase: "rewrite", detail: "Retired overwrite-zero. Each concept now gets the next free slice." });
      }
    }
  }
  if (!placed) {
    steps.push({ phase: "reject", detail: "No placement kept every concept readable." });
    return finish(cloneMind(mind), "FAIL", steps);
  }
  const next = cloneMind(mind);
  if (placement !== mind.placement) {
    next.fossils = [...next.fossils, { kind: "placement", before: mind.placement, because: goal }].slice(-12);
  }
  next.placement = placement;
  next.binds = placed;
  const self = placed.find((item) => item.name === name);
  steps.push({ phase: "commit", detail: `${name} is on tile ${self.tile}, slice ${self.slice}. ${placed.length} concepts still read back.` });
  return finish(next, "PASS", steps, { got: value, expected: value });
}

export function think(mind, goalText) {
  const goal = String(goalText || "").trim();
  const state = mind?.opcodes ? normalizeMind(mind) : createMind();
  if (!goal) {
    return finish(state, "UNVERIFIED", [{ phase: "unverified", detail: "Enter a goal." }]);
  }
  const remembered = remember(state, goal);
  if (remembered) return remembered;
  const recalled = recall(state, goal);
  if (recalled) return recalled;
  const bound = bindConcept(state, goal);
  if (bound) return bound;

  const ast = parseExpr(goal);
  if (!ast) {
    return finish(state, "UNVERIFIED", [{
      phase: "unverified",
      detail: "No executable check for that sentence. Use an integer expression, remember name = value, recall name, or bind name value."
    }]);
  }
  const expected = evalAst(ast);
  const steps = [{ phase: "check", detail: `Independent value is ${expected}.` }];
  if (expected === null) {
    steps.push({ phase: "reject", detail: "The expression divides by zero." });
    return finish(state, "FAIL", steps);
  }

  const first = emit(ast, state.opcodes);
  let ran = null;
  try {
    ran = runText(first.join("\n"));
  } catch (error) {
    steps.push({ phase: "reject", detail: error.message });
    return finish(state, "FAIL", steps);
  }
  steps.push({ phase: "run", detail: `Current operator table returned ${ran.value}.`, source: ran.source });

  let opcodes = state.opcodes;
  let source = ran.source;
  if (ran.value !== expected) {
    steps.push({ phase: "miss", detail: `${ran.value} does not equal ${expected}. The operator table is wrong.` });
    const revised = reviseOpcodes(opcodes, ast, expected, state.cases);
    if (!revised) {
      steps.push({ phase: "reject", detail: "No rewrite matched this goal, the probes, and the earlier cases." });
      return finish(state, "FAIL", steps, { got: ran.value, expected });
    }
    opcodes = revised.opcodes;
    const rewritten = emit(ast, opcodes);
    const again = runText(rewritten.join("\n"));
    source = again.source;
    steps.push({
      phase: "rewrite",
      detail: `Operator table changed (${revised.distance} opcode${revised.distance === 1 ? "" : "s"}). FrayVM now returns ${again.value}.`,
      source
    });
    if (again.value !== expected) {
      steps.push({ phase: "reject", detail: "The rewrite did not survive a second run." });
      return finish(state, "FAIL", steps, { got: again.value, expected });
    }
  }

  if (!holdsForCases(opcodes, state.cases)) {
    steps.push({ phase: "reject", detail: "The table broke an earlier goal. It was not stored." });
    return finish(state, "FAIL", steps, { got: ran.value, expected });
  }
  if (state.cases.length) steps.push({ phase: "replay", detail: `Replayed ${state.cases.length} earlier goal${state.cases.length === 1 ? "" : "s"}.` });

  const folded = compress(source, expected);
  if (folded.kept && folded.source !== source) {
    source = folded.source;
    steps.push({ phase: "compress", detail: "The meta-compiler shortened the program and the value still matched." });
  }

  const next = cloneMind(state);
  if (JSON.stringify(opcodes) !== JSON.stringify(state.opcodes)) {
    next.fossils = [...next.fossils, { kind: "opcodes", before: state.opcodes, because: goal }].slice(-12);
  }
  next.opcodes = opcodes;
  next.activeProgram = source;
  next.cases = [...next.cases, { expr: goal.replace(/\s+/g, ""), expected }].slice(-32);
  steps.push({ phase: "commit", detail: "Stored. The next goal runs on this table." });
  return finish(next, "PASS", steps, { got: expected, expected, source });
}
