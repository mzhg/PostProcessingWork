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

import poly2tri.common.Edge;
import poly2tri.common.Point;
import poly2tri.common.Triangle;
import poly2tri.common.Utils;

/**
 * Sweep-line, Constrained Delauney Triangulation (CDT) See: Domiter, V. and
 * Zalik, B.(2008)'Sweep-line algorithm for constrained Delaunay triangulation',
 * International Journal of Geographical Information Science
 *
 * "FlipScan */
public class Sweep {
	
	private static final double M_PI_2 = Math.PI/2.0;
	final ArrayList<P2TNode> nodes_ = new ArrayList<P2TNode>();
	/**
	   * Triangulate simple polygon with holes
	   * 
	   * @param tcx
	   */
	 public void triangulate(SweepContext tcx){
		 tcx.initTriangulation();
		  tcx.createAdvancingFront(nodes_);
		  // Sweep points; build mesh
		  sweepPoints(tcx);
		  // Clean up
		  finalizationPolygon(tcx);
	 }
	  
	  /**
	   * Start sweeping the Y-sorted point set from bottom to top
	   * 
	   * @param tcx
	   */
	  void sweepPoints(SweepContext tcx){
		  for (int i = 1; i < tcx.point_count(); i++) {
		    Point point = tcx.getPoint(i);
		    P2TNode node = pointEvent(tcx, point);
		    for (int j = 0; j < point.edge_list.size(); j++) {
		      edgeEvent(tcx, point.edge_list.get(j), node);
		    }
		  }
	  }

	  /**
	   * Find closes node to the left of the new point and
	   * create a new triangle. If needed new holes and basins
	   * will be filled to.
	   *
	   * @param tcx
	   * @param point
	   * @return
	   */
	  P2TNode pointEvent(SweepContext tcx, Point point){
		  P2TNode node = tcx.locateNode(point);
		  P2TNode new_node = newFrontTriangle(tcx, point, node);

		  // Only need to check +epsilon since point never have smaller
		  // x value than node due to how we fetch nodes from the front
		  if (point.x <= node.point.x + Utils.EPSILON) {
		    fill(tcx, node);
		  }

		  //tcx.AddNode(new_node);

		  fillAdvancingFront(tcx, new_node);
		  return new_node;
	  }

	   /**
	     * 
	     * 
	     * @param tcx
	     * @param edge
	     * @param node
	     */
	  void edgeEvent(SweepContext tcx, Edge edge, P2TNode node){
		  tcx.edge_event.constrained_edge = edge;
		  tcx.edge_event.right = (edge.p.x > edge.q.x);

		  if (isEdgeSideOfTriangle(node.triangle, edge.p, edge.q)) {
		    return;
		  }

		  // For now we will do all needed filling
		  // TODO: integrate with flip process might give some better performance
		  //       but for now this avoid the issue with cases that needs both flips and fills
		  fillEdgeEvent(tcx, edge, node);
		  edgeEvent(tcx, edge.p, edge.q, node.triangle, edge.q);
	  }

	  void edgeEvent(SweepContext tcx, Point ep, Point eq, Triangle triangle, Point point){
		  if (isEdgeSideOfTriangle(triangle, ep, eq)) {
			    return;
			  }

			  Point p1 = triangle.pointCCW(point);
			  int o1 = Utils.orient2d(eq, p1, ep);
			  if (o1 == Utils.COLLINEAR) {
				  // ASSIMP_CHANGE (aramis_acg)
//				  throw new RuntimeException("EdgeEvent - collinear points not supported");
			    if( triangle.contains(eq, p1)) {
			      triangle.markConstrainedEdge(eq, p1 );
			      // We are modifying the constraint maybe it would be better to 
			      // not change the given constraint and just keep a variable for the new constraint
			      tcx.edge_event.constrained_edge.q = p1;
			      triangle = triangle.neighborAcross(point);
			      edgeEvent( tcx, ep, p1, triangle, p1 );
			    } else {
				  // ASSIMP_CHANGE (aramis_acg)
			    	throw new RuntimeException("EdgeEvent - collinear points not supported");
			    }
			    return;
			  }

			  Point p2 = triangle.pointCW(point);
			  int o2 = Utils.orient2d(eq, p2, ep);
			  if (o2 == Utils.COLLINEAR) {
				  // ASSIMP_CHANGE (aramis_acg)
//				  throw new RuntimeException("EdgeEvent - collinear points not supported");

			    if( triangle.contains(eq, p2)) {
			      triangle.markConstrainedEdge(eq, p2 );
			      // We are modifying the constraint maybe it would be better to 
			      // not change the given constraint and just keep a variable for the new constraint
			      tcx.edge_event.constrained_edge.q = p2;
			      triangle = triangle.neighborAcross(point);
			      edgeEvent( tcx, ep, p2, triangle, p2 );
			    } else {
			      // ASSIMP_CHANGE (aramis_acg)
			    	throw new RuntimeException("EdgeEvent - collinear points not supported");
			    }
			    return;
			  }

			  if (o1 == o2) {
			    // Need to decide if we are rotating CW or CCW to get to a triangle
			    // that will cross edge
			    if (o1 == Utils.CW) {
			      triangle = triangle.neighborCCW(point);
			    }       else{
			      triangle = triangle.neighborCW(point);
			    }
			    
			    edgeEvent(tcx, ep, eq, triangle, point);
			  } else {
			    // This triangle crosses constraint so lets flippin start!
			    flipEdgeEvent(tcx, ep, eq, triangle, point);
			  }
	  }

