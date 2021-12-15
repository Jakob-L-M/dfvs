import javax.swing.*;
import java.io.File;
import java.util.*;

public class Packing {
    private DirectedGraph graph;
    private final DirectedGraph graphFixed;
    private final Set<Integer> usedNodes;
    private final Set<Deque<Integer>> costlySubGraphs;
    private Set<Set<Integer>> digraphs;
    private Set<Integer> safeToDeleteDigraphNodes;

    Packing(DirectedGraph graph) {
        this.graph = new DirectedGraph(graph);
        graphFixed = new DirectedGraph(graph);
        costlySubGraphs = new HashSet<>();
        usedNodes = new HashSet<>();
        digraphs = new HashSet<>();
        safeToDeleteDigraphNodes = new HashSet<>();
    }

    public static void main(String[] args) {
        DirectedGraph graph = new DirectedGraph("instances/synthetic/synth-n_140-m_1181-k_20-p_0.1.txt");
        Packing packing = new Packing(graph);
        System.out.println(packing.getDigraphs());
        Set<Integer> safeDigraphDeletions = packing.getSafeToDeleteDigraphNodes();
        System.out.println(safeDigraphDeletions);

        packing.graph = new DirectedGraph(packing.graphFixed);
        long time = -System.nanoTime();
        System.out.println(packing.findQuickPacking());
        time += System.nanoTime();
        System.out.println(time);

        packing.graph = new DirectedGraph(packing.graphFixed);
        time = -System.nanoTime();
        packing.findCyclePacking();
        System.out.println(packing.costlySubGraphs);
        time += System.nanoTime();
        System.out.println(time);

        packing.graph = new DirectedGraph(packing.graphFixed);
        time = -System.nanoTime();
        System.out.println(packing.findQuickPacking());
        time += System.nanoTime();
        System.out.println(time);

        packing.graph = new DirectedGraph(packing.graphFixed);
        //System.out.println(packing.getDigraphs());
        packing.safeDeletionDigraph();
        System.out.println(safeDigraphDeletions);





        /*File complexInstances = new File("instances/complex");
        long time = -System.nanoTime();
        for (File file : complexInstances.listFiles()) {
            DirectedGraph graph = new DirectedGraph(file.getPath());
            Packing packing = new Packing(graph);
            //Set<Set<Integer>> digraphs = stacking.getDigraphs();
            System.out.println(packing.findQuickPacking());
            for (Set<Integer> cycle : packing.findQuickPacking()) {
                if (cycle.size() > 2) System.out.println("gut: " + cycle);
            }
        }
        time += System.nanoTime();
        System.out.println(time);
        //System.out.println(stacking.getSafeToDeleteDigraphNodes().size());
        //System.out.println(digraphs);
        PriorityQueue<Set<Integer>> digraphQueue = new PriorityQueue<>(200, new Main.SetComparator());
        int lowerBound = 0;
        for (Set<Integer> digraph : digraphs) {
            lowerBound += digraph.size() - 1;
            for (Integer i : digraph) {
                for (Integer j : digraph) {
                    if (i != j && !graph.hasEdge(i, j)) System.out.println("test");
                }
            }
            digraphQueue.add(digraph);
            System.out.println(digraph);
            System.out.println(stacking.getSafeToDeleteDigraphNodes());
            System.out.println(stacking.getSafeToDeleteDigraphNodes().size());
        }
        graph = new DirectedGraph(stacking.graphFixed);
        Set<Integer> safeToDelete = new HashSet<>();
        while(!digraphQueue.isEmpty()) {
            Set<Integer> digraph = digraphQueue.poll();
            int n = digraph.size();
            for (Integer i : digraph){
                if(graph.getNode(i).getInDegree() == n - 1 || graph.getNode(i).getOutDegree() == n - 1) {
                    safeToDelete.add(i);
                }
            }
        }
        System.out.println(safeToDelete.size());*/

        //System.out.println(stacking.findBigDigraphs().size());
    }

