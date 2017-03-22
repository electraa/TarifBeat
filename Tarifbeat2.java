import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.lang.*;
import java.io.PrintWriter;

import com.ugos.jiprolog.engine.JIPEngine;
import com.ugos.jiprolog.engine.JIPQuery;
import com.ugos.jiprolog.engine.JIPSyntaxErrorException;
import com.ugos.jiprolog.engine.JIPTerm;
import com.ugos.jiprolog.engine.JIPTermParser;

public class Tarifbeat2 {

    protected static class Node {
        private ArrayList<Edge> edges;
        private String name;
        private double x, y;
        private ArrayList<String> taxis;
        public Boolean hasClient;
        public double gScore;
        public double hScore;
        public double fScore;
        public String callTime;

        private Node parentNode;
        
        public Node(String x, String y, String name) {
            this.x = Double.parseDouble(x);
            this.y = Double.parseDouble(y);
            this.name = name;
            this.edges = new ArrayList<Edge>();
            this.taxis = new ArrayList<String>();
            this.hasClient = false;
            this.fScore = 0;
            this.callTime="";

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

        public void addCallTime(String timeName) {
            this.callTime = timeName;
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
            return Math.sqrt((start.getX() - to.getX())*(start.getX() - to.getX()) + (start.getY() - to.getY())*(start.getY() - to.getY())) / 45.0;
        }
        public Node getfrom() {
            return this.from;
        }
        public Node getto() {
            return this.to;
        }
        
    }
    protected static class Taxi {
        Boolean available, long_distance;
        int minCapacity, maxCapacity;
        double rating;
        String type, id;
        Node location;
        HashMap<String,Boolean> languages;

