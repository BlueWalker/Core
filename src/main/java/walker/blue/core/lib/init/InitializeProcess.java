package walker.blue.core.lib.init;

import android.content.Context;
import android.util.Log;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.core.lib.utils.BuildingDetector;

/**
 * Initialization process of the Bluewalker core package
 */
public class InitializeProcess implements Callable<String> {

    private static final String LOG_FAILED_BD = "Consuming BuildingDetector Future failed - %s";
    private static final String LOG_FAILED_BEACONS= "Consuming beacons Future failed - %s";
    private static final String LOG_RECIEVED_BUILDING_ID = "Received buidling ID - %s";
    private static final String LOG_BEACONS_FUTURE = "Consuming Beacons Future";
    private static final String LOG_BEACONS_DONE = "Finished consuming Beacons Future. %s beacons found";

    private Context context;
    private BuildingDetector buildingDetector;
    private ExecutorService executorService;

    public InitializeProcess(final Context context) {
        this.context = context;
        this.buildingDetector = new BuildingDetector(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public String call() {
        BuildingDetector.Output output;
        try {
            output = this.executorService.submit(this.buildingDetector).get();
            Log.d(this.getClass().getName(), String.format(LOG_RECIEVED_BUILDING_ID, output.getBuildingID()));
        } catch (Exception e) {
            Log.d(this.getClass().getName(), String.format(LOG_FAILED_BD, e.getMessage()));
            return null;
        }

        try {
            Log.d(this.getClass().getName(), LOG_BEACONS_FUTURE);
            final Set<Beacon> beaconSet = output.getFuture().get();
            Log.d(this.getClass().getName(), String.format(LOG_BEACONS_DONE, beaconSet.size()));
        } catch (Exception e ) {
            Log.d(this.getClass().getName(), String.format(LOG_FAILED_BEACONS, e.getMessage()));
            return null;
        }
        return output.getBuildingID();
    }
}