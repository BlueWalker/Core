package walker.blue.core.lib.beacon;

import java.util.Comparator;
import java.util.List;

import walker.blue.beacon.lib.beacon.Beacon;

public class BeaconComparator implements Comparator<Beacon> {

    @Override
    public int compare(final Beacon lhs, final Beacon rhs) {
        final double lhsAveragePower = this.getAveragePower(lhs);
        final double rhsAveragePower = this.getAveragePower(rhs);

        return (lhsAveragePower < rhsAveragePower) ? -1 : (lhsAveragePower > rhsAveragePower) ? 1 : 0;
    }

    private double getAveragePower(final Beacon beacon) {
        int total = 0;
        final List<Integer> rssiValues =  beacon.getMeasuredRSSIValues();
        for (final int rssi : rssiValues) {
            total += rssi;
        }
        return total / rssiValues.size();
    }
}
