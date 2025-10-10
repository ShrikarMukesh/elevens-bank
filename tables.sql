CREATE TABLE accounts (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,   -- Reference to Customer Service
    account_number VARCHAR(20) UNIQUE NOT NULL,
    account_type ENUM('SAVINGS', 'CURRENT', 'FIXED_DEPOSIT') NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    currency VARCHAR(3) DEFAULT 'INR',
    branch_code VARCHAR(10),
    interest_rate DECIMAL(5,2) DEFAULT 0.00,
    overdraft_limit DECIMAL(15,2) DEFAULT 0.00,
    nominee_name VARCHAR(100),
    nominee_relation VARCHAR(50),
    status ENUM('ACTIVE', 'INACTIVE', 'CLOSED', 'FROZEN') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL
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



