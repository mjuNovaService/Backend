# API 명세서(학생회 정보·링크·Q&A 파트)

정리 파일: 엑셀(Nova_council_API_명세서_v1.xlsx)보다 가볍게 흐름 파악용으로 아래 내용을 읽으시면 됩니다.

| 파트 | 내용 | 비고 |
|---|---|---|
| 학생회 소개 | 소개·공약·조직도·회칙·FAQ를 종류(type)별로 보여준다 | 누구나 조회, 수정은 관리자만 |
| 학생회 소개 | 관리자가 글을 쓰고 고치고 지운다. 마지막으로 고친 사람이 기록된다 | updated_by |
| 학생회 소개 | 소개·조직도·회칙은 글 1개만, 공약·FAQ는 여러 개 | 이미 있으면 409 IN002 |
| 바로가기 링크 | 인스타·카톡 오픈채팅 같은 외부 링크 목록 | 링크 이름은 중복 불가 |
| 바로가기 링크 | 링크 이름은 프론트가 버튼을 찾는 식별자라 수정 불가, 주소만 수정 | 이름 바꾸려면 삭제 후 재등록 |
| Q&A | 로그인한 학생이 질문을 남기고, 내 질문만 볼 수 있다 | 남의 질문은 못 봄 |
| Q&A | 관리자가 전체 질문을 보고 답변을 단다. 질문 하나에 답변 하나 | Answer가 Question의 PK를 같이 씀(@MapsId) |
| Q&A | 1:1 문의: 학생은 본인 질문만, 관리자는 전체 | 공개 게시판 아님 |
| Q&A | 답변 달리기 전까지만 학생이 질문을 수정·삭제할 수 있다 | 답변 후 409 |
| 공통 | 내용 길이 제한: 질문·답변 5,000자, 소개 글 20,000자 | 넘으면 400 |

## 흐름 전개

### 학생회 소개
메인/소개 탭 진입 → 종류 선택(소개·공약·조직도·회칙·FAQ) → 해당 글 목록 보기

### 바로가기 링크
푸터/메뉴 진입 → 링크 목록 → 클릭 시 외부 페이지 이동(프론트 처리)

### Q&A (학생)
질문하기 → 제목·내용 입력 → 등록 → 내 질문 목록 → 상세(답변 있으면 같이 보임) → 답변 전이면 수정·삭제 가능

### Q&A (관리자)
전체 질문 목록(답변 여부로 거르기) → 상세 → 답변 작성 → 필요하면 답변 수정

### 관리자 콘텐츠 관리
소개 글/링크 추가 → 수정 → 삭제

| CLIENT | API | 메서드-URL |
|---|---|---|
| 소개 탭에서 종류 선택 | 소개 글 조회 | GET /api/infos?type=PLEDGE |
| (관리자) "글 추가" 버튼 | 소개 글 등록 | POST /api/admin/infos |
| (관리자) "수정" 버튼 | 소개 글 수정 | PATCH /api/admin/infos/{infoId} |
| (관리자) "삭제" 버튼 | 소개 글 삭제 | DELETE /api/admin/infos/{infoId} |
| 링크 영역 노출 | 링크 목록 조회 | GET /api/links |
| (관리자) "링크 추가" 버튼 | 링크 등록 | POST /api/admin/links |
| (관리자) "주소 수정" 버튼 | 링크 주소 수정 | PATCH /api/admin/links/{linkId} |
| (관리자) "삭제" 버튼 | 링크 삭제 | DELETE /api/admin/links/{linkId} |
| "질문하기" 버튼 | 질문 등록 | POST /api/questions |
| 내 질문 탭 | 내 질문 목록 | GET /api/questions/me |
| 질문 클릭 | 내 질문 상세 | GET /api/questions/{questionId} |
| "수정" 버튼 | 내 질문 수정 | PATCH /api/questions/{questionId} |
| "삭제" 버튼 | 내 질문 삭제 | DELETE /api/questions/{questionId} |
| (관리자) Q&A 관리 탭 | 전체 질문 목록 | GET /api/admin/questions?answered=false |
| (관리자) 질문 클릭 | 질문 상세 | GET /api/admin/questions/{questionId} |
| (관리자) "답변 등록" 버튼 | 답변 등록 | POST /api/admin/questions/{questionId}/answer |
| (관리자) "답변 수정" 버튼 | 답변 수정 | PATCH /api/admin/questions/{questionId}/answer |

