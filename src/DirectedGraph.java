import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class DirectedGraph implements Comparable<DirectedGraph> {
    final Stack<StackTuple> stack = new Stack<>();
    Map<Integer, DirectedNode> nodeMap = new HashMap<>();
    BiMap<String, Integer> dict = HashBiMap.create();

    DirectedGraph(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String currentLine;

            // using a unique id for each node
            int count = 0;

            while ((currentLine = reader.readLine()) != null) {

                if (currentLine.contains("#") || currentLine.contains("%") || currentLine.isEmpty()) continue;
                String[] nodes = currentLine.split(" ");

                // creating a new node if its the first edge of that node
                if (!dict.containsKey(nodes[0])) dict.put(nodes[0], count++);
                if (!dict.containsKey(nodes[1])) dict.put(nodes[1], count++);

                addNode(dict.get(nodes[0]));
                addNode(dict.get(nodes[1]));

                addEdge(dict.get(nodes[0]), dict.get(nodes[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DirectedGraph(DirectedGraph that) {
        for (Integer key : that.nodeMap.keySet()) {
            DirectedNode nodeToCopy = that.nodeMap.get(key);
            this.nodeMap.put(key, nodeToCopy.clone());
        }
        this.dict = that.dict;
    }

    public DirectedGraph(BiMap<String, Integer> dict) {
        this.dict = dict;
    }

    public Set<Integer> cleanGraph(int k) {

        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        Set<Integer> deletedNodes = new HashSet<>();

        for (Integer nodeId : nodes) {

            // first we check if the node is still present
            DirectedNode node = nodeMap.get(nodeId);
            if (node == null) continue;

            // if the node is a sink or source we will remove it and recursively remove all newly
            // created sinks and sources
            if (node.isSinkSource()) {
                removeSinkSource(nodeId);
                continue;
            }

            // clean chains
            if (node.isChain()) {
                int temp = cleanChain(nodeId);
                if (temp != -1) {
                    deletedNodes.add(temp);
                }
                continue;
            }

            // clean semi pendant triangle
            Set<Integer> triangle = cleanSemiPendantTriangle(nodeId);
            if (triangle != null) {
                deletedNodes.addAll(triangle);
                continue;
            }

            // clean a flower if the leftover budget is greater than 2
            if (k - deletedNodes.size() > 2) {
                int temp = cleanPetal(nodeId, k - deletedNodes.size());
                if (temp != -1) {
                    deletedNodes.add(temp);
                }
            }

        }

        // repeat until nothing is left to be cleaned
        if (!deletedNodes.isEmpty()) deletedNodes.addAll(cleanGraph(k - deletedNodes.size()));

        return deletedNodes;
    }

    /**
     * removes a chain and checks for selfCycles. If a selfCycle is generated if is remove immediately.
     *
     * @param nodeId id of a chain node. Method requires that the nodeId has been checked to be a chain.
     *               Use .isChain()
     * @return nodeId of a removed selfCycle, -1 if no selfCycle was generated.
     */
    public int cleanChain(int nodeId) {
        int result = -1;
        DirectedNode node = nodeMap.get(nodeId);
        if (node.getInDegree() == 1 && node.getOutDegree() >= 1) {
            int preNode = node.getInNodes().iterator().next();
            for (Integer postNode : node.getOutNodes()) {
                addEdge(preNode, postNode);
            }

            // If a selfCycle is created we will remove it immediately
            if (nodeMap.get(preNode).isSelfCycle()) {
                removeNode(preNode);
                result = preNode;
            }

        }

        // If in_degree > 1 and out_degree == 1
        else {
            int postNode = node.getOutNodes().iterator().next();
            for (Integer preNode : node.getInNodes()) {
                addEdge(preNode, postNode);
            }

            // If a selfCycle is created we will remove it immediately
            if (nodeMap.get(postNode).isSelfCycle()) {
                removeNode(postNode);
                result = postNode;
            }
        }
        removeNode(nodeId);
        return result;
    }

    /**
     * Calculates the max-flow value of a node and finds all petal nodes.
     *
     * @param nodeId id of the node that should be calculated
     * @return Tuple. Tuple.value() gives the max-flow, Tuple.set() is a set of all nodes
     * present in any flower leave or the flower center itself.
     */
    public Tuple calculatePetal(int nodeId) {
        return Petal.getPetalSet(this, nodeId);
    }

    /**
     * checks if a flower is present and removes that flower. Also removes a node
     * if that node is not in any circle. The petal value of a node will only be
     * updated if a potential flower is found.
     *
     * @param nodeId id of the node that should be cleaned
     * @param k      maximum budget for cleaning
     * @return nodeId if node has been clean or -1 if not
     */
    public int cleanPetal(int nodeId, int k) {
        boolean recalculated = false;

        if (nodeMap.get(nodeId).getPedal() == 0) {

            // could have never been calculated
            calculatePetal(nodeId);
            recalculated = true;

            if (nodeMap.get(nodeId).getPedal() == 0) {
                removeNode(nodeId);
                return -1;
            }
        }
        // possible flower
        if (nodeMap.get(nodeId).getPedal() > k) {

            // only recalculate if necessary
            if (!recalculated) calculatePetal(nodeId);

            if (nodeMap.get(nodeId).getPedal() > k) {
                removeNode(nodeId);
                return nodeId;
            }
        }

        // no flower
        return -1;
    }

    /**
     * cleans a semi pendant triangle at a given node
     *
     * @param nodeId id of the node that should be cleaned
     * @return Set of cleaned nodeIds, null if no triangle was found
     */
    public Set<Integer> cleanSemiPendantTriangle(int nodeId) {
        DirectedNode node = nodeMap.get(nodeId);
        if (node.getInDegree() >= 2 && node.getOutDegree() >= 2 && (node.getInDegree() == 2 || node.getOutDegree() == 2)) {
            Iterator<Integer> outIterator = node.getOutNodes().iterator();
            int outNode1 = outIterator.next();
            int outNode2 = outIterator.next();
            if (node.isTwoCycleWith(outNode1) &&
                    node.isTwoCycleWith(outNode2) &&
                    nodeMap.get(outNode1).isTwoCycleWith(outNode2)) {
                removeNode(node.getNodeID());
                Set<Integer> result = new HashSet<>();
                result.add(outNode1);
                result.add(outNode2);
                removeNode(outNode1);
                removeNode(outNode2);
                return result;
            }
        }
        return null;
    }

    public boolean addNode(Integer nid) {
        if (!nodeMap.containsKey(nid)) {
            nodeMap.put(nid, new DirectedNode(nid));
            return true;
        }
        return false;
    }

    public boolean addEdge(Integer preID, Integer postID, boolean stack) {
        DirectedNode preNode = nodeMap.get(preID);
        DirectedNode postNode = nodeMap.get(postID);
        boolean added = preNode.addOutNode(postID) &&
                postNode.addInNode(preID);
        if (stack && added) {
            this.stack.push(new StackTuple(true, preID, postID));
        }
        return added;
    }

    public boolean addEdge(Integer preID, Integer postID) {
        return addEdge(preID, postID, true);
    }

    public boolean removeEdge(Integer preID, Integer postID, boolean stack) {
        if (nodeMap.containsKey(preID) && nodeMap.containsKey(postID)) {
            if (stack) {
                this.stack.push(new StackTuple(false, preID, postID));
            }
            nodeMap.get(preID).removeOutNode(postID);
            nodeMap.get(postID).removeInNode(preID);
            return true;
        }
        return false;
    }

    public boolean removeEdge(Integer preID, Integer postID) {
        return removeEdge(preID, postID, true);
    }

    public boolean removeNode(Integer nodeID, boolean stack) {
        if (!nodeMap.containsKey(nodeID)) return false;
        DirectedNode node = nodeMap.get(nodeID);

        if (stack) {
            // false since node was removed
            this.stack.push(new StackTuple(false, node));
        }

        for (Integer preNode : node.getInNodes()) {
            nodeMap.get(preNode).removeOutNode(nodeID);
        }
        for (Integer postNode : node.getOutNodes()) {
            nodeMap.get(postNode).removeInNode(nodeID);
        }
        nodeMap.remove(nodeID);
        return true;
    }

    public boolean removeNode(Integer nodeID) {
        return removeNode(nodeID, true);
    }

    public void addStackCheckpoint() {
        stack.push(null);
    }

    public void rebuildGraph() {
        while (!stack.isEmpty()) {
            StackTuple stackTuple = stack.pop();

            if (stackTuple == null) {
                //checkpoint reached
                return;
            }

            if (stackTuple.type == StackTuple.GraphType.NODE) {
                // node are always removed and never added

                int nodeId = stackTuple.nodeId;

                addNode(nodeId); //only added if not present

                for (Integer outNode : stackTuple.outNodes) {
                    addNode(outNode); //only added if not present
                    addEdge(nodeId, outNode, false);
                }

                for (Integer inNode : stackTuple.inNodes) {
                    addNode(inNode); //only added if not present
                    addEdge(inNode, nodeId, false);
                }
            } else if (stackTuple.type == StackTuple.GraphType.EDGE) {
                // edges are always added (chain cleaning)
                removeEdge(stackTuple.from, stackTuple.to, false);
            }
        }
        //should never reach here
        System.out.println("Stack empty");
    }

    public void removeSinkSource(Integer nodeID) {
        if (!nodeMap.containsKey(nodeID)) return;
        DirectedNode node = nodeMap.get(nodeID);

        if (node.getInDegree() > 0) {
            HashSet<Integer> inNodes = new HashSet<>(node.getInNodes());
            removeNode(nodeID);
            for (Integer inNodeId : inNodes) {
                DirectedNode inNode = nodeMap.get(inNodeId);
                if (inNode == null) continue;
                inNode.removeOutNode(nodeID);
                if (inNode.isSinkSource()) {
                    removeSinkSource(inNodeId);
                }
            }
        } else {
            HashSet<Integer> outNodes = new HashSet<>(node.getOutNodes());
            removeNode(nodeID);
            for (Integer outNodeId : outNodes) {
                DirectedNode outNode = nodeMap.get(outNodeId);
                if (outNode == null) continue;
                outNode.removeInNode(nodeID);
                if (outNode.isSinkSource()) {
                    removeSinkSource(outNodeId);
                }
            }
        }
    }

    public void clearStack() {
        stack.clear();
    }

    @Override
    public String toString() {
        StringBuilder graphString = new StringBuilder();

        for (DirectedNode node : nodeMap.values()) {
            graphString.append(node.toString()).append("\n");
        }

        return graphString.toString();
    }

    public Deque<Integer> findBestCycle() {
        HashMap<Integer, Boolean> visited = new HashMap<>();
        HashMap<Integer, Integer> parent = new HashMap<>();
        Deque<Integer> cycle = new ArrayDeque<>();
        for (Integer i : nodeMap.keySet()) {
            visited.put(i, false);
            parent.put(i, -1);
        }
        for (Integer start : nodeMap.keySet()) {
            if (visited.get(start)) continue;
            Deque<Integer> queue = new ArrayDeque<>();
            Deque<Integer> tempCycle = new ArrayDeque<>();
            queue.add(start);
            visited.put(start, true);
            while (!queue.isEmpty()) {
                Integer u = queue.pop();
                for (Integer v : nodeMap.get(u).getOutNodes()) {
                    if (!visited.get(v)) {
                        parent.put(v, u);
                        visited.put(v, true);
                        queue.add(v);
                    }
                    if (v.equals(start)) {

                        int w = u;
                        while (w != -1) {
                            tempCycle.add(w);
                            w = parent.get(w);
                        }
                    }
                    if (!tempCycle.isEmpty()) break;
                }
                if (!tempCycle.isEmpty()) break;
            }
            Deque<Integer> nodesLeft = new ArrayDeque<>();
            if (!cycle.isEmpty()) {
                for (Integer u : tempCycle) {
                    if (!nodeMap.get(u).isFixed()) nodesLeft.add(u);
                }
            } else {
                nodesLeft = tempCycle;
            }
            if (tempCycle.size() == 2) return tempCycle;
            if (cycle.isEmpty() || (cycle.size() > nodesLeft.size() && nodesLeft.size() > 1)) {
                cycle = nodesLeft;
            }
        }
        if (cycle.isEmpty()) return null;
        return cycle;
    }

    public boolean containsNode(Integer u) {
        return nodeMap.containsKey(u);
    }

    public boolean hasEdge(Integer u, Integer v) {
        return nodeMap.get(u).getOutNodes().contains(v);
    }

    public DirectedNode getNode(Integer u) {
        return nodeMap.get(u);
    }

    public int size() {
        return nodeMap.size();
    }

    public String hash() {
        StringBuilder hash = new StringBuilder();
        for (Integer nodeId : nodeMap.keySet()) {
            hash.append(nodeId).append(';');
            for (Integer outNode : nodeMap.get(nodeId).getOutNodes()) {
                hash.append(outNode).append(',');
            }
            hash.append(':');
        }
        return hash.toString();
    }



    @Override
    public int compareTo(DirectedGraph o) {
        return Integer.compare(this.size(), o.size());
    }
}