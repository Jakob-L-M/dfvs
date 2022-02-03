import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Packing {
    private final DirectedGraph graph;
    private final List<List<Integer>> mixedStructs;
    private final Set<Integer> safeToDeleteDigraphNodes;
    public List<Set<Integer>> digraphs;
    public List<Deque<Integer>> costlySubGraphs;
    public Set<Integer> usedNodes;

    Packing(DirectedGraph graph) {
        this.graph = graph;
        digraphs = new ArrayList<>();
        safeToDeleteDigraphNodes = new HashSet<>();
        mixedStructs = new ArrayList<>();
        costlySubGraphs = new LinkedList<>();
        usedNodes = new HashSet<>();
    }

    public static void main(String[] args) {
        //DirectedGraph g = new DirectedGraph("instances/h_001", true);
        //System.out.println(g.size());
        //System.out.println(Main.dfvsSolve(g));

        //for (int runs =  0; runs < 10; runs++) {
          //  for (int level = 0; level < 5; level++) {
            //    for (int sccs = 1; sccs < 13; sccs+=2) {
              //      runParam(args[0], runs, level, sccs);


        if (true) {
            Set<String> jakobInst = new HashSet<>();
            String[] jakobNames = new String[] {"h_119"};//"e_159","e_169","h_177","h_133","h_157","h_171","h_179","h_187","h_189"};
            for (String inst : jakobNames) {
                jakobInst.add(inst);
            }
            try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("packingTimes.txt", true));
            writer.write("name heurK\n");
            File instances = new File("instances");
            for (File subInst : instances.listFiles()) {
                if (!subInst.getName().contains("sheet5") || !subInst.isDirectory()) continue;
                int count = 0;
                for (File instance : subInst.listFiles()) {
                    //if (!jakobInst.contains(instance.getName())) continue;
                    int best_heur = Integer.MAX_VALUE;
                    int runs = 1;
                    int[] all_heurs = new int[runs];
                    long singleTime = -System.nanoTime();
                    for (int i = 0; i < runs; i++) {
                        if (instance.getName().contains("h_119")) continue;
                        DirectedGraph graph = new DirectedGraph(instance.getPath());
                        Set<Integer> safeNodes = graph.rootClean();
                        //Packing packing = new Packing(graph);
                        //packing.getDigraphs();
                        //safeNodes.addAll(packing.getSafeToDeleteDigraphNodes());
                        //graph.removeAllNodes(safeNodes);
                        //safeNodes.addAll(graph.rootClean(safeNodes));
                        int sol = graph.deathByPacking(30, 4) + safeNodes.size();
                        all_heurs[i] = sol;
                        if (sol < best_heur) best_heur = sol;
                    }
                    singleTime += System.nanoTime();
                    System.out.println(instance.getName() + " time: " + singleTime);
                    writer.write(instance.getPath() + " " + best_heur + "\n");// + " out of " + Arrays.toString(all_heurs) + "\n");
                    System.out.println("done " + instance.getName());
                    //if (count > 100) break;


                    System.out.println("count: " + count++);
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        }
        else {
            try {
                long time = - System.nanoTime();
                BufferedWriter writer = new BufferedWriter(new FileWriter("packingTimesSinglePacking.txt", true));
                int runs = 1;
                int best_heur = Integer.MAX_VALUE;
                int[] all_heurs = new int[runs];
                for (int i = 0; i < runs; i++) {
                    DirectedGraph graph = new DirectedGraph(args[0]);
                    int sol = graph.deathByPacking(1,1);
                    all_heurs[i] = sol;
                    if (sol < best_heur) best_heur = sol;
                }
                time += System.nanoTime();
                writer.write(args[0] + " " + best_heur + " out of " + Arrays.toString(all_heurs) + "\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void runParam(String filename, int runs, int level, int sccs) {
        try {
            long time = - System.nanoTime();
            BufferedWriter writer = new BufferedWriter(new FileWriter("packingTimes"+"runs"+runs+"level"+level+"sccs"+sccs +".txt", true));
            int best_heur = Integer.MAX_VALUE;
            int[] all_heurs = new int[runs];
            for (int i = 0; i < runs; i++) {
                DirectedGraph graph = new DirectedGraph(filename);
                int sol = graph.deathByPacking(level, sccs);
                all_heurs[i] = sol;
                if (sol < best_heur) best_heur = sol;
            }
            time += System.nanoTime();
            writer.write(filename + " " + best_heur + " out of " + Arrays.toString(all_heurs) + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void safeDeletionDigraph() {
        Set<Integer> safeToDelete = new HashSet<>();
        for (Set<Integer> digraph : digraphs) {
            int n = digraph.size();
            Set<Integer> deletionCandidates = new HashSet<>();
            for (Integer i : digraph) {
                if (graph.getNode(i).getInDegree() == n - 1 || graph.getNode(i).getOutDegree() == n - 1) {
                    deletionCandidates.addAll(digraph);
                    deletionCandidates.remove(i);
                    break;
                }
            }
            safeToDelete.addAll(deletionCandidates);
        }
        safeToDeleteDigraphNodes.addAll(safeToDelete);
    }

    public Set<Integer> getSafeToDeleteDigraphNodes() {
        safeDeletionDigraph();
        return safeToDeleteDigraphNodes;
    }

    public List<Deque<Integer>> findCyclePacking() {
        graph.addStackCheckpoint();
        List<Deque<Integer>> costlySubGraphs = new ArrayList<>();
        Deque<Integer> cycle = graph.findBestCycle();
        while (cycle != null && !cycle.isEmpty()) {
            costlySubGraphs.add(cycle);
            for (Integer i : cycle) {
                graph.removeNode(i);
            }
            cycle = graph.findBestCycle();
        }
        graph.rebuildGraph();
        return costlySubGraphs;
    }

    public List<Deque<Integer>> newFindCyclePacking(int cycleLimit, int sameCycleCount) {
        graph.addStackCheckpoint();
        List<Deque<Integer>> packing = new ArrayList<>();
        Deque<Integer> cycle = graph.findBestCycle(cycleLimit, sameCycleCount);
        while (cycle != null && !cycle.isEmpty()) {
            packing.add(cycle);
            for (Integer i : cycle) {
                DirectedNode node = graph.nodeMap.get(i);
                if (node != null) {
                    Set<Integer> neighbours = new HashSet<>(node.getInNodes());
                    neighbours.addAll(node.getOutNodes());
                    graph.removeNode(i);
                    graph.quickClean(neighbours);
                }
            }
            cycle = graph.findBestCycle(cycleLimit, sameCycleCount);
        }
        graph.rebuildGraph();
        return packing;
    }

    public List<Set<Integer>> getDigraphs() {
        graph.addStackCheckpoint();
        List<Set<Integer>> digraphs = new ArrayList<>();
        Set<Integer> nodes = graph.nodeMap.keySet();
        while (!graph.nodeMap.isEmpty()) {
            Integer u = nodes.stream().iterator().next();
            Set<Integer> a = expand(u);
            digraphs.add(a);
            for (Integer i : a) {
                graph.removeNode(i);
            }
            a.forEach(nodes::remove);
        }
        this.digraphs = digraphs;
        graph.rebuildGraph();
        return digraphs;
    }

    /*
    public List<List<Integer>> getMixedStruct() {
        graph.addStackCheckpoint();
        List<List<Integer>> digraphs = new ArrayList<>();
        Set<Integer> nodes = graph.nodeMap.keySet();
        while (!graph.nodeMap.isEmpty()) {
            Integer u = nodes.stream().iterator().next();
            List<Integer> a = expand(u);
            if (a.size() > 2) digraphs.add(a);
            for (Integer i : a) {
                graph.removeNode(i);
            }
            nodes.removeAll(a);
        }
        this.digraphs = digraphs;
        this.mixedStructs.addAll(digraphs);
        this.mixedStructs.add(null);
        for (Deque<Integer> cycle : findCyclePacking()) {
            digraphs.add(new ArrayList<>(cycle));
        }
        graph.rebuildGraph();
        return mixedStructs;
    }
*/

    public int lowerDigraphBound() {
        int lowerBound = 0;
        for (Set<Integer> struct : digraphs) {
            lowerBound += struct.size() - 1;
        }
        return lowerBound;
    }


    private Set<Integer> expand(Integer start) {
        Set<Integer> digraph = new HashSet<>();
        digraph.add(start);
        boolean change = true;
        Set<Integer> commonNeighbours = new HashSet<>(graph.nodeMap.get(start).getInNodes());
        while (change) {
            commonNeighbours.addAll(graph.getNode(digraph.iterator().next()).getInNodes());
            for (Integer u : digraph) {
                commonNeighbours.retainAll(graph.getNode(u).getInNodes());
                commonNeighbours.retainAll(graph.getNode(u).getOutNodes());
                if (commonNeighbours.isEmpty()) {
                    change = false;
                    break;
                }
            }
            if (!commonNeighbours.isEmpty()) {
                digraph.add(commonNeighbours.iterator().next());
            }
        }
        //Set<Integer> commonNeighbours = new HashSet<>();
        commonNeighbours.addAll(graph.getNode(start).getOutNodes());
        commonNeighbours.retainAll(graph.getNode(start).getInNodes());
        //System.out.println(start + " " + graph.getNode(start).getOutNodes()
        // + " " + graph.getNode(start).getInNodes() + " " + commonNeighbours);
        return digraph;
    }


//________________________________________________Ab hier Baustelle


    public Set<Set<Integer>> findQuickPacking() {
        Set<Set<Integer>> packing = new HashSet<>();
        Set<Integer> nodes = new HashSet<>();
        Set<Integer> nodesForBiggerCircles = new HashSet<>();
        nodes.addAll(graph.nodeMap.keySet());
        while (!nodes.isEmpty()) {
            Integer i = nodes.stream().findFirst().get();
            if (graph.getNode(i).isTwoCycle() != -1) {
                Set<Integer> twoCycle = new HashSet<>();
                twoCycle.add(i);
                twoCycle.add(graph.getNode(i).isTwoCycle());
                packing.add(twoCycle);
                nodes.removeAll(twoCycle);
                graph.removeAllNodes(twoCycle);
            } else {
                nodes.remove(i);
            }
        }
        while (true) {
            Deque<Integer> newCycle = graph.findBestCycle();
            if (newCycle == null) break;
            Set<Integer> newC = new HashSet<>();
            newC.addAll(newCycle);
            if (newC == null || newC.isEmpty()) break;
            packing.add(newC);
            graph.removeAllNodes(newC);
        }
        return packing;
    }
    //get all fourCycles
    Set<Set<Integer>> getFourCycles() {
        graph.addStackCheckpoint();
        Set<Set<Integer>> fourCycles = new HashSet<>();
        Deque<Integer> c4Deque = graph.findC4s();
        if (c4Deque == null) return fourCycles;
        Set<Integer> fourCycle = new HashSet<>(c4Deque);

        while (fourCycle != null) {
            fourCycles.add(fourCycle);
            graph.removeAllNodes(fourCycle);
            c4Deque = graph.findC4s();
            if (c4Deque == null) return fourCycles;
            fourCycle = new HashSet<>(c4Deque);
        }
        graph.rebuildGraph();
        return fourCycles;
    }





    // Levs Code
    public void swapOne(List<Deque<Integer>> pack) {
        //Find nodes not included in packing.
    	/*Set<Integer> freeNodes = new HashSet<Integer>();
    	for (Integer node: graph.nodeMap.keySet()) {
    		if (!usedNodes.contains(node)) freeNodes.add(node);
    	}*/
        //For each cycle, try removing it and find more than one cycle.
        ArrayList<Deque<Integer>> oldCycs =  new ArrayList(pack);
        for (Deque<Integer> cycle: oldCycs) {
            //System.out.println("A " + costlySubGraphs.size());
            costlySubGraphs.remove(cycle);
            //System.out.println("B " + costlySubGraphs.size());
            addCyclesWithout(cycle);
            //System.out.println("C " + costlySubGraphs.size());
        }
    }

    private void addCyclesWithout(Deque<Integer> removeCycle) {
        graph.addStackCheckpoint();
        Set<Integer> nodes = new HashSet<>(graph.nodeMap.keySet());
        for (Integer node: nodes) {
            if (usedNodes.contains(node) && !removeCycle.contains(node)) {
                graph.removeNode(node);
            }
        }
        usedNodes.removeAll(removeCycle);
        Deque<Integer> cycle = graph.findBestCycle();
        while (cycle != null && !cycle.isEmpty()) {
            costlySubGraphs.add(cycle);
            for (Integer i : cycle) {
                graph.removeNode(i);
                usedNodes.addAll(cycle);
            }
            cycle = graph.findBestCycle();
        }
        graph.rebuildGraph();
    }

    public int getCycleNumber(){
        return costlySubGraphs.size();
    }

    public List<Deque<Integer>> getCycles(){
        return costlySubGraphs;
    }
}