package poly2tri.common;

import java.util.ArrayList;

public class Point {

	public double x;
	public double y;
	/** The edges this point constitutes an upper ending point */
	public final ArrayList<Edge> edge_list = new ArrayList<Edge>();
	
	public Point() {
	}

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	/// Set this point to all zeros.
	public void set_zero()
	  {
	    x = 0.0;
	    y = 0.0;
	  }

	  /// Set this point to some specified coordinates.
	public void set(double x_, double y_)
	  {
	    x = x_;
	    y = y_;
	  }
	
	public void set(Point p){
		x = p.x;
		y = p.y;
	}

	  /// Negate this point.
	 public Point nega()
	  {
	    Point v = new Point(-x, -y);
	    return v;
	  }

	  /// Add a point to this point.
	  public void add(Point v)
	  {
	    x += v.x;
	    y += v.y;
	  }

	  /// Subtract a point from this point.
	  public void sub(Point v)
	  {
	    x -= v.x;
	    y -= v.y;
	  }

	  /// Multiply this point by a scalar.
	  public void mul(double a)
	  {
	    x *= a;
	    y *= a;
	  }

	  /// Get the length of this point (the norm).
	  public double length()
	  {
	    return Math.sqrt(x * x + y * y);
	  }

	  /// Convert this point into a unit point. Returns the Length.
	  public double normalize()
	  {
	    double len = length();
	    x /= len;
	    y /= len;
	    return len;
	  }
	
	  /** Add two points_ component-wise. */
	  public static Point add(Point a, Point b, Point out){
		  if(out == null)
			  out = new Point(a.x + b.x, a.y + b.y);
		  else
			  out.set(a.x + b.x, a.y + b.y);
		  return out;
	  }
	  
	  /** Subtract two points_ component-wise. */
	  public static Point sub(Point a, Point b, Point out){
		  if(out == null)
			  out = new Point(a.x - b.x, a.y - b.y);
		  else
			  out.set(a.x - b.x, a.y - b.y);
		  return out;
	  }
	  
	  /** Multiply point by scalar. */
	  public static Point mul(double s, Point b, Point out){
		  if(out == null)
			  out = new Point(s * b.x, s * b.y);
		  else
			  out.set(s * b.x, s * b.y);
		  return out;
	  }

	  /* No need this
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
		Point other = (Point) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}*/
	
	public boolean equals(Point b) {
		return x == b.x && y == b.y;
	}
	
	/// Peform the dot product on two vectors.
	public static double dot(Point a, Point b)
	{
	  return a.x * b.x + a.y * b.y;
	}

	/// Perform the cross product on two vectors. In 2D this produces a scalar.
	public static double cross(Point a, Point b)
	{
	  return a.x * b.y - a.y * b.x;
	}

	/// Perform the cross product on a point and a scalar. In 2D this produces
	/// a point.
	public static Point Cross(Point a, double s, Point out)
	{
		if(out == null)
	  	  	out = new Point(s * a.y, -s * a.x);
		else
			out.set(s * a.y, -s * a.x);
		return out;
	}

	/// Perform the cross product on a scalar and a point. In 2D this produces
	/// a point.
	public static Point cross(double s, Point a, Point out)
	{
//	  return Point(-s * a.y, s * a.x);
		if(out == null)
	  	  	out = new Point(-s * a.y, s * a.x);
		else
			out.set(-s * a.y, s * a.x);
		return out;
	}
	  
}