	  /**
	   * Creates a new front triangle and legalize it
	   * 
	   * @param tcx
	   * @param point
	   * @param node
	   * @return
	   */
	  P2TNode newFrontTriangle(SweepContext tcx, Point point, P2TNode node){
		  Triangle triangle = new Triangle(point, node.point, node.next.point);

		  triangle.markNeighbor(node.triangle);
		  tcx.addToMap(triangle);

		  P2TNode new_node = new P2TNode(point);
		  nodes_.add(new_node);

		  new_node.next = node.next;
		  new_node.prev = node;
		  node.next.prev = new_node;
		  node.next = new_node;

		  if (!legalize(tcx, triangle)) {
		    tcx.mapTriangleToNodes(triangle);
		  }

		  return new_node;
	  }

	  /**
	   * Adds a triangle to the advancing front to fill a hole.
	   * @param tcx
	   * @param node - middle node, that is the bottom of the hole
	   */
	  void fill(SweepContext tcx, P2TNode node){
		  Triangle triangle = new Triangle(node.prev.point, node.point, node.next.point);

		  // TODO: should copy the constrained_edge value from neighbor triangles
		  //       for now constrained_edge values are copied during the legalize
		  triangle.markNeighbor(node.prev.triangle);
		  triangle.markNeighbor(node.triangle);

		  tcx.addToMap(triangle);

		  // Update the advancing front
		  node.prev.next = node.next;
		  node.next.prev = node.prev;

		  // If it was legalized the triangle has already been mapped
		  if (!legalize(tcx, triangle)) {
		    tcx.mapTriangleToNodes(triangle);
		  }
	  }

	  /**
	   * Returns true if triangle was legalized
	   */
	  boolean legalize(SweepContext tcx, Triangle t){
		// To legalize a triangle we start by finding if any of the three edges
		  // violate the Delaunay condition
		  for (int i = 0; i < 3; i++) {
		    if (t.delaunay_edge[i])
		      continue;

		    Triangle ot = t.getNeighbor(i);

		    if (ot != null) {
		      Point p = t.getPoint(i);
		      Point op = ot.oppositePoint(t, p);
		      int oi = ot.index(op);

		      // If this is a Constrained Edge or a Delaunay Edge(only during recursive legalization)
		      // then we should not try to legalize
		      if (ot.constrained_edge[oi] || ot.delaunay_edge[oi]) {
		        t.constrained_edge[i] = ot.constrained_edge[oi];
		        continue;
		      }

		      boolean inside = incircle(p, t.pointCCW(p), t.pointCW(p), op);

		      if (inside) {
		        // Lets mark this shared edge as Delaunay
		        t.delaunay_edge[i] = true;
		        ot.delaunay_edge[oi] = true;

		        // Lets rotate shared edge one vertex CW to legalize it
		        rotateTrianglePair(t, p, ot, op);

		        // We now got one valid Delaunay Edge shared by two triangles
		        // This gives us 4 new edges to check for Delaunay

		        // Make sure that triangle to node mapping is done only one time for a specific triangle
		        boolean not_legalized = !legalize(tcx, t);
		        if (not_legalized) {
		          tcx.mapTriangleToNodes(t);
		        }

		        not_legalized = !legalize(tcx, ot);
		        if (not_legalized)
		          tcx.mapTriangleToNodes(ot);

		        // Reset the Delaunay edges, since they only are valid Delaunay edges
		        // until we add a new triangle or point.
		        // XXX: need to think about this. Can these edges be tried after we
		        //      return to previous recursive level?
		        t.delaunay_edge[i] = false;
		        ot.delaunay_edge[oi] = false;

		        // If triangle have been legalized no need to check the other edges since
		        // the recursive legalization will handles those so we can end here.
		        return true;
		      }
		    }
		  }
		  return false;
	  }

