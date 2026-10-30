export const VS_LOW = 0xfe00;
export const VS_HIGH = 0xe0100;
export const ZWS = "\u200B";
export const ZWJ = "\u200D";
export const DNA = "ACGT";
export const MAX_BYTES = 65536;

export const CARRIERS = [
  "🧩", "🧠", "🧬", "🔥", "💎", "🌀", "🔐", "📦", "🔗", "💥", "🌊", "🚀",
  "⚡", "🌟", "✨", "😀", "👋", "🌍", "💻", "📊", "🌐", "🔒", "⏰", "💡",
  "✅", "❌", "🛑", "🟢", "🔬", "🎯", "💰", "🍀", "🌙", "⭐", "🎵", "🎨",
  "🤖", "👾", "🦊", "🐙", "🌈", "🍕", "☕", "🎁", "📎", "🗝", "🛸", "🪐"
];

export const MEANINGS = {
  hello: "👋", hi: "👋", goodbye: "👋", world: "🌍", earth: "🌍", love: "💖", heart: "💖",
  mind: "🧠", brain: "🧠", consciousness: "🧠", think: "🧠", physics: "🔬", science: "🔬", quantum: "🌀",
  computer: "💻", code: "💻", program: "💻", data: "📊", network: "🌐", web: "🌐", security: "🔒",
  secret: "🔒", key: "🔐", lock: "🔐", breakthrough: "🚀", launch: "🚀", fast: "🚀", power: "⚡",
  energy: "⚡", money: "💰", success: "🎯", goal: "🎯", time: "⏰", space: "🪐", light: "💡",
  idea: "💡", dark: "🌙", night: "🌙", star: "⭐", yes: "✅", ok: "✅", no: "❌", stop: "🛑",
  go: "🟢", fire: "🔥", hot: "🔥", cold: "🧊", dna: "🧬", life: "🧬", puzzle: "🧩", music: "🎵",
  art: "🎨", robot: "🤖", game: "👾", gift: "🎁", coffee: "☕", rainbow: "🌈", water: "🌊", wave: "🌊"
};

const encoder = new TextEncoder();
const decoder = new TextDecoder("utf-8", { fatal: false });

export function textBytes(text) {
  return encoder.encode(text);
}

export function bytesText(bytes) {
  return decoder.decode(bytes);
}

export function isUtf8(bytes) {
  try {
    new TextDecoder("utf-8", { fatal: true }).decode(bytes);
    return true;
  } catch {
    return false;
  }
}

export function toVS(byte) {
  return String.fromCodePoint(byte < 16 ? VS_LOW + byte : VS_HIGH + byte - 16);
}

export function fromVS(cp) {
  if (cp >= VS_LOW && cp <= VS_LOW + 15) return cp - VS_LOW;
  if (cp >= VS_HIGH && cp <= VS_HIGH + 239) return cp - VS_HIGH + 16;
  return null;
}

export function cleanCarrier(carrier) {
  const kept = [...(carrier || "")].filter((ch) => fromVS(ch.codePointAt(0)) === null && ch !== ZWS);
  return kept.join("") || "🧩";
}

export function encodeGlyph(bytes, carrier, spread = false) {
  const base = cleanCarrier(carrier);
  if (!spread) return base + Array.from(bytes, toVS).join("");
  return Array.from(bytes, (byte) => base + toVS(byte)).join("");
}

export function encodeMeaning(text, bytes, fallback) {
  const words = text.toLowerCase().match(/[a-z]+/g) || [];
  const glyphs = words.map((word) => MEANINGS[word]).filter(Boolean);
  if (!glyphs.length) glyphs.push(cleanCarrier(fallback));
  const share = Math.ceil(bytes.length / glyphs.length) || 1;
  return glyphs.map((glyph, index) => glyph + Array.from(bytes.slice(index * share, (index + 1) * share), toVS).join("")).join("");
}

export function decodeGlyph(text) {
  const bytes = [];
  const carriers = [];
  for (const ch of text) {
    const value = fromVS(ch.codePointAt(0));
    if (value === null) {
      if (ch.trim()) carriers.push(ch);
    } else {
      bytes.push(value);
    }
  }
  return { bytes: Uint8Array.from(bytes), carriers };
}

export function bytesToDna(bytes) {
  let out = "";
  for (const byte of bytes) out += DNA[(byte >> 6) & 3] + DNA[(byte >> 4) & 3] + DNA[(byte >> 2) & 3] + DNA[byte & 3];
  return out;
}

export function dnaToBytes(text) {
  const bases = text.toUpperCase().replace(/[^ACGT]/g, "");
  if (bases.length % 4) throw new Error(`DNA needs a multiple of 4 bases, got ${bases.length}`);
  const out = new Uint8Array(bases.length / 4);
  for (let i = 0; i < out.length; i++) {
    let byte = 0;
    for (let j = 0; j < 4; j++) byte = (byte << 2) | DNA.indexOf(bases[i * 4 + j]);
    out[i] = byte;
  }
  return out;
}

