package fr.mbarberi.fixme.common.protocol.checksum;

public final class ChecksumUtil {
  private ChecksumUtil() {
  }

  public static boolean verifyLine(String line) {
    if (line == null) throw new IllegalArgumentException("line is null");
    if (line.indexOf('\r') >= 0) throw new IllegalArgumentException("line must not contain CR");
    if (!line.endsWith("\n")) throw new IllegalArgumentException("line must end with LF");
    if (line.lastIndexOf('\n') != line.length() - 1) throw new IllegalArgumentException("line must contain exactly one LF at the end");

    int checksumFieldStart = line.lastIndexOf("|10=");
    if (checksumFieldStart < 0) throw new IllegalArgumentException("missing 10 field");

    /* the last '|' must be the one starting "|10=" */
    if (line.lastIndexOf('|') != checksumFieldStart) {
      throw new IllegalArgumentException("checksum not last field");
    }

    int digitsStart = checksumFieldStart + 4; /* after "|10=" */
    int digitsEnd = line.length() - 1; /* exclude '\n' */
    if (digitsEnd - digitsStart != 3) throw new IllegalArgumentException("checksum must be 3 digits");
    
    String providedChecksum = line.substring(digitsStart, digitsEnd);
    String prefix = line.substring(0, checksumFieldStart + 1); /* include trailing '|' */
    return verify(prefix, providedChecksum);
  }

  public static String compute(String prefix) {
    if (prefix == null) throw new IllegalArgumentException("prefix is null");
    if (prefix.indexOf('\n') >= 0 || prefix.indexOf('\r') >= 0) {
      throw new IllegalArgumentException("prefix must not contain CR/LF");
    }

    int sum = 0;
    for (int i =0; i < prefix.length(); i++) {
      char c = prefix.charAt(i);
      if (c > 127) throw new IllegalArgumentException("prefix contains non-ASCII character");
      sum += c;
    }

    int chk = sum % 256;
    return pad3(chk);
  }

  public static boolean verify(String prefix, String providedChecksum) {
    if (providedChecksum == null) throw new IllegalArgumentException("provided checksum is null");
    if (providedChecksum.length() != 3) throw new IllegalArgumentException("checksum must be 3 digits");
    for (int i = 0; i < 3; i++) {
      char c = providedChecksum.charAt(i);
      if (c < '0' || c > '9') throw new IllegalArgumentException("provided checksum must be numeric");
    }
    return compute(prefix).equals(providedChecksum);
  }

  private static String pad3(int value) {
      if (value < 0 || value > 255) throw new IllegalArgumentException("checksum out of range");
      if (value < 10) return "00" + value;
      if (value < 100) return "0" + value;
      return Integer.toString(value);
  } 
}
