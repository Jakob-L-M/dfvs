package dirgraph;

import java.io.IOException;
import java.util.*;

public class Main {
    static Stack<DirectedNode> deletedNodes = new Stack<>();

    public static Set<Integer> dfvsBranch(DirectedGraph graph, int k) {
        if(k < 0) return null;
        graph.cleanGraph();
        Set<Integer> dfvs = new HashSet<>();
        Deque<Integer> cycle = graph.findBusyCycle();
        if(cycle == null) return dfvs;
        //cycle.stream().filter(c -> graph.isFixed((int) c));
        for(Integer v : cycle) {
            /*Set removedNodes = new HashSet<DirectedNode>();
            DirectedNode removedNode = new DirectedNode((DirectedNode) graph.nodeMap.get(v));
            removedNodes.add(removedNode);*/
            DirectedGraph graphCopy = new DirectedGraph(graph);
            //removedNodes.addAll(graph/*Copy*/.removeClean((int) v));
            deletedNodes.addAll(graphCopy.removeClean(v));

            dfvs = dfvsBranch(graphCopy, k - 1);
            if(dfvs != null) {
                dfvs.add(v);
                //System.out.println("nodes to delete: " + v + " which is fixed: " + graph.isFixed((int) v));
                return dfvs;
            }
            else {
                //graph.reconstructNodes(removedNodes);
                graph.fixNode(v);
            }
        }
        return null;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph) {
        int k = 0;
        Set<Integer> dfvs = null;
        while(dfvs == null) {
            dfvs = dfvsBranch(graph, k++);
        }
        return dfvs;
    }

    /*public static void main(String[] args) {
        DirectedGraph graph = new DirectedGraph(args[0]);
        for (int i:dfvsSolve(graph)
             ) {
            System.out.println(i);
        }
    	
    	DirectedGraph graph = new DirectedGraph("./instances/example.txt");
    	graph.visualize("Before");
    	graph = graph.burningBridges();
    	//System.out.println(graph.hasBridge());
    	graph.visualize("After");
        for (int i:dfvsSolve(graph)) {
            System.out.println(i);
        }*/

    public static void main(String[] args) throws IOException {
        DirectedGraph graph = new DirectedGraph("resources/small.txt"/*args[0]*/);
        long time = -System.nanoTime();
        for(int i : dfvsSolve(graph)) {
            System.out.println(i);
        };
        time += System.nanoTime();
        System.out.println(time);
        //big 207s

        /*
        Tarjan tarjan = new Tarjan(graph);
        tarjan.findSCCsTarjan();
        System.out.println(Arrays.toString(tarjan.getLowLinks()));
        System.out.println(tarjan.getSCCs());
        */
    }
}
