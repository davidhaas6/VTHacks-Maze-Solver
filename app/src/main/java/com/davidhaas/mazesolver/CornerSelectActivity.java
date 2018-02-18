package com.davidhaas.mazesolver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.davidhaas.mazesolver.pathfinding.Asolution;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Stack;

import static java.security.AccessController.getContext;

public class CornerSelectActivity extends AppCompatActivity {

    private static final String TAG = "CornerSelectActivity";

    Bitmap image;
    ImageView imageView;
    String imgPath;

    private int[][] corners;
    private int corner_count;
    private boolean maze_solved;
    private double view_scale_ratio;
    int vert_offset;
    private final int scale = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corner_select);

        Intent intent = getIntent();
        imgPath = intent.getStringExtra(MainActivity.IMAGE_FILE);
        image = rotateBitmap(BitmapFactory.decodeFile(imgPath), 90);

        // image = scaleBMP(image, 0.5);

        imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(image);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;

        view_scale_ratio = (double)image.getHeight() / height;

        corners = new int[4][2];
        corner_count = 0;
        maze_solved = false;
        vert_offset = getToolBarHeight();
        Log.i(TAG, "onCreate: " + vert_offset);


        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                view.performClick();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int x = (int) (event.getX() * view_scale_ratio);
                    int y = (int) (event.getY() * view_scale_ratio);

                    if(corner_count < 4) {
                        // TODO: image is translated downwards, get correct coordinates
                        corners[corner_count] = new int[]{x,y+vert_offset};
                        corner_count++;
                        Log.i(TAG, "onTouch: \n" + printArr(corners));
                        if (corner_count == 4) {
                            corners = CVUtils.orderPoints(corners);
                            solveMaze();
                        }
                        //TODO: Make circle objects that you can move
                    } else if(!maze_solved) {
                        solveMaze();
                    }

                    //Log.i(TAG, "onTouch: (" + x + ", " + y + ")");
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

    // Applies a perspective transform and adaptive gausian threshold to get the deskewed matrix of
    // binary (0 or 1) values. 1s represent walls and 0s are paths.
    public int[][] getDeskewedMatrix(int[][] corners, String path){
        //TODO

        Mat img_matrix = new Mat();
        Bitmap bmp32 = image.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, img_matrix);


        img_matrix = CVUtils.fourPointTransform(img_matrix, corners, false);

        Size orig_size = img_matrix.size();
        Mat small_im = new Mat(img_matrix.rows()/scale, img_matrix.cols()/scale, img_matrix.type());
        Imgproc.resize(img_matrix.clone(), small_im, new Size(orig_size.width / scale, orig_size.height / scale));

        // Converts it to gray
        Imgproc.cvtColor(small_im.clone(), small_im, Imgproc.COLOR_RGB2GRAY);

        // TODO: blur image
        Imgproc.GaussianBlur(small_im.clone(), small_im, new Size(15,15), 0);
        // Applies an adaptive Gaussian threshold to the matrix

        Imgproc.adaptiveThreshold(small_im.clone(), small_im, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 25, 5);
        Log.i(TAG, "getDeskewedMatrix: Applied threshold");

