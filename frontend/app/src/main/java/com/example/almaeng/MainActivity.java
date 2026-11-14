package com.example.almaeng;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
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

}

