package walker.blue.core.lib.path;

/**
 * Created by Josh on 12/14/2014.
 */
public class GridNode extends AbstractPathfinderNode {
    /**
     * Holds the state of the node, whether its a wall or a walkable area
     */
    protected boolean walkable;

    /**
     * Constructor. Sets the location field to a new instance of
     * RectCoordinates created using the given values of x, y, x.
     * Sets the value of the walkable field to the given boolean
     *
     * @param x int
     * @param y int
     * @param z int
     * @param walkable boolean
     */
    public GridNode(int x, int y, int z, boolean walkable) {
        this(new RectCoordinates(x, y, z), walkable);
    }

    /**

     *
     * @param location RectCoordinate
     * @param walkable boolean
     */
    public GridNode(RectCoordinates location, boolean walkable) {
        super(location);
        this.walkable = walkable;
    }

    /**
     * Gets the value of walkable for the Node
     *
     * @return boolean
     */
    public boolean walkable() {
        return this.walkable;
    }
}
