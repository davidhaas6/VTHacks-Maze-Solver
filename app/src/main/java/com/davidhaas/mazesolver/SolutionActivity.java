package com.davidhaas.mazesolver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.widget.ImageView;
import android.widget.Toast;

import com.davidhaas.mazesolver.pathfinding.Asolution;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Stack;

/**
 * Name: SolutionActivity
 * Purpose: Displays the solution of the maze.
 * Author: David Haas
 * Last updated: 6/28/18
 */
public class SolutionActivity extends Activity {
    // TODO: Transfer drawing solution over here
    // TODO: Add loading button while program is solving maze
    // TODO: Look into this for cropping ur image: https://goo.gl/Qh8THg
    // TODO: Remove OpenCV dependencies https://developer.android.com/reference/android/graphics/Matrix
    //findViewById(R.id.loadingPanel).setVisibility(View.GONE);

    private static final String TAG = "SolutionActivity";
    Bitmap image;
    ImageView imageView;
    private int[][] corners;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Removes the title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_solution);
        imageView = findViewById(R.id.imageView);

        // Loads the intent
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        corners = (int[][]) bundle.getSerializable(CornerSelectActivity.CORNERS);

        // Loads the intent image as a bitmap for processing
        Uri imgUri = Uri.parse(bundle.getString(MainActivity.IMAGE_URI));
        try {
            image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "onCreate: Error loading image", e);
        }

        // Rotate the image if its wider than it is long
        if (image.getWidth() > image.getHeight())
            image = rotateBitmap(image, 90);

        getCroppedMatrix(corners);

        //solveMaze();
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);

    }

    // Runs the maze solving methods
    public boolean solveMaze() {

        // Returns a deskewed and cropped maze
        // int[][] deskewedMatrix = getDeskewedMatrix(corners);
        int[][] deskewedMatrix = getCroppedMatrix(corners);

        // Runs A* on the maze and gets the solution stack
        //TODO: Multithread A*?

        //TODO: Entrance-finding doesn't work if they're on the top and bottom
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


    // Crops the image in a rectangle bounding the corners and applies a threshold to obtain binary
    // values. 1s represent walls and 0s are paths.
    public int[][] getCroppedMatrix(int[][] corners) {
        // Converts the bitmap to an OpenCV matrix
        Mat img_matrix = new Mat();
        Bitmap bmp32 = image.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, img_matrix);

        Imgproc.cvtColor(img_matrix.clone(), img_matrix, Imgproc.COLOR_RGB2GRAY);

        //Imgproc.cvtColor(img_matrix.clone(), img_matrix, Imgproc.COLOR_RGB2GRAY);

        img_matrix = CVUtils.cropQuadrilateral(img_matrix, corners);

        Imgproc.GaussianBlur(img_matrix, img_matrix, new Size(7, 7), 0);

        Size dstSize = new Size(img_matrix.width() / 5, img_matrix.height() / 5);
        Log.i(TAG, "getCroppedMatrix: new size: " + dstSize);
        Mat dst = new Mat();
        Imgproc.resize(img_matrix, dst, dstSize, 1,1, Imgproc.INTER_NEAREST);

        //displayMat(dst);
        //TODO: consider making blockSize and C based off of image size?
        Imgproc.adaptiveThreshold(img_matrix, img_matrix, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 25, 30);
        displayMat(img_matrix);

        int[][] arr = CVUtils.getBinaryArray(img_matrix);
        //Log.i(TAG, "getCroppedMatrix: arr: + \n" + printArr(arr));

        //displayMat(img_matrix);

        return arr;
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
        final int r = 255;
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

        //TODO: skewed solution isn't drawing
        Rect boundingRect = CVUtils.getBoundingRect(corners);

        putOverlay(image, solution, boundingRect.x, boundingRect.y);
        //putOverlay(image, skewed_solution, corners[0][0], corners[0][1]);
        imageView.setImageBitmap(image);
    }

    public void putOverlay(Bitmap bitmap, Bitmap overlay, int x, int y) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(overlay, x, y, paint);
    }

    public void displayMat(Mat mat) {
        Bitmap bmp;

        Mat tmp = new Mat(mat.height(), mat.width(), CvType.CV_8U, new Scalar(4));
        try {
            Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmp, bmp);
            imageView.setImageBitmap(bmp);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }
    }

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

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
