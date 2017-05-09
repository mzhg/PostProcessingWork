package jet.opengl.postprocessing.util;

public final class Numeric {
	
	public static final float PI = (float)Math.PI;
	
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
}
