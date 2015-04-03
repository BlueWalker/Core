package walker.blue.core.lib.direction;

import android.util.Log;

import walker.blue.core.lib.types.Building;
import walker.blue.path.lib.RectCoordinates;

public class UserAngleMapper {

    private static final int Z_VALUE = 0;

    private int xOffset;
    private int yOffset;

    public UserAngleMapper(final Building building) {
        this(building.getNorthPoint());
    }

    public UserAngleMapper(final RectCoordinates northPoint) {
        this.xOffset = northPoint.getX();
        this.yOffset = northPoint.getY();
    }

    public RectCoordinates getEndPoint(final RectCoordinates userLocation, final double azimuth) {
        Log.d(this.getClass().getName(), "azimuth[: " + azimuth);
        final int northX = userLocation.getX() + xOffset;
        final int northY = userLocation.getY() + yOffset;
        Log.d(this.getClass().getName(), "\tnorthX: " + northX);
        Log.d(this.getClass().getName(), "\tnorthY: " + northY);
        final float endpointXDouble = Math.round((Math.cos(azimuth) * northX) + (Math.sin(azimuth) * northY));
        final float endpointYDouble = Math.round((Math.sin(azimuth) * northX * -1) + (Math.cos(azimuth) * northY));
        Log.d(this.getClass().getName(), "\tMath.cos(azimuth): " + Math.cos(azimuth));
        Log.d(this.getClass().getName(), "\tMath.sin(azimuth): " + Math.sin(azimuth));
        Log.d(this.getClass().getName(), "\tendpointXDouble: " + endpointXDouble);
        Log.d(this.getClass().getName(), "\tendpointYDouble: " + endpointYDouble);
        return new RectCoordinates(Math.round(endpointXDouble), Math.round(endpointYDouble), Z_VALUE);
    }// uth : -2.3
}