	  /**
	   * <b>Requirement</b>:<br>
	   * 1. a,b and c form a triangle.<br>
	   * 2. a and d is know to be on opposite side of bc<br>
	   * <pre>
	   *                a
	   *                +
	   *               / \
	   *              /   \
	   *            b/     \c
	   *            +-------+
	   *           /    d    \
	   *          /           \
	   * </pre>
	   * <b>Fact</b>: d has to be in area B to have a chance to be inside the circle formed by
	   *  a,b and c<br>
	   *  d is outside B if orient2d(a,b,d) or orient2d(c,a,d) is CW<br>
	   *  This preknowledge gives us a way to optimize the incircle test
	   * @param a - triangle point, opposite d
	   * @param b - triangle point
	   * @param c - triangle point
	   * @param d - point opposite a
	   * @return true if d is inside circle, false if on circle edge
	   */
	  boolean incircle(Point pa, Point pb, Point pc, Point pd){
		  double adx = pa.x - pd.x;
		  double ady = pa.y - pd.y;
		  double bdx = pb.x - pd.x;
		  double bdy = pb.y - pd.y;

		  double adxbdy = adx * bdy;
		  double bdxady = bdx * ady;
		  double oabd = adxbdy - bdxady;

		  if (oabd <= 0)
		    return false;

		  double cdx = pc.x - pd.x;
		  double cdy = pc.y - pd.y;

		  double cdxady = cdx * ady;
		  double adxcdy = adx * cdy;
		  double ocad = cdxady - adxcdy;

		  if (ocad <= 0)
		    return false;

		  double bdxcdy = bdx * cdy;
		  double cdxbdy = cdx * bdy;

		  double alift = adx * adx + ady * ady;
		  double blift = bdx * bdx + bdy * bdy;
		  double clift = cdx * cdx + cdy * cdy;

		  double det = alift * (bdxcdy - cdxbdy) + blift * ocad + clift * oabd;

		  return det > 0;
	  }

	  /**
	   * Rotates a triangle pair one vertex CW
	   *<pre>
	   *       n2                    n2
	   *  P +-----+             P +-----+
	   *    | t  /|               |\  t |
	   *    |   / |               | \   |
	   *  n1|  /  |n3           n1|  \  |n3
	   *    | /   |    after CW   |   \ |
	   *    |/ oT |               | oT \|
	   *    +-----+ oP            +-----+
	   *       n4                    n4
	   * </pre>
	   */
	  void rotateTrianglePair(Triangle t, Point p, Triangle ot, Point op){
		  Triangle n1, n2, n3, n4;
		  n1 = t.neighborCCW(p);
		  n2 = t.neighborCW(p);
		  n3 = ot.neighborCCW(op);
		  n4 = ot.neighborCW(op);

		  boolean ce1, ce2, ce3, ce4;
		  ce1 = t.getConstrainedEdgeCCW(p);
		  ce2 = t.getConstrainedEdgeCW(p);
		  ce3 = ot.getConstrainedEdgeCCW(op);
		  ce4 = ot.getConstrainedEdgeCW(op);

		  boolean de1, de2, de3, de4;
		  de1 = t.getDelunayEdgeCCW(p);
		  de2 = t.getDelunayEdgeCW(p);
		  de3 = ot.getDelunayEdgeCCW(op);
		  de4 = ot.getDelunayEdgeCW(op);

		  t.legalize(p, op);
		  ot.legalize(op, p);

		  // Remap delaunay_edge
		  ot.setDelunayEdgeCCW(p, de1);
		  t.setDelunayEdgeCW(p, de2);
		  t.setDelunayEdgeCCW(op, de3);
		  ot.setDelunayEdgeCW(op, de4);

		  // Remap constrained_edge
		  ot.setConstrainedEdgeCCW(p, ce1);
		  t.setConstrainedEdgeCW(p, ce2);
		  t.setConstrainedEdgeCCW(op, ce3);
		  ot.setConstrainedEdgeCW(op, ce4);

		  // Remap neighbors
		  // XXX: might optimize the markNeighbor by keeping track of
		  //      what side should be assigned to what neighbor after the
		  //      rotation. Now mark neighbor does lots of testing to find
		  //      the right side.
		  t.clearNeighbors();
		  ot.clearNeighbors();
		  if (n1 != null) ot.markNeighbor(n1);
		  if (n2 != null) t.markNeighbor(n2);
		  if (n3 != null) t.markNeighbor(n3);
		  if (n4 != null) ot.markNeighbor(n4);
		  t.markNeighbor(ot);
	  }

