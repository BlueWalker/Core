package walker.blue.core.lib.init;

import android.content.Context;

import java.util.concurrent.Callable;

import walker.blue.core.lib.utils.BuildingDetector;

/**
 * Initialization process of the Bluewalker core package
 */
public class InitializeProcess implements Callable<String> {

    private Context context;
    private BuildingDetector buildingDetector;

    public InitializeProcess(final Context context) {
        this.context = context;
        this.buildingDetector = new BuildingDetector(context);
    }

    public String call() {
        return null;
    }
}