package walker.blue.core.lib.main;

import android.content.Context;
import android.util.Log;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.core.lib.beacon.SyncBeaconScanClient;
import walker.blue.core.lib.common.ProcessCommon;
import walker.blue.core.lib.direction.OrientationManager;
import walker.blue.core.lib.indicator.IndicatorView;
import walker.blue.core.lib.init.InitializeProcess;
import walker.blue.core.lib.speech.SpeechSubmitHandler;
import walker.blue.core.lib.types.Building;
import walker.blue.core.lib.user.UserState;
import walker.blue.core.lib.user.UserTracker;
import walker.blue.path.lib.finder.GridAStar;
import walker.blue.path.lib.node.GridNode;
import walker.blue.path.lib.node.RectCoordinates;
import walker.blue.tri.lib.Trilateration;

/**
 * Class representing the main loop of the system
 */
public class MainLoop extends ProcessCommon implements Callable<MainLoop.Output> {

    /**
     * Log messages
     */
    private static final String LOG_NULL_BEACONS = "List of Beacons is NULL";
    private static final String LOG_EMPTY_BEACONS = "List of Beacons is empty";
    private static final String LOG_NUM_BEACONS = "Number of Beacons: %d";
    private static final String LOG_BEACON_VALS = "\t Beacon Major: %d Minor: %d RSSIVals: %s";
    private static final String LOG_SCAN_FAILED = "Failed consuming Beacons from client";
    /**
     * Amount of time (in ms) the client will scan for beacons
     */
    private static final int CLIENT_SCAN_TIME = 1000;
    /**
     * The zone offset used in the user tracker
     */
    private static final double ZONE_OFFSET = 2.0f;
    /**
     * The destination offset used in the user tracker
     */
    private static final double DESTINATION_OFFSET = 1.5f;

    /**
     * Trilatertion object used to calculate the users location
     */
    private Trilateration trilateration;
    /**
     * Building object representing the building the user is currently in
     */
    private Building building;
    /**
     * UserTracker used to keep track of the users current state
     */
    private UserTracker userTracker;
    /**
     * Client used to scan for beacons in each iteration of the main loop
     */
    private SyncBeaconScanClient scanClient;
    /**
     * Current set of beacons being parsed
     */
    private List<Beacon> beacons;
    /**
     * Handler in charge of defining what happens when each state occurs
     */
    private UserStateHandler userStateHandler;

    /**
     * Consturctor sets the fields using the given values
     *
     * @param initOutput output of the initialize process
     * @param context context under which the main loop is being run
     * @param userStateHandler handler for the user states
     */
    public MainLoop(final InitializeProcess.Output initOutput,
                    final Context context,
                    final UserStateHandler userStateHandler) {
        this(initOutput, context, userStateHandler, null, null, null);
    }

    /**
     * Consturctor sets the fields using the given values
     *
     * @param initOutput output of the initialize process
     * @param context context under which the main loop is being run
     * @param userStateHandler handler for the user states
     * @param orientationManager orientation manager user to get data
     *                           for the user direction
     */
    public MainLoop(final InitializeProcess.Output initOutput,
                    final Context context,
                    final UserStateHandler userStateHandler,
                    final OrientationManager orientationManager) {
        this(initOutput, context, userStateHandler, orientationManager, null, null);
    }

    /**
     * Consturctor sets the fields using the given values
     *
     * @param initOutput output of the initialize process
     * @param context context under which the main loop is being run
     * @param userStateHandler handler for the user states
     * @param speechSubmitHandler speech submitter used in the main loop
     */
    public MainLoop(final InitializeProcess.Output initOutput,
                    final Context context,
                    final UserStateHandler userStateHandler,
                    final SpeechSubmitHandler speechSubmitHandler) {
        this(initOutput, context, userStateHandler, null, speechSubmitHandler, null);
    }

    /**
     * Contructor, sets and initializes the fields using the given values
     *
     * @param initOutput output of the initialize process
     * @param context context under which the main loop is being run
     * @param userStateHandler handler for the user states
     * @param orientationManager orientation manager user to get data
     *                           for the user direction
     * @param speechSubmitHandler speech submitter used in the main loop
     * @param indicatorView indicator view being displayed throughout the main loop
     */
    public MainLoop(final InitializeProcess.Output initOutput,
                    final Context context,
                    final UserStateHandler userStateHandler,
                    final OrientationManager orientationManager,
                    final SpeechSubmitHandler speechSubmitHandler,
                    final IndicatorView indicatorView) {
        this.trilateration = initOutput.getTrilateration();
        this.building = initOutput.getBuilding();
        this.userTracker = new UserTracker(speechSubmitHandler,
                initOutput.getPath(),
                ZONE_OFFSET,
                DESTINATION_OFFSET,
                orientationManager,
                indicatorView,
                this.building);
        this.userTracker.updateUserState(initOutput.getCurrentLocation().getLocation());
        this.scanClient = new SyncBeaconScanClient(context);
        this.scanClient.setScanTime(CLIENT_SCAN_TIME);
        this.beacons = null;
        this.userStateHandler = userStateHandler;
    }

