# API 명세서(학기)

현재 학기 자동 전환 + 사물함 신청 기간 설정 흐름 정리입니다. (기능명세 관리자 M7·M9, 학기 초 사물함 오픈 시나리오 참고)

| 파트 | 내용 | 비고 |
|---|---|---|
| 학기 | 학기 날짜는 매년 고정: 1학기 3/1~6/30, 2학기 9/1~12/31 | 운영진이 만들거나 고치지 않음 |
| 학기 | 현재 학기는 날짜로 자동 결정. 1~6월은 그해 1학기, 7~12월은 그해 2학기 | 방학은 다음 학기에 붙임 |
| 학기 | 1/1, 7/1에 다음 학기로 자동 전환. 학기가 DB에 없으면 자동 생성 | 서버 시작 때 + 매일 00:00 |
| 대여 | 방학엔 대여 없음. 수령 날짜가 학기 기간(3/1~, 9/1~) 밖이면 B 쪽에서 막힘 | R004 |
| 사물함 | 학기마다 신청 기간(시작·종료 일시)을 운영진이 정함. 개강 전(방학 중)으로 잡아도 됨 | 미리 신청 → 사용은 학기부터 |
| 공개 | 현재 학기 정보(기간·사물함 신청 기간)는 로그인 없이 조회 | 사물함 화면 안내용 |

## 흐름 전개

### 자동 (서버)
1/1 또는 7/1 자정 → 오늘이 속한 학기 계산 → 없으면 생성 → 이전 현재 학기 해제 → 새 학기를 현재로

### 운영진 (학기 초)
개강 전 사물함 신청 기간 설정 → (B) 슬롯 등록 → 3/1(9/1)부터 대여 시작

### 학생
사물함 화면 진입 → 현재 학기의 신청 기간 확인 → 기간이면 신청

| CLIENT | API | 메서드-URL |
|---|---|---|
| 사물함 화면 진입 (학생, 비로그인 가능) | 현재 학기 조회 | GET /api/semesters/current |
| (관리자) 학기 목록 / 지난 학기 고르기 | 학기 목록 | GET /api/admin/semesters |
| (관리자) "사물함 신청 기간" 저장 | 사물함 신청 기간 설정 | PATCH /api/admin/semesters/current/locker-period |

---

# 내부 흐름 설계 및 명세서

## (자동) 현재 학기 맞추기

1. 누가 호출하나? 서버 스스로 (서버 시작 때 1번 + 매일 00:00, 한국 시각)
2. 무엇을 보내나? : 없음 (오늘 날짜)
3. 서버는 어떤 테이블을 읽고 쓰나? : semester 읽기+쓰기 → **Command**
4. 어떤 경우에 실패하나? : 없음

```
SemesterScheduler (ApplicationRunner + @Scheduled 매일 00:00)
 └ SemesterCommandService.syncCurrent()        @Transactional
     1. 오늘 날짜 → 학기 계산 (SemesterCalendar)
        1~6월 → "2027-1" (3/1~6/30) / 7~12월 → "2027-2" (9/1~12/31)
     2. 그 이름의 학기가 있나? (읽기) → 없으면 생성 (쓰기, 현재 학기 아님)
     3. 지금 현재 학기가 그 학기면 → 끝 (평소엔 여기서 끝남)
     4. 아니면 이전 현재 학기 isCurrent = false → 새 학기 isCurrent = true (쓰기)
        (한 트랜잭션 → 중간에 실패하면 둘 다 취소. 현재 학기가 0개·2개인 상태가 안 생김)
```

## 현재 학기 조회

1. 누가 호출하나? (공개)
2. 무엇을 보내나? : 없음
3. 서버는 어떤 테이블을 읽고 쓰나? : semester 읽기 → **Query**
4. 어떤 경우에 실패하나? : 현재 학기가 없을 때 (서버가 정상이면 생기지 않음)

```
GET /api/semesters/current

SemesterController
 └ SemesterQueryService.getCurrent()           @Transactional(readOnly = true)
     1. is_current = true 인 학기 조회 (읽기)           X 현재 학기 없음 (404 SE001)
 성공(200): { "success": true, "data": { "semesterId": 3, "name": "2027-1",
             "startDate": "2027-03-01", "endDate": "2027-06-30", "current": true,
             "lockerApplyStart": "2027-02-25T10:00:00", "lockerApplyEnd": "2027-03-06T18:00:00" } }
 // 사물함 신청 기간을 아직 안 정했으면 lockerApplyStart·End는 null
```

| 항목 | 내용 |
|---|---|
| 기능 | 현재 학기 조회 |
| 메서드-URL | GET /api/semesters/current |
| 권한 | 공개 |
| Request Body | 없음 |
| Response(성공) | { "success": true, "data": { "semesterId", "name", "startDate", "endDate", "current", "lockerApplyStart", "lockerApplyEnd" } } |
| 처리 흐름 | 현재 학기 조회 → 반환 |
| 사용 테이블 | semester (읽기) |
| 실패 | 404: 현재 학기 없음 (SE001) |

## 학기 목록 (관리자)

