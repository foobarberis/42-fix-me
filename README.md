# Fixme: Project Requirements & Evaluation

## Project Overview
Develop a multi-threaded network application simulating a financial market using the FIX (Financial Information eXchange) protocol.

## Technical Specifications
- **Language**: Java (latest LTS).
- **Build Tool**: Maven (multi-module build).
- **Networking**: Non-blocking sockets (NIO).
- **Concurrency**: Java Executor framework.
- **Design Pattern**: Chain-of-responsibility.
- **Build Command**: `mvn clean package` (must generate runnable `.jar` files for each component).

## Components

### 1. Router (Central Server)
- **Functions**: Dispatch messages, maintain routing table, no business logic.
- **Ports**:
  - `5000`: Broker connections.
  - `5001`: Market connections.
- **ID Assignment**: Assigns a unique 6-digit ID to every connecting component and communicates it back to them.
- **Message Processing**:
  1. Validate checksum.
  2. Identify destination via routing table (using the 6-digit ID).
  3. Forward message.

### 2. Broker (Client)
- **Operations**: Sends `Buy` and `Sell` orders.
- **Inputs**: Receives `Executed` or `Rejected` responses from the Market.
- **Message Requirements**: Must include the assigned ID and a checksum.

### 3. Market (Client)
- **Logic**: Maintains a list of tradable instruments.
- **Execution Rules**:
  - Reject if instrument is not traded.
  - Reject if requested quantity is unavailable (for Buy orders).
  - Update internal inventory upon successful execution.
- **Outputs**: Sends `Executed` or `Rejected` messages to Brokers via the Router.

## FIX Message Structure
Messages must follow FIX notation and include:
- **Header**: ID assigned by the Router.
- **Mandatory Fields**: Instrument, Quantity, Market, Price.
- **Trailer**: Checksum.

## Evaluation Criteria

### Preliminaries
- **Compilation**: Project must build via `mvn clean package` and produce runnable jars.
- **Design**: Must follow Client-Server architecture and utilize the Chain-of-responsibility pattern.

### Functionality
- **Router**: Accepts connections on 5000/5001; assigns IDs upon connection.
- **Broker**: Successfully connects; messages include ID and checksum; messages appear in Router.
- **Market**: Receives/executes/rejects orders; response messages include Router ID and checksum.

## Submission
- Submit via Git repository.
- Ensure folder and file naming conventions are strictly followed.

# Protocol Specification

This section is normative for the wire protocol and runtime behavior. If it conflicts with the high-level overview above, this section takes precedence.

This project implements a small FIX-like protocol subset. It is not a full FIX engine.

### 1. Wire framing

- Encoding: ASCII only.
- One application message = one TCP line terminated by `\n`.
- Fields use `tag=value`.
- Fields are separated by the literal `|` character.
- `10` (`CheckSum`) is always the last field, immediately before `\n`.
- Field order is fixed per message type and must exactly match Section 5.
- Only the tags listed in this section are supported.
- Unsupported tags, duplicate tags, missing mandatory tags, or out-of-order tags make the message malformed.
- Messages addressed to the Router itself use `56=000000`. All other destinations are client IDs.

### 2. Supported tags

| Tag | Name | Meaning |
|---|---|---|
| `8` | `BeginString` | Always `FIX.4.4` |
| `35` | `MsgType` | Message type |
| `49` | `SenderCompID` | `sourceId` |
| `56` | `TargetCompID` | `destinationId` |
| `11` | `ClOrdID` | Client order ID |
| `55` | `Symbol` | Instrument |
| `38` | `OrderQty` | Quantity |
| `44` | `Price` | Price |
| `207` | `Market` | Market / venue name |
| `58` | `Text` | Reject reason or handshake text |
| `10` | `CheckSum` | 3-digit checksum |
| `9001` | `MarketID` | Market ID assigned by the Router |

Unsupported FIX tags such as `9`, `34`, `52`, `54`, `150`, and `39` are not used in this project.

### 3. IDs and routing

