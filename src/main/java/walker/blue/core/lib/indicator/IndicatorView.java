package walker.blue.core.lib.indicator;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Indicator to show location of next waypoint
 */
public class IndicatorView extends SurfaceView implements SurfaceHolder.Callback {

    /**
     * Default radius for the point
     */
    private static final float DEFAULT_POINT_RADIUS = 40f;
    /**
     * Default padding for the line
     */
    private static final int DEFAULT_PADDING = 0;
    /**
     * Starting position of the point
     */
    private static final float STARTING_POSITION = 0;

    /**
     * Paint used to draw the line
     */
    private Paint linePaint;
    /**
     * Paint used to draw the point
     */
    private Paint pointPaint;
    /**
     * Current window dimensions
     */
    private Point windowDimensions;
    /**
     * Padding for the indicator
     */
    private int padding;
    /**
     * Current position of the point
     */
    private float currentPosition;
    /**
     * Current theme of the indicator
     */
    private IndicatorTheme theme;

    /**
     * Contructor
     *
     * @param context
     */
    public IndicatorView(final Context context) {
        this(context, null);
    }

    /**
     *
     * @param context
     * @param attributeSet
     */
    public IndicatorView(final Context context, final AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    /**
     *
     * @param context
     * @param attributeSet
     * @param defStyle
     */
    public IndicatorView(final Context context, final AttributeSet attributeSet, final int defStyle) {
        super(context, attributeSet, defStyle);
        this.padding = DEFAULT_PADDING;
        this.windowDimensions = new Point();
        theme = IndicatorTheme.LIGHT;
        this.linePaint = PaintFactory.buildLinePaint(theme.getLineColor());
        this.pointPaint = PaintFactory.buildPointPaint(theme.getPointColor());
        this.getHolder().addCallback(this);
    }

    /**
     * Set the padding for the line
     *
     * @param padding new value for the padding
     */
    public void setLinePadding(final int padding) {
        this.padding = padding;
    }

    /**
     * Set the theme of the indicator
     *
     * @param theme new theme for the indicator
     */
    public void setIndicatorTheme(final IndicatorTheme theme) {
        this.theme = theme;
        this.linePaint.setColor(theme.getLineColor());
        this.pointPaint.setColor(theme.getPointColor());
    }

    /**
     * Get the current position of the point
     *
     * @return current position of the point
     */
    public float getCurrentPosition() {
        return this.currentPosition;
    }

    /**
     * Get the max width allowed. Padding is taken into consideration
     *
     * @return maximum allowed width
     */
    public int getAdjustedMaxWidth() {
        return this.windowDimensions.x - this.padding;
    }

    /**
     * Get current padding of the indicator
     *
     * @return current value of padding being used
     */
    public int getPadding() {
        return this.padding;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(this.getClass().getName(), "surfaceCreated");
        drawIndicatorAtPosition(this.currentPosition);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(this.getClass().getName(), "surfaceChanged");
        this.updateWindowDimensions();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(this.getClass().getName(), "surfaceDestroyed");
    }

    /**
     * Draw indicator with the point at the given position
     *
     * @param pos new position for the point
     */
    public void drawIndicatorAtPosition(final float pos) {
        if (this.getHolder().getSurface().isValid()) {
            final Canvas canvas = this.getHolder().lockCanvas();
            final int contextColor = this.getContextColor();
            canvas.drawColor(contextColor != -1 ? contextColor : Color.BLACK);
            this.drawLine(canvas);
            this.drawPoint(canvas, pos);
            this.getHolder().unlockCanvasAndPost(canvas);
        } else {
            Log.d(this.getClass().getName(), "Canvas is null");
        }
    }

    /**
     * Draws a line into the given canvas
     *
     * @param canvas Canvas in which the line will be drawn into
     */
    private void drawLine(final Canvas canvas) {
        canvas.drawLine(padding,
                windowDimensions.y / 2,
                windowDimensions.x - padding,
                windowDimensions.y / 2,
                linePaint);
    }

    /**
     * Draws the point into the given canvas at the given position
     *
     * @param canvas Canvas in which the line will be drawn into
     * @param pos Position where the point will be drawn
     */
    private void drawPoint(final Canvas canvas, final float pos) {
        canvas.drawCircle(pos,
                windowDimensions.y / 2,
                DEFAULT_POINT_RADIUS,
                pointPaint);
        this.currentPosition = pos;
    }

    /**
     * Grabs the background color from the views context
     *
     * @return contexts background color
     */
    private int getContextColor() {
        final Context context = this.getContext();
        if (context instanceof Activity) {
            final Activity parentActivity = (Activity) context;
            final TypedValue result = new TypedValue();
            if (parentActivity.getTheme().resolveAttribute(android.R.attr.windowBackground, result, true) &&
                    result.type >= TypedValue.TYPE_FIRST_COLOR_INT) {
                return result.data;
            } else {
                Log.d(this.getClass().getName(), "No Color Found in the parent activity");
            }
        } else {
            Log.d(this.getClass().getName(), "This should get implemented at some point but who cares");
        }
        return -1;
    }

    /**
     * Update windows dimensions from the views context
     */
    private void updateWindowDimensions() {
        final Context context = this.getContext();
        if (context instanceof Activity) {
            final Activity parentActivity = (Activity) context;
            final Display display = parentActivity.getWindowManager().getDefaultDisplay();
            display.getSize(windowDimensions);
        } else {
            Log.d(this.getClass().getName(), "This should get implemented at some point but who cares");
        }
    }
}