	  /**
	   * Fills holes in the Advancing Front
	   *
	   *
	   * @param tcx
	   * @param n
	   */
	  void fillAdvancingFront(SweepContext tcx, P2TNode n){
		// Fill right holes
		  P2TNode node = n.next;

		  while (node.next != null) {
		    double angle = holeAngle(node);
		    if (angle > M_PI_2 || angle < -M_PI_2) break;
		    fill(tcx, node);
		    node = node.next;
		  }

		  // Fill left holes
		  node = n.prev;

		  while (node.prev != null) {
		    double angle = holeAngle(node);
		    if (angle > M_PI_2 || angle < -M_PI_2) break;
		    fill(tcx, node);
		    node = node.prev;
		  }

		  // Fill right basins
		  if (n.next != null && n.next.next != null) {
		    double angle = basinAngle(n);
		    if (angle < Utils.PI_3div4) {
		      fillBasin(tcx, n);
		    }
		  }
	  }

	  /**
	   *
	   * @param node - middle node
	   * @return the angle between 3 front nodes
	   */
	  double holeAngle(P2TNode node){
		  /* Complex plane
		   * ab = cosA +i*sinA
		   * ab = (ax + ay*i)(bx + by*i) = (ax*bx + ay*by) + i(ax*by-ay*bx)
		   * atan2(y,x) computes the principal value of the argument function
		   * applied to the complex number x+iy
		   * Where x = ax*bx + ay*by
		   *       y = ax*by - ay*bx
		   */
		  double ax = node.next.point.x - node.point.x;
		  double ay = node.next.point.y - node.point.y;
		  double bx = node.prev.point.x - node.point.x;
		  double by = node.prev.point.y - node.point.y;
		  return Math.atan2(ax * by - ay * bx, ax * bx + ay * by);
	  }

	  /**
	   * The basin angle is decided against the horizontal line [1,0]
	   */
	  double basinAngle(P2TNode node){
		  double ax = node.point.x - node.next.next.point.x;
		  double ay = node.point.y - node.next.next.point.y;
		  return Math.atan2(ay, ax);
	  }

	  /**
	   * Fills a basin that has formed on the Advancing Front to the right
	   * of given node.<br>
	   * First we decide a left,bottom and right node that forms the
	   * boundaries of the basin. Then we do a reqursive fill.
	   *
	   * @param tcx
	   * @param node - starting node, this or next node will be left node
	   */
	  void fillBasin(SweepContext tcx, P2TNode node){
		  if (Utils.orient2d(node.point, node.next.point, node.next.next.point) == Utils.CCW) {
		    tcx.basin.left_node = node.next.next;
		  } else {
		    tcx.basin.left_node = node.next;
		  }

		  // Find the bottom and right node
		  tcx.basin.bottom_node = tcx.basin.left_node;
		  while (tcx.basin.bottom_node.next != null
		         && tcx.basin.bottom_node.point.y >= tcx.basin.bottom_node.next.point.y) {
		    tcx.basin.bottom_node = tcx.basin.bottom_node.next;
		  }
		  if (tcx.basin.bottom_node == tcx.basin.left_node) {
		    // No valid basin
		    return;
		  }

		  tcx.basin.right_node = tcx.basin.bottom_node;
		  while (tcx.basin.right_node.next != null
		         && tcx.basin.right_node.point.y < tcx.basin.right_node.next.point.y) {
		    tcx.basin.right_node = tcx.basin.right_node.next;
		  }
		  if (tcx.basin.right_node == tcx.basin.bottom_node) {
		    // No valid basins
		    return;
		  }

		  tcx.basin.width = tcx.basin.right_node.point.x - tcx.basin.left_node.point.x;
		  tcx.basin.left_highest = tcx.basin.left_node.point.y > tcx.basin.right_node.point.y;

		  fillBasinReq(tcx, tcx.basin.bottom_node);
	  }