- `49` is the sender ID.
- `56` is the destination ID.
- The Router uses `56` as the routing-table lookup key for client-to-client traffic.
- `000000` is reserved for Router-generated messages and messages addressed to the Router itself.
- A message with `56=000000` is handled locally by the Router. It is not looked up in the client routing table.
- Client IDs are 6 ASCII digits in the range `000001` to `999999`.
- IDs must be globally unique among active connections. After a disconnect, an ID may be reused, but only after the old routing and discovery state for that connection has been fully removed.
- `207` does not affect routing for orders or business responses. The Router only interprets `207` during market registration and market discovery broadcasts.
- `44` and all other business fields are opaque to the Router for forwarded order traffic.
- `9001` is the market ID announced by the Router during market discovery. Brokers must use `9001` in tag `56` when sending to a Market.

### 4. Supported message types (`35`)

The protocol supports these `MsgType` values:

- `ASSIGN`
- `BUY`
- `SELL`
- `EXECUTED`
- `REJECTED`
- `REGISTER_MARKET`
- `MARKET_ONLINE`
- `MARKET_OFFLINE`

`BUY` and `SELL` are separate message types in this project. Tag `54` (`Side`) is not used.

`REJECTED` can be generated by a Market or by the Router. The required field set depends on the sender and is defined in Section 5.

### 5. Message matrix

| `35` | Sender (`49`) | Target (`56`) | Tags in exact order | Notes |
|---|---|---|---|---|
| `ASSIGN` | Router `000000` | Assigned client ID | `8,35,49,56,58,10` | `58=ASSIGNED` |
| `BUY` | Broker ID | Market ID | `8,35,49,56,11,55,38,44,207,10` | Broker -> Market only |
| `SELL` | Broker ID | Market ID | `8,35,49,56,11,55,38,44,207,10` | Broker -> Market only |
| `EXECUTED` | Market ID | Broker ID | `8,35,49,56,11,55,38,44,207,10` | Echoes the order fields |
| `REJECTED` | Market ID | Broker ID | `8,35,49,56,11,55,38,44,207,58,10` | Business reject |
| `REJECTED` | Router `000000` | Original sender ID | `8,35,49,56,11,58,10` | Protocol reject, used when a valid `11` is available |
| `REJECTED` | Router `000000` | Original sender ID | `8,35,49,56,58,10` | Protocol reject, used when `11` is unavailable |
| `REGISTER_MARKET` | Market ID | Router `000000` | `8,35,49,56,207,10` | Market -> Router only, once per connection |
| `MARKET_ONLINE` | Router `000000` | Broker ID | `8,35,49,56,9001,207,10` | Router -> Broker only |
| `MARKET_OFFLINE` | Router `000000` | Broker ID | `8,35,49,56,9001,207,10` | Router -> Broker only |

Notes:
- The schemas above are exclusive. A receiver validates `REJECTED` against the sender-specific Router or Market schema.
- Any sender, target, or direction that does not match this table is `MALFORMED_MESSAGE`.

### 6. Connection handshake

1. A Broker connects to Router port `5000`.
2. A Market connects to Router port `5001`.
3. The Router assigns a unique 6-digit ID.
4. The Router immediately sends this message on the same socket:

```text
8=FIX.4.4|35=ASSIGN|49=000000|56=<assignedId>|58=ASSIGNED|10=<chk>\n
```

5. The client stores `56` from this message as its own ID.
6. A client must not send business messages before it receives `ASSIGN`.
7. A Market may send `REGISTER_MARKET` only after `ASSIGN`, and at most once per connection.
8. After `ASSIGN`, a Broker receives zero or more `MARKET_ONLINE` snapshot messages representing a point-in-time view of currently registered Markets.

Notes:
- `49=000000` means the Router is the sender.
- In the `ASSIGN` message, `56` carries the newly assigned client ID.
- Snapshot and live `MARKET_ONLINE` / `MARKET_OFFLINE` messages may be interleaved. Brokers must reconcile them by `9001`.

### 7. Exact wire formats

Field order is fixed and must be exactly as shown.

#### 7.1 Assign

