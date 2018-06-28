package com.davidhaas.mazesolver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class SelectionBox extends View {
    private static final String TAG = "SelectionBoxView";
    private Dot[] corners = new Dot[4];
    private Paint bPaint;

    public SelectionBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        final int radius = 30;
        bPaint = new Paint();
        bPaint.setColor(Color.argb(210, 204, 230, 153));
        bPaint.setStyle(Paint.Style.STROKE);
        bPaint.setStrokeWidth(8);

        // backwards c shape: tl, tr, br, bl
        corners[0] = new Dot(context, attrs, 300, 300, radius);
        corners[1] = new Dot(context, attrs, 600, 300, radius);
        corners[2] = new Dot(context, attrs, 600, 600, radius);
        corners[3] = new Dot(context, attrs, 300, 600, radius);
    }

    public boolean onTouchEvent(MotionEvent event) {
        //int action = event.getAction();
        // Log.i(TAG, "onTouchEvent: Touched!");
        for (int i = 0; i < 4; i++) {
            corners[i].onTouchEvent(event);
        }
        return true;
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Draws the lines connecting the dots
        Path borders = new Path();
        borders.reset(); // only needed when reusing this path for a new build

        Point loc, topLeft = corners[0].getLocation();
        borders.moveTo(topLeft.x, topLeft.y); // used for first point

        for (int i = 1; i < 4; i++) {
            loc = corners[i].getLocation();
            borders.lineTo(loc.x, loc.y);
        }

        borders.lineTo(topLeft.x, topLeft.y); // there is a setLastPoint action but i found it not to work as expected

        canvas.drawPath(borders, bPaint);

        // Draws each of the dots
        for (int i = 0; i < 4; i++) {
            corners[i].draw(canvas);
        }
        invalidate();
    }

    // Returns the x-y coordinates of the corners
    public int[][] getCornerCoords() {
        int[][] coords = new int[4][2];
        Point coord;

        for (int i = 0; i < 4; i++) {
            coord = corners[i].getLocation();
            coords[i] = new int[]{coord.x, coord.y};
        }

        return coords;
    }

    public void hideView() {
        setVisibility(View.GONE);
    }
}