1. 누가 호출하나? 관리자 (국장·회장단)
2. 무엇을 보내나? : 없음
3. 서버는 어떤 테이블을 읽고 쓰나? : semester 읽기 → **Query**
4. 어떤 경우에 실패하나? : 토큰 없음, 관리자 아님

```
GET /api/admin/semesters

SemesterQueryService.getAll()                  @Transactional(readOnly = true)
  1. 전체 학기를 시작일 최신순으로 조회 (읽기)
 성공(200): { "success": true, "data": [ { "semesterId": 3, "name": "2027-1", ..., "current": true }, ... ] }
 // 학기는 1년에 2개라 페이지 없이 전부. 대여 목록·CSV에서 semesterId 고르는 용도
```

| 항목 | 내용 |
|---|---|
| 기능 | 학기 목록 (관리자) |
| 메서드-URL | GET /api/admin/semesters |
| 권한 | 관리자 |
| Request Body | 없음 |
| Response(성공) | { "success": true, "data": [ { "semesterId", "name", "startDate", "endDate", "current", "lockerApplyStart", "lockerApplyEnd" } ] } |
| 처리 흐름 | 권한 확인 → 시작일 최신순 조회 |
| 사용 테이블 | semester (읽기) |
| 실패 | 401: 토큰 없음·만료 / 403: 관리자 아님 |

## 사물함 신청 기간 설정 (관리자)

1. 누가 호출하나? 관리자
2. 무엇을 보내나? : 신청 시작·종료 일시
3. 서버는 어떤 테이블을 읽고 쓰나? : semester 읽기+쓰기 → **Command**
4. 어떤 경우에 실패하나? : 입력값 오류, 시작이 종료보다 늦거나 같음, 현재 학기 없음

```
PATCH /api/admin/semesters/current/locker-period
{ "lockerApplyStart": "2027-02-25T10:00:00", "lockerApplyEnd": "2027-03-06T18:00:00" }

SemesterAdminController
 └ @Valid: 두 값 필수 (yyyy-MM-dd'T'HH:mm:ss)                 X 입력값 오류 (400 C001)
 └ SemesterCommandService.changeLockerApplyPeriod(시작, 종료)   @Transactional
     1. 시작 < 종료인가?                                        X 신청 기간 오류 (400 SE002)
     2. 현재 학기 조회 (읽기)                                   X 현재 학기 없음 (404 SE001)
     3. 두 값 저장 (변경 감지로 UPDATE)
 성공(200): { "success": true, "data": { "semesterId": 3 } }
 // 학기 시작 전(방학 중)으로 잡아도 됨 → 미리 신청, 사용은 학기부터
```

| 항목 | 내용 |
|---|---|
| 기능 | 현재 학기 사물함 신청 기간 설정 |
| 메서드-URL | PATCH /api/admin/semesters/current/locker-period |
| 권한 | 관리자 |
| Request Body | { "lockerApplyStart": "2027-02-25T10:00:00", "lockerApplyEnd": "2027-03-06T18:00:00" } |
| Response(성공) | { "success": true, "data": { "semesterId": 3 } } |
| 처리 흐름 | 시작 < 종료 확인 → 현재 학기 조회 → 저장 |
| 사용 테이블 | semester (읽기·쓰기) |
| 실패 | 400: 입력값 오류 (C001), 신청 기간 오류 (SE002) / 401 / 403 / 404: 현재 학기 없음 (SE001) |

---

### 에러 코드

| 코드 | HTTP | 메시지 | 언제 |
|---|---|---|---|
| SE001 | 404 | 현재 학기가 설정되지 않았습니다 | (B가 추가) 현재 학기 없음 |
| SE002 | 400 | 사물함 신청 시작은 종료보다 빨라야 합니다 | 신청 시작 ≥ 종료 |

### 정한 것
- 학기 날짜는 고정(1학기 3/1~6/30, 2학기 9/1~12/31). 학기 생성·수정·전환 API 없음
- 방학은 다음 학기에 붙임 (1~2월 → 1학기, 7~8월 → 2학기). 방학엔 대여 없음, 사물함 미리 신청만 가능
- 학기 날짜 겹침 검사 없음 (자동 생성이라 겹칠 일 없음)
- 사물함 신청 기간은 학기 밖(개강 전)이어도 허용
- 사물함 신청 기간 기본값 없음. 새 학기는 신청 기간이 빈 상태(null)로 생기고, 운영진이 설정하기 전까지 사물함 신청 불가
  - 프론트: `lockerApplyStart`가 null이면 "아직 신청 기간이 정해지지 않았어요" 안내
  - 운영: 학기 초 체크리스트(SC-A3)에 "사물함 신청 기간 설정"을 넣어 누락 방지

### 다른 파트와 연결
- B `CurrentSemesterReader`: TEMP 해제. `SemesterRepository.findByIsCurrentTrue()` 사용
- B `RentalLocalDataInitializer`: 서버 시작 때 학기가 먼저 자동 생성되므로 네이티브 INSERT 경로는 더 이상 타지 않음 (정리는 B 판단)
- 사물함 신청(B 다음 PR): 현재 학기의 `lockerApplyStart ~ lockerApplyEnd` 안인지 확인
