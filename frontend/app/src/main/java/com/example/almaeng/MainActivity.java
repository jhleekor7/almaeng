package com.example.almaeng;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

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
    private boolean showingMap;
    private String currentTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        gowun = ResourcesCompat.getFont(this, R.font.gowun_dodum);
        gochi = ResourcesCompat.getFont(this, R.font.gochi_hand);
        showHomeScreen();
    }

    private void showHomeScreen() {
        showingMap = false;
        setContentView(R.layout.activity_main);
        setupWindow(findViewById(R.id.main));
        applyFonts();
        setHeroTitle();
        setupDepthButtons();

        EditText topicInput = findViewById(R.id.topicInput);
        findViewById(R.id.drawMapButton).setOnClickListener(view -> {
            String topic = topicInput.getText().toString().trim();
            if (topic.isEmpty()) {
                topicInput.requestFocus();
                return;
            }
            currentTopic = topic;
            InputMethodManager keyboard = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) {
                keyboard.hideSoftInputFromWindow(topicInput.getWindowToken(), 0);
            }
            showMapScreen();
        });
    }

    private void showMapScreen() {
        showingMap = true;
        setContentView(R.layout.activity_map);
        setupWindow(findViewById(R.id.mapRoot));

        TextView topicTitle = findViewById(R.id.mapTopic);
        topicTitle.setText(currentTopic);
        if (gowun != null) topicTitle.setTypeface(gowun, Typeface.BOLD);
        findViewById(R.id.mapBackButton).setOnClickListener(view -> showHomeScreen());

        MindMapView mindMap = findViewById(R.id.mindMapView);
        mindMap.setTypeface(gowun);
        mindMap.setConcept(currentTopic, selectedDepth);
    }

    private void setupWindow(View root) {
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
    }

    private void applyFonts() {
        TextView brandMark = findViewById(R.id.brandMark);
        TextView wordmark = (TextView) ((android.widget.LinearLayout)
                findViewById(R.id.wordmark)).getChildAt(1);
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
            styled.setSpan(new ScribbleHighlightSpan(gowun,
                            getResources().getDisplayMetrics().density),
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

    @Override
    public void onBackPressed() {
        if (showingMap) {
            showHomeScreen();
        } else {
            super.onBackPressed();
        }
    }
}
