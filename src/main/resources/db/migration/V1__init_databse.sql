CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    keycloak_sub VARCHAR(255) UNIQUE,
    user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('INDIVIDUAL', 'BUSINESS')),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE individuals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    ssn VARCHAR(11) UNIQUE NOT NULL,
    address TEXT,
    government_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE businesses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    business_name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(20) UNIQUE NOT NULL,
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE account_types (
    id BIGSERIAL PRIMARY KEY,
    type_code VARCHAR(20) UNIQUE NOT NULL,
    type_name VARCHAR(100) NOT NULL,
    description TEXT,
    interest_rate DECIMAL(5,4) DEFAULT 0,
    minimum_balance DECIMAL(15,2) DEFAULT 0,
    monthly_fee DECIMAL(10,2) DEFAULT 0
);

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_type_id BIGINT NOT NULL REFERENCES account_types(id),
    routing_number VARCHAR(20) NOT NULL,
    current_balance DECIMAL(15,2) DEFAULT 0,
    available_balance DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'FROZEN', 'CLOSED')),
    opened_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_date TIMESTAMP,
    overdraft_protection BOOLEAN DEFAULT false,
    overdraft_limit DECIMAL(10,2) DEFAULT 0
);

CREATE TABLE transaction_types (
    id BIGSERIAL PRIMARY KEY,
    type_code VARCHAR(20) UNIQUE NOT NULL,
    type_name VARCHAR(100) NOT NULL,
    description TEXT,
    affects_balance BOOLEAN DEFAULT true
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_ref VARCHAR(50) UNIQUE NOT NULL,
    from_account_id BIGINT REFERENCES accounts(id),
    to_account_id BIGINT REFERENCES accounts(id),
    transaction_type_id BIGINT NOT NULL REFERENCES transaction_types(id),
    amount DECIMAL(15,2) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    effective_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount DECIMAL(15,2) NOT NULL,
    running_balance DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transfers (
    id BIGSERIAL PRIMARY KEY,
    from_account_id BIGINT NOT NULL REFERENCES accounts(id),
    to_account_id BIGINT NOT NULL REFERENCES accounts(id),
    amount DECIMAL(15,2) NOT NULL,
    frequency VARCHAR(20) DEFAULT 'ONCE' CHECK (frequency IN ('ONCE', 'DAILY', 'WEEKLY', 'MONTHLY')),
    next_transfer_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    action_type VARCHAR(50) NOT NULL,
    table_name VARCHAR(50),
    record_id BIGINT,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE session_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    session_token VARCHAR(255) NOT NULL,
    ip_address INET,
    user_agent TEXT,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP,
    expired_at TIMESTAMP
);

-- Seed data for account_types
INSERT INTO account_types (type_code, type_name, description, interest_rate, minimum_balance, monthly_fee) VALUES
('CHECKING', 'Checking Account', 'Standard checking account with debit card', 0.0001, 25.00, 0.00),
('SAVINGS', 'Savings Account', 'Interest-bearing savings account', 0.0125, 100.00, 0.00),
('BUSINESS_CHECKING', 'Business Checking', 'Business checking account', 0.0005, 500.00, 15.00),
('BUSINESS_SAVINGS', 'Business Savings', 'Business savings account', 0.0150, 1000.00, 0.00);

-- Seed data for transaction_types
INSERT INTO transaction_types (type_code, type_name, description, affects_balance) VALUES
('DEPOSIT', 'Deposit', 'Funds deposited to account', true),
('WITHDRAWAL', 'Withdrawal', 'Funds withdrawn from account', true),
('TRANSFER', 'Transfer', 'Transfer between accounts', true),
('FEE', 'Fee', 'Account maintenance fee', true),
('INTEREST', 'Interest', 'Interest payment', true);