USE [fox];
GO

CREATE TABLE dbo.roles (
    id UNIQUEIDENTIFIER NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE dbo.users (
    id UNIQUEIDENTIFIER NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active BIT NOT NULL,
    email_verified BIT NOT NULL,
    last_login_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    deleted_at DATETIME2 NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE dbo.user_roles (
    user_id UNIQUEIDENTIFIER NOT NULL,
    role_id UNIQUEIDENTIFIER NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id) REFERENCES dbo.roles(id) ON DELETE CASCADE
);

CREATE TABLE dbo.refresh_tokens (
    id UNIQUEIDENTIFIER NOT NULL,
    user_id UNIQUEIDENTIFIER NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    revoked BIT NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_users FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

CREATE TABLE dbo.password_reset_tokens (
    id UNIQUEIDENTIFIER NOT NULL,
    user_id UNIQUEIDENTIFIER NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    used BIT NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_password_reset_tokens_users FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

CREATE TABLE dbo.login_history (
    id UNIQUEIDENTIFIER NOT NULL,
    user_id UNIQUEIDENTIFIER NULL,
    ip_address VARCHAR(255) NULL,
    user_agent VARCHAR(1000) NULL,
    success BIT NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT pk_login_history PRIMARY KEY (id),
    CONSTRAINT fk_login_history_users FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

CREATE TABLE dbo.process_stage (
    id UNIQUEIDENTIFIER NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT pk_process_stage PRIMARY KEY (id),
    CONSTRAINT uq_process_stage_code UNIQUE (code)
);

CREATE TABLE dbo.process_reason (
    id UNIQUEIDENTIFIER NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT pk_process_reason PRIMARY KEY (id),
    CONSTRAINT uq_process_reason_code UNIQUE (code)
);

CREATE TABLE dbo.[process] (
    id UNIQUEIDENTIFIER NOT NULL,
    process_number VARCHAR(30) NOT NULL,
    operation_type VARCHAR(100) NULL,
    client_name VARCHAR(150) NULL,
    insurance_company VARCHAR(150) NULL,
    dealership_name VARCHAR(150) NULL,
    current_stage_id UNIQUEIDENTIFIER NULL,
    current_reason_id UNIQUEIDENTIFIER NULL,
    insurance_claim_number VARCHAR(100) NULL,
    chassis VARCHAR(100) NULL,
    invoice_number VARCHAR(100) NULL,
    oss_number VARCHAR(100) NULL,
    carrier_name VARCHAR(150) NULL,
    occurrence_date DATE NULL,
    claim_date DATE NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(2) NULL,
    estimated_value DECIMAL(18, 2) NULL,
    description VARCHAR(MAX) NULL,
    status VARCHAR(50) NOT NULL,
    created_by UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    CONSTRAINT pk_process PRIMARY KEY (id),
    CONSTRAINT uq_process_number UNIQUE (process_number),
    CONSTRAINT fk_process_stage FOREIGN KEY (current_stage_id) REFERENCES dbo.process_stage(id),
    CONSTRAINT fk_process_reason FOREIGN KEY (current_reason_id) REFERENCES dbo.process_reason(id),
    CONSTRAINT fk_process_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id)
);

CREATE TABLE dbo.process_history (
    id UNIQUEIDENTIFIER NOT NULL,
    process_id UNIQUEIDENTIFIER NOT NULL,
    type VARCHAR(50) NOT NULL,
    message VARCHAR(MAX) NOT NULL,
    created_by UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT pk_process_history PRIMARY KEY (id),
    CONSTRAINT fk_process_history_process FOREIGN KEY (process_id) REFERENCES dbo.[process](id) ON DELETE CASCADE,
    CONSTRAINT fk_process_history_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id)
);

CREATE TABLE dbo.process_inspection (
    id UNIQUEIDENTIFIER NOT NULL,
    process_id UNIQUEIDENTIFIER NOT NULL,
    provider_name VARCHAR(150) NULL,
    inspection_status VARCHAR(50) NULL,
    scheduled_date DATE NULL,
    completed_date DATE NULL,
    report VARCHAR(MAX) NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    CONSTRAINT pk_process_inspection PRIMARY KEY (id),
    CONSTRAINT uq_process_inspection_process UNIQUE (process_id),
    CONSTRAINT fk_process_inspection_process FOREIGN KEY (process_id) REFERENCES dbo.[process](id) ON DELETE CASCADE
);

CREATE TABLE dbo.process_payment (
    id UNIQUEIDENTIFIER NOT NULL,
    process_id UNIQUEIDENTIFIER NOT NULL,
    estimated_value DECIMAL(18, 2) NULL,
    deductible_value DECIMAL(18, 2) NULL,
    payment_status VARCHAR(50) NULL,
    closure_status VARCHAR(50) NULL,
    paid_amount DECIMAL(18, 2) NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    CONSTRAINT pk_process_payment PRIMARY KEY (id),
    CONSTRAINT uq_process_payment_process UNIQUE (process_id),
    CONSTRAINT fk_process_payment_process FOREIGN KEY (process_id) REFERENCES dbo.[process](id) ON DELETE CASCADE
);

CREATE TABLE dbo.process_document (
    id UNIQUEIDENTIFIER NOT NULL,
    process_id UNIQUEIDENTIFIER NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NULL,
    status VARCHAR(50) NOT NULL,
    uploaded_by UNIQUEIDENTIFIER NULL,
    uploaded_at DATETIME2 NOT NULL,
    CONSTRAINT pk_process_document PRIMARY KEY (id),
    CONSTRAINT fk_process_document_process FOREIGN KEY (process_id) REFERENCES dbo.[process](id) ON DELETE CASCADE,
    CONSTRAINT fk_process_document_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES dbo.users(id)
);
GO

CREATE INDEX ix_process_status ON dbo.[process](status);
CREATE INDEX ix_process_created_at ON dbo.[process](created_at);
CREATE INDEX ix_process_history_process_created ON dbo.process_history(process_id, created_at);
CREATE INDEX ix_login_history_user_created ON dbo.login_history(user_id, created_at);
GO