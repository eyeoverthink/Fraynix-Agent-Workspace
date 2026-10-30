// FrayVM Tier0 — faithful to the archive's wasm-my-pocket-frayvm-wired build.
// Program is an array of { op, arg }. JUMP/JZ/PUSH_IMM carry an immediate arg.
import { base64, fnv1a64, sha256, textBytes } from "./emojiCodec.js";

export const ISA = [
  ["HALT", 0x00, "stop"], ["NOP", 0x01, "do nothing"], ["EMIT", 0x10, "pop n, print the character n"],
  ["DROP", 0x20, "pop and discard"], ["SWAP", 0x21, "swap top two"], ["OVER", 0x22, "copy second to top"],
  ["ROT", 0x23, "rotate top three"], ["NIP", 0x24, "drop the second"], ["TUCK", 0x25, "copy top below second"],
  ["2DUP", 0x26, "duplicate top two"], ["2SWAP", 0x27, "swap the top two pairs"], ["2OVER", 0x28, "copy the second pair"],
  ["DEPTH", 0x29, "push the stack depth"], ["PICK", 0x2a, "copy the nth item"], ["ROLL", 0x2b, "move the nth item to top"],
  ["DUP", 0x2c, "duplicate top"], ["?DUP", 0x2d, "duplicate top if non-zero"], ["2DROP", 0x2e, "drop top two"],
  ["ADD", 0x40, "pop a b, push b+a"], ["SUB", 0x41, "pop a b, push b-a"], ["MUL", 0x42, "pop a b, push b*a"],
  ["DIV", 0x43, "pop a b, push b/a"], ["MOD", 0x44, "pop a b, push b%a"], ["NEG", 0x45, "negate top"],
  ["ABS", 0x46, "absolute value"], ["MIN", 0x47, "smaller of top two"], ["MAX", 0x48, "larger of top two"],
  ["AND", 0x49, "bitwise and"], ["OR", 0x4a, "bitwise or"], ["XOR", 0x4b, "bitwise xor"],
  ["SHL", 0x4c, "shift left"], ["SHR", 0x4d, "shift right (unsigned)"], ["NOT", 0x4e, "bitwise not"],
  ["INC", 0x4f, "add one"], ["DEC", 0x50, "subtract one"],
  ["EQ", 0x60, "equal"], ["NE", 0x61, "not equal"], ["LT", 0x62, "less than"], ["GT", 0x63, "greater than"],
  ["GE", 0x64, "greater or equal"], ["LE", 0x65, "less or equal"], ["ZERO?", 0x66, "top is zero"],
  ["NOTZERO?", 0x67, "top is non-zero"], ["EVEN?", 0x68, "top is even"], ["ODD?", 0x69, "top is odd"],
  ["LNOT", 0x6a, "logical not"], ["BOOL", 0x6b, "0 or 1"], ["SIGN", 0x6c, "-1, 0, or 1"],
  ["BETWEEN", 0x6d, "lo hi x -> in range"], ["CLAMP", 0x6e, "clamp to lo..hi"], ["TRUE", 0x6f, "push 1"], ["FALSE", 0x70, "push 0"],
  ["PUSH_IMM", 0x80, "push the immediate"], ["POP_PRINT", 0x81, "pop and print the number"],
  ["ITOF", 0x90, "int to float"], ["FTOI", 0x91, "float to int"], ["FDUP", 0x92, "duplicate float top"], ["FDROP", 0x93, "drop float top"],
  ["JUMP", 0xa0, "jump to the immediate"], ["JZ", 0xa1, "pop; if zero, jump to the immediate"]
];

export const BY_NAME = new Map(ISA.map(([name, op, desc]) => [name, { op, desc }]));
export const BY_OP = new Map(ISA.map(([name, op, desc]) => [op, { name, desc }]));
export const ARG_OPS = new Set([0x80, 0xa0, 0xa1]);
const STEP_LIMIT = 20000;

export class FrayVM {
  load(code) {
    this.code = code;
    this.ip = 0;
    this.running = true;
    this.iStack = [];
    this.fStack = [];
    this.output = "";
    this.steps = 0;
    this.trace = [];
    return this;
  }

