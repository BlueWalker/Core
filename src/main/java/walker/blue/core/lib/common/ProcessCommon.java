package walker.blue.core.lib.common;

import java.util.Collections;
import java.util.Set;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.core.lib.beacon.BeaconComparator;
import walker.blue.core.lib.types.Building;
import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.RectCoordinates;

public abstract class ProcessCommon {

    private BeaconComparator beaconComparator;

    protected GridNode getUserLocationProximity(final Set<Beacon> beacons, final Building building) {
        if (this.beaconComparator == null) {
            this.beaconComparator = new BeaconComparator();
        }
        final Beacon minBeacon = Collections.max(beacons, this.beaconComparator);
        final RectCoordinates beaconLocation = building.getBeaconLocationMap().get(minBeacon);
        return building.getSearchSpace()
                .get(beaconLocation.getZ())
                .get(beaconLocation.getY())
                .get(beaconLocation.getZ());
    }
}
