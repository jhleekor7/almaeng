# 알맹이 DB (MVP)

알맹이 학습 이해도 시각화 앱의 MySQL 데이터베이스 스키마와 테스트 데이터입니다.

## 환경

- MySQL 8.0.16 이상 권장
- 문자셋: `utf8mb4`
- 엔진: InnoDB

## 파일

- `schema.sql`: `almaeng` 데이터베이스와 테이블 생성
- `sample_data.sql`: 정렬 알고리즘 개념 지도, 퀴즈 및 풀이 샘플 데이터

## 실행 방법

MySQL 클라이언트에서 다음 순서로 실행합니다.

```bash
mysql -u <사용자명> -p < schema.sql
mysql -u <사용자명> -p almaeng < sample_data.sql
```

또는 MySQL Workbench에서 `schema.sql`을 먼저 실행하고, 이어서 `sample_data.sql`을 실행합니다.

## 테이블 관계

- `knowledge_maps` 1 : N `map_nodes`
- `knowledge_maps` 1 : N `map_edges`
- `map_nodes` 1 : N `quiz_questions`
- `quiz_questions` 1 : N `quiz_options`
- `quiz_questions` 1 : N `quiz_attempts`
- `quiz_options` 1 : N `quiz_attempts` (선택된 선택지 기준)

## 테이블 역할

| 테이블 | 역할 |
|---|---|
| `knowledge_maps` | 사용자가 입력한 원문과 개념 지도 생성 상태 저장 |
| `map_nodes` | 개념 노드와 학습 상태 저장 |
| `map_edges` | 개념 노드 사이의 연결 관계 저장 |
| `quiz_questions` | 노드별 객관식 문제와 해설 저장 |
| `quiz_options` | 문제 선택지와 정답 여부 저장 |
| `quiz_attempts` | 사용자의 문제 풀이 결과 저장 |

## 학습 상태

`map_nodes.learning_status` 값은 다음과 같습니다.

- `UNCHECKED`: 아직 퀴즈로 이해도를 확인하지 않음
- `UNDERSTOOD`: 이해 확인
- `NEEDS_REVIEW`: 복습 필요

퀴즈 제출 후 이 상태를 갱신하는 규칙은 API 담당자와 합의해야 합니다. 예를 들어, 노드에 연결된 문제를 모두 맞혔을 때 `UNDERSTOOD`로 바꾸거나, 오답이 있으면 `NEEDS_REVIEW`로 바꿀 수 있습니다.

## MVP 범위 및 팀 합의가 필요한 사항

- 로그인/회원 기능이 확정되지 않아 `users` 테이블은 제외했습니다.
- 효과음 및 움직임 줄이기 설정은 우선 프론트엔드 로컬 저장소에서 관리하는 것으로 가정했습니다.
- 노드 위치 좌표는 선택 항목입니다. 프론트엔드가 매번 레이아웃을 계산한다면 `position_x`, `position_y`는 사용하지 않아도 됩니다.
- 학습 상태 초기화 시 노드 상태만 초기화할지, `quiz_attempts` 기록도 삭제할지는 팀에서 결정해야 합니다.
- 현재 스키마는 퀴즈 응답이 어떤 문제와 선택지를 참조하는지 FK로 보장합니다. 선택지가 실제 해당 문제에 속하는지, `is_correct`가 선택지의 정답 여부와 일치하는지는 API 계층에서 검증해야 합니다.
- `map_edges`의 양쪽 노드가 동일한 지도에 속하는지도 API 계층에서 검증해야 합니다.
- `quiz_attempts`에는 사용자 식별자를 두지 않았습니다. 로그인 또는 사용자별 학습 기록이 필요해지면 `users` 테이블과 FK를 추가하는 마이그레이션이 필요합니다.

## GitHub 업로드 전 확인

1. 팀에서 MySQL 8.0 사용 여부를 확정합니다.
2. API 담당자와 테이블명, 컬럼명, 상태 enum, 퀴즈 응답 형식을 맞춥니다.
3. 샘플 데이터를 로컬 MySQL에서 실행해 FK 오류가 없는지 확인합니다.
4. 팀 저장소의 합의된 폴더에 세 파일을 커밋합니다.