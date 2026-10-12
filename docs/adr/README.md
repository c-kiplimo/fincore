# Architecture decision records

| # | Decision | Status |
|---|---|---|
| 0001 | [Seven business services, nine bounded contexts](0001-seven-business-services-nine-bounded-contexts.md) | Accepted |
| 0002 | [The ledger is the only writer of balances](0002-the-ledger-is-the-only-writer-of-balances.md) | Accepted |
| 0003 | [gRPC for ledger postings, REST with an envelope elsewhere](0003-grpc-for-ledger-postings-rest-with-an-envelope-elsewhere.md) | Accepted |
| 0004 | [JdbcTemplate instead of JPA](0004-jdbctemplate-instead-of-jpa.md) | Accepted |
| 0005 | [Money as integer minor units](0005-money-as-integer-minor-units.md) | Accepted |
| 0006 | [Transactional outbox for events](0006-transactional-outbox-for-events.md) | Accepted |
| 0007 | [Sagas instead of distributed transactions](0007-sagas-instead-of-distributed-transactions.md) | Accepted |
| 0008 | [Tax and fee rules as versioned data](0008-tax-and-fee-rules-as-versioned-data.md) | Accepted |
| 0009 | [AI may explain, never post](0009-ai-may-explain-never-post.md) | Accepted |

Format: context, decision, consequences (Michael Nygard's ADR template).
