package walker.blue.core.lib.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import walker.blue.core.lib.types.Building;
import walker.blue.path.lib.FloorConnector;
import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.RectCoordinates;
import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.beacon.lib.beacon.BeaconBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Json2Building {
    private static String UUID = "UUID";
    private static String NODE_DISTANCE = "NodeDistance";
    private static String FLOOR_HEIGHT = "FloorHeight";
    private static String NODE_COLLECTION = "NodeCollection";
    private static String STAIRS_ELEVATORS = "Stairs/Elevators";
    private static String BEACONS = "Beacons";
    private static String DESTINATIONS = "Destinations";
    private static String X = "x";
    private static String Y = "y";
    private static String Z = "z";
    private static String CONNECTIONS = "Connections";
    
    public Json2Building() {
        
    }

    public String fileToString(String filePath) {
        File file = new File(filePath);

        try {
            FileReader reader = new FileReader(file);

            char[] buf = new char[(int)file.length()];
            // Read the file into the buffer
            reader.read(buf, 0, (int)file.length());
            reader.close();

            return new String(buf, 0, buf.length);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("IO Exception occurred.");
        }

        return null;
    }

    public String toString(Building building) {
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

        // BUILDING node
        ObjectNode buildingNode = nodeFactory.objectNode();

        // UUID
        buildingNode.put(UUID, building.getUUID());

        // NODE_DISTANCE
        buildingNode.put(NODE_DISTANCE, building.getNodeDistance());

        // FLOOR_HEIGHT
        buildingNode.put(FLOOR_HEIGHT, building.getFloorHeight());

        // NODE_COLLECTION
        ArrayNode nodeCollectionNode = nodeFactory.arrayNode();
        List<List<List<GridNode>>> searchSpace = building.getSearchSpace();
        for(List<List<GridNode>> floor : searchSpace) {
            ArrayNode floorNode = nodeFactory.arrayNode();
            for(List<GridNode> row : floor) {
                ArrayNode rowNode = nodeFactory.arrayNode();
                for(GridNode node : row) {
                    if(node instanceof FloorConnector) {
                        FloorConnector connector = (FloorConnector)node;
                        // The node is a FloorConnector, check the type.
                        FloorConnector.Type type = connector.getType();
                        switch(type) {
                            case STAIRS:
                                rowNode.add("S");
                                break;
                            case ELEVATOR:
                                rowNode.add("E");
                                break;
                            default:
                                rowNode.add("-");
                                break;
                        }
                    }
                    else {
                        if(node.walkable()) {
                            rowNode.add("O");
                        }
                        else {
                            rowNode.add("X");
                        }
                    }
                }
                floorNode.add(rowNode);
            }
            nodeCollectionNode.add(floorNode);
        }
        buildingNode.set(NODE_COLLECTION, nodeCollectionNode);

        // STAIRS_ELEVATORS
        ArrayNode stairsElevators = nodeFactory.arrayNode();
        List<FloorConnector> floorConnectors = building.getFloorConnectors();
        // Iterate through each floor connector adding it to the array node
        for(FloorConnector connector : floorConnectors) {
            ObjectNode connectorNode =
                    getObjectNode(nodeFactory.objectNode(), connector.location());

            ArrayNode connectionsNode = nodeFactory.arrayNode();
            // TODO: Will calling getConnections repeatedly get called every loop?
            for(FloorConnector connection : connector.getConnections()) {
                connectionsNode.add(getObjectNode(nodeFactory.objectNode(), connection.location()));
            }
            connectorNode.set(CONNECTIONS, connectionsNode);
            stairsElevators.add(connectorNode);
        }
        buildingNode.set(STAIRS_ELEVATORS, stairsElevators);

        // BEACONS
        ArrayNode beaconsNode = nodeFactory.arrayNode();
        Map<Beacon, RectCoordinates> beaconLocationMap = building.getBeaconLocationMap();
        Set<Beacon> beacons = beaconLocationMap.keySet();
        for(Beacon beacon : beacons) {
            RectCoordinates beaconLocation = beaconLocationMap.get(beacon);

            beaconsNode.add(
                    nodeFactory.objectNode().put("Name", beacon.getName())
                            .put("Address", beacon.getAddress())
                            .put("Major", beacon.getMajor())
                            .put("Minor", beacon.getMinor())
                            .put("RSSI", beacon.getRSSI())
                            .put(X, beaconLocation.x())
                            .put(Y, beaconLocation.y())
                            .put(Z, beaconLocation.z())
            );
        }
        buildingNode.set(BEACONS, beaconsNode);

        // DESTINATIONS
        Map<String, List<GridNode>> destinationMap = building.getDestinationMap();
        // Must convert Map<String, List<GridNode>> to Map<GridNode, List<String>>
        // to allow for easy writing.
        Map<GridNode, List<String>> nodeToTagsMap = new HashMap<>();
        Set<String> tagSet = destinationMap.keySet();
        for(String tag : tagSet) {
            // For every tag, put the grid node into the nodeToTagsMap with the
            // tag as the value and the node as the key. If the grid node is already
            // in the map, then just add the string to the list for that grid node key.
            List<GridNode> nodes = destinationMap.get(tag);
            for(GridNode node : nodes) {
                List<String> nodeTags = nodeToTagsMap.get(node);
                if(nodeTags == null) {
                    List<String> tagsList = new ArrayList<>();
                    tagsList.add(tag);
                    nodeToTagsMap.put(node, tagsList);
                }
                else {
                    nodeTags.add(tag);
                }
            }
        }

        ArrayNode destinationsNode = nodeFactory.arrayNode();
        Set<GridNode> nodeSet = nodeToTagsMap.keySet();
        for(GridNode node : nodeSet) {
            ObjectNode destinationNode = getObjectNode(nodeFactory.objectNode(), node.location());
            List<String> tagsList = nodeToTagsMap.get(node);
            ArrayNode tagsNode = nodeFactory.arrayNode();
            for(String tag : tagsList) {
                tagsNode.add(tag);
            }
            destinationNode.set("Tags", tagsNode);

            destinationsNode.add(destinationNode);
        }
        buildingNode.set(DESTINATIONS, destinationsNode);

        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(buildingNode);
            return json;
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public Building toBuilding(File file) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));

            StringBuilder stringBuilder = new StringBuilder((int)file.length());

            String currLine;
            // Read the file line by line into the string builder
            while((currLine = bufferedReader.readLine()) != null) {
                stringBuilder.append(currLine);
            }
            bufferedReader.close();
            // Call toBuilding to parse the string into a Building object
            return toBuilding(stringBuilder.toString());

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("IO Exception occurred.");
        }
        return null;
    }

    public Building toBuilding(String json) {
        // TODO: Handle exceptions cleanly without just returning null
        if(json == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(json);

            // UUID
            String uuid = node.get(UUID).asText();//parseStringNode(node, "UUID");
            if(uuid == "") {
                System.out.println("unable to parse " + UUID + " from json");
            }
            
            // Node Distance
            Double nodeDistance = node.get(NODE_DISTANCE).asDouble();
            if(nodeDistance == 0.0) {
                System.out.println("unable to parse " + NODE_DISTANCE + " from json");
                return null;
            }            
                     
            // Pass connectors in to be initialized with FloorConnector instances without the actual connections
            List<List<List<GridNode>>> searchSpace = parseSearchSpace(node, NODE_COLLECTION);
            if(searchSpace == null) {
                System.out.println("unable to parse " + NODE_COLLECTION + " from json");
                return null;
            }
            
            // Get a list of FloorConnectors that hold the different floor connections within the building.
            List<FloorConnector> floorConnections = parseFloorConnections(node, searchSpace, STAIRS_ELEVATORS);
            if(floorConnections == null) {
                System.out.println("unable to parse " + STAIRS_ELEVATORS + " from json");
                return null;
            }

            // Beacons
            Map<Beacon, RectCoordinates> beaconLocationMap = parseBeaconLocationMap(node, BEACONS, uuid);
            if(beaconLocationMap == null) {
                System.out.println("unable to parse " + BEACONS + " from json");
                return null;
            }

            // Destinations
            Map<String, List<GridNode>> destinationsMap = parseDestinations(node, searchSpace, DESTINATIONS);
            if(destinationsMap == null) {
                System.out.println("unable to parse " + DESTINATIONS + " map from json");
                return null;
            }

            // Floor Height
            Double floorHeight = node.get(FLOOR_HEIGHT).asDouble();
            // If the search space is only one floor, a floor height does not need to be in the json file.
            if(searchSpace.size() > 1 && floorHeight == 0.0) {
                System.out.println("unable to parse " + FLOOR_HEIGHT + " from json");
                return null;
            }

            return new Building(uuid,
                    nodeDistance,
                    floorHeight,
                    searchSpace,
                    floorConnections,
                    beaconLocationMap,
                    destinationsMap);

        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }
      
    private List<List<List<GridNode>>> parseSearchSpace(JsonNode parent, String fieldName) {        
        JsonNode nodeCollectionNode = parent.get(fieldName);
        if(nodeCollectionNode == null || !nodeCollectionNode.isArray()) {
            System.out.println("unable to parse node collection from json");
            return null;
        }
        
        List<List<List<GridNode>>> searchSpace = new ArrayList<>(nodeCollectionNode.size()); 
        
        for(int i = 0; i < nodeCollectionNode.size(); i++) {    
            JsonNode floorNode = nodeCollectionNode.get(i);
            
            if(floorNode.isArray()) {
                List<List<GridNode>> floor = new ArrayList<>(floorNode.size());
                
                for(int j = 0; j < floorNode.size(); j++) {
                    JsonNode rowNode = floorNode.get(j);
                    
                    if(rowNode.isArray()) {
                        List<GridNode> row = new ArrayList<>(rowNode.size());
                        
                        for(int k = 0; k < rowNode.size(); k++) {
                            JsonNode gridNodeNode = rowNode.get(k);
                            
                            if(gridNodeNode.isTextual()) {
                                switch(gridNodeNode.asText()) {
                                case "O": {
                                    row.add(new GridNode(k, j, i, true));
                                    break;
                                }
                                case "X": {
                                    row.add(new GridNode(k, j, i, false));
                                    break;
                                }
                                case "S": {
                                    FloorConnector connector = new FloorConnector(k, j, i, false);
                                    connector.setType(FloorConnector.Type.STAIRS);
                                    row.add(connector);
                                    break;
                                }
                                case "E": {
                                    FloorConnector connector = new FloorConnector(k, j, i, false);
                                    connector.setType(FloorConnector.Type.ELEVATOR);
                                    row.add(connector);
                                    break;
                                }
                                default:
                                    System.out.println(
                                            "unknown grid node type when parsing search space at (" + k + ", " + j + ", " + i + ")");
                                    return null;
                                }  
                            }
                            else {
                                System.out.println("unable to parse building layout from json, a gridnode node is not textual");
                                return null;
                            }
                        }
                        floor.add(row);
                    }
                    else {
                        System.out.println("unable to parse building layout from json, rowNode is not an array");
                        return null;
                    }
                }
                searchSpace.add(floor);
            }
            else {
                System.out.println("unable to parse building layout from json, floorNode is not an array");
                return null;
            }
        }
        return searchSpace;
    }
    
    /**
     * Parses the parent node holding the information needed to create a 2d list
     * and uses the search space to initialize the floor connections for each
     * floor connector in the search space. 
     * @param parent        the JsonNode that holds the floor connections 
     *                      that needs to be parsed
     * @param searchSpace   the 3d list that contains floor connectors that need their
     *                      connections initialized based off the info in parent
     * @return              a 2d list, if successful, where rows represent
     *                      the particular floor of the building the floor connector is on
     *                      and columns hold the floor connectors. If the parent
     *                      node has an incorrect format, null is returned.
     */
    private List<FloorConnector> parseFloorConnections(JsonNode parent, List<List<List<GridNode>>> searchSpace, String fieldName) {
        // stairs/elevators from node collection
        JsonNode stairsElevatorsNode = parent.get(fieldName);
        if(stairsElevatorsNode == null || !stairsElevatorsNode.isArray()) {
            System.out.println("unable to parse stairs/elevators from json. Not an array?");
            return null;
        }
        
        List<FloorConnector> connections = new ArrayList<>();
        // Need to loop through every floor connection and create a floor connector
        // and place it in a list indexed based on the order within the json file
        for(int i = 0; i < stairsElevatorsNode.size(); i++) {
            RectCoordinates loc = parseRectCoordinates(stairsElevatorsNode.get(i));
            if(loc == null ||
                    loc.z() >= searchSpace.size() ||
                    loc.y() >= searchSpace.get(loc.z()).size() ||
                    loc.x() >= searchSpace.get(loc.z()).get(loc.y()).size()) {
                System.out.println("unable to parse coordinates from json at index " + i + " of " + fieldName + ".");
                return null;
            }
            
            GridNode gridNode = searchSpace.get(loc.z()).get(loc.y()).get(loc.x());
            // Check to make sure the grid node is also a floor connector. If it isn't, then
            // there is an error in the format of the json file.
            if(gridNode instanceof FloorConnector) {
                FloorConnector floorConnector = (FloorConnector)gridNode;

                // Add index of floorConnector inside connections ArrayList
                floorConnector.setIndex(i);
                
                // Get the connections node that holds the locations/indices of the search space
                // for all of the connections for the current connector 
                JsonNode connectionsNode = stairsElevatorsNode.get(i).get(CONNECTIONS);
                if(connectionsNode == null || !connectionsNode.isArray()) {
                    System.out.println(
                            "unable to parse connections from json at stairs/elevators index " + i + ". Not an array?");
                    return null;
                }
                
                // Add all of the connections to the FloorConnector as long as there are no errors.
                for(int j = 0; j < connectionsNode.size(); j++) {
                    JsonNode conNode = connectionsNode.get(j);
                    
                    RectCoordinates conLoc = parseRectCoordinates(conNode);
                    if(conLoc == null ||
                            conLoc.z() >= searchSpace.size() ||
                            conLoc.y() >= searchSpace.get(conLoc.z()).size() ||
                            conLoc.x() >= searchSpace.get(conLoc.z()).get(conLoc.y()).size()) {
                        System.out.println("unable to parse coordinates from json at index " +
                            i + ", connector " + j + " of " + fieldName + ".");
                        return null;
                    }
                    
                    GridNode gridNodeConnection = searchSpace.get(conLoc.z()).get(conLoc.y()).get(conLoc.x());
                    // Check that the connection is a floor connector. If it isn't, then json was not written correctly.
                    if(gridNodeConnection instanceof FloorConnector) {
                        if(!floorConnector.addConnection((FloorConnector)gridNodeConnection)) {
                            System.out.println(
                                    "unable to add connection (" + conLoc.x() + ", " + conLoc.y() + ", " + conLoc.z() +
                                    ") to connector ("+ loc.x() + ", " + loc.y() + ", " + loc.z() + ")");
                        }
                    }
                    else {
                        System.out.println(
                                "unable to parse connection (" + conLoc.x() + ", " + conLoc.y() + ", " + conLoc.z() +
                                ") to connector ("+ loc.x() + ", " + loc.y() + ", " + loc.z() + "). The search space node is not a FloorConnector.");
                    }
                }
                
                // Add the floor connections list
                connections.add(floorConnector);
            }
            else {
                System.out.println(
                        "unable to parse because search space node (" +
                                loc.x() + ", " + loc.y() + ", " + loc.z() + ") is not a FloorConnector");
                return null;
            }
        }
        
        // Run algorithm to fill in the remaining connections within each floor connector
        // in the list based off of the connections parsed from the json file.
        // TODO: Write method to do this
        connections = calcRemainingConnections(connections, searchSpace.size());
        
        return connections;
    }
    
    private Map<String, List<GridNode >> parseDestinations(JsonNode parent, List<List<List<GridNode>>> searchSpace, String fieldName) {
        JsonNode destinationsNode = parent.get(fieldName);
        if(destinationsNode == null || !destinationsNode.isArray()) {
            System.out.println("unable to parse destinations from json");
            return null;
        }
        
        Map<String, List<GridNode>> destinationsMap = new HashMap<>();
        
        for(int i = 0; i < destinationsNode.size(); i++) {
            RectCoordinates location = parseRectCoordinates(destinationsNode.get(i));
            if(location == null ||
                    location.z() >= searchSpace.size() ||
                    location.y() >= searchSpace.get(location.z()).size() ||
                    location.x() >= searchSpace.get(location.z()).get(location.y()).size()) {
                System.out.println("unable to parse coordinates from json at index " + i + " of " + fieldName + ".");
                return null;
            }
            
            JsonNode tagsNode = destinationsNode.get(i).get("Tags");
            if(tagsNode == null || !tagsNode.isArray()) {
                System.out.println("unable to parse Tags in " + fieldName + " at index " + i + " from json");
                return null;
            }
            // Loop through each tag for the particular node
            for(int j = 0; j < tagsNode.size(); j++) {
                String tag = tagsNode.get(j).asText();
                if(tag == "") {
                    System.out.println("unable to parse tag at " + fieldName + ": " + i + "tag#: " + j + " from json");
                    return null;
                }
                
                // Get the list holding the grid nodes that have the same
                // string as a tag if the list exists.
                List<GridNode> mappedNodes = destinationsMap.get(tag);
                if(mappedNodes == null) {
                    // If there is no mapping for the key yet, then create an
                    // array list containing the grid node specified by the 
                    // parsed x, y, and z coordinates.
                    List<GridNode> list = new ArrayList<>();
                    list.add(searchSpace.get(location.z()).get(location.y()).get(location.x()));
                    destinationsMap.put(tag, list);
                }
                else {
                    mappedNodes.add(searchSpace.get(location.z()).get(location.y()).get(location.x()));
                }
            }
        }
        return destinationsMap;
    }
    
    private Map<Beacon, RectCoordinates> parseBeaconLocationMap(JsonNode parent, String fieldName, String beaconUUID) {
        JsonNode beaconsNode = parent.get(fieldName);
        if(beaconsNode == null || !beaconsNode.isArray()) {
            System.out.println("unable to parse destinations from json");
            return null;
        }
        
        BeaconBuilder beaconBuilder = new BeaconBuilder();
        Map<Beacon, RectCoordinates> beaconLocationMap = new HashMap<>();
        
        for(int i = 0; i < beaconsNode.size(); i++) {
            JsonNode beaconNode = beaconsNode.get(i);
            // Beacon name
            String beaconName = beaconNode.get("Name").asText();
            if(beaconName == "") {
                System.out.println("unable to parse beacon name at index " + i + " from json");
                return null;
            }
            // Beacon address
            String beaconAddress = beaconNode.get("Address").asText();
            if(beaconAddress == "") {
                System.out.println("unable to parse beacon address at index " + i + " from json");
                return null;
            }
            // Beacon major value
            int beaconMajorValue = parseIntNode(beaconNode, "Major");
            if(beaconMajorValue == -1) {
                System.out.println("unable to parse beacon major value at index " + i + ". Not an int?");
                return null;
            }
            // Beacon minor value
            int beaconMinorValue = parseIntNode(beaconNode, "Minor");
            if(beaconMinorValue == -1) {
                System.out.println("unable to parse beacon minor value at index " + i + ". Not an int?");
                return null;
            }
            // Beacon rssi
            int beaconRSSI = parseIntNode(beaconNode, "RSSI");
            if(beaconRSSI == -1) {
                System.out.println("unable to parse beacon rssi at index " + i + ". Not an int?");
                return null;
            }
            // Beacon real world location
            RectCoordinates loc = parseRectCoordinates(beaconNode);
            if(loc == null) {
                System.out.println("unable to parse location index " + i + " of " + fieldName + ".");
                return null;
            }

            // Create the beacon
            Beacon beacon = beaconBuilder.setBeaconName(beaconName)
                    .setBeaconAddress(beaconAddress)
                    .setBeaconUUID(beaconUUID)
                    .setBeaconMajor(beaconMajorValue)
                    .setBeaconMinor(beaconMinorValue)
                    .setBeaconRSSI(beaconRSSI)
                    .build();

            // If there is no beacon with the same settings as the newly created beacon,
            // add it to the map. Otherwise, return null.
            if(beaconLocationMap.get(beacon) == null) {
                beaconLocationMap.put(beacon, loc);
            }
            else {
                System.out.println("unable to parse json. multiple beacons with the same settings");
                return null;
            }
        }
        return beaconLocationMap;
    }

    private List<FloorConnector> calcRemainingConnections(List<FloorConnector> floorConnectors,
                                                          int numFloors) {
        if(numFloors > 2) {
            // Start at floor 2 and increment each floor adding the appropriate connections.
            for(int currFloor = 2; currFloor < numFloors; currFloor++) {
                floorConnectors =
                        updateFloorConnections(floorConnectors, currFloor, currFloor - 1);
            }

            // Start at 2 floors below the top floor and decrement each floor adding the
            // appropriate connections.
            for(int currFloor = numFloors - 3; currFloor >= 0; currFloor--) {
                floorConnectors =
                        updateFloorConnections(floorConnectors, currFloor, currFloor + 1);
            }

            // Add all connectors that are on the same floor to each others' connections
            for(FloorConnector connector : floorConnectors) {
                for(FloorConnector otherConnector : floorConnectors) {
                    if(connector != otherConnector &&
                            connector.location().z() == otherConnector.location().z()) {
                        connector.addConnection(otherConnector);
                    }
                }
            }
        }
        return floorConnectors;
    }

    private List<FloorConnector> updateFloorConnections(List<FloorConnector> floorConnectors,
                                                        int currentFloor,
                                                        int connectionFloor) {

        for(FloorConnector connector : floorConnectors) {
            if(connector.location().z() == currentFloor) {
                // Used to store all of the connections that needed to be added to
                // connector's connections, so that they can be added after all iterating to
                // prevent ConcurrentModificationExceptions
                List<FloorConnector> newConnections = new ArrayList<>();

                List<FloorConnector> connections = connector.getConnections();
                for(FloorConnector connection : connections) {
                    if(connection.location().z() == connectionFloor) {
                        List<FloorConnector> conConnections = connection.getConnections();
                        for(FloorConnector conConnection : conConnections) {
                            if(conConnection != connector) {
                                // Add the connection's connection to the connector's
                                // connections as long as it's not the current connector and
                                // it is on the connectionFloor.
                                newConnections.add(conConnection);
                            }
                        }
                    }
                }
                connector.addAllConnections(newConnections);
            }
        }
        return floorConnectors;
    }
    
    private RectCoordinates parseRectCoordinates(JsonNode parent) {
        int x = parseIntNode(parent, X);
        if(x < 0) {
            System.out.println("Invalid x.");
            return null;
        }
        
        int y = parseIntNode(parent, Y);
        if(y < 0) {
            System.out.println("Invalid y.");
            return null;
        }
        
        int z = parseIntNode(parent, Z);
        if(z < 0) {
            System.out.println("Invalid z.");
            return null;
        }
        
        return new RectCoordinates(x, y, z);
    }
    
    private int parseIntNode(JsonNode parent, String fieldName) {
        JsonNode node = parent.get(fieldName);
        if(node == null || !node.isInt()) {
            return -1;
        }
        return node.asInt(-1);
    }

    private ObjectNode getObjectNode(ObjectNode objNode, RectCoordinates location) {
        return objNode.put(X, location.x())
                .put(Y, location.y())
                .put(Z, location.z());
    }
}
