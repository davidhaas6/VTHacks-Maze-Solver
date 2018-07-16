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

/**
 * Name: SelectionBox
 * Purpose: A view that allows the user to manipulate the selection bounds for the maze
 * @author  David Haas
 * @since  2/20/18
 */

public class SelectionBox extends View {
    private static final String TAG = "SelectionBoxView";
    private Dot[] corners = new Dot[4];
    private Paint bPaint;

    /**
     * Initializes the SelectionBox and its Dots.
     * @param context The context
     * @param attrs Any attributes
     */
    public SelectionBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        final int radius = 30;
        bPaint = new Paint();
        bPaint.setColor(Color.argb(210, 144,164,174));
        bPaint.setStyle(Paint.Style.STROKE);
        bPaint.setStrokeWidth(8);

        // backwards c shape: tl, tr, br, bl
        corners[0] = new Dot(context, attrs, 300, 300, radius);
        corners[1] = new Dot(context, attrs, 600, 300, radius);
        corners[2] = new Dot(context, attrs, 600, 600, radius);
        corners[3] = new Dot(context, attrs, 300, 600, radius);
    }

    /**
     * Takes the touches and forwards them on to the Dots that were touched
     * @param event The touch event
     */
    public boolean onTouchEvent(MotionEvent event) {
        //int action = event.getAction();
        // Log.i(TAG, "onTouchEvent: Touched!");
        for (int i = 0; i < 4; i++) {
            if(corners[i].contains((int) event.getX(), (int) event.getY())) {
                corners[i].onTouchEvent(event);
                break;
            }
        }
        return true;
    }

    /**
     * Draws the view and connects the Dots.
     * @param canvas The canvas to draw on
     */
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

    /**
     * Returns the x-y coordinates of the four corners.
     * @return The x-y coordinates of the four corners
     */
    public int[][] getCornerCoords() {
        int[][] coords = new int[4][2];
        Point coord;

        for (int i = 0; i < 4; i++) {
            coord = corners[i].getLocation();
            coords[i] = new int[]{coord.x, coord.y};
        }

        return coords;
    }

    /**
     * Sets the width and height in which the dots of the selection box can move in. The method then
     * forwards this information on to the dots.
     * @param width The width that the dots may move from (0 to width)
     * @param height The height that the dots may move from (0 to height)
     */
    public void setImageBounds(int width, int height){
        for (int i = 0; i < corners.length; i++)
            corners[i].setBounds(width, height);
    }
}
