-- =================================================================
-- AUTH SERVICE
-- =================================================================

CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    role ENUM('CUSTOMER', 'ADMIN', 'STAFF') DEFAULT 'CUSTOMER',
    status ENUM('ACTIVE', 'LOCKED', 'DISABLED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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

-- =================================================================
-- ACCOUNT SERVICE
-- =================================================================

CREATE TABLE accounts (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    user_id BIGINT,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    account_type VARCHAR(30) NOT NULL CHECK (account_type IN ('SAVINGS', 'CURRENT', 'FIXED_DEPOSIT', 'RECURRING_DEPOSIT')),
    currency VARCHAR(3) DEFAULT 'INR',
    balance DECIMAL(15,2) DEFAULT 0.00,
    available_balance DECIMAL(15,2) DEFAULT 0.00,
    interest_rate DECIMAL(5,2) DEFAULT 0.00,
    overdraft_limit DECIMAL(15,2) DEFAULT 0.00,
    branch_code VARCHAR(10),
    branch_name VARCHAR(100),
    nominee_name VARCHAR(100),
    nominee_relation VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED', 'FROZEN', 'SUSPENDED')),
    account_opened_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    account_closed_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    version BIGINT DEFAULT 0,
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status)
);

-- =================================================================
-- TRANSACTION SERVICE
-- =================================================================

CREATE TABLE transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_type ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'PAYMENT') NOT NULL,
    transaction_mode ENUM('CASH', 'CHEQUE', 'UPI', 'NEFT', 'RTGS', 'IMPS', 'CARD') NOT NULL,
    reference_number VARCHAR(50) UNIQUE,
    status ENUM('SUCCESS', 'FAILED', 'PENDING') DEFAULT 'PENDING',
    description VARCHAR(255),
    -- New counterparty fields for transfers
    counterparty_account VARCHAR(20),
    counterparty_ifsc VARCHAR(11),
    counterparty_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

CREATE TABLE beneficiaries (
    beneficiary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    ifsc_code VARCHAR(11) NOT NULL,
    account_holder_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_beneficiary (customer_id, account_number)
);

-- =================================================================
-- CARDS SERVICE
-- =================================================================

CREATE TABLE cards (
    card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    customer_id VARCHAR(20) NOT NULL, -- Changed from BIGINT
    card_number VARCHAR(16) UNIQUE NOT NULL,
    card_type ENUM('DEBIT', 'CREDIT', 'PREPAID') NOT NULL,
    network ENUM('VISA', 'MASTERCARD', 'RUPAY', 'AMEX') NOT NULL,
    expiry_date DATE NOT NULL,
    cvv VARCHAR(4) NOT NULL,
    pin_hash VARCHAR(255) NOT NULL,
    daily_limit DECIMAL(15,2) DEFAULT 50000.00,
    monthly_limit DECIMAL(15,2) DEFAULT 200000.00,
    status ENUM('ACTIVE', 'BLOCKED', 'EXPIRED', 'LOST', 'STOLEN') DEFAULT 'ACTIVE',
    -- New feature toggles
    contactless_enabled BOOLEAN DEFAULT TRUE,
    ecommerce_enabled BOOLEAN DEFAULT TRUE,
    international_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

-- =================================================================
-- LOAN SERVICE
-- =================================================================

CREATE TABLE loan_applications (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    loan_type VARCHAR(50) NOT NULL,
    amount_requested DECIMAL(15,2) NOT NULL,
    tenure_months_requested INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, UNDER_REVIEW, APPROVED, REJECTED
    credit_score INT,
    application_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE loans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT, -- Link to the original application
    customer_id VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    tenure_months INT NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, APPROVED, REJECTED, ACTIVE, CLOSED
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL,
    FOREIGN KEY (application_id) REFERENCES loan_applications(application_id)
);

CREATE TABLE repayment_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    due_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, PAID, OVERDUE
    FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE
);

CREATE TABLE loan_repayments (
    repayment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    schedule_id BIGINT, -- Optional: link to the specific installment
    amount_paid DECIMAL(15,2) NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    late_fee DECIMAL(10,2) DEFAULT 0.00,
    payment_mode VARCHAR(20),
    FOREIGN KEY (loan_id) REFERENCES loans(id),
    FOREIGN KEY (schedule_id) REFERENCES repayment_schedules(id)
);
