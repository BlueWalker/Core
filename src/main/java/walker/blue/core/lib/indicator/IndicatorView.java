package walker.blue.core.lib.indicator;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
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
    private static final int ANGLE_RANGE = 55;

    /**
     * Current height of the view
     */
    private int height;
    /**
     * Current width of the view
     */
    private int width;
    /**
     * Paint used to draw the line
     */
    private Paint linePaint;
    /**
     * Paint used to draw the point
     */
    private Paint pointPaint;
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
     ** Constructor
     *
     * @param context
     * @param attributeSet
     */
    public IndicatorView(final Context context, final AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attributeSet
     * @param defStyle
     */
    public IndicatorView(final Context context, final AttributeSet attributeSet, final int defStyle) {
        super(context, attributeSet, defStyle);
        this.theme = IndicatorTheme.LIGHT;
        this.linePaint = PaintFactory.buildLinePaint(theme.getLineColor());
        this.pointPaint = PaintFactory.buildPointPaint(theme.getPointColor());
        this.getHolder().addCallback(this);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        this.height = MeasureSpec.getSize(heightMeasureSpec);
        this.width = MeasureSpec.getSize(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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


    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        Log.d(this.getClass().getName(), "surfaceCreated");
        this.drawIndicatorAtPosition(this.currentPosition);
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
        Log.d(this.getClass().getName(), "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        Log.d(this.getClass().getName(), "surfaceDestroyed");
    }

    /**
     * Draws the indicator for the given vector
     *
     * @param angle Angle at which the i
     */
    public void drawIndicatorAtAngle(final double angle) {
        final float pos;
        if (Math.abs(angle) >= ANGLE_RANGE) {
            pos = angle > 0 ? this.width : 0;
        } else {
            final double percentage = angle / ANGLE_RANGE;
            pos = (this.width / 2) + Math.round((this.width / 2) * percentage);
        }
        this.drawIndicatorAtPosition(pos);
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
        canvas.drawLine(0,
                this.height / 2,
                this.width,
                this.height / 2,
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
                this.height / 2,
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
}