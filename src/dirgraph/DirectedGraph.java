package dirgraph;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.*;


public class DirectedGraph {
    Map<Integer, DirectedNode> nodeMap = new HashMap<>();
    BiMap<String, Integer> dict = HashBiMap.create();

    public DirectedGraph(Collection<DirectedNode> nodes) {
        for (DirectedNode node : nodes) {
            nodeMap.put(node.getNodeID(), node);
        }
    }

    DirectedGraph(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String currentLine;

            // using a unique id for each node
            int count = 0;

            while ((currentLine = reader.readLine()) != null) {

                if (currentLine.charAt(0) == '#') continue;
                String[] nodes = currentLine.split(" ");

                // creating a new node if its the first edge of that node
                if (!dict.containsKey(nodes[0])) dict.put(nodes[0], count++);
                if (!dict.containsKey(nodes[1])) dict.put(nodes[1], count++);

                addNode(dict.get(nodes[0]));
                addNode(dict.get(nodes[1]));

                addEdge(dict.get(nodes[0]), dict.get(nodes[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DirectedGraph(DirectedGraph that) {
        for (Integer key : that.nodeMap.keySet()) {
            DirectedNode nodeToCopy = that.nodeMap.get(key);
            this.nodeMap.put(key, nodeToCopy.clone());
        }
    }

    public void fixNode(Integer nodeID) {
        DirectedNode node = nodeMap.get(nodeID);
        node.fixNode();
    }

    public boolean isFixed(Integer nodeID) {
        DirectedNode node = nodeMap.get(nodeID);

        try {
            return node.isFixed();
        } catch (NullPointerException e) {
            return true;
            //e.printStackTrace();
        }
    }

    public Set<DirectedNode> cleanGraph() {
        Set<DirectedNode> removedNodes = new HashSet<>();
        boolean change = false;
        Set<Integer> nodes = new HashSet<>(nodeMap.keySet());
        for (Integer nid : nodes) {
            DirectedNode node = nodeMap.get(nid);
            if (node.getIn_degree() == 0 || node.getOut_degree() == 0) {
                removedNodes.add(nodeMap.get(nid));
                removeNode(nid);
                change = true;
            }
        }
        if (change) removedNodes.addAll(cleanGraph());
        return removedNodes;
    }

    public boolean addNode(Integer nid) {
        if (!nodeMap.containsKey(nid)) {
            nodeMap.put(nid, new DirectedNode(nid));
            return true;
        }
        return false;
    }

    public boolean addEdge(Integer preID, Integer postID) {
        DirectedNode preNode = nodeMap.get(preID);
        DirectedNode postNode = nodeMap.get(postID);
        return preNode.addPostNode(postID) &&
                postNode.addPreNode(preID);
    }

    public boolean removeEdge(Integer preID, Integer postID) {
        DirectedNode preNode = nodeMap.get(preID);
        DirectedNode postNode = nodeMap.get(postID);
        if (nodeMap.containsKey(preID) && nodeMap.containsKey(postID)) {
            preNode.removePostNode(postID);
            postNode.removePreNode(preID);
            return true;
        }
        return false;
    }

    public boolean removeNode(Integer nodeID) {
        if (!nodeMap.containsKey(nodeID)) return false;
        DirectedNode node = nodeMap.get(nodeID);
        ArrayList<Integer> neighbours = new ArrayList<>();
        neighbours.addAll(node.getPostNodes());
        neighbours.addAll(node.getPreNodes());
        for (Object neighbourID : neighbours) {
            removeEdge(nodeID, (int) neighbourID);
            removeEdge((int) neighbourID, nodeID);
        }
        nodeMap.remove(nodeID);
        return true;
    }

    public Set<DirectedNode> removeClean(Integer nodeID) {
        Set<DirectedNode> removedNodes = new HashSet<>();
        removedNodes.add(nodeMap.get(nodeID));
        removeNode(nodeID);
        removedNodes.addAll(cleanGraph());
        return removedNodes;
    }

    public void reconstructNode(DirectedNode node) {
        nodeMap.put(node.getNodeID(), node);
        for (int preID : node.getPreNodes()) {
            addEdge(preID, node.getNodeID());
        }
        for (int postID : node.getPostNodes()) {
            addEdge(node.getNodeID(), postID);
        }
    }

    public void reconstructNodes(Set<DirectedNode> nodes) {
        for (DirectedNode node : nodes) {
            nodeMap.put(node.getNodeID(), node);
        }
        for (DirectedNode node : nodes) {
            for (int preID : node.getPreNodes()) {
                addEdge(preID, node.getNodeID());
            }
            for (int postID : node.getPostNodes()) {
                addEdge(node.getNodeID(), postID);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder graphString = new StringBuilder();

        for (Object node : nodeMap.values()) {
            graphString.append(node.toString()).append("\n");
        }

        return graphString.toString();
    }

    public Deque<Integer> findCycle() {
        HashMap<Integer, Boolean> visited = new HashMap<>();
        for (Integer i : nodeMap.keySet()) {
            visited.put(i, false);
        }
        for (Integer start : nodeMap.keySet()) {
            if (visited.get(start)) continue;
            Deque<Integer> deque = new ArrayDeque<>();
            deque.push(start);
            while (!deque.isEmpty()) {
                int current = deque.peek();
                if (visited.get(current) != null && !visited.get(current)) {
                    visited.put(current, true);
                    DirectedNode currentNode = nodeMap.get(current);
                    if (currentNode.getOut_degree() == 0) deque.pop();
                    for (int dest : currentNode.getPostNodes()) {
                        deque.push(dest);
                        if (visited.get(dest) != null && visited.get(dest)) {
                            while (deque.peekLast() != dest) deque.pollLast();
                            deque.pop();
                            return deque;
                        }
                    }
                } else {
                    deque.pop();
                }
            }
        }
        return null;
    }

    //Set to just one cycle!
    public Set<Deque<Integer>> findCycles() {
        Set<Deque<Integer>> cycles = new HashSet<>();
        Map<Integer, Boolean> visited = new HashMap<>();

        for (Integer i : nodeMap.keySet()) {
            visited.put(i, false);
        }

        for (Integer start : nodeMap.keySet()) {
            if (visited.get(start)) continue;
            Deque<Integer> deque = new ArrayDeque<>();
            deque.push(start);
            while (!deque.isEmpty()) {
                int current = deque.peek();
                if (visited.get(current) != null && !visited.get(current)) {
                    visited.put(current, true);
                    DirectedNode currentNode = nodeMap.get(current);
                    if (currentNode.getOut_degree() == 0) deque.pop();
                    for (int dest : currentNode.getPostNodes()) {
                        deque.push(dest);
                        if (visited.get(dest) != null && visited.get(dest)) {
                            while (deque.peekLast() != dest) deque.pollLast();
                            deque.pop();
                            cycles.add(copyDeque(deque));
                            if (cycles.size() > 5) return cycles;
                        }
                    }
                } else {
                    deque.pop();
                }
            }
        }
        return cycles;
    }

    private Deque<Integer> copyDeque(Deque<Integer> deque) {
        return new ArrayDeque<>(deque);
    }

    public static int findCrossing(Set<Deque<Integer>> cycles) {
        if (cycles.isEmpty()) return -1;
        Map<Integer, Integer> occurrences = new HashMap<>();
        for (Deque<Integer> cycle : cycles) {
            for (Integer v : cycle) {
                if (occurrences.containsKey(v)) occurrences.put(v, occurrences.get(v) + 1);
                else occurrences.put(v, 1);
            }
        }
        for (Integer key : occurrences.keySet()) {
            if (occurrences.get(key).equals(Collections.max(occurrences.values()))) {
                return key;
            }
        }
        return -1;
    }

    public Deque<Integer> findBusyCycle() {
        Set<Deque<Integer>> cycles = findCycles();
        if (cycles == null) return null;
        int crossingID = findCrossing(cycles);
        Deque<Integer> smallCycle = null;
        for (Deque<Integer> cycle : cycles) {
            if (cycle.contains(crossingID) && smallCycle == null
                    || cycle.contains(crossingID) && cycle.size() < smallCycle.size()) {
                smallCycle = cycle;
            }
        }
        return smallCycle;
    }

    public void visualize(String name) {

        GraphDraw frame = new GraphDraw(name);

        frame.setSize(900,900);

        frame.setVisible(true);

        for(DirectedNode node: nodeMap.values()) {
            frame.addNode(node.getNodeID());
            for (int i: node.getPostNodes()) {
                frame.addEdge(node.getNodeID(), i);
            }
        }
    }
    
    public DirectedGraph burningBridges() {
    	DirectedGraph copy = new DirectedGraph(this);
    	while(copy.hasBridge()) {
    		for (Integer node : nodeMap.keySet()) {
        		if (copy.nodeMap.get(node).getIn_degree() == 1 && nodeMap.get(node).getOut_degree() == 1) {
        			for (Integer innode: copy.nodeMap.get(node).getPreNodes()) {
        				for (Integer outnode: copy.nodeMap.get(node).getPostNodes()) {
        					copy.addEdge(innode, outnode);
            			}
        			}
        			copy.removeNode(nodeMap.get(node).getNodeID());
        		}

        	}
    	}
    	return copy;
    }
    
    public boolean hasBridge() {
    	for (DirectedNode node : nodeMap.values()) {
    		if (node.getIn_degree() == 1 && node.getOut_degree() == 1) {
    			return true;
    		}
    	}
    	return false;
    }

    public class GraphDraw extends JFrame {
        int width;
        int height;
        int num_vertices;

        Map<Integer, Node> nodes;
        List<Edge> edges;

        public GraphDraw(String name) {
            this.setTitle(name);
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            nodes = new HashMap<>();
            edges = new ArrayList<>();
            width = 20;
            height = 20;
            num_vertices = nodeMap.values().size();
        }

        class Node {
            int index;
            int x,y;

            public Node(int index) {
                x = 450 - (int) Math.round(350* Math.cos(index*2*Math.PI/num_vertices));
                y = 450 + (int) Math.round(350* Math.sin(index*2*Math.PI/num_vertices));
                this.index = index;
            }
        }

        class Edge {
            int i,j;

            public Edge(int ii, int jj) {
                i = ii;
                j = jj;
            }
        }

        public void addNode(int name) {
            nodes.put(name, new Node(name));
            this.repaint();
        }
        public void addEdge(int i, int j) {
            edges.add(new Edge(i,j));
            this.repaint();
        }

        public void paint(Graphics g) { // draw the nodes and edges
            FontMetrics f = g.getFontMetrics();
            int nodeHeight = Math.max(height, f.getHeight());

            g.setColor(Color.black);
            for (Edge e : edges) {
                g.drawLine(nodes.get(e.i).x, nodes.get(e.i).y,
                        nodes.get(e.j).x, nodes.get(e.j).y);
                g.setColor(Color.black);
                g.fillOval((int) Math.floor(0.9*nodes.get(e.i).x + 0.1*nodes.get(e.j).x) - 5, (int) Math.floor(0.9*nodes.get(e.i).y + 0.1*nodes.get(e.j).y) -5, 10, 10);
            }

            for (Node n : nodes.values()) {
                int nodeWidth = Math.max(width, f.stringWidth(Integer.toString(n.index))+width/2);
                g.setColor(Color.white);
                g.fillOval(n.x-nodeWidth/2, n.y-nodeHeight/2,
                        nodeWidth, nodeHeight);
                g.setColor(Color.black);
                g.drawOval(n.x-nodeWidth/2, n.y-nodeHeight/2,
                        nodeWidth, nodeHeight);

                g.drawString(String.valueOf(n.index), n.x-f.stringWidth(Integer.toString(n.index))/2,
                        n.y+f.getHeight()/2);
            }
        }
    }
}
