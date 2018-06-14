import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    private HashMap<Long, Node> nodes = new HashMap<>();
    private HashMap<Long, Way> ways = new HashMap<>();
    private HashMap<String, Node> name2node = new HashMap<>();
    Trie trie = new Trie();
    int node_number = 0;

    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputFile, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    public void addnode(Node n){
        this.nodes.put(n.id(), n);
    }

    public void addtn(String word) { this.trie.insert(cleanString(word));}

    public void addway(Way w){ this.ways.put(w.id, w); }

    public void addname2node(String name, Node n){
        this.name2node.put(cleanString(name), n);
    }
    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     * Find all the nodes whose name has prefix s
     * @param s prefix of some words
     * @return all the words have prefix s
     */
    public ArrayList<String> findlocationbyprefix(String s){
        ArrayList<String> result = new ArrayList<>();
        for (String i : this.trie.findbyprefix(cleanString(s))){
            result.add(this.name2node.get(i).name);
        }
        return result;
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        // TODO: Your code here.
        Iterator iter = nodes.entrySet().iterator();
        while (iter.hasNext()){
            Map.Entry<Long, Node> entry = (Map.Entry) iter.next();
            if (entry.getValue().head == entry.getValue().tail){
                iter.remove();
                this.node_number--;
            }
        }
    }

    /** Returns an iterable of all vertex IDs in the graph. */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        ArrayList<Long> result = new ArrayList<>();
        Iterator iter = nodes.entrySet().iterator();
        while (iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            result.add((Long) entry.getKey());
        }
        return result;
    }

    /** Returns ids of all vertices adjacent to v. */
    Iterable<Long> adjacent(long v) {
        ArrayList<Long> result = new ArrayList<>();
        Node.Vertix point = this.nodes.get(v).head;
        while (point.next != null){
            point = point.next;
            result.add(point.id);
        }
        return result;
    }

    /** Returns the Euclidean distance between vertices v and w, where Euclidean distance
     *  is defined as sqrt( (lonV - lonV)^2 + (latV - latV)^2 ). */
    double distance(long v, long w) {
        double result;
        result = Math.sqrt((nodes.get(v).latitude - nodes.get(w).latitude) * (nodes.get(v).latitude - nodes.get(w).latitude) +
                (nodes.get(v).longitude - nodes.get(w).longitude) * (nodes.get(v).longitude - nodes.get(w).longitude));
        return result;
    }

    /** set adjacent nodes **/
    public void adj(long v, long w){
        this.nodes.get(v).insert_adj(w, distance(v, w));
        this.nodes.get(w).insert_adj(v, distance(v, w));
    }

    /** Returns the vertex id closest to the given longitude and latitude. */
    long closest(double lon, double lat) {
        long result=0;
        double s = 10000, lon2, lat2;
        Iterator iter = nodes.entrySet().iterator();
        while (iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            lon2 = ((Node) entry.getValue()).longitude;
            lat2 = ((Node) entry.getValue()).latitude;
            if (s > Math.sqrt(Math.pow(lon2-lon, 2) + Math.pow(lat2-lat, 2))){
                s = (Math.pow(lon2-lon, 2) + Math.pow(lat2-lat, 2));
                result = (long) entry.getKey();
            }
        }
        return result;
    }

    /** Longitude of vertex v. */
    double lon(long v) {
        return this.nodes.get(v).longitude;
    }

    /** Latitude of vertex v. */
    double lat(long v) { return this.nodes.get(v).latitude; }

    /**
     * Node which comprises the backbone of the map
     */
    public static class Node {
        Vertix head, tail;
        double latitude, longitude;
        String name;
        long id;

        public Node(String i, String lat, String lon){
            this.head = new Vertix(Long.parseLong(i));
            this.tail = this.head;
            this.latitude = Double.parseDouble(lat);
            this.longitude = Double.parseDouble(lon);
            id = Long.parseLong(i);
        }

        public void insert_adj(long n, double dist){
            this.tail.next = new Vertix(n, dist);
            this.tail = this.tail.next;
            this.tail.distance = dist;
        }

        /** return name of this node **/
        public long id(){ return this.head.id; }

        public class Vertix{
            private long id;
            private double distance;
            Vertix next;

            public Vertix(long n){
                this.id = n;
                this.distance = 0;
            }

            public Vertix(long n, double dist){
                this.id = n;
                this.distance = dist;
            }

            public long name(){
                return this.id;
            }
            /** distance of this vertix between main vertix **/
            public double distance(){
                return this.distance;
            }
        }
    }

    /** Way which is comprised by Nodes **/
    public static class Way{
        boolean valid;
        String maxspeed;
        String name;
        String lastnode;
        ArrayList<Long> nodes = new ArrayList<>();
        long id;

        public Way(String i){
            this.valid = false;
            this.maxspeed = "";
            this.name = "";
            this.id = Long.parseLong(i);
        }

        public void addnode(String i) { nodes.add(Long.parseLong(i)); }
    }

    public static class Trienode{
        char content;
        boolean isEnd;
        ArrayList<Trienode> childlist;

        public Trienode(char c){
            childlist = new ArrayList<>();
            isEnd = false;
            content = c;
        }

        public Trienode subNode(char c){
                if (childlist != null){
                    for (Trienode child : childlist){
                        if (child.content == c)
                            return child;
                    }
                }
                return null;
        }

        public ArrayList<String> findbyprefix(String prefix){
            ArrayList<String> result = new ArrayList<>();
            for (Trienode tn : this.childlist){
                if (tn.isEnd == true)
                    result.add(prefix + tn.content);
                if (tn.childlist.size() != 0)
                    result.addAll(tn.findbyprefix(prefix + tn.content));
            }
            return result;
        }
    }

    public static class Trie{
        private Trienode root;

        public Trie(){
            root = new Trienode(' ');
        }

        public boolean search(String word){
            Trienode current = root;
            for (int i = 0; i < word.length(); i++){
                if (current.subNode(word.charAt(i)) == null)
                    return false;
                else
                    current = current.subNode(word.charAt(i));
            }
            if (current.isEnd == true)
                return true;
            else
                return false;
        }

        public ArrayList<String> findbyprefix(String prefix){
            Trienode current = root;
            for (int i = 0; i < prefix.length(); i++){
                if (current.subNode(prefix.charAt(i)) == null)
                    return new ArrayList<>();
                else
                    current = current.subNode(prefix.charAt(i));
            }
            return current.findbyprefix(prefix);
        }

        public void insert(String word){
            Trienode current = this.root;
            if (this.search(word)) return;
            for (int i = 0; i < word.length(); i++){
                Trienode temp = current.subNode(word.charAt(i));
                if (temp == null){
                    current.childlist.add(new Trienode(word.charAt(i)));
                    current = current.subNode(word.charAt(i));
                }else{
                    current = temp;
                }
            }
            current.isEnd = true;
        }

    }

}
