package walker.blue.core.lib.user;

import java.util.HashSet;
import java.util.Set;

import walker.blue.path.lib.RectCoordinates;

/**
 * Created by noname on 4/3/15.
 */
public class PathZone {

    private static final double DELTA = .0001;

    private Set<VertexPair> pairSet;

    public PathZone(final RectCoordinates startNode,
                    final RectCoordinates endNode,
                    final double offset) {
        this(startNode, endNode, offset, offset);
    }

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

        /*
        System.out.println("nl: x: " + nodeLine.x + " y: " + nodeLine.y);
        System.out.println("pnl: x: " + perpNodeLine.x + " y: " + perpNodeLine.y);
        System.out.println("x: " + a.x + " y: " + a.y);
        System.out.println("x: " + b.x + " y: " + b.y);
        System.out.println("x: " + c.x + " y: " + c.y);
        System.out.println("x: " + d.x + " y: " + d.y);

        System.out.println("["+a.x+","+b.x+","+d.x+","+c.x+","+a.x+"],");
        System.out.println("["+a.y+","+b.y+","+d.y+","+c.y+","+a.y+"],");
        */
        this.pairSet = new HashSet<>();
        this.pairSet.add(new VertexPair(a, b));
        this.pairSet.add(new VertexPair(a, c));
        this.pairSet.add(new VertexPair(c, d));
        this.pairSet.add(new VertexPair(b, d));
    }

    public boolean isPointInside(final RectCoordinates point) {
        return this.isPointInside(point.getX(), point.getY());
    }

    public boolean isPointInside(final double x, final double y) {
        double totalAngle = 0;
        for(final VertexPair pair : this.pairSet) {
            final LineVector a = new LineVector(pair.a.x - x, pair.a.y - y).getNormalized();
            final LineVector b = new LineVector(pair.b.x - x, pair.b.y - y).getNormalized();
            totalAngle += Math.acos(a.dot(b));
        }
        return Math.abs(totalAngle - (2 * Math.PI)) < DELTA;
    }

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

    private class VertexPair {
        private Vertex a;
        private Vertex b;

        VertexPair(final Vertex a, final Vertex b) {
            this.a = a;
            this.b = b;
        }
    }

    private class Vertex {
        double x;
        double y;

        Vertex(final double x, final double y) {
            this.x = x;
            this.y = y;
        }
    }
}
