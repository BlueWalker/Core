package walker.blue.core.lib.ddb;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.beacon.lib.beacon.BeaconBuilder;
import walker.blue.core.lib.types.Building;
import walker.blue.core.lib.types.DestinationTable;
import walker.blue.core.lib.types.DestinationType;
import walker.blue.path.lib.FloorConnector;
import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.RectCoordinates;

/**
 * Class responsible for converting DynamoDB objects to java objects
 */
public class AttrToJava {

    private static final int NORTH_POINT_Z = 0;

    /**
     * Converts the given GetItemResult into a building
     *
     * @param queryResult GetItemResult of a dynamo db query
     * @return Building object. Null if the GetItemResult is not a valid
     *         building
     */
    public static Building getItemResultToBuilding(final GetItemResult queryResult) {
        return attrToBuilding(queryResult.getItem());
    }

    /**
     * Converts the given map of attribute values into a building object
     * @param rawData Map of strings to attribute values which comes from a
     *                dynamo db query
     * @return Building object. Null if the given map is not a valid
     *         building
     */
    public static Building attrToBuilding(final Map<String, AttributeValue> rawData) {
        if (!rawData.containsKey(DDBConstants.BUILDING_DATA)) {
            return null;
        }
        final Map<String, AttributeValue> rawBuildingData = rawData.get(DDBConstants.BUILDING_DATA).getM();
        final int floorHeight = parseBuildingFloorHeight(rawBuildingData);
        final String buildingUUID = parseBuildingUUID(rawData);
        final int nodeDistance = parseBuildingNodeDistance(rawBuildingData);
        final Map<Beacon, RectCoordinates> beacons = parseBuildingBeacons(rawBuildingData);
        final List<List<List<GridNode>>> nodes = parseNodes(rawBuildingData);
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        final DestinationTable destinations = parseBuildingDestinations(rawBuildingData, nodes);
        final List<FloorConnector> connectors = parseBuildingFloorConnectors(rawBuildingData, nodes);
        final RectCoordinates northPoint = parseNorthPoint(rawBuildingData);
        if (floorHeight != DDBConstants.DEFAULT_FLOOR_HEIGHT && buildingUUID != null &&
                nodeDistance != DDBConstants.DEFAULT_NODE_DISTANCE && beacons != null && !beacons.isEmpty() &&
                destinations != null && !destinations.isEmpty()) {
            return new Building(buildingUUID,
                    nodeDistance,
                    floorHeight,
                    nodes,
                    connectors,
                    beacons,
                    destinations,
                    northPoint);
        } else {
            return null;
        }
    }

    /**
     * Converts the given AttributeValue to a Beacon object
     *
     * @param rawBeacon AttributeValue representing a beacon
     * @return Returns a beacon object built from the given data
     */
    public static Beacon attrToBeacon(final AttributeValue rawBeacon) {
        final Map<String, AttributeValue> attributeValueMap = rawBeacon.getM();
        return new BeaconBuilder()
                .setBeaconAddress(attributeValueMap.get(DDBConstants.BEACON_ADDRESS).getS())
                .setBeaconMajor(Integer.valueOf(attributeValueMap.get(DDBConstants.BEACON_MAJOR).getN()))
                .setBeaconMinor(Integer.valueOf(attributeValueMap.get(DDBConstants.BEACON_MINOR).getN()))
                .setBeaconName(attributeValueMap.get(DDBConstants.BEACON_NAME).getS())
                .setBeaconRSSI(Integer.valueOf(attributeValueMap.get(DDBConstants.BEACON_RSSI).getN()))
                .build();
    }

    /**
     * Parses the given AttributeValue to get the locaton of the beacon
     *
     * @param rawBeacon RectCoordinates representing a beacon
     * @return Returns a RectCoordinates object witht eh location fo the beacon
     */
    public static RectCoordinates attrToBeaconLocation(final AttributeValue rawBeacon) {
        final Map<String, AttributeValue> attributeValueMap = rawBeacon.getM();
        return new RectCoordinates(Integer.valueOf(attributeValueMap.get(DDBConstants.X).getN()),
                Integer.valueOf(attributeValueMap.get(DDBConstants.Y).getN()),
                Integer.valueOf(attributeValueMap.get(DDBConstants.Z).getN()));
    }

