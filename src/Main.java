import java.util.*;

public class Main {
    public static Set<Integer> fixGlobally = new HashSet<>();
    public static int recursions;
    public static Set<Integer> dfvsBranch(DirectedGraph graph, int k) {

        if (k < 0) return null;
        Set<Integer> dfvs = new HashSet<>();


        if(k < 5) {
            //method fully functional without the following                     TARJAN BEGIN
            Tarjan tarjan = new Tarjan(graph);
            Set<DirectedGraph> subGraphs = tarjan.getSCCGraphs();
            Iterator<DirectedGraph> it = subGraphs.iterator();
            if (subGraphs.size() > 1) {
                Set<Integer> dfvs_mult = new HashSet<>();
                while (it.hasNext() && dfvs_mult.size() <= k) {
                    dfvs_mult.addAll(dfvsSolve(it.next()));
                }
                if (dfvs_mult.size() <= k) {
                    return dfvs_mult;
                } else return null;
            }
        }

        Deque<Integer> cycle = graph.findCycleBFS();


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
            recursions++;
            dfvs = dfvsBranch(graphCopy, k - 1);


            // if there is a valid solution in the recursion it will be returned
            if (dfvs != null) {
                dfvs.add(v);
                dfvs.addAll(selfCycles);
                //System.out.println("k ist: " + k + ". Trotzdem noch " + selfCycles.size() + " + 1 Knoten aus " + selfCycles.toString() + " hinzugefügt.");
                return dfvs;
            }
            else {
                fixGlobally.add(v);
            }
        }
        return null;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph) {
        Set<Integer> selfCycle = graph.cleanGraph();
        //System.out.println("Ganz am Anfang " + selfCycle.size() + " Knoten:"+ selfCycle.toString());
        int k = 0;
        Set<Integer> dfvs = null;
        recursions = 0;
        while (dfvs == null) {
            for(Integer node : fixGlobally) {
                graph.fixNode(node);
            }
            fixGlobally = new HashSet<>();
            dfvs = dfvsBranch(graph, k++);
        }
        dfvs.addAll(selfCycle);
        return dfvs;
    }

    public static void main(String[] args) {
        long time = -System.nanoTime();
        DirectedGraph graph = new DirectedGraph(args[0]);//"instances/complex/biology-n_13-m_39-p_0.75-4");
        time += System.nanoTime();
        for (int i : dfvsSolve(graph)) {
            System.out.println(graph.dict.inverse().get(i));
        }
        System.out.println("#recursive steps: " + recursions);
    }
}