export function bytesToZeroWidth(bytes) {
  let out = "";
  for (const byte of bytes) for (let bit = 7; bit >= 0; bit--) out += (byte >> bit) & 1 ? ZWJ : ZWS;
  return out;
}

export function zeroWidthToBytes(text) {
  const bits = [...text].filter((ch) => ch === ZWS || ch === ZWJ).map((ch) => (ch === ZWJ ? 1 : 0));
  const out = new Uint8Array(Math.floor(bits.length / 8));
  for (let i = 0; i < out.length; i++) {
    let byte = 0;
    for (let j = 0; j < 8; j++) byte = (byte << 1) | bits[i * 8 + j];
    out[i] = byte;
  }
  return out;
}

export function hideZeroWidth(visible, bytes) {
  const chars = [...(visible || "")];
  if (chars.length < 2) return (visible || "") + bytesToZeroWidth(bytes);
  return chars[0] + bytesToZeroWidth(bytes) + chars.slice(1).join("");
}

export function detect(text) {
  const value = text || "";
  if ([...value].some((ch) => fromVS(ch.codePointAt(0)) !== null)) return "glyph";
  if (value.includes(ZWS) || (value.includes(ZWJ) && !/\p{Extended_Pictographic}/u.test(value))) return "zero";
  const bases = value.replace(/\s/g, "");
  if (bases && /^[ACGTacgt]+$/.test(bases) && bases.length % 4 === 0) return "dna";
  return "text";
}

export function readBack(text) {
  const kind = detect(text);
  if (kind === "glyph") {
    const { bytes, carriers } = decodeGlyph(text);
    return { kind, bytes, carriers };
  }
  if (kind === "zero") return { kind, bytes: zeroWidthToBytes(text), carriers: [] };
  if (kind === "dna") return { kind, bytes: dnaToBytes(text), carriers: [] };
  return { kind, bytes: textBytes(text), carriers: [] };
}

export function fnv1a64(bytes) {
  let hash = 14695981039346656037n;
  for (const byte of bytes) hash = ((hash ^ BigInt(byte)) * 1099511628211n) & 0xffffffffffffffffn;
  return hash.toString(16).padStart(16, "0");
}

export async function sha256(bytes) {
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

export function hex(bytes, spaced = true) {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join(spaced ? " " : "");
}

export function hexToBytes(text) {
  const clean = text.replace(/0x/gi, "").replace(/[^0-9a-f]/gi, "");
  if (clean.length % 2) throw new Error("Hex needs an even number of digits");
  return Uint8Array.from(clean.match(/../g) || [], (pair) => parseInt(pair, 16));
}

export function base64(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary);
}

export function views(bytes) {
  const list = Array.from(bytes);
  return {
    text: bytesText(bytes),
    hex: hex(bytes),
    base64: base64(bytes),
    json: JSON.stringify(list),
    c: `unsigned char payload[${list.length}] = { ${list.map((b) => "0x" + b.toString(16).padStart(2, "0")).join(", ")} };`,
    js: `new Uint8Array([${list.join(", ")}])`,
    python: `bytes([${list.join(", ")}])`,
    java: `byte[] payload = { ${list.map((b) => (b > 127 ? `(byte) 0x${b.toString(16).padStart(2, "0")}` : `0x${b.toString(16).padStart(2, "0")}`)).join(", ")} };`,
    dna: bytesToDna(bytes)
  };
}