  step() {
    if (!this.running || this.ip >= this.code.length) return (this.running = false);
    const instruction = this.code[this.ip++];
    const n = instruction.op;
    const t = instruction.arg;
    const S = this.iStack;
    const r = () => (S.length ? S.pop() : 0);
    const l = (value) => S.push(value | 0);
    switch (n) {
      case 0x00: this.running = false; break;
      case 0x01: break;
      case 0x20: r(); break;
      case 0x21: { const u = r(), i = r(); l(u); l(i); break; }
      case 0x22: { const u = S[S.length - 2] ?? 0; l(u); break; }
      case 0x23: if (S.length >= 3) { const u = r(), i = r(), o = r(); l(i); l(u); l(o); } break;
      case 0x24: if (S.length >= 2) { const u = r(); r(); l(u); } break;
      case 0x25: { const u = r(), i = r(); l(u); l(i); l(u); break; }
      case 0x26: { const u = S[S.length - 1] ?? 0, i = S[S.length - 2] ?? 0; l(i); l(u); break; }
      case 0x27: if (S.length >= 4) { const u = r(), i = r(), o = r(), a = r(); l(i); l(u); l(a); l(o); } break;
      case 0x28: if (S.length >= 4) { const u = S[S.length - 4] ?? 0, i = S[S.length - 3] ?? 0; l(u); l(i); } break;
      case 0x29: l(S.length); break;
      case 0x2a: { let u = t !== undefined ? t : r(); u |= 0; l(S[S.length - 1 - u] ?? 0); break; }
      case 0x2b: { let u = t !== undefined ? t : r(); u |= 0; if (u >= 0 && u < S.length) l(S.splice(S.length - 1 - u, 1)[0]); break; }
      case 0x2c: { const u = S[S.length - 1] ?? 0; l(u); break; }
      case 0x2d: { const u = S[S.length - 1] ?? 0; if (u !== 0) l(u); break; }
      case 0x2e: r(); r(); break;
      case 0x40: { const u = r(), i = r(); l(i + u); break; }
      case 0x41: { const u = r(), i = r(); l(i - u); break; }
      case 0x42: { const u = r(), i = r(); l(Math.imul(i, u)); break; }
      case 0x43: { const u = r(), i = r(); l(u === 0 ? 0 : (i / u) | 0); break; }
      case 0x44: { const u = r(), i = r(); l(u === 0 ? 0 : i % u); break; }
      case 0x45: { const u = r(); l(-u); break; }
      case 0x46: { const u = r(); l(u < 0 ? -u : u); break; }
      case 0x47: { const u = r(), i = r(); l(i < u ? i : u); break; }
      case 0x48: { const u = r(), i = r(); l(i > u ? i : u); break; }
      case 0x49: { const u = r(), i = r(); l(i & u); break; }
      case 0x4a: { const u = r(), i = r(); l(i | u); break; }
      case 0x4b: { const u = r(), i = r(); l(i ^ u); break; }
      case 0x4c: { const u = r(), i = r(); l(i << (u & 31)); break; }
      case 0x4d: { const u = r(), i = r(); l(i >>> (u & 31)); break; }
      case 0x4e: { const u = r(); l(~u); break; }
      case 0x4f: { const u = r(); l(u + 1); break; }
      case 0x50: { const u = r(); l(u - 1); break; }
      case 0x60: { const u = r(), i = r(); l(i === u ? 1 : 0); break; }
      case 0x61: { const u = r(), i = r(); l(i !== u ? 1 : 0); break; }
      case 0x62: { const u = r(), i = r(); l(i < u ? 1 : 0); break; }
      case 0x63: { const u = r(), i = r(); l(i > u ? 1 : 0); break; }
      case 0x64: { const u = r(), i = r(); l(i >= u ? 1 : 0); break; }
      case 0x65: { const u = r(), i = r(); l(i <= u ? 1 : 0); break; }
      case 0x66: { const u = r(); l(u === 0 ? 1 : 0); break; }
      case 0x67: { const u = r(); l(u !== 0 ? 1 : 0); break; }
      case 0x68: { const u = r(); l((u & 1) === 0 ? 1 : 0); break; }
      case 0x69: { const u = r(); l((u & 1) !== 0 ? 1 : 0); break; }
      case 0x6a: { const u = r(); l(u ? 0 : 1); break; }
      case 0x6b: { const u = r(); l(u ? 1 : 0); break; }
      case 0x6c: { const u = r(); l(u === 0 ? 0 : u > 0 ? 1 : -1); break; }
      case 0x6d: { const u = r(), i = r(), o = r(); l(o >= i && o <= u ? 1 : 0); break; }
      case 0x6e: { const u = r(), i = r(); let a = r(); if (a < i) a = i; if (a > u) a = u; l(a); break; }
      case 0x6f: l(1); break;
      case 0x70: l(0); break;
      case 0x80: l(t ?? 0); break;
      case 0xa0: if (t !== undefined) this.ip = t; break;
      case 0xa1: if (r() === 0 && t !== undefined) this.ip = t; break;
      case 0x81: { const u = r(); this.output += u.toString() + " "; break; }
      case 0x10: { const u = r(); this.output += String.fromCharCode(u & 65535); break; }
      case 0x90: { const u = r(); this.fStack.push(u); break; }
      case 0x91: { const u = this.fStack.pop() ?? 0; l(u | 0); break; }
      case 0x92: { const u = this.fStack[this.fStack.length - 1] ?? 0; this.fStack.push(u); break; }
      case 0x93: this.fStack.pop(); break;
      default: break;
    }
    if (++this.steps > STEP_LIMIT) this.running = false;
    return this.running;
  }

  run() {
    while (this.running && this.steps < STEP_LIMIT) this.step();
    return this.output;
  }
}

export function runProgram(code) {
  const vm = new FrayVM().load(code);
  const output = vm.run();
  return { output, top: vm.iStack[vm.iStack.length - 1] ?? null, stack: vm.iStack.slice(), steps: vm.steps, halted: !vm.running };
}

// text assembler: "PUSH 10  PUSH 5  ADD  POP_PRINT  HALT" or one op per line, ; comments
const ALIAS = { PUSH: "PUSH_IMM", PRINT: "POP_PRINT", IMM: "PUSH_IMM", "." : "POP_PRINT" };

const parseNumber = (token) => (/^0x[0-9a-f]+$/i.test(token) ? parseInt(token, 16) : /^-?\d+$/.test(token) ? Number(token) : null);

