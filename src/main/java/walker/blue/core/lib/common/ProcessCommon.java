package walker.blue.core.lib.common;

import android.util.Log;

import java.util.Collections;
import java.util.List;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.core.lib.beacon.BeaconComparator;
import walker.blue.core.lib.types.Building;
import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.RectCoordinates;

/**
 * Base class for the MainLoop and and the Initialize process. IT defines how
 * the users location is found
 */
public abstract class ProcessCommon {

    /**
     * Log Messages
     */
    private static final String LOG_PASSED_DETLA = "Beacons passed delta.";
    private static final String LOG_MAX_BEACON = "Max Beacon: Major: %d Minor: %d AvgRSSI: %f";
    private static final String LOG_SEC_BEACON = "Second Beacon: Major: %d Minor: %d AvgRSSI: %f";
    private static final String LOG_NEW_VALS = "New Values(x,y,z): (%d, %d, %d)";
    /**
     * Allowed delta value to place user between two beacons
     */
    private static final double BEACON_POWER_DELTA = 5;

    /**
     * Comparator used to sort the list of beacons according to their average rssi values
     */
    private BeaconComparator beaconComparator;

    /**
     * Gets the users location by checking which beacon is closest to the user
     * TODO: User calibration RSSI value to better support different kinds of Beacons
     *
     * @param beacons list of beacons scanned
     * @param building object representing which building the user is currently in
     * @return GridNode representing the current location of the user
     */
    protected GridNode getUserLocationProximity(final List<Beacon> beacons, final Building building) {
        if (this.beaconComparator == null) {
            this.beaconComparator = new BeaconComparator();
        }

        if (beacons.size() > 2) {
            Log.d(this.getClass().getName(), "tw0 beacons");
            Collections.sort(beacons, this.beaconComparator);
            final Beacon maxBeacon = beacons.get(beacons.size() - 1);
            final Beacon secondBeacon = beacons.get(beacons.size() - 2);
            if (Math.abs(maxBeacon.getAverageRSSIValue() - secondBeacon.getAverageRSSIValue()) <= BEACON_POWER_DELTA) {
                Log.d(this.getClass().getName(), LOG_PASSED_DETLA);
                this.logBeacon(LOG_MAX_BEACON, maxBeacon);
                this.logBeacon(LOG_SEC_BEACON, secondBeacon);
                final RectCoordinates maxLocation = building.getBeaconLocationMap().get(maxBeacon);
                final RectCoordinates secondLocation = building.getBeaconLocationMap().get(secondBeacon);
                final int newZ = (maxLocation.getZ() + secondLocation.getZ()) / 2;
                final int newY = (maxLocation.getY() + secondLocation.getY()) / 2;
                final int newX = (maxLocation.getX() + secondLocation.getX()) / 2;
                Log.d(this.getClass().getName(), String.format(LOG_NEW_VALS, newX, newY, newZ));
                if (this.searchSpaceContains(building, newX, newY, newZ)) {
                    return building.getSearchSpace()
                            .get(newZ)
                            .get(newY)
                            .get(newX);
                } else {
                    return null;
                }
            }
            return this.getBeaconLocation(building, maxBeacon);
        } else {
            Log.d(this.getClass().getName(), "returning only");
            return this.getBeaconLocation(building, beacons.get(0));
        }
    }

    /**
     * Gets the GridNode for the location of the given beacon
     *
     * @param building Building in which the beacon is located
     * @param beacon beacon whose location is being extracted
     * @return GridNode corresponding to the given beacons location
     */
    private GridNode getBeaconLocation(final Building building, final Beacon beacon) {
        if (!building.getBeaconLocationMap().containsKey(beacon)) {
            return null;
        }
        final RectCoordinates beaconLoc = building.getBeaconLocationMap().get(beacon);
        if (this.searchSpaceContains(building, beaconLoc.getX(), beaconLoc.getY(), beaconLoc.getZ())) {
            Log.d(this.getClass().getName(), "Contains");
            return building.getSearchSpace()
                    .get(beaconLoc.getZ())
                    .get(beaconLoc.getY())
                    .get(beaconLoc.getX());
        } else {
            Log.d(this.getClass().getName(), "null");
            return null;
        }
    }

    private boolean searchSpaceContains(final Building building, final int x, final int y, final int z) {
        final List<List<List<GridNode>>> ss = building.getSearchSpace();
        return ss.size() > z && ss.get(z).size() > y && ss.get(z).get(y).size() > x;
    }

    /**
     * Logs the given beacon using the given format
     *
     * @param f Format  being used in the log message
     * @param b beacon being logged
     */
    private void logBeacon(final String f, final Beacon b) {
        Log.d(this.getClass().getName(), String.format(f, b.getMajor(), b.getMinor(), b.getAverageRSSIValue()));
    }
}
