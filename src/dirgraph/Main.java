package dirgraph;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class Main {

    public static Set<Integer> dfvsBranch(DirectedGraph graph, int k) {

        if (k < 0) return null;
        //graph.cleanGraph();
        Set<Integer> dfvs = new HashSet<>();
        Deque<Integer> cycle = graph.findBusyCycle();

        if (cycle == null) return dfvs;
        //cycle.stream().filter(c -> graph.isFixed((int) c));

        for (Integer v : cycle) {
            /*Set removedNodes = new HashSet<DirectedNode>();
            DirectedNode removedNode = new DirectedNode((DirectedNode) graph.nodeMap.get(v));
            removedNodes.add(removedNode);
            */
            DirectedGraph graphCopy = new DirectedGraph(graph);
            //removedNodes.addAll(graph/*Copy*/.removeClean((int) v));
            graphCopy.removeClean(v);
            dfvs = dfvsBranch(graphCopy, k - 1);
            if (dfvs != null) {
                dfvs.add(v);
                //System.out.println("nodes to delete: " + v + " which is fixed: " + graph.isFixed((int) v));
                return dfvs;
            } else {
                //graph.reconstructNodes(removedNodes);
                graph.fixNode(v);
            }
        }
        return null;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph) {
        int k = 0;
        Set<Integer> dfvs = null;
        while (dfvs == null) {
            dfvs = dfvsBranch(graph, k++);
        }
        return dfvs;
    }

    public static void main(String[] args) {
        DirectedGraph graph = new DirectedGraph("instances/synthetic/synth-n_20-m_54-k_8-p_0.2.txt" /*"resources/small.txargs[0]*/);
        System.out.println(graph);
        graph.cleaningChains();
        System.out.println(graph);

        for (int i : dfvsSolve(graph)
        ) {
            System.out.println(i);
        }

    }
}