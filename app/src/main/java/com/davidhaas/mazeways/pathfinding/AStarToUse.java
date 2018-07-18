package com.davidhaas.mazeways.pathfinding;

import android.util.Log;

import java.util.*;
public class AStarToUse {
    private final int DIAGONAL_COST = 14;
    private final int V_H_COST = 10;
    private Cell[][] grid;
    private PriorityQueue<Cell> open;
    private boolean[][] closed;
    private int startI, startJ;
    private int endI, endJ;


    public AStarToUse(int width, int height,int[][] blocked,
        int startI, int startJ,int endI,int endJ) {
        this.grid = new Cell[height][width];
        this.closed = new boolean[height][width];
        open = new PriorityQueue<>(2, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2)
            {
                Cell c1 = (Cell)o1;
                Cell c2 = (Cell)o2;

                return c1.finalCost<c2.finalCost?-1:
                        c1.finalCost>c2.finalCost?1:0;
            }
        });
        
        setStartCell(startI, startJ);
        setEndCell(endI, endJ);
        for(int i=0;i<height;++i){
            for(int j=0;j<width;++j){
                grid[i][j] = new Cell(i, j);
                grid[i][j].heuristicCost = Math.abs(i-endI)+Math.abs(j-endJ);
            }
         }
        
        for(int i=0;i<blocked.length;++i){
            setBlocked(blocked[i][0], blocked[i][1]);
        }
         grid[startI][startJ].finalCost = 0;

    }


    public void solve() {
     // add the start location to open list.

        open.add(grid[startI][startJ]);

        Cell current;

        while (true) {
            current = open.poll();

            //Log.i("AStarToUse", "solve: " + current);

            if (current == null)
                break;
            closed[current.i][current.j] = true;

            if (current.equals(grid[endI][endJ])) {
                return;
            }

            Cell t;
            if (current.i - 1 >= 0) {
                t = grid[current.i - 1][current.j];
                checkAndUpdateCost(current, t, current.finalCost + V_H_COST);

                if (current.j - 1 >= 0) {
                    t = grid[current.i - 1][current.j - 1];
                    checkAndUpdateCost(current, t, current.finalCost
                        + DIAGONAL_COST);
                }

                if (current.j + 1 < grid[0].length) {
                    t = grid[current.i - 1][current.j + 1];
                    checkAndUpdateCost(current, t, current.finalCost
                        + DIAGONAL_COST);
                }
            }

            if (current.j - 1 >= 0) {
                t = grid[current.i][current.j - 1];
                checkAndUpdateCost(current, t, current.finalCost + V_H_COST);
            }

            if (current.j + 1 < grid[0].length) {
                t = grid[current.i][current.j + 1];
                checkAndUpdateCost(current, t, current.finalCost + V_H_COST);
            }

            if (current.i + 1 < grid.length) {
                t = grid[current.i + 1][current.j];
                checkAndUpdateCost(current, t, current.finalCost + V_H_COST);

                if (current.j - 1 >= 0) {
                    t = grid[current.i + 1][current.j - 1];
                    checkAndUpdateCost(current, t, current.finalCost
                        + DIAGONAL_COST);
                }

                if (current.j + 1 < grid[0].length) {
                    t = grid[current.i + 1][current.j + 1];
                    checkAndUpdateCost(current, t, current.finalCost
                        + DIAGONAL_COST);
                }
            }
        }
    }
    
    public Stack<int[]> getIntPath(){
        Stack<Cell> path = getPath();
        if(path==null) {return null;}
        Stack<Cell> fliped = new Stack<Cell>();
        Stack<int[]> output = new Stack<int[]>();
        while(path.size()>0) {
            fliped.push(path.pop());
        }
        while(fliped.size()>0) {
            Cell temp = fliped.pop();
            int[] ij = new int[]{temp.getI(),temp.getJ()};
            output.push(ij);
        }
        return output;
    }
    
    public Stack<Cell> getPath(){
        Stack<Cell> path = new Stack<Cell>();
        if(closed[endI][endJ]){
            //Trace back the path 
             Cell current = grid[endI][endJ];
             while(current.getParent()!=null){
                 path.push(current.getParent());
                 current = current.getParent();
             } 
             return path;
        }else {
            return null;
        }
    }
    
    public void setBlocked(int i, int j) {
        grid[i][j] = null;
    }


    public void setStartCell(int i, int j) {
        startI = i;
        startJ = j;
    }

    public void setEndCell(int i, int j) {
        endI = i;
        endJ = j;
    }

    private void checkAndUpdateCost(Cell current, Cell t, int cost) {
        if (t == null || closed[t.i][t.j]) {
            return;// checks if the cell is blocked or if it has already been
                   // checked
        }
        int t_final_cost = t.heuristicCost + cost;// increment the final cost by
                                                  // the input cost
        boolean inOpen = open.contains(t);// Checks if the cell is in open. It
                                          // should be.
        if (!inOpen || t_final_cost < t.finalCost) {
            t.finalCost = t_final_cost;
            t.parent = current;
            if (!inOpen)
                open.add(t);
        }

    }
}
