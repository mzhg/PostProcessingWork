/*
 * Poly2Tri Copyright (c) 2009-2010, Poly2Tri Contributors
 * http://code.google.com/p/poly2tri/
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Poly2Tri nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package poly2tri.sweep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import poly2tri.common.Edge;
import poly2tri.common.Point;
import poly2tri.common.Triangle;

public class SweepContext {
	// Inital triangle factor, seed triangle will extend 30% of
	// PointSet width to both left and right.
	static final double kAlpha = 0.3;

	final Basin basin = new Basin();
	final EdgeEvent edge_event = new EdgeEvent();
	
	final ArrayList<Edge> edge_list = new ArrayList<Edge>();
	
	private final ArrayList<Triangle> triangles_ = new ArrayList<>();
	private final ArrayList<Triangle> map_ = new ArrayList<Triangle>();
	private final ArrayList<Point> points_ = new ArrayList<Point>();

	// Advancing front
	private AdvancingFront front_;
	// head point used with advancing front
	private Point head_;
	// tail point used with advancing front
	private Point tail_;

	private P2TNode af_head_, af_middle_, af_tail_;
	
	/// Constructor
	public SweepContext(List<Point> polyline){
		points_.addAll(polyline);
		
		initEdges(points_);
	}

	public void set_head(Point p1) { head_ = p1;}

	public Point head() { return head_;}

	public void set_tail(Point p1) { tail_ = p1;}

	public Point tail() { return tail_;}

	public int point_count(){ return points_.size();}

	public P2TNode locateNode(Point point){ return front_.locateNode(point.x);}

	public void createAdvancingFront(ArrayList<P2TNode> nodes){
		// Initial triangle
		  Triangle triangle = new Triangle(points_.get(0), tail_, head_);

		  map_.add(triangle);

		  af_head_ = new P2TNode(triangle.getPoint(1), triangle);
		  af_middle_ = new P2TNode(triangle.getPoint(0), triangle);
		  af_tail_ = new P2TNode(triangle.getPoint(2));
		  front_ = new AdvancingFront(af_head_, af_tail_);

		  // TODO: More intuitive if head is middles next and not previous?
		  //       so swap head and tail
		  af_head_.next = af_middle_;
		  af_middle_.next = af_tail_;
		  af_middle_.prev = af_head_;
		  af_tail_.prev = af_middle_;
	}

	/// Try to map a node to all sides of this triangle that don't have a neighbor
	public void mapTriangleToNodes(Triangle t){
		for (int i = 0; i < 3; i++) {
		    if (t.getNeighbor(i) == null) {
		      P2TNode n = front_.locatePoint(t.pointCW(t.getPoint(i)));
		      if (n != null)
		        n.triangle = t;
		    }
		  }
	}

	public void addToMap(Triangle triangle) { map_.add(triangle); }

	public Point getPoint(int index){ return points_.get(index);}

	void removeFromMap(Triangle triangle){
		map_.remove(triangle);
	}

	public void addHole(List<Point> polyline){
		initEdges(polyline);
//		  for(unsigned int i = 0; i < polyline.size(); i++) {
//		    points_.push_back(polyline[i]);
//		  }
		points_.addAll(polyline);
	}

	public void addPoint(Point point){ points_.add(point); }
	public AdvancingFront front()    { return front_; }
	public void meshClean(Triangle triangle){
		if (triangle != null && !triangle.isInterior()) {
		    triangle.isInterior(true);
		    triangles_.add(triangle);
		    for (int i = 0; i < 3; i++) {
		      if (!triangle.constrained_edge[i])
		        meshClean(triangle.getNeighbor(i));
		    }
		  }
	}

	public List<Triangle> getTriangles() { return triangles_; }
	public List<Triangle> getMap()       { return map_;}

	void initTriangulation(){
		Point p = points_.get(0);
		double xmax = p.x, xmin = p.x;
		double ymax = p.y, ymin = p.y;

		  // Calculate bounds.
		  for (int i = 1; i < points_.size(); i++) {
		    p = points_.get(i);
		    if (p.x > xmax)
		      xmax = p.x;
		    if (p.x < xmin)
		      xmin = p.x;
		    if (p.y > ymax)
		      ymax = p.y;
		    if (p.y < ymin)
		      ymin = p.y;
		  }

		  double dx = kAlpha * (xmax - xmin);
		  double dy = kAlpha * (ymax - ymin);
		  head_ = new Point(xmax + dx, ymin - dy);
		  tail_ = new Point(xmin - dx, ymin - dy);

		  // Sort points along y-axis
//		  std::sort(points_.begin(), points_.end(), cmp);
		  Collections.sort(points_, cmp);
	}
	
	void initEdges(List<Point> polyline){
		int num_points = polyline.size();
		  for (int i = 0; i < num_points; i++) {
		    int j = i < num_points - 1 ? i + 1 : 0;
		    edge_list.add(new Edge(polyline.get(i), polyline.get(j)));
		  }
	}
	
	private static final Comparator<Point> cmp = new Comparator<Point>() {
		
		@Override
		public int compare(Point a, Point b) {
			 if (a.y < b.y) {
			    return -1;
			  } else if (a.y == b.y) {
			    // Make sure q is point with greater x value
			    if (a.x < b.x) {
			      return -1;
			    }else if(a.x == b.y)
			    	return 0;
			  }
			  return 1;
		}
	};
}
