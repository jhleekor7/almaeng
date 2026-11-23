package com.example.almaeng;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backend에서 받은 노드와 관계를 Canvas로 그리는 개념지도 View.
 * 첫 번째 노드는 중심 주제, 나머지 노드는 아래쪽 카드로 배치한다.
 */
public class MindMapView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();

    private MindmapData mindmap;
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

    public void setMindmap(MindmapData mindmap) {
        this.mindmap = mindmap;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int nodeCount = mindmap == null ? 1 : mindmap.getNodes().size();
        int childCount = Math.max(0, nodeCount - 1);
        int rows = (childCount + 1) / 2;

        int desiredHeight = dp(22 + 74 + 20);
        if (rows > 0) {
            desiredHeight = dp(22 + 74 + 48 + rows * 150 + (rows - 1) * 24 + 20);
        }

        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mindmap == null || mindmap.getNodes().isEmpty()) {
            return;
        }

        List<MindmapData.Node> nodes = mindmap.getNodes();
        float width = getWidth();
        float centerX = width / 2f;

        float rootTop = dp(22);
        float rootWidth = Math.min(width - dp(44), dp(320));
        float rootHeight = dp(74);
        float childTop = rootTop + rootHeight + dp(48);
        float childWidth = (width - dp(48)) / 2f;
        float childHeight = dp(150);
        float rowGap = dp(24);

        Map<Long, RectF> nodeBounds = new LinkedHashMap<>();

        MindmapData.Node root = nodes.get(0);
        RectF rootBounds = new RectF(
                centerX - rootWidth / 2f,
                rootTop,
                centerX + rootWidth / 2f,
                rootTop + rootHeight
        );
        nodeBounds.put(root.getNodeId(), rootBounds);

        for (int i = 1; i < nodes.size(); i++) {
            int childIndex = i - 1;
            int row = childIndex / 2;
            int column = childIndex % 2;

            float left = column == 0
                    ? dp(16)
                    : width - dp(16) - childWidth;
            float top = childTop + row * (childHeight + rowGap);

            nodeBounds.put(
                    nodes.get(i).getNodeId(),
                    new RectF(left, top, left + childWidth, top + childHeight)
            );
        }

        drawEdges(canvas, mindmap.getEdges(), nodeBounds);

        drawNode(canvas, rootBounds, root, true);
        for (int i = 1; i < nodes.size(); i++) {
            drawNode(canvas, nodeBounds.get(nodes.get(i).getNodeId()), nodes.get(i), false);
        }
    }

    private void drawEdges(
            Canvas canvas,
            List<MindmapData.Edge> edges,
            Map<Long, RectF> nodeBounds
    ) {
        paint.setColor(0xFFB985CB);
        paint.setStrokeWidth(dp(2));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);

        for (MindmapData.Edge edge : edges) {
            RectF source = nodeBounds.get(edge.getSourceNodeId());
            RectF target = nodeBounds.get(edge.getTargetNodeId());

            if (source == null || target == null) {
                continue;
            }

            float dx = target.centerX() - source.centerX();
            float dy = target.centerY() - source.centerY();

            if (Math.abs(dx) < 0.1f && Math.abs(dy) < 0.1f) {
                continue;
            }

            float sourceScale = 1f / Math.max(
                    Math.abs(dx) / (source.width() / 2f),
                    Math.abs(dy) / (source.height() / 2f)
            );
            float targetScale = 1f / Math.max(
                    Math.abs(dx) / (target.width() / 2f),
                    Math.abs(dy) / (target.height() / 2f)
            );

            float startX = source.centerX() + dx * sourceScale;
            float startY = source.centerY() + dy * sourceScale;
            float endX = target.centerX() - dx * targetScale;
            float endY = target.centerY() - dy * targetScale;

            canvas.drawLine(startX, startY, endX, endY, paint);

            String label = edge.getRelationshipLabel();
            if (label != null && !label.isBlank()) {
                drawEdgeLabel(canvas, label, (startX + endX) / 2f, (startY + endY) / 2f);
            }
        }

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawEdgeLabel(Canvas canvas, String label, float centerX, float centerY) {
        setupText(dp(9), Typeface.NORMAL, 0xFF6E5680);
        float textWidth = Math.min(paint.measureText(label), dp(96));
        box.set(
                centerX - textWidth / 2f - dp(5),
                centerY - dp(10),
                centerX + textWidth / 2f + dp(5),
                centerY + dp(10)
        );
        paint.setColor(0xFFF9F5EC);
        canvas.drawRoundRect(box, dp(7), dp(7), paint);

        paint.setColor(0xFF6E5680);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, centerX, centerY + dp(3), paint);
    }

    private void drawNode(
            Canvas canvas,
            RectF bounds,
            MindmapData.Node node,
            boolean root
    ) {
        if (bounds == null) {
            return;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(root ? 0xFFF9F5EC : 0xE6FFFDF8);
        paint.setShadowLayer(dp(2), 0, dp(2), 0x22292821);
        canvas.drawRoundRect(bounds, dp(root ? 20 : 16), dp(root ? 20 : 16), paint);
        paint.clearShadowLayer();

        paint.setColor(0xFFB985CB);
        paint.setStrokeWidth(dp(root ? 1.4f : 1.2f));
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(bounds, dp(root ? 20 : 16), dp(root ? 20 : 16), paint);
        paint.setStyle(Paint.Style.FILL);

        if (!root) {
            paint.setColor(0xFFB985CB);
            canvas.drawCircle(bounds.left + dp(18), bounds.top + dp(18), dp(4), paint);
        }

        float centerX = bounds.centerX();
        float headingBaseline = bounds.top + dp(root ? 29 : 47);
        float headingWidth = bounds.width() - dp(24);

        setupText(dp(root ? 16 : 14), Typeface.BOLD, 0xFF292821);
        drawWrapped(
                canvas,
                node.getConceptName(),
                centerX,
                headingBaseline,
                headingWidth,
                2,
                dp(root ? 19 : 18)
        );

        float descriptionBaseline = bounds.top + dp(root ? 52 : 80);
        setupText(dp(11), Typeface.NORMAL, 0xFF6E685E);
        drawWrapped(
                canvas,
                node.getDescription(),
                centerX,
                descriptionBaseline,
                bounds.width() - dp(22),
                root ? 1 : 3,
                dp(15)
        );

        String status = node.getLearningStatus();
        if (status != null && !status.isBlank() && !"UNCHECKED".equals(status)) {
            setupText(dp(9), Typeface.NORMAL, 0xFF8D6A9E);
            canvas.drawText(status, centerX, bounds.bottom - dp(10), paint);
        }
    }

    private void setupText(float size, int style, int color) {
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(typeface == null ? Typeface.DEFAULT : Typeface.create(typeface, style));
        paint.setTextAlign(Paint.Align.CENTER);
    }

    private void drawWrapped(
            Canvas canvas,
            String text,
            float centerX,
            float baseline,
            float maxWidth,
            int maxLines,
            float lineHeight
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int start = 0;
        for (int line = 0; line < maxLines && start < text.length(); line++) {
            int count = paint.breakText(text, start, text.length(), true, maxWidth, null);
            if (count <= 0) {
                break;
            }

            int end = start + count;
            if (end < text.length() && line < maxLines - 1) {
                int space = text.lastIndexOf(' ', end);
                if (space >= start) {
                    end = space;
                }
            }

            String part = text.substring(start, end).trim();
            if (end < text.length() && line == maxLines - 1) {
                part += "…";
            }

            canvas.drawText(part, centerX, baseline + line * lineHeight, paint);
            start = Math.max(end, start + 1);
            while (start < text.length() && text.charAt(start) == ' ') {
                start++;
            }
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
