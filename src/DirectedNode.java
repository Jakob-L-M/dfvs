import java.util.HashSet;
import java.util.Set;

public class DirectedNode {
    private final Integer nodeID;
    private final Set<Integer> inNodes = new HashSet<>();
    private final Set<Integer> outNodes = new HashSet<>();
    private int pedal;
    private boolean fixed;

    DirectedNode(Integer nodeID) {
        this.nodeID = nodeID;
        this.fixed = false;
        this.pedal = 0;
    }

    public boolean addInNode(Integer pre) {
        return inNodes.add(pre);
    }

    public boolean addOutNode(Integer post) {
        return outNodes.add(post);
    }

    public boolean removeInNode(Integer pre) {
        return inNodes.remove(pre);
    }

    public boolean removeOutNode(Integer post) {
        return outNodes.remove(post);
    }

    public void fixNode() {
        this.fixed = true;
    }

    public void unfixNode() {
        this.fixed = false;
    }

    public boolean isFixed() {
        return fixed;
    }

    public Integer getNodeID() {
        return this.nodeID;
    }

    public boolean isChain() {
        return (outNodes.size() >= 1 && inNodes.size() == 1) || (outNodes.size() == 1 && inNodes.size() >= 1);
    }

    public boolean isSinkSource() {
        return outNodes.size() == 0 || inNodes.size() == 0;
    }

    public boolean isSelfCycle() {
        return outNodes.contains(nodeID) || inNodes.contains(nodeID);
    }

    /**
     * Check if node builds a two-Cycle with a specific other node
     *
     * @param otherNode id of the node to be checked. Use isTwoCycle if searching for a two-Cycle
     * @return whether or not the node builds a two-Cycle with the given NodeId
     */
    public boolean isTwoCycleWith(int otherNode) {
        return outNodes.contains(otherNode) && inNodes.contains(otherNode);
    }

    /**
     * Checks all neighbours for a two-Cycle
     *
     * @return id of a two-Cycle Node or -1 if node is not in any two-Cycle
     */
    public int isTwoCycle() {
        if (outNodes.size() < inNodes.size()) {
            for (Integer outNode : outNodes) {
                if (inNodes.contains(outNode)) {
                    return outNode;
                }
            }
        } else {
            for (Integer inNode : inNodes) {
                if (outNodes.contains(inNode)) {
                    return inNode;
                }
            }
        }
        return -1;
    }

    /**
     * Number of bi-directional edges is equivalent to the number of connected twoCycles
     * @return number of connected bi-directional edges
     */
    public int biDirectionalCount() {
        int biDirectionalCount = 0;
        if (outNodes.size() < inNodes.size()) {
            for (Integer outNode : outNodes) {
                if (inNodes.contains(outNode)) {
                    biDirectionalCount++;
                }
            }
        } else {
            for (Integer inNode : inNodes) {
                if (outNodes.contains(inNode)) {
                    biDirectionalCount++;
                }
            }
        }
        return biDirectionalCount;
    }

    public int getInDegree() {
        return inNodes.size();
    }

    public int getOutDegree() {
        return outNodes.size();
    }

    public Set<Integer> getOutNodes() {
        return outNodes;
    }

    public Set<Integer> getInNodes() {
        return inNodes;
    }

    public int getPedal() {
        return pedal;
    }

    public void setPedal(int pedal) {
        this.pedal = pedal;
    }

    @Override
    public String toString() {
        return "Node: " + nodeID + " - in: {" + inNodes + "}, out: {" + outNodes + "}";
    }
}