```text
8=FIX.4.4|35=ASSIGN|49=000000|56=<assignedId>|58=ASSIGNED|10=<chk>\n
```

#### 7.2 Buy order

```text
8=FIX.4.4|35=BUY|49=<brokerId>|56=<marketId>|11=<clOrdId>|55=<symbol>|38=<qty>|44=<price>|207=<market>|10=<chk>\n
```

#### 7.3 Sell order

```text
8=FIX.4.4|35=SELL|49=<brokerId>|56=<marketId>|11=<clOrdId>|55=<symbol>|38=<qty>|44=<price>|207=<market>|10=<chk>\n
```

#### 7.4 Executed response

```text
8=FIX.4.4|35=EXECUTED|49=<marketId>|56=<brokerId>|11=<clOrdId>|55=<symbol>|38=<qty>|44=<price>|207=<market>|10=<chk>\n
```

`EXECUTED` echoes the order fields and swaps sender and target IDs.

#### 7.5 Rejected response from Market

```text
8=FIX.4.4|35=REJECTED|49=<marketId>|56=<brokerId>|11=<clOrdId>|55=<symbol>|38=<qty>|44=<price>|207=<market>|58=<reason>|10=<chk>\n
```

#### 7.6 Correlated rejected response from Router

```text
8=FIX.4.4|35=REJECTED|49=000000|56=<senderId>|11=<clOrdId>|58=<reason>|10=<chk>\n
```

#### 7.7 Uncorrelated rejected response from Router

```text
8=FIX.4.4|35=REJECTED|49=000000|56=<senderId>|58=<reason>|10=<chk>\n
```

#### 7.8 Register market

```text
8=FIX.4.4|35=REGISTER_MARKET|49=<marketId>|56=000000|207=<market>|10=<chk>\n
```

#### 7.9 Market online

```text
8=FIX.4.4|35=MARKET_ONLINE|49=000000|56=<brokerId>|9001=<marketId>|207=<market>|10=<chk>\n
```

#### 7.10 Market offline

```text
8=FIX.4.4|35=MARKET_OFFLINE|49=000000|56=<brokerId>|9001=<marketId>|207=<market>|10=<chk>\n
```

### 8. Field rules

- All tag values are case-sensitive.
- Printable ASCII text means bytes in the range `0x20` to `0x7E`, excluding `|` and `=`.
- `8`: must be exactly `FIX.4.4`.
- `35`: must be one of the supported message types.
- `49`, `56`, `9001`: exactly 6 ASCII digits (`[0-9]{6}`).
- `49=000000` is valid only in Router-generated messages.
- `56=000000` is valid only when a client intentionally addresses the Router. In this protocol, that is only `REGISTER_MARKET`.
- `11`: non-empty printable ASCII text, unique per Broker order, and must not contain `\r` or `\n`.
- `55`: non-empty printable ASCII text and must not contain `\r` or `\n`.
- `38`: positive integer matching `[1-9][0-9]*`.
- `44`: positive decimal number matching `0\.[0-9]+|[1-9][0-9]*(\.[0-9]+)?`.
- `207`: non-empty printable ASCII text and must not contain `\r` or `\n`.
- `58`: non-empty printable ASCII text and must not contain `\r` or `\n`.
- `10`: exactly 3 ASCII digits.
- `44` is required on `BUY`, `SELL`, `EXECUTED`, and Market-generated `REJECTED`. The Router treats it as opaque and forwards it unchanged.
- `207` is required on `BUY`, `SELL`, `EXECUTED`, Market-generated `REJECTED`, `REGISTER_MARKET`, `MARKET_ONLINE`, and `MARKET_OFFLINE`.
- On `BUY`, `SELL`, `EXECUTED`, and Market-generated `REJECTED`, `207` is informational business data. It does not affect routing.
- On `REGISTER_MARKET`, `MARKET_ONLINE`, and `MARKET_OFFLINE`, `207` is the market name bound to `9001`.
- On `EXECUTED` and Market-generated `REJECTED`, the Market must echo `11`, `55`, `38`, `44`, and `207` unchanged from the original order.

