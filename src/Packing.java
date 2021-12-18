import java.util.*;

public class Packing {
    private final DirectedGraph graph;
    private final List<List<Integer>> mixedStructs;
    private final Set<Integer> safeToDeleteDigraphNodes;
    public Set<List<Integer>> digraphs;

    Packing(DirectedGraph graph) {
        this.graph = graph;
        digraphs = new HashSet<>();
        safeToDeleteDigraphNodes = new HashSet<>();
        mixedStructs = new ArrayList<>();
    }

    public void safeDeletionDigraph() {
        Set<Integer> safeToDelete = new HashSet<>();
        for (List<Integer> digraph : digraphs) {
            int n = digraph.size();
            Set<Integer> deletionCandidates = new HashSet<>();
            for (Integer i : digraph) {
                if (graph.getNode(i).getInDegree() == n - 1 || graph.getNode(i).getOutDegree() == n - 1) {
                    deletionCandidates.addAll(digraph);
                    deletionCandidates.remove(i);
                    break;
                }
            }
            safeToDelete.addAll(deletionCandidates);
        }
        safeToDeleteDigraphNodes.addAll(safeToDelete);
    }

    public Set<Integer> getSafeToDeleteDigraphNodes() {
        safeDeletionDigraph();
        return safeToDeleteDigraphNodes;
    }

    public List<Deque<Integer>> findCyclePacking() {
        graph.addStackCheckpoint();
        List<Deque<Integer>> costlySubGraphs = new ArrayList<>();
        Deque<Integer> cycle = graph.findBestCycle();
        while (cycle != null && !cycle.isEmpty()) {
            costlySubGraphs.add(cycle);
            for (Integer i : cycle) {
                graph.removeNode(i);
            }
            cycle = graph.findBestCycle();
        }
        graph.rebuildGraph();
        return costlySubGraphs;
    }

    public Set<List<Integer>> getDigraphs() {
        graph.addStackCheckpoint();
        Set<List<Integer>> digraphs = new HashSet<>();
        Set<Integer> nodes = graph.nodeMap.keySet();
        while (!graph.nodeMap.isEmpty()) {
            Integer u = nodes.stream().iterator().next();
            List<Integer> a = expand(u);
            boolean fullDigraphDeletable = false;
            /*
            for (Integer node : nodes) {
                if (!a.contains(node)) {
                    int countIn = 0;
                    int countOut = 0;
                    for (Integer v :  a) {
                        if (graph.hasEdge(v, node)) countOut++;
                        if (graph.hasEdge(node, v)) countIn++;
                        if (countIn > 0 && countOut > 0) {
                            break;
                        }
                    }
                    if (countIn > 0 && countOut > 0) {
                        fullDigraphDeletable = true;
                        break;
                    }
                }
            }
            if (fullDigraphDeletable) safeToDeleteDigraphNodes.addAll(a);
            else {
                digraphs.add(a);
            }*/
            digraphs.add(a);
            for (Integer i : a) {
                graph.removeNode(i);
            }
            nodes.removeAll(a);
        }
        this.digraphs = digraphs;
        graph.rebuildGraph();
        return digraphs;
    }

    public List<List<Integer>> getMixedStruct() {
        graph.addStackCheckpoint();
        Set<List<Integer>> digraphs = new HashSet<>();
        Set<Integer> nodes = graph.nodeMap.keySet();
        while (!graph.nodeMap.isEmpty()) {
            Integer u = nodes.stream().iterator().next();
            List<Integer> a = expand(u);
            if (a.size() > 2) digraphs.add(a);
            for (Integer i : a) {
                graph.removeNode(i);
            }
            nodes.removeAll(a);
        }
        this.digraphs = digraphs;
        this.mixedStructs.addAll(digraphs);
        this.mixedStructs.add(null);
        for (Deque<Integer> cycle : findCyclePacking()) {
            digraphs.add(new ArrayList<>(cycle));
        }
        graph.rebuildGraph();
        return mixedStructs;
    }


    public int lowerDigraphBound() {
        int lowerBound = 0;
        for (List<Integer> struct : digraphs) {
            lowerBound += struct.size() - 1;
        }
        return lowerBound;
    }


    private List<Integer> expand(Integer start) {
        List<Integer> digraph = new ArrayList<>();
        digraph.add(start);
        boolean change = true;
        Set<Integer> commonNeighbours = new HashSet<>(graph.nodeMap.get(start).getInNodes());
        while (change) {
            commonNeighbours.addAll(graph.getNode(digraph.get(0)).getInNodes());
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
        //Set<Integer> commonNeighbours = new HashSet<>();
        commonNeighbours.addAll(graph.getNode(start).getOutNodes());
        commonNeighbours.retainAll(graph.getNode(start).getInNodes());
        //System.out.println(start + " " + graph.getNode(start).getOutNodes()
        // + " " + graph.getNode(start).getInNodes() + " " + commonNeighbours);
        return digraph;
    }


//________________________________________________Ab hier Baustelle


    public Set<Set<Integer>> findQuickPacking() {
        Set<Set<Integer>> packing = new HashSet<>();
        Set<Integer> nodes = new HashSet<>();
        Set<Integer> nodesForBiggerCircles = new HashSet<>();
        nodes.addAll(graph.nodeMap.keySet());
        while (!nodes.isEmpty()) {
            Integer i = nodes.stream().findFirst().get();
            if (graph.getNode(i).isTwoCycle() != -1) {
                Set<Integer> twoCycle = new HashSet<>();
                twoCycle.add(i);
                twoCycle.add(graph.getNode(i).isTwoCycle());
                packing.add(twoCycle);
                nodes.removeAll(twoCycle);
                graph.removeAllNodes(twoCycle);
            } else {
                nodes.remove(i);
            }
        }
        while (true) {
            Deque<Integer> newCycle = graph.findBestCycle();
            if (newCycle == null) break;
            Set<Integer> newC = new HashSet<>();
            newC.addAll(newCycle);
            if (newC == null || newC.isEmpty()) break;
            packing.add(newC);
            graph.removeAllNodes(newC);
        }
        return packing;
    }


}