┌────────────────┐
│ Account Service │───┐
└────────────────┘   │
┌────────────────┐   │
│ Transaction Svc │──┼──► Kafka Topic (bank.events)
└────────────────┘   │
┌────────────────┐   │
│ Card Service    │──┘
└────────────────┘
│
▼
┌───────────────────────────┐
│ Notification Service      │
│  - Subscribes to Kafka    │
│  - Parses event payload   │
│  - Selects template       │
│  - Sends via Email/SMS    │
│  - Stores in MongoDB      │
└───────────────────────────┘
