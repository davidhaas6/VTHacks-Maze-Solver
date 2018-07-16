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
import com.wang.avi.AVLoadingIndicatorView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.LogisticRegression;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.graphics.Point;

/**
 * Name: SolutionActivity
 * Purpose: Displays the solution of the maze.
 * Author: David Haas
 * Created: 6/28/18
 */
public class SolutionActivity extends Activity {
    // TODO: Add loading button while program is solving maze
    // TODO: Remove OpenCV dependencies https://developer.android.com/reference/android/graphics/Matrix, https://goo.gl/Qh8THg
    //findViewById(R.id.loadingPanel).setVisibility(View.GONE);

    private static final String TAG = "SolutionActivity";
    private final int SCALE_FACTOR = 2; // The amount the maze scales down before using A*
    private final int MAZE_SOLVED = 1, MAZE_NOT_SOLVED = 0, IMG_DEBUG = -1;
    private final long MIN_LOAD_TIME = 1000;
    private Point mazeCorner; // TODO: Can you get rid of this global variable?
    private ImageView imageView;
    private TextView loadingText;
    private TextView failText;
    private AVLoadingIndicatorView loadingIcon;
    private Button backButton;
    private Handler mHandler;
    private Runnable solnRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

            mHandler = new MyHandler(Looper.getMainLooper(), image);

            solnRunnable = new SolutionRunnable(image, corners);
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

    private class MyHandler extends Handler {
        private Bitmap image;
        private boolean debugging;

        private MyHandler(Looper myLooper, Bitmap mazeImg) {
            super(myLooper);
            this.image = mazeImg;
            debugging = false;
        }

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
                    }
                    break;
                case MAZE_NOT_SOLVED:
                    if (!debugging) {
                        stopLoading();
                        failText.setVisibility(View.VISIBLE);
                        backButton.setVisibility(View.VISIBLE);
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

    private class SolutionRunnable implements Runnable {
        Bitmap image;
        int[][] corners;

        private SolutionRunnable(Bitmap image, int[][] corners) {
            // store parameter for later user
            this.image = image;
            this.corners = corners;
        }

        public void run() {
            int state;
            final long startTime = System.currentTimeMillis();

            Mat croppedMaze = getCroppedMaze(corners, image);
            int[][] croppedBinaryMaze = CVUtils.getBinaryArray(croppedMaze);

            // Runs A* on the maze and gets the solution stack
            //TODO: Entrance-finding doesn't work if they're on the top and bottom
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
            }

            final long executionTime = System.currentTimeMillis() - startTime;
            Log.i(TAG, "run: Execution time: " + executionTime + " ms");

            if (executionTime < MIN_LOAD_TIME) // Forces the loading icon showing for MIN_LOAD_TIME
                android.os.SystemClock.sleep(MIN_LOAD_TIME - executionTime);

            Bundle b = new Bundle();
            b.putSerializable("path", solution);
            b.putSerializable("binary", croppedBinaryMaze);
            Message completeMessage = mHandler.obtainMessage(state, b);

            completeMessage.sendToTarget();
        }
    }

    private void stopLoading() {
        loadingIcon.setVisibility(View.INVISIBLE);
        loadingText.setVisibility(View.INVISIBLE);
    }

    private void startLoading() {
        loadingIcon.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
    }

    private void sendMat2Handler(Mat mat) {
        Bundle b = new Bundle();
        b.putParcelable("img", mat2BMP(mat));
        Message completeMessage = mHandler.obtainMessage(IMG_DEBUG, b);
        completeMessage.sendToTarget();
    }

    // Crops the image in a rectangle bounding the corners and applies a threshold to obtain binary
    // values. 1s represent walls and 0s are paths.
    private Mat getCroppedMaze(int[][] corners, Bitmap image) {
        // Converts the bitmap to an OpenCV matrix
        Mat img_matrix = new Mat();
        Bitmap bmp32 = image.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, img_matrix);

        Imgproc.cvtColor(img_matrix.clone(), img_matrix, Imgproc.COLOR_RGB2GRAY);

        Imgproc.GaussianBlur(img_matrix.clone(), img_matrix, new Size(11, 11), 0);

        //TODO: consider making blockSize and C based off of image size?
        Imgproc.adaptiveThreshold(img_matrix.clone(), img_matrix, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 55, 5);

        // Crops the image AFTER the thresholding to avoid those border lines
        img_matrix = CVUtils.cropQuadrilateral(img_matrix, corners);

        //sendMat2Handler(img_matrix);

        List<MatOfPoint> mazePerim = CVUtils.largest2PerimContours(img_matrix);
        if (mazePerim != null) {
            ArrayList<Rect> rects = new ArrayList<>();
            for (MatOfPoint c : mazePerim)
                rects.add(Imgproc.boundingRect(c));
            //displayMatCnts(img_matrix, mazePerim);

            Rect combined = CVUtils.combineRects(rects.get(0), rects.get(1));
            rects.clear();
            rects.add(combined);

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

            //sendMat2Handler(drawCnts(img_matrix, mazePerim));

            img_matrix = CVUtils.cropQuadrilateral(img_matrix, bounds);

            //sendMat2Handler(img_matrix);


            // Find the lowest x and y coord because that will define the rect that the first pass
            // cropped maze was in
            int lowestX = corners[0][0], lowestY = corners[0][1];
            for (int[] p : corners) {
                if (p[0] < lowestX)
                    lowestX = p[0];
                if (p[1] < lowestY)
                    lowestY = p[1];
            }

            mazeCorner = new Point(combined.x + lowestX, combined.y + lowestY);
        }


