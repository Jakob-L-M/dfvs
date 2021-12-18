import java.util.*;

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

    public void unfixNode() { this.fixed = false;}

    public boolean isFixed() {
        return fixed;
    }

    public Integer getNodeID() {
        return this.nodeID;
    }

    public boolean isChain() {return (outNodes.size() >= 1 && inNodes.size() == 1) || (outNodes.size() == 1 && inNodes.size() >= 1);}

    public boolean isSinkSource() {return outNodes.size() == 0 || inNodes.size() == 0;}

    public boolean isSelfCycle() {return outNodes.contains(nodeID) || inNodes.contains(nodeID);}

    public boolean isTwoCycleWith(int otherNode) {return outNodes.contains(otherNode) && inNodes.contains(otherNode);}

    public int isTwoCycle() {
        if (outNodes.size() < inNodes.size()) {
            for (Integer outNode : outNodes) {
                if (inNodes.contains(outNode)) {
                    return outNode;
                }
            }
        } else {
            for (Integer inNode : inNodes) {
                if(outNodes.contains(inNode)) {
                    return inNode;
                }
            }
        }
        return -1;
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

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    @Override
    public DirectedNode clone() {
        DirectedNode nodeCopy = new DirectedNode(this.nodeID);
        for(int postNode : this.outNodes) {
            nodeCopy.addOutNode(postNode);
        }
        for(int preNode : this.inNodes) {
            nodeCopy.addInNode(preNode);
        }
        return nodeCopy;
    }

    @Override
    public String toString() {
        return "Node: " + nodeID + " - in: {" + inNodes + "}, out: {" + outNodes +"}";
    }
}