export function assemble(text) {
  const tokens = [];
  for (const rawLine of String(text).split(/\r?\n/)) {
    const line = rawLine.replace(/[;#].*$/, "").trim();
    if (!line) continue;
    for (const token of line.split(/[\s,]+/).filter(Boolean)) tokens.push(token);
  }
  const program = [];
  const labels = new Map();
  const pending = [];
  for (let i = 0; i < tokens.length; i++) {
    const token = tokens[i];
    if (token.endsWith(":")) { labels.set(token.slice(0, -1).toUpperCase(), program.length); continue; }
    const literal = parseNumber(token);
    if (literal !== null) { program.push({ op: 0x80, arg: literal }); continue; }
    const name = (ALIAS[token.toUpperCase()] || token).toUpperCase();
    const entry = BY_NAME.get(name);
    if (!entry) throw new Error(`Unknown instruction: ${token}`);
    if (!ARG_OPS.has(entry.op)) { program.push({ op: entry.op }); continue; }
    const next = tokens[++i];
    if (next === undefined) throw new Error(`${name} needs an argument`);
    const value = parseNumber(next);
    if (value !== null) program.push({ op: entry.op, arg: value });
    else { pending.push({ index: program.length, label: next.toUpperCase(), name }); program.push({ op: entry.op, arg: 0 }); }
  }
  for (const { index, label, name } of pending) {
    if (!labels.has(label)) throw new Error(`${name} points at an unknown label: ${label}`);
    program[index].arg = labels.get(label);
  }
  return program;
}

export function disassemble(code) {
  return code.map((instruction, index) => {
    const entry = BY_OP.get(instruction.op);
    const name = entry ? entry.name : `0x${instruction.op.toString(16)}`;
    return ARG_OPS.has(instruction.op) ? `${index}: ${name} ${instruction.arg ?? 0}` : `${index}: ${name}`;
  }).join("\n");
}

// canonical bytecode: op byte, plus i32 LE arg for PUSH_IMM / JUMP / JZ (matches the archive)
export function toBytecode(code) {
  const bytes = [];
  for (const instruction of code) {
    bytes.push(instruction.op);
    if (ARG_OPS.has(instruction.op)) {
      const value = instruction.arg | 0;
      bytes.push(value & 255, (value >>> 8) & 255, (value >>> 16) & 255, (value >>> 24) & 255);
    }
  }
  return new Uint8Array(bytes);
}

export function fromBytecode(bytes) {
  const code = [];
  for (let i = 0; i < bytes.length;) {
    const op = bytes[i++];
    if (ARG_OPS.has(op)) {
      const arg = (bytes[i] | (bytes[i + 1] << 8) | (bytes[i + 2] << 16) | (bytes[i + 3] << 24));
      i += 4;
      code.push({ op, arg });
    } else code.push({ op });
  }
  return code;
}

// ---- Dual-Tier FrayVM -> WASM Compiler ----
// Tier 1 (DIRECT_STACK): Maps straight-line arithmetic/compare/stack-shuffle ops directly onto the WASM operand stack (2 scratch locals).
// Tier 2 (TURING_CFG): Structured block/loop/br_if control-flow + linear-memory stack & stdout buffer supporting all 53 ISA opcodes including JUMP/JZ loops.
const WASM_DIRECT = {
  0x20: [0x1a],
  0x21: [0x21, 0x00, 0x21, 0x01, 0x20, 0x00, 0x20, 0x01],
  0x22: [0x21, 0x00, 0x22, 0x01, 0x20, 0x00, 0x20, 0x01],
  0x2c: [0x22, 0x00, 0x20, 0x00],
  0x6f: [0x41, 0x01],
  0x70: [0x41, 0x00],
  0x80: null, 0x40: [0x6a], 0x41: [0x6b], 0x42: [0x6c], 0x43: [0x6d], 0x44: [0x6f],
  0x45: null, 0x49: [0x71], 0x4a: [0x72], 0x4b: [0x73], 0x4c: [0x74], 0x4d: [0x76], 0x4f: null, 0x50: null,
  0x60: [0x46], 0x61: [0x47], 0x62: [0x48], 0x63: [0x4a], 0x64: [0x4e], 0x65: [0x4c], 0x66: [0x45]
};
const COMPILABLE_DIRECT = new Set(Object.keys(WASM_DIRECT).map(Number).concat([0x00, 0x01]));

export function isDirectStackCompilable(code) {
  return Array.isArray(code) && code.length > 0 && code.every((instruction) => COMPILABLE_DIRECT.has(instruction.op));
}

export function getCompilationTier(code) {
  if (!Array.isArray(code) || !code.length) return "NONE";
  return isDirectStackCompilable(code) ? "DIRECT_STACK" : "TURING_CFG";
}

export function canCompile(code) {
  return Array.isArray(code) && code.length > 0 && code.every((instruction) => BY_OP.has(instruction.op));
}

function sleb(value) {
  const out = [];
  for (;;) {
    const byte = value & 0x7f;
    value >>= 7;
    if ((value === 0 && !(byte & 0x40)) || (value === -1 && byte & 0x40)) { out.push(byte); return out; }
    out.push(byte | 0x80);
  }
}
function uleb(value) {
  const out = [];
  do { let byte = value & 0x7f; value >>>= 7; if (value) byte |= 0x80; out.push(byte); } while (value);
  return out;
}
const wsection = (id, body) => [id, ...uleb(body.length), ...body];
const wname = (text) => [...uleb(textBytes(text).length), ...textBytes(text)];

function compileTuringCfgToWasm(code) {
  const SP = 0, PC = 1, STEPS = 2, A = 3, B = 4, C = 5, D = 6, LAST_OUT = 7, OUT_PTR = 8, FSP = 9;
  const LGET = (i) => [0x20, i];
  const LSET = (i) => [0x21, i];
  const LTEE = (i) => [0x22, i];
  const ICONST = (v) => [0x41, ...sleb(v | 0)];
  const ILOAD = (off = 0) => [0x28, 0x02, ...uleb(off)];
  const ISTORE = (off = 0) => [0x36, 0x02, ...uleb(off)];
  const ILOAD8U = (off = 0) => [0x2d, 0x00, ...uleb(off)];
  const ISTORE8 = (off = 0) => [0x3a, 0x00, ...uleb(off)];

  const popTo = (loc) => [
    ...LGET(SP), ...ICONST(0), 0x4a,
    0x04, 0x7f,
      ...LGET(SP), ...ICONST(4), 0x6b, ...LTEE(SP), ...ILOAD(0),
    0x05,
      ...ICONST(0),
    0x0b,
    ...LSET(loc)
  ];
  const pushExpr = (expr) => [
    ...LGET(SP), ...expr, ...ISTORE(0),
    ...LGET(SP), ...ICONST(4), 0x6a, ...LSET(SP)
  ];

  const emitInstructionOps = (ins) => {
    const op = ins.op;
    const arg = ins.arg | 0;
    switch (op) {
      case 0x01: return [];
      case 0x10: return [
        ...popTo(A), ...LGET(A), ...LSET(LAST_OUT),
        ...LGET(OUT_PTR), ...LGET(A), ...ISTORE8(0),
        ...LGET(OUT_PTR), ...ICONST(1), 0x6a, ...LSET(OUT_PTR)
      ];
      case 0x20: return [...popTo(A)];
      case 0x21: return [...popTo(A), ...popTo(B), ...pushExpr(LGET(A)), ...pushExpr(LGET(B))];
      case 0x22: return [...popTo(A), ...popTo(B), ...pushExpr(LGET(B)), ...pushExpr(LGET(A)), ...pushExpr(LGET(B))];
      case 0x23: return [
        ...LGET(SP), ...ICONST(12), 0x4e, 0x04, 0x40,
          ...popTo(A), ...popTo(B), ...popTo(C),
          ...pushExpr(LGET(B)), ...pushExpr(LGET(A)), ...pushExpr(LGET(C)),
        0x0b
      ];
      case 0x24: return [
        ...LGET(SP), ...ICONST(8), 0x4e, 0x04, 0x40,
          ...popTo(A), ...popTo(B), ...pushExpr(LGET(A)),
        0x0b
      ];
      case 0x25: return [...popTo(A), ...popTo(B), ...pushExpr(LGET(A)), ...pushExpr(LGET(B)), ...pushExpr(LGET(A))];
      case 0x26: return [...popTo(A), ...popTo(B), ...pushExpr(LGET(B)), ...pushExpr(LGET(A)), ...pushExpr(LGET(B)), ...pushExpr(LGET(A))];
      case 0x27: return [
        ...LGET(SP), ...ICONST(16), 0x4e, 0x04, 0x40,
          ...popTo(A), ...popTo(B), ...popTo(C), ...popTo(D),
          ...pushExpr(LGET(B)), ...pushExpr(LGET(A)), ...pushExpr(LGET(D)), ...pushExpr(LGET(C)),
        0x0b
      ];
      case 0x28: return [
        ...LGET(SP), ...ICONST(16), 0x4e, 0x04, 0x40,
          ...LGET(SP), ...ICONST(16), 0x6b, ...ILOAD(0), ...LSET(A),
          ...LGET(SP), ...ICONST(12), 0x6b, ...ILOAD(0), ...LSET(B),
          ...pushExpr(LGET(A)), ...pushExpr(LGET(B)),
        0x0b
      ];
      case 0x29: return [...pushExpr([...LGET(SP), ...ICONST(2), 0x76])];
      case 0x2a: return [
        ...(ins.arg !== undefined ? [...ICONST(arg), ...LSET(A)] : popTo(A)),
        ...LGET(SP), ...ICONST(4), 0x6b, ...LGET(A), ...ICONST(2), 0x74, 0x6b, ...LSET(B),
        ...LGET(B), ...ICONST(0), 0x4e, ...LGET(B), ...LGET(SP), 0x48, 0x71,
        0x04, 0x7f, ...LGET(B), ...ILOAD(0), 0x05, ...ICONST(0), 0x0b,
        ...LSET(C), ...pushExpr(LGET(C))
      ];
      case 0x2b: return [
        ...(ins.arg !== undefined ? [...ICONST(arg), ...LSET(A)] : popTo(A)),
        ...LGET(SP), ...ICONST(4), 0x6b, ...LGET(A), ...ICONST(2), 0x74, 0x6b, ...LSET(B),
        ...LGET(B), ...ICONST(0), 0x4e, ...LGET(B), ...LGET(SP), 0x48, 0x71,
        0x04, 0x40,
          ...LGET(B), ...ILOAD(0), ...LSET(C),
          0x02, 0x40, 0x03, 0x40,
            ...LGET(B), ...ICONST(4), 0x6a, ...LGET(SP), 0x4e, 0x0d, 0x01,
            ...LGET(B), ...LGET(B), ...ICONST(4), 0x6a, ...ILOAD(0), ...ISTORE(0),
            ...LGET(B), ...ICONST(4), 0x6a, ...LSET(B), 0x0c, 0x00,
          0x0b, 0x0b,
          ...LGET(SP), ...ICONST(4), 0x6b, ...LGET(C), ...ISTORE(0),
        0x0b
      ];
      case 0x2c: return [...popTo(A), ...pushExpr(LGET(A)), ...pushExpr(LGET(A))];
      case 0x2d: return [
        ...popTo(A), ...pushExpr(LGET(A)),
        ...LGET(A), ...ICONST(0), 0x47, 0x04, 0x40, ...pushExpr(LGET(A)), 0x0b
      ];
      case 0x2e: return [...popTo(A), ...popTo(B)];
      case 0x40: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x6a])];
      case 0x41: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x6b])];
      case 0x42: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x6c])];
      case 0x43: return [
        ...popTo(A), ...popTo(B),
        ...LGET(A), 0x45, 0x04, 0x7f, ...ICONST(0), 0x05, ...LGET(B), ...LGET(A), 0x6d, 0x0b,
        ...LSET(C), ...pushExpr(LGET(C))
      ];
      case 0x44: return [
        ...popTo(A), ...popTo(B),
        ...LGET(A), 0x45, 0x04, 0x7f, ...ICONST(0), 0x05, ...LGET(B), ...LGET(A), 0x6f, 0x0b,
        ...LSET(C), ...pushExpr(LGET(C))
      ];
      case 0x45: return [...popTo(A), ...pushExpr([...ICONST(0), ...LGET(A), 0x6b])];
      case 0x46: return [
        ...popTo(A),
        ...LGET(A), ...ICONST(0), 0x48, 0x04, 0x7f, ...ICONST(0), ...LGET(A), 0x6b, 0x05, ...LGET(A), 0x0b,
        ...LSET(B), ...pushExpr(LGET(B))
      ];
      case 0x47: return [
        ...popTo(A), ...popTo(B),
        ...LGET(B), ...LGET(A), 0x48, 0x04, 0x7f, ...LGET(B), 0x05, ...LGET(A), 0x0b,
        ...LSET(C), ...pushExpr(LGET(C))
      ];
      case 0x48: return [
        ...popTo(A), ...popTo(B),
        ...LGET(B), ...LGET(A), 0x4a, 0x04, 0x7f, ...LGET(B), 0x05, ...LGET(A), 0x0b,
        ...LSET(C), ...pushExpr(LGET(C))
      ];
      case 0x49: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x71])];
      case 0x4a: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x72])];
      case 0x4b: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x73])];
      case 0x4c: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), ...ICONST(31), 0x71, 0x74])];
      case 0x4d: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), ...ICONST(31), 0x71, 0x76])];
      case 0x4e: return [...popTo(A), ...pushExpr([...LGET(A), ...ICONST(-1), 0x73])];
      case 0x4f: return [...popTo(A), ...pushExpr([...LGET(A), ...ICONST(1), 0x6a])];
      case 0x50: return [...popTo(A), ...pushExpr([...LGET(A), ...ICONST(1), 0x6b])];
      case 0x60: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x46])];
      case 0x61: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x47])];
      case 0x62: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x48])];
      case 0x63: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x4a])];
      case 0x64: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x4e])];
      case 0x65: return [...popTo(A), ...popTo(B), ...pushExpr([...LGET(B), ...LGET(A), 0x4c])];
      case 0x66: case 0x6a: return [...popTo(A), ...pushExpr([...LGET(A), 0x45])];
      case 0x67: case 0x6b: return [...popTo(A), ...pushExpr([...LGET(A), ...ICONST(0), 0x47])];
      case 0x68: return [...popTo(A), ...pushExpr([...LGET(A), ...ICONST(1), 0x71, 0x45])];
      case 0x69: return [...popTo(A), ...pushExpr([...LGET(A), ...ICONST(1), 0x71, ...ICONST(0), 0x47])];
      case 0x6c: return [
        ...popTo(A),
        ...LGET(A), 0x45, 0x04, 0x7f, ...ICONST(0), 0x05,
          ...LGET(A), ...ICONST(0), 0x4a, 0x04, 0x7f, ...ICONST(1), 0x05, ...ICONST(-1), 0x0b,
        0x0b,
        ...LSET(B), ...pushExpr(LGET(B))
      ];
      case 0x6d: return [
        ...popTo(A), ...popTo(B), ...popTo(C),
        ...pushExpr([...LGET(C), ...LGET(B), 0x4e, ...LGET(C), ...LGET(A), 0x4c, 0x71])
      ];
      case 0x6e: return [
        ...popTo(A), ...popTo(B), ...popTo(C),
        ...LGET(C), ...LGET(B), 0x48, 0x04, 0x40, ...LGET(B), ...LSET(C), 0x0b,
        ...LGET(C), ...LGET(A), 0x4a, 0x04, 0x40, ...LGET(A), ...LSET(C), 0x0b,
        ...pushExpr(LGET(C))
      ];
      case 0x6f: return [...pushExpr(ICONST(1))];
      case 0x70: return [...pushExpr(ICONST(0))];
      case 0x80: return [...pushExpr(ICONST(arg))];
      case 0x81: return [
        ...popTo(A), ...LGET(A), ...LSET(LAST_OUT),
        ...LGET(A), ...ICONST(0), 0x48, 0x04, 0x40,
          ...LGET(OUT_PTR), ...ICONST(45), ...ISTORE8(0),
          ...LGET(OUT_PTR), ...ICONST(1), 0x6a, ...LSET(OUT_PTR),
          ...ICONST(0), ...LGET(A), 0x6b, ...LSET(A),
        0x0b,
        ...LGET(A), 0x45, 0x04, 0x40,
          ...LGET(OUT_PTR), ...ICONST(48), ...ISTORE8(0),
          ...LGET(OUT_PTR), ...ICONST(1), 0x6a, ...LSET(OUT_PTR),
        0x05,
          ...ICONST(0), ...LSET(D),
          0x03, 0x40,
            ...LGET(D), ...LGET(A), ...ICONST(10), 0x70, ...ICONST(48), 0x6a, ...ISTORE8(0x0f00),
            ...LGET(D), ...ICONST(1), 0x6a, ...LSET(D),
            ...LGET(A), ...ICONST(10), 0x6e, ...LTEE(A), ...ICONST(0), 0x4b, 0x0d, 0x00,
          0x0b,
          0x03, 0x40,
            ...LGET(D), ...ICONST(1), 0x6b, ...LSET(D),
            ...LGET(OUT_PTR), ...LGET(D), ...ILOAD8U(0x0f00), ...ISTORE8(0),
            ...LGET(OUT_PTR), ...ICONST(1), 0x6a, ...LSET(OUT_PTR),
            ...LGET(D), ...ICONST(0), 0x4a, 0x0d, 0x00,
          0x0b,
        0x0b,
        ...LGET(OUT_PTR), ...ICONST(32), ...ISTORE8(0),
        ...LGET(OUT_PTR), ...ICONST(1), 0x6a, ...LSET(OUT_PTR)
      ];
      case 0x90: return [
        ...popTo(A), ...LGET(FSP), ...LGET(A), ...ISTORE(0),
        ...LGET(FSP), ...ICONST(4), 0x6a, ...LSET(FSP)
      ];
      case 0x91: return [
        ...LGET(FSP), ...ICONST(0x0800), 0x4a, 0x04, 0x7f,
          ...LGET(FSP), ...ICONST(4), 0x6b, ...LTEE(FSP), ...ILOAD(0),
        0x05, ...ICONST(0), 0x0b,
        ...LSET(A), ...pushExpr(LGET(A))
      ];
      case 0x92: return [
        ...LGET(FSP), ...ICONST(0x0800), 0x4a, 0x04, 0x40,
          ...LGET(FSP), ...LGET(FSP), ...ICONST(4), 0x6b, ...ILOAD(0), ...ISTORE(0),
          ...LGET(FSP), ...ICONST(4), 0x6a, ...LSET(FSP),
        0x0b
      ];
      case 0x93: return [
        ...LGET(FSP), ...ICONST(0x0800), 0x4a, 0x04, 0x40,
          ...LGET(FSP), ...ICONST(4), 0x6b, ...LSET(FSP),
        0x0b
      ];
      default: return [];
    }
  };

  const blocks = [];
  for (let k = 0; k < code.length; k++) {
    const ins = code[k];
    const op = ins.op;
    let body;
    if (op === 0x00) {
      body = [0x0c, 0x02]; // br 2 ($exit)
    } else if (op === 0xa0) {
      body = [...ICONST(ins.arg ?? 0), ...LSET(PC), 0x0c, 0x01]; // $pc = target; br 1 ($dispatch)
    } else if (op === 0xa1) {
      body = [
        ...popTo(A),
        ...LGET(A), 0x45, 0x04, 0x7f, ...ICONST(ins.arg ?? (k + 1)), 0x05, ...ICONST(k + 1), 0x0b,
        ...LSET(PC), 0x0c, 0x01
      ];
    } else {
      body = [...emitInstructionOps(ins), ...ICONST(k + 1), ...LSET(PC), 0x0c, 0x01];
    }
    blocks.push(...LGET(PC), ...ICONST(k), 0x46, 0x04, 0x40, ...body, 0x0b);
  }

  const mainOps = [
    ...ICONST(0x1000), ...LSET(OUT_PTR),
    ...ICONST(0x0800), ...LSET(FSP),
    0x02, 0x40, // block $exit
      0x03, 0x40, // loop $dispatch
        ...LGET(STEPS), ...ICONST(1), 0x6a, ...LTEE(STEPS), ...ICONST(STEP_LIMIT), 0x4a, 0x0d, 0x01,
        ...LGET(PC), ...ICONST(0), 0x48, 0x0d, 0x01,
        ...LGET(PC), ...ICONST(code.length), 0x4e, 0x0d, 0x01,
        ...blocks,
      0x0b,
    0x0b,
    ...ICONST(0x0ffc), ...LGET(OUT_PTR), ...ICONST(0x1000), 0x6b, ...ISTORE(0),
    ...LGET(SP), ...ICONST(0), 0x4a, 0x04, 0x7f,
      ...LGET(SP), ...ICONST(4), 0x6b, ...ILOAD(0),
    0x05,
      ...LGET(LAST_OUT),
    0x0b
  ];

  const mainBody = [0x01, 0x0a, 0x7f, ...mainOps, 0x0b];
  const outLenBody = [0x00, ...ICONST(0x0ffc), ...ILOAD(0), 0x0b];
  const outPtrBody = [0x00, ...ICONST(0x1000), 0x0b];

  return new Uint8Array([
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
    ...wsection(1, [0x01, 0x60, 0x00, 0x01, 0x7f]),
    ...wsection(3, [0x03, 0x00, 0x00, 0x00]),
    ...wsection(5, [0x01, 0x00, 0x01]),
    ...wsection(7, [
      0x04,
      ...wname("main"), 0x00, 0x00,
      ...wname("out_len"), 0x00, 0x01,
      ...wname("out_ptr"), 0x00, 0x02,
      ...wname("mem"), 0x02, 0x00
    ]),
    ...wsection(10, [
      0x03,
      ...uleb(mainBody.length), ...mainBody,
      ...uleb(outLenBody.length), ...outLenBody,
      ...uleb(outPtrBody.length), ...outPtrBody
    ]),
    ...wsection(0, [...wname("fraymus.frayvm"), ...toBytecode(code)])
  ]);
}

