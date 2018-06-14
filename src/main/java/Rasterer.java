import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    QuadTree quadtree;
    double ul_lon = -122.2998046875;
    double ul_lat = 37.892195547244356;
    double lr_lon = -122.2119140625;
    double lr_lat = 37.82280243352756;
    // Recommended: QuadTree instance variable. You'll need to make
    //              your own QuadTree since there is no built-in quadtree in Java.

    /** imgRoot is the name of the directory containing the images.
     *  You may not actually need this for your class. */
    public Rasterer(String imgRoot) {
        this.quadtree = new QuadTree(imgRoot, ul_lon, ul_lat, lr_lon, lr_lat);
    }

    static class QuadTree{
        TreeNode head;
        String imgRoot;
        int maxLevel;

        private QuadTree(String imgRoot, double ul_lon, double ul_lat, double lr_lon, double lr_lat){
            this.maxLevel = 7;
            this.head = constructTree("", 0, ul_lon, ul_lat, lr_lon, lr_lat);
            this.imgRoot = imgRoot;
            this.head.directory = "0";
        }

        private TreeNode constructTree(String root, int level, double ul_lon, double ul_lat, double lr_lon, double lr_lat){

            TreeNode node = new TreeNode(root, level, ul_lon, ul_lat, lr_lon, lr_lat);
            if(level == 7) return node; //TODO: check if we can find root in file
            node.children = new TreeNode[2][2];
            node.children[0][0] = constructTree(root + 1, level + 1, ul_lon, ul_lat,ul_lon+(lr_lon-ul_lon)/2, lr_lat+(ul_lat-lr_lat)/2);
            node.children[0][1] = constructTree(root + 2, level + 1, ul_lon+(lr_lon-ul_lon)/2, ul_lat, lr_lon, lr_lat+(ul_lat-lr_lat)/2);
            node.children[1][0] = constructTree(root + 3, level + 1, ul_lon, lr_lat+(ul_lat-lr_lat)/2,ul_lon+(lr_lon-ul_lon)/2, lr_lat);
            node.children[1][1] = constructTree(root + 4, level + 1, ul_lon+(lr_lon-ul_lon)/2, lr_lat+(ul_lat-lr_lat)/2,lr_lon, lr_lat);
//            this.maxLevel = level + 1;
            return node;
        }
    }

    static class TreeNode{
        String directory;
        int level;
        TreeNode[][] children;
        double ul_lon;
        double ul_lat;
        double lr_lon;
        double lr_lat;

        private TreeNode(String dir, int level, double ul_lon, double ul_lat, double lr_lon, double lr_lat){
            this.directory = dir;
            this.level = level;
            this.ul_lon = ul_lon;
            this.ul_lat = ul_lat;
            this.lr_lon = lr_lon;
            this.lr_lat = lr_lat;
        }
    }
    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     * <p>
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     * </p>
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified:
     * "render_grid"   -> String[][], the files to display
     * "raster_ul_lon" -> Number, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Number, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Number, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Number, the bounding lower right latitude of the rastered image <br>
     * "depth"         -> Number, the 1-indexed quadtree depth of the nodes of the rastered image.
     *                    Can also be interpreted as the length of the numbers in the image
     *                    string. <br>
     * "query_success" -> Boolean, whether the query was able to successfully complete. Don't
     *                    forget to set this to true! <br>
     * @see #REQUIRED_RASTER_REQUEST_PARAMS
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        // System.out.println(params);
        Map<String, Object> results = new HashMap<>();
        double ullon = params.get("ullon");
        double ullat = params.get("ullat");
        double lrlon = params.get("lrlon");
        double lrlat = params.get("lrlat");
        int level = 0;
        results.put("query_success", false);
        // check if params valid
        if(!REQUIRED_RASTER_REQUEST_PARAMS(params)) return results;

        // get the level
        double londpp = (lrlon - ullon)/params.get("w");
        double level_londpp = 0.00034332275390625;
        while(level_londpp > londpp){
            level_londpp /= 2;
            level ++;
        }
        if(level > this.quadtree.maxLevel) level = this.quadtree.maxLevel;

        // get ul & lr nodes
        TreeNode node1 = this.quadtree.head;
        while(node1.children != null && node1.level < level){
            node1 = node1.children[(node1.ul_lat - ullat)/(node1.ul_lat-node1.lr_lat)<0.5?0:1][(ullon-node1.ul_lon)/(node1.lr_lon-node1.ul_lon)<0.5?0:1];
        }
        TreeNode node2 = this.quadtree.head;
        while(node2.children != null && node2.level < level) {
            node2 = node2.children[(node2.ul_lat - lrlat) / (node2.ul_lat - node2.lr_lat) < 0.5 ? 0 : 1][(lrlon - node2.ul_lon) / (node2.lr_lon - node2.ul_lon) < 0.5 ? 0 : 1];
        }
        results.put("raster_ul_lon", node1.ul_lon);
        results.put("raster_ul_lat", node1.ul_lat);
        results.put("raster_lr_lon", node2.lr_lon);
        results.put("raster_lr_lat", node2.lr_lat);
        results.put("depth", level);

        int m = (int) Math.round((node1.ul_lat - node2.lr_lat)/(node1.ul_lat - node1.lr_lat));
        int n = (int) Math.round((node2.lr_lon - node1.ul_lon)/(node1.lr_lon - node1.ul_lon));
        String[][] grid = new String[m][n];
        grid[0][0] = node1.directory;
        char[] str;
        for(int i=0; i<m; i++){
            if(i == 0) str = grid[0][0].toCharArray();
            else{
                str = grid[i-1][0].toCharArray();
                for(int k=level-1; k>=0; k--){
                    str[k] += 2;
                    if(str[k] <= '4') break;
                    else str[k] -= 4;
                }
                grid[i][0] = new String(str);
            }
            for(int j=1; j<n; j++){
                for(int k=level-1; k>=0; k--){
                    if(str[k] == '1' || str[k] == '3') {
                        str[k] += 1;
                        break;
                    }
                    else str[k] -= 1;
                }
                grid[i][j] = new String(str);
            }
        }
        for(int i=0; i<m; i++){
            for(int j=0; j<n; j++){
                grid[i][j] = "img/" + grid[i][j] + ".png";
            }
        }
        results.put("render_grid", grid);
        results.put("query_success", true);
        return results;
    }
    private boolean REQUIRED_RASTER_REQUEST_PARAMS(Map<String, Double> params){
        if(params.get("ullon") >= params.get("lrlon")) return false;
        if(params.get("lrlat") >= params.get("ullat")) return false;
        if(params.get("lrlon") <= this.ul_lon) return false;
        if(params.get("ullat") <= this.lr_lat) return false;
        return true;
    }

}
