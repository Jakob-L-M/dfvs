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
        DirectedGraph g = new DirectedGraph("instances/sheet5-heuristic/e_077.new");
        System.out.println(Main.dfvsSolve(g).size());
        //System.out.println("clean: " + g.cleanSelfCyclesInit().size());
        Packing pack = new Packing(g);
        List<Deque<Integer>> remove = pack.findCyclePacking();
        while(!remove.isEmpty()) {
            int count = 0;
            for (Deque<Integer> circle : remove) {
                g.removeNode(circle.peek());
                count++;
            }
            System.out.println("pack remove: " + count);
            remove = pack.findCyclePacking();
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("packingTimes.txt", true));
            writer.write("name heurK\n");
            File instances = new File("instances");
            for (File subInst : instances.listFiles()) {
                if (!subInst.getName().contains("synthetic3") || !subInst.isDirectory()) continue;
                int count = 0;
                for (File instance : subInst.listFiles()) {
                    DirectedGraph graph = new DirectedGraph(instance.getPath());
                    writer.write(instance.getName() + " " + graph.deathByPacking() + "\n");
                    System.out.println("done");
                    //if (count > 100) break;
                    System.out.println("count: " + count++);
                }
            }
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