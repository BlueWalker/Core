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
import walker.blue.core.lib.init.InitializeProcess;
import walker.blue.core.lib.types.Building;
import walker.blue.core.lib.user.UserState;
import walker.blue.core.lib.user.UserTracker;
import walker.blue.path.lib.GridAStar;
import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.RectCoordinates;
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
    private static final double ZONE_OFFSET = 3.0f;
    /**
     * The destination offset used in the user tracker
     */
    private static final double DESTINATION_OFFSET = 2.0f;

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
     * Contructor, sets and initializes the fields using the given values
     *
     * @param initOutput Output of the InitializeProcess used before the
     *                   MainLoop
     * @param context Context under which the MainLoop is being run
     * @param userStateHandler Handler in charge of the behavior of the
     *                         application once each state appears
     */
    public MainLoop(final InitializeProcess.Output initOutput,
                    final Context context,
                    final UserStateHandler userStateHandler) {
        this.trilateration = initOutput.getTrilateration();
        this.building = initOutput.getBuilding();
        this.userTracker = new UserTracker(initOutput.getPath(), ZONE_OFFSET, DESTINATION_OFFSET);
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
            Log.d(this.getClass().getName(), "current location: " + currentLocation.toString());
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
     * Updates the class in order to set the current path of the user to the
     * given path
     *
     * @param path New path being set for the user
     */
    public void setNewPath(final List<GridNode> path) {
        this.userTracker = new UserTracker(path, ZONE_OFFSET, DESTINATION_OFFSET);
    }

    public UserTracker getUserTracker() {
        return this.userTracker;
    }

    /**
     * Iterator for subpaht using in the debugLocationGet method
     */
    private Iterator<GridNode> currentSubPath;
    /**
     * Pathfinder used by the debugLocationGet method
     */
    private GridAStar aStar = new GridAStar();

    /**
     * Method used to test the user tracker
     *
     * @return GridNode
     */
    private GridNode debugLocationGet() {
        if (currentSubPath == null || !currentSubPath.hasNext()) {
            this.currentSubPath = this.aStar.findPath(this.building.getSearchSpace().get(3),
                    rc2Gn(this.userTracker.getLatestLocation()),
                    this.userTracker.getNextNode()).iterator();
        }
        return currentSubPath.next();
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

    /**
     * Logs the given Beacons
     *
     * @param beacons Beacons being logged
     */
    private void logBeaconRSSIVals(final List<Beacon> beacons) {
        for (final Beacon b : beacons) {
            Log.d(this.getClass().getName(),
                    String.format(LOG_BEACON_VALS,b.getMajor(), b.getMinor(), b.getMeasuredRSSIValues().toString()));
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
}
