
CREATE TABLE accounts (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    user_id BIGINT,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    account_type VARCHAR(30) NOT NULL CHECK (account_type IN ('SAVINGS', 'CURRENT', 'FIXED_DEPOSIT', 'RECURRING_DEPOSIT')),
    currency VARCHAR(3) DEFAULT 'INR',
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    available_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    interest_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    overdraft_limit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    branch_code VARCHAR(10),
    nominee_name VARCHAR(100),
    nominee_relation VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED', 'FROZEN', 'SUSPENDED')),
    account_opened_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    account_closed_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0,
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_branch_code (branch_code)
);



INSERT INTO accounts
(account_id, customer_id, user_id, account_number, account_type, currency, balance, available_balance,
 interest_rate, overdraft_limit, branch_code, nominee_name, nominee_relation, status,
 account_opened_date, account_closed_date, created_by, updated_by, created_at, updated_at, version)
VALUES
(1, 'CUST12345', 16, 'SB1000123456', 'SAVINGS', 'INR', 25000.00, 25000.00, 3.75, 10000.00, 'BLR001', 'Suresh Reddy', 'Brother', 'ACTIVE', '2025-10-18 00:34:42', NULL, 'system', NULL, '2025-10-18 00:34:42', '2025-10-18 00:36:45', 0),
(2, 'CUST12345', 16, '710054323333', 'CURRENT', 'INR', 60000.00, 50000.00, 0.00, 0.00, NULL, NULL, NULL, 'ACTIVE', '2025-10-19 10:18:51', NULL, NULL, NULL, '2025-10-19 10:18:51', '2025-10-23 17:31:20', 0),
(3, 'CUST12345', 16, '457854323333', 'FIXED_DEPOSIT', 'INR', 20000.00, 20000.00, 0.00, 0.00, NULL, NULL, NULL, 'ACTIVE', '2025-10-19 10:23:11', NULL, NULL, NULL, '2025-10-19 10:23:11', '2025-10-23 17:31:20', 0),
(4, 'CUST12345', 16, '210054323333', 'CURRENT', 'INR', 60000.00, 60000.00, 0.00, 0.00, NULL, NULL, NULL, 'ACTIVE', '2025-10-19 10:26:32', NULL, NULL, NULL, '2025-10-19 10:26:32', '2025-10-23 17:31:20', 0);

