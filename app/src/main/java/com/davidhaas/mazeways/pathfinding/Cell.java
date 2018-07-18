package com.davidhaas.mazeways.pathfinding;

class Cell {

    public int heuristicCost = 0; //Heuristic cost
    public int finalCost = 0; //G+H
    public int i, j;
    public Cell parent; //Used for path generation
    
    Cell(int i, int j){
        this.i = i;
        this.j = j; 
    }
    
    public void setParent(Cell cell) {
        parent=cell;
    }
    
    public Cell getParent() {
        return parent;
    }
    
    public int getI() {
        return i;
    }
    
    public int getJ() {
        return j;
    }
    
    @Override
    public String toString(){
        return "["+this.i+", "+this.j+"]";
    }
}