### 9. Checksum

Checksum uses the standard FIX-style modulo-256 rule, adapted to this project's wire format.

Algorithm:

1. Serialize the message up to but not including tag `10`.
2. Include every byte before `10=`, including all `|` separators.
3. Do not include the bytes of tag `10`.
4. Do not include the final `\n`.
5. Sum the ASCII byte values.
6. Compute `sum % 256`.
7. Encode the result as a 3-digit decimal string with leading zeros if needed.

Example structure:

- Prefix used for checksum:

```text
8=FIX.4.4|35=BUY|49=123456|56=654321|11=ord-1|55=AAPL|38=10|44=182.50|207=XNAS|
```

- Final message on the wire:

```text
8=FIX.4.4|35=BUY|49=123456|56=654321|11=ord-1|55=AAPL|38=10|44=182.50|207=XNAS|10=XYZ\n
```

Where `XYZ` is the zero-padded modulo-256 checksum of the prefix.

### 10. Router behavior

For each received message, the Router must:

1. Read until `\n`.
2. Reject non-ASCII bytes.
3. Parse the `tag=value` fields.
4. Validate the field order and field set against Section 5.
5. Validate required fields and value grammar from Section 8.
6. Validate the checksum in tag `10`.
7. Validate that `49` matches the ID assigned to the sending socket for client-originated messages.
8. Validate the sender type, target type, and message direction.
9. If `56=000000`, handle the message locally as Router-addressed control traffic.
10. Otherwise, look up `56` in the routing table.
11. If valid, forward the message unchanged to the destination socket.

- The Router accepts any number of simultaneous Broker connections on port `5000`.
- The Router accepts any number of simultaneous Market connections on port `5001`.
- Each active connection receives a globally unique 6-digit ID.
- The Router stores at least:
  - `id -> socket`
  - `socket -> id`
  - `id -> client type` (`Broker` or `Market`)
  - `marketId -> market name` for registered Markets
- When a client disconnects, its ID is removed from the routing table.
- When a registered Market disconnects, its registration state is removed before `MARKET_OFFLINE` is broadcast.
- `BUY` and `SELL` must be sent from Broker to Market.
- `EXECUTED` and business `REJECTED` must be sent from Market to Broker.
- `REGISTER_MARKET` must be sent from Market to Router with `56=000000`.
- `ASSIGN`, `MARKET_ONLINE`, `MARKET_OFFLINE`, and Router-generated `REJECTED` originate only from the Router.
- Any sender/target/direction violation, wrong `REJECTED` schema, invalid use of `56=000000`, or second `REGISTER_MARKET` on the same connection is `MALFORMED_MESSAGE`.
- If `56` does not map to an active connection for client-to-client traffic, the Router returns `REJECTED` with `58=UNKNOWN_DESTINATION`.
- For Router-generated `REJECTED`, if the rejected message contained a syntactically valid `11`, the Router must echo that `11`; otherwise it uses the uncorrelated Router `REJECTED` format without `11`.
- When the Router accepts `REGISTER_MARKET`, it stores `<marketId> -> <market>` and broadcasts `MARKET_ONLINE` to every connected Broker.

### 11. Market behavior

- A Market maintains inventory for traded instruments.
- A Market must not act on `BUY` or `SELL` until it has validated the checksum, exact schema, and sender/target IDs of the inbound message.
- On `BUY`:
  - Reject if `55` is not a traded instrument.
  - Reject if `38` is greater than available inventory.
  - On success, decrease inventory by `38` and send `EXECUTED`.
- On `SELL`:
  - Reject if `55` is not a traded instrument.
  - On success, increase inventory by `38` and send `EXECUTED`.
- Market-generated `EXECUTED` and `REJECTED` must echo `11`, `55`, `38`, `44`, and `207` unchanged from the original order, and must swap `49` and `56`.
- Market-generated `REJECTED` is a business response only. It is not used for protocol errors such as bad checksum or malformed structure.

### 12. Broker behavior