export function javaSource(bytes, carrier) {
  const chunks = base64(bytes).match(/.{1,4096}/g) || [""];
  const glyph = cleanCarrier(carrier);
  const escaped = [...glyph].map((ch) => {
    const cp = ch.codePointAt(0);
    return cp < 128 && ch !== '"' && ch !== "\\" ? ch : `\\u${ch.charCodeAt(0).toString(16).padStart(4, "0")}${ch.length > 1 ? `\\u${ch.charCodeAt(1).toString(16).padStart(4, "0")}` : ""}`;
  }).join("");
  return `import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * FrayGlyph — the Fraymus variation-selector codec (ProtocolZero) in plain Java.
 * JDK 11+ only: java.lang, java.util, java.security, java.nio, java.io. No external libs.
 * Byte 0-15 -> U+FE00-FE0F, byte 16-255 -> U+E0100-E01EF. One selector per byte, all 256 values.
 */
public final class FrayGlyph {
    static final String CARRIER = "${escaped}";
    static final String[] PAYLOAD_B64 = {
${chunks.map((chunk) => `        "${chunk}"`).join(",\n")}
    };

    static String encode(String carrier, byte[] bytes) {
        StringBuilder sb = new StringBuilder(carrier);
        for (byte b : bytes) {
            int v = b & 0xFF;
            sb.appendCodePoint(v < 16 ? 0xFE00 + v : 0xE0100 + (v - 16));
        }
        return sb.toString();
    }

    static byte[] decode(String glyph) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        glyph.codePoints().forEach(cp -> {
            if (cp >= 0xFE00 && cp <= 0xFE0F) out.write(cp - 0xFE00);
            else if (cp >= 0xE0100 && cp <= 0xE01EF) out.write(cp - 0xE0100 + 16);
        });
        return out.toByteArray();
    }

    static String dna(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 4);
        for (byte b : bytes) for (int shift = 6; shift >= 0; shift -= 2) sb.append("ACGT".charAt(((b & 0xFF) >> shift) & 3));
        return sb.toString();
    }

    static long fnv1a64(byte[] bytes) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : bytes) {
            hash ^= (b & 0xFF);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    static String sha256(byte[] bytes) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** Reads the Fray-Exec x86 subset (mov/add/sub/xor eax,imm32 · xor eax,eax · inc · dec · nop · ret). Interprets, never executes. */
    static Integer x86Subset(byte[] code) {
        int eax = 0, ip = 0;
        while (ip < code.length) {
            int op = code[ip] & 0xFF;
            if ((op == 0xB8 || op == 0x05 || op == 0x2D || op == 0x35) && ip + 5 <= code.length) {
                int imm = (code[ip + 1] & 0xFF) | (code[ip + 2] & 0xFF) << 8 | (code[ip + 3] & 0xFF) << 16 | (code[ip + 4] & 0xFF) << 24;
                eax = op == 0xB8 ? imm : op == 0x05 ? eax + imm : op == 0x2D ? eax - imm : eax ^ imm;
                ip += 5;
            } else if (op == 0x31 && ip + 1 < code.length && (code[ip + 1] & 0xFF) == 0xC0) { eax = 0; ip += 2; }
            else if (op == 0x40) { eax++; ip++; }
            else if (op == 0x48) { eax--; ip++; }
            else if (op == 0x90) ip++;
            else if (op == 0xC3) return eax;
            else return null;
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        byte[] payload = Base64.getDecoder().decode(String.join("", PAYLOAD_B64));
        String glyph = encode(CARRIER, payload);
        byte[] back = decode(glyph);
        System.out.println("payload      " + payload.length + " bytes");
        System.out.println("glyph        " + glyph.codePointCount(0, glyph.length()) + " code points");
        System.out.println("round trip   " + (Arrays.equals(payload, back) ? "exact" : "DIFFERS"));
        System.out.println("sha256       " + sha256(back));
        System.out.println("fnv1a-64     " + String.format("%016x", fnv1a64(back)));
        String bases = dna(back);
        System.out.println("dna          " + (bases.length() > 64 ? bases.substring(0, 64) + "…" : bases));
        Integer eax = x86Subset(back);
        if (eax != null) System.out.println("x86 subset   eax = " + eax);
        Files.writeString(Path.of(args.length > 0 ? args[0] : "glyph.txt"), glyph, StandardCharsets.UTF_8);
        System.out.println("wrote        " + (args.length > 0 ? args[0] : "glyph.txt"));
    }
}
`;
}

export function x86Subset(bytes) {
  const lines = [];
  const imm = (at) => (bytes[at] | (bytes[at + 1] << 8) | (bytes[at + 2] << 16) | (bytes[at + 3] << 24)) | 0;
  const IMM_OPS = { 0xb8: "mov", 0x05: "add", 0x2d: "sub", 0x35: "xor" };
  let eax = 0;
  let i = 0;
  while (i < bytes.length) {
    const op = bytes[i];
    const take = (size, asm) => {
      lines.push({ at: i, hex: hex(bytes.slice(i, i + size)), asm });
      i += size;
    };
    if (IMM_OPS[op] && i + 5 <= bytes.length) {
      const value = imm(i + 1);
      if (op === 0xb8) eax = value;
      if (op === 0x05) eax = (eax + value) | 0;
      if (op === 0x2d) eax = (eax - value) | 0;
      if (op === 0x35) eax ^= value;
      take(5, `${IMM_OPS[op]} eax, ${value}`);
    } else if (op === 0x31 && bytes[i + 1] === 0xc0) {
      eax = 0;
      take(2, "xor eax, eax");
    } else if (op === 0x40) {
      eax = (eax + 1) | 0;
      take(1, "inc eax");
    } else if (op === 0x48) {
      eax = (eax - 1) | 0;
      take(1, "dec eax");
    } else if (op === 0x90) {
      take(1, "nop");
    } else if (op === 0xc3) {
      take(1, "ret");
      return { lines, eax, ok: true };
    } else {
      return { lines, eax, ok: false, stop: i };
    }
  }
  return { lines, eax, ok: false, stop: i };
}

export function codepoints(text) {
  return [...text].map((ch) => "U+" + ch.codePointAt(0).toString(16).toUpperCase().padStart(4, "0"));
}