---

# 내부 흐름 설계 및 명세서

## 소개 글 조회

1. 누가 호출하나? (공개)
2. 무엇을 보내나? : 종류(type) 쿼리 파라미터
3. 서버는 어떤 테이블을 읽고 쓰나? : info 읽기 → **Query**
4. 어떤 경우에 실패하나? : type 값이 정해진 다섯 개가 아닐 때

```
GET /api/infos?type=PLEDGE

InfoController
 └ type → InfoType enum 변환                         X 잘못된 type (400)
 └ InfoQueryService.getInfos(type)          @Transactional(readOnly = true)
     1. info에서 type이 같은 글 조회 (읽기)
     2. 작성 순서(info_id 오름차순) 정렬 → DTO로 변환
 성공(200): { "success": true, "data": [ { "infoId": 1, "type": "PLEDGE", "title": "공약 1", "content": "...", "updatedAt": "..." } ] }
 // 글이 없으면 빈 배열 [] (에러 아님)
```

**명세서**

| 항목 | 내용 |
|---|---|
| 기능 | 학생회 소개 글 종류별 조회 |
| 메서드-URL | GET /api/infos?type={INTRO\|PLEDGE\|ORG_CHART\|RULES\|FAQ} |
| 권한 | 공개 |
| Request Body | 없음 (쿼리 파라미터 type) |
| Response(성공) | { "success": true, "data": [ { "infoId", "type", "title", "content", "updatedAt" } ] } |
| 처리 흐름 | type 변환 → 종류별 조회 → 작성순 정렬 → 반환 |
| 사용 테이블 | info (읽기) |
| 실패 | 400: 잘못된 type 값 |

## 소개 글 등록

1. 누가 호출하나? 관리자(학생회) — Access Token 필요
2. 무엇을 보내나? : 종류, 제목, 내용
3. 서버는 어떤 테이블을 읽고 쓰나? : student 읽기, info 쓰기 → **Command**
4. 어떤 경우에 실패하나? : 입력값 오류, 토큰 없음, 관리자 아님, 1개짜리 종류에 이미 글 있음

```
POST /api/admin/infos   { "type": "PLEDGE", "title": "공약 1", "content": "..." }
Authorization: Bearer eyJ...

JwtAuthenticationFilter → 토큰 확인                    X 토큰 없음·만료 (401 A101)
SecurityConfig → /api/admin/** 은 국장·회장단만        X 관리자 아님 (403 A102)
InfoController
 └ @Valid: type 필수, title 1~200자, content 1~20,000자  X 입력값 오류 (400)
 └ @AuthenticationPrincipal 로 로그인한 학번 꺼내기
 └ InfoCommandService.create(학번, 요청)        @Transactional
     1. INTRO·ORG_CHART·RULES면 → 이미 글이 있나? (읽기)   X 1개만 가능 (409 IN002)
     2. 학번으로 student 조회 (읽기)                     X 회원 없음 (404)
     3. Info 생성 (updatedBy = 이 관리자)
     4. info 저장 (쓰기)
 성공(201): { "success": true, "data": { "infoId": 7 } }
```

**명세서**

| 항목 | 내용 |
|---|---|
| 기능 | 학생회 소개 글 등록 |
| 메서드-URL | POST /api/admin/infos |
| 권한 | 관리자 |
| Request Body | { "type": "PLEDGE", "title": "공약 1", "content": "..." } |
| Response(성공) | 201 { "success": true, "data": { "infoId": 7 } } |
| 처리 흐름 | 토큰·권한 확인 → 입력 검사 → (1개짜리 종류면) 기존 글 확인 → 작성자 조회 → 저장 |
| 사용 테이블 | student (읽기), info (쓰기) |
| 실패 | 400: 입력값 오류 / 401: 토큰 없음·만료 / 403: 관리자 아님 / 404: 회원 없음 / 409: 1개짜리 종류에 이미 글 있음 |

## 소개 글 수정

