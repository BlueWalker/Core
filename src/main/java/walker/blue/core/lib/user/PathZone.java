package walker.blue.core.lib.user;

import java.util.HashSet;
import java.util.Set;

import walker.blue.path.lib.node.RectCoordinates;

/**
 * Class that represents a zone around two points in the path
 */
public class PathZone {

    /**
     * Allowed delta when comparing doubles
     */
    private static final double DELTA = .0001;

    /**
     * Set containing the different pairs of vertexes
     */
    private Set<VertexPair> pairSet;

    /**
     * Cosntructor. Builds the zone around the given start and end zone with the given offset
     *
     * @param startNode The starting node that the zone will be built around
     * @param endNode The ending node that the zone will be built around
     * @param offset The offset for the zone and the destination
     */
    public PathZone(final RectCoordinates startNode,
                    final RectCoordinates endNode,
                    final double offset) {
        this(startNode, endNode, offset, offset);
    }

    /**
     * Contructor. Builds the zone around the given start and end zone with the given offsets
     *
     * @param startNode The starting node that the zone will be built around
     * @param endNode The ending node that the zone will be built around
     * @param horizontalZoneOffset The offest being used for the zone
     * @param verticalZoneOffset The offset being used for the destination
     */
    public PathZone(final RectCoordinates startNode,
                    final RectCoordinates endNode,
                    final double horizontalZoneOffset,
                    final double verticalZoneOffset) {
        final LineVector nodeLine =
                new LineVector(endNode.getX() - startNode.getX(), endNode.getY() - startNode.getY()).getNormalized();
        final LineVector perpNodeLine = nodeLine.getPerpendicular();

        final Vertex a =
                this.makeVertex(startNode, nodeLine, horizontalZoneOffset, perpNodeLine, verticalZoneOffset, -1, 1);
        final Vertex b =
                this.makeVertex(startNode, nodeLine, horizontalZoneOffset, perpNodeLine, verticalZoneOffset, -1, -1);
        final Vertex c =
                this.makeVertex(endNode, nodeLine, horizontalZoneOffset, perpNodeLine, verticalZoneOffset, 1, 1);
        final Vertex d =
                this.makeVertex(endNode, nodeLine, horizontalZoneOffset, perpNodeLine, verticalZoneOffset, 1,-1);

        this.pairSet = new HashSet<>();
        this.pairSet.add(new VertexPair(a, b));
        this.pairSet.add(new VertexPair(a, c));
        this.pairSet.add(new VertexPair(c, d));
        this.pairSet.add(new VertexPair(b, d));
    }

    /**
     * Checks whether the given RectCoordinates is inside the zone
     *
     * @param point RectCoordinates
     * @return boolean indicating whether the given RectCoordinates is inside
     *         the zone
     */
    public boolean isPointInside(final RectCoordinates point) {
        return this.isPointInside(point.getX(), point.getY());
    }

    /**
     * Checks whether the point at the given coordinates is inside the zone
     *
     * @param x x value of the point
     * @param y y value of the point
     * @return boolean indicating whether the point at the given coordinates
     *         is inside the zone
     */
    public boolean isPointInside(final double x, final double y) {
        double totalAngle = 0;
        for(final VertexPair pair : this.pairSet) {
            final LineVector a = new LineVector(pair.a.x - x, pair.a.y - y).getNormalized();
            final LineVector b = new LineVector(pair.b.x - x, pair.b.y - y).getNormalized();
            totalAngle += Math.acos(a.dot(b));
        }
        return Math.abs(totalAngle - (2 * Math.PI)) <= DELTA;
    }

    /**
     * Makes a vertex using the given parameters
     *
     * @param node node where the vertex is being built
     * @param hVector Horizontal vector being applied to the node
     * @param hOffset Horizontal offset
     * @param vVector Vertical vector being applied to the node
     * @param vOffset Vertical offset
     * @param hMod Modifier for the horizontal vector (-1 or 1)
     * @param vMod Modifier for the vertical vector (-1 or 1)
     * @return Vertex built
     */
    private Vertex makeVertex(final RectCoordinates node,
                              final LineVector hVector,
                              final double hOffset,
                              final LineVector vVector,
                              final double vOffset,
                              final int hMod,
                              final int vMod) {
        final double xVal = node.getX()
                + (hMod * hOffset * hVector.x)
                + (vMod * vOffset * vVector.x);
        final double yVal = node.getY()
                + (hMod * hOffset * hVector.y)
                + (vMod * vOffset * vVector.y);
        return new Vertex(xVal, yVal);
    }

    /**
     * Class used to define a vector
     */
    private class LineVector {

        double x;
        double y;
        double length;

        LineVector(final double x, final double y) {
            this.x = x;
            this.y = y;
            this.length = Math.sqrt((x * x) + (y * y));
        }

        LineVector getNormalized() {
            return new LineVector(this.x / this.length, this.y / this.length);
        }

        LineVector getPerpendicular() {
            return new LineVector(-1.0f * this.y, this.x);
        }

        double dot(final LineVector other) {
            return (this.x * other.x) + (this.y * other.y);
        }
    }

    /**
     * Class used to hold a pair of vertexes
     */
    private class VertexPair {
        private Vertex a;
        private Vertex b;

        VertexPair(final Vertex a, final Vertex b) {
            this.a = a;
            this.b = b;
        }
    }

    /**
     * Class used to define a vertex
     */
    private class Vertex {
        double x;
        double y;

        Vertex(final double x, final double y) {
            this.x = x;
            this.y = y;
        }
    }
}
