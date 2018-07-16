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
 * @author  David Haas
 * @since   2/20/18
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

    /**
     * Initializes the Dot.
     * @param context The context.
     * @param attrs Any attributes.
     */
    public Dot(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Initializes the Dot.
     * @param context The context.
     * @param attrs Any attributes.
     * @param x The x coordinate of the center of the dot
     * @param y The y coordinate of the center of the dot
     * @param radius The radius of the dot
     */
    public Dot(Context context, AttributeSet attrs, int x, int y, int radius) {
        super(context, attrs);

        int maxWidth = getResources().getDisplayMetrics().widthPixels;
        int maxHeight = getResources().getDisplayMetrics().heightPixels;
        boundingScreenRect = new Rect(0, 0, maxWidth, maxHeight);

        mPosX = x;
        mPosY = y;
        this.RADIUS = radius;
        feather = (int) (RADIUS * 2.2);

        boundingBox = new Rect(x - (RADIUS + feather), y - (RADIUS + feather),
                x + (RADIUS + feather),
                y + (RADIUS + feather));

        myPaint = new Paint();
        myPaint.setColor(Color.argb(255, 84,110,122));
        myPaint.setAntiAlias(true);
    }

    /**
     * Moves the dot when it's touched and dragged.
     * @param event The touch event
     */
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

    /**
     * Draws the Dot
     * @param canvas The canvas to draw the Dot on.
     */
    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.drawCircle(mPosX, mPosY, RADIUS, myPaint);
        // canvas.drawRect(boundingBox, myPaint);
        // invalidate();
    }

    /**
     * Sets the bounds in which the Dot can move
     * @param width The width that the dots may move from (0 to width)
     * @param height The height that the dots may move from (0 to height)
     */
    public void setBounds(int width, int height) {
        boundingScreenRect = new Rect(0, 0, width, height);
    }

    /**
     * Gets the location of the Dot.
     * @return The location of the Dot
     */
    public Point getLocation() {
        return new Point(mPosX, mPosY);
    }

    /**
     * See if the feathered bounding box for the Dot contains a given point.
     * @param x The x-coordinate
     * @param y The x-coordinate
     * @return Whether or not the dot's bounding box contains the coordinates
     */
    public boolean contains(int x, int y) {
        return boundingBox.contains(x, y);
    }
}