1. 누가 호출하나? 관리자
2. 무엇을 보내나? : 바꿀 필드만 (type, title, content 중 일부)
3. 서버는 어떤 테이블을 읽고 쓰나? : student 읽기, info 읽기+쓰기 → **Command**
4. 어떤 경우에 실패하나? : 입력값 오류, 글 없음, 권한 없음, 1개짜리 종류로 옮기는데 이미 글 있음

```
PATCH /api/admin/infos/7   { "title": "공약 1 (수정)" }

InfoController
 └ @Valid: 보낸 값만 길이 검사                            X 입력값 오류 (400)
 └ InfoCommandService.update(학번, infoId, 요청)  @Transactional
     1. infoId로 info 조회 (읽기)                        X 글 없음 (404)
     2. type을 다른 1개짜리 종류로 바꾸면 → 거기 이미 글 있나?   X 1개만 가능 (409 IN002)
     3. 학번으로 student 조회 (읽기)
     4. 보낸 필드만 바꾸고 updatedBy = 이 관리자
        (save 호출 없이 트랜잭션 끝날 때 변경 감지로 UPDATE)
 성공(200): { "success": true, "data": { "infoId": 7 } }
```

**명세서**

| 항목 | 내용 |
|---|---|
| 기능 | 학생회 소개 글 수정 |
| 메서드-URL | PATCH /api/admin/infos/{infoId} |
| 권한 | 관리자 |
| Request Body | { "type"?, "title"?, "content"? } (보낸 것만 수정) |
| Response(성공) | { "success": true, "data": { "infoId": 7 } } |
| 처리 흐름 | 글 조회 → (종류 바꾸면) 1개 규칙 확인 → 수정자 조회 → 필드 변경 → 변경 감지로 저장 |
| 사용 테이블 | student (읽기), info (읽기·쓰기) |
| 실패 | 400: 입력값 오류 / 401 / 403 / 404: 글 없음 / 409: 1개짜리 종류에 이미 글 있음 |

## 소개 글 삭제

1. 누가 호출하나? 관리자
2. 무엇을 보내나? : URL의 infoId
3. 서버는 어떤 테이블을 읽고 쓰나? : info 읽기+삭제 → **Command**
4. 어떤 경우에 실패하나? : 글 없음, 권한 없음

```
DELETE /api/admin/infos/7

InfoCommandService.delete(infoId)          @Transactional
  1. infoId로 info 조회 (읽기)                          X 글 없음 (404)
  2. info 삭제 (쓰기)
 성공(200): { "success": true }
```

| 항목 | 내용 |
|---|---|
| 기능 | 학생회 소개 글 삭제 |
| 메서드-URL | DELETE /api/admin/infos/{infoId} |
| 권한 | 관리자 |
| Request Body | 없음 |
| Response(성공) | { "success": true } |
| 처리 흐름 | 글 조회 → 삭제 |
| 사용 테이블 | info (읽기·쓰기) |
| 실패 | 401 / 403 / 404: 글 없음 |

## 링크 목록 조회

1. 누가 호출하나? (공개)
2. 무엇을 보내나? : 없음
3. 서버는 어떤 테이블을 읽고 쓰나? : link 읽기 → **Query**
4. 어떤 경우에 실패하나? : 없음 (비어 있으면 빈 배열)

```
GET /api/links

LinkQueryService.getLinks()                @Transactional(readOnly = true)
  1. link 전체 조회 (읽기) → DTO 변환
 성공(200): { "success": true, "data": [ { "linkId": 1, "linkName": "인스타그램", "url": "https://..." } ] }
```

| 항목 | 내용 |
|---|---|
| 기능 | 바로가기 링크 목록 조회 |
| 메서드-URL | GET /api/links |
| 권한 | 공개 |
| Request Body | 없음 |
| Response(성공) | { "success": true, "data": [ { "linkId", "linkName", "url" } ] } |
| 처리 흐름 | 전체 조회 → 반환 |
| 사용 테이블 | link (읽기) |
| 실패 | 없음 |

## 링크 등록

1. 누가 호출하나? 관리자
2. 무엇을 보내나? : 링크 이름, URL
3. 서버는 어떤 테이블을 읽고 쓰나? : link 읽기+쓰기 → **Command**
4. 어떤 경우에 실패하나? : 입력값 오류, 이미 있는 링크 이름

