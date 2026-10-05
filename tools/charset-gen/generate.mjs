// Generates Kotlin charset tables for KmpPrinter from Java Charset dumps
// (the same implementation the Android and JVM targets use at runtime).
//
// Usage, from the repository root:
//
//   1. javac tools/charset-gen/Oracle.java
//   2. java -cp tools/charset-gen Oracle tools/charset-gen
//   3. bun tools/charset-gen/generate.mjs
//
// The Java step is authoritative. TextDecoder was tried first and rejected: it
// disagrees with Java on 101 GBK and 458 BIG5 positions.
//
// Double-byte output format: one Kotlin String holding the Unicode value for
// each (lead, trail) position in the configured ranges. U+0000 marks a position
// with no mapping. Kotlin builds its lookup map lazily on first use.
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const DIR = HERE;
const ROOT = join(HERE, '..', '..');
const OUT = join(
  ROOT,
  'printer',
  'src',
  'commonMain',
  'kotlin',
  'ngga',
  'ring',
  'printer',
  'util',
  'platform',
);

function readCsv(name) {
  return readFileSync(`${DIR}/${name}`, 'utf8')
    .trim()
    .split('\n')
    .map((line) => line.split(',').map(Number));
}

// Kotlin string literals only understand \t \b \n \r \' \" \\ \$ and \uXXXX.
const SIMPLE_ESCAPES = {
  0x09: '\\t',
  0x0a: '\\n',
  0x0d: '\\r',
  0x22: '\\"',
  0x24: '\\$',
  0x5c: '\\\\',
};

function escapeCp(cp) {
  if (SIMPLE_ESCAPES[cp] !== undefined) return SIMPLE_ESCAPES[cp];
  if (cp >= 0xd800 && cp <= 0xdfff) throw new Error('surrogate U+' + cp.toString(16));
  if (cp < 0x20 || cp >= 0x7f) return '\\u' + cp.toString(16).padStart(4, '0');
  return String.fromCodePoint(cp);
}

/** Packs escaped tokens into lines without splitting an escape sequence. */
function renderTokens(tokens, perLine) {
  const lines = [];
  let current = '';
  let count = 0;
  for (const token of tokens) {
    if (count >= perLine) {
      lines.push(current);
      current = '';
      count = 0;
    }
    current += token;
    count++;
  }
  if (count > 0) lines.push(current);
  return lines.map((l) => `        "${l}"`).join(' +\n');
}

/* ---------------------------------------------------------------- single byte */

const SINGLE_BYTE_LABEL = {
  CP437: 'IBM CP437',
  ISO8859_1: 'ISO-8859-1',
  WINDOWS1252: 'Windows-1252',
};

function singleByte(name, csvName) {
  const rows = readCsv(csvName);
  const tokens = [];
  let mapped = 0;
  let seen = 0;
  for (const [byteValue, cp] of rows) {
    if (byteValue !== seen) throw new Error(`${name}: expected dense 0..255, saw ${byteValue} at ${seen}`);
    if (cp < 0) tokens.push('\\u0000');
    else {
      tokens.push(escapeCp(cp));
      mapped++;
    }
    seen++;
  }
  if (seen !== 256) throw new Error(`${name}: expected 256 rows, got ${seen}`);

  return `/**
 * ${SINGLE_BYTE_LABEL[name]} single-byte decode table, generated from Java's Charset
 * implementation.
 *
 * Index is the byte value. U+0000 marks a byte with no mapping.
 */
internal object ${name}Table {
    const val MAPPED = ${mapped}

    val TABLE: String =
${renderTokens(tokens, 24)}
}`;
}

/* ---------------------------------------------------------------- double byte */

const DOUBLE_BYTE_LABEL = { GBK: 'GBK', BIG5: 'Big5' };

