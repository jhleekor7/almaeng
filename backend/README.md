# Almaeng Backend API

Spring Boot 기반 개념지도 생성 API입니다.

## 실행

Java 17과 Gradle이 필요합니다.

\`\`\`bash
cd backend
gradle bootRun
\`\`\`

기본 서버 주소는 \`http://localhost:8080\`입니다.

## API

### 상태 확인

\`GET /api/v1/health\`

### 개념지도 생성

\`POST /api/v1/mindmaps\`

요청:

\`\`\`json
{
  "sourceText": "광합성의 원리",
  "depth": 5
}
\`\`\`

\`depth\`는 프론트 화면과 동일하게 \`3\`, \`5\`, \`8\`만 허용합니다.
\`sourceText\`는 공백이 아닌 문자열이어야 하며 최대 500자입니다.

응답 예시(개념 노드 일부):

\`\`\`json
{
  "mapId": null,
  "sourceText": "광합성의 원리",
  "depth": 5,
  "generationStatus": "MOCK_GENERATED_NOT_SAVED",
  "nodes": [
    {
      "nodeId": 1,
      "conceptName": "광합성의 원리",
      "description": "사용자가 입력한 학습 주제입니다.",
      "learningStatus": "UNCHECKED"
    }
  ],
  "edges": [
    {
      "edgeId": 1,
      "sourceNodeId": 1,
      "targetNodeId": 2,
      "relationshipLabel": "세부 개념"
    }
  ]
}
\`\`\`

현재 응답은 DB에 저장하지 않습니다. 따라서 \`mapId\`는 \`null\`이고 노드·관계 ID는 응답 안에서만 유효한 임시 ID입니다.

## 실제 AI 생성 활성화

API 키를 코드나 Git에 올리지 말고 환경 변수로 설정합니다.

- \`ALMAENG_AI_ENABLED=true\`
- \`ALMAENG_AI_API_KEY=발급받은 키\`
- \`ALMAENG_AI_MODEL=사용할 모델 이름\`
- \`ALMAENG_AI_BASE_URL=https://api.openai.com/v1\` (기본값)

설정이 없거나 \`ALMAENG_AI_ENABLED\`가 꺼져 있으면 연동 테스트용 mock 응답을 반환합니다.
mock 응답은 실제 AI가 생성한 개념이 아닙니다.

## 테스트

\`\`\`bash
gradle test
\`\`\`
