import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {

    public static int recursions;
    public static Set<List<Integer>> fullDigraphs;
    public static Map<String, Integer> solSizes;
    public static Map<String, List<Double>> timeMap;
    public static Map<String, List<Integer>> recMap;
    private static int petalK;
    private static int petalRec;
    private static boolean petalEnabled;
    private static int costK;
    private static int costRec;
    private static boolean costEnabled;
    private static boolean interrupt;

    public static Set<Integer> dfvsBranch(DirectedGraph graph) {

        if (interrupt) {
            return null;
        }
        // increment recursions to keep track of tree size
        recursions++;

        // clean the graph if the budget limit is reached we can break here
        Set<Integer> cleanedNodes = graph.cleanGraph();

        if (cleanedNodes == null) {
            return null;
        }

        Packing packing = null;
        if (petalEnabled && graph.k > petalK && recursions % petalRec == 0) { // 3 && graph.k <= Math.sqrt(graph.nodeMap.size()) - 1) {
            int maxPetal = -1;
            int maxPetalId = -1;
            Set<Integer> petalSet = null;
            for (Integer nodeId : new HashSet<>(graph.nodeMap.keySet())) {
                // all petals will be greater than 0 after the cleaning cycle.
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
                graph.cleanGraph();
                //graph.k = tempK - preClean.size();
                //if (!preClean.isEmpty()) System.out.println("hier clean: " + preClean.size());
                if (tempK - packing.findCyclePacking().size() + graph.solution.size() < maxPetal) {

                    graph.rebuildGraph();
                    graph.k = tempK;
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

                } else {
                    graph.rebuildGraph();
                    graph.k = tempK;
                }
            }
        }

        if (graph.k < 0) {
            return null;
        }

        if (costEnabled && graph.k > costK && recursions % costRec == 0) {
            long time = -System.nanoTime();

            packing = new Packing(graph);
            packing.getDigraphs();
            Set<Integer> safeDiGraphNodes = packing.getSafeToDeleteDigraphNodes();
            if (safeDiGraphNodes.size() > graph.k) {
                return null;
            }
            if (!safeDiGraphNodes.isEmpty()) {
                graph.k = graph.k - safeDiGraphNodes.size();
                graph.removeAllNodes(safeDiGraphNodes);
                cleanedNodes.addAll(safeDiGraphNodes);

                Set<Integer> newDeletedNodes = graph.cleanGraph();
                if (newDeletedNodes == null || graph.k < 0) {
                    return null;
                }
                cleanedNodes.addAll(newDeletedNodes);

                cleanedNodes.addAll(graph.solution);
                graph.solution.clear();
            }
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

        Set<List<Integer>> remainingDigraphs = graph.cleanDigraphSet(fullDigraphs);
        List<Integer> digraph = null;
        if (!remainingDigraphs.isEmpty()) {
            digraph = remainingDigraphs.iterator().next();
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

            while (dfvs == null && !interrupt) {
                scc.addStackCheckpoint();
                scc.k = k;
                dfvs = dfvsBranch(scc);
                k++;
                scc.rebuildGraph();
                scc.unfixAll();
            }

            if (interrupt) {
                return null;
            }
            allDfvs.addAll(dfvs);
        }
        return allDfvs;
    }

    public static void developMain(String file) {
        String name = file.substring(file.lastIndexOf('/') + 1);
        int opt_sol = solSizes.get(name);
        DirectedGraph graph = new DirectedGraph(file);
        graph.clearStack();
        long time = -System.nanoTime();
        int k;
        try {
            k = dfvsSolve(graph).size();
        } catch (NullPointerException e) {
            //System.out.println(name + "\tTimelimit");
            timeMap.get(name).add(20.0);
            recMap.get(name).add(recursions);
            recursions = 0;
            return;
        }
        if (k != opt_sol) {
            //System.err.println("\nERROR!\tcorrect solution: " + opt_sol + "\n");
        }
        //System.out.println("\t#petal calcs: " + petal + " - " + petal_suc);
        //System.out.println("\t\ttime: " + utils.round(petal_time / 1_000_000_000.0, 4));
        //System.out.println("\t#digraph calcs: " + digraph + " - " + digraph_suc);
        //System.out.println("\t\ttime: " + utils.round(digraph_time / 1_000_000_000.0, 4));
        double sec = utils.round((time + System.nanoTime()) / 1_000_000_000.0, 4);
        timeMap.get(name).add(sec);
        recMap.get(name).add(recursions);
        //System.out.println(name + "\tk: " + k + "\t#recursive steps: " + recursions + "\ttime: " + sec);
        recursions = 0;
    }

    public static void productionMain(String args) {
        petalK = 4;
        petalRec = 16;
        costRec = 49;
        costK = 9;
        petalEnabled = true;
        costEnabled = true;

        DirectedGraph graph = new DirectedGraph(args);
        Set<Integer> solution = dfvsSolve(graph);
        for (int i : solution) {
            System.out.println(i);
        }
        System.out.println("#recursive steps: " + recursions);
    }

    public static void main(String[] args) {


        solSizes = utils.loadSolSizes();

        rootLowerbounds();

        //productionMain(args[0]);
    }

    private static void parameterOptimization() {
        // fill Maps
        timeMap = new TreeMap<>();
        recMap = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("instances/run_me.txt"));
            String line = br.readLine();
            while (line != null) {
                line = line.split("\t")[0];
                timeMap.put(line.substring(line.lastIndexOf('/') + 1), new ArrayList<>());
                recMap.put(line.substring(line.lastIndexOf('/') + 1), new ArrayList<>());
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        petalK = 4;
        petalRec = 16;
        costRec = 49;
        costK = 9;
        petalEnabled = true;
        costEnabled = true;
        for (int i : List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)) {
            petalK = i;
            System.out.println("petalK: " + petalK + " | petalRec: " + petalRec + " | costK: " + costK + " | costRec: " + costRec);
            run5kRec();
        }
        /*
        for (int i : List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16)) {
            costK = i;
            System.out.println("petalK: " + petalK + " | petalRec: " + petalRec + " | costK: " + costK + " | costRec: " + costRec);
            run5kRec();
        }

         */
        printTimeMap(timeMap);
    }

    private static void rootLowerbounds() {
        File files = new File("instances/all/");
        for (File file : files.listFiles()) {
            DirectedGraph graph = new DirectedGraph(file.getPath());

            String name = file.getPath().substring(14);

            System.out.print(name);

            graph.k = Integer.MAX_VALUE;
            Set<Integer> allDfvs = new HashSet<>(graph.cleanGraph());
            graph.k = 0;

            Packing packing = new Packing(graph);
            packing.getDigraphs();
            Set<Integer> newDeletedNodes = packing.getSafeToDeleteDigraphNodes();
            graph.removeAllNodes(newDeletedNodes);
            allDfvs.addAll(newDeletedNodes);
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

            int lowerbound = allDfvs.size();
            for (DirectedGraph scc : SCCs) {

                Packing sccPacking = new Packing(scc);
                int k = sccPacking.findCyclePacking().size();
                lowerbound += k;
            }
            System.out.println(";" + solSizes.get(name) + ";" + lowerbound);
        }
    }

    private static void printTimeMap(Map<String, List<Double>> map) {
        for (String key : map.keySet()) {
            List<Double> times = map.get(key);
            System.out.print(key);
            for (Double time : times) {
                System.out.print("\t" + time);
            }
            System.out.println();
        }
    }

    private static void run5kRec() {
        Stack<String> files = new Stack<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("instances/run_me.txt"));
            String line = br.readLine();
            while (line != null) {
                line = line.split("\t")[0];
                files.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread t1 = new Thread(() -> {
            while (!files.isEmpty()) {
                String finalLine = files.pop();
                Thread t = new Thread(() -> {
                    developMain(finalLine);
                });
                t.start();
                try {
                    t.join(20_000); //Time-Limit
                    if (t.isAlive()) {

                        interrupt = true;
                        t.join();
                        interrupt = false;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread t2 = new Thread(() -> {
            while (!files.isEmpty()) {
                String finalLine = files.pop();
                Thread t = new Thread(() -> {
                    developMain(finalLine);
                });
                t.start();
                try {
                    t.join(20_000); //Time-Limit
                    if (t.isAlive()) {

                        interrupt = true;
                        t.join();
                        interrupt = false;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
/*
Correct in:
Total time: 32.5907sec
 */