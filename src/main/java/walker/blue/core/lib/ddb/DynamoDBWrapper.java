package walker.blue.core.lib.ddb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for the DynamoDB client
 */
public class DynamoDBWrapper {

    /**
     * Name for the Building ID attribute
     */
    private static final String BUILDING_ID = "BuildingID";
    /**
     * Name of the DynamoDB table
     */
    private static final String TABLE_NAME = "BlueWalker";

    /**
     * DynamoDB client being used
     */
    private AmazonDynamoDBClient client;

    /**
     * Constructor. Initlaizes the DynamoDB client
     */
    public DynamoDBWrapper() {
        //TODO: check for null credentials
        this.client = new AmazonDynamoDBClient(AWSCredentialsLoader.loadCredentials());
    }

    /**
     * Fetch data for the building corresponding to the given Building ID
     *
     * @param buildingID String
     * @return GetItemResult TODO Parse this into our Building class
     */
    public GetItemResult getBuildingData(final String buildingID) {
        final Map<String, AttributeValue> attributes = new AttributeMapBuilder()
                .addAttribute(BUILDING_ID, buildingID)
                .build();
        final GetItemResult result = this.client.getItem(TABLE_NAME, attributes);
        //TODO handle exceptions
        return result;
    }

    /**
     * Builder Class to help build the Attribute Map used to fetch data
     */
    private class AttributeMapBuilder {

        /**
         * Map where attributes are stored
         */
        private Map<String, AttributeValue> map;

        /**
         * Constructor. Initializes the map
         */
        public AttributeMapBuilder() {
            this.map = new HashMap<>();
        }

        /**
         * Add an attribute with the given name to the Map
         *
         * @param name String
         * @param attribute String
         * @return AttributeMapBuilder
         */
        public AttributeMapBuilder addAttribute(String name, String attribute) {
            this.map.put(name, new AttributeValue(attribute));
            return this;
        }

        /**
         * Finalizes the builing of the map and returns it
         *
         * @return Map of Strings mapped to AttributeValues
         */
        public Map<String, AttributeValue> build() {
            return this.map;
        }
    }
}
