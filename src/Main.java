import java.util.*;

public class Main {

    public static Set<Integer> dfvsBranch(DirectedGraph graph, int k) {

        if (k < 0) return null;
        Set<Integer> dfvs = new HashSet<>();
        //Set<Integer> selfCycles = graph.cleanGraph();
        Set<Integer> selfCycles = new HashSet<>();
        if (selfCycles.size() > k) {
            //System.out.println("Does this happen?" + k);
            return null;
        }

        Deque<Integer> cycle = graph.findCycle();


        if (cycle == null && selfCycles.size() <= k) {
            dfvs.addAll(selfCycles);
            //System.out.println("Kreisfrei und k ist " + k + ". Es liegen " + selfCycles.size() + " Self-Cycles vor.");
            return dfvs;
        }

        for (Integer v : cycle) {
            // create a copy
            DirectedGraph graphCopy = new DirectedGraph(graph);

            // delete a vertex of the circle and branch for here
            graphCopy.removeNode(v);
            dfvs = dfvsBranch(graphCopy, k - 1 - selfCycles.size());
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
        //System.out.println("Ganz am Anfang " + selfCycle.size() + " Knoten:"+ selfCycle.toString());
        int k = 0;
        Set<Integer> dfvs = null;
        while (dfvs == null) {
            dfvs = dfvsBranch(graph, k++);
        }
        dfvs.addAll(selfCycle);
        return dfvs;
    }

    public static void main(String[] args) {
        DirectedGraph graph = new DirectedGraph(args[0]);//"./instances/complex/biology-n_13-m_39-p_0.5-4");
        Set<Integer> solution = dfvsSolve(graph);
        for (int i : solution) {
            System.out.println(graph.dict.inverse().get(i));
        }
        //System.out.println("opt k:" + solution.size());
    }
}