        // Resize the image
        Size dstSize = new Size(img_matrix.width() / SCALE_FACTOR, img_matrix.height() / SCALE_FACTOR);
        Mat dst = new Mat();
        Imgproc.resize(img_matrix, dst, dstSize, 1, 1, Imgproc.INTER_AREA);
        img_matrix = dst;
        Log.i(TAG, "getCroppedMaze: Scaled image size: " + dstSize);

        //TODO: consider making blockSize and C based off of image size?
        Imgproc.adaptiveThreshold(img_matrix, img_matrix, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 25, 30);

        //sendMat2Handler(img_matrix);

        return img_matrix;
    }

    // Writes the solution to the deskewed matrix, and writes a bitmap image in which all the
    // non-solution coordinates have a alpha value 0, denoting their transparency. The bitmap image
    // then undergoes a reverse of the perspective transform applied to the maze and is overlayed
    // over the src image.
    private void drawSolution(Stack<int[]> path, int[][] mazetrix, Bitmap image) {

        // Sets the value of the solution pixels to 2
        for (int[] coords : path) {
            mazetrix[coords[0]][coords[1]] = 2;
        }

        int[][] pixOut = new int[mazetrix.length][mazetrix[0].length];

        // TODO: MAke bloom amount related to the perimeter instead? Like lower perimeter, higher bloom. Or maybe compare peri to area.
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
        int[] pixels = get1DArray(pixOut, 1);

        Bitmap solution = Bitmap.createBitmap(pixels, mazetrix[0].length, mazetrix.length, Bitmap.Config.ARGB_8888);
        solution = Bitmap.createScaledBitmap(
                solution,
                solution.getWidth() * SCALE_FACTOR,
                solution.getHeight() * SCALE_FACTOR,
                true
        );

        Bitmap out = putOverlay(image, solution, mazeCorner.x, mazeCorner.y);
        imageView.setImageBitmap(out);
    }

    private Bitmap putOverlay(Bitmap base, Bitmap overlay, int x, int y) {
        Bitmap baseCpy = base.copy(Bitmap.Config.ARGB_8888, true);
        Paint p = new Paint();
        p.setColor(Color.RED);

        Canvas canvas = new Canvas(baseCpy);
        canvas.drawCircle(x, y, 5, p);
        canvas.drawBitmap(overlay, x, y, null);

        return baseCpy;
    }


    private void displayMat(Mat mat) {
        Bitmap bmp = mat2BMP(mat);
        if (bmp != null)
            imageView.setImageBitmap(bmp);
        else
            Log.e(TAG, "displayMat: Bitmap is null!");
    }

    private Bitmap mat2BMP(Mat mat) {
        Bitmap bmp = null;

        Mat tmp = new Mat(mat.height(), mat.width(), CvType.CV_8U, new Scalar(4));
        try {
            if (mat.channels() == 1)
                Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
            else if (mat.channels() == 4)
                tmp = mat;
            else
                Log.i(TAG, "mat2BMP: ERROR: CHANNELS = " + mat.channels());

            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmp, bmp);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }

        return bmp;
    }

    private Mat drawCnts(Mat mat, List<MatOfPoint> contours) {
        Mat tmp = new Mat(mat.height(), mat.width(), CvType.CV_8U, new Scalar(4));

        if (mat.channels() == 1)
            Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
        else if (mat.channels() == 4)
            tmp = mat;
        else
            Log.i(TAG, "drawCnts: ERROR: CHANNELS = " + mat.channels());
        Imgproc.drawContours(tmp, contours, -1, new Scalar(0, 255, 0, 255), 1);

        return tmp;
    }

    private Mat drawRects(Mat mat, List<Rect> rects) {
        Mat tmp = new Mat(mat.height(), mat.width(), CvType.CV_8U, new Scalar(4));

        if (mat.channels() == 1)
            Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
        else if (mat.channels() == 4)
            tmp = mat;
        else
            Log.i(TAG, "drawRects: ERROR: CHANNELS = " + mat.channels());

        for (Rect r : rects) {
            org.opencv.core.Point p1 = new org.opencv.core.Point(r.x, r.y);
            org.opencv.core.Point p2 = new org.opencv.core.Point(r.x + r.width, r.y + r.height);
            //Core.rectangle(tmp, p1, p2, new Scalar(255, 0, 0, 255));
            Imgproc.rectangle(tmp, p1, p2, new Scalar(255, 0, 0, 255));
        }

        return tmp;
    }

    private String printArr(int[][] arr) {
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


    private int[] get1DArray(int[][] arr, int numChannels) {
        int[] ret = new int[arr.length * arr[0].length * numChannels];
        int count = 0;

        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[0].length; j++) {
                ret[count] = arr[i][j];
                count += 1;
            }
        }
        return ret;
    }

    private static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
