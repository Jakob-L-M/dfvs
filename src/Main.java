import java.util.*;

public class Main {

    public static int recursions;

    public static Set<Integer> dfvsBranch(DirectedGraph graph, int k, boolean isScc) {

        Packing stacking = new Packing(graph);
        stacking.findCyclePacking();
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
            // We will return the found selfCycles
            return selfCycles;

        }


        Map<Integer, List<Integer>> sortedCycle = new HashMap<>();
        for (Integer v : cycle) {
            graph.calculatePetal(v);
            if (!sortedCycle.containsKey(graph.getNode(v).getPedal())) {
                sortedCycle.put(graph.getNode(v).getPedal(), new ArrayList<>());
            }
            sortedCycle.get(graph.getNode(v).getPedal()).add(v);
        }
        List<Integer> pedalValues = new ArrayList<>(sortedCycle.keySet());
        pedalValues.sort(Collections.reverseOrder());


        for (Integer i : pedalValues) {

            for (Integer v : sortedCycle.get(i)) {

                // delete a vertex of the circle and branch for here
                graph.addStackCheckpoint();
                graph.removeNode(v);

                // increment recursions to keep track of tree size
                recursions++;

                // branch with a maximum cost of k
                // -1: Just deleted a node
                // -selfCycles.size(): nodes that where removed during graph reduction
                Set<Integer> dfvs = dfvsBranch(graph, k - 1, false);

                // if there is a valid solution in the recursion it will be returned
                if (dfvs != null) {

                    // Add the nodeId of the valid solution and all selfCycles
                    dfvs.add(v);
                    dfvs.addAll(selfCycles);

                    return dfvs;
                } else {
                    graph.rebuildGraph();
                    graph.getNode(v).fixNode();
                }
            }
        }
        return null;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph) {
        Set<Integer> allDfvs = graph.cleanGraph(Integer.MAX_VALUE);
        graph.clearStack();
        Set<DirectedGraph> SCCs = new Tarjan(graph).getSCCGraphs();
        graph.clearStack();

        for (DirectedGraph scc : SCCs) {
            allDfvs.addAll(scc.cleanGraph(Integer.MAX_VALUE));
            scc.clearStack();

            Packing stacking = new Packing(scc);
            stacking.findCyclePacking();
            int k = stacking.getLowerBound();

            Set<Integer> dfvs = null;

            while (dfvs == null) {
                scc.addStackCheckpoint();
                dfvs = dfvsBranch(scc, k, true);
                k++;
                scc.rebuildGraph();
            }
            allDfvs.addAll(dfvs);
        }

        return allDfvs;
    }

    public static void developMain(String file) {
        DirectedGraph graph = new DirectedGraph(file);
        graph.clearStack();
        System.out.println("Solving: " + file);
        long time = -System.nanoTime();
        System.out.println("\tk: " + dfvsSolve(graph).size());
        System.out.println("\t#recursive steps: " + recursions);
        double sec = utils.round((time + System.nanoTime()) / 1_000_000_000.0, 4);
        System.out.println("\ttime: " + sec);
        recursions = 0;
    }

    public static void main(String[] args) {

        /*
        developMain("instances/complex/chess-n_1000 "); // 60
        developMain("instances/complex/health-n_1000");// 232
        developMain("instances/complex/link-kv-n_300"); // 55
        developMain("instances/complex/biology-n_25-m_231-p_0.9-6"); // 8
        developMain("instances/complex/biology-n_30-m_287-p_0.5-5"); // 15
        developMain("instances/complex/biology-n_35-m_315-p_0.5-18"); // 17

        developMain("instances/synthetic/synth-n_40-m_203-k_8-p_0.2.txt"); // 7
        developMain("instances/synthetic/synth-n_60-m_110-k_2-p_0.05.txt");// 1
        developMain("instances/synthetic/synth-n_60-m_386-k_4-p_0.2.txt"); // 4
        developMain("instances/synthetic/synth-n_200-m_1172-k_20-p_0.05.txt"); // 20
        developMain("instances/synthetic/synth-n_100-m_1235-k_20-p_0.2.txt"); // 20
        //developMain("instances/synthetic/synth-n_90-m_327-k_30-p_0.05.txt"); // 14



         */
        //developMain("instances/synthetic/synth-n_50-m_357-k_20-p_0.2.txt");//20
        //developMain("instances/synthetic/synth-n_140-m_1181-k_20-p_0.1.txt"); //20
        //developMain("instances/synthetic/synth-n_120-m_492-k_30-p_0.05.txt"); //21
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
	#recursive steps: 14805
	time: 11.1876
Solving: instances/complex/biology-n_35-m_315-p_0.75-18
	k: 15
	#recursive steps: 34718
	time: 6.6971
Solving: instances/complex/link-kv-n_300
	k: 55
	#recursive steps: 120
	time: 0.009
Solving: instances/complex/biology-n_30-m_287-p_0.5-5
	k: 15
	#recursive steps: 32768
	time: 5.4951
Solving: instances/complex/biology-n_35-m_315-p_0.5-18
	k: 17
	#recursive steps: 42901
	time: 9.4446
 */