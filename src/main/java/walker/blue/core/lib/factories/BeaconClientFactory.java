package walker.blue.core.lib.factories;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.util.Collection;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.beacon.lib.beacon.BluetoothDeviceToBeacon;
import walker.blue.beacon.lib.client.BeaconClientBuilder;
import walker.blue.beacon.lib.client.BeaconScanClient;

/**
 * Factory class to build BeaconScanClients
 */
public class BeaconClientFactory {

    /**
     * Context used to create clients
     */
    private Context context;

    /**
     * Constructor for the factory.
     *
     * @param context Context
     */
    public BeaconClientFactory(final Context context) {
        this.context = context;
    }

    /**
     * Creates a BeaconScanClient used to check which building the user is in
     *
     * @param beacons Set of Beacons
     * @param scanTime int
     * @return
     */
    public BeaconScanClient buildClient(final Collection<Beacon> beacons, final int scanTime) {
        return new BeaconClientBuilder()
                .scanInterval(scanTime)
                .setContext(this.context)
                .setLeScanCallback(buildCallback(beacons))
                .build();
    }

    /**
     * Builds the LeScanCallback which is used in the BeaconScanClient used to
     * check which building the user is in
     *
     * @param beacons Set of Beacons
     * @return LeScanCallback
     */
    private BluetoothAdapter.LeScanCallback buildCallback(final Collection<Beacon> beacons) {
        return new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                final Beacon beacon = BluetoothDeviceToBeacon.toBeacon(device, rssi, scanRecord);
                if (beacon != null) {
                    beacons.add(beacon);
                }
            }
        };
    }
}
