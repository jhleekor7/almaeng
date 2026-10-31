-- 알맹이 MVP 테스트용 샘플 데이터
-- schema.sql 실행 후 실행하세요.

USE almaeng;

START TRANSACTION;

INSERT INTO knowledge_maps
    (map_id, source_text, requested_node_count, generation_status)
VALUES
    (1, '정렬 알고리즘은 데이터의 순서를 특정 기준에 따라 재배치하는 알고리즘이다. 대표적으로 버블 정렬과 병합 정렬이 있다.',
     3, 'COMPLETED');

INSERT INTO map_nodes
    (node_id, map_id, concept_name, description, learning_status, display_order)
VALUES
    (101, 1, '정렬 알고리즘', '데이터를 특정 기준에 따라 재배치하는 알고리즘', 'UNDERSTOOD', 1),
    (102, 1, '버블 정렬', '인접한 두 원소를 비교하고 순서가 잘못되면 교환하는 정렬 방식', 'UNCHECKED', 2),
    (103, 1, '병합 정렬', '데이터를 분할한 뒤 정렬된 부분 배열을 병합하는 분할 정복 기반 정렬 방식', 'NEEDS_REVIEW', 3);

INSERT INTO map_edges
    (edge_id, map_id, source_node_id, target_node_id, relationship_label)
VALUES
    (201, 1, 101, 102, '정렬 알고리즘의 종류'),
    (202, 1, 101, 103, '정렬 알고리즘의 종류');

INSERT INTO quiz_questions
    (question_id, node_id, question_text, explanation, difficulty, display_order)
VALUES
    (301, 102, '버블 정렬의 기본 동작으로 가장 알맞은 것은 무엇인가?',
     '버블 정렬은 인접한 두 원소를 비교하고 필요하면 교환하는 과정을 반복합니다.',
     'EASY', 1),
    (302, 103, '병합 정렬의 핵심 아이디어는 무엇인가?',
     '병합 정렬은 문제를 작은 부분으로 나누어 정렬한 뒤 결과를 병합합니다.',
     'MEDIUM', 1);

INSERT INTO quiz_options
    (option_id, question_id, option_text, is_correct, display_order)
VALUES
    (401, 301, '인접한 두 원소를 비교하고 필요하면 교환한다.', TRUE, 1),
    (402, 301, '항상 최솟값을 찾아 맨 앞 원소와 교환한다.', FALSE, 2),
    (403, 301, '정렬된 배열 두 개만 병합한다.', FALSE, 3),
    (404, 301, '해시 함수를 이용해 원소를 버킷에 넣는다.', FALSE, 4),
    (405, 302, '데이터를 분할하고 각 부분을 정렬한 뒤 병합한다.', TRUE, 1),
    (406, 302, '모든 원소를 무작위로 섞는다.', FALSE, 2),
    (407, 302, '인접한 원소만 한 번 비교한다.', FALSE, 3),
    (408, 302, '각 원소를 그래프의 정점으로 바꾼다.', FALSE, 4);

INSERT INTO quiz_attempts
    (attempt_id, question_id, selected_option_id, is_correct)
VALUES
    (501, 301, 401, TRUE),
    (502, 302, 406, FALSE);

COMMIT;
