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
        int count = 0;
        File allInst = new File("instances");
        BufferedWriter writer = new BufferedWriter(new FileWriter("dfas_heur.txt", true));
        for (File instFolder : allInst.listFiles()) {
            if (!instFolder.isDirectory() || instFolder.getName().contains("sheet5")) continue;
            for (File instance : instFolder.listFiles()) {
                //if (count > 50) break;
                long time = -System.nanoTime();
                Set<Integer> solution = runInst(instance.getPath());
                time += System.nanoTime();
                String timeString = Long.toString((int) time/60_000_000_000L) + ":" + Long.toString((int)((time/1_000_000_000L)%60)) +"."+Long.toString((time/10_000_000L)%6000);
                System.out.println(count++ + ": " + instance.getName() + " " + timeString + " " + solution.size());
                writer.write("../" + instance.getPath().replace('\\', '/') + " " + solution.size() + " " + timeString + "\n");
            }
        }
        writer.close();
    }

    public static Set<Integer> runInst(String filename) {
        DirectedGraph dfvsGraph = new DirectedGraph(filename);
        Set<Integer> solution = new HashSet<>();
        //Knoten werden im DFAS-Konstruktor neu benannt, dort aber immer nur die v+-Knoten
        //in die Lösungsmenge aufgenommen. Das Cleaning liefert unter Umständen gleichnamige
        //Integer. Daher hier negativ in die Lösung aufgenommen.
        for (Integer u : dfvsGraph.rootClean()) {
            solution.add(u);
        }
        DirectedGraph dfasGraph = new DirectedGraph(dfvsGraph, true);
        //Solange der Graph nicht gelöscht ist, werden weiter Knoten in die topologische
        //Sortierung aufgenommen und potentiell gelöscht.
        Set<Integer> addToTopoLeft = new HashSet<>();
        Set<Integer> addToTopoRight = new HashSet<>();
        Set<Integer> notInTopo = new HashSet<>(dfasGraph.nodeMap.keySet());
        int n = dfasGraph.size();

        //dfasGraph.fillIndegreePriorityQueue();
        //dfasGraph.fillOutdegreePriorityQueue();
        // Sinks und Sources werden kostenlos in die topologische Sortierung aufgenommen

            /*
            //Knoten mit Outdegree 0 aus der Queue werden aufgenommen
            if (!dfasGraph.outDegreeQueue.isEmpty()) {
                while (dfasGraph.outDegreeQueue.peek().getOutDegree() == 0) {
                    Integer nodeID = dfasGraph.outDegreeQueue.poll().getNodeID();
                    addToTopoRight.add(-Math.abs(nodeID));
                    addToTopoLeft.add(Math.abs(nodeID));
                    dfasGraph.removeEdge(-Math.abs(nodeID),Math.abs(nodeID));
                    notInTopo.remove(Math.abs(nodeID));
                    notInTopo.remove(-Math.abs(nodeID));
                    change = true;
                }
            }
            */
            //aktualisiert die beiden PriorityQueues, evtl. Verbesserungsbedarf (nur bzgl. Laufzeit),
            //da diese komplett neu aufgesetzt werden
            //dfasGraph.fillIndegreePriorityQueue();
            //dfasGraph.fillOutdegreePriorityQueue();
        //}
        System.out.println(addToTopoLeft);
        System.out.println(addToTopoRight);

        if (dfasGraph.size() > 0) {
            Integer firstNode = notInTopo.stream().iterator().next();
            addToTopoLeft.add(firstNode);
            notInTopo.remove(firstNode);
        }

        // Vor allem ab hier eigentlich interessant.
        while (!notInTopo.isEmpty()) {
            boolean change = true;
            while (change) {
                change = false;
                //Knoten mit Indegree 0 aus der Queue werden aufgenommen
                Set<Integer> deleteNodes = new HashSet<>();
                for (Integer nodeID : notInTopo) {
                    if (dfasGraph.getNode(nodeID).getInDegree() == 0) {
                        addToTopoLeft.add(nodeID);
                        deleteNodes.add(nodeID);
                        change = true;
                    }
                    if (dfasGraph.getNode(nodeID).getOutDegree() == 0) {
                        addToTopoRight.add(nodeID);
                        deleteNodes.add(nodeID);
                        change = true;
                    }
                }
                notInTopo.removeAll(deleteNodes);
            }

            Integer vPlus = 0;
            Integer backEdgeMax = 0;
            Integer vFree = 0;
            for (Integer notTopoNode : notInTopo) {
                Set<Integer> backConnections = dfasGraph.getNode(notTopoNode).getOutNodes();
                backConnections.retainAll(addToTopoLeft);
                if (backConnections.size() > backEdgeMax) {
                    backEdgeMax = backConnections.size();
                    vPlus = notTopoNode;
                }
                if (backConnections.size() == 0 && vFree == 0 || backConnections.size() == 0 && notTopoNode > 0) {
                    vFree = notTopoNode;
                }
            }
            if (vFree != 0) {
                addToTopoLeft.add(vFree);
                notInTopo.remove(vFree);
            }
            else {
                vPlus = Math.abs(vPlus);
                addToTopoLeft.add(vPlus);
                addToTopoRight.add(-vPlus);
                notInTopo.remove(vPlus);
                notInTopo.remove(-vPlus);
                solution.add(vPlus - 1);
                dfasGraph.removeEdge(-vPlus, vPlus);
            }


            //System.out.println((n- notInTopo.size()) + "/" + notInTopo.size() + "  vFree: " + vFree);
        }
        System.out.println(solution);
        return solution;
    }


}
