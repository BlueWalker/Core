package walker.blue.core.lib.speech;

/**
 * Class which holds all the values calculated when generating speech that
 * will be spoken to the user
 */
public class GeneratedSpeech {

    /**
     * Distance until the next event
     */
    private double distance;
    /**
     * Type of the next event
     */
    private NodeEvent event;
    /**
     * Direction of the next event
     */
    private NodeDirection direction;

    /**
     * Contructor. Sets the fields to the given values
     *
     * @param distance  Distance until the next event
     * @param event Type of the next event
     * @param direction Direction of the next event
     */
    public GeneratedSpeech(final double distance,
                           final NodeEvent event,
                           final NodeDirection direction) {
        this.distance = distance;
        this.event = event;
        this.direction = direction;
    }

    /**
     * Getter for the distance field
     *
     * @return Distance until the next event
     */
    public double getDistance() {
        return this.distance;
    }

    /**
     * Getter for the event field
     *
     * @return Type of the next event
     */
    public NodeEvent getEvent() {
        return this.event;
    }

    /**
     * Getter for the direction field
     *
     * @return Direction of the next event
     */
    public NodeDirection getDirection() {
        return this.direction;
    }

    @Override
    public String toString() {
        switch (this.event) {
            case TURN:
            case REACHING_DESTINATION:
                return  String.format(this.event.getFormat(),
                        this.direction.name().toLowerCase(),
                        this.distance);
            case REACHING_DESTINATION_AHEAD:
                return  String.format(this.event.getFormat(),
                        this.distance);
            default:
                return super.toString();
        }
    }
}