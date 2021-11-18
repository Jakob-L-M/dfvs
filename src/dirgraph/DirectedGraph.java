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

    public void cleanGraph() {
        cleaningChains();
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

    public boolean removeEdge(Integer preID, Integer postID) {
        if (nodeMap.containsKey(preID) && nodeMap.containsKey(postID)) {
            nodeMap.get(preID).removePostNode(postID);
            nodeMap.get(postID).removePreNode(preID);
            return true;
        }
        return false;
    }

    public boolean removeNode(Integer nodeID) {
        if (!nodeMap.containsKey(nodeID)) return false;
        DirectedNode node = nodeMap.get(nodeID);

        for (Integer postID : node.getPostNodes()) {
            nodeMap.get(postID).removePreNode(nodeID);
        }
        for (Integer preNodes: node.getPreNodes()) {
            nodeMap.get(preNodes).removePostNode(nodeID);
        }
        nodeMap.remove(nodeID);
        return true;
    }

    public Set<DirectedNode> removeClean(Integer nodeID) {
        Set<DirectedNode> removedNodes = new HashSet<>();
        removedNodes.add(nodeMap.get(nodeID));
        removeNode(nodeID);
        //removedNodes.addAll(cleanGraph());
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

    /**
     * Removes all Chains by adding direct edges. Therefor reducing to edge and vertex count by 1 for each Chain
     *
     * @return a chain cleaned graph
     */
    public void cleaningChains() {
        int chainNode = this.getChain();
        while (chainNode != -1) {
            // There is only one node pointing towards the chain Node
            // This also catches the case: in_degree = out_degree = 1
            if (this.nodeMap.get(chainNode).getIn_degree() == 1) {
                int preNode = this.nodeMap.get(chainNode).getPreNodes().iterator().next();
                for (Integer outNode : this.nodeMap.get(chainNode).getPostNodes()) {
                    this.addEdge(preNode, outNode);
                }
            }
            // If in_degree > 1 or 0 and out_degree = 1
            else {
                int postNode = this.nodeMap.get(chainNode).getPostNodes().iterator().next();
                for (Integer inNode : this.nodeMap.get(chainNode).getPreNodes()) {
                    this.addEdge(inNode, postNode);
                }
            }
            // Remove the node from the copy
            this.removeNode(chainNode);

            // Call method again to check if all bridges are now gone
            chainNode = this.getChain();
        }
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
                if (node.getIn_degree() != 0 && node.getOut_degree() != 0)
                return node.getNodeID();
            }
        }
        return -1;
    }

    public int size() {
        return nodeMap.size();
    }


}