    /**
     * Parses the given RectCoordinates into a GridNode using the given x, y,
     * and z values for the coordinates
     *
     * @param rawNode AttributeValue representing a gridnode
     * @param x x coordinate used in the node
     * @param y y coordinate used in the node
     * @param z z coordinate used in the node
     * @return GridNode built from the given data and coordinates
     */
    public static GridNode attrToGridNode(final AttributeValue rawNode, final int x, final int y, final int z) {
        switch (rawNode.getS()) {
            case DDBConstants.NODE_TRANSVERSABLE:
                return new GridNode(x, y, z, true);
            case DDBConstants.NODE_NON_TRANSVERSABLE:
                return new GridNode(x, y, z, false);
            case DDBConstants.NODE_STARIS:
                return new FloorConnector(x, y, z, false, FloorConnector.Type.STAIRS);
            case DDBConstants.NODE_ELEVATOR:
                return new FloorConnector(x, y, z, false, FloorConnector.Type.ELEVATOR);
            case DDBConstants.NODE_DEFAULT:
            default:
                return new FloorConnector(x, y, z, false, FloorConnector.Type.NONE);
        }
    }

    /**
     * Parses the given map of strings to Attributes values in order get the
     * floor height
     *
     * @param rawData Map of AttributeValues representing the Building data
     * @return the floor height for thr building
     */
    private static int parseBuildingFloorHeight(final Map<String, AttributeValue> rawData) {
        if (!rawData.containsKey(DDBConstants.FLOOR_HEIGHT)) {
            return DDBConstants.DEFAULT_FLOOR_HEIGHT;
        }
        return Integer.valueOf(rawData.get(DDBConstants.FLOOR_HEIGHT).getN());
    }

    /**
     * Parses the given map of strings to Attributes values in order get the
     * uuid
     *
     * @param rawData Map of AttributeValues representing the Building data
     * @return the uuid of the building
     */
    private static String parseBuildingUUID(final Map<String, AttributeValue> rawData) {
        if (!rawData.containsKey(DDBConstants.UUID)) {
            return null;
        }
        return rawData.get(DDBConstants.UUID).getS();
    }

    /**
     * Parses the given map of strings to Attributes values in order get the
     * node distance of the building
     *
     * @param rawData Map of AttributeValues representing the complete query result
     * @return node distance of the building
     */
    private static int parseBuildingNodeDistance(final Map<String, AttributeValue> rawData) {
        if (!rawData.containsKey(DDBConstants.NODE_DISTANCE)) {
            return DDBConstants.DEFAULT_NODE_DISTANCE;
        }
        return Integer.valueOf(rawData.get(DDBConstants.NODE_DISTANCE).getN());
    }

    /**
     * Parses the given map of strings to Attributes values in order get the
     * beacons and their location
     *
     * @param rawData Map of AttributeValues representing the Building data
     * @return map with the beaconsa and their locations
     */
    private static Map<Beacon, RectCoordinates> parseBuildingBeacons(final Map<String, AttributeValue> rawData) {
        if (!rawData.containsKey(DDBConstants.BEACONS)) {
            return null;
        }
        final Map<Beacon, RectCoordinates> beaconLocMap = new HashMap<>();
        for (final AttributeValue value : rawData.get(DDBConstants.BEACONS).getL()) {
            final Beacon beacon = attrToBeacon(value);
            final RectCoordinates beaconLoc = attrToBeaconLocation(value);
            beaconLocMap.put(beacon, beaconLoc);
        }
        return beaconLocMap;
    }

    /**
     * Parses the given map of strings to Attributes values in order get the
     * nodes of the building
     *
     * @param rawData Map of AttributeValues representing the Building data
     * @return List containing all the GridNodes for a building
     */
    private static List<List<List<GridNode>>> parseNodes(final Map<String, AttributeValue> rawData) {
        if (!rawData.containsKey(DDBConstants.NODES)) {
            return null;
        }
        final List<List<List<GridNode>>> nodes = new ArrayList<>();
        int zCount = 0;
        for (final AttributeValue valsZ : rawData.get(DDBConstants.NODES).getL()) {
            int yCount = 0;
            final List<List<GridNode>> listY = new ArrayList<>();
            for (final AttributeValue valsY : valsZ.getL()) {
                int xCount = 0;
                final List<GridNode> listX = new ArrayList<>();
                for (final AttributeValue rawNode : valsY.getL()) {
                    listX.add(attrToGridNode(rawNode, xCount, yCount, zCount));
                    xCount++;
                }
                listY.add(listX);
                yCount++;
            }
            nodes.add(listY);
            zCount++;
        }
        return nodes;
    }

