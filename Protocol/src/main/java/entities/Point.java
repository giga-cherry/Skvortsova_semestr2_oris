package entities;

public class Point {
    //Attributes
    private double x;
    private double y;

    //Constructor
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    //Get & Set methods
    public double getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    //toString method
    public String toString() {
        return x + ", " + y;
    }
}

