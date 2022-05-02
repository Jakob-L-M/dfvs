package Utilities;

import Graph.DirectedGraph;
import Graph.Packing;
import gurobi.*;

import java.io.*;
import java.util.*;

public class DataCreation {

    static GRBEnv env;

    static {
        try {
            env = new GRBEnv();
            env.set(GRB.IntParam.OutputFlag, 0);
            double timelimit = 20.0;
            env.set(GRB.DoubleParam.TimeLimit, timelimit);
            env.start();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public static void createSolutions() throws GRBException, IOException {

        System.out.print("# ");

        GRBEnv env = new GRBEnv();
        env.set(GRB.IntParam.OutputFlag, 0);
        double timelimit = 60.0;
        env.set(GRB.DoubleParam.TimeLimit, timelimit);
        env.start();

        BufferedWriter bw = new BufferedWriter(new FileWriter("graph-metadata/nodeData.csv"));
        bw.write("instance;solution\n");

        for (File file : Objects.requireNonNull(new File("instances/instances/").listFiles())) {

            String instance = file.getPath();

            if (!instance.contains("_s")) continue;

            DirectedGraph graph = new DirectedGraph(instance);
            graph.k = Integer.MAX_VALUE;
            graph.cleanGraph();

            Packing p = new Packing(graph);
            graph.removeAllNodes(p.getSafeToDeleteDigraphNodes(true));

            if (graph.size() == 0) continue;

            graph.createTopoLPFile("temp.lp");

            List<Integer> sol = ilp("temp.lp", 60.0);

            if (sol == null) {
                System.out.println("Timeout for " + graph.name);
                continue;
            }

            if (!sol.isEmpty()) {
                bw.write(file.getName() + ";" + sol + "\n");
                bw.flush();
            }

            System.out.println("Created a solution for " + graph.name + " of size " + sol.size());

            //findMoreSolutions(graph, sol, bw, env);
        }
    }

    private static void findMoreSolutions(DirectedGraph graph, List<Integer> sol, BufferedWriter bw, GRBEnv env) throws IOException {
        int opt = sol.size();
        int foundSols = 0;
        List<Integer> currentSol = new ArrayList<>();

        for (Integer node : new ArrayList<>(graph.nodeMap.keySet())) {

            if (sol.contains(node)) continue;

            currentSol.clear();

            graph.addStackCheckpoint();
            graph.k = Integer.MAX_VALUE;

            graph.removeNode(node);
            currentSol.addAll(graph.cleanGraph());

            if (graph.size() != 0) {

                graph.createTopoLPFile("temp.lp");

                List<Integer> tSol = ilp("temp.lp", 180.0);

                if (tSol == null) {
                    graph.rebuildGraph();
                    continue;
                }

                currentSol.addAll(tSol);
            }

            if (currentSol.size() <= opt-1) {
                bw.write(graph.name + ";" + currentSol + "\n");
                foundSols++;
            }
            graph.rebuildGraph();
        }
        if (foundSols > 0) System.out.println("\t Found " + foundSols + " more solutions");
    }

    public static List<Integer> ilp(String file, double timelimit) {

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

    private static void createNodeData(int iterations, Map<String, List<Integer>> solutions, BufferedWriter bw, String instance, String name) throws IOException {

        if (!solutions.containsKey(name)) return;

        DirectedGraph g = new DirectedGraph(instance);
        g.k = Integer.MAX_VALUE;
        g.cleanGraph();

        Packing p = new Packing(g);
        g.removeAllNodes(p.getSafeToDeleteDigraphNodes(true));

        g.cleanGraph();

        List<Integer> sol = new ArrayList<>(solutions.get(name));
        for (int i = 0; i < iterations; i++) {
            int b = new Random().nextInt(Math.max(1, sol.size()/3));
            Collections.shuffle(sol);
            for (int j = 0; j < b; j++) {
                g.removeNode(sol.get(j));
            }
            g.cleanGraph();
            g.extractNodeMetaData(bw);
        }
    }

    private static void nodesMeta() throws IOException {
        String fileToSave = "./graph-metadata/nodes_v7.csv";

        Map<String, List<Integer>> solutions = utils.loadSolutions("graph-metadata/nodeData.csv");

        BufferedWriter bw = new BufferedWriter(new FileWriter(fileToSave));

        int iterations = 25;

        bw.write("instance,nodeId");

        // prep columns that carry features. Size should be equal to node data vector
        for (int i = 0; i < 11; i++) {
            bw.write("," + i);
        }
        bw.write("\n");

        for (File instance : Objects.requireNonNull(new File("instances/instances/").listFiles())) {
            String name = instance.getName();

            if (!solutions.containsKey(name)) continue;
            if (solutions.get(name).size() < 4) continue;

            System.out.println("Creating data for: " + name);

            createNodeData(iterations, solutions, bw, instance.getPath(), name);
        }
    }


    public static void main(String[] args) throws GRBException, IOException {
        nodesMeta();
    }
}
