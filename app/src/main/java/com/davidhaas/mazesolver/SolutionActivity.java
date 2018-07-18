package com.davidhaas.mazesolver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.davidhaas.mazesolver.pathfinding.Asolution;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.wang.avi.AVLoadingIndicatorView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.graphics.Point;

/**
 * Name: SolutionActivity
 * Purpose: Parses the maze image into an array then computes and displays its solution.
 * @author  David Haas
 * @since   6/28/18
 */
public class SolutionActivity extends Activity {
    // TODO: Remove OpenCV dependencies https://developer.android.com/reference/android/graphics/Matrix, https://goo.gl/Qh8THg

    private static final String TAG = "SolutionActivity";
    private final int SCALE_FACTOR = 2; // The amount the maze scales down before using A*
    private final int MAZE_SOLVED = 1, MAZE_NOT_SOLVED = 0, IMG_DEBUG = -1;
    private final long MIN_LOAD_TIME = 1000; // The min time to show the loading icon
    private Point mazeCorner;
    private ImageView imageView;
    private TextView loadingText;
    private TextView failText;
    private AVLoadingIndicatorView loadingIcon;
    private Button backButton;
    private Handler mHandler;
    private FirebaseAnalytics mFirebaseAnalytics;

    /**
     * Instantiates the UI elements and starts the solution thread.
     * @param savedInstanceState The saved instance state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Removes the title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_solution);

        imageView = findViewById(R.id.imageView);
        loadingText = findViewById(R.id.loadingText);
        loadingIcon = findViewById(R.id.loadingIcon);
        failText = findViewById(R.id.failText);
        backButton = findViewById(R.id.backButton);

        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/press_start_2p.ttf");
        loadingText.setTypeface(font);
        failText.setTypeface(font);
        backButton.setTypeface(font);

        failText.setVisibility(View.INVISIBLE);
        backButton.setVisibility(View.INVISIBLE);
        startLoading();

        // Loads the intent
        final Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        final int[][] corners = (int[][]) bundle.getSerializable(CornerSelectActivity.CORNERS);

        // Loads the intent image as a bitmap for processing
        Uri imgUri = Uri.parse(bundle.getString(MainActivity.IMAGE_URI));
        try {
            Bitmap image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);

            // Rotate the image if its wider than it is long
            if (image.getWidth() > image.getHeight())
                image = rotateBitmap(image, 90);

            mHandler = new MazeUIHandler(Looper.getMainLooper(), image);

            Runnable solnRunnable = new SolutionRunnable(image, corners);
            new Thread(solnRunnable).start();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "onCreate: Error loading image", e);
        }

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    /**
     * The handler that interacts with the solution thread to display the maze's results in the main
     * UI thread.
     */
    private class MazeUIHandler extends Handler {
        private Bitmap image;
        private boolean debugging;

        /**
         * The constructor for the UI handler.
         * @param myLooper The main thread's looper.
         * @param image The image containing the maze.
         */
        private MazeUIHandler(Looper myLooper, Bitmap image) {
            super(myLooper);
            this.image = image;
            debugging = false;
        }

        /**
         * Receives the solution thread's updates and displays the results on the main UI.
         * @param msg The message containing the maze's results.
         */
        public void handleMessage(Message msg) {
            int state = msg.what;

            switch (state) {
                case MAZE_SOLVED:
                    if (!debugging) {
                        Bundle b = (Bundle) msg.obj;
                        Stack<int[]> path = (Stack<int[]>) b.getSerializable("path");
                        int[][] binaryMaze = (int[][]) b.getSerializable("binary");

                        stopLoading();
                        drawSolution(path, binaryMaze, image);
                        backButton.setVisibility(View.VISIBLE);

                        Bundle fireB = new Bundle();
                        fireB.putString("RESULT", "MAZE_NOT_SOLVED");
                        mFirebaseAnalytics.logEvent("MAZE_PROCESSED", fireB);
                    }
                    break;
                case MAZE_NOT_SOLVED:
                    if (!debugging) {
                        stopLoading();
                        failText.setVisibility(View.VISIBLE);
                        backButton.setVisibility(View.VISIBLE);

                        Bundle fireB = new Bundle();
                        fireB.putString("RESULT", "MAZE_NOT_SOLVED");
                        mFirebaseAnalytics.logEvent("MAZE_PROCESSED", fireB);
                    }
                    break;
                case IMG_DEBUG:
                    debugging = true;

                    stopLoading();

                    Bundle b1 = (Bundle) msg.obj;
                    image = b1.getParcelable("img");
                    Log.i(TAG, "handleMessage: displaying debug");

                    imageView.setImageBitmap(image);
                    backButton.setVisibility(View.VISIBLE);
                    break;

            }
        }
    }

    /**
     * A thread to solve the maze in the background while the loading icon is being displayed.
     */
    private class SolutionRunnable implements Runnable {
        Bitmap image;
        int[][] corners;

        /**
         * The constructor for the solution runnable thread
         * @param image The image containing the maze
         * @param corners The four corners that define the user's selected region
         */
        private SolutionRunnable(Bitmap image, int[][] corners) {
            // store parameter for later user
            this.image = image;
            this.corners = corners;
        }

