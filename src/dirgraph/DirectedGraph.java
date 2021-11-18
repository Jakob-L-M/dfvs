package dirgraph;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class DirectedGraph {
    Map<Integer, DirectedNode> nodeMap = new HashMap<>();
    BiMap<String, Integer> dict = HashBiMap.create();

    public DirectedGraph(Collection<DirectedNode> nodes) {
        for (DirectedNode node : nodes) {
            nodeMap.put(node.getNodeID(), node);
        }
    }

    DirectedGraph(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String currentLine;

            // using a unique id for each node
            int count = 0;

            while ((currentLine = reader.readLine()) != null) {

                if (currentLine.charAt(0) == '#') continue;
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
    }

    public static int findCrossing(Set<Deque<Integer>> cycles) {
        if (cycles.isEmpty()) return -1;
        Map<Integer, Integer> occurrences = new HashMap<>();
        for (Deque<Integer> cycle : cycles) {
            for (Integer v : cycle) {
                if (occurrences.containsKey(v)) occurrences.put(v, occurrences.get(v) + 1);
                else occurrences.put(v, 1);
            }
        }
        for (Integer key : occurrences.keySet()) {
            if (occurrences.get(key).equals(Collections.max(occurrences.values()))) {
                return key;
            }
        }
        return -1;
    }

    public void fixNode(Integer nodeID) {
        DirectedNode node = nodeMap.get(nodeID);
        node.fixNode();
    }

    public boolean isFixed(Integer nodeID) {
        DirectedNode node = nodeMap.get(nodeID);

        try {
            return node.isFixed();
        } catch (NullPointerException e) {
            return true;
            //e.printStackTrace();
        }
    }

    public Stack<DirectedNode> cleanGraph() {
        Stack<DirectedNode> removedNodes = new Stack<>();
        boolean change = false;
        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        for (Integer nid : nodes) {
            DirectedNode node = nodeMap.get(nid);
            if (node.getIn_degree() == 0 || node.getOut_degree() == 0) {
                removedNodes.add(nodeMap.get(nid));
                removeNode(nid);
                change = true;
            }
        }

        Tarjan tarjan = new Tarjan(this);
        for (ArrayList<Integer> scc : tarjan.getSCCs().values()) {
            if (scc.size() == 1) {
                removeNode(scc.get(0));
                System.out.println("removed Tarjan node: " + scc.get(0));
                change = true;
            }
        }
        if (change) removedNodes.addAll(cleanGraph());
        return removedNodes;
    }

    public boolean addNode(Integer nid) {
        if (!nodeMap.containsKey(nid)) {
            nodeMap.put(nid, new DirectedNode(nid));
            return true;
        }
        return false;
    }

    public boolean addEdge(Integer preID, Integer postID) {
        DirectedNode preNode = nodeMap.get(preID);
        DirectedNode postNode = nodeMap.get(postID);
        return preNode.addPostNode(postID) &&
                postNode.addPreNode(preID);
    }

    //TODO Erst get und dann nochmal contains? -> Kürzen?
    public boolean removeEdge(Integer preID, Integer postID) {
        DirectedNode preNode = nodeMap.get(preID);
        DirectedNode postNode = nodeMap.get(postID);
        if (nodeMap.containsKey(preID) && nodeMap.containsKey(postID)) {
            preNode.removePostNode(postID);
            postNode.removePreNode(preID);
            return true;
        }
        return false;
    }

    public boolean removeNode(Integer nodeID) {
        if (!nodeMap.containsKey(nodeID)) return false;
        DirectedNode node = nodeMap.get(nodeID);
        //TODO Warum neue ArrayList? Interieren über einzelene (schon fertige) Listen schneller
        ArrayList<Integer> neighbours = new ArrayList<>();
        neighbours.addAll(node.getPostNodes());
        neighbours.addAll(node.getPreNodes());
        // TODO Object?
        for (Object neighbourID : neighbours) {
            removeEdge(nodeID, (int) neighbourID);
            removeEdge((int) neighbourID, nodeID);
        }
        nodeMap.remove(nodeID);
        return true;
    }

    public Stack<DirectedNode> removeClean(Integer nodeID) {
        Stack<DirectedNode> removedNodes = new Stack<>();
        removedNodes.add(nodeMap.get(nodeID));
        removeNode(nodeID);
        removedNodes.addAll(cleanGraph());
        return removedNodes;
    }

    public void reconstructNode(DirectedNode node) {
        nodeMap.put(node.getNodeID(), node);
        for (int preID : node.getPreNodes()) {
            addEdge(preID, node.getNodeID());
        }
        for (int postID : node.getPostNodes()) {
            addEdge(node.getNodeID(), postID);
        }
    }

    public void reconstructNodes(Stack<DirectedNode> nodes) {
        for (DirectedNode node : nodes) {
            nodeMap.put(node.getNodeID(), node);
        }
        for (DirectedNode node : nodes) {
            for (int preID : node.getPreNodes()) {
                addEdge(preID, node.getNodeID());
            }
            for (int postID : node.getPostNodes()) {
                addEdge(node.getNodeID(), postID);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder graphString = new StringBuilder();

        for (Object node : nodeMap.values()) {
            graphString.append(node.toString()).append("\n");
        }

        return graphString.toString();
    }

    public Deque<Integer> findCycle() {
        HashMap<Integer, Boolean> visited = new HashMap<>();
        for (Integer i : nodeMap.keySet()) {
            visited.put(i, false);
        }
        for (Integer start : nodeMap.keySet()) {
            if (visited.get(start)) continue;
            Deque<Integer> deque = new ArrayDeque<>();
            deque.push(start);
            while (!deque.isEmpty()) {
                int current = deque.peek();
                if (visited.get(current) != null && !visited.get(current)) {
                    visited.put(current, true);
                    DirectedNode currentNode = nodeMap.get(current);
                    if (currentNode.getOut_degree() == 0) deque.pop();
                    for (int dest : currentNode.getPostNodes()) {
                        deque.push(dest);
                        if (visited.get(dest) != null && visited.get(dest)) {
                            while (deque.peekLast() != dest) deque.pollLast();
                            deque.pop();
                            return deque;
                        }
                    }
                } else {
                    deque.pop();
                }
            }
        }
        return null;
    }

    //Set to just one cycle!
    public Set<Deque<Integer>> findCycles() {
        Set<Deque<Integer>> cycles = new HashSet<>();
        Map<Integer, Boolean> visited = new HashMap<>();

        for (Integer i : nodeMap.keySet()) {
            visited.put(i, false);
        }

        for (Integer start : nodeMap.keySet()) {
            if (visited.get(start)) continue;
            Deque<Integer> deque = new ArrayDeque<>();
            deque.push(start);
            while (!deque.isEmpty()) {
                int current = deque.peek();
                if (visited.get(current) != null && !visited.get(current)) {
                    visited.put(current, true);
                    DirectedNode currentNode = nodeMap.get(current);
                    if (currentNode.getOut_degree() == 0) deque.pop();
                    for (int dest : currentNode.getPostNodes()) {
                        deque.push(dest);
                        if (visited.get(dest) != null && visited.get(dest)) {
                            while (deque.peekLast() != dest) deque.pollLast();
                            deque.pop();
                            cycles.add(copyDeque(deque));
                            if (cycles.size() > 5) return cycles;
                        }
                    }
                } else {
                    deque.pop();
                }
            }
        }
        return cycles;
    }

    private Deque<Integer> copyDeque(Deque<Integer> deque) {
        return new ArrayDeque<>(deque);
    }

    public Deque<Integer> findBusyCycle() {
        Set<Deque<Integer>> cycles = findCycles();
        if (cycles == null) return null;
        int crossingID = findCrossing(cycles);
        Deque<Integer> smallCycle = null;
        for (Deque<Integer> cycle : cycles) {
            if (cycle.contains(crossingID) && smallCycle == null
                    || cycle.contains(crossingID) && cycle.size() < smallCycle.size()) {
                smallCycle = cycle;
            }
        }
        return smallCycle;
    }

    public int[] findSCCsTarjan() {
        int unvisited = -1;
        int nodeCount = 0;
        int compCount = 0;
        int[] ids = new int[nodeMap.size()];
        int[] lowLinks = new int[nodeMap.size()];
        int[] sccIDs = new int[nodeMap.size()];
        boolean[] stacked = new boolean[nodeMap.size()];
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < nodeMap.size(); i++) ids[i] = unvisited;
        for (int i = 0; i < nodeMap.size(); i++) {
            if (ids[i] == unvisited) {
                stack.push(i);
                stacked[i] = true;
                ids[i] = nodeCount;
                lowLinks[i] = nodeCount++;
            }
        }
        return lowLinks;
    }

    /**
     * Removes all Chains by adding direct edges. Therefor reducing to edge and vertex count by 1 for each Chain
     *
     * @return a chain cleaned graph
     */
    public DirectedGraph cleaningChains() {
        // TODO brauchen wir wirklich eine Copy
        DirectedGraph copy = new DirectedGraph(this);
        int chainNode = copy.getChain();
        while (chainNode != -1) {
            // There is only one node pointing towards the chain Node
            // This also catches the case: in_degree = out_degree = 1
            if (copy.nodeMap.get(chainNode).getIn_degree() == 1) {
                int preNode = copy.nodeMap.get(chainNode).getPreNodes().iterator().next();
                for (Integer outNode : copy.nodeMap.get(chainNode).getPostNodes()) {
                    copy.addEdge(preNode, outNode);
                }
            }
            // If in_degree > 1 and out_degree = 1
            else {
                int postNode = copy.nodeMap.get(chainNode).getPreNodes().iterator().next();
                for (Integer inNode : copy.nodeMap.get(chainNode).getPreNodes()) {
                    copy.addEdge(inNode, postNode);
                }
            }
            // Remove the node from the copy
            copy.removeNode(chainNode);

            // Call method again to check if all bridges are now gone
            chainNode = copy.getChain();
        }
        return copy;
    }

    /**
     * Iterates over all nodes and searches for a bridge
     * Compare to lecture 2 slide 7
     *
     * @return Index of bridge node or -1 if no Node was found
     */
    public int getChain() {
        for (DirectedNode node : nodeMap.values()) {
            if (node.getIn_degree() == 1 || node.getOut_degree() == 1) {
                return node.getNodeID();
            }
        }
        return -1;
    }

    public int size() {
        return nodeMap.size();
    }


}
