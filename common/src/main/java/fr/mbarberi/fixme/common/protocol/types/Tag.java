package fr.mbarberi.fixme.common.protocol.types;

public enum Tag {
  BEGIN_STRING("8"),
  MSG_TYPE("35"),
  SENDER_COMP_ID("49"),
  TARGET_COMP_ID("56"),
  CL_ORD_ID("11"),
  SYMBOL("55"),
  ORDER_QTY("38"),
  PRICE("44"),
  MARKET("207"),
  TEXT("58"),
  CHECKSUM("10"),
  MARKET_ID("9001");

  private final String code;

  Tag(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }
}