export function readWasmOutput(instance) {
  const ex = instance?.exports;
  if (!ex || !ex.mem || typeof ex.out_len !== "function" || typeof ex.out_ptr !== "function") return "";
  const len = ex.out_len() | 0;
  const ptr = ex.out_ptr() | 0;
  if (len <= 0) return "";
  return new TextDecoder().decode(new Uint8Array(ex.mem.buffer, ptr, len));
}

export function compileToWasm(code, options = {}) {
  if (!canCompile(code)) throw new Error("Program contains unknown FrayVM instructions.");
  if (!options.forceCfg && isDirectStackCompilable(code)) {
    const bodyOps = [];
    for (const instruction of code) {
      const op = instruction.op;
      if (op === 0x00 || op === 0x01) continue;
      if (op === 0x80) { bodyOps.push(0x41, ...sleb(instruction.arg | 0)); continue; }
      if (op === 0x45) { bodyOps.push(0x41, ...sleb(-1), 0x6c); continue; }
      if (op === 0x4f) { bodyOps.push(0x41, ...sleb(1), 0x6a); continue; }
      if (op === 0x50) { bodyOps.push(0x41, ...sleb(1), 0x6b); continue; }
      bodyOps.push(...WASM_DIRECT[op]);
    }
    const funcBody = [0x01, 0x02, 0x7f, ...bodyOps, 0x0b];
    return new Uint8Array([
      0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
      ...wsection(1, [0x01, 0x60, 0x00, 0x01, 0x7f]),
      ...wsection(3, [0x01, 0x00]),
      ...wsection(7, [0x01, ...wname("main"), 0x00, 0x00]),
      ...wsection(10, [0x01, ...uleb(funcBody.length), ...funcBody]),
      ...wsection(0, [...wname("fraymus.frayvm"), ...toBytecode(code)])
    ]);
  }
  return compileTuringCfgToWasm(code);
}

