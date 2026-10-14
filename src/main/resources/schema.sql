CREATE UNIQUE INDEX IF NOT EXISTS uk_rent_item_active ON rent (item_id) WHERE status = 'RENTED';
CREATE UNIQUE INDEX IF NOT EXISTS uk_locker_assignment_student_active ON locker_assignment (student_id, semester_id) WHERE status = 'IN_USE';
CREATE UNIQUE INDEX IF NOT EXISTS uk_locker_assignment_locker_active ON locker_assignment (locker_id, semester_id) WHERE status = 'IN_USE';
