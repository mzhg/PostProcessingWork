package poly2tri.common;

public final class Utils {
	
	public static final double PI_3div4 = 3 * Math.PI / 4;
	public static final double EPSILON = 1e-15;
	
	public static final int CW = 0;
	public static final int CCW = 1;
	public static final int COLLINEAR = 2;

	private Utils(){}
	
	/**
	 * Forumla to calculate signed area<br>
	 * Positive if CCW<br>
	 * Negative if CW<br>
	 * 0 if collinear<br>
	 * <pre>
	 * A[P1,P2,P3]  =  (x1*y2 - y1*x2) + (x2*y3 - y2*x3) + (x3*y1 - y3*x1)
	 *              =  (x1-x3)*(y2-y3) - (y1-y3)*(x2-x3)
	 * </pre>
	 */
	public static final int orient2d(Point pa, Point pb, Point pc)
	{
	  double detleft = (pa.x - pc.x) * (pb.y - pc.y);
	  double detright = (pa.y - pc.y) * (pb.x - pc.x);
	  double val = detleft - detright;
	  if (val > -EPSILON && val < EPSILON) {
	    return COLLINEAR;
	  } else if (val > 0) {
	    return CCW;
	  }
	  return CW;
	}

	public static final boolean inScanArea(Point pa, Point pb, Point pc, Point pd)
	{
	  double pdx = pd.x;
	  double pdy = pd.y;
	  double adx = pa.x - pdx;
	  double ady = pa.y - pdy;
	  double bdx = pb.x - pdx;
	  double bdy = pb.y - pdy;

	  double adxbdy = adx * bdy;
	  double bdxady = bdx * ady;
	  double oabd = adxbdy - bdxady;

	  if (oabd <= EPSILON) {
	    return false;
	  }

	  double cdx = pc.x - pdx;
	  double cdy = pc.y - pdy;

	  double cdxady = cdx * ady;
	  double adxcdy = adx * cdy;
	  double ocad = cdxady - adxcdy;

	  if (ocad <= EPSILON) {
	    return false;
	  }

	  return true;
	}
	
}
