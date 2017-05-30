package main;

import java.util.HashMap;

public class Edge {

    public Node node1;
    public Node node2;

    String index;

    int betweenness = 0;

    public boolean del = false;

    public Edge () {}

    public Edge(Node n1, Node n2) {

        index = (n1.id > n2.id)? (n1.id + "" + n2.id) : (n2.id + "" + n1.id);
        betweenness = 1;
        node1 = n1;
        node2 = n2;
    }

    public String getCsvData() {
        return this.index + ";" + this.node1.id + ";" + this.node2.id + ";" + this.betweenness + ";" + this.del;
    }

    @Override
    public String toString() {
        return  "Edge : " + "\n" +
                "Betweeness : " + betweenness + "\n" +
                "Deleted ? :" + this.del + "\n" +
                node1.toString() + "\n" +
                node2.toString();
    }

    public void constructFromCsvData(String[] data, HashMap<Integer, Node> nodes) {
        index = data[0];
        node1 = nodes.get(Integer.parseInt(data[1]));
        node2 = nodes.get(Integer.parseInt(data[2]));
        del = Boolean.parseBoolean(data[4]);
        betweenness = 0;
    }
}