package walker.blue.core.lib.ddb;

/**
 * This class holds DynamoDB information that is shared across
 * various other classes. Some information includes the table
 * and attribute names used in DynamoDB.
 */
public class DdbCommon {

    /**
     * Name for the Building ID attribute
     */
    public static final String BUILDING_ID = "BuildingID";

    /**
     * Name of the DynamoDB table
     */
    public static final String TABLE_NAME = "BlueWalker";

    /**
     * Name of the JSON documents
     */
    public static final String JSON_DOCUMENT = "json";
}
