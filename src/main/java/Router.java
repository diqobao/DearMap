import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {
    /**
     * Return a LinkedList of <code>Long</code>s representing the shortest path from st to dest, 
     * where the longs are node IDs.
     */
    public static LinkedList<Long> shortestPath(GraphDB g, double stlon, double stlat, double destlon, double destlat) {
        long start = g.closest(stlon, stlat), destination = g.closest(destlon, destlat), last = start;
        LinkedList<Long> route = new LinkedList<Long>();
        double shortest;
        int count = 0, max_count = 999999999; //TODO: initialize max_count with g.node_number
        while(count < max_count){
            Iterable<Long> neighbors= g.adjacent(start);
            long shortestNode = 0;
            shortest = 99999999; // TODO: smarter approach?
            for(long neighbor: neighbors){
                if(g.distance(neighbor, destination) + g.distance(neighbor, last) < shortest){
                    shortest = g.distance(neighbor, destination) + g.distance(neighbor, last);
                    shortestNode = neighbor;
                }
                if(neighbor == destination) {
                    route.add(neighbor);
                    return route;
                }
            }
            route.add(shortestNode);
            last = shortestNode;
            count ++;
        }
        return new LinkedList<Long>();
    }
}
