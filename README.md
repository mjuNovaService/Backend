# NOVA Backend

학생회 물품 대여 자동화 서비스 백엔드

## 기술 스택

- Java 21
- Spring Boot 4.1.1
- Spring Data JPA (Hibernate 7)
- PostgreSQL 16
- Gradle
- Springdoc OpenAPI (Swagger)

## 개발 환경 세팅

### 1. 사전 준비

- **JDK 21** (없으면 Gradle이 자동으로 받아옵니다)
- **Docker Desktop** 설치 후 앱을 한 번 실행 — 메뉴바 고래 아이콘이 떠 있어야 합니다!
- IntelliJ IDEA

JDK 확인:

```bash
java -version    # 21.x.x 가 나와야 함
```

여러 버전이 깔려 있다면 21로 고정 (macOS):

```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
source ~/.zshrc
```

### 2. 클론docker compose up -d

```bash
git clone https://github.com/mjuNovaService/Backend.git
cd Backend
```

### 3. DB 실행

```bash
docker compose up -d
docker ps          # nova-postgres 가 (healthy) 될 때까지 대기 (첫 실행 30초 정도)
```

- PostgreSQL 16 / 포트 **5433** (로컬 PostgreSQL과 충돌 방지)
- DB명 `nova` / 계정 `nova` / 비밀번호 `root1234`
- 로컬 개발 전용 계정입니다. 실서버 접속 정보는 커밋X

접속 확인:

```bash
docker exec -it nova-postgres psql -U nova -d nova -c "\l"
```

### 4. IntelliJ 설정

- `Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM` → **21**
- `Settings → Build, Execution, Deployment → Compiler → Annotation Processors` → **Enable annotation processing** 체크 (Lombok 필수)

### 5. 실행

```bash
./gradlew bootRun
```

| 항목 | 주소 |
|---|---|
| 서버 | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| 헬스체크 | http://localhost:8080/health |

Gradle은 wrapper를 사용합니다. 별도 설치 없이 항상 `./gradlew` 로 실행.

## 프로젝트 구조

```
com.mju.nova.backend
├── common/                     공통 모듈
│   ├── config/                 설정 (JpaConfig)
│   ├── entity/                 BaseEntity (createdAt, updatedAt)
│   ├── exception/              ErrorCode, BusinessException, GlobalExceptionHandler
│   ├── response/               ApiResponse (공통 응답 포맷)
│   └── HealthController.java
└── <도메인>/                    도메인별 패키지 (rental, item, member ...)
    ├── presentation/           Controller, DTO
    ├── application/            Service (유스케이스, 트랜잭션 경계)
    ├── domain/                 Entity, Repository 인터페이스, Enum
    └── infrastructure/         외부 연동
```

### 계층 역할

| 계층 | 역할 |
|---|---|
| presentation | HTTP 요청/응답만 담당. 비즈니스 로직 금지 |
| application | 유스케이스 흐름 조립, `@Transactional` 경계. 판단은 domain에 위임 |
| domain | 비즈니스 규칙. Entity가 자기 상태를 스스로 지킴 |
| infrastructure | DB, 외부 API, 메일 등 기술적 세부사항 |

의존성 방향: `presentation → application → domain ← infrastructure`

## 설정 (application.yaml)

프로필은 `local` / `prod` 두 가지입니다.

| 항목 | local | prod |
|---|---|---|
| DB 정보 | 평문 | 환경변수 (`${DB_URL}` 등) |
| `ddl-auto` | `update` | `validate` |
| `show-sql` | `true` | `false` |
| Swagger | 활성 | 비활성 |

기본값은 `local` 입니다. prod로 실행하려면:

```bash
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 개발 규칙

### 공통 응답

모든 API는 `ApiResponse` 로 감싸서 반환.

```java
@GetMapping("/api/rentals/{id}")
public ApiResponse<RentalResponse> findOne(@PathVariable Long id) {
    return ApiResponse.success(rentalService.findOne(id));
}
```

응답 형태:

```json
{ "success": true, "data": { "id": 1, "itemName": "충전기" } }
{ "success": false, "message": "이미 대여 중인 물품입니다" }
```

### 예외 처리

컨트롤러에서 try-catch X. `BusinessException` 을 던지면 `GlobalExceptionHandler` 가 처리.

```java
Rental rental = rentalRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.RENTAL_NOT_FOUND));
```

새 에러는 `ErrorCode` enum에 추가. 코드 접두어는 도메인별로 구분.

| 접두어 | 영역 |
|---|---|
| `C` | 공통 |
| `M` | 회원 |
| `R` | 대여 |
| `I` | 물품 |

### Entity

- `@Setter`, `@Data` 사용 금지. 상태 변경은 의미 있는 메서드로 (`rental.cancel()`)
- 생성/수정 시각이 필요하면 `BaseEntity` 를 상속
- 응답에 Entity를 그대로 노출하지 않고 DTO로 변환

```java
// 나쁜 예 — 규칙 우회 가능
rental.setStatus(RentalStatus.RETURNED);

// 좋은 예 — 규칙이 도메인 안에 있음
rental.returnItem();
```

### Swagger 어노테이션

컨트롤러에는 최소한 `@Tag`, `@Operation` 을 답니다.

```java
@Tag(name = "대여", description = "물품 대여 API")
@RestController
public class RentalController {

    @Operation(summary = "대여 신청", description = "회원이 물품을 대여 신청합니다")
    @PostMapping("/api/rentals")
    public ApiResponse<RentalResponse> rent(@Valid @RequestBody RentalCreateRequest request) { ... }
}
```

## 협업 규칙

### 브랜치

| 접두어 | 용도 |
|---|---|
| `feature/` | 새 기능 |
| `fix/` | 버그 수정 |
| `refactor/` | 리팩토링 |
| `chore/` | 설정, 빌드 |

작업 흐름:

```bash
git checkout main
git pull origin main              # 항상 최신 상태에서 시작
git checkout -b feature/rental-create

# 작업 후
git add .
git commit -m "feat: 대여 신청 API 구현"
git push origin feature/rental-create
# → GitHub에서 PR 생성 → 리뷰 → main에 merge
```

`main` 에 직접 push X.

### 커밋 메시지

```
feat: 대여 신청 API 구현
fix: 대여 기간 계산 오류 수정
refactor: RentalService 메서드 분리
chore: springdoc 의존성 추가
docs: README 수정
test: 대여 신청 단위 테스트 추가
```

## 트러블슈팅

| 증상                                           | 원인 / 해결                                                      |
|----------------------------------------------|--------------------------------------------------------------|
| `Unsupported class file major version`       | JDK 버전 불일치 → Gradle JVM을 21로 변경                              |
| getter를 못 찾는 컴파일 에러                          | Lombok → Annotation processing 활성화                           |
| `permission denied: ./gradlew`               | `chmod +x gradlew`                                           |
| `Connection refused` | `docker ps` 에서 `(healthy)` 인지 확인 |
| `Failed to determine a suitable driver class` | 프로필 미적용 → `application.yaml` 확인                              |
| 스키마가 꼬였을 때                                   | `docker compose down -v && docker compose up -d` (데이터 전부 삭제됨) |

## 참고

Spring Boot **4.x** 기준.
DB는 PostgreSQL 16 기준. MySQL에서 전환됨.