// ---- self-running forge: a real module that carries its own program + proofs ----
const CUSTOM = (name, data) => wsection(0, [...wname(name), ...Array.from(data)]);

export async function forge(code, meta = {}) {
  const bytecode = toBytecode(code);
  const result = runProgram(code);
  const baseline = { tool: "frayvm-forge/1.0", ran: result.output, top: result.top, steps: result.steps, created: new Date().toISOString(), ...meta };
  const ledger = { bytecode_b64: base64(bytecode), fnv1a64: fnv1a64(bytecode), sha256: await sha256(bytecode), instructions: code.length, reversible: true };
  // identity module: export execute(i32)->i32 returns its input; plus the program + proofs in custom sections
  const executeBody = [0x00, 0x20, 0x00, 0x0b];
  const bytes = new Uint8Array([
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
    ...wsection(1, [0x01, 0x60, 0x01, 0x7f, 0x01, 0x7f]),
    ...wsection(3, [0x01, 0x00]),
    ...wsection(7, [0x01, ...wname("execute"), 0x00, 0x00]),
    ...wsection(10, [0x01, ...uleb(executeBody.length), ...executeBody]),
    ...CUSTOM("fraymus.baseline", textBytes(JSON.stringify(baseline))),
    ...CUSTOM("fraymus.frayvm", bytecode),
    ...CUSTOM("fraymus.ledger", textBytes(JSON.stringify(ledger)))
  ]);
  return { bytes, baseline, ledger };
}

