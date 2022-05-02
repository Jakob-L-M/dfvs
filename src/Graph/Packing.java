package Graph;

import java.util.*;

public class Packing {
    private final DirectedGraph graph;
    private Set<Integer> safeToDeleteDigraphNodes;
    private List<Set<Integer>> digraphs;

    public Packing(DirectedGraph graph) {
        this.graph = graph;
    }


    public void safeDeletionDigraph() {

        // finds all digraphs
        if (digraphs == null) {
            getDigraphs();
        }

        this.safeToDeleteDigraphNodes = new HashSet<>();
        for (Set<Integer> digraph : digraphs) {
            int n = digraph.size();

            // Sink-Sources and SaveToDelete Two Cycles have already been cleaned.
            if (n < 2) continue;

            for (Integer i : digraph) {
                if (graph.getNode(i).getInDegree() == n - 1 || graph.getNode(i).getOutDegree() == n - 1) {
                    Set<Integer> deletionCandidates = new HashSet<>(digraph);
                    deletionCandidates.remove(i);
                    safeToDeleteDigraphNodes.addAll(deletionCandidates);
                    break;
                }
            }
        }
    }

    public Set<Integer> getSafeToDeleteDigraphNodes() {
        safeDeletionDigraph();
        return safeToDeleteDigraphNodes;
    }

    public Set<Integer> getSafeToDeleteDigraphNodes(boolean quick) {
        if (!quick) return getSafeToDeleteDigraphNodes();

        if (this.digraphs == null) {
            graph.addStackCheckpoint();
            List<Set<Integer>> digraphs = new ArrayList<>();
            Set<Integer> nodes = graph.nodeMap.keySet();
            while (!graph.nodeMap.isEmpty()) {
                Integer u = nodes.iterator().next();
                Set<Integer> digraph = quickExpand(u);
                digraphs.add(digraph);
                graph.removeAllNodes(digraph);
            }
            this.digraphs = digraphs;
            graph.rebuildGraph();
        }

        Set<Integer> safeToDelete = new HashSet<>();
        for (Set<Integer> digraph : this.digraphs) {
            int n = digraph.size();
            for (Integer i : digraph) {
                if (graph.getNode(i).getInDegree() == n - 1 || graph.getNode(i).getOutDegree() == n - 1) {
                    safeToDelete.addAll(digraph);
                    safeToDelete.remove(i);
                    break;
                }
            }
        }
        return safeToDelete;
    }

    public List<Deque<Integer>> newFindCyclePacking() {
        return newFindCyclePacking(Integer.MAX_VALUE - 100, Integer.MAX_VALUE - 100, Integer.MAX_VALUE - 100);
    }

    public List<Deque<Integer>> newFindCyclePacking(int limit, int sameCycleCount, int packingLimit) {
        graph.addStackCheckpoint();
        int kBefore = graph.k;
        graph.k = Integer.MAX_VALUE;
        List<Deque<Integer>> packing = new ArrayList<>();
        Deque<Integer> cycle = graph.findBestCycle(sameCycleCount, limit);
        while (cycle != null && packing.size() < packingLimit) {
            packing.add(cycle);
            for (Integer i : cycle) {
                DirectedNode node = graph.nodeMap.get(i);
                if (node != null) {
                    Set<Integer> neighbours = new HashSet<>(node.getInNodes());
                    neighbours.addAll(node.getOutNodes());
                    graph.removeNode(i);
                    graph.quickClean(neighbours);
                }
            }
            cycle = graph.findBestCycle(limit, sameCycleCount);
        }
        graph.rebuildGraph();
        graph.k = kBefore;
        return packing;
    }

    public List<Set<Integer>> getDigraphs() {
        if (this.digraphs != null) return digraphs;
        graph.addStackCheckpoint();
        List<Set<Integer>> digraphs = new ArrayList<>();
        Set<Integer> nodes = graph.nodeMap.keySet();
        while (!graph.nodeMap.isEmpty()) {
            Integer u = nodes.stream().iterator().next();
            Set<Integer> a = expand(u);
            digraphs.add(a);
            for (Integer i : a) {
                graph.removeNode(i);
            }
            a.forEach(nodes::remove);
        }
        this.digraphs = digraphs;
        graph.rebuildGraph();
        return digraphs;
    }

    private Set<Integer> expand(Integer start) {
        Set<Integer> digraph = new HashSet<>();
        digraph.add(start);
        boolean change = true;
        Set<Integer> commonNeighbours = new HashSet<>(graph.nodeMap.get(start).getInNodes());
        while (change) {
            for (Integer u : digraph) {
                commonNeighbours.retainAll(graph.getNode(u).getInNodes());
                commonNeighbours.retainAll(graph.getNode(u).getOutNodes());
                if (commonNeighbours.isEmpty()) {
                    change = false;
                    break;
                }
            }
            if (!commonNeighbours.isEmpty()) {
                digraph.add(commonNeighbours.iterator().next());
            }
        }
        return digraph;
    }

    private Set<Integer> quickExpand(Integer startNode) {
        Set<Integer> digraph = new HashSet<>();
        digraph.add(startNode);
        boolean change = true;

        // Initialize common neighbours
        Set<Integer> commonNeighbours;
        if (graph.nodeMap.get(startNode).getInDegree() > graph.nodeMap.get(startNode).getOutDegree()) {
            commonNeighbours = new HashSet<>(graph.nodeMap.get(startNode).getOutNodes());
            commonNeighbours.retainAll(graph.nodeMap.get(startNode).getInNodes());
        } else {
            commonNeighbours = new HashSet<>(graph.nodeMap.get(startNode).getInNodes());
            commonNeighbours.retainAll(graph.nodeMap.get(startNode).getOutNodes());
        }

        while (change) {
            for (Integer u : digraph) {
                commonNeighbours.retainAll(graph.getNode(u).getInNodes());
                commonNeighbours.retainAll(graph.getNode(u).getOutNodes());
                if (commonNeighbours.isEmpty()) {
                    change = false;
                    break;
                }
            }
            if (!commonNeighbours.isEmpty()) {
                digraph.add(commonNeighbours.iterator().next());
            }
        }
        return digraph;
    }
}