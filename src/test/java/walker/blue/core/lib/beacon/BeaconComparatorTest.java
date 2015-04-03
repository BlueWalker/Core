package walker.blue.core.lib.beacon;

import junit.framework.Assert;

import org.junit.Test;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.beacon.lib.beacon.BeaconBuilder;

public class BeaconComparatorTest {

    private static final BeaconComparator comparator = new BeaconComparator();
    private static final Beacon beacon1 = new BeaconBuilder()
            .setBeaconMajor(1)
            .setBeaconMinor(1)
            .setMeasuredRSSI(-100)
            .setMeasuredRSSI(-99)
            .setMeasuredRSSI(-98)
            .setMeasuredRSSI(-97)
            .build();
    private static final Beacon beacon2 = new BeaconBuilder()
            .setBeaconMajor(2)
            .setBeaconMinor(2)
            .setMeasuredRSSI(-80)
            .setMeasuredRSSI(-79)
            .setMeasuredRSSI(-78)
            .setMeasuredRSSI(-77)
            .build();

    @Test
    public void testNegResult() {
        Assert.assertTrue(this.comparator.compare(beacon1, beacon2) < 0);
    }

    @Test
    public void testPosResult() {
        Assert.assertTrue(this.comparator.compare(beacon2, beacon1) > 0);
    }

    @Test
    public void testZeroResult() {
        Assert.assertTrue(this.comparator.compare(beacon1, beacon1) == 0);
    }
}
