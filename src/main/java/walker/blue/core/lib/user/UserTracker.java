package walker.blue.core.lib.user;

import java.util.Iterator;
import java.util.List;

import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.RectCoordinates;

/**
 * Class in change of tracking the movement of the user
 */
public class UserTracker {

    /**
     * The current user state according to the given path.
     */
    private UserState userState;
    /**
     * Iterator that holds the path
     */
    private Iterator<GridNode> pathIterator;
    /**
     * The last node the user passed in the path.
     */
    private GridNode previousNode;
    /**
     * The next node the user needs to reach in the path.
     */
    private GridNode nextNode;
    /**
     * The latest knows location of the user
     */
    private RectCoordinates latestLocation;
    /**
     * size of the padding used when creating the zones
     */
    private double zoneOffset;
    /**
     * Zone of the padding used around destinations and waypoints
     */
    private double destOffset;
    /**
     * PathZone defining the allowed zone
     */
    private PathZone bufferZone;
    /**
     * PathZone defining the warning zone
     */
    private PathZone warningZone;

    /**
     * Constructor. Sets the path to the given path and creates the warning
     * and buffer zone
     *
     * @param path Path which the user is expected to follow
     * @param zoneOffset Offset being used to create the zones
     * @param destOffset Offest being used for the destinations and waypoints
     */
    public UserTracker(final List<GridNode> path, final double zoneOffset, final double destOffset) {
        this.zoneOffset = zoneOffset;
        this.destOffset = destOffset;
        this.setPath(path);
    }

    /**
     * Updates the state of the user according to the given location
     *
     * @param userLocation New location for the user
     */
    public void updateUserState(final RectCoordinates userLocation) {
        if (this.userState == UserState.ARRIVED) {
            return;
        }
        this.latestLocation = userLocation;
        if (userLocation.getZ() != this.nextNode.getLocation().getZ()) {
            // TODO Different Floors
        }
        if (!this.warningZone.isPointInside(userLocation)) {
            this.userState = UserState.OFF_COURSE;
        } else if (!this.bufferZone.isPointInside(userLocation)) {
            this.userState = UserState.IN_WARNING_ZONE;
        } else if (this.getDistance(userLocation, this.nextNode.getLocation()) <= this.destOffset) {
            if (!this.pathIterator.hasNext()) {
                this.userState = UserState.ARRIVED;
            } else {
                this.incrementProgress();
            }
        } else {
            this.userState = UserState.ON_COURSE;
        }
    }

    /**
     * Getter for the users state
     *
     * @return Current state of the user
     */
    public UserState getUserState() {
        return this.userState;
    }

    /**
     * Getter for the latest location of the user
     *
     * @return Latest location of the user
     */
    public RectCoordinates getLatestLocation() {
        return this.latestLocation;
    }

    /**
     * Getter for the previous node the user passed in the path
     *
     * @return Previous node the user passed in the path
     */
    public GridNode getPreviousNode() {
        return this.previousNode;
    }

    /**
     * Getter for the next node in the path
     *
     * @return Next node in the path
     */
    public GridNode getNextNode() {
        return this.nextNode;
    }

    /**
     * Sets the path being used by the user tracker
     *
     * @param path new path for the usertracker
     */
    public void setPath(final List<GridNode> path) {
        this.pathIterator = path.iterator();
        this.setProgress(this.pathIterator.next(), this.pathIterator.next());
        this.userState = UserState.UNINITIALIZED;
    }

    /**
     * Gets the distance between the given RectCoordinates
     *
     * @param a One of the RectCoordinates
     * @param b One of the RectCoordinates
     * @return distance between the given RectCoordinates
     */
    private double getDistance(final RectCoordinates a, final RectCoordinates b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2)
                + Math.pow(a.getY() - b.getY(), 2)
                + Math.pow(a.getZ() - b.getZ(), 2));
    }

    /**
     * Increments the current position of the user in the path
     */
    private void incrementProgress() {
        this.setProgress(this.nextNode, this.pathIterator.next());
    }

    /**
     * Sets the progress of the user in the path
     *
     * @param previousNode Previous node in the path
     * @param nextNode Next node in the path
     */
    private void setProgress(final GridNode previousNode, final GridNode nextNode) {
        this.previousNode = previousNode;
        this.nextNode = nextNode;
        this.bufferZone =
                new PathZone(previousNode.getLocation(), nextNode.getLocation(), this.zoneOffset);
        this.warningZone =
                new PathZone(previousNode.getLocation(), nextNode.getLocation(), this.zoneOffset * 2);
    }
}