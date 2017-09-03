package poly2tri.common;

/**Represents a simple polygon's edge*/
public class Edge {

	public Point p;
	public Point q;
	
	public Edge(Point p1, Point p2) {
		if (p1.y > p2.y) {
		      q = p1;
		      p = p2;
		    } else if (p1.y == p2.y) {
		      if (p1.x > p2.x) {
		        q = p1;
		        p = p2;
		      } else if (p1.x == p2.x) {
		        // Repeat points
		        // ASSIMP_CHANGE (aramis_acg)
		        //assert(false);
		    	  throw new IllegalArgumentException("repeat points");
		      }
		    }

		    q.edge_list.add(this);
	}
}
