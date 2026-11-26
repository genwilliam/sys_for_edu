CREATE TABLE schedule_items (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    course VARCHAR(255) NOT NULL,
    class_name VARCHAR(255) NOT NULL,
    teacher VARCHAR(255) NOT NULL,
    room VARCHAR(255) NOT NULL,
    campus VARCHAR(255) NOT NULL,
    weekday VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_schedule_time CHECK (start_time < end_time)
);

CREATE INDEX idx_schedule_weekday_time ON schedule_items (weekday, start_time, end_time);
