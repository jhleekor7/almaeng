package com.example.almaeng;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.style.ReplacementSpan;

/** Draws the soft hand-marked underline beneath the emphasized hero phrase. */
final class ScribbleHighlightSpan extends ReplacementSpan {
    private final Typeface boldTypeface;
    private final float density;
    private final Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);

    ScribbleHighlightSpan(Typeface baseTypeface, float density) {
        this.boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD);
        this.density = density;
        highlight.setColor(0x59B985CB);
        highlight.setStyle(Paint.Style.FILL);
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end,
                        Paint.FontMetricsInt fontMetrics) {
        Typeface previous = paint.getTypeface();
        paint.setTypeface(boldTypeface);
        int width = (int) Math.ceil(paint.measureText(text, start, end));
        paint.setTypeface(previous);
        return width;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, Paint paint) {
        Typeface previousTypeface = paint.getTypeface();
        int previousColor = paint.getColor();
        Paint.Style previousStyle = paint.getStyle();
        paint.setTypeface(boldTypeface);
        float width = paint.measureText(text, start, end);
        float left = x - 3f * density;
        float right = x + width + 3f * density;
        float centerY = y - 1f * density;
        canvas.save();
        canvas.rotate(-1f, (left + right) / 2f, centerY);
        canvas.drawOval(new RectF(left, centerY - 3f * density,
                right, centerY + 5f * density), highlight);
        canvas.restore();
        paint.setColor(previousColor);
        paint.setStyle(previousStyle);
        canvas.drawText(text, start, end, x, y, paint);
        paint.setTypeface(previousTypeface);
    }
}
