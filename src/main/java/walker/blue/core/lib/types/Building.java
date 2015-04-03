package walker.blue.core.lib.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.path.lib.FloorConnector;
import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.RectCoordinates;

public class Building {

    /**
     * Default value for the UUID
     */
    private static final String DEFAULT_UUID = "";
    private static final double DEFAULT_NODE_DISTANCE = 0.0f;
    private static final double DEFAULT_FLOOR_HEIGHT = 0.0f;
    private static final int DEFAULT_DIMENSION = 0;

    /**
     * The uuid of the building to uniquely identify it from other buildings.
     */
    protected String uuid;
    /**
     * The distance between each node and its adjacent neighbors in searchSpace.
     */
    protected double nodeDistance;
    /**
     * The approximate height of each floor assuming all floors have the same height.
     */
    protected double floorHeight;
    /**
     * Holds the bode layout of the building used for pathfinding.
     */
    protected List<List<List<GridNode>>> searchSpace;
    /**
     * Contains references to all FloorConnectors within the searchSpace.
     */
    protected List<FloorConnector> floorConnectors;
    /**
     * Maps a beacon to its location within the building.
     */
    protected Map<Beacon, RectCoordinates> beaconLocationMap;
    /**
     * Maps strings to a list of locations that represent the locations of the nodes
     * in the search space that have the given string tag.
     */
    protected DestinationTable destinationTable;
    /**
     *
     */
    protected RectCoordinates northPoint;

    /**
     * Constructor. Sets the fields to their default values
     */
    public Building() {
        this(DEFAULT_UUID,
                DEFAULT_NODE_DISTANCE,
                DEFAULT_FLOOR_HEIGHT,
                new ArrayList<List<List<GridNode>>>(),
                new ArrayList<FloorConnector>(),
                new HashMap<Beacon, RectCoordinates>(),
                new DestinationTable(),
                new RectCoordinates(DEFAULT_DIMENSION, DEFAULT_DIMENSION, DEFAULT_DIMENSION));
    }

    /**
     * Constructor. Sets the fuelds to the given valyes
     *
     * @param uuid String of unique building identifier
     * @param nodeDistance double of distance between neighboring nodes
     * @param floorHeight double of floor height in building
     * @param searchSpace 3D list holding the building's node layout
     * @param floorConnectors a list holding all of the building's floor connectors
     * @param beaconLocationMap map that maps beacons to locations in the building
     * @param destinationTable table containing the destinations
     */
    public Building(final String uuid,
                    final double nodeDistance,
                    final double floorHeight,
                    final List<List<List<GridNode>>> searchSpace,
                    final List<FloorConnector> floorConnectors,
                    final Map<Beacon, RectCoordinates> beaconLocationMap,
                    final DestinationTable destinationTable,
                    final RectCoordinates northPoint) {
        this.uuid = uuid;
        this.nodeDistance = nodeDistance;
        this.floorHeight = floorHeight;
        this.searchSpace = searchSpace;
        this.floorConnectors = floorConnectors;
        this.beaconLocationMap = beaconLocationMap;
        this.destinationTable = destinationTable;
        this.northPoint = northPoint;
    }

    /**
     * Getter for the uuid field
     *
     * @return current value of the uuid field
     */
    public String getUUID() {
        return this.uuid;
    }

    /**
     * Sets the uuid field to the given value
     *
     * @param uuid new value of the uuid
     */
    public void setUUID(final String uuid) {
        this.uuid = uuid;
    }

    /**
     * Getter for the node distance field
     *
     * @return current value of the node distance field
     */
    public double getNodeDistance() {
        return this.nodeDistance;
    }

    /**
     * Sets the nodeDistance field to the given value
     *
     * @param nodeDistance new value of the nodeDistance
     */
    public void setNodeDistance(double nodeDistance) {
        this.nodeDistance = nodeDistance;
    }

    /**
     * Getter for the floor height field
     *
     * @return current value of the floor height field
     */
    public double getFloorHeight() {
        return this.floorHeight;
    }

    /**
     * Sets the floorHeight field to the given value
     *
     * @param floorHeight new value of the floorHeight
     */
    public void setFloorHeight(double floorHeight) {
        this.floorHeight = floorHeight;
    }

    /**
     * Getter for the search space field
     *
     * @return current value of the search space field
     */
    public List<List<List<GridNode>>> getSearchSpace() {
        return this.searchSpace;
    }

    /**
     * Sets the searchSpace field to the given value
     *
     * @param searchSpace new value of the searchSpace
     */
    public void setSearchSpace(List<List<List<GridNode>>> searchSpace) {
        this.searchSpace = searchSpace;
    }

    /**
     * Getter for the floorConnectors field
     *
     * @return current value of the floorConnectors field
     */
    public List<FloorConnector> getFloorConnectors() {
        return this.floorConnectors;
    }

    /**
     * Sets the floorConnectors field to the given value
     *
     * @param floorConnectors new value of the floorConnectors
     */
    public void setFloorConnectors(List<FloorConnector> floorConnectors) {
        this.floorConnectors = floorConnectors;
    }

    /**
     * Getter for the beaconLocationMap field
     *
     * @return current value of the beaconLocationMap field
     */
    public Map<Beacon, RectCoordinates> getBeaconLocationMap() {
        return this.beaconLocationMap;
    }

