package fr.mbarberi.fixme.common.protocol.types;

public enum RejectReason {
  /* router */
  BAD_CHECKSUM,
  UNKNOWN_DESTINATION,
  MALFORMED_MESSAGE,
  /* market */
  UNKNOWN_SYMBOL, INSUFFICIENT_QUANTITY
}
