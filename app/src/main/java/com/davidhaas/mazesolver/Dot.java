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
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import static android.view.MotionEvent.INVALID_POINTER_ID;

/**
 * Name: Dot
 * Purpose: Selection dots for the bounds of the maze
 * Author: David Haas
 * Created: 2/20/18
 */

public class Dot extends View {
    private int RADIUS;
    private int mPosX;
    private int mPosY;
    private Rect boundingBox;  // The area that the user can interact with the dot
    private int feather;  // Allows for a bigger bounding box than the actual shape
    private Rect boundingScreenRect; // The rect that defines where the dot can move
    private Paint myPaint;
    private Point mLastTouch = new Point();


    private static String TAG = "Dot";

    // https://stackoverflow.com/questions/2047573/how-to-draw-filled-polygon
    public Dot(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Dot(Context context, AttributeSet attrs, int x, int y, int radius) {
        super(context, attrs);

        int maxWidth = getResources().getDisplayMetrics().widthPixels;
        int maxHeight = getResources().getDisplayMetrics().heightPixels;
        boundingScreenRect = new Rect(0, 0, maxWidth, maxHeight);

        mPosX = x;
        mPosY = y;
        this.RADIUS = radius;
        feather = (int) (RADIUS * 2.2);

        // Log.i(TAG, "Dot: radius: " + radius);
        // Log.i(TAG, "Dot: feather: " + feather);

        boundingBox = new Rect(x - (RADIUS + feather), y - (RADIUS + feather),
                x + (RADIUS + feather),
                y + (RADIUS + feather));

        // Log.i(TAG, "Dot: x: " + boundingBox.left + " y: " + boundingBox.top + " width: " + boundingBox.width() + " height: " + boundingBox.height());

        myPaint = new Paint();
        myPaint.setColor(Color.argb(255, 153, 153, 255));
        myPaint.setAntiAlias(true);
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Remember where we last started for dragging
                mLastTouch.set((int) event.getX(), (int) event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                final Point touch = new Point((int) event.getX(), (int) event.getY());

                // Calculate the change in x and y from the dragging
                int newX = mPosX + touch.x - mLastTouch.x;
                int newY = mPosY + touch.y - mLastTouch.y;

                // If it's within the bounds, translate the dot to those coordinates
                if (boundingScreenRect.contains(newX, newY)) {
                    mPosX = newX;
                    mPosY = newY;

                    boundingBox.offsetTo(mPosX - (RADIUS + feather), mPosY - (RADIUS + feather));

                    // Set this as the new "last" touch
                    mLastTouch = touch;
                }
                break;
        }
        return true;
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.drawCircle(mPosX, mPosY, RADIUS, myPaint);
        // canvas.drawRect(boundingBox, myPaint);
        // invalidate();
    }

    public void setBounds(int w, int h) {
        boundingScreenRect = new Rect(0, 0, w, h);
    }

    public Point getLocation() {
        return new Point(mPosX, mPosY);
    }

    public boolean contains(int x, int y) {
        return boundingBox.contains(x, y);
    }
}