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
        boolean change = false;
        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        for (Integer nid : nodes) {
            DirectedNode node = nodeMap.get(nid);
            if (node.getIn_degree() == 0 || node.getOut_degree() == 0) {
                removeNode(nid);
                change = true;
            }
        }
        if (change) cleanGraph();
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

        for(Integer preNode : node.getPreNodes()) {
            nodeMap.get(preNode).getPostNodes().remove(nodeID);
        }
        for(Integer postNode : node.getPostNodes()) {
            nodeMap.get(postNode).getPreNodes().remove(nodeID);
        }
        nodeMap.remove(nodeID);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder graphString = new StringBuilder();

        for (Object node : nodeMap.values()) {
            graphString.append(node.toString()).append("\n");
        }

        return graphString.toString();
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

}