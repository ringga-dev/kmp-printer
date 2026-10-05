import java.nio.charset.Charset;
import java.nio.file.*;

/**
 * Dumps decode tables from Java's Charset implementation, which is the
 * reference used by the Android and JVM targets. The Kotlin tables must match
 * this exactly, so the generator consumes these dumps rather than
 * TextDecoder (which disagrees on 101 GBK and 458 BIG5 positions).
 */
public class Oracle {

    public static void main(String[] args) throws Exception {
        String outDir = args[0];

        // Single-byte charsets: every byte value 0x00..0xFF.
        for (String cs : new String[]{"CP437", "ISO-8859-1", "WINDOWS-1252"}) {
            Charset charset = Charset.forName(cs);
            StringBuilder sb = new StringBuilder();
            for (int b = 0; b <= 0xFF; b++) {
                String decoded = new String(new byte[]{(byte) b}, charset);
                int cp = decoded.codePointCount(0, decoded.length()) == 1
                        ? decoded.codePointAt(0) : -1;
                sb.append(b).append(',').append(cp).append('\n');
            }
            write(outDir, cs + "-sb.csv", sb.toString());
        }

        // Double-byte charsets: fixed lead/trail ranges.
        dumpDoubleByte(outDir, "GBK", 0x81, 0xFE, new int[]{0x40, 0x7E, 0x80, 0xFE});
        dumpDoubleByte(outDir, "BIG5", 0xA1, 0xF9, new int[]{0x40, 0x7E, 0xA1, 0xFE});
    }

    private static void dumpDoubleByte(String outDir, String cs, int leadFrom, int leadTo,
                                       int[] ranges) throws Exception {
        Charset charset = Charset.forName(cs);
        StringBuilder sb = new StringBuilder();
        for (int lead = leadFrom; lead <= leadTo; lead++) {
            for (int r = 0; r < ranges.length; r += 2) {
                for (int trail = ranges[r]; trail <= ranges[r + 1]; trail++) {
                    byte[] bytes = {(byte) lead, (byte) trail};
                    String decoded = new String(bytes, charset);
                    int cp = decoded.codePointCount(0, decoded.length()) == 1
                            ? decoded.codePointAt(0) : -1;
                    sb.append(lead).append(',').append(trail).append(',').append(cp).append('\n');
                }
            }
        }
        write(outDir, cs + "-db.csv", sb.toString());
    }

    private static void write(String outDir, String name, String content) throws Exception {
        Path out = Paths.get(outDir, name);
        Files.writeString(out, content);
        int rows = content.split("\n").length;
        System.out.println("wrote " + name + " (" + rows + " rows)");
    }
}