import java.util.*;

public class Packing {
    private final DirectedGraph graph;
    private final Set<Deque<Integer>> costlySubGraphs;

    Packing(DirectedGraph graph) {
        this.graph = graph;
        costlySubGraphs = new HashSet<>();
    }

    public void findCirclePacking() {
        graph.addStackCheckpoint();
        Deque<Integer> cycle = graph.findBestCycle();
        while (cycle != null) {
            costlySubGraphs.add(cycle);
            for (Integer i : cycle) {
                graph.removeNode(i);
            }
            cycle = graph.findBestCycle();
        }
        graph.rebuildGraph();
    }

    public int getLowerBound() {
        int lowerBound = 0;
        for (Deque<Integer> deque : costlySubGraphs) {
            lowerBound++;
            if (deque.size() == 3) {
                List<Integer> triplet = new ArrayList<>(deque);
                Integer u = triplet.get(0);
                Integer v = triplet.get(1);
                Integer w = triplet.get(2);
                if (graph.hasEdge(u, v) && graph.hasEdge(v, u) && graph.hasEdge(u, w)
                    && graph.hasEdge(w, u) && graph.hasEdge(v, w) && graph.hasEdge(w, v)) {
                    System.out.println("3-Digraph found");
                    lowerBound++;
                }
            }
        }
        return lowerBound;
    }
}