        /**
         * Crops the maze, converts it to a binary array, and runs A* over it to find the solution.
         * Sends the path and the maze to mHandler to be drawn in the main thread.
         */
        public void run() {
            int state;
            final long startTime = System.currentTimeMillis();

            Mat croppedMaze = getCroppedMaze(corners, image);
            int[][] croppedBinaryMaze = CVUtils.getBinaryArray(croppedMaze);

            // Runs A* on the maze and gets the solution stack
            Stack<int[]> solution = null;
            try {
                Asolution mySol = new Asolution(croppedBinaryMaze);
                solution = mySol.getPath();

                if (solution == null)
                    state = MAZE_NOT_SOLVED;
                else
                    state = MAZE_SOLVED;

            } catch (Exception e) {
                Log.e(TAG, "run: Exception when solving maze: ", e);
                state = MAZE_NOT_SOLVED;

                Bundle fireB = new Bundle();
                fireB.putString("RESULT", "ERROR_SOLVING_MAZE");
                fireB.putString(FirebaseAnalytics.Param.VALUE, e.getMessage());
                mFirebaseAnalytics.logEvent("MAZE_PROCESSED", fireB);
            }

            final long executionTime = System.currentTimeMillis() - startTime;
            Log.i(TAG, "run: Execution time: " + executionTime + " ms");

            if (executionTime < MIN_LOAD_TIME) // Forces the loading icon showing for MIN_LOAD_TIME
                android.os.SystemClock.sleep(MIN_LOAD_TIME - executionTime);

            Bundle fireB = new Bundle();
            fireB.putString(FirebaseAnalytics.Param.VALUE, executionTime/1000 + " s");
            mFirebaseAnalytics.logEvent("SOLVE_TIME", fireB);

            Bundle b = new Bundle();
            b.putSerializable("path", solution);
            b.putSerializable("binary", croppedBinaryMaze);
            Message completeMessage = mHandler.obtainMessage(state, b);

            completeMessage.sendToTarget();
        }
    }

    /**
     * Hides the loading icon and text.
     */
    private void stopLoading() {
        loadingIcon.setVisibility(View.INVISIBLE);
        loadingText.setVisibility(View.INVISIBLE);
    }

    /**
     * Shows the loading icon and text.
     */
    private void startLoading() {
        loadingIcon.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
    }

    /**
     * A debug method to prematurely send a Mat to mHandler. This is used to view the Mat of the maze
     *  in the solution thread before it is solved.
     * @param mat The Mat to be viewed.
     */
    private void sendMat2Handler(Mat mat) {
        Bundle b = new Bundle();
        b.putParcelable("img", CVUtils.mat2BMP(mat));
        Message completeMessage = mHandler.obtainMessage(IMG_DEBUG, b);
        completeMessage.sendToTarget();
    }

    /**
     * Locates the maze in the selected region, crops it and downscales it.
     * @param corners The four corners that define the user's selected region
     * @param image The image containing the maze
     * @return The cropped and binary-ized maze
     */
    private Mat getCroppedMaze(int[][] corners, Bitmap image) {
        // Converts the bitmap to an OpenCV matrix
        Mat img_matrix = new Mat();
        Bitmap bmp32 = image.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, img_matrix);

        // Convert to gray, blur, and threshold.
        Imgproc.cvtColor(img_matrix.clone(), img_matrix, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(img_matrix.clone(), img_matrix, new Size(11, 11), 0);
        Imgproc.adaptiveThreshold(img_matrix.clone(), img_matrix, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 55, 5);

        // Crops the image AFTER the thresholding to avoid those border lines
        img_matrix = CVUtils.cropQuadrilateral(img_matrix, corners);

        // Gets the two contours with the longest perimeter. Since mazes can have a line drawn
        // through them that splits them in half (the solution), the maze is actually two separate
        // objects, necessitating us to retrieve two contours instead of one.
        List<MatOfPoint> mazePerim = CVUtils.largest2PerimContours(img_matrix);

        if (mazePerim != null) {
            // Get the bounding rects
            ArrayList<Rect> rects = new ArrayList<>();
            for (MatOfPoint c : mazePerim)
                rects.add(Imgproc.boundingRect(c));

            // Combine the bounding rects of the maze perimeter into one, encompassing bounding rect
            Rect combined = CVUtils.combineRects(rects.get(0), rects.get(1));

            // Contracts the bounding box to help eliminate whitespace on the edge of the maze
            final int contract_px = 3;
            combined.x += contract_px;
            combined.y += contract_px;
            combined.width -= contract_px*2;
            combined.height -= contract_px*2;

            int[][] bounds = new int[][]{
                    {combined.x, combined.y},
                    {combined.x + combined.width, combined.y},
                    {combined.x + combined.width, combined.y + combined.height},
                    {combined.x, combined.y + combined.height}
            };

            //img_matrix = CVUtils.cropQuadrilateral(img_matrix, bounds);
            img_matrix = img_matrix.submat(combined);

            // Find the lowest x and y coords because that will define the rect that the first pass
            // cropped maze was inside of
            int lowestX = corners[0][0], lowestY = corners[0][1];
            for (int[] p : corners) {
                if (p[0] < lowestX)
                    lowestX = p[0];
                if (p[1] < lowestY)
                    lowestY = p[1];
            }

            // The point in which the newly cropped maze lies in the original image.
            mazeCorner = new Point(combined.x + lowestX, combined.y + lowestY);
        }

        // Resize the image
        Size dstSize = new Size(img_matrix.width() / SCALE_FACTOR, img_matrix.height() / SCALE_FACTOR);
        Mat dst = new Mat();
        Imgproc.resize(img_matrix, dst, dstSize, 1, 1, Imgproc.INTER_AREA);
        img_matrix = dst;
        Log.i(TAG, "getCroppedMaze: Scaled image size: " + dstSize);

        Imgproc.adaptiveThreshold(img_matrix, img_matrix, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 25, 30);

        return img_matrix;
    }


