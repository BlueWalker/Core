package walker.blue.core.lib.beacon;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.beacon.lib.beacon.BluetoothDeviceToBeacon;
import walker.blue.beacon.lib.client.BeaconClientBuilder;
import walker.blue.beacon.lib.client.BeaconScanClient;
import walker.blue.beacon.lib.service.ScanEndUserCallback;

/**
 * Wrapper around the BeaconScanClient that allows the output to be consumed
 * using Futures
 */
public class SyncBeaconScanClient {

    /**
     * Log Messages
     */
    private static final String LOG_INITIAL_LOCKING_THREAD = "Acquiring initial semaphore";
    private static final String LOG_LOCKING_THREAD = "Acquiring semaphore to lock thread # %d";
    private static final String LOG_RELEASE_THREAD = "Releasing semaphore";
    private static final String LOG_THREAD_UNBLOCKED = "Locked thread (id # %d) unblocked";
    private static final String LOG_FAILED_ACQUIRE = "Failed acquiring semaphore - %s";
    private static final String LOG_SCAN_START = "Starting syncronous scan in thread id # %d";
    private static final String LOG_INITIAL_LOCKING_BD = "Acquiring initial BuildingDetector semaphore";
    private static final String LOG_BD_RELEASE = "Releasing BuildingDetector semaphore";
    private static final String LOG_BD_LATE_RELEASE = "Not ebough beacons found. Releasing BD lock now";
    /**
     * Default scan time (in ms) for the client
     */
    private static final int DEFAULT_SCAN_TIME = 5000;
    /**
     * Number of permits allowed in the semaphore
     */
    private static final int NUMBER_OF_PERMITS = 1;

    private int minNumOfBeacons = 1;
    /**
     * Semaphore used to lock the thread
     */
    private Semaphore lock;
    /**
     * Semaphore used to lock the building detector until the necessary number
     * of beacons are found
     */
    private Semaphore bdLock;
    /**
     * Collection where the beacons are stored
     */
    private Set<Beacon> beacons;
    /**
     * Executor service used to launch the scan
     */
    private ExecutorService executorService;
    /**
     * Client used to scan for beacons
     */
    private BeaconScanClient beaconScanClient;

    /**
     * Callback used by the client whenever a BLE device is found
     */
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            final Beacon beacon = BluetoothDeviceToBeacon.toBeacon(device, rssi, scanRecord);
            if (beacon != null) {
                beacons.add(beacon);
            }
            if (bdLock != null && beacons.size() >= minNumOfBeacons && bdLock.hasQueuedThreads()) {
                Log.d(this.getClass().getName(), LOG_BD_RELEASE);
                bdLock.release();
            }
        }
    };

    /**
     * Callback used to release the lock
     */
    private ScanEndUserCallback unlockCallback = new ScanEndUserCallback() {
        @Override
        public void execute() {
            if (bdLock != null && bdLock.hasQueuedThreads()) {
                Log.d(this.getClass().getName(), LOG_BD_LATE_RELEASE);
                bdLock.release();
            }
            Log.d(this.getClass().getName(),  LOG_RELEASE_THREAD);
            lock.release();
        }
    };

    /**
     * Callable thats starts the scan and locks the thread
     */
    private Callable<Set<Beacon>> startScanCallable = new Callable<Set<Beacon>>() {
        @Override
        public Set<Beacon> call() {
            Log.d(this.getClass().getName(),  String.format(LOG_SCAN_START, Thread.currentThread().getId()));
            beaconScanClient.startScanning();
            try {
                Log.d(this.getClass().getName(), String.format(LOG_LOCKING_THREAD, Thread.currentThread().getId()));
                lock.acquire();
                Log.d(this.getClass().getName(), String.format(LOG_THREAD_UNBLOCKED, Thread.currentThread().getId()));
            } catch (Exception e) {
                Log.d(this.getClass().getName(), String.format(LOG_FAILED_ACQUIRE, e.getMessage()));
                return null;
            } finally {
                lock.release();
            }
            return beacons;
        }
    };

    /**
     * Constructor. Creates the locking client using the given context
     *
     * @param context Constext under which the client is being used
     */
    public SyncBeaconScanClient(final Context context) {
        this(context, new HashSet<Beacon>());
    }

    /**
     * Constructor. Creates the locking client using the given context and beacons
     *
     * @param context Constext under which the client is being used
     * @param beacons The collection where the beacons will be placed
     */
    public SyncBeaconScanClient(final Context context, final Set<Beacon> beacons) {
        this(context, beacons, DEFAULT_SCAN_TIME, 0, null);
    }

    /**
     * Constructor. Creates the lock, client and executor service using the
     * given contexts, set of beacons and scan time
     *
     * @param context Constext under which the client is being used
     * @param beacons The collection where the beacons will be placed
     * @param scanTime Time in ms that the client will scan
     */
    public SyncBeaconScanClient(final Context context,
                                final Set<Beacon> beacons,
                                final int scanTime,
                                final int minNumOfBeacons,
                                final Semaphore bdLock) {
        this.minNumOfBeacons = minNumOfBeacons;
        this.bdLock = bdLock;
        this.lock = new Semaphore(NUMBER_OF_PERMITS);
        this.beacons = beacons;
        this.executorService = Executors.newSingleThreadExecutor();
        this.beaconScanClient = new BeaconClientBuilder()
                .scanInterval(scanTime)
                .setLeScanCallback(leScanCallback)
                .setContext(context)
                .setUserCallback(unlockCallback)
                .build();
    }

    /**
     * Startes the beacons scan
     *
     * @return Future allowing the set of beacons to be consumed
     */
    public Future<Set<Beacon>> startScan() {
        try {
            Log.d(this.getClass().getName(), LOG_INITIAL_LOCKING_THREAD);
            lock.acquire();
            if (bdLock != null) {
                Log.d(this.getClass().getName(), LOG_INITIAL_LOCKING_BD);
                bdLock.acquire();
            }
        } catch (Exception e) {
            Log.d(this.getClass().getName(), String.format(LOG_FAILED_ACQUIRE, e.getMessage()));
            return null;
        }
        return this.executorService.submit(this.startScanCallable);
    }
}