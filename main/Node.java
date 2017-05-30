package main;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Node
{
  public int id;
  public String name;
  public ArrayList<Integer> neighbors;

  public boolean visited = false;

  public int previous = -1;
  public double lng;
  public double lat;

  public double distance = Double.MAX_VALUE;

  public int[] color;
  public boolean colorVisited = false;

  public Node() {}

  public Node(String[] params) {
    id = Integer.parseInt(params[0]);
    name = Normalizer.normalize(params[2].replace("\"", ""), Normalizer.Form.NFD);
    name = name.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    neighbors = new ArrayList<Integer>();

    lng = Double.parseDouble(params[4]);
    lat = Double.parseDouble(params[5]);
  }

  public double getDistance(Node node) {
    double dist = 6366086 *  Math.sqrt(Math.pow(Math.sin(deg2rad(lat) - deg2rad(node.lat)), 2) + Math.pow(Math.sin(deg2rad(lng) - deg2rad(node.lng)), 2));
    return dist;
  }

  /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
  /*::	This function converts decimal degrees to radians						 :*/
  /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
  private static double deg2rad(double deg) {
    return (deg * Math.PI / 180.0);
  }

  /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
  /*::	This function converts radians to decimal degrees						 :*/
  /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
  private static double rad2deg(double rad) {
    return (rad * 180 / Math.PI);
  }

  @Override
  public String toString() {
    return  "NODE : " +
            this.id + ", " +
            this.name + ", " +
            this.previous + ", " +
            // this.lng + ", " +
            // this.lat + ", " +
            this.distance  + ", " +
            this.neighbors.toString() + ", " +
            this.lng + ", " +
            this.lat;
  }

  public String getCsvData() {
    return this.id + ";" + this.name + ";" + this.neighbors.toString() + ";" + this.lng + ";" + this.lat;
  }

  public void constructFromCsvData(String[] data) {
    id = Integer.parseInt(data[0]);
    name = data[1];
    List<String> l = Arrays.asList(data[2].substring(1, data[2].length() - 1).split(", "));
    neighbors = new ArrayList<Integer>();
    for (String s : l) {
      if (s.length() > 0)
        neighbors.add(Integer.parseInt(s));
    }
    lng = Double.parseDouble(data[3]);
    lat = Double.parseDouble(data[4]);
  }
}