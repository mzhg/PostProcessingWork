package jet.opengl.postprocessing.util;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.WritableVector3f;

public final class Numeric {

	// -15 stored using a single precision bias of 127
	private static final int  HALF_FLOAT_MIN_BIASED_EXP_AS_SINGLE_FP_EXP = 0x38000000;
	// max exponent value in single precision that will be converted
	// to Inf or Nan when stored as a half-float
	private static final int  HALF_FLOAT_MAX_BIASED_EXP_AS_SINGLE_FP_EXP = 0x47800000;

	// 255 is the max exponent biased value
	private static final int  FLOAT_MAX_BIASED_EXP = (0xFF << 23);

	private static final int  HALF_FLOAT_MAX_BIASED_EXP = (0x1F << 10);
	
	public static final float PI = (float)Math.PI;
	public static final float EPSILON =1.e-6f;
	
	public static final int MAX_UBYTE = 255;
	public static final int MAX_USHORT = 65535;
	public static final long MAX_UINT = 4294967295l;
	
	public static final float[] EMPTY_FLOAT = new float[0];
	public static final int[]   EMPTY_INT = new int[0];
	public static final byte [] EMPTY_BYTE = new byte[0];
	public static final short[] EMPTY_SHORT = new short[0];
	public static final long[] EMPTY_LONG = new long[0];
	public static final double[] EMPTY_DOUBLE = new double[0];

	public static int unsignedByte(byte b) {
		return b & 0xFF;
	}

	public static int unsignedShort(short s) {
		return s & 0xFFFF;
	}

	public static int unsignedChar(char c) {
		return c & 0xFFFF;
	}

	public static long unsignedInt(int i) {
		long l = i;
		l = l & (0xFFFFFFFFL);
		return l;
	}
	
	public static byte[] toBytes(int x, byte[] out) {
		byte[] intBytes = out == null ? new byte[4] : out; 
		intBytes[3] = (byte) (x >> 24);
		intBytes[2] = (byte) (x >> 16);
		intBytes[1] = (byte) (x >> 8);
		intBytes[0] = (byte) (x >> 0);

		return intBytes;
	}
	public static void toBytes(int[] src, int srcOffset, byte[] dst, int dstOffset, int length){
		if(length < 0)
			throw new IllegalArgumentException("length must be >= 0, length = " + length);
		byte[] intBytes = null;

		for(int i = srcOffset; i < srcOffset + length; i++){
			intBytes = toBytes(src[i], intBytes);

			dst[dstOffset ++] = intBytes[0];
			dst[dstOffset ++] = intBytes[1];
			dst[dstOffset ++] = intBytes[2];
			dst[dstOffset ++] = intBytes[3];
		}
	}

	public static void toInts(byte[] src, int srcOffset, int[] dst, int dstOffset, int length){
		if(length < 0)
			throw new IllegalArgumentException("length must be >= 0, length = " + length);

		for(int i = dstOffset; i < dstOffset + length; i++){
			dst[i] = getInt(src, srcOffset);
			srcOffset += 4;
		}
	}

	public static boolean almostZero(float f){
		return Math.abs(f) < EPSILON;
	}

	public static boolean isClose(float a, float b, float percent){
		if(a == b){
			return true;
		}

		if(a != a && b != b){
			return true;
		}

		if(Math.abs(a) < EPSILON && Math.abs(b) < EPSILON){
			return true;
		}

		float diffAB = a - b;
		float diffBA = b - a;
		percent = Math.max(percent, EPSILON);
		return Math.abs(diffAB/ b) < percent && Math.abs(diffBA/a) < percent;
	}

	public static final int makeRGBA(int r, int g, int b, int a) {
		return (r << 0) | (g << 8) | (b << 16) | (a << 24);
	}
	
	public static final int readInt(byte[] data, int position){
		int a = data[position + 0] & 255;
		int b = data[position + 1] & 255;
		int c = data[position + 2] & 255;
		int d = data[position + 3] & 255;
		
		return makeRGBA(a, b, c, d);
	}

	public static final float random() { return (float)Math.random();}
	
	public static final float random(float low, float high){
		return (float) (Math.random() * (high - low) + low);
	}
	
	public static final int getInt(byte[] data, int position) {
		int a = data[position + 0] & 255;
		int b = data[position + 1] & 255;
		int c = data[position + 2] & 255;
		int d = data[position + 3] & 255;

		return makeRGBA(a, b, c, d);
	}
	
