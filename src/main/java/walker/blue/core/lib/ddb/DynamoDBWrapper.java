package walker.blue.core.lib.ddb;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
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

    public DynamoDBWrapper(final AWSCredentials credentials) {
        this.client = new AmazonDynamoDBClient(credentials);
    }

    /**
     * Fetch data for the building corresponding to the given Building ID
     *
     * @param buildingID String
     * @return GetItemResult TODO Parse this into our Building class
     */
    public GetItemResult getBuildingData(final String buildingID) {
        final Map<String, AttributeValue> attributes = new AttributeMapBuilder()
                .addAttribute(DDBConstants.BUILDING_ID, buildingID)
                .build();
        try {
            return this.client.getItem(DDBConstants.TABLE_NAME, attributes);
        } catch (final AmazonClientException e) {
            Log.d(this.getClass().getName(), "Failed getting data from DDB", e);
            return null;
        }
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
