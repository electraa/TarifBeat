import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.lang.*;
import java.io.PrintWriter;
import java.io.FileWriter;


public class Didaktor {

    protected static class Node {
        private ArrayList<Edge> edges;
        private String name;
        private double x, y;
        private ArrayList<String> taxis;
        private Boolean hasClient;
        public double gScore;
        public double hScore;
        public double fScore;
        private Node parentNode;
        
        public Node(String x, String y) {
            this.x = Double.parseDouble(x);
            this.y = Double.parseDouble(y);
            this.name = x + ";" + y;
            this.edges = new ArrayList<Edge>();
            this.taxis = new ArrayList<String>();
            this.hasClient = false;
            this.fScore = 0;

        }
        public String getName() {
            return this.name;
        }
        public void addEdge(Edge e) {
            this.edges.add(e);
        }
        public ArrayList<Edge> getEdges() {
            return this.edges;
        }
        public double getX() {
            return this.x;
        }
        public double getY() {
            return this.y;
        }

        public double getDistance(double x, double y) {
            return Math.sqrt((this.x - x)*(this.x - x) + (this.y - y)*(this.y - y));
        }

        public void addTaxi(String id) {
            this.taxis.add(id);
        }
        public void addClient() {
            this.hasClient = true;
        }

        public void setGScore(double gScore) {
            this.gScore = gScore;
        }

        public void setHScore(double hScore) {
            this.hScore = hScore;
        }

        public void setFScore(double fScore) {
            this.fScore = fScore;
        }
        public void setParentNode(Node node){
            this.parentNode = node;
        }
        public Node getParentNode(){
            return this.parentNode;
        }

        public void printEdges() {
            Iterator<Edge> iterator = this.edges.iterator();
            while (iterator.hasNext()) {
                System.out.print(iterator.next().to.getName() + ",");
            }
            System.out.println("");
        }

    }
    protected static class Edge {
        private Node from, to;
        private double weight;
        private String id;

        public Edge(Node from, Node to, String id) {
            this.from = from;
            this.to = to;
            this.weight = Math.sqrt((from.getX() - to.getX())*(from.getX() - to.getX()) + (from.getY() - to.getY())*(from.getY() - to.getY()));
            this.id = id;
        }
        public double getHscore(Node start) {
            return Math.sqrt((start.getX() - to.getX())*(start.getX() - to.getX()) + (start.getY() - to.getY())*(start.getY() - to.getY()));
        }
        public Node getfrom() {
            return this.from;
        }
        public Node getto() {
            return this.to;
        }
        
    }

    protected static class KML {
        List<PlacemarkKML> placemarks;
        String xml;
        
        public KML() {
            this.placemarks = new ArrayList<PlacemarkKML>();
            this.xml = "";
        }

        public void addPlacemark(PlacemarkKML placemark) {
            this.placemarks.add(placemark);
        }

        public void build() {
            xml += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
            xml += "<kml xmlns=\"http://earth.google.com/kml/2.1\">";
            xml += "<Document>";

            xml += "<name>Taxi Routes</name>";
            xml += "<Style id=\"green\"><LineStyle><color>ff009900</color><width>4</width></LineStyle></Style>";
            xml += "<Style id=\"red\"><LineStyle><color>ff0000ff</color><width>4</width></LineStyle></Style>";
            
            for(PlacemarkKML p : this.placemarks) {
                xml += "<Placemark>";
                xml += "<name>" + p.getTaxiName() + "</name>";
                if(p.isGreen)
                    xml += "<styleUrl>#green</styleUrl>";
                else
                    xml += "<styleUrl>#red</styleUrl>";
                xml += "<LineString>";
                xml += "<altitudeMode>relative</altitudeMode>";
                xml += "<coordinates>";
                xml += p.extractPath();
                xml += "</coordinates>";
                xml += "</LineString>";
                xml += "</Placemark>";
            }

            xml += "</Document>";
            xml += "</kml>";
        }

