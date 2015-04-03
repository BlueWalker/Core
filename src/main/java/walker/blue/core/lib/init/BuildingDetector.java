package walker.blue.core.lib.init;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.core.lib.beacon.SyncBeaconScanClient;

/**
 * Class in charge of detecting the Building ID for the building in which
 * the user is currently located
 */
public class BuildingDetector implements Callable<BuildingDetector.Output> {

    /**
     * Log Messages
     */
    private static final String LOG_LOCKING_THREAD = "Acquiring semaphore (BD) to lock thread # %d";
    private static final String LOG_THREAD_UNBLOCKED = "Locked thread (BD) (id # %d) unblocked";
    private static final String LOG_FAILED_ACQUIRE = "Failed acquiring semaphore - %s";
    /**
     * Max time (in milliseconds) which the client will scan for Beacons
     */
    private static final int SCAN_TIME = 5000;
    /**
     * Number of permits allowed in the semaphore
     */
    private static final int NUMBER_OF_PERMITS = 1;
    /**
     * Minimum amount of Beacons which the class needs to determine the building ID
     */
    private static final int MIN_BEACONS = 1;

    /**
     * Client used to scan for Beacons
     */
    private SyncBeaconScanClient beaconScanClient;
    /**
     * Set in which the beacons scanned will be stored
     */
    private Set<Beacon> beacons;
    /**
     * Semaphore used to lock the thread until the minimum number of beacons
     * are found
     */
    private Semaphore bdLock;

    /**
     * Initializes all the fields for the Class
     *
     * @param context Context used throughout the class
     */
    public BuildingDetector(final Context context) {
        this.beacons = new HashSet<>();
        this.bdLock = new Semaphore(NUMBER_OF_PERMITS);
        this.beaconScanClient = new SyncBeaconScanClient(context, this.beacons, SCAN_TIME, MIN_BEACONS, bdLock);
    }

    @Override
    public Output call() {
        if (!beacons.isEmpty()) {
            beacons.clear();
        }
        final Future<Set<Beacon>> futureBeacons = this.beaconScanClient.startScan();
        try {
            Log.d(this.getClass().getName(), String.format(LOG_LOCKING_THREAD, Thread.currentThread().getId()));
            bdLock.acquire();
            Log.d(this.getClass().getName(), String.format(LOG_THREAD_UNBLOCKED, Thread.currentThread().getId()));
        } catch (Exception e) {
            Log.d(this.getClass().getName(), String.format(LOG_FAILED_ACQUIRE, e.getMessage()));
            return null;
        } finally {
            bdLock.release();
        }
        return new Output(futureBeacons, this.getGreatestOccurance(beacons));
    }

    /**
     * Finds the most common UUID in the given set of Beacons
     *
     * @param beacons Set of Beacons
     * @return String
     */
    private String getGreatestOccurance(final Set<Beacon> beacons) {
        final Map<String, Integer> idOccurances = new HashMap<>();
        for(final Beacon beacon : beacons) {
            if (idOccurances.containsKey(beacon.getUUID())) {
                int currentOccurances = idOccurances.get(beacon.getUUID());
                idOccurances.put(beacon.getUUID(), currentOccurances + 1);
            } else {
                idOccurances.put(beacon.getUUID(), 0);
            }
        }
        String result = null;
        int currentMax = 0;
        for (final Map.Entry<String, Integer> entry : idOccurances.entrySet()) {
            if (result == null || entry.getValue() > currentMax) {
                result = entry.getKey();
                currentMax = entry.getValue();

            }
        }
        return result;
    }

    /**
     * Class holding the ouputs of the BuildingDetector
     */
    public class Output {

        /**
         * Future of the beacons being scanned by the client
         */
        private Future<Set<Beacon>> futureBeacons;
        /**
         * Id of the building the user is currently in
         */
        private String buildingID;

        /**
         * Contructor, Sets the fields of the class to the given values
         *
         * @param futureBeacons Future of the beacons being scanned by the client
         * @param buildingID Id of the building the user is currently in
         */
        private Output(final Future<Set<Beacon>> futureBeacons, final String buildingID) {
            this.futureBeacons = futureBeacons;
            this.buildingID = buildingID;
        }

        /**
         * Getter method for the futureBeacons field
         *
         * @return Future of the beacons being scanned by the client
         */
        public Future<Set<Beacon>> getFuture() {
            return this.futureBeacons;
        }

        /**
         * Getter method for the buildingID field
         *
         * @return Id of the building the user is currently in
         */
        public String getBuildingID() {
            return this.buildingID;
        }
    }
}
