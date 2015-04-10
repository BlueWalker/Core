package walker.blue.core.lib.init;

import android.content.Context;
import android.util.Log;

import com.amazonaws.services.dynamodbv2.model.GetItemResult;

import org.apache.commons.math3.distribution.LogisticDistribution;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import walker.blue.beacon.lib.beacon.Beacon;
import walker.blue.core.lib.common.ProcessCommon;
import walker.blue.core.lib.ddb.AttrToJava;
import walker.blue.core.lib.ddb.DynamoDBWrapper;
import walker.blue.core.lib.input.UserInputParser;
import walker.blue.core.lib.types.Building;
import walker.blue.core.lib.types.DestinationTable;
import walker.blue.core.lib.types.DestinationType;
import walker.blue.path.lib.FloorSequencer;
import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.RectCoordinates;
import walker.blue.path.lib.ThetaStar;
import walker.blue.tri.lib.Trilateration;

/**
 * Initialization process of the Bluewalker core package
 */
public class InitializeProcess extends ProcessCommon implements Callable<InitializeProcess.Output> {

    /**
     * Log messages
     */
    private static final String LOG_FAILED_BD = "Consuming BuildingDetector Future failed - %s";
    private static final String LOG_FAILED_BEACONS= "Consuming beacons Future failed - %s";
    private static final String LOG_RECIEVED_BUILDING_ID = "Received buidling ID - %s";
    private static final String LOG_BEACONS_FUTURE = "Consuming Beacons Future";
    private static final String LOG_BEACONS_DONE = "Finished consuming Beacons Future. %s beacons found";

    /**
     * Context under which the initialize process is being run
     */
    private Context context;
    /**
     * The user input
     */
    private List<String> userInput;

    /**
     * Constructor. Sets the context and userInput fields to the given objects
     *
     * @param context Context used to create the beacon scan client
     * @param userInput List of strings containing the users input
     */
    public InitializeProcess(final Context context, final List<String> userInput) {
        this.context = context;
        this.userInput = userInput;
    }

    @Override
    public Output call() {
        final BuildingDetector.Output bdOutput = this.getCurrentBuilding();
        if (bdOutput == null) {
            Log.d(this.getClass().getName(), "BuildingDetector Failed");
            return new Output(InitError.BD_FAIL);
        }
        final Building building = this.getBuildingData(bdOutput.getBuildingID());
        if (building == null) {
            Log.d(this.getClass().getName(), "Failed getting building data");
            return new Output(InitError.NULL_BUILDING);
        }
        final UserInputParser inputParser = new UserInputParser(this.userInput);
        final DestinationType destinationType = this.getDestinationType(inputParser);
        if (destinationType == null) {
            Log.d(this.getClass().getName(), "Failed Getting destination type");
            return new Output(InitError.NULL_DEST_TYPE);
        }
        final DestinationTable destinationTable = building.getDestinationTable();
        GridNode destination = null;
        Set<GridNode> possibleDestinations = null;
        if (destinationType.isGeneric()) {
            possibleDestinations = destinationTable.getGeneric(destinationType);
        } else {
            destination = this.getNonGenericDestination(destinationType, inputParser, destinationTable);
        }
        if (destination == null && possibleDestinations == null) {
            Log.d(this.getClass().getName(), "Invalid destination");
            return new Output(InitError.INVALID_INPUT);
        }

        // Consume beacons being scanned
        final List<Beacon> beaconSet;
        try {
            Log.d(this.getClass().getName(), LOG_BEACONS_FUTURE);
            beaconSet = bdOutput.getFuture().get();
            Log.d(this.getClass().getName(), String.format(LOG_BEACONS_DONE, beaconSet.size()));
        } catch (final Exception e) {
            Log.d(this.getClass().getName(), String.format(LOG_FAILED_BEACONS, e.getMessage()));
            return new Output(InitError.BEACONS_FAIL);
        }

        final Trilateration trilateration = null;
        final GridNode currentNode = this.getUserLocationProximity(beaconSet, building);
        if (currentNode == null) {
            Log.d(this.getClass().getName(), "Failed getting initial user location");
            return new Output(InitError.LOCATION_FAIL);
        }

        if (destinationType.isGeneric()) {
            destination = this.findClosestNodeNaive(possibleDestinations, currentNode);
        }

        // Generate the path for the user
        final ThetaStar thetaStar = new ThetaStar();
        final FloorSequencer floorSequencer = new FloorSequencer(thetaStar,
                building.getSearchSpace(),
                building.getFloorConnectors());
        final List<GridNode> path = floorSequencer.findPath(currentNode, destination);
        if (path == null) {
            Log.d(this.getClass().getName(), "Failed getting path");
            return null;
        }

        // Pack everything into the output object and return it
        return new Output(floorSequencer,
                trilateration,
                path,
                building,
                currentNode,
                null);
    }

    /**
     * Uses the BuilderDetector class to find the the of the building the user
     * is in
     *
     * @return BuildingDetector.Output returned by the building detector
     */
    private BuildingDetector.Output getCurrentBuilding() {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        // Detect the building the user is in
        final BuildingDetector buildingDetector = new BuildingDetector(this.context);
        final BuildingDetector.Output bdOutput;
        try {
            bdOutput = executorService.submit(buildingDetector).get();
        } catch (Exception e) {
            Log.d(this.getClass().getName(), String.format(LOG_FAILED_BD, e.getMessage()));
            return null;
        }
        if (bdOutput.getBuildingID() == null) {
            return null;
        } else {
            Log.d(this.getClass().getName(), String.format(LOG_RECIEVED_BUILDING_ID, bdOutput.getBuildingID()));
            return bdOutput;
        }
    }

