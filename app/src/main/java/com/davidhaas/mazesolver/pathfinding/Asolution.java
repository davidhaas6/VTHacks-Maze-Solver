package com.davidhaas.mazesolver.pathfinding;

import android.util.Log;

import java.util.Arrays;
import java.util.Stack;

public class Asolution {

    private int[] start = new int[2];
    private int[] finish = new int[2];
    private int[][] grid;
    private int height;
    private int width;


    public Asolution(int[][] inImg) {
        grid = inImg;
        rowCleanUp();
        height = inImg.length;
        width = inImg[0].length;
        int[] sFarr = startFinish2(grid);
        this.start = new int[]{sFarr[0], sFarr[1]};
        this.finish = new int[]{sFarr[2], sFarr[3]};

    }


    public Stack<int[]> getPath() {
        int[][] blocked = makeBlocked(grid);
        AStarToUse myAStar = new AStarToUse(width, height, blocked, start[0], start[1], finish[0], finish[1]);
        myAStar.solve();
        return myAStar.getIntPath();
    }

    private void rowCleanUp() {
        for (int i = 0; i < grid.length; i++) {
            boolean empty = true;
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] == 1) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                for (int j = grid[0].length - 20; j < grid[0].length; j++) {
                    grid[i][j] = 1;
                }
            }
        }

    }

    public int[] startFinish2(int[][] maze) {
        final int width = maze[0].length, height = maze.length;

        /* Find the changes in the x and y coordinates relative to the walls and make arrays for each, pretty much a
         * graph of the derivative or change in slopes of the outer walls of the maze.
         */

        // Top to bottom
        int dy = 0;
        int prevY = 0;
        int startX = (int) (width * .03);
        int endX = (int) (width * .97);
        int firstTB_Y = 0;
        int[] top_bottom_dys = new int[endX - startX];

        for (int x = startX; x < endX; x++) {
            for (int y = 0; y < height; y++) {
                if (maze[y][x] == 1) {
                    if (x > startX)
                        dy = y - prevY;
                    else
                        firstTB_Y = y;
                    top_bottom_dys[x - startX] = dy;
                    prevY = y;
                    break;
                }
            }
        }

        // Bottom to top
        dy = 0;
        prevY = 0;
        int firstBT_Y = 0;
        int[] bottom_top_dys = new int[endX - startX];
        for (int x = startX; x < endX; x++) {
            for (int y = height - 1; y >= 0; y--) {
                if (maze[y][x] == 1) {
                    if (x > startX)
                        dy = prevY - y;
                    else
                        firstBT_Y = y;
                    bottom_top_dys[x - startX] = dy;
                    prevY = y;
                    break;
                }
            }
        }

        // Left to Right
        int dx = 0;
        int prevX = 0;
        int startY = (int) (height * .03);
        int endY = (int) (height * .97);
        int firstLR_X=0;
        int[] left_right_dxs = new int[endY - startY];

        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < width; x++) {
                if (maze[y][x] == 1) {
                    if (y > startY)
                        dx = x - prevX;
                    else
                        firstLR_X = x;
                    left_right_dxs[y - startY] = dx;
                    prevX = x;
                    break;
                }
            }
        }

        // Right to left
        dx = 0;
        prevX = 0;
        int firstRL_X=0;
        int[] right_left_dxs = new int[endY - startY];
        for (int y = startY; y < endY; y++) {
            for (int x = width - 1; x >= 0; x--) {
                if (maze[y][x] == 1) {
                    if (y > startY)
                        dx = prevX - x;
                    else
                        firstRL_X = x;
                    right_left_dxs[y - startY] = dx;
                    prevX = x;
                    break;
                }
            }
        }

        // Find the extrema of those derivative arrays (biggest jumps from white to black) to find the entrances.
        final int[][] TB_ex = findExtrema(top_bottom_dys);
        final int[][] BT_ex = findExtrema(bottom_top_dys);
        final int[][] LR_ex = findExtrema(left_right_dxs);
        final int[][] RL_ex = findExtrema(right_left_dxs);

        // Find the two sides with the largest extrema
        int[][][] extrema = {TB_ex, BT_ex, LR_ex, RL_ex};
        int largest = -1, largestIndx = 0, secondLargest = -1, secondIndx = 1, diff;
        for (int i = 0; i < extrema.length; i++) {
            diff = Math.abs(extrema[i][0][0] - extrema[i][1][0]);
            if (diff > largest) {
                secondLargest = largest;
                secondIndx = largestIndx;

                largest = diff;
                largestIndx = i;
            } else if (diff > secondLargest) {
                secondLargest = diff;
                secondIndx = i;
            }
        }

        // Extract the actual coordinates of those jumps so they can be used by A*
        int[] extremaIndxs = {largestIndx, secondIndx};
        int[] startEnd = new int[4];
        for (int i = 0; i < 4; i+=2) {
            int indx = extremaIndxs[i/2];
            int[][] pair = extrema[indx];
            switch (indx) {
                case 0:
                    startEnd[i+1] = startX + pair[0][1] + (pair[1][1] - pair[0][1]) / 2;
                    startEnd[i] = firstTB_Y + sumUpTo(top_bottom_dys, pair[0][1]);
                    break;
                case 1:
                    startEnd[i+1] = startX + pair[0][1] + (pair[1][1] - pair[0][1]) / 2;
                    startEnd[i] = firstBT_Y - sumUpTo(bottom_top_dys, pair[0][1]);
                    break;
                case 2:
                    startEnd[i+1] = firstLR_X + sumUpTo(left_right_dxs, pair[0][1]);
                    startEnd[i] = startY + pair[0][1] + (pair[1][1] - pair[0][1]) / 2;
                    break;
                case 3:
                    startEnd[i+1] =  firstRL_X - sumUpTo(right_left_dxs, pair[0][1]);
                    startEnd[i] = startY + pair[0][1] + (pair[1][1] - pair[0][1]) / 2;
                    break;
            }
        }

        Log.i("Asolution", "startFinish: " + Arrays.toString(startEnd));
        return startEnd;
    }

    // Find the largest value in an array and the lowest value following that
    private int[][] findExtrema(int[] arr) {
        int max = arr[0], maxIndx = 0, min = arr[0], minIndx = 0;

        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                maxIndx = i;

                min = 0; // Resets the min to find a new min
                minIndx = -1;
            }
            if (i > maxIndx && arr[i] < min) { // Ensures min comes after max
                min = arr[i];
                minIndx = i;
            }
        }

        return new int[][]{{max, maxIndx}, {min, minIndx}};
    }

    // Sums an array up to an index
    private int sumUpTo(int[] arr, int maxIndex) {
        int sum = 0;
        for (int i = 0; i < maxIndex; i++) {
            sum += arr[i];
        }
        return sum;
    }

   private int[] startFinish(int[][] array) {
        int[] output = new int[]{-1, -1, -1, -1};
        int squareSize = 2;
        //top
        for (int i = squareSize; i < array[0].length - squareSize; i++) {
            boolean comp = true;
            for (int offset = 0; offset < squareSize; offset++) {
                if (array[0][i + offset] == 1 || array[squareSize][i + offset] == 1 || array[offset][i] == 1 || array[offset][i + squareSize] == 1) {
                    comp = false;
                    break;
                }
            }
            if (comp) {
                output[1] = i + squareSize / 2;
                output[0] = 0;
                break;
            }
        }
        //bottom
        for (int i = squareSize; i < array[0].length - squareSize; i++) {
            boolean comp = true;
            for (int offset = 0; offset < squareSize; offset++) {
                if (array[array.length - 1][i + offset] == 1 || array[array.length - 1 - squareSize][i + offset] == 1 || array[array.length - 1 - offset][i] == 1 || array[array.length - 1 - offset][i + squareSize] == 1) {
                    comp = false;
                    break;
                }
            }
            if (comp) {
                if (output[1] == -1) {
                    output[1] = i + squareSize / 2;
                    output[0] = array.length - 1;
                } else {
                    output[3] = i + squareSize / 2;
                    output[2] = array.length - 1;
                }
            }

        }
        //left
        for (int i = squareSize; i < array.length - squareSize; i++) {
            boolean comp = true;
            for (int offset = 0; offset < squareSize; offset++) {
                if (array[i + offset][0] == 1 || array[i + offset][squareSize] == 1 || array[i][offset] == 1 || array[i + squareSize][offset] == 1) {
                    comp = false;
                    break;
                }
            }
            if (comp) {
                if (output[1] == -1) {
                    output[1] = 0 + squareSize / 2;
                    output[0] = i + squareSize / 2;
                } else {
                    output[3] = 0 + squareSize / 2;
                    output[2] = i + squareSize / 2;
                }
            }

        }
        //right
        for (int i = squareSize; i < array.length - squareSize; i++) {
            boolean comp = true;
            for (int offset = 0; offset < squareSize; offset++) {
                if (array[i + offset][array[0].length - 1] == 1 || array[i + offset][array[0].length - 1 - squareSize] == 1 || array[i][array[0].length - 1 - offset] == 1 || array[i + squareSize][array[0].length - 1 - offset] == 1) {
                    comp = false;
                    break;
                }
            }
            if (comp) {
                if (output[1] == -1) {
                    output[1] = array[0].length - 1 - squareSize / 2;
                    output[0] = i + squareSize / 2;
                } else {
                    output[3] = array[0].length - 1 - squareSize / 2;
                    output[2] = i + squareSize / 2;
                }
            }

        }
        Log.i("Asolution", "startFinish: " + Arrays.toString(output));
        return output;
    }

    private int[][] makeBlocked(int[][] array) {
        int[][] blocked;
        int index = 0;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                if (array[i][j] == 1) {
                    index++;
                }
            }
        }
        blocked = new int[index][2];
        index = 0;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                if (array[i][j] == 1) {
                    blocked[index] = new int[]{i, j};
                    index++;
                }
            }
        }
        //print(blocked);
        return blocked;
    }

    private void print(int[][] array) {
        String output = "";
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                output = output + array[i][j] + " ";
            }
            output = output + "\n";
        }
        System.out.println(output);
    }
}
