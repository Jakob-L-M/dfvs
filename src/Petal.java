import java.util.*;

public class Petal {

    public static Tuple getPetalSet(DirectedGraph graph, int nodeId, int limit) {
        graph.addNode(-1); //source
        graph.addNode(-2); //sink

        DirectedNode node = graph.nodeMap.get(nodeId);

        for (Integer outNode : node.getOutNodes()) {
            graph.addEdge(-1, outNode, false);
        }

        for (Integer inNode : node.getInNodes()) {
            graph.addEdge(inNode, -2, false);
        }

        graph.removeNode(nodeId, false);

        Map<Integer, Set<Integer>> rGraph = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> petalSet = new HashSet<>();

        for (Integer nodes : graph.nodeMap.keySet()) {
            rGraph.put(nodes, graph.nodeMap.get(nodes).getOutNodes());
        }

        int max_flow = 0;

        while (bfs(rGraph, parent, limit)) {
            int cur = parent.get(-2);
            while (cur != -1) {
                rGraph.remove(cur);
                petalSet.add(cur);
                cur = parent.get(cur);
            }
            max_flow += 1;
            parent.clear();
        }

        graph.addNode(nodeId);

        for (Integer outNode : node.getOutNodes()) {
            graph.addEdge(nodeId, outNode, false);
        }

        for (Integer inNode : node.getInNodes()) {
            graph.addEdge(inNode, nodeId, false);
        }

        graph.removeNode(-1, false);
        graph.removeNode(-2, false);

        graph.nodeMap.get(nodeId).setPedal(max_flow);

        petalSet.add(nodeId);
        return new Tuple(max_flow, petalSet);
    }

    private static boolean bfs(Map<Integer, Set<Integer>> rGraph, Map<Integer, Integer> parent, int limit) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();

        queue.add(-1);

        // Standard BFS Loop
        while (!queue.isEmpty()) {
            int current = queue.remove();

            if (rGraph.containsKey(current)) {
                for (int outNode : rGraph.get(current)) {
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
