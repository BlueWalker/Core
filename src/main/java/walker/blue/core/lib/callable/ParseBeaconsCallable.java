package walker.blue.core.lib.callable;

import java.util.Set;
import java.util.concurrent.Callable;

import walker.blue.beacon.lib.beacon.Beacon;

/**
 * Created by noname on 1/31/15.
 */
public class ParseBeaconsCallable implements Callable<Object> {

    private Set<Beacon> beacons;

    public ParseBeaconsCallable(final Set<Beacon> beacons) {
        this.beacons = beacons;
    }

    @Override
    public Object call() throws Exception {
        return null;
    }
}