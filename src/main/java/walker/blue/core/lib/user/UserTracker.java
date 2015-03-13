package walker.blue.core.lib.user;

import walker.blue.path.lib.*;
import walker.blue.tri.lib.Trilateration;
import java.util.ArrayList;
import java.util.List;

// Tracks the user according to the path given.
public class UserTracker {
    /**
     * The current user state according to the given path.
     */
    private UserState userState;
    /**
     * The most recent path given through the updatePath method call. 
     */
    private List<GridNode> path;
    /**
     * The last node the user passed in the path.
     */
    private GridNode previousNode;
    /**
     * The next node the user needs to reach in the path.
     */
    private GridNode nextNode;
    
    /**
     * Used to provide a padded zone that allows the user to still remain on course.
     * It is used when creating the rectangular boundaries. It is also used as a radius
     * for determining if the user is within vicinity of the destination or the next
     * node in the path that is on a different floor then the previous node's floor. 
     */
    private double zoneOffset;
    
    private NodeMapper nodeMapper;
    
    public UserTracker(List<GridNode> path, double zoneOffset, NodeMapper nodeMapper) {
        setPath(path);
        this.nodeMapper = nodeMapper;
        this.zoneOffset = zoneOffset;
    }
    
    // User location is in real building coordinates, so convert the nodes' coordinates
    // to real coordinates so calculations are done on one coordinate system.
    public void updateUserState(RectCoordinates userLocation) {
        RectCoordinates realPrevNodeLoc = nodeMapper.getRealLocation(previousNode.getLocation());
        RectCoordinates realNextNodeLoc = nodeMapper.getRealLocation(nextNode.getLocation());
        
        if(userState != UserState.ARRIVED) {
            if(realNextNodeLoc.getZ() != realPrevNodeLoc.getZ()) {
                // The previous and next nodes are on different floors.
                userStateDifferentFloorNodes(userLocation, realPrevNodeLoc, realNextNodeLoc);
            }
            else {
                // If next node is the destination
                if(nextNode == path.get(path.size() - 1)) {
                    // Check if user is within circular perimeter to change their state
                    if(isDestinationReached(userLocation)) {
                        userState = UserState.ARRIVED;
                        return;
                    }
                }
                
                double slope = slope(realPrevNodeLoc, realNextNodeLoc);
                
                if(slope == 0) {
                    userStateWithZeroSlopeLine(userLocation, realPrevNodeLoc, realNextNodeLoc);
                }
                else if(slope == Double.POSITIVE_INFINITY) {
                    // Undefined slope for line between previous and next nodes.
                    userStateWithUndefinedSlopeLine(userLocation, realPrevNodeLoc, realNextNodeLoc);
                }
                else {
                    userStateWithOtherSlopeLine(userLocation, slope, realPrevNodeLoc, realNextNodeLoc);
                }
            }       
        }       
    }
    
    // Used at the beginning and also any time the user goes off-course
    public void setPath(List<GridNode> path) {
        this.path = path; // Update with a new path
        updatePathProgress(path.get(0), path.get(1));
        userState = UserState.UNINITIALIZED;
    }
    
    public UserState getUserState() {
        return userState;
    }
    
    public GridNode getPreviousNode() {
        return previousNode;
    }
    
    public GridNode getNextNode() {
        return nextNode;
    }   
    
    private void updatePathProgress(GridNode previousNode, GridNode nextNode) {
        this.previousNode = previousNode;
        this.nextNode = nextNode;
    }
    
