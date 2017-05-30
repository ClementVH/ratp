import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map.Entry;

import java.util.concurrent.ThreadLocalRandom;

import main.Graph;
import main.Node;
import main.Edge;
import main.CSVUtils;

import processing.core.PApplet;

public class main extends PApplet {

    public static boolean update = true;

    public static Graph graph = null;

    public static void main(String[] args) {

        ////////////////////////////////////// Init graph from raw data of RATP

        // try {
        //     graph = initGraphFromScratch();
        // } catch (Exception e) {}

        ////////////////////////////////////// Save data to csv files

        // try {
        //     graph.saveNodes("nodes_0");
        //     graph.saveEdges("edges_0");
        // } catch (Exception e) {}

        ////////////////////////////////////// Load states file and color the graph

        // loadStateData(0);

        ////////////////////////////////////// Compute btw for one state to another

        // int from = 1;
        // int to = 2;

        // computeBtw(from, to);

        //////////////////////////////////////

        PApplet.main("main");
    }

    public void settings() {
        size(1900, 1000);
    }

    public void draw() {
        fill(200);

        if (update) {
            graph.display(this);
            update = false;
        }
    }

    public static void computeBtw(int from, int to) {

        for (int index = from; index < to; index ++) {

            graph = null;

            try {
                graph = initGraphFromCsv("states/nodes_" + index, "states/edges_" + index);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            graph.graphDiameterDijkstra();

            ArrayList<Edge> maxs = graph.getMaxsBtw();

            for (Edge edge : maxs) {
                System.out.println(edge);
                edge.del = true;
                edge.node1.neighbors.remove((Object) edge.node2.id);
                edge.node2.neighbors.remove((Object) edge.node1.id);
            }

            try {
                graph.saveNodes("nodes_" + (index + 1));
                graph.saveEdges("edges_" + (index + 1));
            } catch (Exception e) {}

        }

        colorGraph();

    }

    public static void loadStateData(int index) {
        try {
            graph = initGraphFromCsv("states/nodes_" + index, "states/edges_" + index);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        colorGraph();
    }

    public static void colorGraph() {

        graph.resetNodes();

        int colorIndex = 0;

        Iterator it = graph.nodes.entrySet().iterator();
        while (it.hasNext()) {
            Entry pair = (Entry) it.next();

            Node start = (Node) pair.getValue();

            if (start.colorVisited)
                continue;

            start.color = new int[] {
                ThreadLocalRandom.current().nextInt(0, 256),
                ThreadLocalRandom.current().nextInt(0, 256),
                ThreadLocalRandom.current().nextInt(0, 256)
            };

            Queue queue = new LinkedList();
            queue.add(start);
            start.visited = true;

            Node node = null;

            while(!queue.isEmpty()) {
                node = (Node) queue.remove();
                // System.out.println(node.name);
                for(int n : node.neighbors) {
                    Node neighbor = graph.nodes.get(n);
                    if (!neighbor.visited) {
                        queue.add(neighbor);
                        neighbor.visited = true;
                        neighbor.colorVisited = true;
                        neighbor.color = start.color;
                        neighbor.previous = node.id;
                    }
                }
            }

            colorIndex ++;
        }

        graph.resetNodes();
    }

    public static Graph initGraphFromScratch() throws IOException {
        return new Graph(new String[]
            {
                "../src/stops/stops_1.txt",
                "../src/stops/stops_2.txt",
                "../src/stops/stops_3.txt",
                "../src/stops/stops_3b.txt",
                "../src/stops/stops_4.txt",
                "../src/stops/stops_5.txt",
                "../src/stops/stops_6.txt",
                "../src/stops/stops_7.txt",
                "../src/stops/stops_7b.txt",
                "../src/stops/stops_8.txt",
                "../src/stops/stops_9.txt",
                "../src/stops/stops_10.txt",
                "../src/stops/stops_11.txt",
                "../src/stops/stops_12.txt",
                "../src/stops/stops_13.txt",
                "../src/stops/stops_14.txt",
            }, new String[]
            {
                "../src/stop-times/stop_times_1.txt",
                "../src/stop-times/stop_times_2.txt",
                "../src/stop-times/stop_times_3.txt",
                "../src/stop-times/stop_times_3b.txt",
                "../src/stop-times/stop_times_4.txt",
                "../src/stop-times/stop_times_5.txt",
                "../src/stop-times/stop_times_6.txt",
                "../src/stop-times/stop_times_7.txt",
                "../src/stop-times/stop_times_7b.txt",
                "../src/stop-times/stop_times_8.txt",
                "../src/stop-times/stop_times_9.txt",
                "../src/stop-times/stop_times_10.txt",
                "../src/stop-times/stop_times_11.txt",
                "../src/stop-times/stop_times_12.txt",
                "../src/stop-times/stop_times_13.txt",
                "../src/stop-times/stop_times_14.txt",
            }
        );
    }

    public static Graph initGraphFromCsv(String path) throws IOException {

        Graph graph = new Graph();

        graph.constructGraphFromCsv("../src/" + path + ".csv");

        return graph;
    }

    public static Graph initGraphFromCsv(String path, String path2) throws IOException {

        Graph graph = new Graph();

        graph.constructGraphFromCsv("../src/" + path + ".csv", "../src/" + path2 + ".csv");

        return graph;
    }
}