        public void save(String filename) {
            try{
                PrintWriter writer = new PrintWriter(filename+".kml", "UTF-8");
                writer.println(this.xml);
                writer.close();
            } catch (IOException e) {
               // do something
            }
        }
    }
    protected static class PlacemarkKML {
        List<Node> path;
        boolean pathReversed;
        double distance;
        boolean isGreen;
        String taxiName;

        public PlacemarkKML() {
            this.path = new ArrayList<Node>();
            this.distance = 0;
            this.pathReversed = false;
            this.isGreen = false;
            this.taxiName = null;
        }

        public void addNode(Node node, double distance) {
            this.path.add(node);
            this.distance += distance;
        }
        
        public double getDistance() {
            return this.distance;
        }

        public String extractPath() {
            if(!pathReversed) {
                Collections.reverse(this.path);
                pathReversed = true;
            }
            String output = "";
            for(Node n : this.path) {
                output += Double.toString(n.getX()) + "," + Double.toString(n.getY()) + ",0\n";
            }
            return output;
        }

        public void setGreen() {
            this.isGreen = true;
        }

        public boolean isGreen() {
            return this.isGreen;
        }

        public void setTaxiName(String name) {
            this.taxiName = name;
        }

        public String getTaxiName() {
            return this.taxiName;
        }
    }
    public static void AstarSearch(Node source, Node goal){

        Set<Node> explored = new HashSet<Node>();
        PriorityQueue<Node> queue = new PriorityQueue<Node>(20, 
            new Comparator<Node>()
            {
                //override compare method
                public int compare(Node i, Node j){
                    if(i.fScore > j.fScore)
                    {
                        return 1;
                    }
                    else if (i.fScore < j.fScore)
                    {
                        return -1;
                    }
                    else
                    {
                        return 0;
                    }
                }
            }
        );

                //cost from start
                source.setGScore(0);
                queue.add(source);

                boolean found = false;

                while((!queue.isEmpty())&&(!found)){

                        //the node in having the lowest f_score value
                        Node current = queue.poll();
                        explored.add(current);

                        //goal found
                        if(current.getName().equals(goal.getName())){
                                found = true;
                        }

                        //check every child of current node
                        for(Edge e : current.getEdges()){
                                Node child = e.to;
                                double cost = e.weight;
                                double temp_g_scores = current.gScore + cost;
                                double temp_f_scores = temp_g_scores + e.getHscore(source);
                                //h_scores;


                                /*if child node has been evaluated and
                                the newer f_score is higher, skip*/

                                if(((explored.contains(child)) && ((temp_f_scores >= child.fScore) ))){
                                    continue;
                                }
                                /*else if child node is not in queue or
                                newer f_score is lower*/

                                else if((!queue.contains(child)) || (temp_f_scores < child.fScore)){
                                    child.setParentNode(current);
                                    child.setGScore(temp_g_scores);
                                    child.setFScore(temp_f_scores);

                                    if(queue.contains(child)){
                                        queue.remove(child);
                                    }
                                    queue.add(child);
                                }

                        }

                }

        }


