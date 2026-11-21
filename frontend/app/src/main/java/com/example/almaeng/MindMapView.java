package com.example.almaeng;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** A small, fixed-layout concept map drawn with Android's Java Canvas API. */
public class MindMapView extends View {
    private static final String[] HEADINGS = {
            "정의", "구성 요소", "진행 과정", "주요 특징",
            "예시", "활용", "관련 개념", "확인 질문"
    };
    private static final String[] DETAILS = {
            "주제를 한 문장으로", "무엇으로 이루어질까?", "어떤 순서로 될까?", "무엇이 특별할까?",
            "주변에서 찾아보기", "어디에 쓸 수 있을까?", "함께 알아둘 말", "내 말로 설명해보기"
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private String topic = "";
    private int depth = 5;
    private Typeface typeface;

    public MindMapView(Context context) {
        super(context);
        init();
    }

    public MindMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MindMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
        invalidate();
    }

    public void setConcept(String topic, int depth) {
        this.topic = topic == null ? "" : topic;
        this.depth = depth == 3 || depth == 8 ? depth : 5;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int rows = (depth + 1) / 2;
        int desiredHeight = dp(166 + rows * 188 + 20);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float centerX = width / 2f;
        float rootWidth = Math.min(width - dp(44), dp(290));
        float rootTop = dp(22);
        float rootHeight = dp(62);
        float cardWidth = (width - dp(48)) / 2f;
        float cardHeight = dp(124);
        float firstTop = dp(166);
        float rowGap = dp(188);
        float leftCenter = dp(16) + cardWidth / 2f;
        float rightCenter = width - leftCenter;
        int rows = (depth + 1) / 2;

        drawConnectors(canvas, centerX, rootTop, rootHeight, leftCenter, rightCenter,
                firstTop, cardHeight, rowGap, rows);
        drawRoot(canvas, centerX, rootTop, rootWidth, rootHeight);

        for (int i = 0; i < depth; i++) {
            int row = i / 2;
            int column = i % 2;
            float left = column == 0 ? dp(16) : width - dp(16) - cardWidth;
            float top = firstTop + row * rowGap;
            drawCard(canvas, left, top, cardWidth, cardHeight, HEADINGS[i], DETAILS[i], i);
        }
    }

    private void drawConnectors(Canvas canvas, float centerX, float rootTop, float rootHeight,
                                float leftCenter, float rightCenter, float firstTop,
                                float cardHeight, float rowGap, int rows) {
        paint.setColor(0xFFB985CB);
        paint.setStrokeWidth(dp(2));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        float spineBottom = firstTop - dp(20) + (rows - 1) * rowGap;
        canvas.drawLine(centerX, rootTop + rootHeight, centerX, spineBottom, paint);
        for (int row = 0; row < rows; row++) {
            float connectorY = firstTop - dp(20) + row * rowGap;
            int firstIndex = row * 2;
            boolean hasLeft = firstIndex < depth;
            boolean hasRight = firstIndex + 1 < depth;
            float leftX = leftCenter;
            float rightX = rightCenter;
            if (hasLeft && hasRight) {
                canvas.drawLine(leftX, connectorY, rightX, connectorY, paint);
            } else if (hasLeft) {
                canvas.drawLine(centerX, connectorY, leftX, connectorY, paint);
            }
            if (hasLeft) canvas.drawLine(leftX, connectorY, leftX, firstTop + row * rowGap, paint);
            if (hasRight) canvas.drawLine(rightX, connectorY, rightX, firstTop + row * rowGap, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawRoot(Canvas canvas, float centerX, float top, float width, float height) {
        box.set(centerX - width / 2f, top, centerX + width / 2f, top + height);
        paint.setColor(0xFFF9F5EC);
        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(dp(3), 0, dp(2), 0x33292821);
        canvas.drawRoundRect(box, dp(20), dp(20), paint);
        paint.clearShadowLayer();
        paint.setColor(0xFF292821);
        paint.setStrokeWidth(dp(1));
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(box, dp(20), dp(20), paint);
        paint.setStyle(Paint.Style.FILL);

        setupText(dp(16), Typeface.BOLD, 0xFF292821);
        drawWrapped(canvas, topic, centerX, top + dp(26), width - dp(28), 2, dp(19));
    }

    private void drawCard(Canvas canvas, float left, float top, float width, float height,
                          String heading, String detail, int index) {
        box.set(left, top, left + width, top + height);
        paint.setColor(index % 2 == 0 ? 0xE6FFFDF8 : 0xDDF9F5EC);
        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(dp(2), 0, dp(2), 0x22292821);
        canvas.drawRoundRect(box, dp(16), dp(16), paint);
        paint.clearShadowLayer();
        paint.setColor(0xFFB985CB);
        paint.setStrokeWidth(dp(1.2f));
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(box, dp(16), dp(16), paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(0xFFB985CB);
        canvas.drawCircle(left + dp(18), top + dp(18), dp(4), paint);
        setupText(dp(14), Typeface.BOLD, 0xFF292821);
        drawWrapped(canvas, heading, left + width / 2f, top + dp(48), width - dp(22), 1, dp(18));
        setupText(dp(11), Typeface.NORMAL, 0xFF6E685E);
        drawWrapped(canvas, detail, left + width / 2f, top + dp(78), width - dp(20), 2, dp(16));
    }

    private void setupText(float size, int style, int color) {
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(typeface == null ? Typeface.DEFAULT : Typeface.create(typeface, style));
        paint.setTextAlign(Paint.Align.CENTER);
    }

    private void drawWrapped(Canvas canvas, String text, float centerX, float baseline,
                             float maxWidth, int maxLines, float lineHeight) {
        if (text == null || text.isEmpty()) return;
        int start = 0;
        for (int line = 0; line < maxLines && start < text.length(); line++) {
            int count = paint.breakText(text, start, text.length(), true, maxWidth, null);
            if (count <= 0) break;
            int end = start + count;
            if (end < text.length() && line < maxLines - 1) {
                int space = text.lastIndexOf(' ', end);
                if (space >= start) end = space;
            }
            String part = text.substring(start, end).trim();
            if (end < text.length() && line == maxLines - 1) part += "…";
            canvas.drawText(part, centerX, baseline + line * lineHeight, paint);
            start = Math.max(end, start + 1);
            while (start < text.length() && text.charAt(start) == ' ') start++;
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
