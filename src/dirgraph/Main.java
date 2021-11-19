package dirgraph;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class Main {

    public static Set<Integer> dfvsBranch(DirectedGraph graph, int k) {

        if (k < 0) return null;
        graph.cleanGraph();
        Set<Integer> dfvs = new HashSet<>();
        Deque<Integer> cycle = graph.findBusyCycle();

        if (cycle == null) return dfvs;

        for (Integer v : cycle) {
            // create a copy
            DirectedGraph graphCopy = new DirectedGraph(graph);

            // delete a vertex of the circle and branch for here
            graphCopy.removeNode(v);
            dfvs = dfvsBranch(graphCopy, k - 1);

            // if there is a valid solution in the recursion it will be returned
            if (dfvs != null) {
                dfvs.add(v);
                return dfvs;
            }
        }
        return null;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph) {
        int k = 0;
        Set<Integer> dfvs = null;
        while (dfvs == null) {
            System.out.println(k);
            dfvs = dfvsBranch(graph, k++);
        }
        return dfvs;
    }

    public static void main(String[] args) {
        DirectedGraph graph = new DirectedGraph("instances/synthetic/synth-n_40-m_203-k_8-p_0.2.txt"/*args[0]*/);
        System.out.println(graph);
        graph.cleanGraph();
        System.out.println(graph);

        for (int i : dfvsSolve(graph)) {
            System.out.println(i);
        }

    }
}