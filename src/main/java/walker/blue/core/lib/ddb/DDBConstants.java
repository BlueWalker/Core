package walker.blue.core.lib.ddb;

/**
 * Constants relating to the DynamoDB database
 */
public class DDBConstants {

    private DDBConstants() {}

    /* ---- GENERIC CONSTANTS ---- */
    public static final String X = "x";
    public static final String Y = "y";
    public static final String Z = "z";
    /* ---- BUILDING DATA CONSTANTS ---- */
    public static final String BUILDING_DATA = "BuildingData";
    /* ---- BEACON CONSTANTS ---- */
    public static final String BEACONS = "Beacons";
    public static final String BEACON_ADDRESS = "Address";
    public static final String BEACON_MAJOR = "Major";
    public static final String BEACON_MINOR = "Minor";
    public static final String BEACON_NAME = "Name";
    public static final String BEACON_RSSI = "RSSI";
    /* ---- DESTINATION CONSTANTS ---- */
    public static final String DESTINATIONS = "Destinations";
    public static final String DESTINATION_TYPE = "Type";
    public static final String DESTINATION_KEY = "Key";
    /* ---- NODE CONSTANTS ---- */
    public static final String NODES = "NodeCollection";
    public static final String NODE_TRANSVERSABLE = "O";
    public static final String NODE_NON_TRANSVERSABLE = "X";
    public static final String NODE_STARIS = "S";
    public static final String NODE_ELEVATOR = "E";
    public static final String NODE_DEFAULT = "-";
    /* ---- NODE DISTANCE CONSTANTS ---- */
    public static final String NODE_DISTANCE = "NodeDistance";
    public static final int DEFAULT_NODE_DISTANCE = -1;
    /* ---- FLOOR HEIGHT CONSTANTS ---- */
    public static final String FLOOR_HEIGHT = "FloorHeight";
    public static final int DEFAULT_FLOOR_HEIGHT = -1;
    /* ---- FLOOR CONNECTORS CONSTANTS ---- */
    public static final String FLOOR_CONNECTORS = "Stairs/Elevators";
    public static final String FLOOR_CONNECTORS_CONNECTIONS = "Connections";
    /* ---- UUID CONSTANTS ---- */
    public static final String UUID = "BuildingID";
    /* ---- NORTHCONSTANTS ---- */
    public static final String NORTH_POINT = "NorthPoint";
}