import java.io.File;
import java.util.*;

public class Packing {
    private final DirectedGraph graph;
    private Set<Integer> safeToDeleteDigraphNodes;
    private List<Set<Integer>> digraphs;

    Packing(DirectedGraph graph) {
        this.graph = graph;
    }

    public static void main(String[] args) {
        File complexInst = new File("instances/sheet5-heuristic");
        System.out.println("name \t cleanTime \t digraphTime \t newPacking \t newTime \t oldPacking \t oldTime");
        for (String ins : List.of("e_159", "e_169", "e_177", "h_133", "h_157", "h_171", "h_179", "h_187", "h_189")) {
            String name = "instances\\sheet5-heuristic\\" + ins;
            if(name.contains(".new")) continue;
            DirectedGraph g = new DirectedGraph(name);
            Packing p = new Packing(g);
            name = name.substring(name.lastIndexOf('\\') + 1);
            System.out.print(name);
            long time = -System.nanoTime();
            g.rootClean(null, false);
            double cleanTime = utils.round((double) (time+System.nanoTime())/1_000_000_000, 5);
            //System.out.print("\t" + cleanTime);
            time = -System.nanoTime();
            g.removeAllNodes(p.getSafeToDeleteDigraphNodes(true));
            double diGraphTime = utils.round((double) (time+System.nanoTime())/1_000_000_000, 5);
            //System.out.print("\t" + diGraphTime);
            time = -System.nanoTime();
            int newPacking = p.newFindCyclePacking(4,2).size();
            double newTime = utils.round((double) (time+System.nanoTime())/1_000_000_000, 5);
            System.out.println("\t" + newPacking + "\t" + newTime);
        }
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
        return newFindCyclePacking(Integer.MAX_VALUE - 100, Integer.MAX_VALUE - 100);
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

    public List<Deque<Integer>> newFindCyclePacking(int limit, int sameCycleCount) {
        graph.addStackCheckpoint();
        List<Deque<Integer>> packing = new ArrayList<>();
        Deque<Integer> cycle = graph.findBestCycle(limit, sameCycleCount);
        while (cycle != null && !cycle.isEmpty()) {
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
        return packing;
    }

    public List<Set<Integer>> getDigraphs() {
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

    public int lowerDigraphBound() {
        int lowerBound = 0;
        for (Set<Integer> struct : digraphs) {
            lowerBound += struct.size() - 1;
        }
        return lowerBound;
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