```
POST /api/admin/links   { "linkName": "인스타그램", "url": "https://instagram.com/..." }

LinkController
 └ @Valid: linkName 1~50자, url 필수·500자 이하·http(s) 형식   X 입력값 오류 (400)
 └ LinkCommandService.create(요청)          @Transactional
     1. 같은 linkName 있나? (읽기)                       X 이름 중복 (409)
     2. link 저장 (쓰기)
        (동시에 두 번 들어오면 DB UNIQUE 제약 uk_link_link_name 이 막음 → 409)
 성공(201): { "success": true, "data": { "linkId": 3 } }
```

| 항목 | 내용 |
|---|---|
| 기능 | 바로가기 링크 등록 |
| 메서드-URL | POST /api/admin/links |
| 권한 | 관리자 |
| Request Body | { "linkName": "인스타그램", "url": "https://..." } |
| Response(성공) | 201 { "success": true, "data": { "linkId": 3 } } |
| 처리 흐름 | 입력 검사 → 이름 중복 확인 → 저장 |
| 사용 테이블 | link (읽기·쓰기) |
| 실패 | 400: 입력값 오류 / 401 / 403 / 409: 링크 이름 중복 |

## 링크 주소 수정

1. 누가 호출하나? 관리자
2. 무엇을 보내나? : 새 url (linkName은 못 바꿈)
3. 서버는 어떤 테이블을 읽고 쓰나? : link 읽기+쓰기 → **Command**
4. 어떤 경우에 실패하나? : 입력값 오류, 링크 없음

```
PATCH /api/admin/links/3   { "url": "https://instagram.com/new" }

LinkAdminController
 └ @Valid: url 필수·500자 이하·http(s) 형식               X 입력값 오류 (400)
 └ LinkCommandService.update(linkId, url)   @Transactional
     1. linkId로 link 조회 (읽기)                        X 링크 없음 (404)
     2. url만 변경 (변경 감지로 UPDATE)
 성공(200): { "success": true, "data": { "linkId": 3 } }
 // linkName은 프론트가 버튼을 찾는 식별자라 바꾸지 않는다. 이름을 바꾸려면 삭제 후 다시 등록
```

| 항목 | 내용 |
|---|---|
| 기능 | 바로가기 링크 주소 수정 |
| 메서드-URL | PATCH /api/admin/links/{linkId} |
| 권한 | 관리자 |
| Request Body | { "url": "https://..." } |
| Response(성공) | { "success": true, "data": { "linkId": 3 } } |
| 처리 흐름 | 링크 조회 → url 변경 |
| 사용 테이블 | link (읽기·쓰기) |
| 실패 | 400: 입력값 오류 / 401 / 403 / 404: 링크 없음 |

## 링크 삭제

1. 누가 호출하나? 관리자
2. 무엇을 보내나? : URL의 linkId
3. 서버는 어떤 테이블을 읽고 쓰나? : link 읽기+삭제 → **Command**
4. 어떤 경우에 실패하나? : 링크 없음

```
DELETE /api/admin/links/3

LinkCommandService.delete(linkId)          @Transactional
  1. linkId로 link 조회 (읽기)                          X 링크 없음 (404)
  2. link 삭제 (쓰기)
 성공(200): { "success": true }
```

| 항목 | 내용 |
|---|---|
| 기능 | 바로가기 링크 삭제 |
| 메서드-URL | DELETE /api/admin/links/{linkId} |
| 권한 | 관리자 |
| Request Body | 없음 |
| Response(성공) | { "success": true } |
| 처리 흐름 | 링크 조회 → 삭제 |
| 사용 테이블 | link (읽기·쓰기) |
| 실패 | 401 / 403 / 404: 링크 없음 |

## 질문 등록

1. 누가 호출하나? 로그인한 학생 (Access Token 필요)
2. 무엇을 보내나? : 제목, 내용
3. 서버는 어떤 테이블을 읽고 쓰나? : student 읽기, question 쓰기 → **Command**
4. 어떤 경우에 실패하나? : 입력값 오류, 토큰 없음, 회원 없음

