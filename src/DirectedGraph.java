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
        this.dict = that.dict;
    }

    public DirectedGraph(BiMap<String, Integer> dict) {
        this.dict = dict;
    }

    public DirectedGraph() {

    }

    public Set<Integer> cleanGraph(int k) {

        cleanSinksSources();
        Set<Integer> newDeletedNodes = cleanChains();
        newDeletedNodes.addAll(cleanSpecialTriangle());
        if (k - newDeletedNodes.size() > 2) {
            newDeletedNodes.addAll(cleanPedals(k - newDeletedNodes.size()));
        }
        Set<Integer> deletedNodes = new HashSet<>(newDeletedNodes);
        while (!newDeletedNodes.isEmpty()) {
            cleanSinksSources();
            newDeletedNodes = cleanChains();
            newDeletedNodes.addAll(cleanSpecialTriangle());
            if (k - newDeletedNodes.size() - deletedNodes.size() > 2) {
                newDeletedNodes.addAll(cleanPedals(k - newDeletedNodes.size() - deletedNodes.size()));
            }
            deletedNodes.addAll(newDeletedNodes);
        }

        return deletedNodes;
    }

    public void cleanSinksSources() {
        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        for (Integer nid : nodes) {
            DirectedNode node = nodeMap.get(nid);
            if (node == null) continue;
            if (node.isSinkSource()) {
                removeSinkSource(nid);
            }
        }
    }

    public Set<Integer> cleanChains() {
        Set<Integer> selfCycles = new HashSet<>();
        int chainNode = findChain();
        while (chainNode != -1) {
            DirectedNode node = nodeMap.get(chainNode);
            if (node.getInDegree() == 1 && node.getOutDegree() >= 1) {
                int preNode = node.getInNodes().iterator().next();
                for (Integer postNode : node.getOutNodes()) {
                    addEdge(preNode, postNode);
                }

                // If a selfCycle is created we will remove it immediately
                if (nodeMap.get(preNode).isSelfCycle()) {
                    removeNode(preNode);
                    selfCycles.add(preNode);
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
                    selfCycles.add(postNode);
                }
            }

            // Remove the chainNode and search for a new one
            removeNode(chainNode);
            chainNode = findChain();
        }
        return selfCycles;
    }

    public int findChain() {
        for (DirectedNode node : nodeMap.values()) {
            if (node.getInDegree() == 1 && node.getOutDegree() >= 1) {
                if (!node.getInNodes().iterator().next().equals(node.getNodeID())) {
                    return node.getNodeID();
                }
            }
            if (node.getOutDegree() == 1 && node.getInDegree() >= 1) {
                if (!node.getOutNodes().iterator().next().equals(node.getNodeID())) {
                    return node.getNodeID();
                }

            }
        }
        return -1;
    }

    public void calculateAllPedalValues() {
        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        for (Integer node : nodes) {
            calculatePedalValue(node);
        }
    }

    public void calculatePedalValue(int nodeId) {
        addNode(-1); //source
        addNode(-2); //sink
        for (Integer outNode : nodeMap.get(nodeId).getOutNodes()) {
            addEdge(-1, outNode);
        }
        for (Integer inNode : nodeMap.get(nodeId).getInNodes()) {
            addEdge(inNode, -2);
        }
        removeNode(nodeId);

        int pedal = Petal.getPetalSet(nodeMap, -1, -2).getValue();

        addNode(nodeId);
        for (Integer outNode : nodeMap.get(-1).getOutNodes()) {
            addEdge(nodeId, outNode);
        }
        for (Integer inNode : nodeMap.get(-2).getInNodes()) {
            addEdge(inNode, nodeId);
        }
        removeNode(-1);
        removeNode(-2);

        nodeMap.get(nodeId).setPedal(pedal);
    }

    public Set<Integer> cleanPedals(int k) {
        Set<Integer> result = new HashSet<>();
        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        for (Integer nodeId : nodes) {

            if (nodeMap.get(nodeId) == null) continue;

            if (nodeMap.get(nodeId).getPedal() == 0) {
                // could have never been calculated
                calculatePedalValue(nodeId);
                if (nodeMap.get(nodeId).getPedal() == 0) {
                    removeNode(nodeId);
                    continue;
                }
            }
            // possible flower
            if (nodeMap.get(nodeId).getPedal() > k - result.size()) {
                // only recalculate if necessary
                calculatePedalValue(nodeId);
                if (nodeMap.get(nodeId).getPedal() > k - result.size()) {
                    result.add(nodeId);
                    removeNode(nodeId);
                }
            }
        }
        return result;
    }

    public Set<Integer> cleanSpecialTriangle() {
        Set<Integer> result = new HashSet<>();
        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        for (Integer nid : nodes) {
            DirectedNode node = nodeMap.get(nid);
            if (node == null) continue;
            if (node.getInDegree() >= 2 && node.getOutDegree() >= 2 && (node.getInDegree() == 2 || node.getOutDegree() == 2)) {
                Iterator<Integer> outIterator = node.getOutNodes().iterator();
                int outNode1 = outIterator.next();
                int outNode2 = outIterator.next();
                if (node.isTwoCycleWith(outNode1) &&
                        node.isTwoCycleWith(outNode2) &&
                        nodeMap.get(outNode1).isTwoCycleWith(outNode2)) {
                    removeNode(node.getNodeID());
                    result.add(outNode1);
                    result.add(outNode2);
                    removeNode(outNode1);
                    removeNode(outNode2);
                }
            }
        }
        return result;
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

        for (Integer preNode : node.getInNodes()) {
            nodeMap.get(preNode).removePostNode(nodeID);
        }
        for (Integer postNode : node.getOutNodes()) {
            nodeMap.get(postNode).removePreNode(nodeID);
        }
        nodeMap.remove(nodeID);
        return true;
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
                inNode.removePostNode(nodeID);
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
                outNode.removePreNode(nodeID);
                if (outNode.isSinkSource()) {
                    removeSinkSource(outNodeId);
                }
            }
        }
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

    public DirectedGraph inducedSubGraph(Set<Integer> nodes) {
        BiMap<String, Integer> inverseDict = dict;
        DirectedGraph subGraph = new DirectedGraph(inverseDict);
        for (Integer n : nodes) {
            subGraph.addNode(n);
        }
        for (Integer u : nodes) {
            DirectedNode uNode = getNode(u);
            for (Integer v : uNode.getInNodes()) {
                if (subGraph.containsNode(v)) {
                    subGraph.addEdge(v, u);
                }
            }
            for (Integer v : uNode.getOutNodes()) {
                if (subGraph.containsNode(v)) {
                    subGraph.addEdge(u, v);
                }
            }
        }
        return subGraph;
    }


    @Override
    public int compareTo(DirectedGraph o) {
        return Integer.compare(this.size(), o.size());
    }

    /* OLD METHODS
    public void cleanSinksSources() {
        boolean change = false;
        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        for (Integer nid : nodes) {
            DirectedNode node = nodeMap.get(nid);
            if (node.getInDegree() == 0 || node.getOutDegree() == 0) {
                removeNode(nid);
                change = true;
            }
        }
        if (change) cleanSinksSources();
    }
     */
}