    /**
     * Sets the beaconLocationMap field to the given value
     *
     * @param beaconLocationMap new value of the beaconLocationMap
     */
    public void setBeaconLocationMap(Map<Beacon, RectCoordinates> beaconLocationMap) {
        this.beaconLocationMap = beaconLocationMap;
    }

    /**
     * Getter for the destinationTable field
     *
     * @return current value of the destinationTable field
     */
    public DestinationTable getDestinationTable() {
        return this.destinationTable;
    }

    /**
     * Sets the destinationTable field to the given value
     *
     * @param destinationTable new value of the destinationTable
     */
    public void setDestinationMap(DestinationTable destinationTable) {
        this.destinationTable = destinationTable;
    }

    public RectCoordinates getNorthPoint() {
        return this.northPoint;
    }

    public void setNorthPoint(final RectCoordinates northPoint) {
        this.northPoint = northPoint;
    }

    /**
     * Converts the searchSpace field to string
     *
     * @return string representation of the searchSpace field
     */
    public String searchSpaceToString() {
        final StringBuilder ssBuilder = new StringBuilder();
        for(final List<List<GridNode>> floor : this.searchSpace) {
            for(final List<GridNode> row : floor) {
                for(final GridNode node : row) {
                    if(node.isTraversable()) {
                        ssBuilder.append("T ");
                    }
                    else {
                        ssBuilder.append("F ");
                    }
                }
                ssBuilder.append("\n");
            }
            ssBuilder.append("\n");
        }
        return ssBuilder.toString();
    }

    /**
     * Converts the floorConnectors field to string
     *
     * @return string representation of the floorConnectors field
     */
    public String floorConnectorsToString() {
        if(this.floorConnectors == null) {
            return "null";
        }
        final StringBuilder fcBuilder = new StringBuilder();
        for(final FloorConnector connector : floorConnectors) {
            fcBuilder.append(connector.toString())
                    .append("\n");
        }
        return fcBuilder.toString();
    }

    /**
     * Converts the beaconLocationMap field to string
     *
     * @return string representation of the beaconLocationMap field
     */
    public String beaconLocationMapToString() {
        if(this.beaconLocationMap == null) {
            return "null";
        }
        final StringBuilder blmBuilder = new StringBuilder();
        final Set<Beacon> beaconSet = beaconLocationMap.keySet();
        for(Beacon beacon : beaconSet) {
            // Get each value for each beacon key and store its string in blmBuilder
            blmBuilder.append(beacon.toString())
                    .append("\n")
                    .append("Location: ")
                    .append(this.beaconLocationMap.get(beacon).toString())
                    .append("\n");
        }
        return blmBuilder.toString();
    }

    /**
     * Converts the destinationTable field to string
     *
     * @return string representation of the destinationTable field
     */
    public String destinationTableToString() {
        if(this.destinationTable == null) {
            return "null";
        }
        final StringBuilder dtBuilder = new StringBuilder();
        for (final DestinationType type : DestinationType.values()) {
            dtBuilder.append("Type : ");
            dtBuilder.append(type.name());
            dtBuilder.append("\n");
            if (type.isGeneric()) {
                for (final GridNode node : this.destinationTable.getGeneric(type)) {
                    dtBuilder.append("\t");
                    dtBuilder.append(node.toString());
                    dtBuilder.append("\n");
                }
            } else {
                for (final Map.Entry<String, GridNode> entry : this.destinationTable.getAllNonGeneric(type)) {
                    dtBuilder.append("\t");
                    dtBuilder.append(entry.getKey());
                    dtBuilder.append("  -  ");
                    dtBuilder.append(entry.getValue());
                    dtBuilder.append("\n");
                }
            }
        }
        return dtBuilder.toString();
    }

    @Override
    public String toString() {
        return "UUID: " + this.uuid + "\n" +
                "Node Distance: " + this.nodeDistance + "\n" +
                "Floor Height: " + this.floorHeight + "\n" +
                "Search Space:\n" + this.searchSpaceToString() + "\n" +
                "Floor Connectors:\n" + this.floorConnectorsToString() + "\n" +
                "Beacon Location Map:\n" + this.beaconLocationMapToString() + "\n" +
                "Destination Map:\n" + this.destinationTableToString() + "\n" +
                "North Point: " + this.northPoint.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Building building = (Building) o;

        if (Double.compare(building.floorHeight, floorHeight) != 0) {
            return false;
        }
        if (Double.compare(building.nodeDistance, nodeDistance) != 0) {
            return false;
        }
        if (!beaconLocationMap.equals(building.beaconLocationMap)) {
            return false;
        }
        if (!destinationTable.equals(building.destinationTable)) {
            return false;
        }
        if (!floorConnectors.equals(building.floorConnectors)) {
            return false;
        }
        if (!searchSpace.equals(building.searchSpace)) {
            return false;
        }
        if (!uuid.equals(building.uuid)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = uuid.hashCode();
        temp = Double.doubleToLongBits(nodeDistance);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(floorHeight);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + searchSpace.hashCode();
        result = 31 * result + floorConnectors.hashCode();
        result = 31 * result + beaconLocationMap.hashCode();
        result = 31 * result + destinationTable.hashCode();
        return result;
    }
}