	  /**
	   * Recursive algorithm to fill a Basin with triangles
	   *
	   * @param tcx
	   * @param node - bottom_node
	   * @param cnt - counter used to alternate on even and odd numbers
	   */
	  void fillBasinReq(SweepContext tcx, P2TNode node){
		// if shallow stop filling
		  if (isShallow(tcx, node)) {
		    return;
		  }

		  fill(tcx, node);

		  if (node.prev == tcx.basin.left_node && node.next == tcx.basin.right_node) {
		    return;
		  } else if (node.prev == tcx.basin.left_node) {
		    int o = Utils.orient2d(node.point, node.next.point, node.next.next.point);
		    if (o == Utils.CW) {
		      return;
		    }
		    node = node.next;
		  } else if (node.next == tcx.basin.right_node) {
		    int o = Utils.orient2d(node.point, node.prev.point, node.prev.prev.point);
		    if (o == Utils.CCW) {
		      return;
		    }
		    node = node.prev;
		  } else {
		    // Continue with the neighbor node with lowest Y value
		    if (node.prev.point.y < node.next.point.y) {
		      node = node.prev;
		    } else {
		      node = node.next;
		    }
		  }

		  fillBasinReq(tcx, node);
	  }

	  boolean isShallow(SweepContext tcx, P2TNode node){
		  double height;

		  if (tcx.basin.left_highest) {
		    height = tcx.basin.left_node.point.y - node.point.y;
		  } else {
		    height = tcx.basin.right_node.point.y - node.point.y;
		  }

		  // if shallow stop filling
		  if (tcx.basin.width > height) {
		    return true;
		  }
		  return false;
	  }

	  boolean isEdgeSideOfTriangle(Triangle triangle, Point ep, Point eq){
		  int index = triangle.edgeIndex(ep, eq);

		  if (index != -1) {
		    triangle.markConstrainedEdge(index);
		    Triangle t = triangle.getNeighbor(index);
		    if (t != null) {
		      t.markConstrainedEdge(ep, eq);
		    }
		    return true;
		  }
		  return false;
	  }

	  void fillEdgeEvent(SweepContext tcx, Edge edge, P2TNode node){
		  if (tcx.edge_event.right) {
		    fillRightAboveEdgeEvent(tcx, edge, node);
		  } else {
		    fillLeftAboveEdgeEvent(tcx, edge, node);
		  }
	  }

	  void fillRightAboveEdgeEvent(SweepContext tcx, Edge edge, P2TNode node){
		  while (node.next.point.x < edge.p.x) {
		    // Check if next node is below the edge
		    if (Utils.orient2d(edge.q, node.next.point, edge.p) == Utils.CCW) {
		      fillRightBelowEdgeEvent(tcx, edge, node);
		    } else {
		      node = node.next;
		    }
		  }
	  }

	  void fillRightBelowEdgeEvent(SweepContext tcx, Edge edge, P2TNode node){
		  if (node.point.x < edge.p.x) {
		    if (Utils.orient2d(node.point, node.next.point, node.next.next.point) == Utils.CCW) {
		      // Concave
		      fillRightConcaveEdgeEvent(tcx, edge, node);
		    } else{
		      // Convex
		      fillRightConvexEdgeEvent(tcx, edge, node);
		      // Retry this one
		      fillRightBelowEdgeEvent(tcx, edge, node);
		    }
		  }
	  }

	  void fillRightConcaveEdgeEvent(SweepContext tcx, Edge edge, P2TNode node){
		  fill(tcx, node.next);
		  if (node.next.point != edge.p) {
		    // Next above or below edge?
		    if (Utils.orient2d(edge.q, node.next.point, edge.p) == Utils.CCW) {
		      // Below
		      if (Utils.orient2d(node.point, node.next.point, node.next.next.point) == Utils.CCW) {
		        // Next is concave
		        fillRightConcaveEdgeEvent(tcx, edge, node);
		      } else {
		        // Next is convex
		      }
		    }
		  }
	  }