	public static final int getIntBE(byte[] data, int position) {
		int a = data[position + 0] & 255;
		int b = data[position + 1] & 255;
		int c = data[position + 2] & 255;
		int d = data[position + 3] & 255;

		return makeRGBA(d, c, b, a);
	}
	
	public static final long getLong(byte[] data, int position) {
		long a = data[position + 0] & 255;
		long b = data[position + 1] & 255;
		long c = data[position + 2] & 255;
		long d = data[position + 3] & 255;
		long e = data[position + 4] & 255;
		long f = data[position + 5] & 255;
		long g = data[position + 6] & 255;
		long h = data[position + 7] & 255;

		return a | (b << 8) | (c << 16) | (d << 24) | (e << 32) | (f << 40) | (g << 48) | (h << 56);
	}
	
	public static final long getLongBE(byte[] data, int position) {
		long a = data[position + 7] & 255;
		long b = data[position + 6] & 255;
		long c = data[position + 5] & 255;
		long d = data[position + 4] & 255;
		long e = data[position + 3] & 255;
		long f = data[position + 2] & 255;
		long g = data[position + 1] & 255;
		long h = data[position + 0] & 255;

		return a | (b << 8) | (c << 16) | (d << 24) | (e << 32) | (f << 40) | (g << 48) | (h << 56);
	}
	
	public static final double getDouble(byte[] data, int position){
		return Double.longBitsToDouble(getLong(data, 0));
	}
	
	public static final double getDoubleBE(byte[] data, int position){
		return Double.longBitsToDouble(getLongBE(data, 0));
	}
	
	public static final int getBytes(int i, byte[] out, int position) {
		out[position++] = (byte) i;
		out[position++] = (byte) (i >> 8);
		out[position++] = (byte) (i >> 16);
		out[position++] = (byte) (i >> 24);
		return position;
	}
	
	public static final int getBytes(short s, byte[] out, int position) {
		out[position++] = (byte) s;
		out[position++] = (byte) (s >> 8);
		return position;
	}

	public static final int getBytes(short[] s, byte[] out, int position) {
		for(int i = 0; i < s.length; i++){
			position = getBytes(s[i], out, position);
		}
		return position;
	}
	
	public static final int getBytes(int[] a, byte[] out, int position) {
		for(int i = 0; i < a.length; i++){
			position = getBytes(a[i], out, position);
		}
		
		return position;
	}
	
	public static final int getBytes(float f, byte[] out, int position){
		return getBytes(Float.floatToRawIntBits(f), out, position);
	}
	
	public static final int getBytes(long l, byte[] out, int position){
		out[position++] = (byte) l;
		out[position++] = (byte) (l >> 8);
		out[position++] = (byte) (l >> 16);
		out[position++] = (byte) (l >> 24);
		out[position++] = (byte) (l >> 32);
		out[position++] = (byte) (l >> 40);
		out[position++] = (byte) (l >> 48);
		out[position++] = (byte) (l >> 56);
		return position;
	}
	
	public static final int getBytes(double d, byte[] out, int position){
		return getBytes(Double.doubleToLongBits(d), out, position);
	}
	
	public static final int getBytes(float[] a, byte[] out, int position) {
		for(int i = 0; i < a.length; i++){
			position = getBytes(a[i], out, position);
		}
		
		return position;
	}
	
	public static final short getShort(byte[] data, int position) {
		int a = data[position + 0] & 255;
		int b = data[position + 1] & 255;

		return (short) (a | (b << 8));
	}
	
	public static final short getShortBE(byte[] data, int position) {
		int a = data[position + 1] & 255;
		int b = data[position + 0] & 255;

		return (short) (a | (b << 8));
	}
	
	public static final float getFloat(byte[] data, int position){
		return Float.intBitsToFloat(getInt(data, position));
	}
	
	public static final float getFloatBE(byte[] data, int position){
		return Float.intBitsToFloat(getIntBE(data, position));
	}
	
	private Numeric(){}

	public static void swap(byte[] a, int i, int j) {
		byte tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}

	public static void swap(short[] a, int i, int j) {
		short tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}
	
	public static void swap(int[] a, int i, int j) {
		int tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}
	
	public static void swap(float[] a, int i, int j) {
		float tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}
	
