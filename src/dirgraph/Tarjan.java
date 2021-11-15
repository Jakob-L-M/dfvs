import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.*;

public class Tarjan {

    public static final int unvisited = -1;
    private int n;
    private DirectedGraph graph;
    private int nodeCount, compCount;
    private int[] sccIDs, lowLinks, tarjanIDs;
    private boolean[] stacked;
    BiMap<Integer, Integer> dict = HashBiMap.create();
    private Deque<Integer> stack;

    public Tarjan(DirectedGraph graph) {
        this.graph = graph;
        n = graph.size();
        tarjanIDs = new int[n];
        int count = 0;
        for (Integer key : graph.nodeMap.keySet()) {
            dict.put(key, count++);
        };
        lowLinks = new int[n];
        sccIDs = new int[n];
        stacked = new boolean[n];
    }

    public int[] findSCCsTarjan() {
        tarjanIDs = new int[n];
        lowLinks = new int[n];
        sccIDs = new int[n];
        stacked = new boolean[n];
        stack = new ArrayDeque<>();
        Arrays.fill(tarjanIDs, unvisited);
        for (int i = 0; i < n; i++) {
            if(tarjanIDs[i] == unvisited) {
                tarjanDFS(i);
            }
        }
        return lowLinks;
    }

    private void tarjanDFS(int i) {
        stack.push(i);
        stacked[i] = true;
        tarjanIDs[i] = lowLinks[i] = nodeCount++;
        for(int post : graph.nodeMap.get(i).getPostNodes()) {
            post = dict.get(post);
            if(tarjanIDs[post] == unvisited) tarjanDFS(post);
            if(stacked[post]) lowLinks[i] = Math.min(lowLinks[i], lowLinks[post]);
        }
        if(tarjanIDs[i] == lowLinks[i]) {
            while(true) { //for(int node = stack.pop();; node = stack.pop()) {
                int node = stack.pop();
                stacked[node] = false;
                lowLinks[node] = tarjanIDs[i];
                if(node == i) break;
            }
            compCount++;
        }
    }

    public int[] getLowLinks() {
        return lowLinks;
    }

    public Map<Integer,ArrayList<Integer>>/*Collection<ArrayList<Integer>>*/ getSCCs() {
        Map<Integer, ArrayList<Integer>> sccMap = new HashMap<>();
        for(int i = 0; i < n; i++) {
            if(!sccMap.containsKey(lowLinks[i])) {
                sccMap.put(lowLinks[i], new ArrayList<>());
                sccMap.get(lowLinks[i]).add(dict.inverse().get(i));
            }
            else {
                sccMap.get(lowLinks[i]).add(dict.inverse().get(i));
            }
        }
        return sccMap;
    }
}
