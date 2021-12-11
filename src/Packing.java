import java.util.*;

public class Packing {
    private final DirectedGraph graph;
    private final DirectedGraph graphFixed;
    private final Set<Integer> usedNodes;
    private final Set<Deque<Integer>> costlySubGraphs;

    Packing(DirectedGraph graph) {
        this.graph = new DirectedGraph(graph);
        graphFixed = new DirectedGraph(graph);
        costlySubGraphs = new HashSet<>();
        usedNodes = new HashSet<>();
    }

    public static void main(String[] args) {
        DirectedGraph graph = new DirectedGraph("instances/complex/biology-n_45-m_326-p_0.5-16");
        Packing stacking = new Packing(graph);
        stacking.findCirclePacking();
        System.out.println(stacking.costlySubGraphs);
        System.out.println(stacking.usedNodes);
    }

    public void findCirclePacking() {
        Deque<Integer> cycle = graph.findBestCycle();
        while (cycle != null) {
            costlySubGraphs.add(cycle);
            for (Integer i : cycle) {
                graph.removeNode(i);
            }
            cycle = graph.findBestCycle();
        }
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
                if (graphFixed.hasEdge(u, v) && graphFixed.hasEdge(v, u) && graphFixed.hasEdge(u, w)
                    && graphFixed.hasEdge(w, u) && graphFixed.hasEdge(v, w) && graphFixed.hasEdge(w, v)) {
                    System.out.println("3-Digraph found");
                    lowerBound++;
                }
            }
        }
        return lowerBound;
    }

    public void findFull3Digraphs() {
        HashMap<Integer, Boolean> visited = new HashMap<>();
        HashMap<Integer, Integer> parent = new HashMap<>();
        Deque<Integer> cycle = new ArrayDeque<>();
        for (Integer i : graph.nodeMap.keySet()) {
            visited.put(i, false);
            parent.put(i, -1);
        }
        for (Integer start : graph.nodeMap.keySet()) {
            if (visited.get(start)) continue;
            Deque<Integer> queue = new ArrayDeque<>();
            Deque<Integer> tempCycle = new ArrayDeque<>();
            queue.add(start);
            visited.put(start, true);
            while(!queue.isEmpty()) {
                Integer u = queue.pop();
                for (Integer v : graph.nodeMap.get(u).getOutNodes()) {
                    if (!visited.get(v)) {
                        parent.put(v, u);
                        visited.put(v, true);
                        queue.add(v);
                    }
                    if (v.equals(start)) {

                        int w = u;
                        while(w != -1) {
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
                    if (!graph.nodeMap.get(u).isFixed()) nodesLeft.add(u);
                }
            }
            else {
                nodesLeft = tempCycle;
            }
            if(tempCycle.size() == 3) {
                Iterator<Integer> it = tempCycle.iterator();
                int u = it.next();
                int v = it.next();
                int w = it.next();
                if (graph.hasEdge(u, v) && graph.hasEdge(v, w) && graph.hasEdge(w, u)
                        && graph.hasEdge(v, u) && graph.hasEdge(w, v) && graph.hasEdge(u, w)) {
                    usedNodes.add(u);
                    usedNodes.add(v);
                    usedNodes.add(w);
                }
            }
        }
    }

}
