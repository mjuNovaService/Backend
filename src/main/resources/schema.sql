-- TEMP(담당: 팀): 이 파일은 local 프로필에서만 실행된다 (application.yaml의 spring.sql.init.mode). prod 적용 방법(Flyway 등)은 미정 / 정해지면 그쪽 마이그레이션으로 옮긴다
CREATE UNIQUE INDEX IF NOT EXISTS uk_rent_item_active ON rent (item_id) WHERE status IN ('RESERVED', 'RENTED');
-- 사물함: 학생당·사물함당 학기마다 사용 중(IN_USE) 배정 1건. 이름으로 LC005(학생)·LC004(사물함)를 구분하므로 바꾸면 LockerCommandService도 같이 바꾼다
CREATE UNIQUE INDEX IF NOT EXISTS uk_locker_assignment_student_active ON locker_assignment (student_id, semester_id) WHERE status = 'IN_USE';
CREATE UNIQUE INDEX IF NOT EXISTS uk_locker_assignment_locker_active ON locker_assignment (locker_id, semester_id) WHERE status = 'IN_USE';
