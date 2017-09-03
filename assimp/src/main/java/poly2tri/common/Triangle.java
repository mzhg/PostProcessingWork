package poly2tri.common;

/**
 *  Triangle-based data structures are know to have better performance than quad-edge structures<p>
 * See: J. Shewchuk, "Triangle: Engineering a 2D Quality Mesh Generator and Delaunay Triangulator"
 *      "Triangulations in CGAL"
 */
public class Triangle {

	/** Triangle points */
	final Point[] points_ = new Point[3];
	/** Neighbor list */
	final Triangle[] neighbors_ = new Triangle[3];

	/** Has this triangle been marked as an interior triangle? */
	boolean interior_;
	
	/** Flags to determine if an edge is a Constrained edge */
	public final boolean[] constrained_edge = new boolean[3];
	/** Flags to determine if an edge is a Delauney edge */
	public final boolean[] delaunay_edge = new boolean[3];
		
	/// Constructor
	public Triangle(Point a, Point b, Point c){
		 points_[0] = a; points_[1] = b; points_[2] = c;
		 neighbors_[0] = null; neighbors_[1] = null; neighbors_[2] = null;
		 constrained_edge[0] = constrained_edge[1] = constrained_edge[2] = false;
		 delaunay_edge[0] = delaunay_edge[1] = delaunay_edge[2] = false;
		 interior_ = false;
	}

	public Point getPoint(int index){return points_[index];}
	/** The point counter-clockwise to given point */
	public Point pointCW(Point point){
		if (point == points_[0]) {
		    return points_[2];
		  } else if (point == points_[1]) {
		    return points_[0];
		  } else if (point == points_[2]) {
		    return points_[1];
		  }
//		  assert(0);
		  assert false;

		  return null;
	}
	
	/** The point counter-clockwise to given point */
	public Point pointCCW(Point point){
		if (point == points_[0]) {
		    return points_[1];
		  } else if (point == points_[1]) {
		    return points_[2];
		  } else if (point == points_[2]) {
		    return points_[0];
		  }
//		  assert(0);
		assert false;

		  return null;
	}
	public Point oppositePoint(Triangle t, Point p){
		Point cw = t.pointCW(p);
		
		  //double x = cw->x;
		  //double y = cw->y;
		  //x = p.x;
		  //y = p.y;
	    return pointCW(cw);
	}

	public Triangle getNeighbor(int index){ return neighbors_[index];}
	/** Update neighbor pointers */
	public void markNeighbor(Point p1, Point p2, Triangle t){
		if ((p1 == points_[2] && p2 == points_[1]) || (p1 == points_[1] && p2 == points_[2]))
		    neighbors_[0] = t;
		  else if ((p1 == points_[0] && p2 == points_[2]) || (p1 == points_[2] && p2 == points_[0]))
		    neighbors_[1] = t;
		  else if ((p1 == points_[0] && p2 == points_[1]) || (p1 == points_[1] && p2 == points_[0]))
		    neighbors_[2] = t;
		  else
//		    assert(0);
			assert false;
	}
	
	/** Exhaustive search to update neighbor pointers */
	public void markNeighbor(Triangle t){
		if (t.contains(points_[1], points_[2])) {
		    neighbors_[0] = t;
		    t.markNeighbor(points_[1], points_[2], this);
		  } else if (t.contains(points_[0], points_[2])) {
		    neighbors_[1] = t;
		    t.markNeighbor(points_[0], points_[2], this);
		  } else if (t.contains(points_[0], points_[1])) {
		    neighbors_[2] = t;
		    t.markNeighbor(points_[0], points_[1], this);
		  }
	}

	public void markConstrainedEdge(int index){ constrained_edge[index] = true; }
	public void markConstrainedEdge(Edge edge){ markConstrainedEdge(edge.p, edge.q);}
	public void markConstrainedEdge(Point p, Point q){
		if ((q == points_[0] && p == points_[1]) || (q == points_[1] && p == points_[0])) {
		    constrained_edge[2] = true;
		  } else if ((q == points_[0] && p == points_[2]) || (q == points_[2] && p == points_[0])) {
		    constrained_edge[1] = true;
		  } else if ((q == points_[1] && p == points_[2]) || (q == points_[2] && p == points_[1])) {
		    constrained_edge[0] = true;
		  }
	}

	public int index(Point p){
		if (p == points_[0]) {
		    return 0;
		  } else if (p == points_[1]) {
		    return 1;
		  } else if (p == points_[2]) {
		    return 2;
		  }
//		  assert(0);
		assert false;

		  return 0;
	}
	
	public int edgeIndex(Point p1, Point p2){
		if (points_[0] == p1) {
		    if (points_[1] == p2) {
		      return 2;
		    } else if (points_[2] == p2) {
		      return 1;
		    }
		  } else if (points_[1] == p1) {
		    if (points_[2] == p2) {
		      return 0;
		    } else if (points_[0] == p2) {
		      return 2;
		    }
		  } else if (points_[2] == p1) {
		    if (points_[0] == p2) {
		      return 1;
		    } else if (points_[1] == p2) {
		      return 0;
		    }
		  }
		  return -1;
	}

	/** The neighbor clockwise to given point */
	public Triangle neighborCW(Point point){
		if (point == points_[0]) {
		    return neighbors_[1];
		  } else if (point == points_[1]) {
		    return neighbors_[2];
		  }
		  return neighbors_[0];
	}
	
