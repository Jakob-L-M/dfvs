package Graph;

import Utilities.*;
import Utilities.Timer;

import java.io.*;
import java.util.*;

public class Main {

    private static final int petalK = 4;
    private static final int petalRec = 16;
    private static final boolean petalEnabled = true;
    private static final int costK = 9;
    private static final int costRec = 49;
    private static final boolean costEnabled = true;
    public static int recursions;
    public static long rootTime;
    public static long cleaningTime;
    public static long packingTime;
    public static long flowerTime;
    public static long digraphTime;
    public static int firstLowerbound;
    public static List<Set<Integer>> fullDigraphs;
    public static Map<String, Integer> solSizes;
    public static Map<String, List<Double>> timeMap;
    public static Map<String, List<Integer>> recMap;
    private static boolean interrupt;

    public static Set<Integer> dfvsBranch(DirectedGraph graph, boolean needsClean) {

        if (interrupt) {
            return null;
        }
        // increment recursions to keep track of tree size
        recursions++;

        // clean the graph if the budget limit is reached we can break here
        Set<Integer> cleanedNodes = new HashSet<>();
        if (needsClean) {
            cleanedNodes = graph.cleanGraph();

            if (cleanedNodes == null) {
                return null;
            }

            if (graph.k < 0) {
                return null;
            }
        }

        Packing packing = new Packing(graph);

        if (packing.newFindCyclePacking(6, 2, Integer.MAX_VALUE).size() > graph.k) {
            return null;
        }

        Set<Integer> digraph = null;
        List<Set<Integer>> remainingDigraphs = null;
        if (fullDigraphs != null) {
            remainingDigraphs = graph.cleanDigraphSet(fullDigraphs);
        }
        if (remainingDigraphs != null && !remainingDigraphs.isEmpty()) {
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
                Set<Integer> dfvs = dfvsBranch(graph, true);

                // if there is a valid solution in the recursion it will be returned
                if (dfvs != null) {

                    // Add the nodeId of the valid solution and all cleanedNodes
                    dfvs.addAll(digraphWithoutV);
                    dfvs.addAll(cleanedNodes);

                    return dfvs;
                } else {
                    graph.rebuildGraph();
                    graph.k = kPrev;
                }
            }
        } else {
            // find a Cycle
            graph.quickClean(new HashSet<>(graph.nodeMap.keySet()));
            Deque<Integer> cycle = graph.findBestCycle();

            if (cycle == null && graph.k >= 0) {
                // The graph does not have any cycles
                // We will return the found cleanedNodes
                return cleanedNodes;
            }

            if (cycle == null || graph.k <= 0) {
                return null;
            }


            Map<Integer, List<Integer>> sortedCycle = new TreeMap<>();
            for (Integer v : cycle) {
                int value = graph.getNode(v).getInDegree() * graph.getNode(v).getOutDegree();
                if (!sortedCycle.containsKey(value)) {
                    sortedCycle.put(value, new ArrayList<>());
                }
                sortedCycle.get(value).add(v);
            }
            List<Integer> branchCycle = new ArrayList<>(sortedCycle.keySet());
            branchCycle.sort(Collections.reverseOrder());


            for (Integer i : branchCycle) {

                for (Integer nodeToDelete : sortedCycle.get(i)) {

                    // delete a vertex of the circle and branch for here

                    graph.addStackCheckpoint();

                    Set<Integer> neighbours = new HashSet<>(graph.nodeMap.get(nodeToDelete).getInNodes());
                    neighbours.addAll(graph.nodeMap.get(nodeToDelete).getOutNodes());
                    graph.removeNode(nodeToDelete);
                    graph.quickClean(neighbours);

                    int kPrev = graph.k;
                    graph.k--;


                    // branch with a maximum cost of k
                    // -1: Just deleted a node
                    // -cleanedNodes.size(): nodes that where removed during graph reduction
                    Set<Integer> dfvs = dfvsBranch(graph, false);

                    // if there is a valid solution in the recursion it will be returned
                    if (dfvs != null) {

                        // Add the nodeId of the valid solution and all cleanedNodes
                        dfvs.add(nodeToDelete);
                        dfvs.addAll(cleanedNodes);

                        return dfvs;
                    } else {
                        graph.rebuildGraph();
                        graph.k = kPrev;
                    }
                }
            }
        }
        return null;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph, boolean isScc) {
        Packing p = new Packing(graph);
        int k = p.newFindCyclePacking(5, 2, Integer.MAX_VALUE).size();

        fullDigraphs = p.getDigraphs();

        Set<Integer> dfvs = null;
        while (dfvs == null) {
            graph.addStackCheckpoint();
            graph.k = k;
            dfvs = dfvsBranch(graph, true);
            k++;
            graph.rebuildGraph();
        }
        return dfvs;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph) {
        graph.k = Integer.MAX_VALUE;
        Set<Integer> allDfvs = new HashSet<>(graph.cleanGraph());
        graph.k = 0;

        Packing packing = new Packing(graph);
        Set<Integer> safeDigraphs = packing.getSafeToDeleteDigraphNodes(true);
        graph.removeAllNodes(safeDigraphs);
        allDfvs.addAll(safeDigraphs);

        graph.k = Integer.MAX_VALUE;
        allDfvs.addAll(graph.cleanGraph());
        graph.k = 0;

        //System.out.println(graph);

        graph.clearStack();
        Set<DirectedGraph> SCCs = new Tarjan(graph).getSCCGraphs();
        graph.clearStack();
        rootTime += System.nanoTime();
        for (DirectedGraph scc : SCCs) {

            allDfvs.addAll(scc.cleanGraph());

            Packing sccPacking = new Packing(scc);
            int k = sccPacking.newFindCyclePacking().size();
            if (k == 0) {
                scc.cleanGraph();
            }
            List<Set<Integer>> digraphs = sccPacking.getDigraphs();

            Set<Integer> dfvs = null;

            while (dfvs == null && !interrupt) {
                fullDigraphs = new ArrayList<>(digraphs);
                scc.addStackCheckpoint();
                scc.k = k;
                dfvs = dfvsBranch(scc, true);
                k++;
                scc.rebuildGraph();
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
            timeMap.get(name).add(20.0);
            recMap.get(name).add(recursions);
            recursions = 0;
            return;
        }
        if (k != opt_sol) {
            System.err.println("\nERROR!\tcorrect solution: " + opt_sol + "\n");
        }
        double sec = utils.round((time + System.nanoTime()) / 1_000_000_000.0, 4);
        System.out.println(name + "\tk: " + k + "\t#recursive steps: " + recursions + "\ttime: " + sec);
        recursions = 0;
    }

    public static void productionMain(String args) {
        DirectedGraph graph = new DirectedGraph(args);

        graph.k = Integer.MAX_VALUE;
        Set<Integer> solution = graph.cleanGraph();

        Packing packing = new Packing(graph);
        Set<Integer> safeToDeleteDigraph = packing.getSafeToDeleteDigraphNodes(true);

        solution.addAll(safeToDeleteDigraph);
        graph.removeAllNodes(safeToDeleteDigraph);

        solution.addAll(graph.cleanGraph());

        System.out.println("Finished Root - " + safeToDeleteDigraph.size() + " - " + solution.size());

        Model m = utils.loadMatrix("mat.txt");
        TimerTuple r = graph.heuristicSolution(0, 1, m, 0.1);
        Set<Integer> res = r.getSolution();
        Utilities.Timer t = r.getTimer();

        solution.addAll(res);

        for (Integer i : solution) {
            //System.out.println(i);
        }
        System.out.println(solution.size() + " " + t);
        // Best known: 15417
        // Current: 16633
    }

    public static void main(String[] args) throws IOException {

        //productionMain("instances/sheet5-heuristic/h_119");
        // #### DEVELOP ONLY ####

        heuristic();

        // All e_ Instances

        // v6
        // Took: 1:34.65min, in total off by: -5485 while total k's: 1094721

        // With 5k Packing Limit:
        // Took: 2:2.26min, in total off by: -8148 while total k's: 1094721

        //createGraphData("instances/all_instances.txt", "results/reduction_all.csv");

    }

    private static void nodesMeta() throws IOException {
        String fileToRun = "instances/all_instances.txt";
        String fileToSave = "./results/nodes_v5.csv";

        Map<String, List<Integer>> solutions = utils.loadSolutions();
        System.out.println(solutions.get("sheet5-heuristic/e_005"));

        BufferedReader br = new BufferedReader(new FileReader(fileToRun));

        BufferedWriter bw = new BufferedWriter(new FileWriter(fileToSave));

        bw.write("instance,w,nodeId,1,2,3,4,5,6,7,8,9,10,11\n");
        String line;
        while ((line = br.readLine()) != null) {
            String name = line.substring(line.indexOf("instances/") + 10);
            if (!solutions.containsKey(name)) continue;
            System.out.println(name);
            createNodeData(15, solutions, bw, line, name);
        }
    }

    private static void heuristic() throws IOException {
        int totalInst = 4512;
        int c = 0;

        Model m = utils.loadMatrix("./mat.txt");

        int kSum = 0;
        long time = -System.nanoTime();
        Utilities.Timer mainTimer = new Timer(6, new String[]{"packing", "predData", "predictions", "cleaning", "dfvsSolve", "tarjan"});
        for (File file : Objects.requireNonNull(new File("instances/instances/").listFiles())) {
            c++;

            String name = file.getName();
            String line = file.getPath();

            long t = -System.nanoTime();

            DirectedGraph g = new DirectedGraph(line);
            g.k = Integer.MAX_VALUE;

            Set<Integer> solution = new HashSet<>(g.cleanGraph());

            Packing p = new Packing(g);
            Set<Integer> safeDigraphs = p.getSafeToDeleteDigraphNodes(true);
            g.removeAllNodes(safeDigraphs);
            solution.addAll(g.cleanGraph());

            solution.addAll(safeDigraphs);
            g.k = 0;
            TimerTuple res = g.heuristicSolution(0, 1, m, 0.0);
            mainTimer.addTimer(res.getTimer());
            solution.addAll(res.getSolution());
            int k = solution.size();
            double s = utils.getSeconds(t, 4);
            g.safeSolution(solution);
            System.out.print(c + "/" + totalInst + "\t" + name + "\t" + k);
            System.out.println("\t" + res.getTimer());
        }


        double sec = utils.round((double) (time + System.nanoTime()) / 1_000_000_000, 2);
        System.out.println("Took: " + (int) Math.floor(sec / 60) + ":" + utils.round(sec % 60, 2) + "min");
        System.out.println("Total Time dist: " + mainTimer);

        Verifier.verifyAndUpdate();
        //createGraphData(fileToRun, fileToSave);

        //runHeuristic(20, fileToRun, fileToSave);

        //runHeuristic(20, "instances/sheet5_instances.txt", "./graph-metadata/random_all_2.csv");

        // #### DEVELOP ONLY ####
    }

    private static void runHeuristic(int iterations, String fileToRun, String fileToSave) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(fileToSave));

        solSizes = utils.loadSolSizes();

        bw.write("instance");
        for (int i = 0; i < iterations; i++) {
            bw.write(",k" + i);
        }
        bw.write(",maxTime\n");

        BufferedReader br = new BufferedReader(new FileReader(fileToRun));
        int numberOfInstances = (int) br.lines().count();
        int c = 0;
        br = new BufferedReader(new FileReader(fileToRun));
        String instance = br.readLine();

        //Model m = Utilities.utils.loadMatrix("graph-metadata/synth_mat_v2.txt");

        while (instance != null) {
            c++;

            String name = instance.substring(instance.indexOf("instances/") + 10);

            System.out.println(c + "/" + numberOfInstances + ": " + name);

            long maxTime = 0;
            int bestK = Integer.MAX_VALUE;
            bw.write(name);
            System.out.print("\t\t|");
            for (int j = 0; j < iterations; j++) {
                System.out.print("-");
            }
            System.out.print("|");
            for (int i = 0; i < iterations; i++) {
                long time = -System.nanoTime();

                // Put Heuristic here
                int k = randomHeuristic(instance);

                time += System.nanoTime();
                if (time > maxTime) {
                    maxTime = time;
                }
                bw.write("," + k);

                // Progress Bar
                for (int j = 0; j < iterations + 1; j++) {
                    System.out.print("\b");
                }
                for (int j = 0; j <= i; j++) {
                    System.out.print("█");
                }
                for (int j = i + 1; j < iterations; j++) {
                    System.out.print("-");
                }
                System.out.print("|");

                if (k < bestK) {
                    bestK = k;
                }
                //Progress Bar end

            }
            double mTime = utils.round((double) maxTime / 1_000_000_000, 4);
            bw.write("," + mTime + "\n");
            bw.flush();
            System.out.print("\t max time: " + mTime + "sec");
            System.out.print("\tbestK: " + bestK);
            if (solSizes.containsKey(name) && solSizes.get(name) != -1) {
                System.out.print("\toptK:" + solSizes.get(name));
            }
            System.out.println();


            //createNodeData(iterations, solutions, bw, instance, name);
            instance = br.readLine();
        }

        bw.close();
    }

    private static void createNodeData(int iterations, Map<String, List<Integer>> solutions, BufferedWriter bw, String instance, String name) throws IOException {
        DirectedGraph g = new DirectedGraph(instance);
        g.k = Integer.MAX_VALUE;
        g.cleanGraph();
        Packing p = new Packing(g);
        g.removeAllNodes(p.getSafeToDeleteDigraphNodes(true));
        if (!solutions.containsKey(name)) return;
        List<Integer> sol = new ArrayList<>(solutions.get(name));
        System.out.print(" " + sol.size());
        for (int i = 0; i < iterations; i++) {
            int b = new Random().nextInt(Math.max(1, sol.size() - 2));
            Collections.shuffle(sol);
            for (int j = 0; j < b; j++) {
                g.removeNode(sol.get(j));
            }
            g.cleanGraph();
            g.extractNodeMetaData(bw);
        }
    }
    /*
    private static void runILP(String file, double timelimit) throws IOException, GRBException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String instance = br.readLine();

        System.out.println("instance;k;fileTime;cleanTime;digraphTime;totalTime;solution");
        while (instance != null) {
            instance = instance.split("\t")[0];
            long time = -System.nanoTime();

            String instanceName = instance.substring(instance.indexOf("instances/") + 10);
            String name = instanceName.substring(0, instanceName.indexOf("/"));

            DirectedGraph graph = new DirectedGraph(instance);

            long cleanTime = -System.nanoTime();
            Set<Integer> cleanedNodes = graph.rootClean();
            double cleanSec = Utilities.utils.round((cleanTime + System.nanoTime()) / 1_000_000_000.0, 4);

            long digraphTime = -System.nanoTime();
            Packing packing = new Packing(graph);

            List<Set<Integer>> digraphs = packing.getDigraphs();
            Set<Integer> safeToDeleteDigraph = packing.getSafeToDeleteDigraphNodes();

            cleanedNodes.addAll(safeToDeleteDigraph);
            graph.removeAllNodes(safeToDeleteDigraph);

            digraphs = graph.cleanDigraphSet(digraphs);

            cleanedNodes.addAll(graph.rootClean());

            if (graph.nodeMap.size() > 1000 || graph.nodeMap.isEmpty()) {
                instance = br.readLine();
                continue;
            }

            double digraphSec = Utilities.utils.round((digraphTime + System.nanoTime()) / 1_000_000_000.0, 4);

            long fileTime = -System.nanoTime();

            int kClean = cleanedNodes.size();
            List<Integer> k = new ArrayList<>();
            double fileSec = 0.0;
            if (graph.nodeMap.size() > 0) {
                graph.createTopoLPFile(digraphs);
                fileSec = Utilities.utils.round((fileTime + System.nanoTime()) / 1_000_000_000.0, 4);

                k = ilp("ILPs/" + instanceName, Math.max(0.001, timelimit - fileSec - digraphSec));
            }
            System.out.print(instance);
            if (k == null) {
                System.out.println(";-1;" + fileSec + ";" + cleanSec + ";" + digraphSec + ";" + timelimit + ";[]");
            } else {

                double sec = Utilities.utils.round((time + System.nanoTime()) / 1_000_000_000.0, 4);
                System.out.println(";" + (k.size() + kClean) + ";" + fileSec + ";" + cleanSec + ";" + digraphSec + ";" + sec + ";" + k);
            }
            instance = br.readLine();
            //break;
        }
    }
    */

    private static int randomHeuristic(String file) {
        DirectedGraph g = new DirectedGraph(file);
        g.k = Integer.MAX_VALUE;
        Set<Integer> k = g.cleanGraph();

        Packing p = new Packing(g);
        p.getDigraphs();
        Set<Integer> safeToDelete = p.getSafeToDeleteDigraphNodes(true);
        if (!safeToDelete.isEmpty()) {
            g.removeAllNodes(safeToDelete);
            k.addAll(safeToDelete);
            k.addAll(g.rootClean());
        }

        while (!g.nodeMap.isEmpty()) {
            Set<Integer> set = g.nodeMap.keySet();
            int index = new Random().nextInt(set.size());
            Iterator<Integer> it = set.iterator();
            for (int i = 0; i < index; i++) {
                it.next();
            }
            int nodeToDelete = it.next();

            Set<Integer> neighbours = new HashSet<>(g.nodeMap.get(nodeToDelete).getOutNodes());
            neighbours.addAll(g.nodeMap.get(nodeToDelete).getInNodes());
            g.removeNode(nodeToDelete);
            k.add(nodeToDelete);
            g.quickClean(neighbours);
        }
        return k.size();
    }

    private static void createGraphData(String fileFrom, String fileTo) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileFrom));
            BufferedWriter bw = new BufferedWriter(new FileWriter((fileTo)));
            bw.write("instance,n_before,n_after,time");
            String instance;
            while ((instance = br.readLine()) != null) {
                String name = instance.substring(instance.indexOf("instances/") + 10);
                DirectedGraph g = new DirectedGraph(instance);
                g.k = Integer.MAX_VALUE;
                bw.write(name);
                bw.write("," + g.nodeMap.size());
                long time = -System.nanoTime();
                g.cleanGraph();
                Packing p = new Packing(g);
                p.getDigraphs();
                g.removeAllNodes(p.getSafeToDeleteDigraphNodes());
                g.rootClean(null, true);

                bw.write("," + g.nodeMap.size());
                bw.write("," + utils.getSeconds(time, 4));

                System.out.println(name);
                bw.write("\n");
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}