	/**
     * Return the exponent of a float number, removing the bias.
     * <p>
     * For float numbers of the form 2<sup>x</sup>, the unbiased
     * exponent is exactly x.
     * </p>
     * @param f number from which exponent is requested
     * @return exponent for d in IEEE754 representation, without bias
     */
    public static int getExponent(final float f) {
        return ((Float.floatToIntBits(f) >>> 23) & 0xff) - 127;
    }
    
    /**
     * Multiply a double number by a power of 2.
     * @param d number to multiply
     * @param n power of 2
     * @return d &times; 2<sup>n</sup>
     */
    public static double ldexp(final double d, final int n) {

        // first simple and fast handling when 2^n can be represented using normal numbers
        if ((n > -1023) && (n < 1024)) {
            return d * Double.longBitsToDouble(((long) (n + 1023)) << 52);
        }

        // handle special cases
        if (Double.isNaN(d) || Double.isInfinite(d) || (d == 0)) {
            return d;
        }
        if (n < -2098) {
            return (d > 0) ? 0.0 : -0.0;
        }
        if (n > 2097) {
            return (d > 0) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }

        // decompose d
        final long bits = Double.doubleToLongBits(d);
        final long sign = bits & 0x8000000000000000L;
        int  exponent   = ((int) (bits >>> 52)) & 0x7ff;
        long mantissa   = bits & 0x000fffffffffffffL;

        // compute scaled exponent
        int scaledExponent = exponent + n;

        if (n < 0) {
            // we are really in the case n <= -1023
            if (scaledExponent > 0) {
                // both the input and the result are normal numbers, we only adjust the exponent
                return Double.longBitsToDouble(sign | (((long) scaledExponent) << 52) | mantissa);
            } else if (scaledExponent > -53) {
                // the input is a normal number and the result is a subnormal number

                // recover the hidden mantissa bit
                mantissa = mantissa | (1L << 52);

                // scales down complete mantissa, hence losing least significant bits
                final long mostSignificantLostBit = mantissa & (1L << (-scaledExponent));
                mantissa = mantissa >>> (1 - scaledExponent);
                if (mostSignificantLostBit != 0) {
                    // we need to add 1 bit to round up the result
                    mantissa++;
                }
                return Double.longBitsToDouble(sign | mantissa);

            } else {
                // no need to compute the mantissa, the number scales down to 0
                return (sign == 0L) ? 0.0 : -0.0;
            }
        } else {
            // we are really in the case n >= 1024
            if (exponent == 0) {

                // the input number is subnormal, normalize it
                while ((mantissa >>> 52) != 1) {
                    mantissa = mantissa << 1;
                    --scaledExponent;
                }
                ++scaledExponent;
                mantissa = mantissa & 0x000fffffffffffffL;

                if (scaledExponent < 2047) {
                    return Double.longBitsToDouble(sign | (((long) scaledExponent) << 52) | mantissa);
                } else {
                    return (sign == 0L) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                }

            } else if (scaledExponent < 2047) {
                return Double.longBitsToDouble(sign | (((long) scaledExponent) << 52) | mantissa);
            } else {
                return (sign == 0L) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }
        }

    }

    /**
     * Multiply a float number by a power of 2.
     * @param f number to multiply
     * @param n power of 2
     * @return f &times; 2<sup>n</sup>
     */
    public static float ldexp(final float f, final int n) {

        // first simple and fast handling when 2^n can be represented using normal numbers
        if ((n > -127) && (n < 128)) {
            return f * Float.intBitsToFloat((n + 127) << 23);
        }

        // handle special cases
        if (Float.isNaN(f) || Float.isInfinite(f) || (f == 0f)) {
            return f;
        }
        if (n < -277) {
            return (f > 0) ? 0.0f : -0.0f;
        }
        if (n > 276) {
            return (f > 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        }

        // decompose f
        final int bits = Float.floatToIntBits(f);
        final int sign = bits & 0x80000000;
        int  exponent  = (bits >>> 23) & 0xff;
        int mantissa   = bits & 0x007fffff;

        // compute scaled exponent
        int scaledExponent = exponent + n;

        if (n < 0) {
            // we are really in the case n <= -127
            if (scaledExponent > 0) {
                // both the input and the result are normal numbers, we only adjust the exponent
                return Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa);
            } else if (scaledExponent > -24) {
                // the input is a normal number and the result is a subnormal number

                // recover the hidden mantissa bit
                mantissa = mantissa | (1 << 23);

                // scales down complete mantissa, hence losing least significant bits
                final int mostSignificantLostBit = mantissa & (1 << (-scaledExponent));
                mantissa = mantissa >>> (1 - scaledExponent);
                if (mostSignificantLostBit != 0) {
                    // we need to add 1 bit to round up the result
                    mantissa++;
                }
                return Float.intBitsToFloat(sign | mantissa);

            } else {
                // no need to compute the mantissa, the number scales down to 0
                return (sign == 0) ? 0.0f : -0.0f;
            }
        } else {
            // we are really in the case n >= 128
            if (exponent == 0) {

                // the input number is subnormal, normalize it
                while ((mantissa >>> 23) != 1) {
                    mantissa = mantissa << 1;
                    --scaledExponent;
                }
                ++scaledExponent;
                mantissa = mantissa & 0x007fffff;

                if (scaledExponent < 255) {
                    return Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa);
                } else {
                    return (sign == 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
                }

            } else if (scaledExponent < 255) {
                return Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa);
            } else {
                return (sign == 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
            }
        }

    }
    
