import java.util.HashSet;
import java.util.Set;

public class StackTuple {

    GraphType type;
    boolean added; //true if added false if removed

    // Node-only fields
    int nodeId;
    Set<Integer> outNodes;
    Set<Integer> inNodes;

    // Edge-only fields
    int from;
    int to;

    public StackTuple(boolean added, DirectedNode node) {
        this.added = added;
        this.type = GraphType.NODE;
        this.nodeId = node.getNodeID();
        this.outNodes = new HashSet<>(node.getOutNodes());
        this.inNodes = new HashSet<>(node.getInNodes());
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
                ", nodeId=" + nodeId +
                ", outNodes=" + outNodes +
                ", from=" + from +
                ", to=" + to +
                "}\n";
    }

    public enum GraphType {
        NODE,
        EDGE
    }
}
