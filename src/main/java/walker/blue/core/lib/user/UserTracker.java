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
        RectCoordinates realPrevNodeLoc = nodeMapper.getRealLocation(previousNode.location());
        RectCoordinates realNextNodeLoc = nodeMapper.getRealLocation(nextNode.location());
        
        if(userState != UserState.ARRIVED) {
            if(realNextNodeLoc.z() != realPrevNodeLoc.z()) {
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
        RectCoordinates destinationLoc = nodeMapper.getRealLocation(path.get(path.size() - 1).location());
        double xDiff = destinationLoc.x() - userLocation.x();
        double yDiff = destinationLoc.y() - userLocation.y();
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
        if(realPrevNodeLoc.x() < realNextNodeLoc.x()) {
            minX = realPrevNodeLoc.x() - zoneOffset;
            maxX = realNextNodeLoc.x() + zoneOffset;
        }
        else {
            minX = realNextNodeLoc.x() - zoneOffset;
            maxX = realPrevNodeLoc.x() + zoneOffset;
        }
        if(realPrevNodeLoc.y() < realNextNodeLoc.y()) {
            minY = realPrevNodeLoc.y() - zoneOffset;
            maxY = realNextNodeLoc.y() + zoneOffset;
        }
        else {
            minY = realNextNodeLoc.y() - zoneOffset;
            maxY = realPrevNodeLoc.y() + zoneOffset;
        }
        
        if(distance(userLocation, realNextNodeLoc) <= zoneOffset) {
            // User is within the radius of the next node, so update the path progress
            updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
        }
        else if(userLocation.x() < minX || userLocation.x() > maxX ||
                userLocation.y() < minY || userLocation.y() > maxY) {
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
        double b = realPrevNodeLoc.y();
        double bottomParallelWarningY = b - zoneOffset;
        double bottomParallelOffCourseY = bottomParallelWarningY - zoneOffset;
        double topParallelWarningY = b + zoneOffset;
        double topParallelOffCourseY = topParallelWarningY + zoneOffset;

        double leftPerpendicularOffCourseX;
        double rightPerpendicularOffCourseX;
        
        // Checks the two different cases. The first one being if the previous
        // node is on the left and the next node is on the right. The second one
        // is when the next node is on the left and the previous node is on the right.
        if(realPrevNodeLoc.x() < realNextNodeLoc.x()) {
            leftPerpendicularOffCourseX = realPrevNodeLoc.x() - zoneOffset;
            rightPerpendicularOffCourseX = realNextNodeLoc.x() + zoneOffset;
            
            if(userLocation.x() < realNextNodeLoc.x() &&
                    userLocation.x() > realPrevNodeLoc.x() &&
                    userLocation.y() < topParallelWarningY &&
                    userLocation.y() > bottomParallelWarningY) {
                // User is on course
                userState = UserState.ON_COURSE;
            }
            else if(userLocation.y() < topParallelOffCourseY &&
                    userLocation.y() > bottomParallelOffCourseY) {
                if(userLocation.x() > realNextNodeLoc.x()) {
                    // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                }
                else if(userLocation.x() < rightPerpendicularOffCourseX &&
                        userLocation.x() > leftPerpendicularOffCourseX) {
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
        else if(realPrevNodeLoc.x() > realNextNodeLoc.x()) {
            leftPerpendicularOffCourseX = realNextNodeLoc.x() - zoneOffset;
            rightPerpendicularOffCourseX = realPrevNodeLoc.x() + zoneOffset;
            
            if(userLocation.x() > realNextNodeLoc.x() &&
                    userLocation.x() < realPrevNodeLoc.x() &&
                    userLocation.y() < topParallelWarningY &&
                    userLocation.y() > bottomParallelWarningY) {
                // User is on course
                userState = UserState.ON_COURSE;
            }
            else if(userLocation.y() < topParallelOffCourseY &&
                    userLocation.y() > bottomParallelOffCourseY) {
                if(userLocation.x() < realNextNodeLoc.x()) {
                    // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                }
                else if(userLocation.x() < rightPerpendicularOffCourseX &&
                        userLocation.x() > leftPerpendicularOffCourseX) {
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
        double leftParallelWarningX = realPrevNodeLoc.x() - zoneOffset;
        double leftParallelOffCourseX = leftParallelWarningX - zoneOffset;
        double rightParallelWarningX = realPrevNodeLoc.x() + zoneOffset;
        double rightParallelOffCourseX = rightParallelWarningX + zoneOffset;
        
        if(realPrevNodeLoc.y() < realNextNodeLoc.y()) {
            double bottomPerpendicularOffCourseY = realPrevNodeLoc.y() - zoneOffset;
            double topPerpendicularOffCourseY = realNextNodeLoc.y() + zoneOffset;
            
            if(userLocation.x() > leftParallelWarningX &&
                    userLocation.x() < rightParallelWarningX &&
                    userLocation.y() < realNextNodeLoc.y() &&
                    userLocation.y() > realPrevNodeLoc.y()) {
                // User is on course
                userState = UserState.ON_COURSE;
                System.out.println("Undefined slope Next node Smaller y: User is on course");
            }
            else if(userLocation.x() > leftParallelOffCourseX &&
                    userLocation.x() < rightParallelOffCourseX) {
                if(userLocation.y() >= realNextNodeLoc.y()) {
                    // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                    System.out.println("Undefined slope Next node Smaller y: User passed the next node");
                }
                else if(userLocation.y() > bottomPerpendicularOffCourseY &&
                        userLocation.y() < topPerpendicularOffCourseY) {
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
        else if(realPrevNodeLoc.y() > realNextNodeLoc.y()) {
            double bottomPerpendicularOffCourseY = realNextNodeLoc.y() - zoneOffset;
            double topPerpendicularOffCourseY = realPrevNodeLoc.y() + zoneOffset;
            
            if(userLocation.x() < rightParallelWarningX &&
                    userLocation.x() > leftParallelWarningX &&
                    userLocation.y() < realPrevNodeLoc.y() &&
                    userLocation.y() > realNextNodeLoc.y()) {
                // User is on course
                userState = UserState.ON_COURSE;
            }
            else if(userLocation.x() > leftParallelOffCourseX &&
                    userLocation.x() < rightParallelOffCourseX) {
                if(userLocation.y() < realNextNodeLoc.y()) {
                 // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                }
                else if(userLocation.y() > bottomPerpendicularOffCourseY &&
                        userLocation.y() < topPerpendicularOffCourseY) {
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
        double lineBetweenAAndB = slope*(userLocation.x() - realPrevNodeLoc.x()) + realPrevNodeLoc.y();
        
        double bottomParallelWarningY = lineBetweenAAndB - c;
        double bottomParallelOffCourseY = bottomParallelWarningY - c;
        double topParallelWarningY = lineBetweenAAndB + c;
        double topParallelOffCourseY = topParallelWarningY + c;
        
        // There are two cases when the slope is not zero and not undefined. The perpendicular
        // lines change depending on these two cases.
        // Case 1) The previous node's y is less than the next node's y
        // Case 2) The previous node's y is greater than the next node's y
        if(realPrevNodeLoc.y() < realNextNodeLoc.y()) {
            double bottomPerpendicularWarningY = (1/slope)*(userLocation.x() - realPrevNodeLoc.x()) + realPrevNodeLoc.y();
            double bottomPerpendicularOffCourseY = bottomPerpendicularWarningY - c;
            double topPerpendicularWarningY = (1/slope)*(userLocation.x() - realNextNodeLoc.x()) + realNextNodeLoc.y();
            double topPerpendicularOffCourseY = topPerpendicularWarningY + c;
            
            if(userLocation.y() < topParallelWarningY &&
                    userLocation.y() > bottomParallelWarningY &&
                    userLocation.y() < topPerpendicularWarningY &&
                    userLocation.y() > bottomPerpendicularWarningY) {
                // User is on course
                userState = UserState.ON_COURSE;
            }
            else if(userLocation.y() < topParallelOffCourseY &&
                    userLocation.y() > bottomParallelOffCourseY) {
                if(userLocation.y() > topPerpendicularWarningY) {
                    // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                }
                else if(userLocation.y() < topPerpendicularOffCourseY &&
                        userLocation.y() > bottomPerpendicularOffCourseY) {
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
        else if(realPrevNodeLoc.y() > realNextNodeLoc.y()) {
            double bottomPerpendicularWarningY = (1/slope)*(userLocation.x() - realNextNodeLoc.x()) + realNextNodeLoc.y();
            double bottomPerpendicularOffCourseY = bottomPerpendicularWarningY - c;
            double topPerpendicularWarningY = (1/slope)*(userLocation.x() - realPrevNodeLoc.x()) + realPrevNodeLoc.y();
            double topPerpendicularOffCourseY = topPerpendicularWarningY + c;
            
            if(userLocation.y() < topParallelWarningY &&
                    userLocation.y() > bottomParallelWarningY &&
                    userLocation.y() < topPerpendicularWarningY &&
                    userLocation.y() > bottomPerpendicularWarningY) {
                // User is on course
                userState = UserState.ON_COURSE;
            }
            else if(userLocation.y() < topParallelOffCourseY &&
                    userLocation.y() > bottomParallelOffCourseY) {
                if(userLocation.y() < bottomPerpendicularWarningY) {
                    // User has passed the nextNode, so update their progress
                    updatePathProgress(nextNode, path.get(path.indexOf(nextNode) + 1));
                }
                else if(userLocation.y() < topPerpendicularOffCourseY &&
                        userLocation.y() > bottomPerpendicularOffCourseY) {
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
        double xDiff = pointB.x() - pointA.x();
        if(xDiff == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return (pointB.y() - pointA.y())/(xDiff);
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
        double xDiff = locB.x() - locA.x();
        double yDiff = locB.y() - locA.y();
        double zDiff = locB.z() - locA.z();
        
        return Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2) + Math.pow(zDiff, 2));
    }
}
