import java.util.*;

public class Main {

    public static Set<Integer> dfvsBranch(DirectedGraph graph, int k) {

        if (k < 0) return null;
        Set<Integer> selfCycles = graph.cleanGraph();
        Set<Integer> dfvs = new HashSet<>();

        Deque<Integer> cycle = graph.findCycle();


        if (cycle == null) {
            dfvs.addAll(selfCycles);
            return dfvs;
        }

        for (Integer v : cycle) {
            // create a copy
            DirectedGraph graphCopy = new DirectedGraph(graph);

            // delete a vertex of the circle and branch for here
            graphCopy.removeNode(v);
            dfvs = dfvsBranch(graphCopy, k - 1 - selfCycles.size());

            // if there is a valid solution in the recursion it will be returned
            if (dfvs != null) {
                dfvs.add(v);
                dfvs.addAll(selfCycles);
                return dfvs;
            }
        }
        return null;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph) {
        Set<Integer> selfCycle = graph.cleanGraph();
        int k = selfCycle.size() - 1;
        Set<Integer> dfvs = null;
        while (dfvs == null) {
            dfvs = dfvsBranch(graph, k++);
        }
        dfvs.addAll(selfCycle);
        return dfvs;
    }

    public static void main(String[] args) {
        DirectedGraph graph = new DirectedGraph("./instances/complex/chess-n_700");
        Set<Integer> solution = dfvsSolve(graph);
        for (int i : solution) {
            System.out.println(graph.dict.inverse().get(i));
        }
        System.out.println("opt k:" + solution.size());
    }
}