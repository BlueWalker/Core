package walker.blue.core.lib.rect;

/**
 * Class representing the coordinate system being used
 */
public class RectCoordinates {

    /**
     * Coordinate values
     */
    private int x;
    private int y;
    private int z;

    /**
     * Class constructor. Sets the x, y, and z coordinates
     *
     * @param x int
     * @param y int
     * @param z int
     */
    public RectCoordinates(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Get the value of the x coordinate
     * @return int
     */
    public int getXValue() {
        return x;
    }

    /**
     * Get the value of the y coordiniate
     *
     * @return int
     */
    public int getYValue() {
        return y;
    }

    /**
     * Get the value of the z coordiniate
     *
     * @return int
     */
    public int getZValue() {
        return z;
    }
}
