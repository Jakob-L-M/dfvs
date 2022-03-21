import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DFAS {


    public static void main(String[] args) throws IOException {
        int count = 0;
        File allInst = new File("instances");
        Map<String,Integer> solutions = utils.loadSolSizes();
        //BufferedWriter writer = new BufferedWriter(new FileWriter("dfas_heur.txt", true));
        for (File instFolder : allInst.listFiles()) {
            if (!instFolder.isDirectory() || instFolder.getName().contains("sheet5")) continue;
            for (File instance : instFolder.listFiles()) {
                //if (count > 50) break;
                String name = instance.getPath().substring(instance.getPath().indexOf("instances") + 10).replace("\\", "/");

                if (!solutions.containsKey(name) || solutions.get(name) < 0) continue;

                Set<Integer> solution = runInst(instance.getPath());

                //String timeString = Long.toString((int) time/60_000_000_000L) + ":" + Long.toString((int)((time/1_000_000_000L)%60)) +"."+Long.toString((time/10_000_000L)%6000);
                System.out.println(count++ + ": " + name + " hK:" + solution.size() + " oK:" + solutions.get(name) + " diff:" + (solution.size() - solutions.get(name)));
                //writer.write("../" + instance.getPath().replace('\\', '/') + " " + solution.size() + " " + timeString + "\n");
            }
        }
        //writer.close();
    }

    public static Set<Integer> runInst(String filename) {
        DirectedGraph dfvsGraph = new DirectedGraph(filename);
        //Knoten werden im DFAS-Konstruktor neu benannt, dort aber immer nur die v+-Knoten
        //in die Lösungsmenge aufgenommen. Das Cleaning liefert unter Umständen gleichnamige
        //Integer. Daher hier negativ in die Lösung aufgenommen.

        dfvsGraph.k = Integer.MAX_VALUE;
        Set<Integer> solution = new HashSet<>(dfvsGraph.rootClean());

        DirectedGraph dfasGraph = new DirectedGraph(dfvsGraph, true);

        //Solange der Graph nicht gelöscht ist, werden weiter Knoten in die topologische
        //Sortierung aufgenommen und potentiell gelöscht.
        Set<Integer> addToTopoLeft = new HashSet<>();
        Set<Integer> addToTopoRight = new HashSet<>();

        Set<Integer> notInTopo = new HashSet<>(dfasGraph.nodeMap.keySet());

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
        //System.out.println(addToTopoLeft);
        //System.out.println(addToTopoRight);

        if (dfasGraph.nodeMap.size() > 0) {
            Integer firstNode = Math.abs(notInTopo.iterator().next());
            addToTopoLeft.add(firstNode);
            notInTopo.remove(firstNode);
        } else {
            return solution;
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
                    else if (dfasGraph.getNode(nodeID).getOutDegree() == 0) {
                        addToTopoRight.add(nodeID);
                        deleteNodes.add(nodeID);
                        change = true;
                    }
                }
                notInTopo.removeAll(deleteNodes);
            }

            Integer vPlus = 0;
            int backEdgeMax = 0;
            Integer vFree = 0;
            for (Integer notTopoNode : notInTopo) {
                Set<Integer> backConnections = new HashSet<>(dfasGraph.getNode(notTopoNode).getOutNodes());

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
        //System.out.println(solution);
        return solution;
    }


}