```
POST /api/questions   { "title": "사물함 신청 언제 하나요?", "content": "..." }
Authorization: Bearer eyJ...

JwtAuthenticationFilter → 토큰 확인                    X 토큰 없음·만료 (401 A101)
QuestionController
 └ @Valid: title 1~200자, content 1~5,000자               X 입력값 오류 (400)
 └ @AuthenticationPrincipal 로 로그인한 학번 꺼내기
 └ QuestionCommandService.create(학번, 요청)    @Transactional
     1. 학번으로 student 조회 (읽기)                     X 회원 없음 (404)
     2. Question 생성 (작성자 = 이 학생)
     3. question 저장 (쓰기)
 성공(201): { "success": true, "data": { "questionId": 12 } }
```

| 항목 | 내용 |
|---|---|
| 기능 | Q&A 질문 등록 |
| 메서드-URL | POST /api/questions |
| 권한 | 로그인한 학생 |
| Request Body | { "title": "...", "content": "..." } |
| Response(성공) | 201 { "success": true, "data": { "questionId": 12 } } |
| 처리 흐름 | 토큰 확인 → 입력 검사 → 작성자 조회 → 저장 |
| 사용 테이블 | student (읽기), question (쓰기) |
| 실패 | 400: 입력값 오류 / 401: 토큰 없음·만료 / 404: 회원 없음 |

## 내 질문 목록

1. 누가 호출하나? 로그인한 학생
2. 무엇을 보내나? : Access Token (+ page, size)
3. 서버는 어떤 테이블을 읽고 쓰나? : question, answer 읽기 → **Query**
4. 어떤 경우에 실패하나? : 토큰 없음·만료

```
GET /api/questions/me?page=0&size=10
Authorization: Bearer eyJ...

QuestionQueryService.getMyQuestions(학번, page)   @Transactional(readOnly = true)
  1. question에서 작성자 학번 = 나 인 것만 최신순 조회 (읽기)
  2. 각 질문에 답변이 있는지(answered) 같이 계산 (answer 읽기)
 성공(200): { "success": true, "data": { "content": [ { "questionId": 12, "title": "...", "answered": false, "createdAt": "..." } ], "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 } }
```

| 항목 | 내용 |
|---|---|
| 기능 | 내가 쓴 질문 목록 |
| 메서드-URL | GET /api/questions/me?page=&size= |
| 권한 | 로그인한 학생 |
| Request Body | 없음 |
| Response(성공) | { "success": true, "data": { "content": [ { "questionId", "title", "answered", "createdAt" } ], "page", "size", "totalElements", "totalPages" } } |
| 처리 흐름 | 토큰 확인 → 내 질문만 조회 → 답변 여부 표시 |
| 사용 테이블 | question (읽기), answer (읽기) |
| 실패 | 401: 토큰 없음·만료 |

## 내 질문 상세

1. 누가 호출하나? 로그인한 학생 (본인 질문만)
2. 무엇을 보내나? : URL의 questionId
3. 서버는 어떤 테이블을 읽고 쓰나? : question, answer 읽기 → **Query**
4. 어떤 경우에 실패하나? : 질문 없음, 남의 질문

```
GET /api/questions/12

QuestionQueryService.getMyQuestion(학번, questionId)   @Transactional(readOnly = true)
  1. questionId로 question 조회 (읽기)                   X 질문 없음 (404)
  2. 작성자 학번 = 나 인가?                               X 남의 질문 (403)
  3. answer 조회 (읽기) — 없으면 answer: null
 성공(200): { "success": true, "data": { "questionId": 12, "title": "...", "content": "...", "createdAt": "...", "answer": { "content": "...", "answeredAt": "...", "updatedAt": "..." } } }
```

| 항목 | 내용 |
|---|---|
| 기능 | 내 질문 상세 (답변 포함) |
| 메서드-URL | GET /api/questions/{questionId} |
| 권한 | 로그인한 학생 (작성자 본인) |
| Request Body | 없음 |
| Response(성공) | { "success": true, "data": { "questionId", "title", "content", "createdAt", "answer": { "content", "answeredAt", "updatedAt" } \| null } } |
| 비고 | answeredAt = 처음 답변 시각, updatedAt = 마지막 수정 시각. 둘이 다르면 화면에 "수정됨" 표시 가능 |
| 처리 흐름 | 질문 조회 → 본인 확인 → 답변 조회 |
| 사용 테이블 | question (읽기), answer (읽기) |
| 실패 | 401 / 403: 남의 질문 / 404: 질문 없음 |

