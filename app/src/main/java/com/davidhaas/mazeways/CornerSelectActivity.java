package com.davidhaas.mazeways;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;

/**
 * Name: CornerSelectActivity
 * Purpose: Provides an interface for the user to select the bounds of the maze
 * @author  David Haas
 * @since   2/16/18
 */

public class CornerSelectActivity extends Activity {

    private static final String TAG = "CornerSelectActivity";
    public static final String CORNERS = "corners";

    /**
     * Instantiates the UI elements and displays the selection box.
     * @param savedInstanceState The saved instance state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bitmap image = null;

        // Removes the title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_corner_select);

        Button solveMazeButton = findViewById(R.id.solveMaze);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/press_start_2p.ttf");
        solveMazeButton.setTypeface(font);

        // Loads the intent image as a bitmap for processing
        final Uri imgUri = Uri.parse(getIntent().getStringExtra(MainActivity.IMAGE_URI));
        try {
             image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "onCreate: Error loading image", e);
        }

        // Rotate the image if its wider than it is long
        if(image.getWidth() > image.getHeight())
            image = rotateBitmap(image, 90);

        // Sets the bitmap
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(image);

        // Gets the ratio between the appeared image height and the actual height so we can map
        // screen touches to pixels on the image
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        final double view_scale_ratio = (double) image.getWidth() / displayMetrics.widthPixels;
        final int scaledImageHeight = (int) (image.getHeight() / view_scale_ratio);

        // The amount of the image that gets cropped off the top and bottom due to scaling
        final int croppedTops;
        if ((scaledImageHeight - displayMetrics.heightPixels) / 2 < 0)
            croppedTops = 0;
        else
            croppedTops = (scaledImageHeight - displayMetrics.heightPixels) / 2;

        // Initializes the selection box
        final SelectionBox selectionBox = findViewById(R.id.selectionBox);
        if (displayMetrics.heightPixels < scaledImageHeight) // Chooses the smaller of image height or display height
            selectionBox.setImageBounds(displayMetrics.widthPixels, displayMetrics.heightPixels);
        else
            selectionBox.setImageBounds(displayMetrics.widthPixels, scaledImageHeight);


        solveMazeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // Grabs the corners from the selection box
                int[][] corners = CVUtils.orderPoints(selectionBox.getCornerCoords());

                // Scales the corners then adjusts the y coordinates to account for the top that is
                // cropped of due to CENTER_CROP scaling on the imageView
                for (int i = 0; i < 4; i++) {
                    corners[i][0] *= view_scale_ratio;
                    corners[i][1] *= view_scale_ratio;
                    corners[i][1] += croppedTops*view_scale_ratio;
                }
                Log.i(TAG, "onClick: Corners post: \n" + printArr(corners));

                // Sends an intent to the SolutionActivity class with the corners and img path
                Intent mIntent = new Intent(CornerSelectActivity.this, SolutionActivity.class);
                Bundle mBundle = new Bundle();
                mBundle.putSerializable(CORNERS, corners);
                mBundle.putString(MainActivity.IMAGE_URI, imgUri.toString());
                mIntent.putExtras(mBundle);

                startActivity(mIntent);
            }
        });

    }

    /**
     * Rotates a bitmap image by a specified angle.
     * @param source The bitmap to be rotated
     * @param angle The angle to rotate the bitmap at
     * @return The rotated bitmap
     */
    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Converts a 2d array to a String nicely.
     * @param arr The 2d array to print
     * @return A String representation of the 2d array
     */
    public String printArr(int[][] arr) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (int q = 0; q < arr.length; q++) {
            if (q > 0)
                sb.append("\t");
            sb.append("[");
            for (int h = 0; h < arr[q].length - 1; h++) {
                sb.append(arr[q][h]);
                sb.append(", ");
            }
            sb.append(arr[q][arr[q].length - 1]);
            sb.append("],\n");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append(" ]");
        return sb.toString();
    }
}