//        Bitmap bmp = null;
//        Mat tmp = new Mat (image.getHeight(), image.getWidth(), CvType.CV_8U, new Scalar(4));
//        try {
//            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
//            Imgproc.cvtColor(img_matrix, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
//            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(tmp, bmp);
//        }
//        catch (CvException e){Log.d("Exception",e.getMessage());}
//        imageView.setImageBitmap(bmp);

        int[][] arr = CVUtils.getBinaryArray(small_im);
        Log.i(TAG, "getDeskewedMatrix: w: " + arr[0].length + "\t h: " + arr.length);
        //for(int i = 0; i < arr[0].length; i++)
            //Log.i(TAG, "getDeskewedMatrix: " + arr[0][i]);
        // Imgproc.

        return arr;
    }

    // Runs the maze solving methods
    public void solveMaze() {
        int[][] deskewedMatrix = getDeskewedMatrix(corners, imgPath);

        Asolution mySol = new Asolution(deskewedMatrix);
        Stack<int[]> solution = mySol.getPath();

        if (solution == null){
            Log.e(TAG, "solveMaze: " + "Could not solve maze" );
            return;
        }
        Log.i(TAG, "solveMaze: MAZE SOLVED!!!!!!");

        drawSolution(solution, deskewedMatrix);
        maze_solved = true;
    }

    // Writes the solution to the deskewed matrix, and writes a bitmap image in which all the
    // non-solution coordinates have a alpha value 0, denoting their transparency. The bitmap image
    // then undergoes a reverse of the perspective transform applied to the maze and is overlayed
    // over the src image.
    public void drawSolution(Stack<int[]> path, int[][] mazetrix) {

        // Sets the value of the solution pixels to 2
        for(int[] coords : path)
        {
            mazetrix[coords[0]][coords[1]] = 2;
        }

        // Colors the solution
        int color, a,r = 255,g = 0, b = 0;
        for(int i = 0; i < mazetrix.length; i++) {
            for (int j = 0; j < mazetrix[0].length; j++) {

                // If pixel is part of the solution, make the color opaque red, otherwise transparent
                if (mazetrix[i][j] == 2){
                    a = 255;
                } else {
                    a = 0;
                }
                // Encodes it in sRGB color space
                color = (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
                mazetrix[i][j] = color;
            }
        }

        // TODO: Reverse transform
        int[] pixels = get1DArray(mazetrix, 1);

        Bitmap solution = Bitmap.createBitmap(mazetrix[0].length, mazetrix.length, Bitmap.Config.ARGB_8888);
        // vector is your int[] of ARGB
        solution.copyPixelsFromBuffer(IntBuffer.wrap(pixels));

        Mat img_matrix = new Mat();
        Bitmap bmp32 = solution.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, img_matrix);

        Mat skew_matrix = new Mat(img_matrix.rows() * scale, img_matrix.cols() * scale, img_matrix.type());
        Imgproc.resize(img_matrix, skew_matrix, skew_matrix.size());

        //skew_matrix = CVUtils.fourPointTransform(skew_matrix, corners, true);

        Bitmap skewed_solution = Bitmap.createBitmap(skew_matrix.cols(), skew_matrix.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(skew_matrix, skewed_solution);
        Bitmap s_soln = (Bitmap) skewed_solution;

        putOverlay(image, solution, corners[0][0], corners[0][1]);
        putOverlay(image, s_soln, corners[0][0], corners[0][1]);
        imageView.setImageBitmap(image);
    }

    public void putOverlay(Bitmap bitmap, Bitmap overlay, int x, int y) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(overlay, x, y, paint);
    }

    public String printArr(int[][] arr) {
        String ret = "";
        for(int q = 0; q < arr.length; q++){
            for (int h = 0; h < arr[q].length; h++){
                ret += arr[q][h] + ", ";
            }
            ret += "\n";
        }
        return ret;
    }

    private int[] get1DArray(int[][] arr, int numChannels) {
        int[] ret = new int[arr.length * arr[0].length * numChannels];
        int count = 0;

        for(int i = 0; i < arr.length; i++){
            for(int j = 0; j < arr[0].length; j++) {
                ret[count] = arr[i][j];
                count += 1;
            }
        }
        return ret;
    }

    public int[][] getArray(Bitmap image){
        int w = image.getWidth(), h = image.getHeight();
        int[] pixel_temp = new int[w * h];
        image.getPixels(pixel_temp, 0, w, 0, 0, w, h);
        return getGrayScaleArray(pixel_temp, w, h);
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    private int[][] getGrayScaleArray(int[] pixels, int width, int height) {
        Log.i(TAG, "w:" + width + " h" + height);

        int[][] rgb = new int[height][width];
        int r, g, b, gray, count = 0;

        Log.i(TAG, "RGB Array init2");

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {

                r = Color.red(pixels[count]);
                g = Color.green(pixels[count]);
                b = Color.blue(pixels[count]);

                gray = (int) ((0.3 * r) + (0.59 * g) + (0.11 * b));
                //Log.i(TAG, "Values got");

                rgb[i][j] = gray;
                count++;
            }
        }

        return rgb;
    }

    public int getToolBarHeight() {
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }
        return -1;
    }
}