## 내 질문 수정

1. 누가 호출하나? 로그인한 학생 (본인 질문만)
2. 무엇을 보내나? : 바꿀 필드만 (title, content)
3. 서버는 어떤 테이블을 읽고 쓰나? : question 읽기+쓰기, answer 읽기 → **Command**
4. 어떤 경우에 실패하나? : 입력값 오류, 질문 없음, 남의 질문, 이미 답변 달림

```
PATCH /api/questions/12   { "content": "오타 고친 내용" }

QuestionController
 └ @Valid: 보낸 값만 검사 (title 200자, content 5,000자, 빈 값 불가)   X 입력값 오류 (400)
 └ QuestionCommandService.update(학번, questionId, 요청)   @Transactional
     1. questionId로 question 조회 (읽기)                X 질문 없음 (404)
     2. 작성자 학번 = 나 인가?                            X 남의 질문 (403)
     3. 답변이 달려 있나? (answer 읽기)                   X 답변 달린 질문 수정 불가 (409)
     4. 보낸 필드만 변경 (변경 감지로 UPDATE)
 성공(200): { "success": true, "data": { "questionId": 12 } }
```

| 항목 | 내용 |
|---|---|
| 기능 | 내 질문 수정 (답변 전까지만) |
| 메서드-URL | PATCH /api/questions/{questionId} |
| 권한 | 로그인한 학생 (작성자 본인) |
| Request Body | { "title"?, "content"? } (보낸 것만 수정) |
| Response(성공) | { "success": true, "data": { "questionId": 12 } } |
| 처리 흐름 | 질문 조회 → 본인 확인 → 답변 여부 확인 → 필드 변경 |
| 사용 테이블 | question (읽기·쓰기), answer (읽기) |
| 실패 | 400: 입력값 오류 / 401 / 403: 남의 질문 / 404: 질문 없음 / 409: 답변 달린 질문 |

## 내 질문 삭제

1. 누가 호출하나? 로그인한 학생 (본인 질문만)
2. 무엇을 보내나? : URL의 questionId
3. 서버는 어떤 테이블을 읽고 쓰나? : question 읽기+삭제, answer 읽기 → **Command**
4. 어떤 경우에 실패하나? : 질문 없음, 남의 질문, 이미 답변 달림

```
DELETE /api/questions/12

QuestionCommandService.delete(학번, questionId)   @Transactional
  1. questionId로 question 조회 (읽기)                   X 질문 없음 (404)
  2. 작성자 학번 = 나 인가?                               X 남의 질문 (403)
  3. 답변이 달려 있나? (answer 읽기)                      X 답변 달린 질문 삭제 불가 (409)
  4. question 삭제 (쓰기)
 성공(200): { "success": true }
```

| 항목 | 내용 |
|---|---|
| 기능 | 내 질문 삭제 |
| 메서드-URL | DELETE /api/questions/{questionId} |
| 권한 | 로그인한 학생 (작성자 본인) |
| Request Body | 없음 |
| Response(성공) | { "success": true } |
| 처리 흐름 | 질문 조회 → 본인 확인 → 답변 여부 확인 → 삭제 |
| 사용 테이블 | question (읽기·쓰기), answer (읽기) |
| 실패 | 401 / 403: 남의 질문 / 404: 질문 없음 / 409: 답변 달린 질문 |

## 전체 질문 목록 (관리자)

1. 누가 호출하나? 관리자
2. 무엇을 보내나? : answered(선택, true/false), page, size
3. 서버는 어떤 테이블을 읽고 쓰나? : question, answer, student 읽기 → **Query**
4. 어떤 경우에 실패하나? : 토큰 없음, 관리자 아님