function doubleByte(name, csvName, leadFrom, leadTo, trailRanges) {
  const rows = readCsv(csvName);
  const trailCount = trailRanges.reduce((sum, [a, b]) => sum + (b - a + 1), 0);
  const expectedLength = (leadTo - leadFrom + 1) * trailCount;
  if (rows.length !== expectedLength) {
    throw new Error(`${name}: expected ${expectedLength} rows, got ${rows.length}`);
  }

  const tokens = [];
  let mapped = 0;
  // Guard against a reordered dump silently corrupting the flat layout: walk
  // the expected (lead, trail) sequence and require the dump to match it.
  let row = 0;
  for (let lead = leadFrom; lead <= leadTo; lead++) {
    for (const [ta, tb] of trailRanges) {
      for (let trail = ta; trail <= tb; trail++) {
        const entry = rows[row];
        if (!entry) throw new Error(`${name}: dump ended early at row ${row}`);
        if (entry[0] !== lead || entry[1] !== trail) {
          throw new Error(
            `${name}: expected ${lead.toString(16)},${trail.toString(16)} at row ${row} but found ` +
              `${entry[0].toString(16)},${entry[1].toString(16)}`,
          );
        }
        row++;
      }
    }
  }

  for (const r of rows) {
    const cp = r[2];
    if (cp < 0) tokens.push('\\u0000');
    else {
      tokens.push(escapeCp(cp));
      mapped++;
    }
  }

  const rangeLiterals = trailRanges
    .map(([a, b]) => `intArrayOf(${'0x' + a.toString(16)}, ${'0x' + b.toString(16)})`)
    .join(', ');

  return `/**
 * ${DOUBLE_BYTE_LABEL[name]} double-byte decode table, generated from Java's Charset
 * implementation.
 *
 * Position = (lead - LEAD_FROM) * TRAIL_COUNT + offset of trail within
 * [TRAIL_RANGES]. U+0000 marks a position with no mapping.
 */
internal object ${name}Table {
    const val LEAD_FROM = ${leadFrom}
    const val LEAD_TO = ${leadTo}
    const val TRAIL_COUNT = ${trailCount}
    const val MAPPED = ${mapped}

    /** Inclusive trail byte ranges, in ascending order. */
    val TRAIL_RANGES: Array<IntArray> = arrayOf(${rangeLiterals})

    val TABLE: String =
${renderTokens(tokens, 20)}

    /**
     * Reverse index from character to its lead/trail byte pair.
     *
     * Built once and cached: a decode table has to be walked once per character
     * class the first time it is touched, and never again.
     */
    fun lookupIndex(): Map<Char, ByteArray> {
        val index = HashMap<Char, ByteArray>(MAPPED * 2)
        var offset = 0
        for (lead in LEAD_FROM..LEAD_TO) {
            for (range in TRAIL_RANGES) {
                for (trail in range[0]..range[1]) {
                    val ch = TABLE[offset++]
                    if (ch != '\\u0000') {
                        // First mapping wins so the lowest byte pair is used,
                        // matching decoder behaviour. Written without
                        // putIfAbsent because that is JVM-only in Kotlin.
                        if (ch !in index) index[ch] = byteArrayOf(lead.toByte(), trail.toByte())
                    }
                }
            }
        }
        return index
    }
}`;
}

/* -------------------------------------------------------------------- write */

const HEADER = `package ngga.ring.printer.util.platform

// Generated by tools/charset-gen. Source of truth: Java's java.nio.charset.Charset
// decode tables, which is what the Android and JVM targets use at runtime.
`;

const files = [
  [
    'CharsetTableSingleByte.kt',
    [
      singleByte('CP437', 'CP437-sb.csv'),
      singleByte('ISO8859_1', 'ISO-8859-1-sb.csv'),
      singleByte('WINDOWS1252', 'WINDOWS-1252-sb.csv'),
    ].join('\n\n'),
  ],
  [
    'CharsetTableDoubleByte.kt',
    [
      doubleByte('GBK', 'GBK-db.csv', 0x81, 0xfe, [
        [0x40, 0x7e],
        [0x80, 0xfe],
      ]),
      doubleByte('BIG5', 'BIG5-db.csv', 0xa1, 0xf9, [
        [0x40, 0x7e],
        [0xa1, 0xfe],
      ]),
    ].join('\n\n'),
  ],
];

for (const [name, content] of files) {
  writeFileSync(`${OUT}/${name}`, HEADER + content + '\n');
  console.log(`wrote ${name} (${content.length} chars)`);
}