    @Override
    public Output call() {
        final Future<List<Beacon>> beaconsFuture = this.scanClient.startScan();
        if (this.beacons == null) {
            Log.d(this.getClass().getName(), LOG_NULL_BEACONS);
        } else if (this.beacons.isEmpty()) {
            Log.d(this.getClass().getName(), LOG_EMPTY_BEACONS);
        } else {
            Log.d(this.getClass().getName(), String.format(LOG_NUM_BEACONS, this.beacons.size()));
            this.logBeaconRSSIVals(this.beacons);
            final GridNode currentLocation = this.getUserLocationProximity(beacons, building);
//            final GridNode currentLocation = this.debugLocationGet();
            this.userTracker.updateUserState(currentLocation.getLocation());
            this.userStateHandler.newStateFound(this.userTracker.getUserState());
        }
        try {
            this.beacons = beaconsFuture.get();
        } catch (final InterruptedException | ExecutionException e) {
            Log.d(this.getClass().getName(), LOG_SCAN_FAILED, e);
            this.beacons = null;
        }
        return new Output(this.userTracker.getLatestLocation(), this.userTracker.getUserState());
    }

    /**
     * Logs the given Beacons
     *
     * @param beacons Beacons being logged
     */
    private void logBeaconRSSIVals(final List<Beacon> beacons) {
        for (final Beacon b : beacons) {
            Log.d(this.getClass().getName(),
                    String.format(LOG_BEACON_VALS,b.getMajor(), b.getMinor(), b.getMeasuredRSSIValues().toString()));
            Log.d(this.getClass().getName(), "\t" + b.getAverageRSSIValue());
        }
    }

    /**
     * Holds the output for the mainloop
     */
    public class Output {

        private RectCoordinates currentLocation;
        private UserState userState;

        private Output(final RectCoordinates currentLocation, final UserState userState) {
            this.currentLocation = currentLocation;
            this.userState = userState;
        }

        public RectCoordinates getCurrentLocation() {
            return this.currentLocation;
        }

        public UserState getUserState() {
            return this.userState;
        }
    }

    /* ############### DEBUG STUFF ############### */

    /**
     * Iterator for subpaht using in the debugLocationGet method
     */
    private Iterator<GridNode> currentSubPath;
    /**
     * Iterator for subpaht using in the debugLocationGet method
     */
    private Iterator<GridNode> currentWrongSubPath;
    /**
     * Pathfinder used by the debugLocationGet method
     */
    private GridAStar aStar = new GridAStar();
    private int followCourse = 0;

    public void setFollowCourse(final int followCourse) {
        this.followCourse = followCourse;
    }

    /**
     * Method used to test the user tracker
     *
     * @return GridNode
     */
    private GridNode debugLocationGet() {
        switch (this.followCourse ) {
            case 0:
                return this.debugLocationGetOnCourse();
            case 1:
                return this.debugLocationGetOffCourse();
            case 2:
                return this.debugLocationGetWarn();
            default:
                return this.debugLocationGetOnCourse();
        }
    }

    private GridNode debugLocationGetOnCourse() {
        if (currentSubPath == null || !currentSubPath.hasNext()) {
            this.currentSubPath = this.aStar.findPath(this.building.getSearchSpace().get(3),
                    rc2Gn(this.userTracker.getLatestLocation()),
                    this.userTracker.getNextNode()).iterator();
        }

        return currentSubPath.next();
    }

    private GridNode debugLocationGetOffCourse() {
        if (currentWrongSubPath == null || !currentWrongSubPath.hasNext()) {
            final GridNode wrongDestination = this.building.getSearchSpace().get(3).get(38).get(17); // Room 432
            final List<GridNode> currentWrongSubPathList = this.aStar.findPath(this.building.getSearchSpace().get(3),
                    rc2Gn(this.userTracker.getLatestLocation()),
                    wrongDestination);
            for (final GridNode node : currentWrongSubPathList) {
            }
            this.currentWrongSubPath = currentWrongSubPathList.iterator();
        }
        return currentWrongSubPath.next();
    }

    private GridNode debugLocationGetWarn() {
        if (currentWrongSubPath == null) {
            final GridNode wrongDestination = this.building.getSearchSpace().get(3).get(38).get(19); // Room 432
            final List<GridNode> currentWrongSubPathList = this.aStar.findPath(this.building.getSearchSpace().get(3),
                    rc2Gn(this.userTracker.getLatestLocation()),
                    wrongDestination);
            for (final GridNode node : currentWrongSubPathList) {
            }
            this.currentWrongSubPath = currentWrongSubPathList.iterator();
        } else if (!currentWrongSubPath.hasNext()) {
            return this.building.getSearchSpace().get(3).get(38).get(19);
        }
        return currentWrongSubPath.next();
    }

    /**
     * Converts the given RectCoordinates to a GridNode object
     *
     * @param rc RectCoordinates being converted to a GridNode object
     * @return GridNode object correspoinding to the given RectCoordinates
     */
    private GridNode rc2Gn(final RectCoordinates rc) {
        return this.building.getSearchSpace()
                .get(rc.getZ())
                .get(rc.getY())
                .get(rc.getX());
    }
}
