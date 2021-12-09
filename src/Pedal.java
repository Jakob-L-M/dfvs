import java.util.*;

public class Pedal {

    public static int findDisjointPaths(Map<Integer, DirectedNode> nodeMap, int s, int t) {
        int u, v;

        // Residual graph where rGraph[i][j] indicates
        // residual capacity of edge from i to j (if there
        // is an edge. If rGraph[i][j] is 0, then there is not)
        Map<Integer, Map<Integer, Integer>> rGraph = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();

        for (Integer node : nodeMap.keySet()) {
            rGraph.put(node, new HashMap<>());
            for (Integer outNode : nodeMap.get(node).getOutNodes()) {
                rGraph.get(node).put(outNode, 1);
            }
            parent.put(node, 0);
        }

        int max_flow = 0; // There is no flow initially

        // Augment the flow while there is path
        // from source to sink
        while (bfs(rGraph, s, t, parent)) {
            // Find minimum residual capacity of the edges
            // along the path filled by BFS. Or we can say
            // find the maximum flow through the path found.
            int path_flow = Integer.MAX_VALUE;

            for (v = t; v != s; v = parent.get(v)) {
                u = parent.get(v);
                path_flow = Math.min(path_flow, rGraph.get(u).containsKey(v) ? 1 : 0);
            }

            // update residual capacities of the edges and
            // reverse edges along the path
            for (v = t; v != s; v = parent.get(v)) {
                u = parent.get(v);
                rGraph.get(u).put(v, rGraph.get(u).get(v) - path_flow);
                rGraph.get(v).put(u, (rGraph.get(v).containsKey(u) ? 1 : 0) + path_flow);
            }

            // Add path flow to overall flow
            max_flow += path_flow;
        }

        // Return the overall flow (max_flow is equal to
        // maximum number of edge-disjoint paths)

        return max_flow;
    }

    private static boolean bfs(Map<Integer, Map<Integer, Integer>> rGraph, int s, int t, Map<Integer, Integer> parent) {
        // Create a visited array and
        // mark all vertices as not visited

        Map<Integer, Boolean> visited = new HashMap<>();


        // Create a queue, enqueue source vertex and
        // mark source vertex as visited
        Queue<Integer> q = new LinkedList<>();
        q.add(s);
        visited.put(s, true);
        parent.put(s, -1);

        // Standard BFS Loop
        while (!q.isEmpty()) {
            int u = q.peek();
            q.remove();

            for (int v : rGraph.get(u).keySet()) {
                if (!visited.containsKey(v) && rGraph.get(u).get(v) > 0) {
                    q.add(v);
                    parent.put(v, u);
                    visited.put(v, true);
                }
            }
        }

        // If we reached sink in BFS
        // starting from source, then
        // return true, else false
        return (visited.containsKey(t));
    }
}
