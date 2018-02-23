package com.davidhaas.mazesolver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.davidhaas.mazesolver.pathfinding.Asolution;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.IntBuffer;
import java.util.Stack;

public class CornerSelectActivity extends AppCompatActivity {

    private static final String TAG = "CornerSelectActivity";

    Bitmap image;
    ImageView imageView;
    String imgPath;

    private int[][] corners;
    private double view_scale_ratio;
    int vert_offset;
    //private final int scale = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corner_select);

        // Loads the intent as a bitmap
        Intent intent = getIntent();
        imgPath = intent.getStringExtra(MainActivity.IMAGE_FILE);
        image = rotateBitmap(BitmapFactory.decodeFile(imgPath), 90);

        // image = scaleBMP(image, 0.5);

        // Scales and set the bitmap
        imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(image);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Gets the ratio between the appeared image height and the actual height so we can map
        // screen touches to pixels on the image
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;

        view_scale_ratio = (double) image.getHeight() / height;

        // Initializes the variables
        corners = new int[4][2];

        // Finds the vertical offset on the image due to the menu bar
        vert_offset = getToolBarHeight();
        Log.i(TAG, "onCreate: " + vert_offset);


        imageView.setOnTouchListener(new View.OnTouchListener() {
            private int corner_count = 0;
            private boolean maze_solved = false;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                view.performClick();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int x = (int) (event.getX() * view_scale_ratio);
                    int y = (int) (event.getY() * view_scale_ratio);

                    if (corner_count < 4) {
                        //TODO: Make circle objects that you can move
                        corners[corner_count] = new int[]{x, y + vert_offset};
                        corner_count++;

                        if (corner_count == 4) {
                            corners = CVUtils.orderPoints(corners);
                            maze_solved = solveMaze();
                        }
                    } else if (!maze_solved) {
                        maze_solved = solveMaze();
                    }
                }
                return false;
            }
        });
    }

    public Bitmap scaleBMP(Bitmap bmp, double scale) {
        Mat mat = new Mat();
        Bitmap bmp32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);
        Log.i(TAG, "scaleBMP: 1 " + mat.size());

        Imgproc.resize(mat.clone(), mat, new Size((int) (mat.cols() * scale), (int) (mat.rows() * scale)));

        Bitmap scaled = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Log.i(TAG, "scaleBMP: 2 " + mat.size());

        Utils.matToBitmap(mat, scaled);
        return scaled;
    }

    // Applies a perspective transform and adaptive gaussian threshold to get the deskewed matrix of
    // binary (0 or 1) values. 1s represent walls and 0s are paths.
    public int[][] getDeskewedMatrix(int[][] corners) {

        // Converts the bitmap to an OpenCV matrix
        Mat img_matrix = new Mat();
        Bitmap bmp32 = image.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, img_matrix);

        // Applies a 4 point transform to the image
        img_matrix = CVUtils.fourPointTransform(img_matrix, corners, false);

