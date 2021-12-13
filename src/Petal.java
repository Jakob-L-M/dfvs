import java.util.*;

public class Petal {

    public static Tuple getPetalSet(DirectedGraph graph, int nodeId) {
        graph.addNode(-1); //source
        graph.addNode(-2); //sink

        for (Integer outNode : graph.nodeMap.get(nodeId).getOutNodes()) {
            graph.addEdge(-1, outNode);
        }

        for (Integer inNode : graph.nodeMap.get(nodeId).getInNodes()) {
            graph.addEdge(inNode, -2);
        }

        graph.removeNode(nodeId);

        Map<Integer, Set<Integer>> rGraph = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> petalSet = new HashSet<>();

        for (Integer node : graph.nodeMap.keySet()) {
            rGraph.put(node, new HashSet<>(graph.nodeMap.get(node).getOutNodes()));
        }

        int max_flow = 0;

        while (bfs(rGraph, parent)) {
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

        for (Integer outNode : graph.nodeMap.get(-1).getOutNodes()) {
            graph.addEdge(nodeId, outNode);
        }

        for (Integer inNode : graph.nodeMap.get(-2).getInNodes()) {
            graph.addEdge(inNode, nodeId);
        }

        graph.removeNode(-1);
        graph.removeNode(-2);

        graph.nodeMap.get(nodeId).setPedal(max_flow);

        petalSet.add(nodeId);
        return new Tuple(max_flow, petalSet);
    }

    private static boolean bfs(Map<Integer, Set<Integer>> rGraph, Map<Integer, Integer> parent) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();

        queue.add(-1);
        visited.add(-1);

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
                        queue.add(outNode);
                        visited.add(outNode);
                    }
                }
            }
        }
        return visited.contains(-2);
    }
}
