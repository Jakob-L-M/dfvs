import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.*;
import java.util.*;


public class DirectedGraph implements Comparable<DirectedGraph> {
    final String name;
    final Stack<StackTuple> stack = new Stack<>();
    Map<Integer, DirectedNode> nodeMap = new HashMap<>();
    BiMap<String, Integer> dict = HashBiMap.create();
    int k;
    Stack<Integer> solution = new Stack<>();
    Map<Double, Set<Integer>> invPredictions = new TreeMap<>();
    Map<Integer, Double> predictions = new HashMap<>();
    Map<Integer, Double> invInPositions = new HashMap<>();
    List<Double> inPositions = new ArrayList<>();
    Map<Integer, Double> invOutPositions = new HashMap<>();
    List<Double> outPositions = new ArrayList<>();

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
        name = fileName.substring(fileName.indexOf("instances/") + 10);
    }

    /**
     * PACE Constructor
     *
     * @param fileName Path to file
     * @param paceInst ture -> PACE Instance
     */
    DirectedGraph(String fileName, boolean paceInst) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String currentLine = reader.readLine();
            for (int i = 1; i <= Integer.parseInt(currentLine.split(" ")[0]); i++) {
                dict.put(Integer.toString(i), i);
                addNode(i);
            }
            int count = 1;
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.contains("#") || currentLine.contains("%") || currentLine.isEmpty()) continue;
                String[] nodes = currentLine.split(" ");
                for (String v : nodes) {
                    addEdge(count, Integer.parseInt(v));
                }
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        name = fileName.substring(fileName.indexOf("instances/") + 10);
    }

    public DirectedGraph() {
        name = "temp";
    }

    public Set<Integer> cleanGraph() {
        return cleanGraph(true);
    }

    public Set<Integer> cleanGraph(boolean costlyDeletions) {

        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        Set<Integer> deletedNodes = new HashSet<>();
        boolean change = false;

        for (Integer nodeId : nodes) {

            if (k < 0) {
                return null;
            }

            // first we check if the node is still present
            DirectedNode node = nodeMap.get(nodeId);
            if (node == null) continue;

            // if the node is a selfCycle we will remove it and reduce k
            if (costlyDeletions && node.isSelfCycle()) {
                removeNode(nodeId);
                this.k--;
                deletedNodes.add(nodeId);
                continue;
            }

            // if the node is a sink or source we will remove it and recursively remove all newly
            // created sinks and sources
            if (node.isSinkSource()) {
                removeSinkSource(nodeId);
                change = true;
                continue;
            }

            // clean chains
            if (costlyDeletions && node.isChain()) {
                int temp = cleanChain(nodeId);
                if (temp != -1) {
                    k--;
                    deletedNodes.add(temp);
                }
                change = true;
                continue;
            }

            // Only Bi-Directional Edges and either In- or Out Edge
            // Remove all non Bi-Directional Edges
            if (node.getInDegree() != node.getOutDegree() && Math.min(node.getInDegree(), node.getOutDegree()) == node.biDirectionalCount()) {
                Set<Integer> inNodes = new HashSet<>(node.getInNodes());
                Set<Integer> outNodes = new HashSet<>(node.getOutNodes());
                if (outNodes.size() > inNodes.size()) {
                    for (Integer outNode : outNodes) {
                        if (!inNodes.contains(outNode)) {
                            removeEdge(nodeId, outNode);
                        }

                        // Might be null if other neighbours where removed in the same step
                        if (nodeMap.get(outNode) != null && nodeMap.get(outNode).isSinkSource()) {
                            removeSinkSource(outNode);
                        }
                    }
                } else {
                    for (Integer inNode : inNodes) {
                        if (!outNodes.contains(inNode)) {
                            removeEdge(inNode, nodeId);
                        }

                        // Might be null if other neighbours where removed in the same step
                        if (nodeMap.get(inNode) != null && nodeMap.get(inNode).isSinkSource()) {
                            removeSinkSource(inNode);
                        }
                    }
                }
            }
        }

        // repeat until nothing is left to be cleaned
        if (change) {
            Set<Integer> newNodes = cleanGraph(costlyDeletions);
            if (newNodes == null) {
                return null;
            }
            deletedNodes.addAll(newNodes);
        }

        return deletedNodes;
    }

    public Set<Integer> rootClean() {
        return rootClean(null, false);
    }

    public Set<Integer> rootClean(Set<Integer> nodes, boolean quick) {

        if (nodes == null) {
            nodes = new HashSet<>(nodeMap.keySet());
        }
        Set<Integer> deletedNodes = new HashSet<>();
        boolean changed = false;

        for (Integer nodeId : nodes) {

            // first we check if the node is still present
            DirectedNode node = nodeMap.get(nodeId);
            if (node == null) continue;

            // if the node is a selfCycle we will remove it and reduce k
            if (node.isSelfCycle()) {
                removeNode(nodeId);
                deletedNodes.add(nodeId);
                changed = true;
                continue;
            }

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
                changed = true;
                continue;
            }


            // Only Bi-Directional Edges and either In- or Out Edge
            // Remove all non Bi-Directional Edges
            if (!quick && node.getInDegree() != node.getOutDegree() && Math.min(node.getInDegree(), node.getOutDegree()) == node.biDirectionalCount()) {
                Set<Integer> inNodes = new HashSet<>(node.getInNodes());
                Set<Integer> outNodes = new HashSet<>(node.getOutNodes());
                if (outNodes.size() > inNodes.size()) {
                    for (Integer outNode : outNodes) {
                        if (!inNodes.contains(outNode)) {
                            removeEdge(nodeId, outNode);
                        }

                        // Might be null if other neighbours where removed in the same step
                        if (nodeMap.get(outNode) != null && nodeMap.get(outNode).isSinkSource()) {
                            removeSinkSource(outNode);
                        }
                    }
                } else {
                    for (Integer inNode : inNodes) {
                        if (!outNodes.contains(inNode)) {
                            removeEdge(inNode, nodeId);
                        }

                        // Might be null if other neighbours where removed in the same step
                        if (nodeMap.get(inNode) != null && nodeMap.get(inNode).isSinkSource()) {
                            removeSinkSource(inNode);
                        }
                    }
                }
            }

        }
        if (quick && changed) {
            deletedNodes.addAll(rootClean(null, true));
        }

        return deletedNodes;
    }

    /**
     * Cleans all Sink/Source nodes of a given Set. Will make sure, that the nodeMap is empty if there is no cycle left.
     *
     * @param nodes Set of nodes to be checked for cleaning
     */
    public void quickClean(Set<Integer> nodes) {
        for (Integer nodeId : nodes) {

            // first we check if the node is still present
            DirectedNode node = nodeMap.get(nodeId);
            if (node == null) continue;

            // if the node is a selfCycle we will remove it and reduce k
            if (node.isSinkSource()) {
                removeSinkSource(nodeId);
            }
        }
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
     * Wrapper to calculate all petal values without limit. !WARNING! can be extremely expensive.
     * Petal-Values will be stored inside nodes
     */
    public void calculateAllPetals() {
        for (Integer nodeId : new HashSet<>(nodeMap.keySet())) {
            calculatePetal(nodeId);
        }
    }

    /**
     * Calculates the max-flow value of a node and finds all petal nodes.
     *
     * @param nodeId id of the node that should be calculated
     * @return Tuple. Tuple.value() gives the max-flow, Tuple.set() is a set of all nodes
     * present in any flower leave or the flower center itself.
     */
    public Tuple calculatePetal(int nodeId, int limit) {
        return Petal.getPetalSet(this, nodeId, limit, false);
    }

    public Tuple calculatePetal(int nodeId, int limit, boolean quick) {
        return Petal.getPetalSet(this, nodeId, limit, quick);
    }

    public Tuple calculatePetal(int nodeId) {
        return Petal.getPetalSet(this, nodeId, Integer.MAX_VALUE, false);
    }

    /**
     * checks if a flower is present and removes that flower. Also removes a node
     * if that node is not in any circle. The petal value of a node will only be
     * updated if a potential flower is found.
     *
     * @param nodeId id of the node that should be cleaned
     * @return nodeId if node has been clean or -1 if not
     */
    public int cleanPetal(int nodeId) {
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
        boolean added = preNode.addOutNode(postID) &
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
            return nodeMap.get(preID).removeOutNode(postID) & nodeMap.get(postID).removeInNode(preID);
        }
        return false;
    }

    public boolean removeEdge(Integer preID, Integer postID) {
        return removeEdge(preID, postID, true);
    }

    public boolean removeNode(Integer nodeID, boolean stack) {
        if (!nodeMap.containsKey(nodeID)) {
            return false;
        }
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

        if (predictions.containsKey(nodeID)) {
            removePrediction(nodeID);
        }
        return true;
    }

    public boolean removeAllNodes(Set<Integer> nodesToRemove) {
        boolean success = true;
        for (Integer u : nodesToRemove) {
            success = removeNode(u) && success;
        }
        return success;
    }

    public boolean hasAllNodes(Set<Integer> nodes) {
        boolean allContained;
        for (Integer u : nodes) {
            allContained = nodeMap.containsKey(u);
            if (!allContained) return false;
        }
        return true;
    }

    public List<Set<Integer>> cleanDigraphSet(List<Set<Integer>> digraphs) {
        List<Set<Integer>> cleanedDigraphs = new ArrayList<>();
        for (Set<Integer> digraph : digraphs) {
            if (digraph.size() < 3) continue;
            boolean containsAll = true;
            for (Integer nodeID : digraph) {
                if (!nodeMap.containsKey(nodeID)) {
                    containsAll = false;
                    break;
                }
            }
            if (containsAll) cleanedDigraphs.add(digraph);
        }
        return cleanedDigraphs;
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

                int nodeId = stackTuple.node.getNodeID();

                addNode(nodeId); //only added if not present

                for (Integer outNode : stackTuple.node.getOutNodes()) {
                    addNode(outNode); //only added if not present
                    addEdge(nodeId, outNode, false);
                }

                for (Integer inNode : stackTuple.node.getInNodes()) {
                    addNode(inNode); //only added if not present
                    addEdge(inNode, nodeId, false);
                }

                nodeMap.get(nodeId).setPedal(stackTuple.node.getPedal());

            } else if (stackTuple.type == StackTuple.GraphType.EDGE) {
                // edges are always added (chain cleaning)
                if (stackTuple.added) {
                    removeEdge(stackTuple.from, stackTuple.to, false);
                } else {
                    addEdge(stackTuple.from, stackTuple.to, false);
                }
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
                if (inNode != null && inNode.isSinkSource()) {
                    removeSinkSource(inNodeId);
                }
            }
        } else {
            HashSet<Integer> outNodes = new HashSet<>(node.getOutNodes());
            removeNode(nodeID);
            for (Integer outNodeId : outNodes) {
                DirectedNode outNode = nodeMap.get(outNodeId);
                if (outNode != null && outNode.isSinkSource()) {
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
        return findBestCycle(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public Deque<Integer> findBestCycle(int sameCycleCount, int limit) {
        for (Integer nodeId : nodeMap.keySet()) {
            int twoCycleNodeId = getNode(nodeId).isTwoCycle();
            if (twoCycleNodeId != -1) {
                Deque<Integer> twoCycle = new ArrayDeque<>();
                twoCycle.add(twoCycleNodeId);
                twoCycle.add(nodeId);
                return twoCycle;
            }
        }

        int foundCycles = 0;

        Map<Integer, Integer> cycleSizes = new HashMap<>();

        Set<Integer> visited = new HashSet<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Deque<Integer> cycle = new ArrayDeque<>();
        int cycleLength = Integer.MAX_VALUE;

        for (Integer start : nodeMap.keySet()) {
            if (visited.contains(start)) continue;
            Deque<Integer> queue = new ArrayDeque<>();
            Deque<Integer> tempCycle = new ArrayDeque<>();
            queue.add(start);
            visited.add(start);
            while (!queue.isEmpty()) {
                Integer currentNodeId = queue.pop();
                for (Integer outNodeId : nodeMap.get(currentNodeId).getOutNodes()) {
                    if (!visited.contains(outNodeId)) {
                        parent.put(outNodeId, currentNodeId);
                        visited.add(outNodeId);

                        // Stop searching if we surpassed the best known cycle
                        if (cycleLength < 1000 && limitParentDepth(outNodeId, start, parent, cycleLength)) {
                            queue.add(outNodeId);
                        }
                    }
                    if (outNodeId.equals(start)) {
                        int w = currentNodeId;
                        while (w != start) {
                            tempCycle.add(w);
                            w = parent.get(w);
                        }
                        tempCycle.add(start);
                    }
                    if (!tempCycle.isEmpty()) break;
                }
                if (!tempCycle.isEmpty()) break;
            }
            foundCycles++;

            int tempSize = tempCycle.size();
            // If a 3-Cycle is found we return it directly
            if (!tempCycle.isEmpty() && tempSize <= 3) {
                return tempCycle;
            }

            // Update best cycle
            if (!tempCycle.isEmpty() && (cycle.size() > tempSize || cycle.isEmpty())) {
                cycle = tempCycle;
                cycleLength = tempSize;
            }

            // limit the amount of cycles
            if (!cycleSizes.containsKey(tempSize)) {
                cycleSizes.put(tempSize, 1);
            } else {
                cycleSizes.put(tempSize, cycleSizes.get(tempSize) + 1);
            }
            // if we found the same smallest cycle size X-Times we will return the best cycle.
            if (cycleSizes.get(cycle.size()) >= sameCycleCount) {
                return cycle;
            }

            if (!cycle.isEmpty() && foundCycles >= limit) {
                return cycle;
            }

        }
        if (cycle.isEmpty()) return null;
        return cycle;
    }

    private boolean limitParentDepth(int nodeId, int start, Map<Integer, Integer> parent, int limit) {
        int c = 1;
        int cur = parent.get(nodeId);
        while (cur != start) {
            if (c >= limit) return false;
            c++;
            cur = parent.get(cur);
        }
        return true;
    }

    public Deque<Integer> findC4s() {
        Set<Integer> visited = new HashSet<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Deque<Integer> cycle = new ArrayDeque<>();
        for (Integer i : nodeMap.keySet()) {
            parent.put(i, -1);
        }
        for (Integer start : nodeMap.keySet()) {
            Deque<Integer> queue = new ArrayDeque<>();
            Deque<Integer> tempCycle = new ArrayDeque<>();
            queue.add(start);
            queue.add(-1);
            int depth = 1;
            visited.add(start);
            while (!queue.isEmpty()) {
                Integer u = queue.pop();
                System.out.println(depth);
                if (u == -1) {
                    depth++;
                    queue.add(-1);
                    if (depth > 4) break;
                    continue;
                }
                for (Integer v : nodeMap.get(u).getOutNodes()) {
                    if (!visited.contains(v)) {
                        parent.put(v, u);
                        visited.add(v);
                        queue.add(v);
                    }
                    if (v.equals(start) && depth == 4) {
                        int w = u;
                        while (w != -1) {
                            tempCycle.add(w);
                            w = parent.get(w);
                        }
                    }
                    //if (!tempCycle.isEmpty()) break;
                }
                //if (!tempCycle.isEmpty()) break;
            }
            visited = new HashSet<>();
            System.out.println(tempCycle);
            if (tempCycle.size() == 4) {
                return tempCycle;
            }
        }
        return null;
    }

    public void unfixAll() {
        for (DirectedNode node : nodeMap.values()) {
            node.unfixNode();
        }
    }

    public Set<Integer> getForbiddenNodes() {
        Set<Integer> forbidden = new HashSet<>();
        for (DirectedNode node : nodeMap.values()) {
            if (node.isFixed()) forbidden.add(node.getNodeID());
        }
        return forbidden;
    }

    public void revertSolution(int v) {
        int i = solution.pop();
        while (!solution.isEmpty() && i != v) {
            i = solution.pop();
        }
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
            hash.append(nodeMap.get(nodeId).hashCode());
        }
        return hash.toString();
    }

    /**
     * !INCORRECT!/!UNFINISHED! Method to generate an gurobi-LP File with all simple Cycles
     */
    public void createLPFile() {

        try {
            //System.out.println(name);
            BufferedWriter bw = new BufferedWriter(new FileWriter("ilps/" + name + ".lp"));
            bw.write("Minimize\n");

            StringBuilder variables = new StringBuilder();
            StringBuilder binaries = new StringBuilder();

            for (Integer nodeId : nodeMap.keySet()) {
                variables.append("x").append(nodeId).append(" + ");
                binaries.append("x").append(nodeId).append(" ");
            }
            bw.write(variables.substring(0, variables.lastIndexOf("+") - 1));

            bw.write("\nSubject To\n");

            Deque<Integer> cycle = findBestCycle();
            int counter = 0;
            //System.out.println("Graph before:");
            //System.out.println(this);
            while (cycle != null) {
                StringBuilder constraint = new StringBuilder();
                constraint.append('c').append(counter++).append(": ");
                int parent = cycle.pop();
                int first = parent;
                constraint.append('x').append(parent).append(" + ");
                for (Integer nodeId : cycle) {
                    constraint.append('x').append(nodeId).append(" + ");
                    if (!removeEdge(nodeId, parent, false)) {
                        System.out.println("NO EDGE1");
                    }
                    parent = nodeId;
                }
                if (!removeEdge(first, parent)) {
                    System.out.println("NO EDGE2" + " " + first + ", " + parent);
                }
                constraint.delete(constraint.length() - 3, constraint.length());
                constraint.append(" >= 1");
                bw.write(constraint + "\n");
                cleanGraph(false);
                cycle = findBestCycle();
            }
            bw.write("Binary\n");
            bw.write(binaries.substring(0, binaries.length() - 1));
            bw.write("\nEnd");

            bw.close();
            //System.out.println("Graph after:");
            //System.out.println(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates an gurobi-LP file based on topological orders.
     * Assumes that the graph is not empty.
     * Files is stored in ILPs/[complex(3) or synthetic(3)/name.lp].
     * Make sure the folders ILPs/complex, ILPs/synthetic, ILPs/complex3 and ILPs/synthetic3 exist.
     */
    public long createTopoLPFile(String filename, List<Set<Integer>> digraphs) {

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            bw.write("Minimize\n");

            StringBuilder variables = new StringBuilder();
            StringBuilder binaries = new StringBuilder();

            for (Integer nodeId : nodeMap.keySet()) {
                variables.append("x").append(nodeId).append(" + ");
                binaries.append("x").append(nodeId).append(" ");
            }
            bw.write(variables.substring(0, variables.lastIndexOf("+") - 1));

            bw.write("\nSubject To\n");
            int counter = 0;

            // #### DIGRAPHS ####
            if (digraphs != null && !digraphs.isEmpty()) {
                for (Set<Integer> digraph : digraphs) {
                    if (digraph.size() < 3) {
                        continue;
                    }
                    StringBuilder constraint = new StringBuilder();
                    constraint.append('c').append(counter++).append(": ");
                    for (Integer nodeId : digraph) {
                        constraint.append('x').append(nodeId).append(" + ");
                    }
                    constraint.delete(constraint.length() - 3, constraint.length() - 1);
                    constraint.append(">= ").append(digraph.size() - 1).append("\n");
                    bw.write(constraint.toString());
                }
            }

            StringBuilder bounds = new StringBuilder();

            // #### TOPOLOGICAL ORDER ####
            int n = nodeMap.size();
            for (Integer nodeId : nodeMap.keySet()) {
                for (Integer outNodeId : nodeMap.get(nodeId).getOutNodes()) {
                    String constraint = "c" + (counter++) + ": u" + nodeId + " - u" + outNodeId + " + " + n + " x" + nodeId + " >= 1\n";
                    bw.write(constraint);
                }
                bounds.append("0 <= u").append(nodeId).append(" <= ").append(n).append("\n");
            }

            // #### EDGE DISJOINT CIRCLES ####
            Deque<Integer> cycle = findBestCycle();
            long cycleTime = -System.nanoTime();
            while (cycle != null && cycleTime + System.nanoTime() < 20_000_000_000L) {
                StringBuilder constraint = new StringBuilder();
                constraint.append('c').append(counter++).append(": ");
                int parent = cycle.pop();
                int first = parent;
                constraint.append('x').append(parent).append(" + ");
                for (Integer nodeId : cycle) {
                    constraint.append('x').append(nodeId).append(" + ");
                    removeEdge(nodeId, parent, false);
                    parent = nodeId;
                }
                removeEdge(first, parent);
                constraint.delete(constraint.length() - 3, constraint.length());
                constraint.append(" >= 1");
                bw.write(constraint + "\n");
                cleanGraph(false);
                cycle = findBestCycle();
            }

            bw.write("\nBounds\n");
            bw.write(bounds.substring(0, bounds.length() - 1));
            bw.write("\nBinary\n");
            bw.write(binaries.substring(0, binaries.length() - 1));
            bw.write("\nEnd");
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    public long createTopoLPFile() {
        return createTopoLPFile("ILPs/" + name + ".lp", null);
    }

    public long createTopoLPFile(String filename) {
        return createTopoLPFile(filename, null);
    }

    public long createTopoLPFile(List<Set<Integer>> digraphs) {
        return createTopoLPFile("ILPs/" + name + ".lp", digraphs);
    }

    public List<Double> extractGraphMetaData() {

        List<Double> result = new ArrayList<>();

        int n = nodeMap.size();
        int m = 0;
        List<Integer> inDegrees = new ArrayList<>();
        List<Integer> outDegrees = new ArrayList<>();
        List<Integer> inInDegrees = new ArrayList<>();
        List<Integer> inOutDegrees = new ArrayList<>();
        List<Integer> outInDegrees = new ArrayList<>();
        List<Integer> outOutDegrees = new ArrayList<>();
        for (Integer nodeId : new HashSet<>(nodeMap.keySet())) {

            if (nodeId % n / 45 != 0) {
                continue;
            }

            DirectedNode node = nodeMap.get(nodeId);
            m += node.getOutDegree();

            inDegrees.add(node.getInDegree());
            outDegrees.add(node.getOutDegree());
            int inInNodes = 0;
            int inOutNodes = 0;
            int outOutNodes = 0;
            int outInNodes = 0;

            try {
                for (Integer inNode : node.getInNodes()) {
                    inInNodes += nodeMap.get(inNode).getInDegree();
                    inOutNodes += nodeMap.get(inNode).getOutDegree();
                }
                for (Integer outNode : node.getOutNodes()) {
                    outInNodes += nodeMap.get(outNode).getInDegree();
                    outOutNodes += nodeMap.get(outNode).getOutDegree();
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                System.out.println(node);
                throw new IndexOutOfBoundsException();
            }

            inInDegrees.add(inInNodes);
            inOutDegrees.add(inOutNodes);
            outInDegrees.add(outInNodes);
            outOutDegrees.add(outOutNodes);
        }

        Collections.sort(inDegrees);
        Collections.sort(outDegrees);
        Collections.sort(inInDegrees);
        Collections.sort(inOutDegrees);
        Collections.sort(outInDegrees);
        Collections.sort(outOutDegrees);

        result.add((double) m / n);

        int nth = inDegrees.size() / 10;
        for (int i = 0; i <= 9; i++) {
            result.add((double) inDegrees.get(i * nth) / n);
            result.add((double) outDegrees.get(i * nth) / n);
            result.add((double) inInDegrees.get(i * nth) / n);
            result.add((double) inOutDegrees.get(i * nth) / n);
            result.add((double) outInDegrees.get(i * nth) / n);
            result.add((double) outOutDegrees.get(i * nth) / n);
        }
        result.add((double) inDegrees.get(inDegrees.size() - 1) / n);
        result.add((double) outDegrees.get(inDegrees.size() - 1) / n);
        result.add((double) inInDegrees.get(inDegrees.size() - 1) / n);
        result.add((double) inOutDegrees.get(inDegrees.size() - 1) / n);
        result.add((double) outInDegrees.get(inDegrees.size() - 1) / n);
        result.add((double) outOutDegrees.get(inDegrees.size() - 1) / n);

        return result;
    }

    public void extractNodeMetaData(BufferedWriter bw) throws IOException {
        String instance = name;
        int week;
        if (instance.substring(0, instance.lastIndexOf('/')).contains("3")) {
            week = 3;
        } else {
            week = 2;
        }

        for (DirectedNode node : new HashSet<>(nodeMap.values())) {
            int id = node.getNodeID();

            List<Double> l = getNodeMetaData(id);

            bw.write(instance + "," + week + "," + id);
            for (double d : l) {
                bw.write("," + d);
            }
            bw.write("\n");

        }
    }

    /**
     * Calculates all metadata values for the given Node.
     * The parameters will be normalized by the current state of the graph
     *
     * @param nodeId id of the node that the meta should be calculated for
     * @return List of all parameters
     */
    public List<Double> getNodeMetaData(int nodeId) {

        if (inPositions.isEmpty()) {
            updatePositions();
        } else if (Math.random() > 0.8) {
            updatePositions();
        }

        List<Double> res = new ArrayList<>();

        int n = nodeMap.size();

        DirectedNode node = nodeMap.get(nodeId);
        double inDegree = utils.round((double) node.getInDegree() / n, 6);
        double outDegree = utils.round((double) node.getOutDegree() / n, 6);

        res.add(inDegree);
        res.add(outDegree);

        double maxInOut = Math.max(inDegree, outDegree);
        double minInOut = Math.min(inDegree, outDegree);

        res.add(maxInOut);
        res.add(minInOut);

        int inInNodes = 0;
        int inOutNodes = 0;
        for (Integer inNode : node.getInNodes()) {
            inInNodes += nodeMap.get(inNode).getInDegree();
            inOutNodes += nodeMap.get(inNode).getOutDegree();
        }
        double inInDegree = utils.round((double) inInNodes / n, 6);
        double inOutDegree = utils.round((double) inOutNodes / n, 6);
        res.add(inInDegree);
        res.add(inOutDegree);

        int outOutNodes = 0;
        int outInNodes = 0;
        for (Integer outNode : node.getOutNodes()) {
            outOutNodes += nodeMap.get(outNode).getOutDegree();
            outInNodes += nodeMap.get(outNode).getInDegree();
        }
        double outOutDegree = utils.round((double) outOutNodes / n, 6);
        double outInDegree = utils.round((double) outInNodes / n, 6);
        res.add(outOutDegree);
        res.add(outInDegree);

        int biDirectionalEdges = node.biDirectionalCount();

        int nonBiInNodes = node.getInDegree() - biDirectionalEdges;
        int nonBiOutNodes = node.getOutDegree() - biDirectionalEdges;

        double biDirectionalDegree = utils.round((double) biDirectionalEdges / Math.min(node.getInDegree(), node.getOutDegree()), 6);
        double noBiInDegree = utils.round((double) nonBiInNodes / Math.min(node.getInDegree(), node.getOutDegree()), 6);
        double noBiOutDegree = utils.round((double) nonBiOutNodes / Math.min(node.getInDegree(), node.getOutDegree()), 6);

        res.add(biDirectionalDegree);
        res.add(noBiInDegree);
        res.add(noBiOutDegree);

        res.add(utils.round((double) calculatePetal(nodeId, 3).getValue() / n, 6));

        res.add(getRelativeInRank(nodeId));
        res.add(getRelativeOutRank(nodeId));

        return res;
    }

    private void updatePositions() {
        inPositions.clear();
        outPositions.clear();
        invInPositions.clear();
        invOutPositions.clear();

        int n = nodeMap.size();
        for (DirectedNode node : nodeMap.values()) {
            double inDeg = utils.round((double) node.getInDegree() / n, 5);
            double outDeg = utils.round((double) node.getOutDegree() / n, 5);

            inPositions.add(inDeg);
            invInPositions.put(node.getNodeID(), inDeg);

            outPositions.add(outDeg);
            invOutPositions.put(node.getNodeID(), outDeg);
        }
    }

    private double getRelativeInRank(int nodeId) {
        int index = inPositions.indexOf(invInPositions.get(nodeId));
        return utils.round((double) index / inPositions.size(), 6);
    }

    private double getRelativeOutRank(int nodeId) {
        int index = outPositions.indexOf(invOutPositions.get(nodeId));
        return utils.round((double) index / outPositions.size(), 6);
    }

    /**
     * Will modify the graph's prediction map. After the prediction map is cleaned there will be batchSize - |mapSize|
     * new Nodes added to the prediction map. Predictions are made using the given model. The methods will calculate
     * each node's meta-data in order to make the prediction.
     *
     * @param batchSize Size to which the prediction map should be filled
     * @param model     Neural Network that will calculate predictions based on meta-data
     */
    public void createPredictionsForBatch(int batchSize, Model model) {
        // Cleaning predictions
        for (Integer nodePd : new HashSet<>(predictions.keySet())) {
            if (nodeMap.containsKey(nodePd)) {
                continue;
            }
            removePrediction(nodePd);
        }
        // End cleaning predictions

        Set<Integer> idsToCalculate;
        if (predictions.size() + batchSize >= nodeMap.size()) {
            idsToCalculate = new HashSet<>(nodeMap.keySet());
        } else {
            idsToCalculate = new HashSet<>();
            List<Integer> ids = new ArrayList<>(nodeMap.keySet());
            Collections.shuffle(ids);
            for (int i = 0; i < ids.size() && idsToCalculate.size() < predictions.size() + batchSize; i++) {
                int cur = ids.get(i);
                if (!predictions.containsKey(cur)) {
                    idsToCalculate.add(cur);
                }
            }
        }

        for (Integer nodeId : new HashSet<>(idsToCalculate)) {
            if (!predictions.containsKey(nodeId)) {
                double predict = model.predict(getNodeMetaData(nodeId));
                if (invPredictions.containsKey(predict)) {
                    invPredictions.get(predict).add(nodeId);
                } else {
                    Set<Integer> l = new HashSet<>();
                    l.add(nodeId);
                    invPredictions.put(predict, l);
                }
                predictions.put(nodeId, predict);
            }
        }
    }

    /**
     * Uses the prediction map to delete all nodes greater or equal to a given limit. After all nodes are deleted the
     * graph will be cleaned.
     *
     * @param limit A Node will be deleted if its prediction is greater or equal to this value
     * @return All nodeIds that have been deleted due to predictions as well as cleaning
     */
    public Set<Integer> deleteNodesByPredictions(double limit) {
        Set<Integer> nodesToDelete = new HashSet<>();
        for (double key : invPredictions.keySet()) {
            if (key >= limit) {
                nodesToDelete.addAll(invPredictions.get(key));
            }
        }
        for (Integer nodeId : nodesToDelete) {
            removeNode(nodeId);
        }
        nodesToDelete.addAll(rootClean());
        return nodesToDelete;
    }

    private void removePrediction(int nodeId) {
        double predict = predictions.get(nodeId);
        predictions.remove(nodeId);
        invPredictions.get(predict).remove(nodeId);
    }

    @Override
    public int compareTo(DirectedGraph o) {
        return Integer.compare(this.size(), o.size());
    }
}