	  void fillRightConvexEdgeEvent(SweepContext tcx, Edge edge, P2TNode node){
		// Next concave or convex?
		  if (Utils.orient2d(node.next.point, node.next.next.point, node.next.next.next.point) == Utils.CCW) {
		    // Concave
		    fillRightConcaveEdgeEvent(tcx, edge, node.next);
		  } else{
		    // Convex
		    // Next above or below edge?
		    if (Utils.orient2d(edge.q, node.next.next.point, edge.p) == Utils.CCW) {
		      // Below
		      fillRightConvexEdgeEvent(tcx, edge, node.next);
		    } else{
		      // Above
		    }
		  }
	  }

	  void fillLeftAboveEdgeEvent(SweepContext tcx, Edge edge, P2TNode node){
		  while (node.prev.point.x > edge.p.x) {
		    // Check if next node is below the edge
		    if (Utils.orient2d(edge.q, node.prev.point, edge.p) == Utils.CW) {
		      fillLeftBelowEdgeEvent(tcx, edge, node);
		    } else {
		      node = node.prev;
		    }
		  }
	  }

	  void fillLeftBelowEdgeEvent(SweepContext tcx, Edge edge, P2TNode node){
		  if (node.point.x > edge.p.x) {
			    if (Utils.orient2d(node.point, node.prev.point, node.prev.prev.point) == Utils.CW) {
			      // Concave
			      fillLeftConcaveEdgeEvent(tcx, edge, node);
			    } else {
			      // Convex
			      fillLeftConvexEdgeEvent(tcx, edge, node);
			      // Retry this one
			      fillLeftBelowEdgeEvent(tcx, edge, node);
			    }
			  }
	  }

	  void fillLeftConcaveEdgeEvent(SweepContext tcx, Edge edge, P2TNode node){
		  fill(tcx, node.prev);
		  if (node.prev.point != edge.p) {
		    // Next above or below edge?
		    if (Utils.orient2d(edge.q, node.prev.point, edge.p) == Utils.CW) {
		      // Below
		      if (Utils.orient2d(node.point, node.prev.point, node.prev.prev.point) == Utils.CW) {
		        // Next is concave
		        fillLeftConcaveEdgeEvent(tcx, edge, node);
		      } else{
		        // Next is convex
		      }
		    }
		  }
	  }

	  void fillLeftConvexEdgeEvent(SweepContext tcx, Edge edge, P2TNode node){
		// Next concave or convex?
		  if (Utils.orient2d(node.prev.point, node.prev.prev.point, node.prev.prev.prev.point) == Utils.CW) {
		    // Concave
		    fillLeftConcaveEdgeEvent(tcx, edge, node.prev);
		  } else{
		    // Convex
		    // Next above or below edge?
		    if (Utils.orient2d(edge.q, node.prev.prev.point, edge.p) == Utils.CW) {
		      // Below
		      fillLeftConvexEdgeEvent(tcx, edge, node.prev);
		    } else{
		      // Above
		    }
		  }
	  }

	  void flipEdgeEvent(SweepContext tcx, Point ep, Point eq, Triangle t, Point p){
		  Triangle ot = t.neighborAcross(p);
		  Point op = ot.oppositePoint(t, p);

//		  if (ot == null) {
		    // If we want to integrate the fillEdgeEvent do it here
		    // With current implementation we should never get here
		    //throw new RuntimeException( "[BUG:FIXME] FLIP failed due to missing triangle");
//		    assert(0);
//			  assert false;
//		  }

		  if (Utils.inScanArea(p, t.pointCCW(p), t.pointCW(p), op)) {
		    // Lets rotate shared edge one vertex CW
		    rotateTrianglePair(t, p, ot, op);
		    tcx.mapTriangleToNodes(t);
		    tcx.mapTriangleToNodes(ot);

		    if (p == eq && op == ep) {
		      if (eq == tcx.edge_event.constrained_edge.q && ep == tcx.edge_event.constrained_edge.p) {
		        t.markConstrainedEdge(ep, eq);
		        ot.markConstrainedEdge(ep, eq);
		        legalize(tcx, t);
		        legalize(tcx, ot);
		      } else {
		        // XXX: I think one of the triangles should be legalized here?
		      }
		    } else {
		      int o = Utils.orient2d(eq, op, ep);
		      t = nextFlipTriangle(tcx, o, t, ot, p, op);
		      flipEdgeEvent(tcx, ep, eq, t, p);
		    }
		  } else {
		    Point newP = nextFlipPoint(ep, eq, ot, op);
		    flipScanEdgeEvent(tcx, ep, eq, t, ot, newP);
		    edgeEvent(tcx, ep, eq, t, p);
		  }
	  }

