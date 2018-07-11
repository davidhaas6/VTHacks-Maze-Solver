package com.davidhaas.mazesolver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.davidhaas.mazesolver.pathfinding.Asolution;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.function.ObjIntConsumer;

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
    private ImageView imageView;
    private Point mazeCorner; // TODO: Can you get rid of this global variable?


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Removes the title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_solution);
        imageView = findViewById(R.id.imageView);

        View loadingBar = findViewById(R.id.loadingPanel);
        loadingBar.setVisibility(View.VISIBLE);

        // Loads the intent
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        int[][] corners = (int[][]) bundle.getSerializable(CornerSelectActivity.CORNERS);

        // Loads the intent image as a bitmap for processing
        Uri imgUri = Uri.parse(bundle.getString(MainActivity.IMAGE_URI));
        try {
            Bitmap image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);

            // Rotate the image if its wider than it is long
            if (image.getWidth() > image.getHeight())
                image = rotateBitmap(image, 90);

            displayMat(getCroppedMaze(corners, image));

            //solveMaze(corners, image);
            loadingBar.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "onCreate: Error loading image", e);
        }

        // run a background job and once complete
        //pb.setVisibility(ProgressBar.INVISIBLE);

    }

    // Runs the maze solving methods
    private boolean solveMaze(int[][] corners, Bitmap image) {

        // Returns a deskewed and cropped maze
        // int[][] deskewedMatrix = getDeskewedMatrix(corners);
        Mat croppedMaze = getCroppedMaze(corners, image);
        //image = mat2BMP(croppedMaze);
        int[][] croppedBinaryMaze = CVUtils.getBinaryArray(croppedMaze);

        // Runs A* on the maze and gets the solution stack
        //TODO: Multithread A*?

        //TODO: Entrance-finding doesn't work if they're on the top and bottom
        Asolution mySol = new Asolution(croppedBinaryMaze);
        Stack<int[]> solution = mySol.getPath();

        if (solution == null) {
            Toast.makeText(getApplicationContext(), "Could not solve maze with current selection!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "solveMaze: " + "Could not solve maze");

            return false;
        }
        Log.i(TAG, "solveMaze: MAZE SOLVED");

        drawSolution(solution, croppedBinaryMaze, image);
        return true;
    }


    // Crops the image in a rectangle bounding the corners and applies a threshold to obtain binary
    // values. 1s represent walls and 0s are paths.
    private Mat getCroppedMaze(int[][] corners, Bitmap image) {
        // Converts the bitmap to an OpenCV matrix
        Mat img_matrix = new Mat();
        Bitmap bmp32 = image.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, img_matrix);

        Imgproc.cvtColor(img_matrix.clone(), img_matrix, Imgproc.COLOR_RGB2GRAY);

        img_matrix = CVUtils.cropQuadrilateral(img_matrix, corners);

        Imgproc.GaussianBlur(img_matrix, img_matrix, new Size(1, 1), 0);

        //TODO: consider making blockSize and C based off of image size?
        Imgproc.adaptiveThreshold(img_matrix, img_matrix, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 25, 30);

        List<MatOfPoint> mazePerim = CVUtils.largest2PerimContours(img_matrix);
        if (mazePerim != null) {
            ArrayList<Rect> rects = new ArrayList<>();
            for (MatOfPoint c : mazePerim)
                rects.add(Imgproc.boundingRect(c));
            //displayMatCnts(img_matrix, mazePerim);

            Rect combined = CVUtils.combineRects(rects.get(0), rects.get(1));
            rects.clear();
            rects.add(combined);

            //displayMatRects(img_matrix, rects);
            int[][] bounds = new int[][]{
                    {combined.x, combined.y},
                    {combined.x + combined.width, combined.y},
                    {combined.x + combined.width, combined.y + combined.height},
                    {combined.x, combined.y + combined.height}
            };

            img_matrix = CVUtils.cropQuadrilateral(img_matrix, bounds);
            //displayMat(img_matrix);

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

        //displayMat(img_matrix);

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
                                else if (mazetrix[i+k][j+x] == 1)
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
        // TODO: Resize solution bitmap by SCALE_FACTOR and use mazeCorner to place it on image.
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
        base = base.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(base);
        Paint p = new Paint();
        p.setColor(Color.RED);
        canvas.drawCircle(x, y, 5, p);
        canvas.drawBitmap(overlay, x, y, null);
        return base;
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
            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmp, bmp);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }

        return bmp;
    }

    private void displayMatCnts(Mat mat, List<MatOfPoint> contours) {
        Mat tmp = new Mat(mat.height(), mat.width(), CvType.CV_8U, new Scalar(4));

        if (mat.channels() == 1)
            Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
        Imgproc.drawContours(tmp, contours, -1, new Scalar(0, 255, 0, 255), 1);

        displayMat(tmp);
    }

    private void displayMatRects(Mat mat, List<Rect> rects) {
        Mat tmp = new Mat(mat.height(), mat.width(), CvType.CV_8U, new Scalar(4));

        if (mat.channels() == 1)
            Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_GRAY2RGBA, 4);

        for (Rect r : rects) {
            org.opencv.core.Point p1 = new org.opencv.core.Point(r.x, r.y);
            org.opencv.core.Point p2 = new org.opencv.core.Point(r.x + r.width, r.y + r.height);
            Core.rectangle(tmp, p1, p2, new Scalar(255, 0, 0, 255));
        }

        displayMat(tmp);
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
