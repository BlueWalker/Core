package walker.blue.core.lib.direction;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import walker.blue.core.lib.indicator.IndicatorView;
import walker.blue.core.lib.user.UserTracker;
import walker.blue.path.lib.RectCoordinates;

public class SensorListener implements SensorEventListener {

    private UserAngleMapper angleMapper;
    private IndicatorView view;
    private UserTracker userTracker;
    private float[] gravityValues;
    private float[] geoValues;
    private boolean hasInterference;

    public SensorListener(final UserAngleMapper angleMapper,
                          final IndicatorView view,
                          final UserTracker userTracker) {
        this.angleMapper = angleMapper;
        this.view = view;
        this.userTracker = userTracker;
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        Log.d(this.getClass().getName(), "onSensorChanged");
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                Log.d(this.getClass().getName(), "Updating values for TYPE_ACCELEROMETER");
                this.gravityValues = event.values;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                Log.d(this.getClass().getName(), "Updating values for TYPE_MAGNETIC_FIELD");
                this.geoValues = event.values;
                break;
        }
        if (this.gravityValues != null && this.geoValues != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            final boolean success = SensorManager.getRotationMatrix(R, I, this.gravityValues, this.geoValues);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                final float azimuth = orientation[0];
                Log.d(this.getClass().getName(), "\tazimuth : " + azimuth);
                Log.d(this.getClass().getName(), "\tpitch : " + orientation[1]);
                Log.d(this.getClass().getName(), "\troll : " + orientation[2]);
                this.updateView(azimuth);
            }
        }

    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
        Log.d(this.getClass().getName(), "onAccuracyChanged");
    }

    private void updateView(final float azimuth) {
        final RectCoordinates userLocation = new RectCoordinates(0,0,0);//this.userTracker.getLatestLocation();
        final RectCoordinates endpoint = this.angleMapper.getEndPoint(userLocation, azimuth);
        Log.d(this.getClass().getName(), "Found endpoint : " + endpoint.toString());
        final RectCoordinates nextLocation = new RectCoordinates(1,0,0);//this.userTracker.getNextNode().getLocation();
        final double angleToWaypoint = this.getAngleToWaypoint(userLocation, endpoint, nextLocation);
        this.view.drawIndicatorAtAngle(angleToWaypoint);
    }

    private double getAngleToWaypoint(final RectCoordinates userLocation,
                                      final RectCoordinates endpoint,
                                      final RectCoordinates nextLocation) {
        final int aX = endpoint.getX() - userLocation.getX();
        final int aY = endpoint.getY() - userLocation.getY();
        final int bX = nextLocation.getX() - userLocation.getX();
        final int bY = nextLocation.getY() - userLocation.getY();
        final double dot = dotProduct(aX, aY, bX, bY);
        Log.d(this.getClass().getName(), "dot : " + dot);
        final double aMag = magnitude(aX, aY);
        final double bMag = magnitude(bX, bY);
        final int sign = getSign(aX, aY, bX, bY);
        return sign * Math.toDegrees(Math.acos(dot / (aMag * bMag)));
    }

    private double magnitude(final int x, final int y) {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    private int dotProduct(final int aX, final int aY, final int bX, final int bY) {
        return (aX * bX) + (aY * bY);
    }

    private int getSign(final int aX, final int aY, final int bX, final int bY) {
        return (-1 * aX * bY) + (aY * bX) >= 0 ? -1 : 1;
    }
}
