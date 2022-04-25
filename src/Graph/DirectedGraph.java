package Graph;

import Utilities.Timer;
import Utilities.*;

import java.io.*;
import java.util.*;


public class DirectedGraph implements Comparable<DirectedGraph> {
    public final String name;
    final Stack<StackTuple> stack = new Stack<>();
    public Map<Integer, DirectedNode> nodeMap = new HashMap<>();
    public int k;
    Map<Integer, Double> predictions = new HashMap<>();

    /**
     * PACE Constructor
     *
     * @param fileName Path to file
     */
    public DirectedGraph(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));

            String currentLine = reader.readLine(); // First line contains metadata

            // create all nodes
            for (int i = 1; i <= Integer.parseInt(currentLine.split(" ")[0]); i++) {
                addNode(i);
            }

            int nodeId = 1;
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.contains("%")) continue;
                if (currentLine.isEmpty()) {
                    nodeId++;
                    continue;
                }
                String[] nodes = currentLine.split(" ");
                for (String v : nodes) {
                    addEdge(nodeId, Integer.parseInt(v));
                }
                nodeId++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        name = fileName.substring(fileName.indexOf("instances/instances/") + 21);
    }

    public DirectedGraph() {
        name = "temp";
    }

    public DirectedNode getNode(Integer u) {
        return nodeMap.get(u);
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
        if (!quick && changed) {
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
     * @return Utilities.Tuple. Utilities.Tuple.value() gives the max-flow, Utilities.Tuple.set() is a set of all nodes
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

    /**
     * Inserts a new node into the graph. The Node will not have any edges by default.
     *
     * @param nid The id of the node. If the given node is already present it will not be overwritten
     * @return Whether a new node has been added
     */
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
        for (Integer u : nodes) {
            if (!nodeMap.containsKey(u)) return false;
        }
        return true;
    }

    public boolean hasAllNodes(Deque<Integer> nodes) {
        for (Integer i : nodes) {
            if (!nodeMap.containsKey(i)) {
                return false;
            }
        }
        return true;
    }

    public List<Set<Integer>> cleanDigraphSet(List<Set<Integer>> digraphs) {
        List<Set<Integer>> cleanedDigraphs = new ArrayList<>();
        for (Set<Integer> digraph : digraphs) {
            if (digraph.size() >= 3 && hasAllNodes(digraph))
                cleanedDigraphs.add(digraph);
        }
        return cleanedDigraphs;
    }

    public boolean removeNode(Integer nodeID) {
        return removeNode(nodeID, true);
    }

    /**
     * Inserts a checkpoint into the stack. When rebuildGraph() is called the DirectedGraph gets rebuild until the
     * most recent checkpoint has been reached. A checkpoint is represented by a null-element.
     */
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

            if (stackTuple.getType() == StackTuple.GraphType.NODE) {
                // node are always removed and never added

                int nodeId = stackTuple.getNode().getNodeID();

                addNode(nodeId); //only added if not present

                for (Integer outNode : stackTuple.getNode().getOutNodes()) {
                    addNode(outNode); //only added if not present
                    addEdge(nodeId, outNode, false);
                }

                for (Integer inNode : stackTuple.getNode().getInNodes()) {
                    addNode(inNode); //only added if not present
                    addEdge(inNode, nodeId, false);
                }

                nodeMap.get(nodeId).setPedal(stackTuple.getNode().getPedal());

            } else if (stackTuple.getType() == StackTuple.GraphType.EDGE) {
                // edges are always added (chain cleaning)
                if (stackTuple.isAdded()) {
                    removeEdge(stackTuple.getFrom(), stackTuple.getTo(), false);
                } else {
                    addEdge(stackTuple.getFrom(), stackTuple.getTo(), false);
                }
            }
        }
        //should never reach here
        System.out.println("Stack empty");
    }

    /**
     * Will remove a sink- or source-node. Additionally, all neighbours will be checked recursively to eliminated new sinks
     * and sources immediately.
     *
     * @param nodeID The id of a know sink or source node
     */
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

    /**
     * Method will find the 'best' (shortest) cycles of a graph. To decrease computation we fist search for 2-cycles
     * since they are the shortest possible cycles, and we can search for them efficiently. If no cycle is found we will
     * use breadth-first search (BFS) to find a cycle. The additional parameters are used to stop searching when certain
     * conditions are met.
     * To ensure the methods works, it should always be called on a cleaned graph. This especially means that each node
     * has to lie in at least one cycle.
     *
     * @param sameCycleCount The amount of cycles with the same (shortest known) length to stop searching.
     * @param limit          The maximum amount of cycles to search for.
     * @return The best known cycle as a deque of nodeIds
     */
    public Deque<Integer> findBestCycle(int sameCycleCount, int limit) {

        // Checking for two-cycles
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
        int cycleLength = 0;

        for (Integer start : nodeMap.keySet()) {
            if (visited.contains(start)) continue;
            Deque<Integer> queue = new ArrayDeque<>();
            Deque<Integer> tempCycle = new ArrayDeque<>();
            queue.add(start);
            visited.add(start);
            while (!queue.isEmpty()) {
                Integer currentNodeId = queue.pop();

                // Stop searching if we surpassed the best known cycle
                if ((!currentNodeId.equals(start)) && !(cycleLength == 0 || limitParentDepth(currentNodeId, start, parent, cycleLength + 1))) {
                    continue;
                }

                for (Integer outNodeId : nodeMap.get(currentNodeId).getOutNodes()) {
                    if (!visited.contains(outNodeId)) {
                        parent.put(outNodeId, currentNodeId);
                        visited.add(outNodeId);

                        // Stop searching if we surpassed the best known cycle
                        if (cycleLength == 0 || limitParentDepth(outNodeId, start, parent, cycleLength)) {
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
            if (!cycle.isEmpty() && cycleSizes.get(cycle.size()) >= sameCycleCount) {
                return cycle;
            }

            if (!cycle.isEmpty() && foundCycles >= limit) {
                return cycle;
            }

        }
        if (cycle.isEmpty()) return null;
        return cycle;
    }

    /**
     * Method to check if BFS has reached a given depth
     *
     * @param nodeId The if of the node that is checked
     * @param start  The id of the BFS root node
     * @param parent A parent map containing the path from nodeId to start
     * @param limit  The maximum allowed path length
     * @return Whether the path is below the given limit
     */
    private boolean limitParentDepth(int nodeId, int start, Map<Integer, Integer> parent, int limit) {
        int c = 0;
        int cur = parent.get(nodeId);
        while (cur != start) {
            if (c++ >= limit) return false;
            cur = parent.get(cur);
        }
        return true;
    }

    /**
     * Main heuristic method. Follows the idea behind the heuristic flowchart.
     *
     * @param level               the depth of recursive calls. Deletion decisions are adjusted based on this depth.
     * @param levelLimit          A minimal level to be reached before we try to solve with an optimal solver
     * @param m                   A model that predicts a deletion confidence based on node parameters
     * @param percentageReduction The amount by which the deletion cutoff is lowered.
     * @return A Utilities.Timer for debugging purposes. Should be void in a production version
     */
    public TimerTuple heuristicSolution(int level, int levelLimit, Model m, double percentageReduction) {

        Utilities.Timer t = new Timer(6, new String[]{"packing", "predData", "predictions", "cleaning", "dfvsSolve", "tarjan"});

        // Cleaning and packing
        // Given huge instances, packing takes (by far) the longest.
        this.k = Integer.MAX_VALUE;
        Set<Integer> heuristic = cleanGraph();
        Packing pack = new Packing(this);
        long time = -System.nanoTime();
        // Calculate a Packing that consists of at most 5k cycles
        List<Deque<Integer>> cyclePacking = pack.newFindCyclePacking(4, 2, 5_000);
        t.addTime("packing", time + System.nanoTime());

        // main while loop. As long as a non-empty packing exists we still need to delete more nodes.
        while (!cyclePacking.isEmpty()) {
            Set<Integer> changedNodes = new HashSet<>();
            List<Deque<Integer>> nonRemovedCycles = new ArrayList<>();
            for (Deque<Integer> cyc : cyclePacking) {
                int nodeToDelete = -1;
                double prediction = -1.0;

                // a node could have been removed due to cleaning after previous deletions
                if (!hasAllNodes(cyc)) {
                    continue;
                }

                // calculation of deletion confidences for all nodes in the cycle
                for (Integer curNode : cyc) {

                    double curPrediction;

                    if (predictions.containsKey(curNode)) {
                        if (Math.random() > 0.7) {

                            time = -System.nanoTime();
                            List<Double> metaData = getNodeMetaData(curNode);
                            t.addTime("predData", time + System.nanoTime());

                            time = -System.nanoTime();
                            curPrediction = m.predict(metaData);
                            t.addTime("predictions", time + System.nanoTime());

                            predictions.put(curNode, curPrediction);
                        } else {
                            curPrediction = predictions.get(curNode);
                        }
                    } else {

                        time = -System.nanoTime();
                        List<Double> metaData = getNodeMetaData(curNode);
                        t.addTime("predData", time + System.nanoTime());

                        time = -System.nanoTime();
                        curPrediction = m.predict(metaData);
                        t.addTime("predictions", time + System.nanoTime());

                        predictions.put(curNode, curPrediction);
                    }

                    if (curPrediction > prediction) {
                        nodeToDelete = curNode;
                        prediction = curPrediction;
                    }
                }

                // If the best node satisfies the current cutoff it will be deleted.
                if (prediction > 0.6 - percentageReduction) {
                    removeNode(nodeToDelete);
                    changedNodes.add(nodeToDelete);
                }
                // To reduce computation in following levels we will remove some cycles based on there length  and some
                // randomness even if they do not satisfy the current cutoff
                else if (Math.random() < 3 * Math.exp(-0.3 * cyc.size())) {
                    removeNode(nodeToDelete);
                    changedNodes.add(nodeToDelete);
                } else {
                    nonRemovedCycles.add(cyc);
                }
            }

            heuristic.addAll(changedNodes);

            time = -System.nanoTime();
            heuristic.addAll(cleanGraph());
            t.addTime("cleaning", time + System.nanoTime());


            percentageReduction += 0.15 * (double) (nonRemovedCycles.size()) / cyclePacking.size();


            if (nodeMap.isEmpty()) {
                return new TimerTuple(t, heuristic);
            }

            if (level >= levelLimit) {
                if (this.size() > 5_000) {
                    TimerTuple res = this.heuristicSolution(level + 1, levelLimit + 2, m, percentageReduction);
                    heuristic.addAll(res.getSolution());
                    t.addTimer(res.getTimer());
                } else {

                    time = -System.nanoTime();
                    Tarjan tarjan = new Tarjan(this);
                    Set<DirectedGraph> sccs = tarjan.getSCCGraphs();
                    t.addTime("tarjan", time + System.nanoTime());

                    for (DirectedGraph scc : sccs) {
                        if (scc.size() > 0) {
                            if (scc.size() > 1000) {
                                TimerTuple res = scc.heuristicSolution(level + 1, levelLimit + 2, m, percentageReduction);
                                heuristic.addAll(res.getSolution());
                                t.addTimer(res.getTimer());
                            } else {
                                TimerTuple res = scc.heuristicSolution(level + 1, levelLimit + 1, m, percentageReduction);
                                heuristic.addAll(res.getSolution());
                                t.addTimer(res.getTimer());
                            }
                        } else {
                            System.out.println("solving correct");
                            time = -System.nanoTime();
                            List<Integer> sol = new ArrayList<>(Main.dfvsSolve(this));
                            t.addTime("dfvsSolve", time + System.nanoTime());
                            if (sol == null) {
                                TimerTuple res = scc.heuristicSolution(level++, levelLimit + 2, m, percentageReduction);
                                heuristic.addAll(res.getSolution());
                                t.addTimer(res.getTimer());
                            } else {
                                heuristic.addAll(sol);
                            }
                        }
                    }
                }
                return new TimerTuple(t, heuristic);
            }

            System.out.println("Total: " + cyclePacking.size() + " Removed: " +
                    (cyclePacking.size() - nonRemovedCycles.size()) + " Percentage: " + (0.8-percentageReduction));

            cleanGraph();

            time = -System.nanoTime();
            cyclePacking = pack.newFindCyclePacking(5, 2, 1_500);

            t.addTime("packing", time + System.nanoTime());

            level++;
        }
        return new TimerTuple(t, heuristic);
    }

    public boolean containsNode(Integer u) {
        return nodeMap.containsKey(u);
    }

    public boolean hasEdge(Integer u, Integer v) {
        return nodeMap.get(u).getOutNodes().contains(v);
    }

    public int size() {
        return nodeMap.size();
    }

    /**
     * Creates a gurobi-LP file based on topological orders.
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

    public void safeSolution(Collection<Integer> solution) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("instances/solutions/" + name));
            for (Integer node : solution) {
                bw.write(node + "\n");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        List<Integer> inTimesOut = new ArrayList<>();
        List<Integer> biDirectional = new ArrayList<>();
        List<Integer> nonBiDirectional = new ArrayList<>();

        for (Integer nodeId : new HashSet<>(nodeMap.keySet())) {

            if (nodeId % n / 50 != 0) {
                continue;
            }

            DirectedNode node = nodeMap.get(nodeId);
            m += node.getOutDegree();

            inDegrees.add(node.getInDegree());
            outDegrees.add(node.getOutDegree());
            inTimesOut.add(node.getInDegree() * node.getOutDegree());

            int inInNodes = 0;
            int inOutNodes = 0;
            int outOutNodes = 0;
            int outInNodes = 0;

            for (Integer inNode : node.getInNodes()) {
                inInNodes += nodeMap.get(inNode).getInDegree();
                inOutNodes += nodeMap.get(inNode).getOutDegree();
            }
            for (Integer outNode : node.getOutNodes()) {
                outInNodes += nodeMap.get(outNode).getInDegree();
                outOutNodes += nodeMap.get(outNode).getOutDegree();
            }

            inInDegrees.add(inInNodes);
            inOutDegrees.add(inOutNodes);
            outInDegrees.add(outInNodes);
            outOutDegrees.add(outOutNodes);
            biDirectional.add(node.biDirectionalCount());
            nonBiDirectional.add(Math.min(node.getInDegree(), node.getOutDegree()) - node.biDirectionalCount());
        }

        Collections.sort(inDegrees);
        Collections.sort(outDegrees);
        Collections.sort(inTimesOut);
        Collections.sort(inInDegrees);
        Collections.sort(inOutDegrees);
        Collections.sort(outInDegrees);
        Collections.sort(outOutDegrees);
        Collections.sort(biDirectional);
        Collections.sort(nonBiDirectional);


        result.add((double) m / n);

        for (int i : List.of(0, inDegrees.size() / 2, inDegrees.size() - 1)) {
            result.add((double) inDegrees.get(i) / n);
            result.add((double) outDegrees.get(i) / n);
            result.add((double) inTimesOut.get(i) / n);
            result.add((double) inInDegrees.get(i) / n);
            result.add((double) inOutDegrees.get(i) / n);
            result.add((double) outInDegrees.get(i) / n);
            result.add((double) outOutDegrees.get(i) / n);
            result.add((double) biDirectional.get(i) / n);
            result.add((double) nonBiDirectional.get(i) / n);
        }

        result.add((double) inDegrees.stream().mapToInt(Integer::intValue).sum() / n);
        result.add((double) outDegrees.stream().mapToInt(Integer::intValue).sum() / n);
        result.add((double) inTimesOut.stream().mapToInt(Integer::intValue).sum() / n);
        result.add((double) inInDegrees.stream().mapToInt(Integer::intValue).sum() / n);
        result.add((double) inOutDegrees.stream().mapToInt(Integer::intValue).sum() / n);
        result.add((double) outInDegrees.stream().mapToInt(Integer::intValue).sum() / n);
        result.add((double) outOutDegrees.stream().mapToInt(Integer::intValue).sum() / n);
        result.add((double) biDirectional.stream().mapToInt(Integer::intValue).sum() / n);
        result.add((double) nonBiDirectional.stream().mapToInt(Integer::intValue).sum() / n);

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

        List<Double> res = new ArrayList<>();

        int n = nodeMap.size();

        DirectedNode node = nodeMap.get(nodeId);
        double inDegree = utils.round((double) node.getInDegree() / n, 8);
        double outDegree = utils.round((double) node.getOutDegree() / n, 8);

        res.add(inDegree);
        res.add(outDegree);

        double maxInOut = Math.max(inDegree, outDegree);
        double minInOut = Math.min(inDegree, outDegree);

        res.add(maxInOut);
        res.add(minInOut);

        double inTimesOut = utils.round((double) (node.getInDegree() * node.getOutDegree()) / n, 8);

        res.add(inTimesOut);

        int inInNodes = 0;
        int inOutNodes = 0;
        for (Integer inNode : node.getInNodes()) {
            inInNodes += nodeMap.get(inNode).getInDegree();
            inOutNodes += nodeMap.get(inNode).getOutDegree();
        }
        double inInDegree = utils.round((double) inInNodes / n, 8);
        double inOutDegree = utils.round((double) inOutNodes / n, 8);
        res.add(inInDegree);
        res.add(inOutDegree);

        int outOutNodes = 0;
        int outInNodes = 0;
        for (Integer outNode : node.getOutNodes()) {
            outOutNodes += nodeMap.get(outNode).getOutDegree();
            outInNodes += nodeMap.get(outNode).getInDegree();
        }
        double outOutDegree = utils.round((double) outOutNodes / n, 8);
        double outInDegree = utils.round((double) outInNodes / n, 8);
        res.add(outOutDegree);
        res.add(outInDegree);

        int biDirectionalEdges = node.biDirectionalCount();

        int nonBiInNodes = node.getInDegree() - biDirectionalEdges;
        int nonBiOutNodes = node.getOutDegree() - biDirectionalEdges;

        double biDirectionalDegree = utils.round((double) biDirectionalEdges / Math.min(node.getInDegree(), node.getOutDegree()), 8);
        double noBiInDegree = utils.round((double) nonBiInNodes / Math.min(node.getInDegree(), node.getOutDegree()), 8);
        double noBiOutDegree = utils.round((double) nonBiOutNodes / Math.min(node.getInDegree(), node.getOutDegree()), 8);

        res.add(biDirectionalDegree);
        res.add(noBiInDegree);
        res.add(noBiOutDegree);

        return res;
    }


    @Override
    public int compareTo(DirectedGraph o) {
        return Integer.compare(this.size(), o.size());
    }

    /**
     * Saves the graph to a file in metis format. Will automatically adjust ids
     * @param name Instance name of the new file
     */
    public void saveToFile(String name) {
        // #### Reducing nodeIds to be in the interval [1, n]
        Map<Integer, Integer> idMap = new HashMap<>();
        int counter = 1;

        // filling id map. Each (unique) nodeId will be mapped to a unique position in [1,n]
        for (Integer nodeId : nodeMap.keySet()) {
            idMap.put(nodeId, counter);
            counter++;
        }
        // #### end of id adjustment

        // #### writing metis file
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("instances/instances/" + name));
            bw.write(nodeMap.keySet().size() + " " +
                    nodeMap.values().stream().mapToInt(DirectedNode::getOutDegree).sum() + " 0\n");
            for (DirectedNode node : nodeMap.values()) {
                for (Integer outNode : node.getOutNodes()) {
                    bw.write(idMap.get(outNode) + " ");
                }
                bw.write("\n");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}