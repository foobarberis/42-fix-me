package fr.mbarberi.fixme.common.protocol.types;

public enum ClientType {
  /* router */
  BAD_CHECKSUM,
  UNKNOWN_DESTINATION,
  MALFORMED_MESSAGE,
  /* market */
  UNKNOWN_SYMBOL, INSUFFICIENT_QUANTITY
}
