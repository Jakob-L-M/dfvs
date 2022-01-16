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

            // clean semi pendant triangle
            if (costlyDeletions) {
                Set<Integer> triangle = cleanSemiPendantTriangle(nodeId);
                if (triangle != null) {
                    k = k - 2;
                    deletedNodes.addAll(triangle);
                    change = true;
                    continue;
                }
            }

            // clean a flower if the leftover budget is greater than 2
            if (costlyDeletions && k > 2) {
                int temp = cleanPetal(nodeId);
                if (temp != -1) {
                    k--;
                    deletedNodes.add(temp);
                    change = true;
                }
            }

        }

        // repeat until nothing is left to be cleaned
        if (change) {
            Set<Integer> newNodes = cleanGraph(costlyDeletions);
            if (newNodes != null) {
                deletedNodes.addAll(newNodes);
            } else {
                return null;
            }
        }

        return deletedNodes;
    }

    public Set<Integer> rootClean() {

        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        Set<Integer> deletedNodes = new HashSet<>();
        boolean change = false;

        for (Integer nodeId : nodes) {

            // first we check if the node is still present
            DirectedNode node = nodeMap.get(nodeId);
            if (node == null) continue;

            // if the node is a selfCycle we will remove it and reduce k
            if (node.isSelfCycle()) {
                removeNode(nodeId);
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
            if (node.isChain()) {
                int temp = cleanChain(nodeId);
                if (temp != -1) {
                    deletedNodes.add(temp);
                }
                change = true;
                continue;
            }

            // clean semi pendant triangle
            Set<Integer> triangle = cleanSemiPendantTriangle(nodeId);
            if (triangle != null) {
                deletedNodes.addAll(triangle);
                change = true;
            }

        }

        // repeat until nothing is left to be cleaned
        if (change) {
            Set<Integer> newNodes = rootClean();
            deletedNodes.addAll(newNodes);
        }

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
    public Tuple calculatePetal(int nodeId) {
        return Petal.getPetalSet(this, nodeId);
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
            return nodeMap.get(preID).removeOutNode(postID) && nodeMap.get(postID).removeInNode(preID);
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

    public boolean removeAllNodes(Set<Integer> nodesToRemove) {
        boolean success = true;
        for (Integer u : nodesToRemove) {
            success = success && removeNode(u);
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
        for (Integer nodeId : new HashSet<>(nodeMap.keySet())) {
            if (nodeMap.containsKey(nodeId) && !nodeMap.get(nodeId).isFixed()) {
                int twoCycleNodeId = getNode(nodeId).isTwoCycle();
                if (twoCycleNodeId != -1) {
                    /* ＞︿＜
                    if (nodeMap.get(twoCycleNodeId).isFixed()) {

                        solution.push(nodeId);
                        removeNode(nodeId);
                        this.k--;

                        Set<Integer> cleanedNodes = cleanGraph();
                        if(cleanedNodes != null && k >= 0) {
                            solution.addAll(cleanedNodes);
                        } else {
                            return null;
                        }
                        continue;
                    }

                     */
                    Deque<Integer> twoCycle = new ArrayDeque<>();
                    twoCycle.add(twoCycleNodeId);
                    twoCycle.add(nodeId);

                    return twoCycle;
                }
            }
        }
        Set<Integer> visited = new HashSet<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Deque<Integer> cycle = new ArrayDeque<>();
        for (Integer i : nodeMap.keySet()) {
            parent.put(i, -1);
        }
        for (Integer start : nodeMap.keySet()) {
            if (visited.contains(start)) continue;
            Deque<Integer> queue = new ArrayDeque<>();
            Deque<Integer> tempCycle = new ArrayDeque<>();
            queue.add(start);
            visited.add(start);
            while (!queue.isEmpty()) {
                Integer u = queue.pop();
                for (Integer v : nodeMap.get(u).getOutNodes()) {
                    if (!visited.contains(v)) {
                        parent.put(v, u);
                        visited.add(v);
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
            if (tempCycle.size() <= 3 && !tempCycle.isEmpty()) {
                return tempCycle;
            }
            if (cycle.isEmpty() || (cycle.size() > nodesLeft.size() && nodesLeft.size() > 1)) {
                cycle = nodesLeft;
            }
        }
        if (cycle.isEmpty()) return null;
        return cycle;
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
    public void createTopoLPFile(String filename, List<Set<Integer>> digraphs) {

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
            /*
            Deque<Integer> cycle = findBestCycle();
            while (cycle != null) {
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
             */
            bw.write("\nBounds\n");
            bw.write(bounds.substring(0, bounds.length() - 1));
            bw.write("\nBinary\n");
            bw.write(binaries.substring(0, binaries.length() - 1));
            bw.write("\nEnd");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createTopoLPFile() {
        createTopoLPFile("ILPs/" + name + ".lp", null);
    }

    public void createTopoLPFile(String filename) {
        createTopoLPFile(filename, null);
    }

    public void createTopoLPFile(List<Set<Integer>> digraphs) {
        createTopoLPFile("ILPs/" + name + ".lp", digraphs);
    }


    @Override
    public int compareTo(DirectedGraph o) {
        return Integer.compare(this.size(), o.size());
    }
}