package Utilities;

import Graph.DirectedNode;

public class StackTuple {

    private final GraphType type;
    private final boolean added; //true if added false if removed

    // Node-only fields
    private DirectedNode node;

    // Edge-only fields
    private int from;
    private int to;

    public StackTuple(boolean added, DirectedNode node) {
        this.added = added;
        this.type = GraphType.NODE;
        this.node = node;
    }

    public StackTuple(boolean added, int from, int to) {
        this.added = added;
        this.type = GraphType.EDGE;
        this.from = from;
        this.to = to;
    }

    public GraphType getType() {
        return type;
    }

    public boolean isAdded() {
        return added;
    }

    public DirectedNode getNode() {
        return node;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    @Override
    public String toString() {
        return "{" +
                "type=" + type +
                ", from=" + from +
                ", to=" + to +
                "}\n";
    }

    public enum GraphType {
        NODE,
        EDGE
    }
}