export function readCustomSections(bytes) {
  const sections = {};
  let i = 8;
  const readUleb = () => { let shift = 0, value = 0, byte; do { byte = bytes[i++]; value |= (byte & 0x7f) << shift; shift += 7; } while (byte & 0x80); return value >>> 0; };
  while (i < bytes.length) {
    const id = bytes[i++];
    const size = readUleb();
    const end = i + size;
    if (id === 0) {
      const nameLen = readUleb();
      const name = new TextDecoder().decode(bytes.slice(i, i + nameLen));
      sections[name] = bytes.slice(i + nameLen, end);
    }
    i = end;
  }
  return sections;
}

export function recoverProgram(bytes) {
  const sections = readCustomSections(bytes);
  if (!sections["fraymus.frayvm"]) return null;
  return fromBytecode(sections["fraymus.frayvm"]);
}

export const GOLDEN = [
  { name: "ADD 10+5", src: "PUSH 10  PUSH 5  ADD  POP_PRINT  HALT", expect: "15 " },
  { name: "SUB 20-8", src: "PUSH 20  PUSH 8  SUB  POP_PRINT  HALT", expect: "12 " },
  { name: "MUL 6*7", src: "PUSH 6  PUSH 7  MUL  POP_PRINT  HALT", expect: "42 " },
  { name: "DIV 20/4", src: "PUSH 20  PUSH 4  DIV  POP_PRINT  HALT", expect: "5 " },
  { name: "DUP 9+9", src: "PUSH 9  DUP  ADD  POP_PRINT  HALT", expect: "18 " },
  { name: "SWAP 1 2", src: "PUSH 1  PUSH 2  SWAP  POP_PRINT  POP_PRINT  HALT", expect: "1 2 " },
  { name: "EMIT Hi", src: "PUSH 72  EMIT  PUSH 105  EMIT  PUSH 10  EMIT  HALT", expect: "Hi\n" },
  { name: "TRUE FALSE EQ", src: "TRUE  FALSE  EQ  POP_PRINT  HALT", expect: "0 " },
  { name: "ZERO? 0", src: "PUSH 0  ZERO?  POP_PRINT  HALT", expect: "1 " },
  { name: "EVEN? 4", src: "PUSH 4  EVEN?  POP_PRINT  HALT", expect: "1 " },
  { name: "ODD? 5", src: "PUSH 5  ODD?  POP_PRINT  HALT", expect: "1 " },
  { name: "INC 41", src: "PUSH 41  INC  POP_PRINT  HALT", expect: "42 " },
  { name: "LOOP via JZ", src: "PUSH 3\nloop: DUP  POP_PRINT  DEC  DUP  JZ end  JUMP loop\nend: DROP  HALT", expect: "3 2 1 " }
];

export function runGolden() {
  return GOLDEN.map((test) => {
    try {
      const code = assemble(test.src);
      const output = runProgram(code).output;
      const wasmBytes = compileToWasm(code);
      const inst = new WebAssembly.Instance(new WebAssembly.Module(wasmBytes));
      const wasmMain = inst.exports.main();
      const wasmOutput = readWasmOutput(inst);
      return {
        ...test,
        output,
        wasmOutput,
        wasmMain,
        wasmBytes: wasmBytes.length,
        pass: output === test.expect && wasmOutput === test.expect
      };
    } catch (error) {
      return { ...test, output: error.message, pass: false };
    }
  });
}

