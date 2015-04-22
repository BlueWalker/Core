package walker.blue.core.lib.init;

import android.content.Context;
import android.util.Log;

import com.amazonaws.services.dynamodbv2.model.GetItemResult;

import java.util.ArrayList;
import java.util.Iterator;
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
import walker.blue.core.lib.speech.GeneratedSpeech;
import walker.blue.core.lib.speech.SpeechGenerator;
import walker.blue.core.lib.types.Building;
import walker.blue.core.lib.types.DestinationTable;
import walker.blue.core.lib.types.DestinationType;
import walker.blue.path.lib.floor.FloorSequencer;
import walker.blue.path.lib.node.GridNode;
import walker.blue.path.lib.node.RectCoordinates;
import walker.blue.path.lib.finder.ThetaStar;
import walker.blue.tri.lib.Trilateration;

/**
 * Initialization process of the Bluewalker core package
 */
public class InitializeProcess extends ProcessCommon implements Callable<InitializeProcess.Output> {

    /**
     * Log messages
     */
    protected static final String LOG_FAILED_BD = "Consuming BuildingDetector Future failed - %s";
    protected static final String LOG_FAILED_BEACONS= "Consuming beacons Future failed - %s";
    protected static final String LOG_RECIEVED_BUILDING_ID = "Received buidling ID - %s";
    protected static final String LOG_BEACONS_FUTURE = "Consuming Beacons Future";
    protected static final String LOG_BEACONS_DONE = "Finished consuming Beacons Future. %s beacons found";

    /**
     * Context under which the initialize process is being run
     */
    protected Context context;
    /**
     * The user input
     */
    protected List<String> userInput;

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
            return new Output(InitError.BD_FAIL);
        }
        final Building building = this.getBuildingData(bdOutput.getBuildingID());
//        final Building building = this.getBuildingData("0112233445566778899aabbccddeeff0");
        if (building == null) {
            return new Output(InitError.NULL_BUILDING);
        }
        final UserInputParser inputParser = new UserInputParser(this.userInput);
        final DestinationType destinationType = this.getDestinationType(inputParser);
        if (destinationType == null) {
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
//        final GridNode currentNode = this.debugLocationGet(building);
        if (currentNode == null) {
            return new Output(InitError.LOCATION_FAIL);
        } else if (this.alreadyArrived(currentNode, destination)) {
            return new Output(InitError.ALREADY_ARRIVED);
        }
        if (destinationType.isGeneric()) {
            destination = this.findClosestNodeNaive(possibleDestinations, currentNode);
        }
        // Generate the path for the user
        final ThetaStar thetaStar = new ThetaStar();
        final FloorSequencer floorSequencer = new FloorSequencer(thetaStar,
                building.getSearchSpace(),
                building.getFloorConnectors());
        final List<GridNode> path = this.checkPath(floorSequencer.findPath(currentNode, destination), building);
        if (path == null) {
            return new Output(InitError.PATH_FAIL);
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
    protected BuildingDetector.Output getCurrentBuilding() {
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
    protected Building getBuildingData(final String buildingID) {
        final DynamoDBWrapper ddb = new DynamoDBWrapper();
        final GetItemResult rawBuildingData = ddb.getBuildingData(buildingID);
        final Building building = AttrToJava.getItemResultToBuilding(rawBuildingData);
        return building;
    }

    /**
     * Gets the destination type from the user input
     *
     * @param inputParser the UserInputParser containing the input of the user
     * @return Destination type specified by the user
     */
    protected DestinationType getDestinationType(final UserInputParser inputParser) {
        final Set<String> keywords = inputParser.getKeywords();
        if (keywords.size() != 1) {
            return null;
        }
        DestinationType destinationType = null;
        for (final String keyword : keywords) {
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
    protected GridNode getNonGenericDestination(final DestinationType destinationType,
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
    protected GridNode findClosestNodeNaive(final Set<GridNode> nodes, final GridNode start) {
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
    protected List<GridNode> findClosestNode(final Set<GridNode> nodes,
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
    protected double pathDistance(final List<GridNode> path) {
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
    protected double distanceBetweenNodes(final GridNode nodeA, final GridNode nodeB) {
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

        public Output(final FloorSequencer pathfinder,
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

    /**
     * Checks if the user is already at the destination
     *
     * @param userLocation current location of the user
     * @param destination The destination
     * @return boolean indicating whether the user is already at the destination
     */
    private boolean alreadyArrived(final GridNode userLocation, final GridNode destination) {
        final double distance = this.distanceBetweenNodes(userLocation, destination);
        final RectCoordinates userRect = userLocation.getLocation();
        final RectCoordinates destRect = destination.getLocation();
        return distance <= 3.0f &&
                (userRect.getX() - destRect.getX() <= 2 || userRect.getY() - destRect.getY() <=2);
    }

    /**
     * Checks the path for troublesome turns. Currently defaults to out solutions.
     * This need to be improved
     *
     * @param path Path being checked
     * @param build building
     * @return fixed list
     */
    private List<GridNode> checkPath(final List<GridNode> path, final Building build) {
        final SpeechGenerator speechGenerator = new SpeechGenerator(path);
        final List<GridNode> res = new ArrayList<>();
        final Iterator<GridNode> i = path.iterator();
        GridNode a = i.next();
        GeneratedSpeech lastSpeech = null;
        while (i.hasNext()) {
            GridNode b = i.next();
            GeneratedSpeech generatedSpeech = i.hasNext() ? speechGenerator.getSpeechForNodes(a.getLocation(), b) : null;
            final double dis = this.distanceBetweenNodes(a, b);
            if (lastSpeech != null &&
                    generatedSpeech !=null &&
                    lastSpeech.getEvent() == generatedSpeech.getEvent() &&
                    lastSpeech.getDirection() == generatedSpeech.getDirection() &&
                    dis < 10.0f) {
                final GridNode newNode = a.getLocation().getX() < 15 ?
                        this.gridNode1(build) :
                        this.gridNode24(build);
                a = null;
                b = newNode;
            }
            if (a != null) {
                res.add(a);
            }
            a = b;
            lastSpeech = generatedSpeech;
        }
        res.add(a);
        return res;
    }

    private GridNode gridNode1(final Building b) {
        return b.getSearchSpace()
                .get(3)
                .get(40)
                .get(1);
    }

    private GridNode gridNode24(final Building b) {
        return b.getSearchSpace()
                .get(3)
                .get(40)
                .get(24);
    }

    /** ------------- DEBUG METHODS ------------- */
    private GridNode debugLocationGet(final Building building) {
        return building.getSearchSpace()
                .get(3)
                .get(40)
                .get(9);

    }
}
