package walker.blue.core.lib.callable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.beacon.lib.client.BeaconScanClient;

/**
 * Created by noname on 1/31/15.
 */
public class ScanBeaconsCallable implements Callable<Set<Beacon>>{

    private static final int SLEEP_TIME = 200;

    private BeaconScanClient beaconScanClient;
    private Set<Beacon> beacons;

    public ScanBeaconsCallable(final BeaconScanClient beaconScanClient) {
        this.beaconScanClient = beaconScanClient;
        this.beacons = new HashSet<>();
    }

    @Override
    public Set<Beacon> call() throws Exception {
        this.beaconScanClient.startScanning();
        while(this.beaconScanClient.isScanning()) {
            try {
                Thread.sleep(SLEEP_TIME);
                // TODO log something ?
                // TODO write a syncronous client
            } catch (final InterruptedException e) {/*TODO*/}
        }
        return beacons;
    }
}
