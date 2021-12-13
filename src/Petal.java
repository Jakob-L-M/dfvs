import java.util.*;

public class Petal {

    public static Tuple getPetalSet(Map<Integer, DirectedNode> nodeMap, int source, int sink) {
        Map<Integer, Set<Integer>> rGraph = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> petalSet = new HashSet<>();

        for (Integer node : nodeMap.keySet()) {
            rGraph.put(node, new HashSet<>(nodeMap.get(node).getOutNodes()));
        }

        int max_flow = 0;

        while (bfs(rGraph, source, sink, parent)) {
            int cur = parent.get(sink);
            while (cur != source) {
                rGraph.remove(cur);
                petalSet.add(cur);
                cur = parent.get(cur);
            }
            max_flow += 1;
            parent.clear();
        }
        return new Tuple(max_flow, petalSet);
    }

    private static boolean bfs(Map<Integer, Set<Integer>> rGraph, int source, int sink, Map<Integer, Integer> parent) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();

        queue.add(source);
        visited.add(source);

        // Standard BFS Loop
        while (!queue.isEmpty()) {
            int current = queue.remove();

            if (rGraph.containsKey(current)) {
                for (int outNode : rGraph.get(current)) {
                    if (!visited.contains(outNode)) {
                        parent.put(outNode, current);
                        // reached sink - break here
                        if (outNode == sink) {
                            return true;
                        }
                        queue.add(outNode);
                        visited.add(outNode);
                    }
                }
            }
        }
        return visited.contains(sink);
    }
}
