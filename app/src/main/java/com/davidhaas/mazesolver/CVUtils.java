package com.davidhaas.mazesolver;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.WARP_INVERSE_MAP;
import static org.opencv.imgproc.Imgproc.boundingRect;

/**
 * Name: CVUtils
 * Purpose: A collection of methods to process the maze image for solving
 * Author: David Haas
 * Created: 2/16/18
 */

public class CVUtils {

    private static final String TAG = "CVUtils";

    public static int[][] orderPoints(int[][] pts) {
        // Orders a set of coordinates in a 4x2 array such that:
        // 0: top left, 1: top right, 2: bottom left, 3: bottom right

        int[][] rect = new int[4][2];
        ArrayList<Integer> sums = new ArrayList<>();
        ArrayList<Integer> diffs = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            sums.add(pts[i][1] + pts[i][0]);
            diffs.add(pts[i][1] - pts[i][0]);
        }

        //Top left will have the smallest sum, bottom right will have the largest sum
        rect[0] = pts[sums.indexOf(Collections.min(sums))];
        rect[3] = pts[sums.indexOf(Collections.max(sums))];

        // Top right will have the smallest diff, bottom left will have largest diff
        rect[1] = pts[diffs.indexOf(Collections.min(diffs))];
        rect[2] = pts[diffs.indexOf(Collections.max(diffs))];

