package ludo.core.entities;

public class Cell {
    private final int x;
    private final int y;
    private final String color; // Color of the home base if applicable
    private final String type; // Type of cell (home base, safe spot, etc.)

    public Cell(int x, int y, String color, String type) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.type = type;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public String getColor() { return color; }
    public String getType() { return type; }

//    public class Cell {
//        private final int x;
//        private final int y;
//
//        public Cell(int x, int y) {
//            this.x = x;
//            this.y = y;
//        }
//
//        public int getX() {
//            return x;
//        }
//
//        public int getY() {
//            return y;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (!(obj instanceof Cell)) return false;
//            Cell other = (Cell) obj;
//            return x == other.x && y == other.y;
//        }
//
//        @Override
//        public int hashCode() {
//            return 31 * x + y;
//        }
//    }
}
