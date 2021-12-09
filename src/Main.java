import java.util.*;

public class Main {

    public static int recursions;
    public static Map<String, Set<Integer>> graphHash = new HashMap<>();

    public static Set<Integer> dfvsBranch(DirectedGraph graph, int k, boolean isScc) {

        Stackings stacking = new Stackings(graph);
        int lowerBound = stacking.findCirclePacking().size();
        if (k < lowerBound) return null;

        // clean the graph and save all selfCycle-Nodes that have been removed during cleaning
        Set<Integer> selfCycles = graph.cleanGraph();//new HashSet<>();

        // If there should be more selfCycles than k, we can break here.
        if (selfCycles.size() > k) {
            return null;
        }

        // Reduce the leftover budget by selfCycles.size()
        k = k - selfCycles.size();


        if(!isScc) {
            //init tarjan
            newTarjan tarjan = new newTarjan(graph);

            // calculate SCCs and create an iterator to access them
            Set<DirectedGraph> subGraphs = tarjan.getSCCGraphs();
            Iterator<DirectedGraph> it = subGraphs.iterator();

            // If there is only one sub-graph we can skip solving sub-graphs
            if (subGraphs.size() > 1) {

                // HashSet to store the solutions of sub-graphs
                Set<Integer> dfvs_mult = new HashSet<>();

                while (it.hasNext() && dfvs_mult.size() <= k) {

                    // Store the next Strongly Connected Component and create its hash
                    DirectedGraph scc = it.next();
                    String hash = scc.hash();

                    if (graphHash.containsKey(hash)) {

                        // We already solved the sub-graph at some point
                        Set<Integer> knownSolution = graphHash.get(hash);

                        // Check if adding the solution still satisfies k.
                        if (k - dfvs_mult.size() >= knownSolution.size()) {

                            // Add the solution and move to the next sub-graph
                            dfvs_mult.addAll(knownSolution);
                            continue;

                        } else {

                            // We know that there is not enough capacity left to solve any other grapf
                            // break branching
                            return null;
                        }
                    }

                    // solve the sub-graph with a reduced upper limit
                    Set<Integer> solutionSCC = dfvsSolve(scc, k - dfvs_mult.size(), true);

                    // Is there a valid solution
                    if (solutionSCC == null) {
                        return null;
                    } else {

                        // save the solution and add it the Set of Nodes
                        graphHash.put(hash, solutionSCC);
                        dfvs_mult.addAll(solutionSCC);
                    }

                }

                // Check why the while-loop broke
                if (dfvs_mult.size() <= k) {

                    // while-loop broke due to iterator having no sub-graphs left
                    // found a solution that satisfies k
                    dfvs_mult.addAll(selfCycles);
                    return dfvs_mult;
                }
                // No Solution found, sub-graphs can not be solved by Limit.
                // We should never reach here
                return null;
            }
        }

        // find a Cycle
        Deque<Integer> cycle = graph.findBestCycle();

        if (cycle == null) {

            // The graph does not have any cycles
            // We will return the found selfCycles if they are in our current limit
            return selfCycles;

        }

        for (Integer v : cycle) {

            // create a copy -> we will branch off of that copy
            DirectedGraph graphCopy = new DirectedGraph(graph);

            // delete a vertex of the circle and branch for here
            graphCopy.removeNode(v);

            // increment recursions to keep track of tree size
            recursions++;

            // branch with a maximum cost of k
            // -1: Just deleted a node
            // -selfCycles.size(): nodes that where removed during graph reduction
            Set<Integer> dfvs = dfvsBranch(graphCopy, k - 1, false);

            // if there is a valid solution in the recursion it will be returned
            if (dfvs != null) {

                // Add the nodeId of the valid solution and all selfCycles
                dfvs.add(v);
                dfvs.addAll(selfCycles);

                return dfvs;
            }
            else {
                graph.getNode(v).fixNode();
            }
        }
        return null;
    }

    public static Set<Integer> dfvsSolve(DirectedGraph graph, int max_k, boolean isScc) {
        Set<Integer> selfCycle = graph.cleanGraph();
        Stackings stacking = new Stackings(graph);
        int lowerBound = stacking.findCirclePacking().size();
        int k = lowerBound;
        Set<Integer> dfvs = null;

        while (dfvs == null) {
            if (k + selfCycle.size() > max_k) {
                return null;
            }
            dfvs = dfvsBranch(graph, k, isScc);
            k++;
        }
        dfvs.addAll(selfCycle);
        return dfvs;
    }

    public static void main(String[] args) {
        //DirectedGraph graph = new DirectedGraph(args[0]);
        DirectedGraph graph = new DirectedGraph("instances/complex/biology-n_45-m_326-p_0.5-16");
        //
        long time = -System.nanoTime();
        Set<Integer> solution = dfvsSolve(graph, Integer.MAX_VALUE, false);
        time += System.nanoTime();
        if (solution != null) {
            for (int i : solution) {
                System.out.println(i);
            }
            System.out.println(solution.size());
        }
        //System.out.println("time: " + time/1000000000L);
        System.out.println("#recursive steps: " + recursions);
    }
}