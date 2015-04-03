package walker.blue.core.lib.user;

import android.util.Log;

import java.util.Iterator;
import java.util.List;

import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.RectCoordinates;

// Tracks the user according to the path given.
public class UserTracker {
    /**
     * The current user state according to the given path.
     */
    private UserState userState;
    private Iterator<GridNode> pathIterator;
    /**
     * The last node the user passed in the path.
     */
    private GridNode previousNode;
    /**
     * The next node the user needs to reach in the path.
     */
    private GridNode nextNode;
    private RectCoordinates latestLocation;
    /**
     * Used to provide a padded zone that allows the user to still remain on course.
     * It is used when creating the rectangular boundaries. It is also used as a radius
     * for determining if the user is within vicinity of the destination or the next
     * node in the path that is on a different floor then the previous node's floor.
     */
    private double zoneOffset;
    private double destOffset;

    private PathZone bufferZone;
    private PathZone warningZone;

    public UserTracker(final List<GridNode> path, final double zoneOffset, final double destOffset) {
        this.zoneOffset = zoneOffset;
        this.destOffset = destOffset;
        this.setPath(path);
    }

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

    public UserState getUserState() {
        return this.userState;
    }

    public RectCoordinates getLatestLocation() {
        return this.latestLocation;
    }

    public GridNode getPreviousNode() {
        return previousNode;
    }

    public GridNode getNextNode() {
        return nextNode;
    }
    public void setPath(List<GridNode> path) {
        this.pathIterator = path.iterator();
        this.setProgress(this.pathIterator.next(), this.pathIterator.next());
        this.userState = UserState.UNINITIALIZED;
    }

    private double getDistance(final RectCoordinates a, final RectCoordinates b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2)
                + Math.pow(a.getY() - b.getY(), 2)
                + Math.pow(a.getZ() - b.getZ(), 2));
    }

    private void incrementProgress() {
        this.setProgress(this.nextNode, this.pathIterator.next());
    }

    private void setProgress(final GridNode previousNode, final GridNode nextNode) {
        this.previousNode = previousNode;
        this.nextNode = nextNode;
        this.bufferZone =
                new PathZone(previousNode.getLocation(), nextNode.getLocation(), this.zoneOffset);
        this.warningZone =
                new PathZone(previousNode.getLocation(), nextNode.getLocation(), this.zoneOffset * 2);
    }
}