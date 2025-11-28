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


CREATE TABLE transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_type ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'PAYMENT') NOT NULL,
    transaction_mode ENUM('CASH', 'CHEQUE', 'UPI', 'NEFT', 'RTGS', 'IMPS', 'CARD') NOT NULL,
    reference_number VARCHAR(50),
    status ENUM('SUCCESS', 'FAILED', 'PENDING') DEFAULT 'PENDING',
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

CREATE TABLE cards (
    card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    card_number VARCHAR(16) UNIQUE NOT NULL,
    card_type ENUM('DEBIT', 'CREDIT', 'PREPAID') NOT NULL,
    network ENUM('VISA', 'MASTERCARD', 'RUPAY', 'AMEX') NOT NULL,
    expiry_date DATE NOT NULL,
    cvv VARCHAR(4) NOT NULL,
    pin_hash VARCHAR(255) NOT NULL,
    daily_limit DECIMAL(15,2) DEFAULT 50000.00,
    monthly_limit DECIMAL(15,2) DEFAULT 200000.00,
    status ENUM('ACTIVE', 'BLOCKED', 'EXPIRED', 'LOST', 'STOLEN') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    role ENUM('CUSTOMER', 'ADMIN', 'STAFF') DEFAULT 'CUSTOMER',
    status ENUM('ACTIVE', 'LOCKED', 'DISABLED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE sessions (
    session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    access_token VARCHAR(500) UNIQUE NOT NULL,
    refresh_token VARCHAR(500) UNIQUE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    is_revoked BOOLEAN DEFAULT FALSE,
    expiry_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

/*
Redis:
- Store active JWT/session tokens
- Key = token, Value = { userId, expiry }
- TTL auto-handled
*/

-- --------------------------------------------------------
-- Loan Service Tables
-- --------------------------------------------------------

CREATE TABLE loans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    tenure_months INT NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, APPROVED, REJECTED, ACTIVE, CLOSED
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL
);

CREATE TABLE repayment_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    due_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, PAID, OVERDUE
    FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE
);

-- --------------------------------------------------------
-- Dummy Data for Loans
-- --------------------------------------------------------

INSERT INTO loans (customer_id, amount, interest_rate, tenure_months, status, applied_at, approved_at) VALUES
('CUST001', 50000.00, 10.50, 12, 'ACTIVE', NOW(), NOW()),
('CUST002', 100000.00, 9.00, 24, 'PENDING', NOW(), NULL),
('CUST003', 25000.00, 12.00, 6, 'CLOSED', DATE_SUB(NOW(), INTERVAL 1 YEAR), DATE_SUB(NOW(), INTERVAL 1 YEAR)),
('CUST004', 75000.00, 11.00, 18, 'APPROVED', NOW(), NOW()),
('CUST005', 200000.00, 8.50, 36, 'REJECTED', NOW(), NULL),
('CUST006', 15000.00, 15.00, 3, 'ACTIVE', NOW(), NOW()),
('CUST007', 300000.00, 8.00, 48, 'PENDING', NOW(), NULL),
('CUST008', 40000.00, 10.00, 12, 'ACTIVE', NOW(), NOW()),
('CUST009', 60000.00, 11.50, 12, 'CLOSED', DATE_SUB(NOW(), INTERVAL 2 YEAR), DATE_SUB(NOW(), INTERVAL 2 YEAR)),
('CUST010', 120000.00, 9.50, 24, 'APPROVED', NOW(), NOW());

-- --------------------------------------------------------
-- Dummy Data for Repayment Schedules (Linked to Loans)
-- --------------------------------------------------------

INSERT INTO repayment_schedules (loan_id, due_date, amount, status) VALUES
(1, DATE_ADD(CURRENT_DATE, INTERVAL 1 MONTH), 4500.00, 'PENDING'),
(1, DATE_ADD(CURRENT_DATE, INTERVAL 2 MONTH), 4500.00, 'PENDING'),
(3, DATE_SUB(CURRENT_DATE, INTERVAL 6 MONTH), 4300.00, 'PAID'),
(3, DATE_SUB(CURRENT_DATE, INTERVAL 5 MONTH), 4300.00, 'PAID'),
(4, DATE_ADD(CURRENT_DATE, INTERVAL 1 MONTH), 4600.00, 'PENDING'),
(6, DATE_ADD(CURRENT_DATE, INTERVAL 1 MONTH), 5100.00, 'PENDING'),
(6, DATE_ADD(CURRENT_DATE, INTERVAL 2 MONTH), 5100.00, 'PENDING'),
(6, DATE_ADD(CURRENT_DATE, INTERVAL 3 MONTH), 5100.00, 'PENDING'),
(8, DATE_ADD(CURRENT_DATE, INTERVAL 1 MONTH), 3600.00, 'PENDING'),
(9, DATE_SUB(CURRENT_DATE, INTERVAL 12 MONTH), 5400.00, 'PAID');