    /**
	 * 
	 * Constrains a value to not exceed a maximum and minimum value.
	 * 
	 * @param amt
	 *            the value to constrain
	 * @param low
	 *            minimum limit
	 * @param high
	 *            maximum limit
	 */

	static public final float clamp(float amt, float low, float high) {
		return (amt < low) ? low : ((amt > high) ? high : amt);
	}
	
	/**
	 * 
	 * Constrains a value to not exceed a maximum and minimum value.
	 * 
	 * @param amt
	 *            the value to constrain
	 * @param low
	 *            minimum limit
	 * @param high
	 *            maximum limit
	 */

	static public final int clamp(int amt, int low, int high) {
		return (amt < low) ? low : ((amt > high) ? high : amt);
	}
	
	/**
	 * Calculates a number between two numbers at a specific increment. The
	 * <b>amt</b> parameter is the amount to interpolate between the two values
	 * where 0.0 equal to the first point, 0.1 is very near the first point, 0.5
	 * is half-way in between, etc. The lerp function is convenient for creating
	 * motion along a straight path and for drawing dotted lines.
	 * 
	 * @param start
	 *            first value
	 * @param stop
	 *            second value
	 * @param amt
	 *            float between 0.0 and 1.0
	 */
	public static final float mix(float start, float stop, float amt) {
		return start + (stop - start) * amt;
	}

	/** A predefined constant for WHITE. */
	public static final int NV_PC_PREDEF_WHITE = 0xFFFFFFFF;
	/** A predefined constant for BLACK. */
	public static final int NV_PC_PREDEF_BLACK = 0xFF000000;

	/** Extracting the red value from an packed color which is in rgb(a) form. */
	public static final int getRedFromRGB(int rgb){
		return rgb & 0xFF;
	}

	/** Extracting the red value from an packed color which is in bgr(a) form. */
	public static final int getRedFromBGR(int bgr){
		return (bgr >> 16) & 0xFF;
	}

	/** Extracting the green value from an packed color no matter in rgb(a) or bgr(a) form. */
	public static final int getGreen(int rgb){
		return (rgb >> 8) & 0xFF;
	}

	/** Extracting the blue value from an packed color which is in rgb(a) form. */
	public static final int getBlueFromRGB(int rgb){
		return (rgb >> 16) & 0xFF;
	}

	/** Extracting the blue value from an packed color which is in bgr(a) form. */
	public static final int getBlueFromBGR(int bgr){
		return bgr & 0xFF;
	}

	/** Extracting the alpha value from an packed colorno matter in rgb(a) or bgr(a) form. */
	public static final int getAlpha(int rgba){
		return (rgba >> 24) & 0xFF;
	}

	/** Extracting the red value as a 0..1 float from an packed color which is in rgb(a) form. */
	public static final float getRedFromRGBf(int rgb){
		return (rgb & 0xFF)/255f;
	}

	/** Extracting the red value as a 0..1 float from an packed color which is in bgr(a) form. */
	public static final float getRedFromBGRf(int bgr){
		return ((bgr >> 16) & 0xFF)/255f;
	}

	/** Extracting the green value as a 0..1 float from an packed color no matter in rgb(a) or bgr(a) form. */
	public static final float getGreenf(int rgb){
		return ((rgb >> 8) & 0xFF)/255f;
	}

	/** Extracting the blue value as a 0..1 float from an packed color which is in rgb(a) form. */
	public static final float getBlueFromRGBf(int rgb){
		return ((rgb >> 16) & 0xFF)/255f;
	}

