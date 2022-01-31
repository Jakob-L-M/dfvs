import gurobi.*;

import java.io.*;
import java.util.*;

public class Main {

    public static int recursions;
    public static long rootTime;
    public static long cleaningTime;
    public static long packingTime;
    public static long flowerTime;
    public static long digraphTime;
    public static int firstLowerbound;
    public static List<Set<Integer>> fullDigraphs;
    public static Map<String, Integer> solSizes;
    public static Map<String, Integer> solSizes3;
    public static Map<String, List<Double>> timeMap;
    public static Map<String, List<Integer>> recMap;
    public static GRBEnv env;
    private static final int petalK = 4;
    private static final int petalRec = 16;
    private static final boolean petalEnabled = true;
    private static final int costK = 9;
    private static final int costRec = 49;
    private static final boolean costEnabled = true;
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

        List<Set<Integer>> remainingDigraphs = graph.cleanDigraphSet(fullDigraphs);
        Set<Integer> digraph = null;
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
        rootTime = -System.nanoTime();
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
        rootTime += System.nanoTime();
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

    public static void productionMain(String args) throws GRBException {
        DirectedGraph graph = new DirectedGraph(args);

        Set<Integer> cleanedNodes = graph.rootClean();

        Packing packing = new Packing(graph);

        List<Set<Integer>> digraphs = packing.getDigraphs();
        Set<Integer> safeToDeleteDigraph = packing.getSafeToDeleteDigraphNodes();

        cleanedNodes.addAll(safeToDeleteDigraph);
        graph.removeAllNodes(safeToDeleteDigraph);

        cleanedNodes.addAll(graph.rootClean());

        digraphs = graph.cleanDigraphSet(digraphs);

        List<Integer> k = new ArrayList<>();
        if (graph.nodeMap.size() > 0) {
            String filename = "./" + args.substring(args.lastIndexOf('/') + 1) + ".lp";
            graph.createTopoLPFile(filename, digraphs);
            k = ilp(filename, Double.MAX_VALUE);
        }
        if (k != null) {
            for (Integer i : k) {
                System.out.println(i);
            }
            for (Integer i : cleanedNodes) {
                System.out.println(i);
            }
        }
    }

    public static void main(String[] args) throws IOException, GRBException {

/*
        // #### GUROBI ####
        System.out.print("# ");

        env = new GRBEnv();
        env.set(GRB.IntParam.OutputFlag, 0);
        double timelimit = 180.0;
        env.set(GRB.DoubleParam.TimeLimit, timelimit);
        env.start();

        // #### GUROBI END ####

 */
        //productionMain(args[0]);

        // #### DEVELOP ONLY ####
        int iterations = 10;
        String fileToRun = "instances/all_instances.txt";
        String fileToSave = "./graph-metadata/synth_net_v2.csv";
        solSizes = utils.loadSolSizes();
        solSizes3 = utils.loadSolSizes3();

        /*
        BufferedWriter bw = new BufferedWriter(new FileWriter("./graph-metadata/vertices.csv"));

        bw.write("instance,week,nodeId,inDegree,outDegree,maxInOut,minInOut," +
                "inInDegree,inOutDegree,outOutDegree,outInDegree," +
                "biDirDegree,noBiInDegree,noBiOutDegree,petal3,relInPos,relOutPos");
        bw.write("\n");
        */
        BufferedWriter bw = new BufferedWriter(new FileWriter(fileToSave));

        bw.write("instance");
        for (int i = 0; i < iterations; i++) {
            bw.write(",k" + i);
        }
        bw.write(",maxTime\n");

        BufferedReader br = new BufferedReader(new FileReader(fileToRun));
        int numberOfInstances = (int)br.lines().count();
        int c = 0;
        br = new BufferedReader(new FileReader(fileToRun));
        String instance = br.readLine();

        Model m = utils.loadMatrix("graph-metadata/synth_mat_v2.txt");

        while (instance != null) {
            c++;

            String name = instance.substring(instance.indexOf("instances/")+10);
            System.out.println(c + "/" + numberOfInstances + ": " +  name);

            long maxTime = 0;
            bw.write(name);
            System.out.print("\t\t|");
            for (int j = 0; j < iterations; j++) {
                System.out.print("-");
            }
            System.out.print("|");
            for (int i = 0; i < iterations; i++) {
                long time = -System.nanoTime();

                // Put Heuristic here
                int k = NNHeuristic(instance, m);

                time += System.nanoTime();
                if (time > maxTime) {
                    maxTime = time;
                }
                bw.write(","+ k);

                // Progress Bar
                for (int j = 0; j < iterations + 1; j++) {
                    System.out.print("\b");
                }
                for (int j = 0; j <= i; j++) {
                    System.out.print("█");
                }
                for (int j = i+1; j < iterations; j++) {
                    System.out.print("-");
                }
                System.out.print("|");
                //Progress Bar end

            }
            double mTime = utils.round((double)maxTime/1_000_000_000, 4);
            bw.write("," + mTime + "\n");
            bw.flush();
            System.out.println("\t max time: " + mTime + "sec");


            //createNodeData(iterations, solutions, bw, instance, name);
            instance = br.readLine();
        }

        bw.close();

        //System.out.println("Validation passed");




        //runILP(instanceFile, timelimit);
        // #### VALIDATE INSTANCES ####

        // #### DEVELOP ONLY ####


    }

    private static void createNodeData(int iterations, Map<String, List<Integer>> solutions, BufferedWriter bw, String instance, String name) throws IOException {
        DirectedGraph g = new DirectedGraph(instance);
        for (int i = 0; i < iterations; i++) {
            List<Integer> sol = new ArrayList<>(solutions.get(name));
            int b = new Random().nextInt(sol.size() - 1);
            Collections.shuffle(sol);
            for (int j = 0; j < b; j++) {
                g.removeNode(sol.get(j));
            }
            g.rootClean();
            g.extractNodeMetaData(bw);

        }
    }

