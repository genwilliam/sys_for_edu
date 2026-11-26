CREATE TABLE schedule_item_students (
    schedule_item_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (schedule_item_id, student_id),
    CONSTRAINT fk_schedule_item_students_item
        FOREIGN KEY (schedule_item_id) REFERENCES schedule_items (id) ON DELETE CASCADE
);

CREATE INDEX idx_schedule_item_students_student ON schedule_item_students (student_id);
