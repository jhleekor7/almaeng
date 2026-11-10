
package com.example.almaengi;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import java.util.ArrayList;

public class MainActivity extends Activity {

    LinearLayout mainLayout;
    EditText topicInput;
    ArrayList<String> concepts = new ArrayList<>();
    ArrayList<String> descriptions = new ArrayList<>();
    ArrayList<String> statuses = new ArrayList<>();

    SharedPreferences preferences;

    String topic = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences("AlmaengiData", MODE_PRIVATE);

        showHome();
    }

    TextView makeText(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.BLACK);
        view.setPadding(12, 12, 12, 12);
        return view;
    }

    Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        return button;
    }

    void setupScreen(String title) {
        ScrollView scrollView = new ScrollView(this);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(20, 20, 20, 20);

        scrollView.addView(mainLayout);
        setContentView(scrollView);

        TextView heading = makeText(title, 26);
        heading.setGravity(Gravity.CENTER);
        mainLayout.addView(heading);
    }

    void showHome() {
        setupScreen("알맹이");

        mainLayout.addView(makeText(
                "AI 기반 학습 개념 및 지식 그래프", 16));

        topicInput = new EditText(this);
        topicInput.setHint("학습 주제를 입력하세요");
        mainLayout.addView(topicInput);

        Button generateButton = makeButton("학습 개념 생성");
        mainLayout.addView(generateButton);

        generateButton.setOnClickListener(v -> {
            topic = topicInput.getText().toString().trim();

            if (topic.isEmpty()) {
                Toast.makeText(this,
                        "학습 주제를 입력하세요",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            generateConcepts();
            showConcepts();
        });

        Button historyButton = makeButton("학습 기록");
        mainLayout.addView(historyButton);

        historyButton.setOnClickListener(v -> showHistory());

        Button quizButton = makeButton("퀴즈 풀기");
        mainLayout.addView(quizButton);

        quizButton.setOnClickListener(v -> showQuiz());
    }

    void generateConcepts() {
        concepts.clear();
        descriptions.clear();
        statuses.clear();

        concepts.add(topic + "의 기본 개념");
        concepts.add(topic + "의 핵심 원리");
        concepts.add(topic + "의 주요 구성 요소");
        concepts.add(topic + "의 활용 방법");
        concepts.add(topic + "의 응용 사례");

        descriptions.add(topic + "을 이해하기 위한 기본 개념입니다.");
        descriptions.add(topic + "이 작동하는 핵심 원리입니다.");
        descriptions.add(topic + "을 구성하는 주요 요소입니다.");
        descriptions.add(topic + "을 실제로 활용하는 방법입니다.");
        descriptions.add(topic + "을 활용할 수 있는 사례입니다.");

        for (int i = 0; i < concepts.size(); i++) {
            String savedStatus = preferences.getString(
                    topic + "_" + i, "모름");

            statuses.add(savedStatus);
        }
    }

    void showConcepts() {
        setupScreen(topic + " 학습");

        Button graphButton = makeButton("지식 그래프 보기");
        mainLayout.addView(graphButton);

        graphButton.setOnClickListener(v -> showGraph());

        for (int i = 0; i < concepts.size(); i++) {
            final int index = i;

            TextView conceptText = makeText(
                    (i + 1) + ". " + concepts.get(i) + "\n"
                            + descriptions.get(i) + "\n"
                            + "현재 상태: " + statuses.get(i),
                    16);

            mainLayout.addView(conceptText);

            Button knowButton = makeButton("알고 있음");
            mainLayout.addView(knowButton);

            knowButton.setOnClickListener(v ->
                    changeStatus(index, "알고 있음"));

            Button unknownButton = makeButton("모름");
            mainLayout.addView(unknownButton);

            unknownButton.setOnClickListener(v ->
                    changeStatus(index, "모름"));

            Button reviewButton = makeButton("복습 필요");
            mainLayout.addView(reviewButton);

            reviewButton.setOnClickListener(v ->
                    changeStatus(index, "복습 필요"));
        }

        Button homeButton = makeButton("홈으로");
        mainLayout.addView(homeButton);
        homeButton.setOnClickListener(v -> showHome());
    }

    void changeStatus(int index, String status) {
        statuses.set(index, status);

        preferences.edit()
                .putString(topic + "_" + index, status)
                .apply();

        Toast.makeText(this,
                "학습 상태가 저장되었습니다.",
                Toast.LENGTH_SHORT).show();

        showConcepts();
    }

    void showGraph() {
        setupScreen("지식 그래프");

        KnowledgeGraphView graph = new KnowledgeGraphView(this);
        mainLayout.addView(graph,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 650));

        Button backButton = makeButton("개념 목록으로");
        mainLayout.addView(backButton);

        backButton.setOnClickListener(v -> showConcepts());
    }

    void showQuiz() {
        setupScreen("학습 퀴즈");

        if (concepts.isEmpty()) {
            mainLayout.addView(makeText(
                    "먼저 홈에서 학습 주제를 입력하고 개념을 생성하세요.",
                    16));

            Button homeButton = makeButton("홈으로");
            mainLayout.addView(homeButton);
            homeButton.setOnClickListener(v -> showHome());
            return;
        }

        mainLayout.addView(makeText(
                "문제: " + topic + "의 학습 개념을 확인해 보세요.",
                18));

        mainLayout.addView(makeText(
                "다음 중 학습 개념에 포함되는 것은 무엇일까요?",
                16));

        for (int i = 0; i < concepts.size(); i++) {
            final int index = i;

            Button answerButton = makeButton(concepts.get(i));
            mainLayout.addView(answerButton);

            answerButton.setOnClickListener(v ->
                    Toast.makeText(this,
                            "학습 개념: " + concepts.get(index),
                            Toast.LENGTH_LONG).show());
        }

        Button backButton = makeButton("홈으로");
        mainLayout.addView(backButton);
        backButton.setOnClickListener(v -> showHome());
    }

    void showHistory() {
        setupScreen("학습 기록");

        if (concepts.isEmpty()) {
            mainLayout.addView(makeText(
                    "학습 주제를 입력하고 개념을 생성하면 기록을 확인할 수 있습니다.",
                    16));
        } else {
            for (int i = 0; i < concepts.size(); i++) {
                String savedStatus = preferences.getString(
                        topic + "_" + i, "모름");

                mainLayout.addView(makeText(
                        concepts.get(i) + "\n학습 상태: " + savedStatus,
                        16));
            }
        }

        Button backButton = makeButton("홈으로");
        mainLayout.addView(backButton);
        backButton.setOnClickListener(v -> showHome());
    }
}
