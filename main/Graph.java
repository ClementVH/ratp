package main;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import main.Node;
import main.Edge;

import processing.core.PApplet;

/**
 * Graph Class
 */
public class Graph
{
  // Store each subway station as a node in this Hash Map
  public HashMap<Integer, Node> nodes;

  // Map indexed by the name of each station, with value the ids of station that share this name
  public HashMap<String, ArrayList<Integer>> namesIds;

  // Map of all edges (calculated in dijkstra and bfs)
  public HashMap<String, Edge> edges;

  // The diameter of the graph
  public int diameter = 0;

  // The longest path
  public ArrayList<Integer> longestPath = new ArrayList<Integer>();

  // The distance of the longest path
  public double maxDistance = 0;

  public Graph() {
    // Initiate nodes Hash Map
    nodes = new HashMap<Integer, Node>();

    // Initiate namesIds
    namesIds = new HashMap<String, ArrayList<Integer>>();

    // Initiate edges
    edges = new HashMap<String, Edge>();
  }

  public Graph(String[] stopsList, String[] stopTimesList) throws IOException
  {
    // Initiate nodes Hash Map
    nodes = new HashMap<Integer, Node>();

    // Initiate namesIds
    namesIds = new HashMap<String, ArrayList<Integer>>();

    edges = new HashMap<String, Edge>();

    // For each lines of subway get the nodes and add them in the nodes Hash Map
    for (String stops : stopsList) {
      List lines = Files.readAllLines(Paths.get(stops));

      fillNodes(lines);
    }

    // For each lines of subway fill neighbors for each nodes
    for (String stopTimes : stopTimesList) {
      List lines = Files.readAllLines(Paths.get(stopTimes));

      fillNeighbours(lines);
    }

    // At the end, we don't want multiple station with the same name
    // So we merge neighbors in one station and delete others
    mergeNeighbors();

    fillEdges();
  }

  /**
   * FILLNODES
   * @param lines : a list of subway station
   * @purpose : Extract each station and convert it into a node then put it in the Hash Map
   */
  public void fillNodes(List lines) {

    for (int i = 1; i < lines.size(); i++) {
      String line = (String) lines.get(i);

      // Create the node from the splitted string
      Node node = new Node(line.split(","));
      int id = node.id;

      // In case the node is already in the Hash Map : exit
      if (nodes.containsKey(id))
        continue;

      // Store the node
      nodes.put(id, node);

      // Get ids of nodes with the same name
      ArrayList ids = namesIds.get(node.name);

      // If a node exists with the same name
      // Add the id of the current node to the id array
      if (ids != null) {
        ids.add(node.id);
      }

      // Else register the node in the namesIds Hash Map
      else {
        ids = new ArrayList<Integer>();
        ids.add(node.id);
        namesIds.put(node.name, ids);
      }
    }

  }

  /**
   * FILLNEIGHBOURS
   * @param lines : a list of stop time
   * @purpose : Follow subway lines and register neighbors of each station
   */
  public void fillNeighbours(List lines) {

    for (int i = 1; i < lines.size() - 1; i++) {

      String[] line = (String[]) ((String)lines.get(i)).split(",");

      // Id of the current node
      int nodeId = Integer.parseInt(line[3]);

      // The number of of this station in the sequence
      int nb = Integer.parseInt(line[4]);

      // Hour of departure
      int hour = Integer.parseInt(line[1].split(":")[0]);

      // Filter illogical values
      if (hour < 10 || hour > 17)
        continue;

      String[] nextLine = (String[]) ((String)lines.get(i + 1)).split(",");
      int nextNodeId = Integer.parseInt(nextLine[3]);
      int nextNb = Integer.parseInt(nextLine[4]);

      // If the nest station is not neighbor to the current station exit
      if (!(nb + 1 == nextNb && nodes.containsKey(nodeId) && nodes.containsKey(nextNodeId)))
        continue;

      // get the current node from it's id
      Node node = nodes.get(nodeId);
      // In case there are multiple station with this name
      // Take the first in the list
      node = getOneNodeByName(node.name, 0);

      // get the next node from it's id
      Node nextNode = nodes.get(nextNodeId);
      // In case there are multiple station with this name
      // Take the first in the list
      nextNode = getOneNodeByName(nextNode.name, 0);

      // Register the neighbor if the neighbors is not registered yet
      if (!node.neighbors.contains(nextNode.id))
        node.neighbors.add(nextNode.id);
    }
  }