    /**
     * Parses the given map of strings to Attributes values in order get the
     * destinations of the building and their tags
     *
     * @param rawData Map of AttributeValues representing the Building data
     * @param nodes List of nodes of the building
     * @return Map containing the tags as keys and their corresponding nodes
     * as values
     */
    private static DestinationTable parseBuildingDestinations(final Map<String, AttributeValue> rawData,
                                                                         final List<List<List<GridNode>>> nodes) {
        if (!rawData.containsKey(DDBConstants.DESTINATIONS) || nodes.isEmpty()) {
            return null;
        }
        final DestinationTable destinations = new DestinationTable();
        for(final AttributeValue destination : rawData.get(DDBConstants.DESTINATIONS).getL()) {
            final Map<String, AttributeValue> destMap = destination.getM();
            System.out.println();
            final GridNode currentNode = nodes.get(Integer.valueOf(destMap.get(DDBConstants.Z).getN()))
                    .get(Integer.valueOf(destMap.get(DDBConstants.Y).getN()))
                    .get(Integer.valueOf(destMap.get(DDBConstants.X).getN()));
            final String typeString = destMap.get(DDBConstants.DESTINATION_TYPE).getS();
            final DestinationType type = DestinationType.valueOf(typeString);
            final String key;
            if (type.isGeneric()) {
                key = null;
            } else {
                key = destMap.get(DDBConstants.DESTINATION_KEY).getS();
            }
            destinations.addValue(type, key, currentNode);
        }
        return destinations;
    }

    /**
     * Parses the given map of strings to Attributes values in order get the
     * floor connectores of the building
     *
     * @param rawData Map of AttributeValues representing the Building data
     * @param nodes List of nodes of the building
     * @return List containing containing the floor connectors for the building
     */
    private static List<FloorConnector> parseBuildingFloorConnectors(final Map<String, AttributeValue> rawData,
                                                                     final List<List<List<GridNode>>> nodes) {
        if (!rawData.containsKey(DDBConstants.FLOOR_CONNECTORS) || nodes.isEmpty()) {
            return new ArrayList<>();
        }
        final List<FloorConnector> connectors = new ArrayList<>();
        int count = 0;
        for(final AttributeValue connector : rawData.get(DDBConstants.FLOOR_CONNECTORS).getL()) {
            final Map<String, AttributeValue> connMap = connector.getM();
            final GridNode currentNode = nodes.get(Integer.valueOf(connMap.get(DDBConstants.Z).getN()))
                    .get(Integer.valueOf(connMap.get(DDBConstants.Y).getN()))
                    .get(Integer.valueOf(connMap.get(DDBConstants.X).getN()));
            if (!(currentNode instanceof FloorConnector)) {
                continue;
            }
            final FloorConnector currentConnector = (FloorConnector) currentNode;
            currentConnector.setIndex(count);
            count++;
            boolean flag = false;
            for (final AttributeValue connection : connMap.get(DDBConstants.FLOOR_CONNECTORS_CONNECTIONS).getL()) {
                final Map<String, AttributeValue> endConnMap = connection.getM();
                final GridNode connectionNode = nodes.get(Integer.valueOf(endConnMap.get(DDBConstants.Z).getN()))
                        .get(Integer.valueOf(endConnMap.get(DDBConstants.Y).getN()))
                        .get(Integer.valueOf(endConnMap.get(DDBConstants.X).getN()));
                if (!(connectionNode instanceof FloorConnector)) {
                    continue;
                } else {
                    flag = true;
                    currentConnector.addConnection((FloorConnector) connectionNode);
                }
            }
            if (flag) {
                connectors.add(currentConnector);
            }
        }
        return connectors;
    }

    /**
     *
     * @param rawData
     * @return
     */
    private static RectCoordinates parseNorthPoint(final Map<String, AttributeValue> rawData) {
        if (!rawData.containsKey(DDBConstants.NORTH_POINT)) {
            return null;
        }
        final AttributeValue rawValue = rawData.get(DDBConstants.NORTH_POINT);
        final Map<String, AttributeValue> attrNorthPoint = rawValue.getM();
        try {
            final String xString = attrNorthPoint.get(DDBConstants.X).getN();
            final String yString = attrNorthPoint.get(DDBConstants.Y).getN();
            final int x = Integer.valueOf(xString);
            final int y = Integer.valueOf(yString);
            return new RectCoordinates(x, y, NORTH_POINT_Z);
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}