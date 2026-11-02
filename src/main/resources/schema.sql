-- TEMP(담당: 팀): 이 파일은 local 프로필에서만 실행된다 (application.yaml의 spring.sql.init.mode). prod 적용 방법(Flyway 등)은 미정 / 정해지면 그쪽 마이그레이션으로 옮긴다
CREATE UNIQUE INDEX IF NOT EXISTS uk_rent_item_active ON rent (item_id) WHERE status IN ('RESERVED', 'RENTED');
-- TEMP(담당: A): 사물함 부분 인덱스. LockerAssignment 엔티티의 컬럼·상태값(IN_USE)과 조건이 맞는지 A 확인 필요 / 사물함 API 구현 때 A가 확정
CREATE UNIQUE INDEX IF NOT EXISTS uk_locker_assignment_student_active ON locker_assignment (student_id, semester_id) WHERE status = 'IN_USE';
CREATE UNIQUE INDEX IF NOT EXISTS uk_locker_assignment_locker_active ON locker_assignment (locker_id, semester_id) WHERE status = 'IN_USE';