    public static void main(String[] args) {
        ArrayList<String> world = new ArrayList<>();
        // String world = "average_speed(0.9).\ntraffic_weight(R, T, 1) :- not(traffic(R, T, _)).\ntraffic_weight(R, T, 0.9) :- traffic(R, T, medium).\ntraffic_weight(R, T, 0.7) :- traffic(R, T, high).\nlighting_weight(good, 1).\nlighting_weight(medium, 0.9).\nlighting_weight(low, 0.8).\nlighting(_, morning, good).\nlighting(_, noon, good).\nlighting(R, afternoon, medium) :- lit(R).\nlighting(R, afternoon, low) :- not(lit(R)).\nforCars(R) :- not(service(R)), not(pedestrian(R)), not(track(R)), not(bus_guideway(R)), not(escape(R)), not(raceway(R)), not(footway(R)), not(bridleway(R)), not(steps(R)), not(path(R)), not(cycleway(R)), not(waterway(R)), not(railway(R)).\naccessible(R) :- access(R), forCars(R).\npriority(R, T, Z) :- accessible(R), maxspeed(R, MS), average_speed(A), traffic_weight(R, T, TW), lighting(R, T, L), lighting_weight(L, LW), Z is MS * A * TW * LW.\npriority(R, _, 0) :- not(accessible(R)).\nmotorway(0).\ntrunk(0).\nprimary(0).\nsecondary(0).\ntertiary(0).\nunclassified(0).\nresidential(0).\nliving_street(0).\nroad(0).\nmotorway_link(0).\ntrunk_link(0).\nprimary_link(0).\nsecondary_link(0).\ntertiary_link(0).\nservice(0).\npedestrian(0).\ntrack(0).\nbus_guideway(0).\nescape(0).\nraceway(0).\nfootway(0).\nbridleway(0).\nsteps(0).\npath(0).\ncycleway(0).\nwaterway(0).\nrailway(0).\n";
        String line = "";
        String cvsSplitBy = ",";
        String csvFile2 = "lines.csv";
        HashMap<String,String> oneways = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile2))) {
            br.readLine();
            while ((line = br.readLine()) != null) {
                // use comma as separator
                // id,highway,name,oneway,lit,lanes,maxspeed,railway,boundary,access,natural,barrier,tunnel,bridge,incline,waterway,busway,toll
                String[] row = line.split(cvsSplitBy, 20);
                String lineId = row[0];
                String highway = row[1];
                String oneway = row[3];
                String lit = row[4];
                String lanes = row[5];
                String maxspeed = row[6];
                String railway = row[7];
                String access = row[9];
                String waterway = row[15];
                String toll = row[17];
                
                HashMap<String, String> speedLimits = new HashMap<>();
                speedLimits.put("motorway", "130");
                speedLimits.put("trunk", "110");
                speedLimits.put("primary", "90");
                speedLimits.put("secondary", "70");
                speedLimits.put("tertiary", "70");
                speedLimits.put("unclassified", "50");
                speedLimits.put("residential", "50");
                speedLimits.put("living_street", "20");
                speedLimits.put("road", "50");
                speedLimits.put("motorway_link", "80");
                speedLimits.put("trunk_link", "50");
                speedLimits.put("primary_link", "50");
                speedLimits.put("secondary_link", "50");
                speedLimits.put("tertiary_link", "50");

                if(highway.equals("") || highway.length() == 0) {
                    highway = "empty";
                }
                maxspeed = (maxspeed.length() > 0) ? maxspeed : ((speedLimits.containsKey(highway)) ? speedLimits.get(highway) : "50");
                world.add(highway + "(e" + lineId + ").\n");
                world.add("maxspeed(e" + lineId + "," + maxspeed + ").\n");
                
                if(oneway.equals("yes")) {
                    world.add("oneway(e" + lineId + ").\n");
                    oneways.put(lineId, "1");
                }else if(oneway.equals("-1")) {
                    world.add("oneway(e" + lineId + ").\n");
                    oneways.put(lineId, "-1");
                }
                if(lit.equals("yes"))
                    world.add("lit(e" + lineId + ").\n");
                if(!lanes.equals("") && lanes.length() > 0)
                    world.add("lanes(e" + lineId + "," + lanes + ").\n");
                if(!railway.equals("") && railway.length() > 0)
                    world.add("railway(e" + lineId + ").\n");
                if(access.equals("yes") || access.length() == 0)
                    world.add("access(e" + lineId + ").\n");
                if(!waterway.equals("") && waterway.length() > 0)
                    world.add("waterway(e" + lineId + ").\n");
                if(toll.equals("yes"))
                    world.add("toll(e" + lineId + ").\n");
            }


        } catch (IOException e) {
            e.printStackTrace();
        }


        String csvFile = "nodes.csv";
        
        HashMap<String, Node> nodesMap = new HashMap<>();
        HashMap<String, String> taxiNodes = new HashMap<>();
        String clientNode = null;
