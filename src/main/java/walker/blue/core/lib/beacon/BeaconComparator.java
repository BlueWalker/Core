package walker.blue.core.lib.beacon;

import java.util.Comparator;

import walker.blue.beacon.lib.beacon.Beacon;

/**
 * Comparator which compares two beacons based on the average value of the
 * measured rssi value
 */
public class BeaconComparator implements Comparator<Beacon> {

    @Override
    public int compare(final Beacon lhs, final Beacon rhs) {
        final double lhsAveragePower = lhs.getAverageRSSIValue();
        final double rhsAveragePower = rhs.getAverageRSSIValue();

        return (lhsAveragePower < rhsAveragePower) ? -1 : (lhsAveragePower > rhsAveragePower) ? 1 : 0;
    }
}
