package walker.blue.core.lib.user;

import java.util.Iterator;
import java.util.List;

import walker.blue.core.lib.direction.OrientationManager;
import walker.blue.core.lib.direction.UserAngleMapper;
import walker.blue.core.lib.indicator.IndicatorView;
import walker.blue.core.lib.speech.GeneratedSpeech;
import walker.blue.core.lib.speech.NodeDirection;
import walker.blue.core.lib.speech.NodeEvent;
import walker.blue.core.lib.speech.SpeechGenerator;
import walker.blue.core.lib.speech.SpeechSubmitHandler;
import walker.blue.core.lib.types.Building;
import walker.blue.path.lib.node.GridNode;
import walker.blue.path.lib.node.RectCoordinates;

/**
 * Class in change of tracking the movement of the user
 */
public class UserTracker implements OrientationManager.OnChangedListener {

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
     * Angle mappper used to feed data to the indicator view
     */
    private UserAngleMapper userAngleMapper;
    /**
     * PathZone defining the warning zone
     */
    private PathZone warningZone;
    /**
     * SpeechSubmitHandler used to communicatr with the user
     */
    private SpeechSubmitHandler speechSubmitHandler;
    /**
     * OrientationManager used to get sensor data
     */
    private OrientationManager orientationManager;
    /**
     * SpeechGenerator used to generate what will displayed and spoken to
     * the user
     */
    private SpeechGenerator speechGenerator;
    /**
     * Indicator view being diplayed to the user
     */
    private IndicatorView indicatorView;

    /**
     * Constructor. Sets the path to the given path and creates the warning
     * and buffer zone
     *
     * @param speechSubmitHandler SpeechSubmitHandler used throughout the class
     * @param path Path which the user is expected to follow
     * @param zoneOffset Offset being used to create the zones
     * @param destOffset Offest being used for the destinations and waypoints
     * @param orientationManager OrientationManager used to get sensor data
     * @param indicatorView IndicatorView being displayed to the user
     * @param building Building the user is currently in
     */
    public UserTracker(final SpeechSubmitHandler speechSubmitHandler,
                       final List<GridNode> path,
                       final double zoneOffset,
                       final double destOffset,
                       final OrientationManager orientationManager,
                       final IndicatorView indicatorView,
                       final Building building) {
        this.speechSubmitHandler = speechSubmitHandler;
        this.zoneOffset = zoneOffset;
        this.destOffset = destOffset;
        this.setPath(path);
        this.indicatorView = indicatorView;
        this.orientationManager = orientationManager;
        if (this.orientationManager != null) {
            this.orientationManager.addOnChangedListener(this);
        }
        this.userAngleMapper = new UserAngleMapper(building);
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
        final double currentDistance = this.getDistance(userLocation, this.nextNode.getLocation());
        this.latestLocation = userLocation;
        if (this.orientationManager != null && !this.orientationManager.isTracking()) {
            this.orientationManager.start();
        }
        if (userLocation.getZ() != this.nextNode.getLocation().getZ()) {
            // TODO Different Floors
        }
        if (!this.warningZone.isPointInside(userLocation)) {
            this.userState = UserState.OFF_COURSE;
        } else if (!this.bufferZone.isPointInside(userLocation)) {
            this.userState = UserState.IN_WARNING_ZONE;
        } else if (currentDistance <= this.destOffset) {
            if (!this.pathIterator.hasNext()) {
                this.userState = UserState.ARRIVED;
            } else {
                this.incrementProgress();
            }
        } else {
            this.userState = UserState.ON_COURSE;
            this.submitCurrentLocationSilent();
        }
    }

    @Override
    public void onAccuracyChanged(final OrientationManager orientationManager) { }

    @Override
    public void onLocationChanged(final OrientationManager orientationManager) { }

    @Override
    public void onOrientationChanged(final OrientationManager orientationManager) {
        float degrees = orientationManager.getHeading();
        final float heading = this.mod(degrees, 360.0f);
        final float angleToNextWaypoint =
                this.userAngleMapper.getAngleToNextWaypoint(this.latestLocation, heading, this.nextNode.getLocation());
        this.indicatorView.drawIndicatorAtAngle(angleToNextWaypoint);
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
        this.speechGenerator = new SpeechGenerator(path);
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
        final GeneratedSpeech actionSpeech =
                this.speechGenerator.getSpeechForNodes(this.previousNode.getLocation(), this.nextNode);
        if (actionSpeech.getDirection() == NodeDirection.BEHIND) {
            return;
        }
        this.submitAction(actionSpeech);
        if (actionSpeech.getEvent() == NodeEvent.REACHING_DESTINATION ||
                actionSpeech.getEvent() == NodeEvent.REACHING_DESTINATION_AHEAD) {
            this.userState = UserState.ARRIVED;
        } else if (this.pathIterator.hasNext()) {
            this.setProgress(this.nextNode, this.pathIterator.next());
        }
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
        this.submitCurrentLocation();
    }

    /**
     * Submits the users current location to be spoken
     */
    private void submitCurrentLocation() {
        this.submitCurrentLocation(false);
    }

    /**
     * Silently submits the current location of the user
     */
    private void submitCurrentLocationSilent() {
        this.submitCurrentLocation(true);
    }

    /**
     * Submits the users current location to be spoken
     *
     * @param silent whether this should be done silently
     */
    private void submitCurrentLocation(final boolean silent) {
        if(this.speechSubmitHandler != null) {
            final RectCoordinates userLocation = (this.latestLocation != null) ?
                    this.latestLocation :
                    this.previousNode.getLocation();
            final GeneratedSpeech speech = this.speechGenerator.getSpeechForNodes(userLocation, this.nextNode);
            if (speech.getDirection() == NodeDirection.BEHIND) {
                return;
            }
            if (silent) {
                this.speechSubmitHandler.submitSilent(speech);
            } else {
                this.speechSubmitHandler.submitQueueAdd(speech);
            }
        }
    }

    /**
     * Submit the given speech if possible
     *
     * @param speech generated speech
     */
    private void submitAction(final GeneratedSpeech speech) {
        if(this.speechSubmitHandler != null) {
            this.speechSubmitHandler.submitAction(speech);
        }
    }

    /**
     * Mod operation
     *
     * @param a
     * @param b
     * @return
     */
    private static float mod(float a, float b) {
        return (a % b + b) % b;
    }

}