- A Broker connects only to Router port `5000` and waits for `ASSIGN` before sending any order.
- A Broker maintains a local map of known Markets from `9001` to `207`, using `MARKET_ONLINE` and `MARKET_OFFLINE` messages.
- A Broker must use the discovered `9001` value in tag `56` when sending `BUY` or `SELL`.
- A Broker must not invent market IDs or route by `207`.
- `207` in a `BUY` or `SELL` message is informational and user-facing only.
- A Broker must handle both correlated and uncorrelated Router `REJECTED` messages.
- If a Router `REJECTED` includes `11`, the Broker correlates it to the matching order. If `11` is absent, the Broker treats it as an uncorrelated protocol error for that connection.

### 13. Reject reasons

Exact `58` values:

Router-generated:
- `BAD_CHECKSUM`
- `UNKNOWN_DESTINATION`
- `MALFORMED_MESSAGE`

Market-generated:
- `UNKNOWN_SYMBOL`
- `INSUFFICIENT_QUANTITY`

No other `58` values are defined by this protocol.

### 14. Error handling

- Bad checksum:
  - The Router does not forward the message.
  - The Router sends `REJECTED` with `58=BAD_CHECKSUM`.

- Unknown destination:
  - The Router does not forward the message.
  - The Router sends `REJECTED` with `58=UNKNOWN_DESTINATION`.

- Malformed message:
  - The Router does not forward the message.
  - The Router sends `REJECTED` with `58=MALFORMED_MESSAGE` if it can still identify the sender socket.
  - If it can also parse a syntactically valid `11`, it uses the correlated Router `REJECTED` format.
  - If the bytes cannot be parsed well enough to build a valid reject, the Router closes the socket.

A message is malformed if any of the following is true:
- unsupported tag
- unsupported `35` value
- duplicate tag
- missing required tag
- wrong field order
- wrong field set for that sender/message-type combination
- invalid numeric format
- non-ASCII bytes
- `49` does not match the ID assigned to the socket
- invalid sender/target type or direction
- invalid use of `56=000000`
- second `REGISTER_MARKET` on the same connection
- `10` is missing or not last

### 15. Market discovery

The Router supports multiple simultaneous Brokers and Markets. Market discovery is Router-driven.

A Market is not discoverable until it registers itself after `ASSIGN`:

```text
8=FIX.4.4|35=REGISTER_MARKET|49=<marketId>|56=000000|207=<market>|10=<chk>\n
```

When the Router accepts `REGISTER_MARKET`, it stores `<marketId> -> <market>` and broadcasts the following message to every connected Broker:

```text
8=FIX.4.4|35=MARKET_ONLINE|49=000000|56=<brokerId>|9001=<marketId>|207=<market>|10=<chk>\n
```

When a registered Market disconnects, the Router broadcasts the following message to every connected Broker:

```text
8=FIX.4.4|35=MARKET_OFFLINE|49=000000|56=<brokerId>|9001=<marketId>|207=<market>|10=<chk>\n
```

When a Broker connects, after `ASSIGN` the Router sends one `MARKET_ONLINE` message for each currently active Market, based on a point-in-time snapshot.

Concurrent `MARKET_ONLINE` / `MARKET_OFFLINE` messages may arrive before, during, or after that snapshot. Brokers must reconcile by `9001`, with the last message for a given `9001` winning.

Multiple Markets may share the same `207` value. `9001` is the authoritative identity and routing key.

### 16. Client-side validation

- Brokers and Markets must validate checksum, field order, required tags, and sender/target IDs on every inbound message before acting on it.
- Brokers accept only `ASSIGN`, `MARKET_ONLINE`, `MARKET_OFFLINE`, `EXECUTED`, and `REJECTED`.
- Markets accept only `ASSIGN` and forwarded `BUY` / `SELL`.
- Unexpected or malformed inbound messages must not be acted upon. The client may log the error and close the socket.
- Clients do not generate protocol-level rejects. Only the Router emits protocol-level `REJECTED`; the Market emits business `REJECTED` for valid orders that fail business rules.
