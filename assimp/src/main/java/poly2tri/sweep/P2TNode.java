package poly2tri.sweep;

import poly2tri.common.Point;
import poly2tri.common.Triangle;

/** Advancing front node */
public class P2TNode {

	Point point;
	Triangle triangle;
	P2TNode next;
	P2TNode prev;
	
	double value;
	
	public P2TNode(Point p) {
		point = p;
		value = p.x;
	}
	
	public P2TNode(Point p, Triangle t){
		point = p;
		triangle = t;
		value = p.x;
	}
}
