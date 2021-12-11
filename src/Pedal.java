import java.util.*;

public class Pedal {

    public static int findDisjointPaths(Map<Integer, DirectedNode> nodeMap, int source, int sink) {
        // Residual graph where rGraph[i][j] indicates
        // residual capacity of edge from i to j (if there
        // is an edge. If rGraph[i][j] is 0, then there is not)
        Map<Integer, Set<Integer>> rGraph = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();

        for (Integer node : nodeMap.keySet()) {
            rGraph.put(node, new HashSet<>(nodeMap.get(node).getOutNodes()));
        }

        int max_flow = 0;

        while (bfs(rGraph, source, sink, parent)) {
            int cur = parent.get(sink);
            while (cur != source) {
                rGraph.remove(cur);
                cur = parent.get(cur);
            }
            max_flow += 1;
            parent.clear();
        }
        return max_flow;
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
                        if (outNode == sink) {
                            return true;
                        }
                        queue.add(outNode);
                        visited.add(outNode);
                    }
                }
            }
        }

        // If we reached sink in BFS
        // starting from source, then
        // return true, else false
        return visited.contains(sink);
    }
}