    public void safeDeletionDigraph() {
        Set<Integer> safeToDelete = new HashSet<>();
        for (Set<Integer> digraph : digraphs) {
            int n = digraph.size();
            Set<Integer> deletionCandidates = new HashSet<>();
            for (Integer i : digraph){
                if(graphFixed.getNode(i).getInDegree() == n - 1 || graphFixed.getNode(i).getOutDegree() == n - 1) {
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

    public Set<Deque<Integer>> findCyclePacking() {
        Deque<Integer> cycle = graph.findBestCycle();
        while (cycle != null && !cycle.isEmpty()) {
            costlySubGraphs.add(cycle);
            for (Integer i : cycle) {
                graph.removeNode(i);
            }
            cycle = graph.findBestCycle();
        }
        return costlySubGraphs;
    }

    public int getLowerBound() {
        int lowerBound = 0;
        for (Deque<Integer> deque : costlySubGraphs) {
            lowerBound++;
            if (deque.size() == 3) {
                List<Integer> triplet = new ArrayList<>(deque);
                Integer u = triplet.get(0);
                Integer v = triplet.get(1);
                Integer w = triplet.get(2);
                if (graphFixed.hasEdge(u, v) && graphFixed.hasEdge(v, u) && graphFixed.hasEdge(u, w)
                        && graphFixed.hasEdge(w, u) && graphFixed.hasEdge(v, w) && graphFixed.hasEdge(w, v)) {
                    //System.out.println("3-Digraph found");
                    lowerBound++;
                }
            }
        }
        return lowerBound;
    }

    public Set<Set<Integer>> getDigraphs() {
        Set<Set<Integer>> digraphs = new HashSet<>();
        Set<Integer> nodes = graph.nodeMap.keySet();
        while(!graph.nodeMap.isEmpty()) {
            Integer u = nodes.stream().iterator().next();
            Set<Integer> a = expand(graph.nodeMap.keySet(), u);
            boolean fullDigraphDeletable = false;
            /*
            for (Integer node : nodes) {
                if (!a.contains(node)) {
                    int countIn = 0;
                    int countOut = 0;
                    for (Integer v :  a) {
                        if (graph.hasEdge(v, node)) countOut++;
                        if (graph.hasEdge(node, v)) countIn++;
                        if (countIn > 0 && countOut > 0) {
                            break;
                        }
                    }
                    if (countIn > 0 && countOut > 0) {
                        fullDigraphDeletable = true;
                        break;
                    }
                }
            }
            if (fullDigraphDeletable) safeToDeleteDigraphNodes.addAll(a);
            else {
                digraphs.add(a);
            }*/
            digraphs.add(a);
            for (Integer i : a) {
                graph.removeNode(i);
            }
            nodes.removeAll(a);
        }
        this.digraphs = digraphs;
        return digraphs;
    }

    public int lowerDigraphBound() {
        int lowerBound = 0;
        for (Set<Integer> digraph : digraphs) {
            lowerBound += digraph.size() - 1;
        }
        return lowerBound;
    }



    private Set<Integer> expand(Set<Integer> nodes, Integer start) {
        Set<Integer> digraph = new HashSet<>();
        digraph.add(start);
        boolean change = true;
        Set<Integer> commonNeighbours = new HashSet<>();
        commonNeighbours.addAll(graph.nodeMap.keySet());
        while (change) {
            commonNeighbours.addAll(graph.nodeMap.keySet());
            change = true;
            for (Integer u : digraph) {
                commonNeighbours.retainAll(graph.getNode(u).getInNodes());
                commonNeighbours.retainAll(graph.getNode(u).getOutNodes());
                if (commonNeighbours.isEmpty()) {
                    change = false;
                    break;
                }
            }
            if (!commonNeighbours.isEmpty()) {
                digraph.add(commonNeighbours.stream().iterator().next());
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



    public void findFull3Digraphs() {
        Set<Integer> nodes = new HashSet<>();
        nodes.addAll(graph.nodeMap.keySet());
        for (Integer u : graph.nodeMap.keySet()) {
            if (graph.getNode(u).isTwoCycle() != -1) {
                Integer v = graph.getNode(u).isTwoCycle();
            }
        }
        Set<Integer> visited = new HashSet<>();
        HashMap<Integer, Integer> parent = new HashMap<>();
        Deque<Integer> cycle = new ArrayDeque<>();
        for (Integer i : graph.nodeMap.keySet()) {
            parent.put(i, -1);
        }
        for (Integer start : graph.nodeMap.keySet()) {
            if (visited.contains(start)) continue;
            Deque<Integer> queue = new ArrayDeque<>();
            Deque<Integer> tempCycle = new ArrayDeque<>();
            queue.add(start);
            visited.add(start);
            while(!queue.isEmpty()) {
                Integer u = queue.pop();
                for (Integer v : graph.nodeMap.get(u).getOutNodes()) {
                    if (!visited.contains(v)) {
                        parent.put(v, u);
                        visited.add(v);
                        queue.add(v);
                    }
                    if (v.equals(start)) {

                        int w = u;
                        while(w != -1) {
                            tempCycle.add(w);
                            w = parent.get(w);
                        }
                    }
                    if (!tempCycle.isEmpty()) break;
                }
                if (!tempCycle.isEmpty()) break;
            }
            Deque<Integer> nodesLeft = new ArrayDeque<>();
            if (!cycle.isEmpty()) {
                for (Integer u : tempCycle) {
                    if (!graph.nodeMap.get(u).isFixed()) nodesLeft.add(u);
                }
            }
            else {
                nodesLeft = tempCycle;
            }
            if(tempCycle.size() == 3) {
                Iterator<Integer> it = tempCycle.iterator();
                int u = it.next();
                int v = it.next();
                int w = it.next();
                if (graph.hasEdge(u, v) && graph.hasEdge(v, w) && graph.hasEdge(w, u)
                        && graph.hasEdge(v, u) && graph.hasEdge(w, v) && graph.hasEdge(u, w)) {
                    usedNodes.add(u);
                    usedNodes.add(v);
                    usedNodes.add(w);
                }
            }
        }
    }


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
            }
            else {
                nodes.remove(i);
            }
        }
        while (true) {
            Deque<Integer> newCycle = graph.findBestCycle();
            if(newCycle == null) break;
            Set<Integer> newC = new HashSet<>();
            newC.addAll(newCycle);
            if(newC == null || newC.isEmpty()) break;
            packing.add(newC);
            graph.removeAllNodes(newC);
        }
        return packing;
    }



}