        return rect;
    }

    public static Mat fourPointTransform(Mat image, int[][] bounds, boolean inverse) {
        int[][] corners = orderPoints(bounds);
        int[] tl = corners[0], tr = corners[1], bl = corners[2], br = corners[3];

        // Gets the width for the new image
        int maxWidth;
        double w1 = distance(tl, tr);
        double w2 = distance(bl, br);
        if (w1 > w2) {
            maxWidth = (int) Math.round(w1);
        } else {
            maxWidth = (int) Math.round(w2);
        }

        // Gets the height for the new image
        int maxHeight;
        double h1 = distance(tl, bl);
        double h2 = distance(tr, br);
        if (h1 > h2) {
            maxHeight = (int) Math.round(h1);
        } else {
            maxHeight = (int) Math.round(h2);
        }

/*        int[][] dst = new int[][]{
                {0, 0},
                {maxWidth - 1, 0},
                {0, maxHeight - 1},
                {maxWidth - 1, maxHeight - 1}
        };*/

        Mat srcMat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstMat = new Mat(4, 1, CvType.CV_32FC2);

        srcMat.put(0, 0,
                tl[0], tl[1],
                tr[0], tr[1],
                bl[0], bl[1],
                br[0], br[1]);

        dstMat.put(0, 0,
                0., 0.,
                maxWidth - 1., 0.,
                0., maxHeight - 1.,
                maxWidth - 1., maxHeight - 1.);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat);

        Mat dst = image.clone();

        if (inverse) {
            Imgproc.warpPerspective(image, dst, perspectiveTransform, new Size(maxWidth, maxHeight), WARP_INVERSE_MAP);
            dst.convertTo(dst, CvType.CV_8UC4);
            return dst;
        } else {
            Imgproc.warpPerspective(image, dst, perspectiveTransform, new Size(maxWidth, maxHeight));
            return dst;
        }
    }

    public static double distance(int[] p1, int[] p2) {
        return Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
    }

    public static Rect getBoundingRect(int[][] bounds) {
        bounds = orderPoints(bounds);
        Point[] corners = new Point[4];
        for (int i = 0; i < 4; i++)
            corners[i] = new Point(bounds[i][0], bounds[i][1]);

        MatOfPoint cornerMat = new MatOfPoint(corners);

        return boundingRect(cornerMat);
    }

    public static Mat cropQuadrilateral(Mat image, int[][] bounds) {
        // https://stackoverflow.com/questions/48301186/cropping-concave-polygon-from-image-using-opencv-python
        // https://stackoverflow.com/questions/41689522/opencv-cropping-non-rectangular-region-from-image-using-c
        image = image.clone();
        bounds = orderPoints(bounds);
        Point[] corners = new Point[4];
        for (int i = 0; i < 4; i++)
            corners[i] = new Point(bounds[i][0], bounds[i][1]);

        // Swaps the bottom two corners so that the order is: tl, tr, br, bl. This is necessary to
        // construct an accurate polygon (otherwise it would just draw an hourglass)
        final Point temp = corners[2];
        corners[2] = corners[3];
        corners[3] = temp;

        MatOfPoint cornerMat = new MatOfPoint(corners);
        Rect bRect = boundingRect(cornerMat);

        // Ensures the bounding rect doesn't go outside the image
        int rightX = bRect.x + bRect.width;
        int bottomY = bRect.y + bRect.height;
        if (rightX > image.width())
            bRect = new Rect(bRect.x, bRect.y, bRect.width - (rightX - image.width()), bRect.height);
        if (bottomY > image.height())
            bRect = new Rect(bRect.x, bRect.y, bRect.width, bRect.height - (bottomY - image.height()));
        if (bRect.x < 0)
            bRect = new Rect(0, bRect.y, bRect.width, bRect.height - (bottomY - image.height()));
        if (bRect.y < 0)
            bRect = new Rect(bRect.x, 0, bRect.width, bRect.height - (bottomY - image.height()));

        // Creates an empty "canvas" in the shape of the size of the image
        Mat mask8 = Mat.zeros(image.size(), CV_8UC1);

        // Creates a mask in the shape of the polygon
        //Core.fillConvexPoly(mask8, cornerMat, new Scalar(255, 255, 255));
        Imgproc.fillConvexPoly(mask8, cornerMat, new Scalar(255, 255, 255));

        // Copies the relevant part of the image into the polygon mask
        Mat result = new Mat(image.size(), image.type(), new Scalar(255, 255, 255));
        image.copyTo(result, mask8);

        // Returns the masked image with the rest of the image being 0s
        return result.submat(bRect);
    }

    public static int[][] getBinaryArray(Mat m) {
        // Reads the matrix into a byte array where the first index is pixel, second index is channel
        int frameSize = m.rows() * m.cols();
        byte[] byteBuffer = new byte[frameSize];
        m.get(0, 0, byteBuffer);

        //write to separate R,G,B arrays
        int[][] out = new int[m.rows()][m.cols()];
        for (int i = 0, c = 0; i < m.rows(); i++) {
            for (int j = 0; j < m.cols(); j++, c++) {
                if (byteBuffer[c] == 0)
                    out[i][j] = 1;
                else
                    out[i][j] = 0;
            }
        }
        return out;
    }

    // Returns two because mazes will be split in half pretty much
    public static List<MatOfPoint> largest2PerimContours(Mat m) {
        List<MatOfPoint> cnts = new ArrayList<>();
        Imgproc.findContours(m.clone(), cnts, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        if (cnts.size() == 0)
            return null;

        MatOfPoint largest = cnts.get(0);
        MatOfPoint secondLargest = largest;
        double largestPerim = 0;
        double secondPerim = 0;
        double cntPerim;

        for (MatOfPoint c : cnts) {
            MatOfPoint2f c2f = new MatOfPoint2f();
            c.convertTo(c2f, CvType.CV_32FC2);
            cntPerim = Imgproc.arcLength(c2f, true);

            if (!isContourSquare(c2f)) {
                if ((cntPerim > largestPerim)) {
                    secondLargest = largest;
                    secondPerim = largestPerim;

                    largest = c;
                    largestPerim = cntPerim;
                } else if (cntPerim > secondPerim) {
                    secondLargest = c;
                    secondPerim = cntPerim;
                }
            }
        }

        List<MatOfPoint> mazeComponents = new ArrayList<>();
        mazeComponents.add(largest);
        mazeComponents.add(secondLargest);
        return mazeComponents;
    }

    private static boolean isContourSquare(MatOfPoint2f thisContour) {
        MatOfPoint2f approxContour2f = new MatOfPoint2f();

        double cntPerim = Imgproc.arcLength(thisContour, true);

        Imgproc.approxPolyDP(thisContour, approxContour2f, cntPerim * .01, true);

        return approxContour2f.height() == 4;
    }

    public static Rect combineRects(Rect r1, Rect r2) {
        android.graphics.Rect aRect1 = OpenCV2AndroidRect(r1);
        android.graphics.Rect aRect2 = OpenCV2AndroidRect(r2);
        aRect1.union(aRect2);
        return android2OpenCVRect(aRect1);
    }

    public static android.graphics.Rect OpenCV2AndroidRect(Rect r) {
        return new android.graphics.Rect(r.x, r.y, r.x + r.width, r.y + r.height);
    }

    public static Rect android2OpenCVRect(android.graphics.Rect r) {
        return new Rect(new Point(r.left, r.top), new Point(r.right, r.bottom));
    }
}
