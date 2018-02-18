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
        int[] sFarr = startFinish(grid);
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

    private int[] startFinish(int[][] array) {
        int[] output = new int[]{-1, -1, -1, -1};
        int squareSize = 18;
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