```
GET /api/admin/questions?answered=false&page=0&size=20

QuestionQueryService.getAllQuestions(answered, page)   @Transactional(readOnly = true)
  1. answered 값이 있으면 답변 있음/없음으로 거르기, 없으면 전체
  2. 최신순 조회 + 작성자 학번·이름 (question, student 읽기)
 성공(200): { "success": true, "data": { "content": [ { "questionId": 12, "title": "...", "studentId": "60231210", "name": "김우성", "answered": false, "createdAt": "..." } ], "page": 0, "size": 20, "totalElements": 45, "totalPages": 3 } }
```

| 항목 | 내용 |
|---|---|
| 기능 | 전체 질문 목록 (관리자) |
| 메서드-URL | GET /api/admin/questions?answered=&page=&size= |
| 권한 | 관리자 |
| Request Body | 없음 |
| Response(성공) | { "success": true, "data": { "content": [ { "questionId", "title", "studentId", "name", "answered", "createdAt" } ], "page", "size", "totalElements", "totalPages" } } |
| 처리 흐름 | 권한 확인 → 답변 여부 필터 → 최신순 조회 |
| 사용 테이블 | question, answer, student (읽기) |
| 실패 | 401 / 403: 관리자 아님 |

## 질문 상세 (관리자)

1. 누가 호출하나? 관리자
2. 무엇을 보내나? : URL의 questionId
3. 서버는 어떤 테이블을 읽고 쓰나? : question, answer, student 읽기 → **Query**
4. 어떤 경우에 실패하나? : 질문 없음

```
GET /api/admin/questions/12

QuestionQueryService.getQuestionForAdmin(questionId)   @Transactional(readOnly = true)
  1. questionId로 question 조회 (읽기)                   X 질문 없음 (404)
  2. 작성자 정보 + answer(있으면) 같이 조회
 성공(200): { "success": true, "data": { "questionId": 12, "title": "...", "content": "...", "studentId": "60231210", "name": "김우성", "answer": null } }
```

| 항목 | 내용 |
|---|---|
| 기능 | 질문 상세 (관리자, 작성자 정보 포함) |
| 메서드-URL | GET /api/admin/questions/{questionId} |
| 권한 | 관리자 |
| Request Body | 없음 |
| Response(성공) | { "success": true, "data": { "questionId", "title", "content", "studentId", "name", "answer" } } |
| 처리 흐름 | 질문 조회 → 작성자·답변 같이 반환 |
| 사용 테이블 | question, answer, student (읽기) |
| 실패 | 401 / 403 / 404: 질문 없음 |

## 답변 등록 (관리자)

1. 누가 호출하나? 관리자
2. 무엇을 보내나? : 답변 내용
3. 서버는 어떤 테이블을 읽고 쓰나? : question·student 읽기, answer 쓰기 → **Command**
4. 어떤 경우에 실패하나? : 입력값 오류, 질문 없음, 이미 답변 있음

```
POST /api/admin/questions/12/answer   { "content": "11월 10일부터입니다." }

AnswerController
 └ @Valid: content 1~5,000자                            X 입력값 오류 (400)
 └ AnswerCommandService.create(학번, questionId, 요청)   @Transactional
     1. questionId로 question 조회 (읽기)                X 질문 없음 (404)
     2. 이미 답변이 있나? (answer 읽기)                  X 이미 답변 있음 (409)
     3. 학번으로 관리자 student 조회 (읽기)
     4. Answer 생성 — PK가 questionId와 같음 (@MapsId)
     5. answer 저장 (쓰기)
        (동시에 두 번 들어와도 PK가 같아서 DB가 두 번째를 막음 → 409)
 성공(201): { "success": true, "data": { "questionId": 12 } }
```

| 항목 | 내용 |
|---|---|
| 기능 | 질문에 답변 등록 |
| 메서드-URL | POST /api/admin/questions/{questionId}/answer |
| 권한 | 관리자 |
| Request Body | { "content": "..." } |
| Response(성공) | 201 { "success": true, "data": { "questionId": 12 } } |
| 처리 흐름 | 질문 조회 → 답변 중복 확인 → 관리자 조회 → 저장 |
| 사용 테이블 | question (읽기), student (읽기), answer (쓰기) |
| 실패 | 400 / 401 / 403 / 404: 질문 없음 / 409: 이미 답변 있음 |

