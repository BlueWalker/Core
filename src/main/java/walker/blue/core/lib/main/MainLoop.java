package walker.blue.core.lib.main;

import android.content.Context;
import android.util.Log;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.core.lib.beacon.SyncBeaconScanClient;
import walker.blue.core.lib.common.ProcessCommon;
import walker.blue.core.lib.init.InitializeProcess;
import walker.blue.core.lib.types.Building;
import walker.blue.core.lib.user.UserTracker;
import walker.blue.core.lib.user.UserState;
import walker.blue.path.lib.GridAStar;
import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.RectCoordinates;
import walker.blue.tri.lib.Trilateration;

/**
 * Class representing the main loop of the system
 */
public class MainLoop extends ProcessCommon implements Callable<MainLoop.Output> {

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
    private Set<Beacon> beacons;
    /**
     * Handler in charge of defining what happens when each state occurs
     */
    private UserStateHandler userStateHandler;
    private GridAStar aStar = new GridAStar();

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
        this.userTracker = new UserTracker(initOutput.getPath(), 3.0f, 1.0f);
        this.userTracker.updateUserState(initOutput.getCurrentLocation().getLocation());
        this.scanClient = new SyncBeaconScanClient(context);
        this.beacons = null;
        this.userStateHandler = userStateHandler;
    }

    @Override
    public Output call() {
        final Future<Set<Beacon>> beaconsFuture = this.scanClient.startScan();
        if (this.beacons != null && !this.beacons.isEmpty()) {
//            final GridNode currentLocation = this.getUserLocationProximity(this.beacons, this.building);
            final GridNode currentLocation = myLocationThing();
            this.userTracker.updateUserState(currentLocation.getLocation());
            this.userStateHandler.newStateFound(this.userTracker.getUserState());
        }
        try {
            this.beacons = new HashSet<>(beaconsFuture.get());
        } catch (final InterruptedException | ExecutionException e) {
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
        this.userTracker = new UserTracker(path, 3.0f, 1.0f);
    }

    private Iterator<GridNode> currentSubPath;
    protected GridNode myLocationThing() {
        if (currentSubPath == null || !currentSubPath.hasNext()) {
            this.currentSubPath = this.aStar.findPath(this.building.getSearchSpace().get(3),
                    rc2Gn(this.userTracker.getLatestLocation()),
                    this.userTracker.getNextNode()).iterator();
        }
        return currentSubPath.next();
    }

    private GridNode rc2Gn(final RectCoordinates rc) {
        return this.building.getSearchSpace()
                .get(rc.getZ())
                .get(rc.getY())
                .get(rc.getX());
    }

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
