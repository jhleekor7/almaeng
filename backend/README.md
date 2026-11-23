# Almaeng Backend API

Spring Boot 기반 개념지도 생성 및 조회 API입니다.

## 환경

- Java 17
- MySQL 8.0.16 이상
- Gradle 9.6.0 Wrapper (프로젝트의 `frontend/gradlew` 사용)

## DB 준비

DB 담당 브랜치의 통합 스키마 `db/schema.sql`을 MySQL에서 먼저 실행합니다.

```bash
mysql -u root -p < db/schema.sql
```

MySQL 접속 정보는 환경 변수로 설정합니다. 비밀번호를 코드나 Git에 직접 작성하지 않습니다.

```bash
export DB_URL='jdbc:mysql://localhost:3306/almaeng?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul'
export DB_USERNAME='root'
export DB_PASSWORD='your-local-password'
```

## 실행

저장소 루트 기준으로 실행합니다.

```bash
cd backend
bash ../frontend/gradlew test
bash ../frontend/gradlew bootRun
```

기본 서버 주소는 `http://localhost:8080`입니다.

## API

### 상태 확인

`GET /api/v1/health`

### 개념지도 생성 및 저장

`POST /api/v1/mindmaps`

요청:

```json
{
  "sourceText": "광합성의 원리",
  "depth": 5
}
```

- `sourceText`: 공백이 아닌 문자열, 최대 500자
- `depth`: 화면 선택값인 `3`, `5`, `8`만 허용

생성 성공 시 `knowledge_maps`, `map_nodes`, `map_edges`에 트랜잭션으로 저장합니다. 반환되는 `mapId`, `nodeId`, `edgeId`는 DB에서 발급된 실제 ID입니다.

응답 예시:

```json
{
  "mapId": 1,
  "sourceText": "광합성의 원리",
  "depth": 5,
  "generationStatus": "COMPLETED",
  "nodes": [
    {
      "nodeId": 1,
      "conceptName": "광합성의 원리",
      "description": "사용자가 입력한 학습 주제입니다.",
      "learningStatus": "UNCHECKED"
    }
  ],
  "edges": []
}
```

실제 응답에는 요청한 깊이만큼 노드가 포함됩니다. 예시는 구조 설명을 위한 일부 데이터입니다.

### 저장된 개념지도 조회

`GET /api/v1/mindmaps/{mapId}`

생성 API가 반환한 `mapId`를 사용하면 DB에서 지도와 노드·관계 목록을 다시 조회합니다. 존재하지 않는 ID에는 404를 반환합니다.

## AI 생성 설정

기본값은 Android 및 DB 연동을 확인하는 Mock 모드입니다.

실제 AI 생성으로 전환할 때는 환경 변수를 설정합니다.

```bash
export ALMAENG_AI_ENABLED=true
export ALMAENG_AI_API_KEY='발급받은 API 키'
export ALMAENG_AI_MODEL='사용할 모델 이름'
export ALMAENG_AI_BASE_URL='https://api.openai.com/v1'
```

AI 키는 코드나 Git에 커밋하지 않습니다.

## 테스트

`gradle test`는 H2 인메모리 DB를 사용해 저장·조회·잘못된 관계 검증을 실행합니다. 실제 운영 DB는 MySQL이며, 로컬 서버 실행 전 `db/schema.sql`을 적용해야 합니다.