    private boolean isDestinationReached(RectCoordinates userLocation) {
        // The destination is reached when the user comes within
        // destinationRadius of the destination node.
        RectCoordinates destinationLoc = nodeMapper.getRealLocation(path.get(path.size() - 1).getLocation());
        double xDiff = destinationLoc.getX() - userLocation.getX();
        double yDiff = destinationLoc.getY() - userLocation.getY();
        double distance = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));
        
        if(distance <= zoneOffset) {
            // User is within the radius and thus reached the destination
            return true;
        }
        
        return false;
    }
    
    private void userStateDifferentFloorNodes(RectCoordinates userLocation, RectCoordinates realPrevNodeLoc, RectCoordinates realNextNodeLoc) {
     // Used as boundaries that assume that the area the stairwell/elevator take
        // up going from one level to the next will fall in between the min and max
        // values to determine if the user is off-course while they are in the
        // transition from the previous node's floor to the next node's floor.
        double minX, minY, maxX, maxY;
        if(realPrevNodeLoc.getX() < realNextNodeLoc.getX()) {
            minX = realPrevNodeLoc.getX() - zoneOffset;
            maxX = realNextNodeLoc.getX() + zoneOffset;
        }
        else {
            minX = realNextNodeLoc.getX() - zoneOffset;
            maxX = realPrevNodeLoc.getX() + zoneOffset;
        }
        if(realPrevNodeLoc.getY() < realNextNodeLoc.getY()) {
            minY = realPrevNodeLoc.getY() - zoneOffset;
            maxY = realNextNodeLoc.getY() + zoneOffset;
        }
        else {
            minY = realNextNodeLoc.getY() - zoneOffset;
            maxY = realPrevNodeLoc.getY() + zoneOffset;
        }
        
        if(distance(userLocation, realNextNodeLoc) <= zoneOffset) {
            // User is within the radius of the next node, so update the path progress
            updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
        }
        else if(userLocation.getX() < minX || userLocation.getX() > maxX ||
                userLocation.getY() < minY || userLocation.getY() > maxY) {
            // User has not reached the next node, but has gone out of bounds from
            // the assumed rectangular boundary of the stairwell/elevator. In going
            // out of bounds, it is assumed that they are on another floor and are off-course.
            userState = UserState.OFF_COURSE;
        }
        else {
            // User is still trying to reach
            userState = UserState.REACHING_NEXT_FLOOR;
        }
    }
    
    private void userStateWithZeroSlopeLine(RectCoordinates userLocation, RectCoordinates realPrevNodeLoc, RectCoordinates realNextNodeLoc) {
        double b = realPrevNodeLoc.getY();
        double bottomParallelWarningY = b - zoneOffset;
        double bottomParallelOffCourseY = bottomParallelWarningY - zoneOffset;
        double topParallelWarningY = b + zoneOffset;
        double topParallelOffCourseY = topParallelWarningY + zoneOffset;

        double leftPerpendicularOffCourseX;
        double rightPerpendicularOffCourseX;
        
        // Checks the two different cases. The first one being if the previous
        // node is on the left and the next node is on the right. The second one
        // is when the next node is on the left and the previous node is on the right.
        if(realPrevNodeLoc.getX() < realNextNodeLoc.getX()) {
            leftPerpendicularOffCourseX = realPrevNodeLoc.getX() - zoneOffset;
            rightPerpendicularOffCourseX = realNextNodeLoc.getX() + zoneOffset;
            
            if(userLocation.getX() < realNextNodeLoc.getX() &&
                    userLocation.getX() > realPrevNodeLoc.getX() &&
                    userLocation.getY() < topParallelWarningY &&
                    userLocation.getY() > bottomParallelWarningY) {
                // User is on course
                userState = UserState.ON_COURSE;
            }
            else if(userLocation.getY() < topParallelOffCourseY &&
                    userLocation.getY() > bottomParallelOffCourseY) {
                if(userLocation.getX() > realNextNodeLoc.getX()) {
                    // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                }
                else if(userLocation.getX() < rightPerpendicularOffCourseX &&
                        userLocation.getX() > leftPerpendicularOffCourseX) {
                    // User is in the warning zone
                    // Checks that the x and y values are inside the off course boundaries
                    userState = UserState.IN_WARNING_ZONE;
                }
                else {
                    // User is off course
                    // The x value failed for the user
                    userState = UserState.OFF_COURSE;
                }
            }
            else {
                // User is off course
                // The y value failed for the user
                userState = UserState.OFF_COURSE;
            }
        }
        else if(realPrevNodeLoc.getX() > realNextNodeLoc.getX()) {
            leftPerpendicularOffCourseX = realNextNodeLoc.getX() - zoneOffset;
            rightPerpendicularOffCourseX = realPrevNodeLoc.getX() + zoneOffset;
            
            if(userLocation.getX() > realNextNodeLoc.getX() &&
                    userLocation.getX() < realPrevNodeLoc.getX() &&
                    userLocation.getY() < topParallelWarningY &&
                    userLocation.getY() > bottomParallelWarningY) {
                // User is on course
                userState = UserState.ON_COURSE;
            }
            else if(userLocation.getY() < topParallelOffCourseY &&
                    userLocation.getY() > bottomParallelOffCourseY) {
                if(userLocation.getX() < realNextNodeLoc.getX()) {
                    // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                }
                else if(userLocation.getX() < rightPerpendicularOffCourseX &&
                        userLocation.getX() > leftPerpendicularOffCourseX) {
                    // User is in the warning zone
                    // Checks that the x and y values are inside the off course boundaries
                    userState = UserState.IN_WARNING_ZONE;
                }
                else {
                    // User is off course
                    // The x value failed for the user
                    userState = UserState.OFF_COURSE;
                }
            }
            else {
                // User is off course
                // The y value failed for the user
                userState = UserState.OFF_COURSE;
            }
        }
        else {
            System.out.println("Error! The previous node and next node have the same location.");
        }
    }
    
    private void userStateWithUndefinedSlopeLine(RectCoordinates userLocation, RectCoordinates realPrevNodeLoc, RectCoordinates realNextNodeLoc) {
        // Relative to an two-dimensional top-down view of the floor
        double leftParallelWarningX = realPrevNodeLoc.getX() - zoneOffset;
        double leftParallelOffCourseX = leftParallelWarningX - zoneOffset;
        double rightParallelWarningX = realPrevNodeLoc.getX() + zoneOffset;
        double rightParallelOffCourseX = rightParallelWarningX + zoneOffset;
        
        if(realPrevNodeLoc.getY() < realNextNodeLoc.getY()) {
            double bottomPerpendicularOffCourseY = realPrevNodeLoc.getY() - zoneOffset;
            double topPerpendicularOffCourseY = realNextNodeLoc.getY() + zoneOffset;
            
            if(userLocation.getX() > leftParallelWarningX &&
                    userLocation.getX() < rightParallelWarningX &&
                    userLocation.getY() < realNextNodeLoc.getY() &&
                    userLocation.getY() > realPrevNodeLoc.getY()) {
                // User is on course
                userState = UserState.ON_COURSE;
                System.out.println("Undefined slope Next node Smaller y: User is on course");
            }
            else if(userLocation.getX() > leftParallelOffCourseX &&
                    userLocation.getX() < rightParallelOffCourseX) {
                if(userLocation.getY() >= realNextNodeLoc.getY()) {
                    // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                    System.out.println("Undefined slope Next node Smaller y: User passed the next node");
                }
                else if(userLocation.getY() > bottomPerpendicularOffCourseY &&
                        userLocation.getY() < topPerpendicularOffCourseY) {
                    // User is in the warning zone
                    // Checks that the x and y values are inside the off course boundaries
                    userState = UserState.IN_WARNING_ZONE;
                    System.out.println("Undefined slope Next node Smaller y: User is in warning zone");
                }
                else {
                    // User is off course
                    // The y value failed for the user
                    userState = UserState.OFF_COURSE;
                    System.out.println("Undefined slope Next node Smaller y: User is off course due to y");
                }
            }
            else {
                // User is off course
                // The x value failed for the user
                userState = UserState.OFF_COURSE;
                System.out.println("Undefined slope Next node Smaller y: User is off course due to x");
            }
        }
        else if(realPrevNodeLoc.getY() > realNextNodeLoc.getY()) {
            double bottomPerpendicularOffCourseY = realNextNodeLoc.getY() - zoneOffset;
            double topPerpendicularOffCourseY = realPrevNodeLoc.getY() + zoneOffset;
            
            if(userLocation.getX() < rightParallelWarningX &&
                    userLocation.getX() > leftParallelWarningX &&
                    userLocation.getY() < realPrevNodeLoc.getY() &&
                    userLocation.getY() > realNextNodeLoc.getY()) {
                // User is on course
                userState = UserState.ON_COURSE;
            }
            else if(userLocation.getX() > leftParallelOffCourseX &&
                    userLocation.getX() < rightParallelOffCourseX) {
                if(userLocation.getY() < realNextNodeLoc.getY()) {
                 // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                }
                else if(userLocation.getY() > bottomPerpendicularOffCourseY &&
                        userLocation.getY() < topPerpendicularOffCourseY) {
                    // User is in the warning zone
                    // Checks that the x and y values are inside the off course boundaries
                    userState = UserState.IN_WARNING_ZONE;
                }
                else {
                    // User is off course
                    // The y value failed for the user
                    userState = UserState.OFF_COURSE;
                }
            }
            else {
                // User is off course
                // The x value failed for the user
                userState = UserState.OFF_COURSE;
            }
        }
        else {
            System.out.println("Error! The previous node and next node have the same location.");
        }
    }
    
    private void userStateWithOtherSlopeLine(RectCoordinates userLocation, double slope, RectCoordinates realPrevNodeLoc, RectCoordinates realNextNodeLoc) {
        double c = calculateC(slope);
        double lineBetweenAAndB = slope*(userLocation.getX() - realPrevNodeLoc.getX()) + realPrevNodeLoc.getY();
        
        double bottomParallelWarningY = lineBetweenAAndB - c;
        double bottomParallelOffCourseY = bottomParallelWarningY - c;
        double topParallelWarningY = lineBetweenAAndB + c;
        double topParallelOffCourseY = topParallelWarningY + c;
        
        // There are two cases when the slope is not zero and not undefined. The perpendicular
        // lines change depending on these two cases.
        // Case 1) The previous node's y is less than the next node's y
        // Case 2) The previous node's y is greater than the next node's y
        if(realPrevNodeLoc.getY() < realNextNodeLoc.getY()) {
            double bottomPerpendicularWarningY = (1/slope)*(userLocation.getX() - realPrevNodeLoc.getX()) + realPrevNodeLoc.getY();
            double bottomPerpendicularOffCourseY = bottomPerpendicularWarningY - c;
            double topPerpendicularWarningY = (1/slope)*(userLocation.getX() - realNextNodeLoc.getX()) + realNextNodeLoc.getY();
            double topPerpendicularOffCourseY = topPerpendicularWarningY + c;
            
            if(userLocation.getY() < topParallelWarningY &&
                    userLocation.getY() > bottomParallelWarningY &&
                    userLocation.getY() < topPerpendicularWarningY &&
                    userLocation.getY() > bottomPerpendicularWarningY) {
                // User is on course
                userState = UserState.ON_COURSE;
            }
            else if(userLocation.getY() < topParallelOffCourseY &&
                    userLocation.getY() > bottomParallelOffCourseY) {
                if(userLocation.getY() > topPerpendicularWarningY) {
                    // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                }
                else if(userLocation.getY() < topPerpendicularOffCourseY &&
                        userLocation.getY() > bottomPerpendicularOffCourseY) {
                    // User is in the warning zone
                    // Checks that the user's y value is between all four off course boundary lines
                    userState = UserState.IN_WARNING_ZONE;
                }
                else {
                    // User is off course
                    // User is outside of a perpendicular off course boundary line
                    userState = UserState.OFF_COURSE;
                }
            }
            else {
                // User is off course
                // User is outside of a parallel off course boundary line
                userState = UserState.OFF_COURSE;
            }
        }
        else if(realPrevNodeLoc.getY() > realNextNodeLoc.getY()) {
            double bottomPerpendicularWarningY = (1/slope)*(userLocation.getX() - realNextNodeLoc.getX()) + realNextNodeLoc.getY();
            double bottomPerpendicularOffCourseY = bottomPerpendicularWarningY - c;
            double topPerpendicularWarningY = (1/slope)*(userLocation.getX() - realPrevNodeLoc.getX()) + realPrevNodeLoc.getY();
            double topPerpendicularOffCourseY = topPerpendicularWarningY + c;
            
            if(userLocation.getY() < topParallelWarningY &&
                    userLocation.getY() > bottomParallelWarningY &&
                    userLocation.getY() < topPerpendicularWarningY &&
                    userLocation.getY() > bottomPerpendicularWarningY) {
                // User is on course
                userState = UserState.ON_COURSE;
            }
            else if(userLocation.getY() < topParallelOffCourseY &&
                    userLocation.getY() > bottomParallelOffCourseY) {
                if(userLocation.getY() < bottomPerpendicularWarningY) {
                    // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                }
                else if(userLocation.getY() < topPerpendicularOffCourseY &&
                        userLocation.getY() > bottomPerpendicularOffCourseY) {
                    // User is in the warning zone
                    // Checks that the user's y value is between all four off course boundary lines
                    userState = UserState.IN_WARNING_ZONE;
                }
                else {
                    // User is off course
                    // User is outside of a perpendicular off course boundary line
                    userState = UserState.OFF_COURSE;
                }
            }
            else {
                // User is off course
                // User is outside of a parallel off course boundary line
                userState = UserState.OFF_COURSE;
            }
        }
    }
    
    private double slope(RectCoordinates pointA, RectCoordinates pointB) {
        double xDiff = pointB.getX() - pointA.getX();
        if(xDiff == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return (pointB.getY() - pointA.getY())/(xDiff);
    }
   
    /**
     * Calculates c used by the line calculation methods and sets c.
     * @param slope - inverse slope of line between points A and B
     * @return void
     */
    private double calculateC(double slope) {
        if(slope < 0) {
            slope = -slope;
        }
        // Angle between vertical line and line perpendicular to 
        // the slope of the line between points A and B.
        double thetaRad = Math.atan(slope);
        // The adjacent side is the zoneOffset, so zoneOffset/cos(thetaRad) will
        // give the length of the hypotenuse, which is our c value.
        return zoneOffset/Math.cos(thetaRad);
    }
    
    private double distance(RectCoordinates locA, RectCoordinates locB) {
        double xDiff = locB.getX() - locA.getX();
        double yDiff = locB.getY() - locA.getY();
        double zDiff = locB.getZ() - locA.getZ();
        
        return Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2) + Math.pow(zDiff, 2));
    }
}
