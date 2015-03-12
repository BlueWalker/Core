package walker.blue.core.lib.types;

import walker.blue.path.lib.FloorConnector;
import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.RectCoordinates;
import walker.blue.beacon.lib.beacon.Beacon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Building {
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
	protected Map<String, List<GridNode>> destinationMap;

    public Building() {
        this.uuid = "";
        this.nodeDistance = 0.0;
        this.floorHeight = 0.0;
        this.searchSpace = new ArrayList<>();
        this.floorConnectors = new ArrayList<>();
        this.beaconLocationMap = new HashMap<>();
        this.destinationMap = new HashMap<>();
    }

    /**
     * Constructor.
     * @param uuid String of unique building identifier
     * @param nodeDistance double of distance between neighboring nodes
     * @param floorHeight double of floor height in building
     * @param searchSpace 3D list holding the building's node layout
     * @param floorConnectors a list holding all of the building's floor connectors
     * @param beaconLocationMap map that maps beacons to locations in the building
     * @param destinationMap map that maps strings/tags to nodes
     */
	public Building(final String uuid,
                    final double nodeDistance,
                    final double floorHeight,
                    final List<List<List<GridNode>>> searchSpace,
                    final List<FloorConnector> floorConnectors,
	                final Map<Beacon, RectCoordinates> beaconLocationMap,
	                final Map<String, List<GridNode>> destinationMap) {
        this.uuid = uuid;
        this.nodeDistance = nodeDistance;
        this.floorHeight = floorHeight;
		this.searchSpace = searchSpace;
		this.floorConnectors = floorConnectors;
		this.beaconLocationMap = beaconLocationMap;
		this.destinationMap = destinationMap;
	}

    public String getUUID() {
        return this.uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public double getNodeDistance() {
        return this.nodeDistance;
    }

    public void setNodeDistance(double nodeDistance) {
        this.nodeDistance = nodeDistance;
    }

    public double getFloorHeight() {
        return this.floorHeight;
    }

    public void setFloorHeight(double floorHeight) {
        this.floorHeight = floorHeight;
    }

    public List<List<List<GridNode>>> getSearchSpace() {
        return this.searchSpace;
    }

    public void setSearchSpace(List<List<List<GridNode>>> searchSpace) {
        this.searchSpace = searchSpace;
    }

    public List<FloorConnector> getFloorConnectors() {
        return this.floorConnectors;
    }

    public void setFloorConnectors(List<FloorConnector> floorConnectors) {
        this.floorConnectors = floorConnectors;
    }

    public Map<Beacon, RectCoordinates> getBeaconLocationMap() {
        return this.beaconLocationMap;
    }

    public void setBeaconLocationMap(Map<Beacon, RectCoordinates> beaconLocationMap) {
        this.beaconLocationMap = beaconLocationMap;
    }

    public Map<String, List<GridNode>> getDestinationMap() {
        return this.destinationMap;
    }

    public void setDestinationMap(Map<String, List<GridNode>> destinationMap) {
        this.destinationMap = destinationMap;
    }

    public String searchSpaceToString() {
        StringBuilder ssBuilder = new StringBuilder();
        for(List<List<GridNode>> floor : this.searchSpace) {
            for(List<GridNode> row : floor) {
                for(GridNode node : row) {
                    if(node.walkable()) {
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

    public String floorConnectorsToString() {
        if(floorConnectors == null) {
            return "null";
        }
        StringBuilder fcBuilder = new StringBuilder();
        for(FloorConnector connector : floorConnectors) {
            fcBuilder.append(connector.toString() + "\n");
        }
        return fcBuilder.toString();
    }

    public String beaconLocationMapToString() {
        if(beaconLocationMap == null) {
            return "null";
        }
        StringBuilder blmBuilder = new StringBuilder();
        Set<Beacon> beaconSet = beaconLocationMap.keySet();
        for(Beacon beacon : beaconSet) {
            // Get each value for each beacon key and store its string in blmBuilder
            blmBuilder.append(beacon.toString() + "\n" +
                    "Location: " + beaconLocationMap.get(beacon).toString() + "\n");
        }
        return blmBuilder.toString();
    }

    public String destinationMapToString() {
        if(destinationMap == null) {
            return "null";
        }
        StringBuilder dmBuilder = new StringBuilder();
        Set<String> tagSet = destinationMap.keySet();
        for(String tag : tagSet) {
            dmBuilder.append(tag + ":\n");
            List<GridNode> nodes = destinationMap.get(tag);
            for(GridNode node : nodes) {
                dmBuilder.append(node.location().toString() + "\n");
            }
        }
        return dmBuilder.toString();
    }

    @Override
    public String toString() {
        return "UUID: " + this.uuid + "\n" +
                "Node Distance: " + this.nodeDistance + "\n" +
                "Floor Height: " + this.floorHeight + "\n" +
                "Search Space:\n" + searchSpaceToString() + "\n" +
                "Floor Connectors:\n" + floorConnectorsToString() + "\n" +
                "Beacon Location Map:\n" + beaconLocationMapToString() + "\n" +
                "Destination Map:\n" + destinationMapToString() + "\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Building building = (Building) o;

        if (Double.compare(building.floorHeight, floorHeight) != 0) {
            return false;
        }
        if (Double.compare(building.nodeDistance, nodeDistance) != 0) {
            return false;
        }
        if (!beaconLocationMap.equals(building.beaconLocationMap)) {
            return false;
        }
        if (!destinationMap.equals(building.destinationMap)) {
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
        result = 31 * result + destinationMap.hashCode();
        return result;
    }
}