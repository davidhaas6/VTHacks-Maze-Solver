package com.davidhaas.mazesolver;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Name: Dot
 * Purpose: Selection dots for the bounds of the maze
 * Author: David Haas
 * Last updated: 6/28/18
 */

public class Dot extends View {
    private int RADIUS;
    private int x;
    private int y;
    private Rect boundingBox;  // The area that the user can interact with the dot
    private int feather;  // Allows for a bigger bounding box than the actual shape
    private int initialX;
    private int initialY;
    private int offsetX;
    private int offsetY;
    private int maxWidth;
    private int maxHeight;
    private Paint myPaint;


    private static String TAG = "Dot";

    // https://stackoverflow.com/questions/2047573/how-to-draw-filled-polygon
    public Dot(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Dot(Context context, AttributeSet attrs, int x, int y, int radius) {
        super(context, attrs);

        this.x = x;
        this.y = y;
        maxWidth = getResources().getDisplayMetrics().widthPixels;
        maxHeight = getResources().getDisplayMetrics().heightPixels;
        Log.i(TAG, "Dot: w,h: " + maxWidth + " " + maxHeight);
        this.RADIUS = radius;
        feather = (int) (RADIUS * 2.2);

        // Log.i(TAG, "Dot: radius: " + radius);
        // Log.i(TAG, "Dot: feather: " + feather);

        boundingBox = new Rect(x - (RADIUS + feather), y - (RADIUS + feather),
                x + (RADIUS + feather),
                y + (RADIUS + feather));

        // Log.i(TAG, "Dot: x: " + boundingBox.left + " y: " + boundingBox.top + " width: " + boundingBox.width() + " height: " + boundingBox.height());

        myPaint = new Paint();
        myPaint.setColor(Color.argb(255, 153,153,255));
        myPaint.setAntiAlias(true);
    }

    //TODO: Add more robust safeguards for preventing x,y < 0
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        Point touch;
        //Log.i(TAG, "onTouchEvent: Touched!");
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touch = new Point((int) event.getX(), (int) event.getY());
                // Log.i(TAG, "onTouchEvent: " + touch);

                // If the touch is within the dot, record the initial starting points
                if (boundingBox.contains(touch.x, touch.y)) {
                    initialX = x;
                    initialY = y;
                    offsetX = touch.x;
                    offsetY = touch.y;
                }

                break;

            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: //TODO: Probably not the right event to use?
                touch = new Point((int) event.getX(), (int) event.getY());
                if (boundingBox.contains(touch.x, touch.y)) {
                    //Log.i(TAG, "onTouchEvent: " + touch);
                    //Log.i(TAG, "onTouchEvent: " + boundingBox);

                    // Drags the dot
                    x = initialX + touch.x - offsetX;
                    y = initialY + touch.y - offsetY;
                    if (x < 0)
                        x = 0;
                    else if (x > maxWidth)
                        x = maxWidth;
                    if (y < 0)
                        y = 0;
                    else if (y > maxHeight)
                        y = maxHeight;

                    boundingBox.offsetTo(x - (RADIUS + feather), y - (RADIUS + feather));
                }
                break;
        }
        return (true);
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.drawCircle(x, y, RADIUS, myPaint);
        // canvas.drawRect(boundingBox, rPaint);
        // invalidate();
    }

    public void setBounds(int w, int h) {
        maxWidth = w;
        maxHeight = h;
    }

    public Point getLocation() {
        return new Point(x,y);
    }
}