## 답변 수정 (관리자)

1. 누가 호출하나? 관리자
2. 무엇을 보내나? : 새 답변 내용
3. 서버는 어떤 테이블을 읽고 쓰나? : answer 읽기+쓰기, student 읽기 → **Command**
4. 어떤 경우에 실패하나? : 입력값 오류, 답변 없음

```
PATCH /api/admin/questions/12/answer   { "content": "11월 12일로 바뀌었습니다." }

AnswerCommandService.update(학번, questionId, 요청)   @Transactional
  1. questionId로 answer 조회 (읽기)                    X 답변 없음 (404)
  2. 내용 변경 + 수정한 관리자 기록 (변경 감지로 UPDATE)
     처음 답변 시각은 created_at에 그대로 남는다 → 응답의 answeredAt
 성공(200): { "success": true, "data": { "questionId": 12 } }
```

| 항목 | 내용 |
|---|---|
| 기능 | 답변 수정 |
| 메서드-URL | PATCH /api/admin/questions/{questionId}/answer |
| 권한 | 관리자 |
| Request Body | { "content": "..." } |
| Response(성공) | { "success": true, "data": { "questionId": 12 } } |
| 처리 흐름 | 답변 조회 → 내용 변경 |
| 사용 테이블 | answer (읽기·쓰기), student (읽기) |
| 실패 | 400 / 401 / 403 / 404: 답변 없음 |

---

### 에러 코드 (ErrorCode.java에 추가)

| 코드 | HTTP | 메시지 | 언제 |
|---|---|---|---|
| IN001 | 404 | 존재하지 않는 학생회 소개 글입니다 | 소개 글 수정·삭제 시 id 없음 |
| IN002 | 409 | 이 종류는 글을 하나만 둘 수 있습니다. 기존 글을 수정해 주세요 | INTRO·ORG_CHART·RULES에 두 번째 글 등록, 또는 그 종류로 변경 |
| LK001 | 404 | 존재하지 않는 링크입니다 | 링크 수정·삭제 시 id 없음 |
| LK002 | 409 | 이미 사용 중인 링크 이름입니다 | 링크 등록 시 이름 중복 |
| QA001 | 404 | 존재하지 않는 질문입니다 | 질문 조회·삭제·답변 등록 시 id 없음 |
| QA002 | 403 | 본인이 작성한 질문만 볼 수 있습니다 | 남의 질문 상세·수정·삭제 |
| QA003 | 409 | 답변이 달린 질문은 삭제할 수 없습니다 | 답변 있는 질문 삭제 |
| QA004 | 409 | 이미 답변이 등록된 질문입니다 | 답변 두 번 등록 |
| QA005 | 404 | 아직 답변이 없는 질문입니다 | 답변 없는데 수정 |
| QA006 | 409 | 답변이 달린 질문은 수정할 수 없습니다 | 답변 있는 질문 수정 |

공통 코드는 로그인 파트 것을 그대로 쓴다: 400 C001(입력값 오류), 401 A101(토큰 없음·만료), 403 A102(권한 없음), 404 M001(회원 없음).
삭제처럼 data가 없는 성공 응답은 `{ "success": true }` 만 나간다 (ApiResponse가 null 칸을 빼고 보냄).

### 정한 것
- 관리자 = DIRECTOR(국장)·PRESIDENT(회장단). SecurityConfig에 /api/admin/**, /api/questions/** 규칙 추가
- Q&A는 1:1 문의 (공개 게시판 아님). 답변 전까지만 학생이 수정·삭제
- 링크 이름은 수정 불가 (주소만 수정)
- 소개·조직도·회칙은 1개만, 공약·FAQ는 여러 개
- 소개 글 순서는 등록순. 조직도는 content에 텍스트 또는 이미지 주소 (순서 변경·이미지 업로드는 이후 검토)
- 목록 페이지 규칙은 공통(PageRequests): page 음수면 0, size는 1~50으로 보정, 에러 안 냄
- 실패 응답 형식은 A 파트 ErrorResponse(`success:false, code, message, errors`)를 그대로 따름
- 답변 없는 질문의 `"answer": null`은 의도한 동작 (프론트에서 null 처리)
