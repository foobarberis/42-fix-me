package fr.mbarberi.fixme.common.protocol.checksum;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ChecksumUtilTest {

  @Test
  void compute_knownVectors() {
    assertEquals("000", ChecksumUtil.compute(""));
    assertEquals("198", ChecksumUtil.compute("ABC"));
    assertEquals("066", ChecksumUtil.compute("ABC|"));
  }

  @Test
  void verify_prefixAndChecksum_trueFalse() {
    assertTrue(ChecksumUtil.verify("ABC|", "066"));
    assertFalse(ChecksumUtil.verify("ABC|", "067"));
  }

  @Test
  void verifyLine_validLine_true() {
    assertTrue(ChecksumUtil.verifyLine("ABC|10=066\n"));
  }

  @Test
  void verifyLine_wrongChecksum_false() {
    assertFalse(ChecksumUtil.verifyLine("ABC|10=067\n"));
  }

  @Test
  void verifyLine_missing10_throws() {
    assertThrows(IllegalArgumentException.class, () -> ChecksumUtil.verifyLine("ABC|35=A\n"));
  }

  @Test
  void verifyLine_10NotLast_throws() {
    assertThrows(IllegalArgumentException.class, () -> ChecksumUtil.verifyLine("ABC|10=066|35=A\n"));
  }

  @Test
  void verifyLine_crPresent_throws() {
    assertThrows(IllegalArgumentException.class, () -> ChecksumUtil.verifyLine("ABC|10=066\r\n"));
  }

  @Test
  void verifyLine_missingFinalLf_throws() {
    assertThrows(IllegalArgumentException.class, () -> ChecksumUtil.verifyLine("ABC|10=066"));
  }

  @Test
  void verifyLine_extraLf_throws() {
    assertThrows(IllegalArgumentException.class, () -> ChecksumUtil.verifyLine("ABC|10=066\n\n"));
  }

  @Test
  void verifyLine_nonNumericChecksum_throws() {
    assertThrows(IllegalArgumentException.class, () -> ChecksumUtil.verifyLine("ABC|10=0a6\n"));
  }
}
