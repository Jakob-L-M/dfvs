package dirgraph;

import java.util.HashSet;
import java.util.Set;

public class DirectedNode {
    private final Integer nodeID;
    private Set<Integer> preNodes = new HashSet<>();
    private Set<Integer> postNodes = new HashSet<>();
    private boolean fixed;
    private int in_degree;
    private int out_degree;

    DirectedNode(Integer nodeID) {
        this.nodeID = nodeID;
        this.fixed = false;
        this.in_degree = 0;
        this.out_degree = 0;
    }

    public DirectedNode(DirectedNode that) {
        this.nodeID = that.getNodeID();
        this.fixed = false;
        this.in_degree = 0;
        this.out_degree = 0;
        this.preNodes.addAll(that.getPreNodes());
        this.postNodes.addAll(that.getPostNodes());
    }

    public boolean addPreNode(Integer pre) {
        if(preNodes.add(pre)) {
            in_degree++;
            return true;
        }
        return false;
    }

    public boolean addPostNode(Integer post) {
        if(postNodes.add(post)) {
            out_degree++;
            return true;
        }
        return false;
    }

    public boolean removePreNode(Integer pre) {
        if(preNodes.remove(pre)) {
            in_degree--;
            return true;
        }
        return false;
    }

    public boolean removePostNode(Integer post) {
        if(postNodes.remove(post)) {
            out_degree--;
            return true;
        }
        return false;
    }

    public void fixNode() {
        this.fixed = true;
    }

    public boolean isFixed() {
        return fixed;
    }

    public Integer getNodeID() {
        return this.nodeID;
    }

    public int getIn_degree() {
        return in_degree;
    }

    public int getOut_degree() {
        return out_degree;
    }

    public Set<Integer> getPostNodes() {
        return postNodes;
    }

    public Set<Integer> getPreNodes() {
        return preNodes;
    }
    @Override
    public DirectedNode clone() {
        DirectedNode nodeCopy = new DirectedNode(this.nodeID);
        for(int postNode : this.postNodes) {
            nodeCopy.addPostNode(postNode);
        }
        for(int preNode : this.preNodes) {
            nodeCopy.addPreNode(preNode);
        }
        return nodeCopy;
    }

    @Override
    public String toString() {
        return "Node: " + nodeID + " - in: {" + preNodes.toString() + "}, out: {" + postNodes.toString() +"}";
    }
}