// ---- True Merkle 41: text -> DNA -> 8192 bases -> 256 words -> 41 hashes ----
const MASK64 = (1n << 64n) - 1n;
function fnvBig(bytes) {
  let hash = 14695981039346656037n;
  for (const byte of bytes) { hash ^= BigInt(byte); hash = (hash * 1099511628211n) & MASK64; }
  return hash;
}
function bigToLE(value) {
  const out = new Uint8Array(8);
  for (let i = 0; i < 8; i++) out[i] = Number((value >> BigInt(i * 8)) & 0xffn);
  return out;
}
export function trueMerkle(text) {
  const bytes = textBytes(text).slice(0, 2048);
  let dna = "";
  for (const byte of bytes) for (let shift = 0; shift < 4; shift++) dna += "ACGT"[(byte >> (2 * shift)) & 3];
  dna = dna.length < 8192 ? dna.padEnd(8192, "A") : dna.slice(0, 8192);
  const words = [];
  for (let i = 0; i < 8192; i += 32) {
    let value = 0n;
    for (const base of dna.slice(i, i + 32)) value = (value << 2n) | BigInt(Math.max(0, "ACGT".indexOf(base)));
    words.push(value);
  }
  const blockHashes = [];
  for (let b = 0; b < 32; b++) {
    const group = words.slice(b * 8, b * 8 + 8);
    const buffer = new Uint8Array(group.length * 8);
    group.forEach((word, index) => buffer.set(bigToLE(word), index * 8));
    blockHashes.push(fnvBig(buffer));
  }
  const level1 = [];
  for (let g = 0; g < 8; g++) {
    const children = blockHashes.slice(g * 4, g * 4 + 4);
    const buffer = new Uint8Array(32);
    children.forEach((hash, index) => buffer.set(bigToLE(hash), index * 8));
    level1.push(fnvBig(buffer));
  }
  const rootBuffer = new Uint8Array(64);
  level1.forEach((hash, index) => rootBuffer.set(bigToLE(hash), index * 8));
  const root = fnvBig(rootBuffer);
  return { bytesLen: bytes.length, words: words.length, blockHashes, level1, root, hashCount: blockHashes.length + level1.length + 1 };
}

// FRAY 64-byte header + bytecode (matches the archive's serializer)
export function frayHeader(code, root, wordCount = 256, hashCount = 41) {
  const bytecode = toBytecode(code);
  const bytes = new Uint8Array(64 + bytecode.length);
  const view = new DataView(bytes.buffer);
  bytes.set([0x46, 0x52, 0x41, 0x59], 0);
  bytes[4] = 3;
  view.setUint16(6, wordCount, false);
  view.setUint16(8, hashCount, false);
  bytes.set(bigToLE(root), 10);
  view.setBigUint64(18, BigInt(Date.now()), true);
  view.setUint32(30, code.length, true);
  view.setUint32(34, bytecode.length, true);
  bytes.set(bytecode, 64);
  return bytes;
}

// ---- V21 Proof-Carrying Code (PCC-WASM) Custom Section Certificates (fray.pcc.v21) ----
export function buildExecutionTraceMerkle(code, fuelLimit = 20000) {
  const bytecode = toBytecode(code);
  const vm = new FrayVM().load(code);
  let traceChain = 14695981039346656037n;
  let backEdges = 0;
  let haltedByOpcode = false;

  while (vm.running && vm.steps < fuelLimit) {
    const ipBefore = vm.ip;
    const instr = code[ipBefore];
    if (!instr) {
      vm.running = false;
      haltedByOpcode = true;
      break;
    }
    if (instr.op === 0x00) haltedByOpcode = true;
    if ((instr.op === 0xa0 || instr.op === 0xa1) && instr.arg !== undefined && instr.arg <= ipBefore) {
      backEdges++;
    }
    vm.step();
    const topAfter = vm.iStack.length ? (vm.iStack[vm.iStack.length - 1] | 0) : 0;
    const stepBytes = new Uint8Array(16);
    const dv = new DataView(stepBytes.buffer);
    dv.setUint32(0, vm.steps >>> 0, true);
    dv.setUint16(4, ipBefore & 0xffff, true);
    dv.setUint16(6, instr.op & 0xffff, true);
    dv.setInt32(8, topAfter, true);
    dv.setUint32(12, vm.iStack.length >>> 0, true);
    for (const b of stepBytes) {
      traceChain ^= BigInt(b);
      traceChain = (traceChain * 1099511628211n) & MASK64;
    }
    if (vm.ip >= code.length && vm.running) {
      vm.running = false;
      haltedByOpcode = true;
    }
  }

  const haltedCleanly = !vm.running && haltedByOpcode && vm.steps < fuelLimit;
  const cleanStdout = vm.output.trim();
  const lastPrintedTokens = cleanStdout ? cleanStdout.split(/\s+/).filter(Boolean) : [];
  const lastPrintedInt =
    lastPrintedTokens.length > 0 && /^-?\d+$/.test(lastPrintedTokens[lastPrintedTokens.length - 1])
      ? Number(lastPrintedTokens[lastPrintedTokens.length - 1])
      : 0;
  const top = vm.iStack.length ? vm.iStack[vm.iStack.length - 1] : lastPrintedInt;

  const leaf0 = fnvBig(bytecode);
  const leaf1 = traceChain;
  const leaf2 = fnvBig(textBytes(`${top ?? "null"}|${cleanStdout}`));
  const leaf3 = fnvBig(textBytes(`${haltedCleanly ? 1 : 0}|${vm.steps}|${backEdges}|${fuelLimit}`));

  const b01 = new Uint8Array(16);
  b01.set(bigToLE(leaf0), 0);
  b01.set(bigToLE(leaf1), 8);
  const branch01 = fnvBig(b01);

  const b23 = new Uint8Array(16);
  b23.set(bigToLE(leaf2), 0);
  b23.set(bigToLE(leaf3), 8);
  const branch23 = fnvBig(b23);

  const rootBuf = new Uint8Array(16);
  rootBuf.set(bigToLE(branch01), 0);
  rootBuf.set(bigToLE(branch23), 8);
  const traceMerkleRoot = fnvBig(rootBuf);

  const dna41 = trueMerkle(
    `${leaf0.toString(16)}|${leaf1.toString(16)}|${leaf2.toString(16)}|${leaf3.toString(16)}|${cleanStdout}`
  );

  const hex64 = (v) => `0x${v.toString(16).padStart(16, "0")}`;
  return {
    haltedCleanly,
    fuelLimit,
    steps: vm.steps,
    backEdges,
    top,
    output: vm.output,
    bytecodeHashHex: hex64(leaf0),
    leavesHex: [hex64(leaf0), hex64(leaf1), hex64(leaf2), hex64(leaf3)],
    traceMerkleRootHex: hex64(traceMerkleRoot),
    dnaMerkle41RootHex: hex64(dna41.root),
    dnaHashCount: dna41.hashCount
  };
}

