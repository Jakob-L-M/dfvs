import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class DirectedGraph implements Comparable<DirectedGraph> {
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
    }

    public DirectedGraph() {

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

    public Set<Integer> cleanGraph() {
        cleanSinksSources();
        cleanChains();
        return cleanSelfCircles();
    }

    public void cleanSinksSources() {
        boolean change = false;
        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        for (Integer nid : nodes) {
            DirectedNode node = nodeMap.get(nid);
            if (node.getIn_degree() == 0 || node.getOut_degree() == 0) {
                removeNode(nid);
                change = true;
            }
        }
        if (change) cleanSinksSources();
    }

    public void cleanChains() {
        int chainNode = findChain();
        while (chainNode != -1) {
            DirectedNode node = nodeMap.get(chainNode);

            if (node.getIn_degree() == 1 && node.getOut_degree() >= 1) {
                int preNode = node.getPreNodes().iterator().next();
                for (Integer postNode : node.getPostNodes()) {
                    addEdge(preNode, postNode);
                }
            }
            // If in_degree > 1 and out_degree == 1
            else {
                int postNode = node.getPostNodes().iterator().next();
                for (Integer preNode : node.getPreNodes()) {
                    addEdge(preNode, postNode);
                }
            }
            removeNode(chainNode);
            chainNode = findChain();
        }
    }

    public int findChain() {
        for (DirectedNode node : nodeMap.values()) {
            if (node.getIn_degree() == 1 && node.getOut_degree() >= 1) {
                if (!node.getPreNodes().iterator().next().equals(node.getNodeID())) {
                    return node.getNodeID();
                }
            }
            if (node.getOut_degree() == 1 && node.getIn_degree() >= 1) {
                if (!node.getPostNodes().iterator().next().equals(node.getNodeID())) {
                    return node.getNodeID();
                }

            }
        }
        return -1;
    }

    public Set<Integer> cleanSelfCircles() {
        Set<Integer> nodeToDelete = new HashSet<>();
        for (DirectedNode node : nodeMap.values()) {
            if (node.getPreNodes().contains(node.getNodeID())) {
                nodeToDelete.add(node.getNodeID());
            }
        }
        for (Integer nodeId : nodeToDelete) {
            removeNode(nodeId);
        }
        return nodeToDelete;
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

        for (Integer preNode : node.getPreNodes()) {
            nodeMap.get(preNode).removePostNode(nodeID);
        }
        for (Integer postNode : node.getPostNodes()) {
            nodeMap.get(postNode).removePreNode(nodeID);
        }
        nodeMap.remove(nodeID);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder graphString = new StringBuilder();

        for (DirectedNode node : nodeMap.values()) {
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

    public boolean containsNode(Integer u) {
        return nodeMap.containsKey(u);
    }

    public DirectedNode getNode(Integer u) {
        return nodeMap.get(u);
    }

    public int size() {
        return nodeMap.size();
    }

    public Set<Set<Integer>> findDoubleLinks() {
        Set<Set<Integer>> out = new HashSet();
        for (Integer node : nodeMap.keySet()) {
            if (!getNode(node).getPostNodes().contains(node)) {
                for (Integer succ : getNode(node).getPostNodes()) {
                    if (getNode(succ).getPostNodes().contains(node)) {
                        Set<Integer> tmp = new HashSet();
                        tmp.add(node);
                        tmp.add(succ);
                        out.add(tmp);
                    }
                }
            }
        }
        return out;
    }

    public Set<Set<Integer>> findTripleLinks(Set<Set<Integer>> doubles) {
        Set<Set<Integer>> out = new HashSet();
        for (Set<Integer> pair : doubles) {
            Set<Integer> temp = findAllOutNodes(pair);
            temp.retainAll(findAllInNodes(pair));
            for (Integer node : temp) {
                Set<Integer> temp2 = new HashSet(pair);
                temp2.add(node);
                out.add(temp2);
            }
        }
        return out;
    }

    public Set<Integer> findAllOutNodes(Set<Integer> nodes) {
        Set<Integer> out = new HashSet();
        for (Integer node : nodes) {
            out.addAll(getNode(node).getPostNodes());
        }
        out.removeAll(nodes);
        return out;
    }

    public Set<Integer> findAllInNodes(Set<Integer> nodes) {
        Set<Integer> out = new HashSet();
        for (Integer node : nodes) {
            out.addAll(getNode(node).getPreNodes());
        }
        out.removeAll(nodes);
        return out;
    }

    @Override
    public int compareTo(DirectedGraph o) {
        return Integer.compare(this.size(), o.size());
    }
}