    /**
     * Fetches the data for the building corresponding to the given building id
     *
     * @param buildingID String the id of a building
     * @return Building object correspoinding to the given buildign id
     */
    private Building getBuildingData(final String buildingID) {
        final DynamoDBWrapper ddb = new DynamoDBWrapper();
        final GetItemResult rawBuildingData = ddb.getBuildingData(buildingID);
        final Building building = AttrToJava.getItemResultToBuilding(rawBuildingData);
        if (building != null) {
            Log.d(this.getClass().getName(), "Got building data");
        }
        return building;
    }

    /**
     * Gets the destination type from the user input
     *
     * @param inputParser the UserInputParser containing the input of the user
     * @return Destination type specified by the user
     */
    private DestinationType getDestinationType(final UserInputParser inputParser) {
        final Set<String> keywords = inputParser.getKeywords();
        if (keywords.size() != 1) {
            Log.d(this.getClass().getName(), "keywords.size() != 1");
            return null;
        }
        DestinationType destinationType = null;
        for (final String keyword : keywords) {
            Log.d(this.getClass().getName(), "keyword" + keyword);
            destinationType = DestinationType.valueOf(keyword.toUpperCase());
        }
        return destinationType;
    }

    /**
     * Excracts a non-generic desitination from the Destination table accoring
     * to the users input
     *
     * @param destinationType DestinationType of the destination being
     *                        extracted
     * @param inputParser the UserInputParser containing the input of the user
     * @param destinationTable DestinationTable for the building
     * @return Destination specified by the user
     */
    private GridNode getNonGenericDestination(final DestinationType destinationType,
                                              final UserInputParser inputParser,
                                              final DestinationTable destinationTable) {
        final Set<String> keys = inputParser.getFilteredNumbers(destinationTable, destinationType);
        if (keys.size() != 1) {
            return null;
        }
        String secondaryKey = null;
        for(final String key : keys) {
            secondaryKey = key;
        }
        return destinationTable.getNonGeneric(destinationType, secondaryKey);
    }

    /**
     * Find closest node to the given start node using a naive distance
     * formula method
     *
     * @param nodes Nodes being compared to the starting node
     * @param start Starting node
     * @return Node closes to the given starting node
     */
    private GridNode findClosestNodeNaive(final Set<GridNode> nodes, final GridNode start) {
        double minDistance = Double.MAX_VALUE;
        GridNode closestNode = null;
        for (final GridNode node : nodes) {
            final double currentDistance = this.distanceBetweenNodes(start, node);
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
                closestNode = node;
            }
        }
        return closestNode;
    }

    /**
     * Find closest node to the given start node using a more robust method
     * NOTE: This calculates the path for the all given nodes and is therefore
     *       slow
     *
     * @param nodes Nodes being compared to the starting node
     * @param start Starting node
     * @param pathfinder Pathfinder obejct user to get the path between the
     *                   nodes
     * @return Shotest path for all given ndoes
     */
    private List<GridNode> findClosestNode(final Set<GridNode> nodes,
                                           final GridNode start,
                                           final FloorSequencer pathfinder) {
        double minDistance = Double.MAX_VALUE;
        List<GridNode> shortestPath = null;
        for (final GridNode node : nodes) {
            final List<GridNode> path = pathfinder.findPath(start, node);
            final double currentPathDistance = this.pathDistance(path);
            if (currentPathDistance < minDistance) {
                minDistance = currentPathDistance;
                shortestPath = path;
            }
        }
        return shortestPath;
    }

    /**
     * Calculate the total distance of the given path
     *
     * @param path Path whose distance is being calculated
     * @return Total distance of the path
     */
    private double pathDistance(final List<GridNode> path) {
        double distance = 0.0f;
        for (int i = 1; i < path.size(); i++) {
            distance += this.distanceBetweenNodes(path.get(i - 1), path.get(i));
        }
        return distance;
    }

    /**
     * Calculate the distance between the 2 given nodes using the distance
     * formula
     *
     * @param nodeA Node
     * @param nodeB Node
     * @return Distance between the two given nodes
     */
    private double distanceBetweenNodes(final GridNode nodeA, final GridNode nodeB) {
        final RectCoordinates locA = nodeA.getLocation();
        final RectCoordinates locB = nodeB.getLocation();
        return Math.sqrt(Math.pow(locA.getX() - locB.getX(), 2) +
                Math.pow(locA.getY() - locB.getY(), 2) +
                Math.pow(locA.getZ() - locB.getZ(), 2));
    }

    /**
     * Class which holds the output of the InitializeProcess
     */
    public class Output {

        private FloorSequencer pathfinder;
        private Trilateration trilateration;
        private List<GridNode> path;
        private Building building;
        private GridNode currentLocation;
        private InitError error;

        public Output(final InitError error) {
            this(null,
                    null,
                    null,
                    null,
                    null,
                    error);
        }

        private Output(final FloorSequencer pathfinder,
                       final Trilateration trilateration,
                       final List<GridNode> path,
                       final Building building,
                       final GridNode currentLocation,
                       final InitError error) {
            this.pathfinder = pathfinder;
            this.trilateration = trilateration;
            this.path = path;
            this.building = building;
            this.currentLocation = currentLocation;
            this.error = error;
        }

        public Trilateration getTrilateration() {
            return this.trilateration;
        }

        public List<GridNode> getPath() {
            return this.path;
        }

        public Building getBuilding() {
            return this.building;
        }

        public GridNode getCurrentLocation() {
            return this.currentLocation;
        }

        public InitError getError() {
            return this.error;
        }
    }
}
