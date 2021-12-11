import java.util.*;

public class Main {

    public static int recursions;

    public static Set<Integer> dfvsBranch(DirectedGraph graph, int k, boolean isScc) {

        Packing stacking = new Packing(graph);
        stacking.findCirclePacking();
        int lowerBound = stacking.getLowerBound();
        if (k < lowerBound) return null;

        // clean the graph and save all selfCycle-Nodes that have been removed during cleaning
        Set<Integer> selfCycles;
        if (!isScc) {
            selfCycles = graph.cleanGraph(k);//new HashSet<>();
        } else {
            selfCycles = new HashSet<>();
        }

        // If there should be more selfCycles than k, we can break here.
        if (selfCycles.size() > k) {
            return null;
        }

        // Reduce the leftover budget by selfCycles.size()
        k = k - selfCycles.size();


        // find a Cycle
        Deque<Integer> cycle = graph.findBestCycle();

        if (cycle == null) {

            // The graph does not have any cycles
            // We will return the found selfCycles if they are in our current limit
            return selfCycles;

        }

        Map<Integer, List<Integer>> sortedCycle = new HashMap<>();
        for (Integer v : cycle) {
            graph.calculatePedalValue(v);
            if (!sortedCycle.containsKey(graph.getNode(v).getPedal())) {
                sortedCycle.put(graph.getNode(v).getPedal(), new ArrayList<>());
            }
            sortedCycle.get(graph.getNode(v).getPedal()).add(v);
        }
        List<Integer> pedalValues = new ArrayList<>(sortedCycle.keySet());
        pedalValues.sort(Collections.reverseOrder());

        for (Integer i : pedalValues) {

            for (Integer v : sortedCycle.get(i)) {
                // create a copy -> we will branch off of that copy
                DirectedGraph graphCopy = new DirectedGraph(graph);

                // delete a vertex of the circle and branch for here
                graphCopy.removeNode(v);

                // increment recursions to keep track of tree size
                recursions++;

                // branch with a maximum cost of k
                // -1: Just deleted a node
                // -selfCycles.size(): nodes that where removed during graph reduction
                Set<Integer> dfvs = dfvsBranch(graphCopy, k - 1, false);

                // if there is a valid solution in the recursion it will be returned
                if (dfvs != null) {

                    // Add the nodeId of the valid solution and all selfCycles
                    dfvs.add(v);
                    dfvs.addAll(selfCycles);

                    return dfvs;
                } else {
                    graph.getNode(v).fixNode();
                }
            }
        }
        return null;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph) {
        Set<Integer> allDfvs = graph.cleanGraph(Integer.MAX_VALUE);
        Set<DirectedGraph> SCCs = new Tarjan(graph).getSCCGraphs();

        for (DirectedGraph scc : SCCs) {
            allDfvs.addAll(scc.cleanGraph(Integer.MAX_VALUE));
            Packing stacking = new Packing(scc);
            stacking.findCirclePacking();
            int k = stacking.getLowerBound();
            Set<Integer> dfvs = null;

            while (dfvs == null) {
                dfvs = dfvsBranch(scc, k, true);
                k++;
            }
            allDfvs.addAll(dfvs);
        }

        return allDfvs;
    }

    public static void developMain(String file) {
        DirectedGraph graph = new DirectedGraph(file);
        System.out.println("Solving: " + file);
        long time = -System.nanoTime();
        Set<Integer> solution = dfvsSolve(graph);
        System.out.println("\tk: " + solution.size());
        System.out.println("\t#recursive steps: " + recursions);
        double sec = utils.round((time + System.nanoTime()) / 1_000_000_000.0, 4);
        System.out.println("\ttime: " + sec);
        recursions = 0;
    }

    public static void main(String[] args) {
        /*
        developMain("instances/complex/chess-n_1000 "); //60
        developMain("instances/complex/biology-n_35-m_315-p_0.75-18");//15
        developMain("instances/complex/link-kv-n_300"); //55
        developMain("instances/complex/biology-n_30-m_287-p_0.5-5"); //15
        developMain("instances/complex/biology-n_35-m_315-p_0.5-18"); //17

         */
        developMain("instances/synthetic/synth-n_50-m_357-k_20-p_0.2.txt");//20
        /*
        DirectedGraph graph = new DirectedGraph(
                args[0]);
        Set<Integer> solution = dfvsSolve(graph);
        if (solution != null) {
            for (int i : solution) {
                System.out.println(i);
            }
        }
        System.out.println("#recursive steps: " + recursions);
         */
    }
}
/*
Solving: instances/complex/chess-n_1000
k: 60
#recursive steps: 167710 - 24974
time: 0:19.70 - 17.2822

Solving: instances/complex/biology-n_35-m_315-p_0.5-18
k: 17
#recursive steps: 996892 - 27353
time: 0:50.35 - 11.5849

Solving: instances/complex/biology-n_35-m_315-p_0.75-18
k: 15
#recursive steps: 266033 - 23847
time: 0:15.13 - 8.8515
 */