    /**
     * Overlays the solution on top of the original maze image.
     * @param path The solution of the maze
     * @param mazetrix The binary array representing the maze
     * @param image The original image containing the maze
     */
    private void drawSolution(Stack<int[]> path, int[][] mazetrix, Bitmap image) {
        // Sets the value of the solution pixels to 2
        for (int[] coords : path) {
            mazetrix[coords[0]][coords[1]] = 2;
        }

        int[][] pixOut = new int[mazetrix.length][mazetrix[0].length];

        // The radius that the path "puffs" out in
        final int bloomAmount = (int) ((mazetrix.length * mazetrix[0].length) * Math.pow(SCALE_FACTOR, 2) / 50000);
        Log.i(TAG, "drawSolution: bloom: " + bloomAmount);

        // Colors the solution
        final int opaque = (int) ((long) 0xff << 24) | 0xff << 16; // Encodes it in hexadecimal sRGB color space
        final int translucent = (0xff) << 16;
        for (int i = 0; i < mazetrix.length; i++) {
            for (int j = 0; j < mazetrix[0].length; j++) {

                // If pixel is part of the solution, make the color opaque red, otherwise transparent
                if (mazetrix[i][j] == 2) {
                    pixOut[i][j] = opaque;

                    // Draw the bloom
                    int y;
                    for (int x = -bloomAmount; x <= bloomAmount; x++) {
                        y = (int) Math.round(Math.sqrt(bloomAmount * bloomAmount - x * x));
                        for (int k = -y; k <= y; k++) {
                            if (0 <= i + k && i + k < mazetrix.length && 0 <= j + x && j + x < mazetrix[0].length)
                                if (mazetrix[i + k][j + x] == 0)
                                    pixOut[i + k][j + x] = opaque;
                                else if (mazetrix[i + k][j + x] == 1)
                                    break;
                        }
                    }
                } else {
                    pixOut[i][j] = translucent;
                }
            }
        }

        // Create a bitmap out of the solution and scale it according to SCALE_FACTOR
        int[] pixels = get1DArray(pixOut);
        Bitmap solution = Bitmap.createBitmap(pixels, mazetrix[0].length, mazetrix.length, Bitmap.Config.ARGB_8888);
        solution = Bitmap.createScaledBitmap(
                solution,
                solution.getWidth() * SCALE_FACTOR,
                solution.getHeight() * SCALE_FACTOR,
                true
        );

        // Overlay the image
        Bitmap out = putOverlay(image, solution, mazeCorner.x, mazeCorner.y);
        imageView.setImageBitmap(out);
    }

    /**
     * Overlays on bitmap on top of another.
     * @param base The base image
     * @param overlay The overlaid image
     * @param x The x-coordinate of where the top left corner of overlay is placed onto base
     * @param y The y-coordinate of where the top left corner of overlay is placed onto base
     * @return The bitmap of the overlay imaged on top of the base
     */
    private Bitmap putOverlay(Bitmap base, Bitmap overlay, int x, int y) {
        // Copy the bmp to ensure that it's mutable
        Bitmap baseCpy = base.copy(Bitmap.Config.ARGB_8888, true);

        Paint p = new Paint();
        p.setColor(Color.RED);

        // Draw the solution
        Canvas canvas = new Canvas(baseCpy);
        canvas.drawBitmap(overlay, x, y, null);

        return baseCpy;
    }

    /**
     * Converts a 2D array into a 1D array.
     * @param arr The inputted 2D array to be converted to 1D
     * @return The 1D version of the inputted 2D array
     */
    private int[] get1DArray(int[][] arr) {
        int[] ret = new int[arr.length * arr[0].length];
        int count = 0;

        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[0].length; j++) {
                ret[count] = arr[i][j];
                count += 1;
            }
        }
        return ret;
    }

    /**
     * Rotates a bitmap image by a specified angle.
     * @param source The bitmap to be rotated
     * @param angle The angle to rotate the bitmap at
     * @return The rotated bitmap
     */
    private static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void displayMat(Mat mat) {
        Bitmap bmp = CVUtils.mat2BMP(mat);
        if (bmp != null)
            imageView.setImageBitmap(bmp);
        else
            Log.e(TAG, "displayMat: Bitmap is null!");
    }
}
