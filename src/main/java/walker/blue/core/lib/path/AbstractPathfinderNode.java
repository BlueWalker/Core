package walker.blue.core.lib.path;

public abstract class AbstractPathfinderNode implements Comparable<AbstractPathfinderNode> {
    /**
     * Used to give an x, y, and z position of the node in 3D space.
     */
    protected RectCoordinates location;
    /**
     * Holds the parent node that will help with following the path
     */
    protected AbstractPathfinderNode parent;
    /**
     * Distance from the starting node to this node following the current path
     */
    protected double g;
    /**
     * Estimated distance from this node to the destination node
     */
    protected double h;

    /**
     * Constructor. Initializes the location field to a new location using the
     * given x,y,z values
     *
     * @param x int
     * @param y int
     * @param z int
     */
    public AbstractPathfinderNode(int x, int y, int z) {
        this(new RectCoordinates(x, y, z));
    }

    /**
     * Constructor. Initializes the location field to the given RectCoordinates
     *
     * @param location RectCoordinates
     */
    public AbstractPathfinderNode(RectCoordinates location) {
        this.location = location;
        this.g = 0.0;
        this.h = 0.0;
    }

    /**
     * Gets the location for the Node
     *
     * @return RectCoordinates
     */
    public RectCoordinates location() {
        return location;
    }

    /**
     * Gets the parent of the given Node
     *
     * @return AbstractPathfinderNode
     */
    public AbstractPathfinderNode parent() {
        return parent;
    }

    /**
     * Sets the parent of the given node to the given node
     *
     * @param par AbstractPathfinderNode
     */
    public void setParent(AbstractPathfinderNode par) {
        parent = par;
    }

    /**
     * Gets the g value for the node
     *
     * @return double
     */
    public double g() {
        return g;
    }

    /**
     * Sets the value of g for the node
     *
     * @param val double
     */
    public void setG(double val) {
        g = val;
    }

    /**
     * Gets the value of h for the Node
     *
     * @return double
     */
    public double h() {
        return h;
    }

    /**
     * Sets the value of h of the node
     *
     * @param val double
     */
    public void setH(double val) {
        h = val;
    }

    @Override
    public int compareTo(AbstractPathfinderNode another) {
        double f = this.h + this.g;
        double anotherF = another.h + another.g;
        if(f < anotherF) {
            return -1;
        }
        else if(f > anotherF) {
            return 1;
        }
        else {
            return 0;
        }
    }
}