	  /**
	   * After a flip we have two triangles and know that only one will still be
	   * intersecting the edge. So decide which to contiune with and legalize the other
	   * 
	   * @param tcx
	   * @param o - should be the result of an orient2d( eq, op, ep )
	   * @param t - triangle 1
	   * @param ot - triangle 2
	   * @param p - a point shared by both triangles 
	   * @param op - another point shared by both triangles
	   * @return returns the triangle still intersecting the edge
	   */
	  Triangle nextFlipTriangle(SweepContext tcx, int o, Triangle  t, Triangle ot, Point p, Point op){
		  if (o == Utils.CCW) {
		    // ot is not crossing edge after flip
		    int edge_index = ot.edgeIndex(p, op);
		    ot.delaunay_edge[edge_index] = true;
		    legalize(tcx, ot);
		    ot.clearDelunayEdges();
		    return t;
		  }

		  // t is not crossing edge after flip
		  int edge_index = t.edgeIndex(p, op);

		  t.delaunay_edge[edge_index] = true;
		  legalize(tcx, t);
		  t.clearDelunayEdges();
		  return ot;
	  }

	   /**
	     * When we need to traverse from one triangle to the next we need 
	     * the point in current triangle that is the opposite point to the next
	     * triangle. 
	     * 
	     * @param ep
	     * @param eq
	     * @param ot
	     * @param op
	     * @return
	     */
	  Point nextFlipPoint(Point ep, Point eq, Triangle ot, Point op){
		  int o2d = Utils.orient2d(eq, op, ep);
		  if (o2d == Utils.CW) {
		    // Right
		    return ot.pointCCW(op);
		  } else if (o2d == Utils.CCW) {
		    // Left
		    return ot.pointCW(op);
		  } else{
		    throw new RuntimeException("[Unsupported] Opposing point on constrained edge");
			  // ASSIMP_CHANGE (aramis_acg)
//			  throw std::runtime_error("[Unsupported] Opposing point on constrained edge");
		  }
	  }

	   /**
	     * Scan part of the FlipScan algorithm<br>
	     * When a triangle pair isn't flippable we will scan for the next 
	     * point that is inside the flip triangle scan area. When found 
	     * we generate a new flipEdgeEvent
	     * 
	     * @param tcx
	     * @param ep - last point on the edge we are traversing
	     * @param eq - first point on the edge we are traversing
	     * @param flipTriangle - the current triangle sharing the point eq with edge
	     * @param t
	     * @param p
	     */
	  void flipScanEdgeEvent(SweepContext tcx, Point ep, Point eq, Triangle flip_triangle, Triangle t, Point p){
		  Triangle ot = t.neighborAcross(p);
		  Point op = ot.oppositePoint(t, p);

		  if (t.neighborAcross(p) == null) {
		    // If we want to integrate the fillEdgeEvent do it here
		    // With current implementation we should never get here
		    //throw new RuntimeException( "[BUG:FIXME] FLIP failed due to missing triangle");
//		    assert(0);
			  assert false;
		  }

		  if (Utils.inScanArea(eq, flip_triangle.pointCCW(eq), flip_triangle.pointCW(eq), op)) {
		    // flip with new edge op.eq
		    flipEdgeEvent(tcx, eq, op, ot, op);
		    // TODO: Actually I just figured out that it should be possible to
		    //       improve this by getting the next ot and op before the the above
		    //       flip and continue the flipScanEdgeEvent here
		    // set new ot and op here and loop back to inScanArea test
		    // also need to set a new flip_triangle first
		    // Turns out at first glance that this is somewhat complicated
		    // so it will have to wait.
		  } else{
		    Point newP = nextFlipPoint(ep, eq, ot, op);
		    flipScanEdgeEvent(tcx, ep, eq, flip_triangle, ot, newP);
		  }
	  }

	  void finalizationPolygon(SweepContext tcx){
		// Get an Internal triangle to start with
		  Triangle t = tcx.front().head().next.triangle;
		  Point p = tcx.front().head().next.point;
		  while (!t.getConstrainedEdgeCW(p)) {
		    t = t.neighborCCW(p);
		  }

		  // Collect interior triangles constrained by edges
		  tcx.meshClean(t);
	  }
}
