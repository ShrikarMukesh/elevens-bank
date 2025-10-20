CREATE TABLE accounts (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 🔗 Link to Customer Service (String-based ID)
    customer_id VARCHAR(20) NOT NULL,

    -- 🔗 Optional link to Auth Service
    user_id BIGINT,

    -- Account identifiers
    account_number VARCHAR(20) UNIQUE NOT NULL,
    account_type VARCHAR(30) NOT NULL CHECK (account_type IN ('SAVINGS', 'CURRENT', 'FIXED_DEPOSIT', 'RECURRING_DEPOSIT')),
    currency VARCHAR(3) DEFAULT 'INR',

    -- Financial details
    balance DECIMAL(15,2) DEFAULT 0.00,
    available_balance DECIMAL(15,2) DEFAULT 0.00,
    interest_rate DECIMAL(5,2) DEFAULT 0.00,
    overdraft_limit DECIMAL(15,2) DEFAULT 0.00,

    -- Account branch info
    branch_code VARCHAR(10),
    branch_name VARCHAR(100),

    -- Nominee details
    nominee_name VARCHAR(100),
    nominee_relation VARCHAR(50),

    -- Account status
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED', 'FROZEN', 'SUSPENDED')),

    -- Audit fields
    account_opened_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    account_closed_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Metadata / Tracking
    created_by VARCHAR(50),
    updated_by VARCHAR(50),

    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_account_type (account_type),
    INDEX idx_branch_code (branch_code)
);

select * from accounts;

-- Make sure an account exists. Let's assume this creates an account with account_id = 1
INSERT INTO accounts (customer_id, account_number, account_type, balance, available_balance)
VALUES ('CUST12345', '987654321012', 'SAVINGS', 10000.00, 10000.00);

INSERT INTO accounts (customer_id, account_number, account_type, balance, available_balance)
VALUES ('CUST12345', '710054323333', 'CURRENT', 60000.00, 50000.00);

INSERT INTO accounts (customer_id, account_number, account_type, balance, available_balance)
VALUES ('CUST12345', '457854323333', 'FIXED_DEPOSIT', 20000.00, 20000.00);

INSERT INTO accounts (customer_id, account_number, account_type, balance, available_balance)
VALUES ('CUST12345', '311233344444', 'RECURRING_DEPOSIT', 900000.00, 900000.00);