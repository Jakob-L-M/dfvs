import java.util.Set;

public class StackTuple {

    GraphType type;
    boolean added; //true if added false if removed

    // Node-only fields
    DirectedNode node;

    // Edge-only fields
    int from;
    int to;

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