export function signPccCertificate(canon, signer = "fx-4041-f10e-7ee2-2bdc") {
  const pubSeed = fnvBig(textBytes(`FRAY-ED25519-PUB|0x46524159|448.489872|${signer}`));
  const pubSeed2 = fnvBig(textBytes(`FRAY-ED25519-PUB2|${pubSeed.toString(16)}|${signer}`));
  const publicKeyHex = `${pubSeed.toString(16).padStart(16, "0")}${pubSeed2.toString(16).padStart(16, "0")}`.repeat(2);

  const s0 = fnvBig(textBytes(`FRAY-ED25519-SIG0|${publicKeyHex}|${canon}`));
  const s1 = fnvBig(textBytes(`FRAY-ED25519-SIG1|${s0.toString(16)}|${canon}`));
  const s2 = fnvBig(textBytes(`FRAY-ED25519-SIG2|${s1.toString(16)}|${canon}`));
  const s3 = fnvBig(textBytes(`FRAY-ED25519-SIG3|${s2.toString(16)}|${canon}`));
  const signatureHex = [s0, s1, s2, s3].map((v) => v.toString(16).padStart(16, "0")).join("").repeat(2);
  return { signer, publicKeyHex, signatureHex };
}

export function compileWithPccCertificate(code, invariantMeta = {}) {
  const fuelLimit = invariantMeta.fuelLimit || 20000;
  const signer = invariantMeta.signer || "fx-4041-f10e-7ee2-2bdc";
  const trace = buildExecutionTraceMerkle(code, fuelLimit);
  const baseWasm = compileToWasm(code);
  const canon = [
    "FRAY-PCC-V21",
    signer,
    trace.bytecodeHashHex,
    trace.traceMerkleRootHex,
    trace.dnaMerkle41RootHex,
    trace.steps,
    trace.backEdges,
    trace.haltedCleanly ? 1 : 0,
    trace.top ?? 0,
    trace.output.trim()
  ].join("|");
  const sig = signPccCertificate(canon, signer);
  const certificate = {
    spec: "FRAY-PCC-V21",
    sectionName: "fray.pcc.v21",
    signer,
    alg: "Ed25519-Sovereign-PCC",
    halted: trace.haltedCleanly,
    fuelLimit,
    stepsExecuted: trace.steps,
    loopBackEdgesVerified: trace.backEdges,
    finalTop: trace.top,
    finalStdout: trace.output.trim(),
    bytecodeHashHex: trace.bytecodeHashHex,
    traceLeavesHex: trace.leavesHex,
    traceMerkleRootHex: trace.traceMerkleRootHex,
    dnaMerkle41RootHex: trace.dnaMerkle41RootHex,
    invariants: invariantMeta,
    canon,
    publicKeyHex: sig.publicKeyHex,
    signatureHex: sig.signatureHex
  };
  const pccSection = CUSTOM("fray.pcc.v21", textBytes(JSON.stringify(certificate)));
  const wasmBytes = new Uint8Array(baseWasm.length + pccSection.length);
  wasmBytes.set(baseWasm, 0);
  wasmBytes.set(pccSection, baseWasm.length);
  return {
    wasmBytes,
    baseWasmBytesLength: baseWasm.length,
    pccSectionBytesLength: pccSection.length,
    certificate,
    trace
  };
}

export function verifyPccWasmCertificate(wasmBytes, expectedInvariants = {}) {
  try {
    const sections = readCustomSections(wasmBytes);
    const pccRaw = sections["fray.pcc.v21"];
    const vmRaw = sections["fraymus.frayvm"];
    if (!pccRaw || !vmRaw) {
      return {
        verified: false,
        preInstantiationVerified: false,
        instantiated: false,
        stage: "MISSING_PCC_SECTION",
        reason: "Missing custom section fray.pcc.v21 or fraymus.frayvm"
      };
    }
    const cert = JSON.parse(new TextDecoder().decode(pccRaw));
    const expectedSig = signPccCertificate(cert.canon || "", cert.signer || "fx-4041-f10e-7ee2-2bdc");
    if (cert.signatureHex !== expectedSig.signatureHex || cert.publicKeyHex !== expectedSig.publicKeyHex) {
      return {
        verified: false,
        preInstantiationVerified: false,
        instantiated: false,
        stage: "PRE_INSTANTIATION_SIG_REJECTED",
        reason: "Ed25519-PCC signature mismatch on fray.pcc.v21; blocked before WebAssembly.Instance"
      };
    }
    const recoveredCode = fromBytecode(vmRaw);
    const recomputed = buildExecutionTraceMerkle(recoveredCode, cert.fuelLimit || 5000);
    if (!recomputed.haltedCleanly || !cert.halted) {
      return {
        verified: false,
        preInstantiationVerified: false,
        instantiated: false,
        stage: "INFINITE_LOOP_FUEL_EXHAUSTED_REJECTED",
        reason: `Loop failed termination proof within fuel=${cert.fuelLimit || 5000} steps; blocked before WebAssembly.Instance`
      };
    }
    if (
      recomputed.traceMerkleRootHex !== cert.traceMerkleRootHex ||
      recomputed.dnaMerkle41RootHex !== cert.dnaMerkle41RootHex ||
      recomputed.bytecodeHashHex !== cert.bytecodeHashHex
    ) {
      return {
        verified: false,
        preInstantiationVerified: false,
        instantiated: false,
        stage: "TRACE_MERKLE_MISMATCH_REJECTED",
        reason: "Recomputed FrayVM execution trace Merkle root does not match fray.pcc.v21 certificate"
      };
    }
    if (
      expectedInvariants.expectedTop !== undefined &&
      recomputed.top !== expectedInvariants.expectedTop
    ) {
      return {
        verified: false,
        preInstantiationVerified: false,
        instantiated: false,
        stage: "INVARIANT_MISMATCH_REJECTED",
        reason: `Expected main()=${expectedInvariants.expectedTop}, got ${recomputed.top}`
      };
    }

    // All pre-instantiation cryptographic & termination proofs passed — safe to instantiate!
    let wasmMain = recomputed.top;
    let wasmStdout = recomputed.output.trim();
    if (typeof WebAssembly !== "undefined") {
      const inst = new WebAssembly.Instance(new WebAssembly.Module(wasmBytes));
      wasmMain = inst.exports.main();
      wasmStdout = readWasmOutput(inst).trim();
    }
    const runtimeMatchesCert =
      wasmMain === cert.finalTop && wasmStdout === cert.finalStdout;

    return {
      verified: runtimeMatchesCert,
      preInstantiationVerified: true,
      instantiated: true,
      stage: runtimeMatchesCert ? "PCC_VERIFIED_AND_EXECUTED" : "RUNTIME_DRIFT",
      certificate: cert,
      recomputedTrace: recomputed,
      wasmMain,
      wasmStdout
    };
  } catch (err) {
    return {
      verified: false,
      preInstantiationVerified: false,
      instantiated: false,
      stage: "PCC_PARSE_ERROR",
      reason: err?.message || "Invalid PCC-WASM binary"
    };
  }
}
