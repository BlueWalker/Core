package walker.blue.core.lib.user;

/**
 * Represents the current state of the user
 */
public enum UserState {

    /**
     * The user is currently on course
     */
    ON_COURSE,
    /**
     * The user has entered warning zone
     */
    IN_WARNING_ZONE,
    /**
     * User is now off course
     */
    OFF_COURSE,
    /**
     * User is reaching the next floor
     */
    REACHING_NEXT_FLOOR,
    /**
     * User has arrived at the destination
     */
    ARRIVED,
    /**
     * State is uninitialized
     */
    UNINITIALIZED
}
