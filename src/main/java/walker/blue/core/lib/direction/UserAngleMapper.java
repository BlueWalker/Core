package walker.blue.core.lib.direction;

import walker.blue.core.lib.types.Building;
import walker.blue.path.lib.node.RectCoordinates;

/**
 * Map that calcuates the angle between the direction the user is facing and
 * the next waypoint in the path
 */
public class UserAngleMapper {

    /**
     * X offset for the north point
     */
    private int xOffset;
    /**
     * Y offset for the north point
     */
    private int yOffset;

    /**
     * Constructor. Sets the fields using the northpoint of the given building
     *
     * @param building Building the user is currently in
     */
    public UserAngleMapper(final Building building) {
        this(building.getNorthPoint());
    }

    /**
     * Constructor. Sets the fields using the given northPoint
     *
     * @param northPoint Vector pointing north
     */
    public UserAngleMapper(final RectCoordinates northPoint) {
        this.xOffset = northPoint.getX();
        this.yOffset = northPoint.getY();
    }

    /**
     * Calculates the angle between the direction the user is facing and the
     * next waypoint
     *
     * @param userLocation Current location of the user
     * @param heading Heading measured by the device
     * @param nextNode Next node in the path
     * @return angle between the direction the user is facing and the next waypoint
     */
    public float getAngleToNextWaypoint(final RectCoordinates userLocation,
                                        final double heading,
                                        final RectCoordinates nextNode) {
        final DoubleVector userVector = this.getUserFacingVector(userLocation, heading);
        double wayPointVectorX = nextNode.getX() - userLocation.getX();
        double wayPointVectorY = nextNode.getY() - userLocation.getY();
        final double waypointMag = this.getMagnitude(wayPointVectorX, wayPointVectorY);
        wayPointVectorX = wayPointVectorX / waypointMag;
        wayPointVectorY = wayPointVectorY / waypointMag;
        final double cross = this.crossProduct2D(userVector.x, userVector.y, wayPointVectorX, wayPointVectorY);
        int multiplier = 0;
        if (cross > 0) {
            multiplier = 1;
        } else if (cross < 0) {
            multiplier = -1;
        }
        final float angle =
                Math.round(this.getAngleBetweenVectors(userVector.x, userVector.y, wayPointVectorX, wayPointVectorY));
        return multiplier * angle;
    }

    /**
     * Calculates the vecotor of the for the direction in which the user is facing
     *
     * @param userLocation current location of the user
     * @param heading angle between the direction the suer is facing and north
     * @return Vector representing the direction of the user
     */
    private DoubleVector getUserFacingVector(final RectCoordinates userLocation, final double heading) {
        final double headingRads = Math.toRadians(heading);
        final DoubleVector result = new DoubleVector();
        final int northX = userLocation.getX() + xOffset;
        final int northY = userLocation.getY() + yOffset;
        final double endpointXDouble = (Math.cos(headingRads) * northX) + (Math.sin(headingRads) * northY);
        final double endpointYDouble = (Math.sin(headingRads) * northX * -1) + (Math.cos(headingRads) * northY);
        final double mag = this.getMagnitude(endpointXDouble, endpointYDouble);
        result.x = endpointYDouble / mag;
        result.y = endpointXDouble / mag;
        return result;
    }

    /**
     * Gets the angle between two given 2D vectors
     *
     * @param aX X value of vector A
     * @param aY Y value of vector A
     * @param bX X value of vector B
     * @param bY Y value of vector B
     * @return angle between the two vectors
     */
    private double getAngleBetweenVectors(final double aX, final double aY, final double bX, final double bY) {
        final double dot = (aX * bX) + (aY * bY);
        final double magA = this.getMagnitude(aX, aY);
        final double magB = this.getMagnitude(bX, bY);
        return Math.toDegrees(Math.acos(dot / (magA * magB)));
    }

    /**
     * Gets the magnitude of the given 2D vector
     *
     * @param x X value of the vector
     * @param y Y value of the vecotr
     * @return magnitude of the given vector
     */
    private double getMagnitude(final double x, final double y) {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    /**
     * Calculates the cross product between the two given 2D vectors
     *
     * @param aX X value of vector A
     * @param aY Y value of vector A
     * @param bX X value of vector B
     * @param bY Y value of vector B
     * @return cross prduct of the two given cectors
     */
    private double crossProduct2D(final double aX, final double aY, final double bX, final double bY) {
        return (aX * bY) - (aY * bX);
    }

    /**
     * Helper class representing a 2D vector
     */
    private class DoubleVector {
        double x;
        double y;
    }
}