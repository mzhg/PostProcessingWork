package poly2tri.sweep;

final class Basin {

	P2TNode left_node;
	  P2TNode bottom_node;
	  P2TNode right_node;
	  double width;
	  boolean left_highest;

//	  Basin() : left_node(NULL), bottom_node(NULL), right_node(NULL), width(0.0), left_highest(false)
//	  {
//	  }

	  void clear()
	  {
	    left_node = null;
	    bottom_node = null;
	    right_node = null;
	    width = 0.0;
	    left_highest = false;
	  }
}