  /**
   * MERGENEIGHBORS
   * @purpose : Retreive all neighbors of nodes with same name and merge the into one single node
   *            And remove useless nodes after that;
   */
  public void mergeNeighbors() {

    // Loop through the namesIds map
    Iterator it = namesIds.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();

        // Find all the nodes with the given name
        ArrayList<Node> _nodes = getNodesByName((String) pair.getKey());

        // If the node has no simblings just exit
        if (_nodes.size() <= 1)
          continue;

        // Get the first Node, the one we will keep
        Node node = _nodes.get(0);

        // For each simbling merge neighbors with the node we keep
        for (int i = 1; i < _nodes.size(); i++) {

          Node neighbor = this.nodes.get(_nodes.get(i).id);

          node.neighbors.addAll(neighbor.neighbors);

          // Remove the useless Node
          nodes.remove(neighbor.id);
        }

        // Free space
        while (((ArrayList) pair.getValue()).size() > 1)
          ((ArrayList) pair.getValue()).remove(1);
    }

  }

  /**
   * FILLEDGES
   * @purpose : Create edges for the graph
   */
  public void fillEdges() {

    // Loop through the nodes map
    Iterator it = nodes.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();

        Node node = (Node) pair.getValue();

        // Loop through it's neighbors
        for (int n : node.neighbors) {
          Node neighbor = nodes.get(n);

          if (neighbor == null)
            continue;

          // Create the edge
          Edge edge = new Edge(node, neighbor);

          // Id the edge doesn't exist, save it'
          if (!edges.containsKey(edge.index))
            edges.put(edge.index, edge);
        }
    }

  }

  /*******************
   * GRAPH FUNCTIONS *
   *******************/

  // BFS

  /**
   * GRAPHDIAMETERBFS
   * @purpose : Find the diameter of a graph and the longest path
   */
  public void graphDiameterBfs() {

    // Make sure to reset nodes before anything else
    this.resetNodes();

    // Init the longestPath to empty array
    longestPath = new ArrayList<Integer>();

    // Init diameter to 0
    diameter = 0;

    Iterator it = nodes.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();

        Node start = (Node) pair.getValue();

        Iterator it2 = nodes.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry pair2 = (Map.Entry)it2.next();

            // Process the bfs algorithm from start to end
            Node end = bfs(start.name, ((Node) pair2.getValue()).name);

            // In the caae there is no path
            if (end == null)
              continue;

            // Retrieve the path
            ArrayList<Integer> path = getPath(end);

            int length = path.size();

            // If the path is longer, update the diameter and the longest path
            if (length > diameter) {
              diameter = length;
              longestPath = path;
            }

            // Reset nodes for the next bfs
            resetNodes();
        }
    }

    System.out.println("Diameter : " + diameter);

    for (int n : longestPath)
      System.out.println(nodes.get(n).name);
  }

  /**
   * BFS
   * @param start : the name of the starting node
   * @param end : the name of the ending node
   * @return : the ending node if found
   */
  public Node bfs(String start, String end) {

    // Get the starting node
    Node startNode = getNodesByName(start).get(0);

    if (startNode == null)
      return null;

    // Init the queue
    Queue queue = new LinkedList();

    // Add the first node and mark it
    queue.add(startNode);
    startNode.visited = true;

    Node node = null;

    // Iterate while the is a node in the queue
    while(!queue.isEmpty()) {

      // Pick the last node in the queue
      node = (Node) queue.remove();

      // Iterate through neighbord of the picked node
      for(int n : node.neighbors) {

        // Retreive the neighbor node
        Node neighbor = nodes.get(n);

        // If we haven't visited it yet
        if (!neighbor.visited) {

          // Enqueue the neighbor
          queue.add(neighbor);

          // Mark it and set it's previous node'
          neighbor.visited = true;
          neighbor.previous = node.id;
        }

        // If the neighbor is the one we are looking for return it
        if (neighbor.name.equals(end))
          return neighbor;
      }
    }

    // If the end has not been reached
    return null;
  }

  // DIJKSTRA

  /**
   * GRAPHDIAMETERDIJKSTRA
   * @purpose : Find the longest path and the maxDistance of a graph using dijkstra algorithm
   */
  public void graphDiameterDijkstra() {

    // make sure to reset the nodes before doing anything
    this.resetNodes();

    // Set the maxDistance to zero
    maxDistance = 0;

    // Init the longest path to an empty array
    longestPath = new ArrayList<Integer>();

    Iterator it = nodes.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();

        Node start = (Node) pair.getValue();

        Iterator it2 = nodes.entrySet().iterator();
        while (it2.hasNext()) {
          Map.Entry pair2 = (Map.Entry)it2.next();

          // Process the dijkstra algorithm from start to end
          Node end = dijkstra(start.name, ((Node) pair2.getValue()).name);

          // If there is no path from start to end, exit
          if (end == null) {
            resetNodes();
            continue;
          }

          // Retreive the path from start to end
          ArrayList<Integer> path = getPath(end);

          // If the distance is more than the previous one, update it and change the longest path
          if (end.distance < Double.MAX_VALUE && end.distance > maxDistance) {
            maxDistance = end.distance;
            longestPath = path;
          }

          // Reset the nodes for the next Dijkstra
          resetNodes();
        }
    }

    System.out.println("Max Distance : " + maxDistance + " M");

    // Print the longest path
    int previousIndex = -1;

    for (int n : longestPath) {
      Node node = nodes.get(n);
      Node previous = nodes.get(previousIndex);
      if (previous != null)
        System.out.print(node.name + " (" + node.getDistance(previous) + ") \n");
      else
        System.out.print(node.name + " \n");
      previousIndex = node.id;
    }
  }

  /**
   * COMPUTEBTW
   * @purpose : Calculate betweeness of all egdes
   */
  public void computeBtw() {

    // make sure to reset the nodes before doing anything
    this.resetNodes();

    // Set the maxDistance to zero
    maxDistance = 0;

    // Init the longest path to an empty array
    longestPath = new ArrayList<Integer>();

    Iterator it = nodes.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();

        Node start = (Node) pair.getValue();

        Iterator it2 = nodes.entrySet().iterator();
        while (it2.hasNext()) {
          Map.Entry pair2 = (Map.Entry)it2.next();

          // Process the dijkstra algorithm from start to end
          Node end = dijkstra(start.name, ((Node) pair2.getValue()).name);

          // If there is no path from start to end, exit
          if (end == null) {
            resetNodes();
            continue;
          }

          // Retreive the path from start to end
          ArrayList<Integer> path = getPath(end);

          // Loop through the path to update betweeness of the edges
          for (int id : path) {

            // retreive the node
            Node node = nodes.get(id);

            // If the node has no previous exit
            if (node.previous < 0)
              continue;

            // Create the edge
            Edge edge = new Edge(node, nodes.get(node.previous));

            // If the edge already exist, increment it's betweeness'
            if (edges.containsKey(edge.index))
              edges.get(edge.index).betweenness ++;

            // Else puth the newly created edge in the edges map
            else
              edges.put(edge.index, edge);
          }

          // reset nodes to find the next Dijkstra
          resetNodes();
        }
    }

  }

  /**
   * DIJKSTRA
   * @param start : the name of the starting node
   * @param end : the name of the ending node
   * @return : The ending node
   */
  public Node dijkstra(String start, String end) {

    // Retreive the starting node
    Node nodeStart = getOneNodeByName(start, 0);

    // Set it's distance to zero'
    nodeStart.distance = 0;

    // Init the queue
    LinkedList<Node> queue = new LinkedList<Node>();

    // Fill the queue with all the nodes
    fillQueue(queue);

    // While the queue is no empty ( while all the nodes aren't visited )
    while (!queue.isEmpty()) {

      // Retreive the nearest node ( with the lowest distance )
      Node nearest = getNearestNode(queue);

      // If there is no nearest node -> leave the while loop
      if (nearest == null)
        break;

      // Mark the nearest node as visited
      nearest.visited = true;

      // Loop through neighbors of the nearest nodes to update their distances
      for (int n : nearest.neighbors)
        updateDistance(nearest, nodes.get(n));

      // if the neares is the ending one retur it
      if (nearest.name.equals(end))
        return nearest;
    }

    return null;
  }

  /**
   * FILLQUEUE
   * @param q : the queue to fill
   * @purpose : fill the queue with all the nodes of the hashmap
   */
  public void fillQueue(LinkedList<Node> q) {

    Iterator it = nodes.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();

        Node node = (Node) pair.getValue();
        q.add(node);
    }

  }

  /**
   * GETNEARESTNODE
   * @param q : the queue to search for the node with the lower distance
   * @return : the node with the lower distance
   */
  public Node getNearestNode(LinkedList<Node> q) {
    double minDistance = Double.MAX_VALUE;

    Node resNode = null;

    for (Node currentNode : q) {
        if (currentNode.visited)
          continue;

        if (currentNode.distance < minDistance) {
          minDistance = currentNode.distance;
          resNode = currentNode;
        }
    }

    // Remove the nearest node from the queue
    q.remove(resNode);
    return resNode;
  }

  /**
   * UPDATEDISTANCE
   * @param n1 : the parent node
   * @param n2 : the neighbor node
   */
  public void updateDistance(Node n1, Node n2) {

    // If the path through n1 is smaller update it and change the previous of n2 to n1
    if (n2.distance > n1.distance + n1.getDistance(n2)) {
      n2.distance =  n1.distance + n1.getDistance(n2);
      n2.previous = n1.id;
    }
  }

  /**
   * GETPATH
   * @param end : the ending node
   * @return : return the ordered list of nodes id of the path,
   *            begining with the end and ending with the starting node
   */
  public ArrayList<Integer> getPath(Node end) {
    ArrayList<Integer> path = new ArrayList<Integer>();

    Node node = end;

    path.add(node.id);

    while (node.previous > 0) {
      node = nodes.get(node.previous);
      path.add(node.id);
    }

    return path;
  }

  /**
   * GETMAXBTW
   * @return : Retuen an array of all edges that have the maximum betweeness
   */
  public ArrayList<Edge> getMaxsBtw() {

    int maxBtw = 0;
    ArrayList<Edge> maxBtwEdges = new ArrayList<Edge>();

    // Calculate the max betweeness
    Iterator it = edges.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry)it.next();

      Edge edge = (Edge) pair.getValue();

      if (edge.betweenness >= maxBtw)
        maxBtw = edge.betweenness;
    }

    // Fill the array with all edges with that max betweeness
    it = edges.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry)it.next();

      Edge edge = (Edge) pair.getValue();

      if (edge.betweenness == maxBtw)
        maxBtwEdges.add(edge);
    }

    return maxBtwEdges;
  }

  /*********************
   * UTILITY FUNCTIONS *
   *********************/

  /**
   * GETNODESBYNAME
   * @param name: Node name
   * @return : The list of all Nodes that share the given name
   */
  public ArrayList<Node> getNodesByName(String name) {

    name = Normalizer.normalize(name, Normalizer.Form.NFD);
    name = name.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

    ArrayList<Integer> ids = namesIds.get(name);

    if (ids == null)
      return null;

    ArrayList res = new ArrayList<Node>();
    for (int id : ids) {
      res.add(nodes.get(id));
    }

    return res;
  }

  /**
   * GETONENODEBYNAME
   * @param name: Node name
   * @return : The first Node that has this name
   */
  public Node getOneNodeByName(String name, int index) {

    name = Normalizer.normalize(name, Normalizer.Form.NFD);
    name = name.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

    ArrayList<Integer> ids = namesIds.get(name);

    if (ids == null)
      return null;

    ArrayList<Node> res = new ArrayList<Node>();
    for (int id : ids) {
      res.add(nodes.get(id));
    }

    return res.get(index);
  }


  /**
   * GETNEIGHBORSNAMES
   * @param node : a node
   * @return : The list of all neighbors name of the given node
   */
  public ArrayList<String> getNeighborsNames(Node node) {
    ArrayList<Integer> ids = node.neighbors;

    ArrayList<String> res = new ArrayList<String>();
    for (int id: ids) {
      res.add(nodes.get(id).name);
    }
    return res;
  }

  /**
   * PRINTNODES
   * @purpose : Print all nodes in the console
   */
  public void printNodes() {
    Iterator it = nodes.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();
        System.out.println(pair.getKey() + " = " + ((Node) pair.getValue()).name);
    }
  }

  /**
   * RESETNODES
   * @purpose : Set the boolean 'visited' of all nodes to false, and set all previous to "-1"
   */
  public void resetNodes() {
    Iterator it = nodes.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();
        ((Node) pair.getValue()).visited = false;
        ((Node) pair.getValue()).distance = Double.MAX_VALUE;
        ((Node) pair.getValue()).previous = -1;
    }
  }

  /**
   * FINDBYID
   * @param id : The id of the node we want
   * @return : a Node that match the given Id
   */
  public Node findById(int id) {
    return nodes.get(id);
  }

  /**
   * COUNTVISITED
   * @return : The number of nodes with the flag visited set to 'false'
   */
  public int countVisited() {
    Iterator it = nodes.entrySet().iterator();
    int nb = 0;
    while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();
        if (((Node) pair.getValue()).visited)
          nb++;
    }
    return nb;
  }

  /**
   * SAVENODES
   * @param filename : the name of the file in where to save the nodes
   * @purpose : Save nodes data in a CSV file
   */
  public void saveNodes(String filename) throws IOException {

        String csvFile = "../src/states/" + filename + ".csv";
        FileWriter writer = new FileWriter(csvFile);

        Iterator it = nodes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Node node = (Node) pair.getValue();
            writer.append(node.getCsvData() + "\n");
        }
        writer.close();
  }

  /**
   * SAVEEDGES
   * @param filename : the name of the file in where to save the edges
   * @purpose : Save edges data in a CSV file
   */
  public void saveEdges(String filename) throws IOException {

        String csvFile = "../src/states/" + filename + ".csv";
        FileWriter writer = new FileWriter(csvFile);

        Iterator it = edges.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Edge edge = (Edge) pair.getValue();
            writer.append(edge.getCsvData() + "\n");
        }
        writer.close();
  }

  /**
   * CONSTRUCTGRAPHFROMCSV
   * @param filename : the name of the file where nodes data are saved
   * @purpose : Construct the graph from data in CSV format
   */
  public void constructGraphFromCsv(String fileName) throws IOException {
      List<String> lines = Files.readAllLines(Paths.get(fileName));

      for (String line : lines) {
        Node node = new Node();
        node.constructFromCsvData(line.split(";"));
        nodes.put(node.id, node);// Get ids of nodes with the same name

        ArrayList ids = namesIds.get(node.name);

        // If a node exists with the same name
        // Add the id of the current node to the id array
        if (ids != null) {
          ids.add(node.id);
        }

        // Else register the node in the namesIds Hash Map
        else {
          ids = new ArrayList<Integer>();
          ids.add(node.id);
          namesIds.put(node.name, ids);
        }
      }
  }

  /**
   * CONSTRUCTGRAPHFROMCSV
   * @param filename : the name of the file where nodes data are saved
   * @param filename2 : the name of the file where edges data are saved
   * @purpose : Construct the graph from data in CSV format
   */
  public void constructGraphFromCsv(String fileNameNodes, String fileNameEdges) throws IOException {
      constructGraphFromCsv(fileNameNodes);

      List<String> lines = Files.readAllLines(Paths.get(fileNameEdges));

      for (String line : lines) {
        Edge edge = new Edge();
        edge.constructFromCsvData(line.split(";"), nodes);
        edges.put(edge.index, edge);
      }
  }

  /**
   * DISPLAY
   * @param p : reference to the processing object
   * @purpose : display nodes and Edges
   */
  public void display(PApplet p) {

      float minLng = Float.MAX_VALUE;
      float minLat = Float.MAX_VALUE;

      ArrayList<float[]> data = new ArrayList<float[]>();

      // Convert coordinates and find minimums
      Iterator it = nodes.entrySet().iterator();
      while (it.hasNext()) {
          Map.Entry pair = (Map.Entry)it.next();
          Node node = (Node) pair.getValue();

          float[] coords = new float[2];

          coords[0] = (float) Math.sin(node.lat * p.PI / 180);
          coords[1] = (float) Math.sin(node.lng * p.PI / 180);

          if (coords[0] < minLat)
            minLat = coords[0];

          if (coords[1] < minLng)
            minLng = coords[1];

          data.add(coords);
      }

      // Multiply to enlarge datas on the graph
      for( float[] coords : data) {
        coords[0] = (coords[0] - minLat) * 6366 * 60 + 100;
        coords[1] = (coords[1] - minLng) * 6366 * 70;
      }

      p.strokeWeight(5);

      for (float[] coords : data)
          p.point(coords[0], 900 - coords[1]); // Draw the nodes

      p.strokeWeight(3);

      it = edges.entrySet().iterator();
      while (it.hasNext()) {
          Map.Entry pair = (Map.Entry)it.next();
          Edge edge = (Edge) pair.getValue();

          // Set color to the color of the nodes
          if (edge.node1.color != null)
            p.stroke(edge.node1.color[0], edge.node1.color[1], edge.node1.color[2]);
          else
            p.stroke(0);

          // Don't display if the edge is deleted'
          if (edge.del)
            continue;

          double lat1 = (Math.sin(edge.node1.lat * p.PI / 180) - minLat) * 6366 * 60 + 100;
          double lng1 = (Math.sin(edge.node1.lng * p.PI / 180) - minLng) * 6366 * 70;
          double lat2 = (Math.sin(edge.node2.lat * p.PI / 180) - minLat) * 6366 * 60 + 100;
          double lng2 = (Math.sin(edge.node2.lng * p.PI / 180) - minLng) * 6366 * 70;
          p.line((float) lat1,  900 - ((float) lng1), (float) lat2,  900 - ((float) lng2));
      }
  }
}