//        // Scales the image down
//        Size orig_size = img_matrix.size();
//        Mat small_im = new Mat(img_matrix.rows()/scale, img_matrix.cols()/scale, img_matrix.type());
//        Imgproc.resize(img_matrix.clone(), small_im, new Size(orig_size.width / scale, orig_size.height / scale));

        // Converts it to gray
        Imgproc.cvtColor(img_matrix.clone(), img_matrix, Imgproc.COLOR_RGB2GRAY);

        Imgproc.GaussianBlur(img_matrix.clone(), img_matrix, new Size(15, 15), 0);

        // Applies an adaptive Gaussian threshold to the matrix
        Imgproc.adaptiveThreshold(img_matrix.clone(), img_matrix, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 25, 5);

        // Returns a 2d array in which the 1s are walls and the 0s are paths
        int[][] arr = CVUtils.getBinaryArray(img_matrix);

        Log.i(TAG, "getDeskewedMatrix: w: " + arr[0].length + "\t h: " + arr.length);

        return arr;
    }

    // Crops the image in a rectangle bounding the corners and applies a threshold to obtain binary
    // values. 1s represent walls and 0s are paths.
    public int[][] getCroppedMatrix(int[][] corners) {

        // Converts the bitmap to an OpenCV matrix
        Mat img_matrix = new Mat();
        Bitmap bmp32 = image.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, img_matrix);

        // Applies a 4 point transform to the image
        img_matrix = CVUtils.getRectCropped(img_matrix, corners);

        Imgproc.cvtColor(img_matrix.clone(), img_matrix, Imgproc.COLOR_RGB2GRAY);

        Imgproc.GaussianBlur(img_matrix.clone(), img_matrix, new Size(15, 15), 0);

        Imgproc.adaptiveThreshold(img_matrix.clone(), img_matrix, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 25, 5);

        int[][] arr = CVUtils.getBinaryArray(img_matrix);

        Log.i(TAG, "getDeskewedMatrix: w: " + arr[0].length + "\t h: " + arr.length);

        return arr;
    }


    // Runs the maze solving methods
    public boolean solveMaze() {
        //TODO: Maybe don't transform and just crop????

        // Returns a deskewed and cropped maze
        // int[][] deskewedMatrix = getDeskewedMatrix(corners);
        int[][] deskewedMatrix = getCroppedMatrix(corners);

        // Runs A* on the maze and gets the solution stack
        //TODO: Multithread A*?
        Asolution mySol = new Asolution(deskewedMatrix);
        Stack<int[]> solution = mySol.getPath();

        if (solution == null) {
            Toast.makeText(getApplicationContext(), "Could not solve maze with current selection!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "solveMaze: " + "Could not solve maze");

            return false;
        }
        Log.i(TAG, "solveMaze: MAZE SOLVED!!!!!!");

        drawSolution(solution, deskewedMatrix);
        return true;
    }

    // Writes the solution to the deskewed matrix, and writes a bitmap image in which all the
    // non-solution coordinates have a alpha value 0, denoting their transparency. The bitmap image
    // then undergoes a reverse of the perspective transform applied to the maze and is overlayed
    // over the src image.
    public void drawSolution(Stack<int[]> path, int[][] mazetrix) {

        // Sets the value of the solution pixels to 2
        for (int[] coords : path) {
            mazetrix[coords[0]][coords[1]] = 2;
        }

        // Colors the solution  re
        int color, a;
        final int r = 255, g = 0, b = 0;
        for (int i = 0; i < mazetrix.length; i++) {
            for (int j = 0; j < mazetrix[0].length; j++) {

                // If pixel is part of the solution, make the color opaque red, otherwise transparent
                if (mazetrix[i][j] == 2) {
                    a = 255;
                } else {
                    a = 0;
                }
                // Encodes it in hexadecimal sRGB color space
                color = (a & 0xff) << 24 | (r & 0xff) << 16;
                mazetrix[i][j] = color;
            }
        }

        int[] pixels = get1DArray(mazetrix, 1);

        Bitmap solution = Bitmap.createBitmap(mazetrix[0].length, mazetrix.length, Bitmap.Config.ARGB_8888);
        // vector is your int[] of ARGB
        solution.copyPixelsFromBuffer(IntBuffer.wrap(pixels));

        // https://docs.opencv.org/java/2.4.9/org/opencv/android/Utils.html#bitmapToMat(Bitmap,%20org.opencv.core.Mat,%20boolean)
        Mat img_matrix = new Mat();
        Bitmap bmp32 = solution.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, img_matrix, true);

//        // Scales the image back to normal
//        Mat skew_matrix = new Mat(img_matrix.rows() * scale, img_matrix.cols() * scale, img_matrix.type());
//        Imgproc.resize(img_matrix, skew_matrix, skew_matrix.size());

        //  img_matrix = CVUtils.fourPointTransform(img_matrix, corners, true);

        Bitmap skewed_solution = Bitmap.createBitmap(img_matrix.cols(), img_matrix.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(img_matrix, skewed_solution, true);

        //TODO: skewed solution isn't drawing
        putOverlay(image, solution, corners[0][0], corners[0][1]);
        putOverlay(image, skewed_solution, corners[0][0], corners[0][1]);
        imageView.setImageBitmap(image);
    }

    public void putOverlay(Bitmap bitmap, Bitmap overlay, int x, int y) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(overlay, x, y, paint);
    }

    public void displayMat(Mat mat) {
        Bitmap bmp = null;

        Mat tmp = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8U, new Scalar(4));
        try {
            Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmp, bmp);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }
        imageView.setImageBitmap(bmp);
    }

    public String printArr(int[][] arr) {
        StringBuilder sb = new StringBuilder();

        for (int q = 0; q < arr.length; q++) {
            for (int h = 0; h < arr[q].length; h++) {
                sb.append(arr[q][h]);
                sb.append(", ");
            }
            sb.append("\n");
        }
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

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public int getToolBarHeight() {
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        return -1;
    }
}