        public Taxi(Node location, String available, String capacity, Double rating, String long_distance, String type, String languages, String id) {
            this.available = available.equals("yes");
            this.minCapacity = Integer.parseInt(capacity.split("-")[0]);
            this.maxCapacity = Integer.parseInt(capacity.split("-")[1]);
            this.rating = rating;
            this.long_distance = long_distance.equals("yes");
            this.type = type;
            this.location = location;
            this.id = id;
            this.languages = new HashMap<>();
            String[] lang = languages.split("\\|");
            for(String l:lang) {
                this.languages.put(l, true);
            }
        }
        public Boolean canSpeak(String language) {
            return this.languages.containsKey(language);
        }
        public Boolean isAvailable() {
            return this.available;
        }
        public Boolean canFitPersons(int persons) {
            return (persons >= this.minCapacity && persons <= this.maxCapacity);
        }
        public Boolean canFitLuggage(int luggage) {
            if(this.type.equals("minivan") && luggage <= 5)
                return true;
            if(this.type.equals("large") && luggage <= 3)
                return true;
            if(this.type.equals("compact") && luggage <= 2)
                return true;
            if(this.type.equals("subcompact") && luggage <= 1)
                return true;
            return false;
        }
        public Boolean longDistance() {
            return this.long_distance;
        }
        public Node getLocation() {
            return this.location;
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
                else if(p.isCyan)
                    xml += "<styleUrl>#DodgerBlue</styleUrl>";
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
        boolean isCyan;

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
        public void setCyan() {
            this.isCyan = true;
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

                                if((explored.contains(child)) && (temp_f_scores >= child.fScore)){
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

    public static double AVERAGE_RADIUS_OF_EARTH_KM = 6371;
    public static double calculateDistanceInKilometer(double userLat, double userLng,
  double venueLat, double venueLng) {

    double latDistance = Math.toRadians(userLat - venueLat);
    double lngDistance = Math.toRadians(userLng - venueLng);

    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
      + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(venueLat))
      * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return (double) (AVERAGE_RADIUS_OF_EARTH_KM * c);
}


    public static void AstarSearchMeKosmognwsti(Node source, Node goal, JIPEngine jip, JIPTermParser kosmognwstis, HashMap<String, Node> nodesMap){

        Set<Node> explored = new HashSet<Node>();
        PriorityQueue<Node> queue = new PriorityQueue<Node>(20, 
            new Comparator<Node>()
            {
                //override compare method
                public int compare(Node i, Node j) {
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
        String timeName="";
        if (source.hasClient==true)
            timeName=source.callTime;
        else 
            timeName=goal.callTime;
        boolean found = false;

        while((!queue.isEmpty())&&(!found)) {

            //the node in having the lowest f_score value
            Node current = queue.poll();
            explored.add(current);

            //goal found
            if(current.getName().equals(goal.getName())){
                    found = true;
            }

            //check every child of current node
            JIPQuery jipQuery; 
            JIPTerm term;
            jipQuery = jip.openSynchronousQuery(kosmognwstis.parseTerm("canMoveFromTo(n" + current.getName() + ",Y,R,D)."));
            // System.out.println("canMoveFromTo(n" + current.getName() + ",Y,R,D).");
            // System.out.print(term.getVariablesTable().get("X").toString());
            term = jipQuery.nextSolution();
            while(term != null) {
                Node child = nodesMap.get(term.getVariablesTable().get("Y").toString().replace("n", ""));
                String distanceStr = term.getVariablesTable().get("D").toString();
                double distance = Double.parseDouble(distanceStr);
                JIPQuery jipQuery2; 
                JIPTerm term2;
                jipQuery2 = jip.openSynchronousQuery(kosmognwstis.parseTerm("priority(" + term.getVariablesTable().get("R").toString() + ","+"morning"+",Z)."));
                // System.out.println("priority(" + term.getVariablesTable().get("R").toString() + ",morning,Z).");
                term2 = jipQuery2.nextSolution();
                double speed = Double.parseDouble(term2.getVariablesTable().get("Z").toString());
                double cost = distance / speed;
                // System.out.println("Cost " + cost);
                // System.out.println("Distance " + distance);
                // System.out.println("Speed " + speed);
                double temp_g_scores = current.gScore + cost;

                Edge e = new Edge(current, child, term.getVariablesTable().get("R").toString().replace("e", ""));
                double temp_f_scores = temp_g_scores + e.getHscore(source);

                //h_scores;

                term = jipQuery.nextSolution();
                /*if child node has been evaluated and
                the newer f_score is higher, skip*/

                if((explored.contains(child)) && (temp_f_scores >= child.fScore)){
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
    	JIPEngine jip = new JIPEngine();
		jip.consultFile("kosmos.pl");
		jip.consultFile("kosmognwstis.pl");
		JIPTermParser parser = jip.getTermParser();
        JIPQuery jipQuery; 
		JIPTerm term;
		// jipQuery = jip.openSynchronousQuery(parser.parseTerm("belongsTo(807174360,X)."));
		// term = jipQuery.nextSolution();
		 System.out.print("Preparing Map ......");

        String csvFile = "nodes.csv";
        String line = "";
        String cvsSplitBy = ",";
        HashMap<String, Node> nodesMap = new HashMap<>();
        HashMap<String, Taxi> taxiNodes = new HashMap<>();
        String clientNode = null;
        String destNode = null;

        Edge currEdge = null;
        String prevEdgeId = null;
        String prevNodeName = null;
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            br.readLine();
            while ((line = br.readLine()) != null) {

                // use comma as separator
                
                String[] row = line.split(cvsSplitBy,10);
                String nodeX = row[0];
                String nodeY = row[1];
                String nodeName = row[3];
                String edgeId = row[2];
                if (!nodesMap.containsKey(nodeName)) {
                    Node n = new Node(nodeX, nodeY, nodeName);
                    nodesMap.put(nodeName, n);
                }
                if(prevEdgeId != null && prevEdgeId.equals(edgeId)) {
                    Edge from = new Edge(nodesMap.get(prevNodeName), nodesMap.get(nodeName), edgeId);
                    Edge to = new Edge(nodesMap.get(nodeName), nodesMap.get(prevNodeName), edgeId);
                    nodesMap.get(prevNodeName).addEdge(from);
                    nodesMap.get(nodeName).addEdge(to);
                }
                prevEdgeId = edgeId;
                prevNodeName =  nodeName;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Complete!");

        try (BufferedReader br = new BufferedReader(new FileReader("taxis.csv"))) {
            System.out.print("Gathering Taxi Data ......");

            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] row = line.split(cvsSplitBy, 20);
                double taxiX = Double.parseDouble(row[0]);
                double taxiY = Double.parseDouble(row[1]);
                String taxiId = row[2];
                String available = row[3];
                String capacity = row[4];
                String languages = row[5];
                double rating = Double.parseDouble(row[6]);
                String long_distance = row[7];
                String type = row[8];
                String closestNode = null;
                double minDistance = 0;
                for (String key : nodesMap.keySet()) {
                    double tempDistance = nodesMap.get(key).getDistance(taxiX, taxiY);
                    if(closestNode == null || minDistance > tempDistance) {
                        closestNode = key;
                        minDistance = tempDistance;
                    }
                }
                nodesMap.get(closestNode).addTaxi(taxiId);
                Taxi taxiNode = new Taxi(nodesMap.get(closestNode), available, capacity, rating, long_distance, type, languages, taxiId);
                taxiNodes.put(taxiId, taxiNode);
            }   
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Complete!");

        String timeName = "", language = "";
        int persons = 0;
        int luggage = 0;
        System.out.print("Gathering Client Information ......");

        try (BufferedReader br = new BufferedReader(new FileReader("client.csv"))) {
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] row = line.split(cvsSplitBy, 10);
                double clientX = Double.parseDouble(row[0]);
                double clientY = Double.parseDouble(row[1]);
                double destX = Double.parseDouble(row[2]);
                double destY = Double.parseDouble(row[3]);
                String[] time = row[4].split(":");
                timeName = "other";
                int hour = Integer.parseInt(time[0]);
                int minute = Integer.parseInt(time[1]);
                if((hour >= 9 && hour < 11) || (hour == 11 && minute == 0)) {
                    timeName = "morning";
                }
                else if((hour >= 13 && hour < 15) || (hour == 15 && minute == 0)) {
                    timeName = "noon";
                }
                else if((hour >= 17 && hour < 19) || (hour == 19 && minute == 0)) {
                    timeName = "afternoon";
                }
                persons = Integer.parseInt(row[5]);
                language = row[6];
                luggage = Integer.parseInt(row[7]);
                
                String closestNode = null;
                double minDistance = 0;
                for (String key : nodesMap.keySet()) {
                    double tempDistance = nodesMap.get(key).getDistance(clientX, clientY);
                    if(closestNode == null || minDistance > tempDistance) {
                        closestNode = key;
                        minDistance = tempDistance;
                    }
                }
                nodesMap.get(closestNode).addClient();
                clientNode = closestNode;
                nodesMap.get(clientNode).addCallTime(timeName);
                closestNode = null;
                minDistance = 0;
                for (String key : nodesMap.keySet()) {
                    double tempDistance = nodesMap.get(key).getDistance(destX, destY);
                    if(closestNode == null || minDistance > tempDistance) {
                        closestNode = key;
                        minDistance = tempDistance;
                    }
                }
                // nodesMap.get(closestNode).addClient();
                destNode = closestNode;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Complete!");

        Node  destination= nodesMap.get(destNode);
        Node  clientStart= nodesMap.get(clientNode);
        AstarSearchMeKosmognwsti(clientStart, destination, jip, parser, nodesMap);
        KML kml = new KML();
        PlacemarkKML destPlacemark = new PlacemarkKML();

        double clientDestDistance = 0;    
        for(Node node = destination; node!=clientStart; node = node.getParentNode()){
            //System.out.println(node.getParentNode());
            if (node.getParentNode()!=null) {//elec
                clientDestDistance += calculateDistanceInKilometer(node.getX(), node.getY(),node.getParentNode().getX(),node.getParentNode().getY());
                //node.getDistance(node.getParentNode().getX(), node.getParentNode().getY());

            }
            destPlacemark.addNode(node, clientDestDistance);

        }
        destPlacemark.setTaxiName("Route");
        
        //System.out.println(clientDestDistance);
        
        PlacemarkKML closestPlacemark = null;
        double distance = 0;
        String selectedTaxi = null;
        //ArrayList<double> fastestTaxis = new ArrayList<double>;
        //ArrayList<int> fastestTaxisID = new ArrayList<int>;
        TreeMap<Double,String> fastestTaxis=new TreeMap<Double,String>();  



        System.out.println("Searching for taxis...");

        for (String key : taxiNodes.keySet()) {
            Taxi currTaxi = taxiNodes.get(key);
            if(!currTaxi.canSpeak(language)) continue;
            if(!currTaxi.canFitPersons(persons)) continue;
            if(!currTaxi.canFitLuggage(luggage)) continue;
            if(!currTaxi.isAvailable()) continue;
            Node target = nodesMap.get(clientNode);
            Node source = taxiNodes.get(key).getLocation();
            //System.out.println("\tSource is: " + source);
            //System.out.println("\tTarget is: " + target);
            AstarSearchMeKosmognwsti(source, target, jip, parser, nodesMap);
            //System.out.println("\tASTARFINISHED ");


            PlacemarkKML tempPlacemark = new PlacemarkKML();
            double tempDistance = 0;
            double tempDistancekm = 0;

         
            for(Node node = target; node!=source; node = node.getParentNode()){
                //System.out.println("poulo!");
                if (node.getParentNode()!=null) {

                    tempDistance += node.getDistance(node.getParentNode().getX(), node.getParentNode().getY());
                    tempDistancekm += calculateDistanceInKilometer(node.getX(), node.getY(),node.getParentNode().getX(), node.getParentNode().getY());
                }
                tempPlacemark.addNode(node, tempDistance);
            }
            if(closestPlacemark == null || distance > tempDistance) {
                distance = tempDistance;
                closestPlacemark = tempPlacemark;
                selectedTaxi = key;
            }
            if(!currTaxi.longDistance() && (tempDistancekm + clientDestDistance > 30)) continue; // Edw vazeis 30km sto 1
            //System.out.println("Taxi " + String.format("%5s", currTaxi.id) + " ----------------");
            //System.out.println("\tDistance: " + tempDistance); 
            //System.out.println("\tRating: " + currTaxi.rating);
            //System.out.println("\tCar Type: " + currTaxi.type);
            //System.out.println("\tDistance in km: " + tempDistancekm);

            fastestTaxis.put(tempDistancekm,currTaxi.id);
            tempPlacemark.setTaxiName(key);
            kml.addPlacemark(tempPlacemark);
        }

        TreeMap<Double,String> bestTaxis=new TreeMap<Double,String>();  

        System.out.println(" ----------------" + "1st Taxi Sorting " + " ----------------");
        int count=0;
        for(Map.Entry m:fastestTaxis.entrySet()){  
            if (count<5){
                count++;

                System.out.println(count+ ". Taxi " + m.getValue() + " ----------------");
                System.out.println("Smart Function Result = " + m.getKey());  
                double key = (double)m.getKey();
                String val = (String)m.getValue();



                bestTaxis.put(key,val);

            }

        }  
        TreeMap<Double,String> bestTaxisRating=new TreeMap<Double,String>(Collections.reverseOrder());  


        for (String key : taxiNodes.keySet()) {
            Taxi currTaxi = taxiNodes.get(key);

            for(Map.Entry m:bestTaxis.entrySet()){ 
                if (currTaxi.id.equals(m.getValue()))
                    bestTaxisRating.put(currTaxi.rating,currTaxi.id);
            } 

        }

        System.out.println(" ----------------" + "2nd Taxi Sorting " + " ----------------");
         count=0;

        for(Map.Entry m:bestTaxisRating.entrySet()){  
                count++;
                System.out.println(count+ ". Taxi " + m.getValue() + " ----------------");
                System.out.println("Smart Function Result = " + m.getKey());  

        }  

        // Edw erwtisi ston xristi gia pio taksi thelei kai afto na orizeis ws closes placemark & selected taxi
        KML resultkml = new KML();
        resultkml.addPlacemark(destPlacemark);
        for(PlacemarkKML p : kml.placemarks) {
            for(Map.Entry m:bestTaxis.entrySet()){ 
                if (p.getTaxiName().equals(m.getValue()))
                    resultkml.addPlacemark(p);

            }
        }
                    System.out.println("Please choose your taxi." );  


                    Scanner sc = new Scanner(System.in);
                    boolean notfound=true;
                    while (notfound){
            String input = sc.next();
                boolean blnExists = bestTaxisRating.containsValue(input);
                if (blnExists) {
                    for(PlacemarkKML p : resultkml.placemarks) {
                        if (p.getTaxiName().equals(input)){
                            p.setGreen();
                            notfound=false;
                        }

                    }
                }else{
                    System.out.println("Please enter a valid Taxi ID." );  

                }
                }

            
 

        System.out.print("Success!!!");  
        destPlacemark.setCyan();

        resultkml.addPlacemark(destPlacemark);

        resultkml.build();
        resultkml.save("clientoriginal");
//         for (String name: nodesMap.keySet()){

//             String key =name.toString();
//             Node value = nodesMap.get(name);  
//             System.out.print(key + ": ");  
//             value.printEdges();


// } 
        // HashMap<String, List> nodesMap = new HashMap<>();

    }

}
