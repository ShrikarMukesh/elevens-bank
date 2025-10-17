

| Feature                            | Benefit                            |
| ---------------------------------- | ---------------------------------- |
| Add retry count column             | Prevent infinite retries           |
| Add exponential backoff logic      | Smarter retries                    |
| Add Kafka transactional producer   | Atomicity with DB                  |
| Add DLT (Dead Letter Topic)        | Move permanent failures to `*.DLT` |
| Expose `/actuator/outbox` endpoint | Monitor pending events             |


| Step | Action              | Result                            |
| ---- | ------------------- | --------------------------------- |
| 1️⃣  | User registers      | Saved in DB                       |
| 2️⃣  | Event created       | Saved in `event_outbox`           |
| 3️⃣  | Kafka publish fails | Marked as FAILED                  |
| 4️⃣  | Scheduler retries   | Publishes to Kafka when available |
| 5️⃣  | Event marked SENT   | ✅ Reliable delivery               |



| Behavior                                 | Status                |
| ---------------------------------------- | --------------------- |
| Scheduler executes every 30 seconds      | ✅ Confirmed           |
| Hibernate `SELECT` query appears in logs | ✅ Confirmed           |
| Failed events are retried                | ✅ Works automatically |
| Retries stop once status = SENT          | ✅ Expected behavior   |
