# Charset table generator

Regenerates the pure Kotlin charset tables in
`printer/src/commonMain/kotlin/ngga/ring/printer/util/platform/CharsetTable*.kt`.

These tables let the JS and iOS targets encode GBK, BIG5, CP437,
Windows-1252 and ISO-8859-1 correctly. Before this existed both platforms fell
back to UTF-8 for anything outside ASCII, which produced unreadable output on
thermal printers.

## Why Java is the source of truth

Android and JVM encode through `java.nio.charset.Charset`, so the tables are
dumped from that implementation and the Kotlin encoder reproduces its output
byte for byte.

Using the JS `TextDecoder` as the source was tried first and rejected: it
disagreed with Java on 101 GBK positions and 458 BIG5 positions (private-use
code points, alternate mappings, and padding holes).

## Usage

From the repository root:

```bash
cd tools/charset-gen
javac Oracle.java
java -cp . Oracle .
cd ../..
bun tools/charset-gen/generate.mjs
```

The Java step writes five CSV dumps into `tools/charset-gen/`. They are build
artifacts and are not committed; `generate.mjs` refuses to run without them.

`generate.mjs` validates the dumps before emitting anything: single-byte tables
must be dense `0..255`, and double-byte tables must follow the exact
`(lead, trail)` sequence the flat layout assumes. A reordered or truncated dump
fails loudly rather than silently corrupting a table.

## Files

| File | Role |
|---|---|
| `Oracle.java` | Dumps decode tables from `java.nio.charset.Charset` to CSV |
| `generate.mjs` | Emits the Kotlin table objects from the CSV dumps |

## After regenerating

```bash
./gradlew :printer:jvmTest :printer:testDebugUnitTest
```

`CharsetOracleParityTest` compares the regenerated tables against Java directly,
so a bad regeneration fails the build.