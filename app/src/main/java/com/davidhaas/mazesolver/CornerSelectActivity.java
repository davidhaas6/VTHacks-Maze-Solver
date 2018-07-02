package com.davidhaas.mazesolver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;

/**
 * Name: CornerSelectActivity
 * Purpose: Provides an interface for the user to select the bounds of the maze
 * Author: David Haas
 * Last updated: 6/28/18
 */

public class CornerSelectActivity extends Activity {

    private static final String TAG = "CornerSelectActivity";
    public static final String CORNERS = "corners";
    Bitmap image;
    ImageView imageView;
    SelectionBox selectionBox;
    Button solveMazeButton;
    String imgPath;


    private int[][] corners;
    private double view_scale_ratio;
    int vert_offset;
    //private final int scale = 1;

    //TODO: Move solution etc into new activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Removes the title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_corner_select);
        selectionBox = findViewById(R.id.selectionBox);
        solveMazeButton = findViewById(R.id.solveMaze);

        // Loads the intent image as a bitmap for processing
        Uri imgUri = Uri.parse(getIntent().getStringExtra(MainActivity.IMAGE_URI));
        try {
            image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "onCreate: Error loading image", e);
        }

        image = rotateBitmap(image, 90);

        // Scales and set the bitmap
        //TODO: Maybe rescale it in SolutionActivity?
        image = getResizedBitmap(image, .5f); // For efficiency
        imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(image);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Gets the ratio between the appeared image height and the actual height so we can map
        // screen touches to pixels on the image
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        vert_offset = getStatusBarHeight();  // Finds the vertical offset on the image due to the menu bar

        view_scale_ratio = (double) image.getHeight() / displayMetrics.heightPixels;
        Log.i(TAG, "onCreate: Screen Height: " + displayMetrics.heightPixels);
        Log.i(TAG, "onCreate: getStatusBarHeight: " + getStatusBarHeight());


        corners = new int[4][2];  // Initializes the variables to store the corners of the maze

        solveMazeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // Closes the selection box and button
                selectionBox.hideView();
                solveMazeButton.setVisibility(View.GONE);

                corners = CVUtils.orderPoints(selectionBox.getCornerCoords());

                for (int i = 0; i < 4; i++) {
                    corners[i][0] *= view_scale_ratio;
                    corners[i][1] *= view_scale_ratio;
                    corners[i][1] += 25; //TODO: Where does this come from??
                }

                // Sends an intent to the SolutionActivity class with the corners and img path
                Intent mIntent = new Intent(CornerSelectActivity.this, SolutionActivity.class);
                Bundle mBundle = new Bundle();
                mBundle.putSerializable(CORNERS, corners);
                mBundle.putString(MainActivity.IMAGE_URI, imgPath);
                mIntent.putExtras(mBundle);

                startActivity(mIntent);
            }
        });

    }

    public Bitmap getResizedBitmap(Bitmap bm, float scale) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scale,scale);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}