	/** The neighbor counter-clockwise to given point */
	public Triangle neighborCCW(Point point){
		if (point == points_[0]) {
		    return neighbors_[2];
		  } else if (point == points_[1]) {
		    return neighbors_[0];
		  }
		  return neighbors_[1];
	}
	
	public boolean getConstrainedEdgeCCW(Point p){
		if (p == points_[0]) {
		    return constrained_edge[2];
		  } else if (p == points_[1]) {
		    return constrained_edge[0];
		  }
		  return constrained_edge[1];
	}
	
	public boolean getConstrainedEdgeCW(Point p){
		if (p == points_[0]) {
		    return constrained_edge[1];
		  } else if (p == points_[1]) {
		    return constrained_edge[2];
		  }
		  return constrained_edge[0];
	}
	
	public void setConstrainedEdgeCCW(Point p, boolean ce){
		if (p == points_[0]) {
		    constrained_edge[2] = ce;
		  } else if (p == points_[1]) {
		    constrained_edge[0] = ce;
		  } else {
		    constrained_edge[1] = ce;
		  }
	}
	
	public void setConstrainedEdgeCW(Point p, boolean ce){
		if (p == points_[0]) {
		    constrained_edge[1] = ce;
		  } else if (p == points_[1]) {
		    constrained_edge[2] = ce;
		  } else {
		    constrained_edge[0] = ce;
		  }
	}
	
	public boolean getDelunayEdgeCCW(Point p){
		if (p == points_[0]) {
		    return delaunay_edge[2];
		  } else if (p == points_[1]) {
		    return delaunay_edge[0];
		  }
		  return delaunay_edge[1];
	}
	
	public boolean getDelunayEdgeCW(Point p){
		if (p == points_[0]) {
		    return delaunay_edge[1];
		  } else if (p == points_[1]) {
		    return delaunay_edge[2];
		  }
		  return delaunay_edge[0];
	}
	
	public void setDelunayEdgeCCW(Point p, boolean e){
		if (p == points_[0]) {
		    delaunay_edge[2] = e;
		  } else if (p == points_[1]) {
		    delaunay_edge[0] = e;
		  } else {
		    delaunay_edge[1] = e;
		  }
	}
	
	public void setDelunayEdgeCW(Point p, boolean e){
		if (p == points_[0]) {
		    delaunay_edge[1] = e;
		  } else if (p == points_[1]) {
		    delaunay_edge[2] = e;
		  } else {
		    delaunay_edge[0] = e;
		  }
	}

	public boolean contains(Point p){return p == points_[0] || p == points_[1] || p == points_[2];}
	public boolean contains(Edge e) { return contains(e.p) && contains(e.q);}
	public boolean contains(Point p, Point q){ return contains(p) && contains(q);}
	/** Legalized triangle by rotating clockwise around point(0) */
	public void legalize(Point point){
		points_[1] = points_[0];
		points_[0] = points_[2];
		points_[2] = point;
	}
	
	/** Legalize triagnle by rotating clockwise around oPoint */
	public void legalize(Point opoint, Point npoint){
		if (opoint == points_[0]) {
		    points_[1] = points_[0];
		    points_[0] = points_[2];
		    points_[2] = npoint;
		  } else if (opoint == points_[1]) {
		    points_[2] = points_[1];
		    points_[1] = points_[0];
		    points_[0] = npoint;
		  } else if (opoint == points_[2]) {
		    points_[0] = points_[2];
		    points_[2] = points_[1];
		    points_[1] = npoint;
		  } else {
//		    assert(0);
			  assert false;
		  }
	}
	/**
	 * Clears all references to all other triangles and points
	 */
	public void clear(){
		Triangle t;
	    for( int i=0; i<3; i++ )
	    {
	        t = neighbors_[i];
	        if( t != null )
	        {
	            t.clearNeighbor( this );
	        }
	    }
	    clearNeighbors();
	    points_[0]=points_[1]=points_[2] = null;
	}
	
	public void clearNeighbor(Triangle triangle ){
		if( neighbors_[0] == triangle )
	    {
	        neighbors_[0] = null;
	    }
	    else if( neighbors_[1] == triangle )
	    {
	        neighbors_[1] = null;            
	    }
	    else
	    {
	        neighbors_[2] = null;
	    }
	}
	
	public void clearNeighbors(){
		neighbors_[0] = null;
		neighbors_[1] = null;
		neighbors_[2] = null;
	}
	
	public void clearDelunayEdges(){
		delaunay_edge[0] = delaunay_edge[1] = delaunay_edge[2] = false;
	}

	public boolean isInterior(){ return interior_;}
	public void isInterior(boolean b){ interior_ = b;}

	public Triangle neighborAcross(Point opoint){
		if (opoint == points_[0]) {
		    return neighbors_[0];
		  } else if (opoint == points_[1]) {
		    return neighbors_[1];
		  }
		  return neighbors_[2];
	}

	public void debugPrint(){
		System.out.print(points_[0].x + "," + points_[0].y + " ");
		System.out.print(points_[1].x + "," + points_[1].y + " ");
		System.out.print(points_[2].x + "," + points_[2].y + "\n");
	}
	
}
