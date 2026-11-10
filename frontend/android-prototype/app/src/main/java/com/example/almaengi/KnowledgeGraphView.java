
package com.example.almaengi;

import android.content.Context;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;

public class KnowledgeGraphView extends View {

    Paint paint = new Paint();

    String[] nodes = {
            "기본 개념",
            "핵심 원리",
            "구성 요소",
            "활용 방법",
            "응용 사례"
    };

    public KnowledgeGraphView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;

        float[] x = {
                centerX, centerX - 150, centerX + 150,
                centerX - 100, centerX + 100
        };

        float[] y = {
                100, 230, 230, 400, 400
        };

        paint.setStrokeWidth(5);
        paint.setColor(Color.GRAY);

        canvas.drawLine(x[0], y[0], x[1], y[1], paint);
        canvas.drawLine(x[0], y[0], x[2], y[2], paint);
        canvas.drawLine(x[1], y[1], x[3], y[3], paint);
        canvas.drawLine(x[2], y[2], x[4], y[4], paint);

        for (int i = 0; i < nodes.length; i++) {
            paint.setColor(Color.rgb(100, 180, 240));
            canvas.drawCircle(x[i], y[i], 65, paint);

            paint.setColor(Color.BLACK);
            paint.setTextSize(24);
            paint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText(nodes[i], x[i], y[i] + 8, paint);
        }
    }
}
