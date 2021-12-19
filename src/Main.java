import java.util.*;

public class Main {

    public static int recursions;
    public static int petal;
    public static long petal_time;
    public static int petal_suc;
    public static int digraph;
    public static long digraph_time;
    public static int digraph_suc;
    public static Set<List<Integer>> fullDigraphs;
    public static Map<String, Integer> solSizes;

    public static Set<Integer> dfvsBranch(DirectedGraph graph) {

        // increment recursions to keep track of tree size
        recursions++;

        // clean the graph if the budget limit is reached we can break here
        Set<Integer> cleanedNodes = graph.cleanGraph();

        if (cleanedNodes == null) {
            return null;
        }

        Packing packing = null;
        if (graph.k > 3 && graph.k <= Math.sqrt(graph.nodeMap.size()) - 1) {
            petal++;
            long time = -System.nanoTime();
            int k_before = graph.k;

            int maxPetal = -1;
            int maxPetalId = -1;
            Set<Integer> petalSet = null;
            for (Integer nodeId : new HashSet<>(graph.nodeMap.keySet())) {

                // all petals will be grater than 0 after the cleaning cycle.
                if (graph.nodeMap.get(nodeId).getPedal() > maxPetal) {
                    Tuple currentFlower = graph.calculatePetal(nodeId);

                    // if the found petal is still larger we will update the maximum flower
                    if (currentFlower.getValue() > maxPetal) {
                        maxPetalId = nodeId;
                        maxPetal = currentFlower.getValue();
                        petalSet = currentFlower.getSet();
                    }
                }
            }

            if (maxPetalId != -1) {
                graph.addStackCheckpoint();
                graph.removeAllNodes(petalSet);
                packing = new Packing(graph);
                int tempK = graph.k;
                graph.k = 1000000;
                Set<Integer> preClean = graph.cleanGraph();
                graph.k = tempK - preClean.size();
                //if (!preClean.isEmpty()) System.out.println("hier clean: " + preClean.size());
                if (graph.k - preClean.size() - packing.findCyclePacking().size() + graph.solution.size() < maxPetal) {

                    graph.rebuildGraph();
                    graph.removeNode(maxPetalId);
                    cleanedNodes.add(maxPetalId);
                    graph.k--;

                    cleanedNodes.addAll(graph.solution);
                    graph.solution.clear();

                    Set<Integer> newDeletedNodes = graph.cleanGraph();
                    if (newDeletedNodes == null) {
                        return null;
                    }
                    cleanedNodes.addAll(newDeletedNodes);
                    cleanedNodes.addAll(preClean);

                } else {
                    graph.rebuildGraph();
                }
            }
            if (k_before > graph.k) {
                petal_suc++;
            }
            petal_time += (time + System.nanoTime());
        }

        if (graph.k < 0) {
            return null;
        }

        if (graph.k > 4 && recursions % 5 == 0) {
            digraph++;
            long time = -System.nanoTime();
            int k_before = graph.k;

            packing = new Packing(graph);
            packing.getDigraphs();
            Set<Integer> safeDiGraphNodes = packing.getSafeToDeleteDigraphNodes();
            if (safeDiGraphNodes.size() > graph.k) {
                digraph_time += (time + System.nanoTime());
                return null;
            }
            if (!safeDiGraphNodes.isEmpty()) {
                graph.k = graph.k - safeDiGraphNodes.size();
                graph.removeAllNodes(safeDiGraphNodes);
                cleanedNodes.addAll(safeDiGraphNodes);

                Set<Integer> newDeletedNodes = graph.cleanGraph();
                if (newDeletedNodes == null) {
                    return null;
                }
                cleanedNodes.addAll(newDeletedNodes);
                if (k_before > graph.k) {
                    digraph_suc++;
                }

                cleanedNodes.addAll(graph.solution);
                graph.solution.clear();
            }
            digraph_time += (time + System.nanoTime());
        }

        if (graph.k < 0) {
            return null;
        }

        if (packing == null) {
            packing = new Packing(graph);
        }
        if (Math.max(packing.findCyclePacking().size(), packing.lowerDigraphBound()) > graph.k) {
            return null;
        }

        fullDigraphs = graph.cleanDigraphSet(fullDigraphs);
        List<Integer> digraph = null;
        if (!fullDigraphs.isEmpty()) {
            digraph = fullDigraphs.iterator().next();
        }
        if (digraph != null && digraph.size() > 1) {
            for (Integer v : digraph) {
                // delete all vertices except one
                graph.addStackCheckpoint();
                Set<Integer> digraphWithoutV = new HashSet<>(digraph);
                digraphWithoutV.remove(v);
                graph.removeAllNodes(digraphWithoutV);

                // branch with a maximum cost of k
                // -1: Just deleted a node
                // -cleanedNodes.size(): nodes that where removed during graph reduction
                int kPrev = graph.k;
                graph.k = graph.k - digraphWithoutV.size();
                Set<Integer> dfvs = dfvsBranch(graph);

                // if there is a valid solution in the recursion it will be returned
                if (dfvs != null) {

                    // Add the nodeId of the valid solution and all cleanedNodes
                    dfvs.addAll(digraphWithoutV);
                    dfvs.addAll(cleanedNodes);

                    return dfvs;
                } else {
                    graph.rebuildGraph();
                    graph.k = kPrev;
                    graph.getNode(v).fixNode();
                }
            }
        } else {
        // find a Cycle
        graph.solution.clear();
        Deque<Integer> cycle = graph.findBestCycle();
        cleanedNodes.addAll(graph.solution);
        graph.solution.clear();

        if (cycle == null && graph.k >= 0) {
            // The graph does not have any cycles
            // We will return the found cleanedNodes
            return cleanedNodes;
        }

        if (cycle == null || graph.k <= 0) {
            return null;
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
                int kPrev = graph.k;
                graph.k--;


                // branch with a maximum cost of k
                // -1: Just deleted a node
                // -cleanedNodes.size(): nodes that where removed during graph reduction
                Set<Integer> dfvs = dfvsBranch(graph);

                // if there is a valid solution in the recursion it will be returned
                if (dfvs != null) {

                    // Add the nodeId of the valid solution and all cleanedNodes
                    dfvs.add(v);
                    dfvs.addAll(cleanedNodes);

                    return dfvs;
                } else {
                    graph.rebuildGraph();
                    graph.k = kPrev;
                    graph.getNode(v).fixNode();
                }
            }
        }
        }
        return null;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph) {
        Packing packing = new Packing(graph);
        packing.getDigraphs();
        Set<Integer> newDeletedNodes = packing.getSafeToDeleteDigraphNodes();
        graph.removeAllNodes(newDeletedNodes);
        Set<Integer> allDfvs = new HashSet<>(newDeletedNodes);
        while (!newDeletedNodes.isEmpty()) {
            packing = new Packing(graph);
            packing.getDigraphs();
            newDeletedNodes = packing.getSafeToDeleteDigraphNodes();
            graph.removeAllNodes(newDeletedNodes);
            allDfvs.addAll(newDeletedNodes);
        }

        graph.k = Integer.MAX_VALUE;
        allDfvs.addAll(graph.cleanGraph());
        graph.k = 0;

        graph.clearStack();
        Set<DirectedGraph> SCCs = new Tarjan(graph).getSCCGraphs();
        graph.clearStack();

        for (DirectedGraph scc : SCCs) {

            Packing sccPacking = new Packing(scc);
            int k = sccPacking.findCyclePacking().size();
            fullDigraphs = sccPacking.getDigraphs();

            Set<Integer> dfvs = null;

            while (dfvs == null) {
                scc.addStackCheckpoint();
                scc.k = k;
                dfvs = dfvsBranch(scc);
                k++;
                scc.rebuildGraph();
                scc.unfixAll();
            }
            allDfvs.addAll(dfvs);
        }
        return allDfvs;
    }

    public static void developMain(String file) {
        int opt_sol = solSizes.get(file.substring(file.lastIndexOf('/') + 1));
        DirectedGraph graph = new DirectedGraph(file);
        graph.clearStack();
        System.out.println("Solving: " + file);
        long time = -System.nanoTime();
        int k = dfvsSolve(graph).size();
        if (k != opt_sol) {
            System.out.println("ERROR!\tcorrect solution: " + opt_sol);
        }
        System.out.println("\tk: " + k);
        System.out.println("\t#recursive steps: " + recursions);
        //System.out.println("\t#petal calcs: " + petal + " - " + petal_suc);
        //System.out.println("\t\ttime: " + utils.round(petal_time / 1_000_000_000.0, 4));
        //System.out.println("\t#digraph calcs: " + digraph + " - " + digraph_suc);
        //System.out.println("\t\ttime: " + utils.round(digraph_time / 1_000_000_000.0, 4));
        double sec = utils.round((time + System.nanoTime()) / 1_000_000_000.0, 4);
        System.out.println("\ttime: " + sec);
        recursions = 0;
        petal = 0;
        digraph = 0;
        petal_suc = 0;
        digraph_suc = 0;
        petal_time = 0;
        digraph_time = 0;
    }

    public static void productionMain(String args) {
        DirectedGraph graph = new DirectedGraph(args);
        Set<Integer> solution = dfvsSolve(graph);
        for (int i : solution) {
            System.out.println(i);
        }
        System.out.println("#recursive steps: " + recursions);
    }

    public static void main(String[] args) {
/*
        solSizes = utils.loadSolSizes();

        long total_time = -System.nanoTime();
        developMain("instances/complex/chess-n_1000"); // 60
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
        developMain("instances/synthetic/synth-n_90-m_327-k_30-p_0.05.txt"); // 14


        developMain("instances/synthetic/synth-n_160-m_683-k_8-p_0.05.txt"); //6

        developMain("instances/synthetic/synth-n_50-m_357-k_20-p_0.2.txt");//20
        developMain("instances/synthetic/synth-n_140-m_1181-k_20-p_0.1.txt"); //20

        //developMain("instances/synthetic/synth-n_120-m_492-k_30-p_0.05.txt"); //21
        developMain("instances/synthetic/synth-n_80-m_444-k_25-p_0.1.txt"); //20


        //developMain("instances/synthetic/synth-n_70-m_342-k_30-p_0.1.txt"); //19

        //System.out.println("\nTotal time: " + utils.round((total_time + System.nanoTime()) / 1_000_000_000.0, 4) + "sec");
*/
        productionMain(args[0]);
    }
}
/*
Correct in:
Total time: 32.5907sec
 */