package Utilities;

import Graph.DirectedGraph;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SubGraphs {

    public static void main(String[] args) {
        File[] files = Objects.requireNonNull(new File("instances/instances/").listFiles());
        for (File file : files) {
            String path = file.getPath();
            String name = file.getName();

            System.out.println("Generating Subgraphs for: " + name);

            DirectedGraph g = new DirectedGraph(path);
            g.rootClean();

            if (g.nodeMap.size() > 350) {
                int iterations = Math.max(10, g.nodeMap.size()/100);

                List<Integer> nodeIds = new ArrayList<>(g.nodeMap.keySet());

                for (int c = 1; c <= iterations; c++) {
                    Collections.shuffle(nodeIds);
                    g.addStackCheckpoint();
                    int size = (int) (175 + Math.random()*155);
                    int i = 0;
                    while (g.nodeMap.size() > size) {
                        g.removeNode(nodeIds.get(i++));
                        g.rootClean();
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
