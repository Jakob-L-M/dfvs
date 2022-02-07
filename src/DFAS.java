import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

public class DFAS {


    public static void main(String[] args) throws IOException {
        File allInst = new File("instances");
        BufferedWriter writer = new BufferedWriter(new FileWriter("dfas_heur.txt", true));
        for (File instFolder : allInst.listFiles()) {
            if (!instFolder.isDirectory() || instFolder.getName().contains("sheet5")) continue;
            for (File instance : instFolder.listFiles()) {
                long time = -System.nanoTime();
                Set<Integer> solution = runInst(instance.getPath());
                time += System.nanoTime();
                String timeString = Long.toString((int) time/60_000_000_000L) + ":" + Long.toString((int)((time/1_000_000_000L)%60)) +"."+Long.toString((time/10_000_000L)%6000);
                System.out.println(instance.getName() + " " + timeString + " " + solution.size());
                writer.write(instance.getPath() + " " + solution.size() + " " + timeString + "\n");
            }
        }
        writer.close();
    }

    public static Set<Integer> runInst(String filename) {
        DirectedGraph dfvsGraph = new DirectedGraph(filename);
        Set<Integer> solution = new HashSet<>();
        for (Integer u : dfvsGraph.rootClean()) {
            solution.add(-u);
        }
        DirectedGraph dfasGraph = new DirectedGraph(dfvsGraph, true);
        //System.out.println(dfasGraph);
        while (!dfasGraph.nodeMap.keySet().isEmpty()) {
            boolean change = true;
            dfasGraph.fillIndegreePriorityQueue();
            dfasGraph.fillOutdegreePriorityQueue();
            while (change) {
                change = false;
                Set<Integer> addToTopo = new HashSet<>();
                if (!dfasGraph.inDegreeQueue.isEmpty()) {
                    while (dfasGraph.inDegreeQueue.peek().getInDegree() == 0) {
                        addToTopo.add(dfasGraph.inDegreeQueue.poll().getNodeID());
                    }
                }
                if (!dfasGraph.outDegreeQueue.isEmpty()) {
                    while (dfasGraph.outDegreeQueue.peek().getOutDegree() == 0) {
                        addToTopo.add(dfasGraph.outDegreeQueue.poll().getNodeID());
                    }
                }
                for (Integer nodeId : addToTopo) {
                    dfasGraph.removeNode(nodeId);
                    dfasGraph.removeNode(-nodeId);
                    change = true;
                }
                dfasGraph.fillIndegreePriorityQueue();
                dfasGraph.fillOutdegreePriorityQueue();
            }
            Integer vMinus = 0;
            int maxHeur = 0;
            if (!dfasGraph.inDegreeQueue.isEmpty()) {
                int minInDegree = dfasGraph.inDegreeQueue.peek().getInDegree();
                vMinus = dfasGraph.inDegreeQueue.peek().getNodeID();
                maxHeur = dfasGraph.getNode(-vMinus).getOutDegree();
                for (DirectedNode node : dfasGraph.inDegreeQueue) {
                    if (node.getInDegree() <= minInDegree) {
                        if (dfasGraph.getNode(-node.getNodeID()).getOutDegree() > maxHeur) {
                            maxHeur = dfasGraph.getNode(-node.getNodeID()).getOutDegree();
                            vMinus = node.getNodeID();
                        }
                    }
                    else {
                        break;
                    }
                }
                vMinus = dfasGraph.inDegreeQueue.peek().getNodeID();
            }
            else if (!dfasGraph.outDegreeQueue.isEmpty()) {
                int minOutDegree = dfasGraph.outDegreeQueue.peek().getOutDegree();
                vMinus = -dfasGraph.outDegreeQueue.peek().getNodeID();
                maxHeur = dfasGraph.getNode(-vMinus).getInDegree();
                for (DirectedNode node : dfasGraph.outDegreeQueue) {
                    if (node.getInDegree() <= minOutDegree) {
                        if (dfasGraph.getNode(-node.getNodeID()).getInDegree() > maxHeur) {
                            maxHeur = dfasGraph.getNode(-node.getNodeID()).getInDegree();
                            vMinus = node.getNodeID();
                        }
                    }
                    else {
                        break;
                    }
                }
                vMinus = dfasGraph.outDegreeQueue.peek().getNodeID();
            }
            //System.out.println("deleting edge: (" + vMinus + ", " + -vMinus + ")");
            if (vMinus != 0) solution.add(Math.abs(vMinus));
            dfasGraph.removeNode(vMinus);
            dfasGraph.removeNode(-vMinus);
        }
        return solution;
    }
}
