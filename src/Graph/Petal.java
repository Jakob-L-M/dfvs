package Graph;

import Utilities.Tuple;

import java.util.*;

public class Petal {

    public static Tuple getPetalSet(DirectedGraph graph, int nodeId, int limit, boolean quick) {
        graph.addNode(-1); //source
        graph.addNode(-2); //sink

        DirectedNode node = graph.nodeMap.get(nodeId);

        for (Integer outNode : node.getOutNodes()) {
            graph.addEdge(-1, outNode);
        }

        for (Integer inNode : node.getInNodes()) {
            graph.addEdge(inNode, -2);
        }

        graph.addStackCheckpoint();

        graph.removeNode(nodeId);

        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> petalSet = new HashSet<>();

        int max_flow = 0;

        while (bfs(graph, parent, limit)) {
            int cur = parent.get(-2);
            while (cur != -1) {
                graph.removeNode(cur);
                if(!quick) petalSet.add(cur);
                cur = parent.get(cur);
            }
            max_flow += 1;
            parent.clear();
        }

        graph.rebuildGraph();
        graph.removeNode(-1, false);
        graph.removeNode(-2, false);

        if(!quick)graph.nodeMap.get(nodeId).setPedal(max_flow);

        petalSet.add(nodeId);
        return new Tuple(max_flow, petalSet);
    }

    private static boolean bfs(DirectedGraph graph, Map<Integer, Integer> parent, int limit) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();

        queue.add(-1);

        // Standard BFS Loop
        while (!queue.isEmpty()) {
            int current = queue.remove();

            if (graph.nodeMap.containsKey(current)) {
                for (int outNode : graph.nodeMap.get(current).getOutNodes()) {
                    if (!visited.contains(outNode)) {
                        parent.put(outNode, current);
                        // reached sink - break here
                        if (outNode == -2) {
                            return true;
                        }
                        visited.add(outNode);

                        // limit flower size
                        if (limitParentDepth(outNode, parent,limit)) {
                            queue.add(outNode);
                        }
                    }
                }
            }
        }
        return visited.contains(-2);
    }

    private static boolean limitParentDepth(int nodeId, Map<Integer, Integer> parent, int limit) {
        int c = 1;
        int cur = parent.get(nodeId);
        while (cur != -1) {
            c++;
            cur = parent.get(cur);
            if (c >= limit) return false;
        }
        return true;
    }
}
