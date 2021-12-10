import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.*;

public class newTarjan {
    private final int n;
    private final DirectedGraph graph;
    BiMap<Integer, Integer> dict = HashBiMap.create();
    private int nodeCount = 1;
    private int[] lowLinks, tarjanIDs;
    private boolean[] stacked;
    private Deque<Integer> stack;

    public newTarjan(DirectedGraph graph) {
        this.graph = graph;
        n = graph.size();
        tarjanIDs = new int[n];
        int count = 0;
        for (Integer key : graph.nodeMap.keySet()) {
            dict.put(key, count++);
        }
        lowLinks = new int[n];
        stacked = new boolean[n];
    }

    public int[] findSCCsTarjan() {
        tarjanIDs = new int[n];
        lowLinks = new int[n];
        stacked = new boolean[n];
        stack = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (tarjanIDs[i] == 0) {
                tarjanDFS(i);
            }
        }
        return lowLinks;
    }

    private void tarjanDFS(int i) {
        stack.push(i);
        stacked[i] = true;
        tarjanIDs[i] = lowLinks[i] = nodeCount++;
        for (int post : graph.nodeMap.get(dict.inverse().get(i)).getOutNodes()) {
            post = dict.get(post);
            if (tarjanIDs[post] == 0) tarjanDFS(post);
            if (stacked[post]) lowLinks[i] = Math.min(lowLinks[i], lowLinks[post]);
        }
        if (tarjanIDs[i] == lowLinks[i]) {
            while (true) { //for(int node = stack.pop();; node = stack.pop()) {
                int node = stack.pop();
                stacked[node] = false;
                lowLinks[node] = tarjanIDs[i];
                if (node == i) break;
            }
        }
    }

    public int[] getLowLinks() {
        return lowLinks;
    }

    public Map<Integer, ArrayList<Integer>> getSCCs() {
        findSCCsTarjan();
        Map<Integer, ArrayList<Integer>> sccMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (!sccMap.containsKey(lowLinks[i])) {
                sccMap.put(lowLinks[i], new ArrayList<>());
            }
            sccMap.get(lowLinks[i]).add(dict.inverse().get(i));
        }
        return sccMap;
    }

    public Set<DirectedGraph> getSCCGraphs() {
        Set<DirectedGraph> sccGraphSet = new HashSet<>();
        BiMap<String, Integer> inverseDict = graph.dict;//
        Map<Integer, ArrayList<Integer>> scc = getSCCs();
        for (int i : scc.keySet()) {
            DirectedGraph addGraph = graph.inducedSubGraph(new HashSet(scc.get(i)));
            sccGraphSet.add(addGraph);
        }
        return sccGraphSet;
    }
}
