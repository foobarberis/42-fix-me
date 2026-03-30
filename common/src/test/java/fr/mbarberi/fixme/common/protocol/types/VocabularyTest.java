package fr.mbarberi.fixme.common.protocol.types;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class VocabularyTest {

  @Test
  void rejectReason_containsExactlySpecValues() {
    String[] names = Arrays.stream(RejectReason.values()).map(Enum::name).toArray(String[]::new);
    assertArrayEquals(
        new String[] {
          "BAD_CHECKSUM",
          "UNKNOWN_DESTINATION",
          "MALFORMED_MESSAGE",
          "UNKNOWN_SYMBOL",
          "INSUFFICIENT_QUANTITY"
        },
        names);
  }

  @Test
  void msgType_containsExactlySpecValues() {
    String[] names = Arrays.stream(MsgType.values()).map(Enum::name).toArray(String[]::new);
    assertArrayEquals(
        new String[] {
          "ASSIGN",
          "BUY",
          "SELL",
          "EXECUTED",
          "REJECTED",
          "REGISTER_MARKET",
          "MARKET_ONLINE",
          "MARKET_OFFLINE"
        },
        names);
  }

  @Test
  void tag_codes() {
    assertEquals("9001", Tag.MARKET_ID.code());
    assertEquals("10", Tag.CHECKSUM.code());
  }
}
