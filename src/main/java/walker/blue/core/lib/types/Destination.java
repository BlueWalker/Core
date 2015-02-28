package walker.blue.core.lib.types;

import walker.blue.path.lib.RectCoordinates;

/**
 * Class representing a Destination within a building
 */
public class Destination {

    /**
     * Name of the destination
     */
    private String name;
    /**
     * Coordinates of the location fo the destination
     */
    private RectCoordinates location;

    /**
     * Constructor. Sets the fields of the class to the given values
     *
     * @param name String
     * @param location RectCoordinates
     */
    public Destination(final String name, final RectCoordinates location) {
        this.name = name;
        this.location = location;
    }

    /**
     * Get the name of the destination
     *
     * @return String
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the location of the Destination
     *
     * @return RectCoordinates
     */
    public RectCoordinates getLocation() {
        return this.location;
    }
}