// 
        Edge currEdge = null;
        String prevEdgeId = null;
        String prevNodeName = null;
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            br.readLine();
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] row = line.split(cvsSplitBy);
                String nodeX = row[0];
                String nodeY = row[1];
                String nodeName = row[3];
                String edgeId = row[2];
                if (!nodesMap.containsKey(nodeName)) {
                    Node n = new Node(nodeX, nodeY);
                    nodesMap.put(nodeName, n);
                    world.add("node(" + nodeX + "," + nodeY + ",n" + nodeName + ").\n");
                    world.add("belongsTo(n" + nodeName + ",e" + edgeId + ").\n");
                }
                if(prevEdgeId != null && prevEdgeId.equals(edgeId)) {
                    Node prevNode = nodesMap.get(prevNodeName);
                    Node currNode = nodesMap.get(nodeName);
                    // 
                    Double distance = Math.sqrt((currNode.getX() - prevNode.getX())*((currNode.getX()) - (prevNode.getX())) + ((currNode.getY()) - (prevNode.getY()))*((currNode.getY()) - (prevNode.getY())));
if(oneways.containsKey(edgeId)){
    if (oneways.get(edgeId).equals("1"))
        world.add("canMoveFromTo(n" + prevNodeName + ",n" + nodeName + ",e" + edgeId + "," + String.format(Locale.US,"%.10f", distance) + ").\n");
    else 
        world.add("canMoveFromTo(n" + nodeName + ",n" + prevNodeName + ",e" + edgeId + "," + String.format(Locale.US,"%.10f", distance) + ").\n");
}else{
    world.add("canMoveFromTo(n" + prevNodeName + ",n" + nodeName + ",e" + edgeId + "," + String.format(Locale.US,"%.10f", distance) + ").\n");
    world.add("canMoveFromTo(n" + nodeName + ",n" + prevNodeName + ",e" + edgeId + "," + String.format(Locale.US,"%.10f", distance) + ").\n");

}
                    //world.add("canMoveFromTo(n" + prevNodeName + ",n" + nodeName + ",e" + edgeId + "," + String.format("%.10f", distance) + ").\n");
                    //if(oneways.containsKey(edgeId))
                    //     world.add("canMoveFromTo(n" + nodeName + ",n" + prevNodeName + ",e" + edgeId + "," + String.format("%.10f", distance) + ").\n");
                    Edge from = new Edge(nodesMap.get(prevNodeName), nodesMap.get(nodeName), edgeId);
                    Edge to = new Edge(nodesMap.get(nodeName), nodesMap.get(prevNodeName), edgeId);
                    nodesMap.get(prevNodeName).addEdge(from);
                    nodesMap.get(nodeName).addEdge(to);
                    world.add("next(n" + prevNodeName + ",n" + nodeName + ").\n");
                }
                prevEdgeId = edgeId;
                prevNodeName =  nodeName;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        
        String csvFile3 = "traffic.csv";
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile3))) {
            br.readLine();
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] row = line.split(cvsSplitBy, 3);
                String lineId = row[0];
                String trafficInfo = "";
                try {
                    trafficInfo = row[2];
                }
                catch(Exception e) {
                    ;
                }
                if(trafficInfo != "" && trafficInfo.length() > 0) {
                    String[] slots = trafficInfo.split("\\|");
                    for(String currSlot:slots) {
                        String[] slot = currSlot.split("=");
                        String timeName = "";
                        if(slot[0].equals("09:00-11:00"))
                            timeName = "morning";
                        else if(slot[0].equals("13:00-15:00"))
                            timeName = "noon";
                        else if(slot[0].equals("17:00-19:00"))
                            timeName = "afternoon";
                        world.add("traffic(e" + lineId + "," + timeName + "," + slot[1] + ").\n");
                    }
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.sort(world);
        try(FileWriter writer = new FileWriter("kosmos.pl")) {
            for(String str:world) {
                writer.write(str);
            }
            writer.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

}
