import java.util.Deque;
import java.util.List;
import java.util.Set;

public class Main {

    public static int recursions;

    public static Set<Integer> dfvsBranch(DirectedGraph graph, int k) {

        if (k < 0) return null;

        // clean the graph and save all selfCycle-Nodes that have been removed during cleaning
        Set<Integer> selfCycles = graph.cleanGraph();

        // If there should be more selfCycles than k, we can break here.
        if (selfCycles.size() > k) {
            return null;
        }

        // find a Cycle
        Deque<Integer> cycle = graph.findBestCycle();

        if (cycle == null && selfCycles.size() <= k) {
            // The graph does not have a cycle anymore
            // We will return the found selfCycles
            return selfCycles;
        } else if (cycle == null) {
            return null;
        }

        for (Integer v : cycle) {
            // create a copy
            DirectedGraph graphCopy = new DirectedGraph(graph);

            // delete a vertex of the circle and branch for here
            graphCopy.removeNode(v);
            recursions++;
            Set<Integer> dfvs = dfvsBranch(graphCopy, k - 1 - selfCycles.size());
            //dfvs = dfvsBranch(graphCopy, k - 1);

            // if there is a valid solution in the recursion it will be returned
            if (dfvs != null) {
                dfvs.add(v);
                dfvs.addAll(selfCycles);
                //System.out.println("k ist: " + k + ". Trotzdem noch " + selfCycles.size() + " + 1 Knoten aus " + selfCycles.toString() + " hinzugefügt.");
                return dfvs;
            }
        }
        return null;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph) {
        Set<Integer> selfCycle = graph.cleanGraph();

        //System.out.println("Anfang: " + selfCycle);
        int k = 0;
        Set<Integer> dfvs = null;
        while (dfvs == null) {
            dfvs = dfvsBranch(graph, k++);
        }
        dfvs.addAll(selfCycle);
        return dfvs;
    }

    public static void main(String[] args) {
        DirectedGraph graph = new DirectedGraph(args[0]);
        for (int i: dfvsSolve(graph)) {
            System.out.println(i);
        }
        System.out.println("#recursive steps: " + recursions);
    }
}