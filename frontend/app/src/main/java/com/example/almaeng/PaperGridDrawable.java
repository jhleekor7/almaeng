package com.example.almaeng;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Warm paper background with the subtle 5dp grid from the original design. */
final class PaperGridDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    PaperGridDrawable(Context context) {
        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        paint.setShader(new LinearGradient(
                bounds.left, bounds.top, bounds.right, bounds.bottom,
                new int[]{0xFFF4EDDF, 0xFFF1EADB, 0xFFEEE5D5},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(bounds, paint);
        paint.setShader(null);
        paint.setColor(0x16FFFFFF);
        paint.setStrokeWidth(Math.max(1f, density * 0.45f));
        float step = 5f * density;
        for (float x = bounds.left; x < bounds.right; x += step) {
            canvas.drawLine(x, bounds.top, x, bounds.bottom, paint);
        }
        for (float y = bounds.top; y < bounds.bottom; y += step) {
            canvas.drawLine(bounds.left, y, bounds.right, y, paint);
        }
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter colorFilter) { }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