	/** Extracting the blue value as a 0..1 float from an packed color which is in bgr(a) form. */
	public static final float getBlueFromBGRf(int bgr){
		return (bgr & 0xFF)/255f;
	}

	/** Extracting the alpha value as a 0..1 float from an packed colorno matter in rgb(a) or bgr(a) form. */
	public static final float getAlphaf(int rgba){
		return ((rgba >> 24) & 0xFF)/255f;
	}

	/** Setting just the alpha value of the color, leaving the rest intact. */
	public static final int setAlpha(int c, int a){
		// this algorithm may be not right
		return ( ((c)&0xFFFFFF) | ((((a))&0xFF)<<24) );
	}

	/** Divides two integers and rounds up */
	public static int divideAndRoundUp(int dividend, int divisor) {
		return (dividend + divisor - 1) / divisor;
	}

	public static int encode(short first, short second){
		return (second << 16) | (first & 0xFFFF);
	}

	public static int decodeFirst(int value){
		return value & 0xFFFF;
	}

	public static int decodeSecond(int value){
		return (value >> 16) & 0xFFFF;
	}

	public static long encode(int first, int second){
		long s = second;
		long f = first;
		return (s << 32) | (f & 0xFFFFFFFFl);
	}

	public static int decodeFirst(long value){
		return (int) (value & 0xFFFFFFFFl);
	}

	public static int decodeSecond(long value){
		return (int) ((value >> 32) & 0xFFFFFFFFl);
	}

	public static boolean testRayIntersectWithTriangle(ReadableVector3f orig, ReadableVector3f dir,
													   ReadableVector3f v0, ReadableVector3f v1, ReadableVector3f v2,
													   WritableVector3f tuv){
		// Find vectors for two edges sharing vert0
		Vector3f edge1 = Vector3f.sub(v1, v0, null);
		Vector3f edge2 = Vector3f.sub(v2, v0, null);

		// Begin calculating determinant - also used to calculate U parameter
		Vector3f pvec = Vector3f.cross(dir, edge2, null);
	//	D3DXVec3Cross(&pvec, &dir, &edge2);

		// If determinant is near zero, ray lies in plane of triangle
		float det = Vector3f.dot(edge1, pvec);

		Vector3f tvec;
		if (det > 0)
		{
			tvec = Vector3f.sub(orig, v0, null);
		}
		else
		{
			tvec = Vector3f.sub(v0, orig, null);
			det = -det;
		}

		if (det < 0.0001f)
			return false;

		// Calculate U parameter and test bounds
		float u = Vector3f.dot(tvec, pvec);
		if (u < 0.0f || u > det)
		return false;

		// Prepare to test V parameter
		Vector3f qvec = Vector3f.cross(tvec, edge1, pvec);

		// Calculate V parameter and test bounds
		float v = Vector3f.dot(dir, qvec);
		if (v < 0.0f || u + v > det)
			return false;

		if(tuv != null){
			// Calculate t, scale parameters, ray intersects triangle
			float t = Vector3f.dot(edge2, qvec);
			float fInvDet = 1.0f / det;
			t *= fInvDet;
			u *= fInvDet;
			v *= fInvDet;

			tuv.set(t,u,v);
		}

		return true;
	}

	public static float convertHFloatToFloat(short hf)
	{
		int    sign = (hf >> 15);
		int    mantissa = (hf & ((1 << 10) - 1));
		int    exp = (hf & HALF_FLOAT_MAX_BIASED_EXP);
		int    f;

		if (exp == HALF_FLOAT_MAX_BIASED_EXP)
		{
			// we have a half-float NaN or Inf
			// half-float NaNs will be converted to a single precision NaN
			// half-float Infs will be converted to a single precision Inf
			exp = FLOAT_MAX_BIASED_EXP;
			if (mantissa != 0)
				mantissa = (1 << 23) - 1;    // set all bits to indicate a NaN
		}
		else if (exp == 0x0)
		{
			// convert half-float zero/denorm to single precision value
			if (mantissa != 0)
			{
				mantissa <<= 1;
				exp = HALF_FLOAT_MIN_BIASED_EXP_AS_SINGLE_FP_EXP;
				// check for leading 1 in denorm mantissa
				while ((mantissa & (1 << 10)) == 0)
				{
					// for every leading 0, decrement single precision exponent by 1
					// and shift half-float mantissa value to the left
					mantissa <<= 1;
					exp -= (1 << 23);
				}
				// clamp the mantissa to 10-bits
				mantissa &= ((1 << 10) - 1);
				// shift left to generate single-precision mantissa of 23-bits
				mantissa <<= 13;
			}
		}
		else
		{
			// shift left to generate single-precision mantissa of 23-bits
			mantissa <<= 13;
			// generate single precision biased exponent value
			exp = (exp << 13) + HALF_FLOAT_MIN_BIASED_EXP_AS_SINGLE_FP_EXP;
		}

		f = (sign << 31) | exp | mantissa;

		return Float.intBitsToFloat(f);
	}