    private static void runILP(String file, double timelimit) throws IOException, GRBException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String instance = br.readLine();

        System.out.println("instance;k;fileTime;cleanTime;digraphTime;totalTime;solution");
        while (instance != null) {
            instance = instance.split("\t")[0];
            long time = -System.nanoTime();

            String instanceName = instance.substring(instance.indexOf("instances/") + 10);
            String name = instanceName.substring(0, instanceName.indexOf("/"));
            int optk;
            if (name.contains("3")) {
                optk = solSizes3.get(instance.substring(instance.lastIndexOf("/") + 1));
                instance = br.readLine();
                continue;
            } else {
                optk = solSizes.get(instance.substring(instance.lastIndexOf("/") + 1));
            }

            DirectedGraph graph = new DirectedGraph(instance);

            long cleanTime = -System.nanoTime();
            Set<Integer> cleanedNodes = graph.rootClean();
            double cleanSec = utils.round((cleanTime + System.nanoTime()) / 1_000_000_000.0, 4);

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

            double digraphSec = utils.round((digraphTime + System.nanoTime()) / 1_000_000_000.0, 4);

            long fileTime = -System.nanoTime();

            int kClean = cleanedNodes.size();
            List<Integer> k = new ArrayList<>();
            double fileSec = 0.0;
            if (graph.nodeMap.size() > 0) {
                graph.createTopoLPFile(digraphs);
                fileSec = utils.round((fileTime + System.nanoTime()) / 1_000_000_000.0, 4);

                k = ilp("ILPs/" + instanceName, Math.max(0.001, timelimit - fileSec - digraphSec));
            }
            System.out.print(instance);
            if (k == null) {
                if (optk == -1) {
                    // Yellow: a timeout that also has no known solution
                    System.out.print("\u001B[33m !both Timeouts! \u001B[0m");
                }
                System.out.println(";-1;" + fileSec + ";" + cleanSec + ";" + digraphSec + ";" + timelimit + ";[]");
            } else {

                if (optk == -1) {
                    // Green: a Solution with no known optimal Solution.
                    System.out.print("\u001B[32m !Solution unkown! \u001B[0m");
                } else if (optk != k.size() + kClean) {
                    // Red: an incorrect Solution
                    System.out.print("\u001B[31m !Incorrect Solution! \u001B[0m");
                }

                double sec = utils.round((time + System.nanoTime()) / 1_000_000_000.0, 4);
                System.out.println(";" + (k.size() + kClean) + ";" + fileSec + ";" + cleanSec + ";" + digraphSec + ";" + sec + ";" + k);
            }
            instance = br.readLine();
            //break;
        }
    }

    private static List<Integer> ilp(String file, double timelimit) {

        try {
            GRBModel model = new GRBModel(env, file);

            model.optimize();

            int optimstatus = model.get(GRB.IntAttr.Status);

            if (optimstatus == GRB.Status.INF_OR_UNBD) {
                model.set(GRB.IntParam.Presolve, 0);
                model.set(GRB.DoubleParam.TimeLimit, timelimit);
                model.optimize();
                optimstatus = model.get(GRB.IntAttr.Status);
            }

            if (optimstatus == GRB.Status.OPTIMAL) {
                List<Integer> res = new ArrayList<>();
                for (GRBVar var : model.getVars()) {
                    if (var.get(GRB.DoubleAttr.X) > 0.9 && var.get(GRB.StringAttr.VarName).contains("x")) {
                        res.add(Integer.valueOf(var.get(GRB.StringAttr.VarName).substring(1)));
                    }
                }
                return res;
            } else if (optimstatus == GRB.Status.INFEASIBLE) {
                System.out.println("Model is infeasible");
                model.computeIIS();
                model.write("model.ilp");
            } else if (optimstatus == GRB.Status.UNBOUNDED) {
                System.out.println("Model is unbounded");
            } else if (optimstatus == GRB.Status.TIME_LIMIT) {
                return null;
            } else {
                System.out.println("Optimization was stopped with status = "
                        + optimstatus);
            }
            model.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
        }
        return null;
    }

    private static String randomHeuristic(String file) {
        DirectedGraph g = new DirectedGraph(file);

        long time = -System.nanoTime();

        Set<Integer> k = g.rootClean();

        while (!g.nodeMap.isEmpty()) {
            Set<Integer> set = g.nodeMap.keySet();
            int index = new Random().nextInt(set.size());
            Iterator<Integer> it = set.iterator();
            for (int i = 0; i < index; i++) {
                it.next();
            }
            int nodeToDelete = it.next();
            g.removeNode(nodeToDelete);
            k.add(nodeToDelete);
            k.addAll(g.rootClean());
        }
        double sec = utils.round((double)(time+System.nanoTime())/1_000_000_000, 4);
        return "" + k.size();
    }

    private static int NNHeuristic(String file, Model model) {
        DirectedGraph g = new DirectedGraph(file);

        Set<Integer> k = g.rootClean();

        int batch = 30;
        double limit = 0.8;

        while (!g.nodeMap.isEmpty() && g.nodeMap.size() != g.predictions.size()) {
            g.createPredictionsForBatch(batch, model);

            k.addAll(g.deleteNodesByPredictions(limit));

            if (g.predictions.size() == batch) {
                batch += 10;
                limit -= 0.05;
            }
        }
        while (!g.nodeMap.isEmpty()) {
            k.addAll(g.deleteNodesByPredictions(limit));
            limit -= 0.05;
        }

        return k.size();
    }
}