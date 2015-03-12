package walker.blue.core.lib.ddb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;

import java.util.HashMap;
import java.util.Map;

import walker.blue.core.lib.types.Building;
import walker.blue.core.lib.utils.Json2Building;

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

    /**
     * Fetch data for the building corresponding to the given Building ID
     *
     * @param buildingID String
     * @return GetItemResult TODO Parse this into our Building class
     */
    public GetItemResult getBuildingData(final String buildingID) {
        final Map<String, AttributeValue> attributes = new AttributeMapBuilder()
                .addAttribute(DdbCommon.BUILDING_ID, buildingID)
                .build();
        final GetItemResult result = this.client.getItem(DdbCommon.TABLE_NAME, attributes);
        //TODO handle exceptions
        return result;
    }

    public void putBuilding(final Building building) {
        // Will validate that the string can be parsed prior to inserting and
        // if of a correct format, will use the uuid in the file for inserting
        // into the database.
        if(building != null) {
            String json = new Json2Building().toString(building);

            DynamoDB dynamo = new DynamoDB(client);

            Table table = dynamo.getTable(DdbCommon.TABLE_NAME);

            Item item = new Item()
                    .withPrimaryKey(DdbCommon.BUILDING_ID, building.getUUID())
                    .withJSON(DdbCommon.JSON_DOCUMENT, json);
            table.putItem(item);
        }
    }

    public Building getBuilding(String uuid) {
        DynamoDB dynamo = new DynamoDB(client);

        Table table = dynamo.getTable(DdbCommon.TABLE_NAME);

        Item documentItem =
                table.getItem(new GetItemSpec()
                        .withPrimaryKey(DdbCommon.BUILDING_ID, uuid)
                        .withAttributesToGet(DdbCommon.JSON_DOCUMENT));

        return new Json2Building().toBuilding(documentItem.getJSONPretty(DdbCommon.JSON_DOCUMENT));
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