	/**
	 * Convert the 32-FP to 16-FP.
	 * @param f
	 * @return
     */
	public static short convertFloatToHFloat(float f)
	{
		int i = Float.floatToRawIntBits(f);
		int s =  (i >> 16) & 0x00008000;
		int e = ((i >> 23) & 0x000000ff) - (127 - 15);
		int m =   i        & 0x007fffff;

		//
		// Now reassemble s, e and m into a half:
		//

		if (e <= 0)
		{
			if (e < -10)
			{
				//
				// E is less than -10.  The absolute value of f is
				// less than HALF_MIN (f may be a small normalized
				// float, a denormalized float or a zero).
				//
				// We convert f to a half zero with the same sign as f.
				//

//		        return *((hfloat *)&s);
				return (short)s;
			}

			//
			// E is between -10 and 0.  F is a normalized float
			// whose magnitude is less than HALF_NRM_MIN.
			//
			// We convert f to a denormalized half.
			//

			//
			// Add an explicit leading 1 to the significand.
			//

			m = m | 0x00800000;

			//
			// Round to m to the nearest (10+e)-bit value (with e between
			// -10 and 0); in case of a tie, round to the nearest even value.
			//
			// Rounding may cause the significand to overflow and make
			// our number normalized.  Because of the way a half's bits
			// are laid out, we don't have to treat this case separately;
			// the code below will handle it correctly.
			//

			int t = 14 - e;
			int a = (1 << (t - 1)) - 1;
			int b = (m >> t) & 1;

			m = (m + a + b) >> t;

			//
			// Assemble the half from s, e (zero) and m.
			//

			int r = s | m;
//		    return *((hfloat *)&r);
			return (short)r;
		}
		else if (e == 0xff - (127 - 15))
		{
			if (m == 0)
			{
				//
				// F is an infinity; convert f to a half
				// infinity with the same sign as f.
				//

				int r = s | 0x7c00;
//		        return *((hfloat *)&r);
				return (short)r;
			}
			else
			{
				//
				// F is a NAN; we produce a half NAN that preserves
				// the sign bit and the 10 leftmost bits of the
				// significand of f, with one exception: If the 10
				// leftmost bits are all zero, the NAN would turn
				// into an infinity, so we have to set at least one
				// bit in the significand.
				//

				m >>= 13;
				int r = s | 0x7c00 | m | ((m == 0) ? 1 : 0);
//		        return *((hfloat *)&r);
				return (short)r;
			}
		}
		else
		{
			//
			// E is greater than zero.  F is a normalized float.
			// We try to convert f to a normalized half.
			//

			//
			// Round to m to the nearest 10-bit value.  In case of
			// a tie, round to the nearest even value.
			//

			m = m + 0x00000fff + ((m >> 13) & 1);

			if ((m & 0x00800000) != 0)
			{
				m =  0;		// overflow in significand,
				e += 1;		// adjust exponent
			}

			//
			// Handle exponent overflow
			//

			if (e > 30)
			{
//		        overflow ();	// Cause a hardware floating point overflow;
				int r = s | 0x7c00; // if this returns, the half becomes an
//		        return *((hfloat *)&r); // infinity with the same sign as f.
				return (short)r;
			}

			//
			// Assemble the half from s, e and m.
			//

			{
				int r = s | (e << 10) | (m >> 13);
//		        return *((hfloat *)&r);
				return (short)r;
			}
		}
	}

	public static float fresnelTerm(float cos_theta, float refractionIndex) {
		float r = refractionIndex;
		float c = cos_theta;
		double g = Math.sqrt(r * r + c * c - 1);

		double g_c = g - c;
		double gc = g+c;
		double t1 = c * gc - 1;
		double t2 = c * g_c + 1;
		return (float) (0.5 * (g_c * g_c)/ (gc * gc) * (1 + (t1 * t1)/ (t2 * t2)));
	}
}
