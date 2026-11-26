CREATE TABLE auth_accounts (
    username VARCHAR(80) NOT NULL PRIMARY KEY,
    password_salt VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    account_role VARCHAR(16) NOT NULL,
    person_id VARCHAR(100),
    status VARCHAR(16) NOT NULL,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_auth_account_person UNIQUE (account_role, person_id),
    CONSTRAINT chk_auth_account_role CHECK (account_role IN ('ADMIN', 'TEACHER', 'STUDENT')),
    CONSTRAINT chk_auth_account_status CHECK (status IN ('ACTIVE', 'PENDING', 'DISABLED'))
);

CREATE TABLE auth_sessions (
    token_hash VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_session_account FOREIGN KEY (username) REFERENCES auth_accounts (username)
        ON DELETE CASCADE
);

CREATE INDEX idx_auth_sessions_expiry ON auth_sessions (expires_at);

CREATE TABLE leave_requests (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    request_type VARCHAR(24) NOT NULL,
    requester_role VARCHAR(16) NOT NULL,
    requester_person_id VARCHAR(100) NOT NULL,
    student_id VARCHAR(100),
    teacher_id VARCHAR(100),
    class_id VARCHAR(100),
    leave_date DATE NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reviewed_by VARCHAR(80),
    review_note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_leave_request_type CHECK (request_type IN ('STUDENT_LEAVE', 'TEACHER_LEAVE')),
    CONSTRAINT chk_leave_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_leave_requests_student ON leave_requests (student_id, created_at);
CREATE INDEX idx_leave_requests_teacher ON leave_requests (teacher_id, created_at);
CREATE INDEX idx_leave_requests_status ON leave_requests (status, created_at);
