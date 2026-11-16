package com.example.almaeng;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.widget.TextView;
import org.json.JSONObject;
import org.json.JSONArray;
import android.widget.EditText;
import android.app.AlertDialog;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private Typeface gowun;
    private Typeface gochi;
    private int selectedDepth = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        gowun = ResourcesCompat.getFont(this, R.font.gowun_dodum);
        gochi = ResourcesCompat.getFont(this, R.font.gochi_hand);

        View root = findViewById(R.id.main);
        root.setBackground(new PaperGridDrawable(this));
        WindowCompat.getInsetsController(getWindow(), root)
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), root)
                .setAppearanceLightNavigationBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return insets;
        });

        applyFonts();
        setHeroTitle();
        setupDepthButtons();
        setupGenerateButton();
    }

    private void applyFonts() {
        TextView brandMark = findViewById(R.id.brandMark);
        TextView wordmark = (TextView) ((android.widget.LinearLayout) findViewById(R.id.wordmark))
                .getChildAt(1);
        TextView kicker = findViewById(R.id.homeKicker);
        TextView depthLabel = findViewById(R.id.depthLabel);
        EditText topicInput = findViewById(R.id.topicInput);

        if (gowun != null) {
            wordmark.setTypeface(gowun, Typeface.BOLD);
            depthLabel.setTypeface(gowun);
            topicInput.setTypeface(gowun);
        }
        if (gochi != null) {
            brandMark.setTypeface(gochi);
            kicker.setTypeface(gochi);
            ((TextView) findViewById(R.id.depth3)).setTypeface(gochi);
            ((TextView) findViewById(R.id.depth5)).setTypeface(gochi);
            ((TextView) findViewById(R.id.depth8)).setTypeface(gochi);
        }
    }

    private void setHeroTitle() {
        TextView title = findViewById(R.id.heroTitle);
        if (gowun != null) title.setTypeface(gowun);
        String text = getString(R.string.home_title);
        SpannableString styled = new SpannableString(text);
        int highlightStart = text.indexOf('\n') + 1;
        if (highlightStart > 0 && highlightStart < text.length()) {
            styled.setSpan(new ScribbleHighlightSpan(gowun, getResources().getDisplayMetrics().density),
                    highlightStart, text.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        title.setText(styled);
    }

    private void setupDepthButtons() {
        setDepthOption(R.id.depth3, 3);
        setDepthOption(R.id.depth5, 5);
        setDepthOption(R.id.depth8, 8);
    }

    private void setDepthOption(int viewId, int depth) {
        TextView option = findViewById(viewId);
        updateDepthAppearance(option, depth == selectedDepth);
        option.setOnClickListener(view -> {
            selectedDepth = depth;
            updateDepthAppearance((TextView) findViewById(R.id.depth3), depth == 3);
            updateDepthAppearance((TextView) findViewById(R.id.depth5), depth == 5);
            updateDepthAppearance((TextView) findViewById(R.id.depth8), depth == 8);
        });
    }

    private void updateDepthAppearance(TextView option, boolean active) {
        option.setBackgroundResource(active
                ? R.drawable.bg_depth_active : R.drawable.bg_depth_inactive);
        option.setTextColor(active ? getColor(R.color.ink) : getColor(R.color.ink_soft));
    }


    // 지도 그리기 버튼: Backend API 호출
    private void setupGenerateButton() {
        EditText topicInput = findViewById(R.id.topicInput);
        View generateButton = findViewById(R.id.drawMapButton);

        generateButton.setOnClickListener(view -> {
            String topic = topicInput.getText().toString().trim();

            if (topic.isEmpty()) {
                topicInput.setError("학습 주제를 입력해주세요.");
                topicInput.requestFocus();
                return;
            }

            generateButton.setEnabled(false);

            MindmapApiClient.createMindmap(
                    topic,
                    selectedDepth,
                    new MindmapApiClient.Callback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            runOnUiThread(() -> {
                                generateButton.setEnabled(true);
                                showMindmapResponse(response);
                            });
                        }

                        @Override
                        public void onFailure(String message) {
                            runOnUiThread(() -> {
                                generateButton.setEnabled(true);

                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("마인드맵 생성 실패")
                                        .setMessage(message)
                                        .setPositiveButton("확인", null)
                                        .show();
                            });
                        }
                    }
            );
        });
    }

    // Backend에서 받은 JSON 응답 확인
    private void showMindmapResponse(JSONObject response) {
        String status = response.optString("generationStatus");
        String sourceText = response.optString("sourceText");
        JSONArray nodes = response.optJSONArray("nodes");
        JSONArray edges = response.optJSONArray("edges");

        StringBuilder message = new StringBuilder();

        message.append("생성 상태: ").append(status).append("\n");
        message.append("학습 주제: ").append(sourceText).append("\n\n");

        int nodeCount = nodes == null ? 0 : nodes.length();
        int edgeCount = edges == null ? 0 : edges.length();

        message.append("개념 노드: ").append(nodeCount).append("개\n");
        message.append("연결 관계: ").append(edgeCount).append("개\n\n");

        if (nodes != null) {
            for (int i = 0; i < nodes.length(); i++) {
                JSONObject node = nodes.optJSONObject(i);

                if (node != null) {
                    message.append(i + 1)
                            .append(". ")
                            .append(node.optString("conceptName"))
                            .append("\n")
                            .append(node.optString("description"))
                            .append("\n\n");
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("마인드맵 생성 응답")
                .setMessage(message.toString())
                .setPositiveButton("확인", null)
                .show();
    }

}
