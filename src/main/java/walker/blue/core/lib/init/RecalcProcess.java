package walker.blue.core.lib.init;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Future;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.core.lib.beacon.SyncBeaconScanClient;
import walker.blue.core.lib.types.Building;
import walker.blue.path.lib.floor.FloorSequencer;
import walker.blue.path.lib.node.GridNode;
import walker.blue.path.lib.finder.ThetaStar;
import walker.blue.tri.lib.Trilateration;

/**
 * Initialization process of the Bluewalker core package
 */
public class RecalcProcess extends InitializeProcess {

    /**
     * The user input
     */
    private InitializeProcess.Output prevOutput;

    /**
     * Constructor. Sets the fields to the given values
     *
     * @param context Context under which the recalculation process is being run
     * @param prevOutput previous output of the initialize process
     */
    public RecalcProcess(final Context context, final InitializeProcess.Output prevOutput) {
        super(context, null);
        this.prevOutput = prevOutput;
    }

    @Override
    public InitializeProcess.Output call() {
        final SyncBeaconScanClient scanClient = new SyncBeaconScanClient(context);
        final Future<List<Beacon>> beaconFutures = scanClient.startScan();

        // Consume beacons being scanned
        final List<Beacon> beaconSet;
        try {
            Log.d(this.getClass().getName(), LOG_BEACONS_FUTURE);
            beaconSet = beaconFutures.get();
            Log.d(this.getClass().getName(), String.format(LOG_BEACONS_DONE, beaconSet.size()));
        } catch (final Exception e) {
            Log.d(this.getClass().getName(), String.format(LOG_FAILED_BEACONS, e.getMessage()));
            return new Output(InitError.BEACONS_FAIL);
        }

        final Building building = this.prevOutput.getBuilding();
        final GridNode destination = this.getDestinationFromPath(this.prevOutput.getPath());

        final Trilateration trilateration = null;
        final GridNode currentNode = this.getUserLocationProximity(beaconSet, building);
        if (currentNode == null) {
            return new Output(InitError.LOCATION_FAIL);
        }

        final ThetaStar thetaStar = new ThetaStar();
        final FloorSequencer floorSequencer = new FloorSequencer(thetaStar,
                building.getSearchSpace(),
                building.getFloorConnectors());
        final List<GridNode> path = floorSequencer.findPath(currentNode, destination);
        if (path == null) {
            return new Output(InitError.PATH_FAIL);
        }

        // Pack everything into the output object and return it
        return new InitializeProcess.Output(floorSequencer,
                trilateration,
                path,
                building,
                currentNode,
                null);
    }

    /**
     * Gets the destination from the given path
     *
     * @param path Path whose destination is being extracted
     * @return destination of the given path
     */
    private GridNode getDestinationFromPath(final List<GridNode> path) {
        if (path.size() > 1) {
            return path.get(path.size() - 1);
        } else {
            return path.get(0);
        }
    }
}