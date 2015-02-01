package walker.blue.core.lib.utils;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.beacon.lib.client.BeaconScanClient;
import walker.blue.core.lib.factories.BeaconClientFactory;

/**
 * Class in charge of detecting the Building ID for the building in which
 * the user is currently located
 */
public class BuildingDetector {

    /**
     * Max time (in milliseconds) which the client will scan for Beacons
     */
    private static final int SCAN_TIME = 5000;
    /**
     * Minimum amount of Beacons which the class needs to determine the building ID
     */
    private static final int MIN_BEACONS = 1;
    /**
     * Amount of time (in milliseconds) the class will wait until checking the
     * set of beacons found
     */
    private static final int SLEEP_TIME = 200;
    /**
     * Client used to scan for Beacons
     */
    private BeaconScanClient beaconScanClient;
    /**
     * Set in which the beacons scanned will be stored
     */
    private Set<Beacon> beacons;

    /**
     * Initializes all the fields for the Class
     *
     * @param context Context used throughout the class
     */
    public BuildingDetector(final Context context) {
        final BeaconClientFactory clientFactory = new BeaconClientFactory(context);
        this.beacons = new HashSet<>();
        this.beaconScanClient = clientFactory.buildClient(this.beacons, SCAN_TIME);
    }

    /**
     * Scans for Beacons and uses them to determine the building ID for the
     * building the user is currently in
     *
     * @return String
     */
    public String getBuildingID() {
        if (!beacons.isEmpty()) {
            beacons.clear();
        }
        this.beaconScanClient.startScanning();
        // TODO make this better
        while(this.beacons.size() < MIN_BEACONS && this.beaconScanClient.isScanning()) {
            try {
                Log.d("#######", "Sleeping: currently have " + beacons.size() + " beacons");
                Thread.sleep(SLEEP_TIME, 0);
            } catch (final InterruptedException e) {/*TODO*/}
        }
        if (this.beaconScanClient.isScanning()) {
            this.beaconScanClient.stopScanning();
        }
        return getGreatestOccurance(beacons);
    }

    /**
     * Finds the most common UUID in the given set of Beacons
     *
     * @param beacons Set of Beacons
     * @return String
     */
    private String getGreatestOccurance(final Set<Beacon> beacons) {
        final Map<String, Integer> idOccurances = new HashMap<>();
        for(final Beacon beacon : this.beacons) {
            if (idOccurances.containsKey(beacon.getUUID())) {
                int currentOccurances = idOccurances.get(beacon.getUUID());
                idOccurances.put(beacon.getUUID(), currentOccurances + 1);
            } else {
                idOccurances.put(beacon.getUUID(), 0);
            }
        }
        String result = null;
        int currentMax = 0;
        for (Map.Entry<String, Integer> entry : idOccurances.entrySet()) {
            if (result == null || entry.getValue() > currentMax) {
                result = entry.getKey();
                currentMax = entry.getValue();
            }
        }
        return result;
    }
}
