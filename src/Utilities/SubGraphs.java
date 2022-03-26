package Utilities;

import Graph.DirectedGraph;

import java.io.File;
import java.util.*;

public class SubGraphs {

    public static void main(String[] args) {
        File[] files = Objects.requireNonNull(new File("instances/instances/").listFiles());
        for (File file : files) {
            String path = file.getPath();
            String name = file.getName();

            if (name.contains("_s")) continue;

            System.out.println("Generating Subgraphs for: " + name);

            DirectedGraph g = new DirectedGraph(path);
            g.k = Integer.MAX_VALUE;
            g.cleanGraph();

            if (g.nodeMap.size() > 350) {
                int iterations = Math.min(25,Math.max(10, g.nodeMap.size()/250));

                List<Integer> nodeIds = new ArrayList<>(g.nodeMap.keySet());

                for (int c = 1; c <= iterations; c++) {
                    Collections.shuffle(nodeIds);
                    g.addStackCheckpoint();
                    int size = (int) (75 + Math.random()*275);
                    int i = 0;
                    while (g.nodeMap.size() > size) {
                        int nodeToDelete = nodeIds.get(i);

                        if (!g.nodeMap.containsKey(nodeToDelete)) {
                            i++;
                            continue;
                        }

                        Set<Integer> neighbours = new HashSet<>(g.nodeMap.get(nodeToDelete).getInNodes());
                        neighbours.addAll(g.nodeMap.get(nodeToDelete).getOutNodes());
                        g.removeNode(nodeToDelete);
                        g.rootClean(neighbours, true);
                    }
                    if (!g.nodeMap.isEmpty()) {
                        g.saveToFile(name + "_s" + c + "_" + g.nodeMap.size());
                    }
                    g.rebuildGraph();
                }
            }
        }
    }
}
