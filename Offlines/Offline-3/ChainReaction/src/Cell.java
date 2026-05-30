
public class Cell {

    private int orbCount ;
    private COLOR orbColor ;
    private int cellColumn ;
    private int cellRow ;
    private  int criticalMass ;


    public Cell(int orbCount, COLOR orbColor, int cellRow , int cellColumn,  int NumRow ,int NumColumn ) {
        this.orbCount = orbCount;
        this.orbColor = orbColor;
        this.cellColumn = cellColumn;
        this.cellRow = cellRow;
        this.criticalMass = calculateCriticalMass(NumRow , NumColumn) ;
    }


    int calculateCriticalMass(int NumRow ,int NumColumn) {
//        System.out.println("NumRow: " + NumRow + ", NumColumn: " + NumColumn + ", cellRow: " + cellRow + ", cellColumn: " + cellColumn);
        if( (cellRow == 0 || cellRow == NumRow - 1) && (cellColumn == 0 || cellColumn == NumColumn - 1)) {
            return 2;
        }
        else if( cellRow == 0 || cellRow == NumRow - 1 || cellColumn == 0 || cellColumn == NumColumn - 1) {
            return 3;
        }
        else {
            return 4;
        }
    }





    public int getCriticalMass() {
        return criticalMass;
    }

    public boolean isEmpty() {
        return orbCount == 0;
    }

    public boolean willExplode() {
        return orbCount >= criticalMass;
    }

    @Override
    public boolean equals(Object other){
        if(this == other) return true;
        if(!(other instanceof Cell otherCell)) return false;
        return this.cellColumn == otherCell.cellColumn &&
               this.cellRow == otherCell.cellRow;
    }

    public boolean canTakeAction(Player player){
        return isEmpty() || orbColor == player.getColor();
    }


    public COLOR getOrbColor() {
        return orbColor;
    }

    public void setOrbColor(COLOR orbColor) {
        this.orbColor = orbColor;
    }

    public int getCellColumn() {
        return cellColumn;
    }

    public void setCellColumn(int cellColumn) {
        this.cellColumn = cellColumn;
    }

    public int getCellRow() {
        return cellRow;
    }

    public void setCellRow(int cellRow) {
        this.cellRow = cellRow;
    }


    public int getOrbCount() {
        return orbCount;
    }

    public void setOrbCount(int orbCount) {
        this.orbCount = orbCount;
    }

    @Override
    public String toString() {
        return "Cell{" +
                "orbCount=" + orbCount +
                ", orbColor=" + orbColor +
                ", cellColumn=" + cellColumn +
                ", cellRow=" + cellRow +
                ", criticalMass=" + criticalMass +
                '}';
    }
}
