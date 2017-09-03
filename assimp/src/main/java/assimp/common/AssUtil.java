package assimp.common;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public final class AssUtil {
	
	static final double[] fast_atof_table =	{
			0.0,
			0.1,
			0.01,
			0.001,
			0.0001,
			0.00001,
			0.000001,
			0.0000001,
			0.00000001,
			0.000000001,
			0.0000000001,
			0.00000000001,
			0.000000000001,
			0.0000000000001,
			0.00000000000001,
			0.000000000000001,
			0
		};
	
	public static final int SIZE_OF_INT = 4;
	public static final int SIZE_OF_FLOAT = 4;
	public static final int SIZE_OF_BYTE = 1;
	public static final int SIZE_OF_SHORT = 2;
	public static final int SIZE_OF_LONG = 8;
	public static final int SIZE_OF_VEC2 = 8;
	public static final int SIZE_OF_VEC3 = 12;
	public static final int SIZE_OF_VEC4 = 16;
	
	public static final float[] EMPTY_FLOAT = new float[0];
	public static final int[] EMPTY_INT = new int[0];
	public static final long[] EMPTY_LONG = new long[0];
	
	private AssUtil(){}

	public static final int makeMagic(String string){
		return makefourcc(string.charAt(3), string.charAt(2), string.charAt(1), string.charAt(0));
	}
	
	public static final int makefourcc(int c0, int c1, int c2, int c3){
		return c0 | (c1 << 8) | (c2 << 16) | (c3 << 24);
	}
	
	/** Convert a R8G8B8A8 color(LE) into floating-point format.*/
	public static final Vector4f convertColor(int color, Vector4f out){
		float r=  (color & 0xFF)/255.0f;
		float g=  ((color >> 8) & 0xFF)/255.0f;
		float b = ((color >> 16) & 0xFF)/255.0f;
		float a = ((color >> 24) & 0xFF)/255.0f;
		
		if(out == null)
			out = new Vector4f(r, g, b, a);
		else
			out.set(r, g, b, a);
		return out;
	}
	
	public static final int getInt(byte[] data, int position) {
		int a = data[position + 0] & 255;
		int b = data[position + 1] & 255;
		int c = data[position + 2] & 255;
		int d = data[position + 3] & 255;

		return makefourcc(a, b, c, d);
	}
	
	public static final int getIntBE(byte[] data, int position) {
		int a = data[position + 0] & 255;
		int b = data[position + 1] & 255;
		int c = data[position + 2] & 255;
		int d = data[position + 3] & 255;

		return makefourcc(d, c, b, a);
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
	
	@Deprecated
	public static short swap16(short rgb) {
		int a = (rgb >> 8) & 0xFF;
		int r = rgb & 0xff;

		return (short) ((r << 8) | a);
	}
	
	@Deprecated
	public static int swap32(int rgb) {
		int a = (rgb >> 24) & 0xFF;
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >> 8) & 0xff;
		int b = rgb & 0xff;

		return (b << 24) | (g << 16) | (r << 8) | a;
	}
	
	public static final float getFloat(byte[] data, int position){
		return Float.intBitsToFloat(getInt(data, position));
	}
	
	public static final float getFloatBE(byte[] data, int position){
		return Float.intBitsToFloat(getIntBE(data, position));
	}
	
	@Deprecated
	public static int extractFloat(byte[] data , int from, float[] out, int outIndex){
		int end = -1;
		for(int i = from; i < data.length; i++){
			if(data[i] == 0){
				end = i;
				break;
			}
		}
		
		if(end == -1)
			end = data.length;
		
		if(end == -1){
			throw new NumberFormatException();
		}
		
		out[outIndex] = Float.parseFloat(new String(data, from, end));
		return end;
	}
	
	/** Decompose the matrix <code>source</code> to source = TRS form. */ 
	public static void decompose(Matrix4f source, Vector3f scaling, Quaternion rotation, Vector3f position){
		// extract translation
		position.x = source.m30;
		position.y = source.m31;
		position.z = source.m32;
		
		// extract the scaling factors
		scaling.x = length(source.m00, source.m01, source.m02);
		scaling.y = length(source.m10, source.m11, source.m12);
		scaling.z = length(source.m20, source.m21, source.m22);
		
		// and the sign of the scaling
		if(source.determinant() < 0){
			scaling.scale(-1);
		}
		
//		source.transpose();
//		rotation.setFromMatrix(source);
//		source.transpose();
		
		Matrix3f mat3 = new Matrix3f();
		mat3.m00 = source.m00/scaling.x;
		mat3.m10 = source.m01/scaling.x;
		mat3.m20 = source.m02/scaling.x;
		
		mat3.m01 = source.m10/scaling.y;
		mat3.m11 = source.m11/scaling.y;
		mat3.m21 = source.m12/scaling.y;
		
		mat3.m02 = source.m20/scaling.z;
		mat3.m12 = source.m21/scaling.z;
		mat3.m22 = source.m22/scaling.z;
		
		rotation.setFromMatrix(mat3);
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
	
	static void encodeTestShort(){
		for(int i = 0; i < 10000; i++){
			short first = (short)(Math.random() * Integer.MAX_VALUE);
			short second = (short)(Math.random() * Integer.MAX_VALUE);
			int value = encode(first, second);
			short _first = (short) decodeFirst(value);
			short _second = (short) decodeSecond(value);
			
			if(first != _first || second != _second){
				System.out.println(first + " = " + _first + ", " + second + " = " + _second);
			}
		}
	}
	
	static void encodeTestInt(){
		for(int i = 0; i < 10000; i++){
			int first = (int)(Math.random() * Long.MAX_VALUE);
			int second = (int)(Math.random() * Long.MAX_VALUE);
			long value = encode(first, second);
			int _first = decodeFirst(value);
			int _second = decodeSecond(value);
			
			if(first != _first || second != _second){
				System.out.println(first + " = " + _first + ", " + second + " = " + _second);
			}
		}
	}
	
	static void partial_sum_test(){
		int[] src = {1,2,3,4,5};
		int[] result = new int[5];
		int returnd_result = partial_sum(src, 0, 5, result, 0);
		System.out.println("using default partial_sum: ");
		System.out.println(Arrays.toString(result));
		System.out.println("returnd_result = " + returnd_result);
	}
	
	static void upper_bound_test(){
		int myints[] = {10,20,30,30,20,10,10,20};
		Arrays.sort(myints);
		
		int up = upper_bound(myints, 0, myints.length, 20);
		int low = lower_bound(myints, 0, myints.length, 20);
		System.out.println("lower_bound at position " + low);
		System.out.println("upper_bound at position " + up);
		System.out.println(Arrays.toString(myints));
	}
	
	public static void main(String[] args) {
//		test_put_int();
		
//		byte[] bytes = new byte[10];
//		int count = assimp_itoa10(bytes, 0, 10, 12356);
//		System.out.println("count = " + count + ", " +new String(bytes));
		
//		VectorKey key = new VectorKey(10, new Vector3f(1, 2, 3));
//		VectorKey[] keys = copyOf(new VectorKey[]{key});
//		System.out.println(keys[0].mTime);
//		System.out.println(keys[0].mValue);
		
//		List<AssUtil> test = new ArrayList<AssUtil>();
//		Class<?> clazz = test.getClass();
//		System.out.println(clazz.getCanonicalName());
//		System.out.println(clazz.getTypeName());
////		System.out.println(clazz.getGenericSuperclass().toString());
//		Type[] types = clazz.getGenericInterfaces();
//		for(Type t: types)
//			System.out.println(t.toString());
		
		
//		find_first_null_test();
		
//		upper_bound_test();
		encodeTestInt();
//		ByteBuffer buf = ByteBuffer.wrap("123.5555".getBytes());
//		System.out.println(fast_atoreal_move(buf, false));
		
		System.out.println();
		System.out.println("done");
	}
	
	static void find_first_null_test(){
		Integer[] test_int = new Integer[100];
		
		for(int i = 0; i < 100000; i++){
			int k = (int)(Math.random() * 99);
			for(int j = 0; j < k; j++){
				test_int[j] = 1 + j;
			}
			
			for(int j = k; j < 100; j ++){
				test_int[j] = null;
			}
			
			System.out.println("k = " + k);
			if(k != findfirstNull(test_int))
				throw new RuntimeException();
			
			
		}
	}
	
	public static Object newInstance(String className){
		try {
			Class<?> cls = Class.forName(className);
			Constructor<?>[] constructors = cls.getDeclaredConstructors();
			Object obj = null;
			for(Constructor<?> constr : constructors){
				if(constr.getParameterTypes().length == 0){
					constr.setAccessible(true);
					obj = constr.newInstance();
					break;
				}
			}
			
			return obj;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	static void test_put_int(){
		byte[] bytes = new byte[4];
		Random random = new Random();
		int i = random.nextInt();
		System.out.println("i= " + i);
		getBytes(i, bytes, 0);
		int j = getInt(bytes, 0);
		
		System.out.println("\ni= " + j);
		System.out.println("Checked: " + (i == j));
	}
	
	static void test_fast_atoreal_move(){
		String[] array = new String[10000];
		byte[][] data = new byte[10000][];
		double[] value = new double[array.length];
		
		for(int i = 0; i < array.length; i++){
			array[i] = Double.toString(Math.random() * 1000 - 500); 
			data[i] = array[i].getBytes();
		}
		
		long time = System.currentTimeMillis();
		
		for(int i = 0; i < array.length; i++){
			value[i] = fast_atoreal_move(data[i], 0, null, false);
		}
		
		System.out.println((System.currentTimeMillis() - time) * 0.001);
		time = System.currentTimeMillis();
		for(int i = 0; i < array.length; i++){
			value[i] = Double.parseDouble(array[i]);
		}
		
		System.out.println((System.currentTimeMillis() - time) * 0.001);
		
		System.out.println(fast_atoreal_move("123m".getBytes(), 0, null, false));
	}
	
	static void test_fast_atoreal_move_buffer(){
		String[] array = new String[10000];
		ByteBuffer[] data = new ByteBuffer[10000];
		double[] value = new double[array.length];
		
		for(int i = 0; i < array.length; i++){
			array[i] = Double.toString(Math.random() * 1000 - 500); 
			data[i] = ByteBuffer.wrap(array[i].getBytes());
		}
		
		for(int i = 0; i < array.length; i++){
			double df = fast_atoreal_move(data[i], false);
			double dd = Double.parseDouble(array[i]);
			double d = df - dd;
			if(Math.abs(d) > 0.00000001){
				System.out.println("No Match " + i + ": " + d + ", df = " + df + ", dd = " + dd);
			}
			
			data[i].position(0);
		}
		
		long time = System.currentTimeMillis();
		
		for(int i = 0; i < array.length; i++){
			value[i] = Double.parseDouble(array[i]);
		}
		
		System.out.println((System.currentTimeMillis() - time) * 0.001);
		time = System.currentTimeMillis();
		for(int i = 0; i < array.length; i++){
			value[i] = fast_atoreal_move(data[i], false);
		}
		
		System.out.println((System.currentTimeMillis() - time) * 0.001);
		
	}
	
	static void test_decompose(){
		Matrix4f mat = new Matrix4f();
		mat.translate(1, 2, 3);
		mat.rotate((float) (Math.PI/4), 1, 0, 0);
		mat.scale(1, 2, 3);
		
		Vector3f scaling = new Vector3f();
		Quaternion rotation = new Quaternion();
		Vector3f position = new Vector3f();
		decompose(mat, scaling, rotation, position);
		
		System.out.println(scaling);
		System.out.println(position);
		System.out.println(rotation);
		
		rotation.setFromAxisAngle(1, 0, 0, (float) (Math.PI/4));
		System.out.println(rotation);
	}
	
	public static float length(float x, float y, float z){
		return (float) Math.sqrt(x * x + y * y + z * z);
	}
	
	public static String getString(ByteBuffer in, int start, int len){
		byte[] bytes = new byte[len];
		int position = in.position();
		in.position(start);
		in.get(bytes).position(position);
		return new String(bytes);
	}
	
	/**
	 * Parsing a long value from the ascii-character-sequences. Call this method will increase the position of the input value <i>in</i>.
	 * @param in
	 * @return
	 */
	public static long strtoul10_64(ByteBuffer in){
		long value = 0;
		int position = in.position();
		boolean signChecked = false;
		boolean negtive =false;
		while(in.remaining() > 0){
			char cur = (char)in.get();
			if(!signChecked){
				if(cur == '-'){
					negtive = true;
				}else if(cur == '+'){
					// nothing need to do.
				}else if(cur < '0' || cur > '9' ){
					value = cur - '0';
				}
				
				signChecked = true;
				continue;
			}
			
			if(cur < '0' || cur > '9' ){
				final long new_value = ( value * 10 ) + ( cur - '0' );
				if(new_value < 0){  /* numeric overflow, we rely on you */
					throw new NumberFormatException("Converting the string \"" + getString(in, position, in.position()) + "\" into a value resulted in overflow.");
				}
				value = new_value;
			}else{ // not a digit char
				break;
			}
		}
		
		return negtive ? -value : value;
	}
	
	public static long strtoul10_64(byte[] in, int offset, int[] out_and_max){
		int cur = 0;
		long value = 0;
		
		if(offset == in.length){
			if(out_and_max != null){
				out_and_max[0] = offset;
				out_and_max[1] = 0;
			}
			return 0;
		}

		int _in = in[offset] & 0xFF;
		if ( _in < '0' || _in > '9' )
				throw new IllegalArgumentException("The string \"" + new String(in, offset, in.length - offset) + "\" cannot be converted into a value.");

		while (offset < in.length)
		{
			if ( _in < '0' || _in > '9' )
				break;

			final long new_value = ( value * 10 ) + ( _in - '0' );
			
			if (new_value < value) /* numeric overflow, we rely on you */
				throw new RuntimeException("Converting the string \"" + new String(in, offset, in.length - offset) + "\" into a value resulted in overflow.");

			value = new_value;

//			++in;
			++cur;
			++offset;
			if(offset == in.length)
				break;
			_in = in[offset] & 0xFF;

			if (out_and_max != null && out_and_max[1] == cur) {
						
//				/* skip to end */
				while (_in >= '0' && _in <= '9'){
//					++in;
					++offset; 
					if(offset == in.length)
						break;
					_in = in[offset] & 0xFF;
				}
				out_and_max[0] = offset;
				return value;
			}
		}
		if (out_and_max != null)
//			*out = in;
			out_and_max[0] = offset;

		if (out_and_max != null)
			out_and_max[1] = cur;

		return value;
	}
	
	public static int strtoul10(String out, int pos) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public static int getBytes(Vector3f v, byte[] out, int out_offset){
		int position = out_offset;
		position = getBytes(v.x, out, position);
		position = getBytes(v.y, out, position);
		position = getBytes(v.z, out, position);
		
		return position;
	}
	
	public static int getBytes(Vector3f[] va, byte[] out, int out_offset){
		int position = out_offset;
		for(int i = 0; i < va.length; i++){
//			Vector3f v = va[i];
//			position = putFloat(v.x, out, position);
//			position = putFloat(v.y, out, position);
//			position = putFloat(v.z, out, position);
			
			position = getBytes(va[i], out, position);
		}
		
		return position;
	}
	
	public static int getBytes(Vector4f v, byte[] out, int out_offset){
		int position = out_offset;
		position = getBytes(v.x, out, position);
		position = getBytes(v.y, out, position);
		position = getBytes(v.z, out, position);
		position = getBytes(v.w, out, position);
		
		return position;
	}
	
	public static int getBytes(Vector4f[] va, byte[] out, int out_offset){
		int position = out_offset;
		for(int i = 0; i < va.length; i++){
			position = getBytes(va[i], out, position);
		}
		
		return position;
	}
	
	public static int getBytes(UVTransform v, byte[] out, int out_offset){
		int position = out_offset;
		position = getBytes(v.mTranslation.x, out, position);
		position = getBytes(v.mTranslation.y, out, position);
		position = getBytes(v.mScaling.x, out, position);
		position = getBytes(v.mScaling.y, out, position);
		position = getBytes(v.mRotation, out, position);
		
		return position;
	}
	
	public static int getBytes(UVTransform[] va, byte[] out, int out_offset){
		int position = out_offset;
		for(int i = 0; i < va.length; i++){
			position = getBytes(va[i], out, position);
		}
		
		return position;
	}
	
	/**
	 * Return the length of the given array. If the array is null, return 0.
	 * @param arr
	 * @return
	 */
	public static int length(Object[] arr){
		return arr != null ? arr.length : 0;
	}
	
	/**
	 * Return the length of the given string. If the string is null, return 0.
	 * @param arr
	 * @return
	 */
	public static int length(CharSequence arr){
		return arr != null ? arr.length() : 0;
	}
	
	public static float fast_atof(byte[] c){
		return (float)fast_atoreal_move(c, 0, null, false);
	}
	
	public static double fast_atoreal_move(ByteBuffer in, boolean check_comma){
		double value = 0.0;
		boolean signChecked = false;
		boolean negtive = false;
		boolean dotChecked = false;
		int start = in.position();
		int dot_pos = -1;
		while(in.remaining() > 0){
			int cur = in.get();
			if(!signChecked){
				if(cur == '-'){
					negtive = true;
				}else if(cur == '+'){
					// nothing need to do.
				}else if(cur == '.' || (check_comma && cur ==',')){
					dotChecked = true;
					dot_pos = in.position();
				}else if(cur >= '0' || cur <= '9' ){
					value = cur - '0';
				}else{  // not a valid number.
					break;
				}
				
				signChecked = true;
				continue;
			}
			
			if(cur == '.' || (check_comma && cur ==',')){
				if(dotChecked){
					throw new NumberFormatException("Invalid string \"" + getString(in, start, in.position() + 1) + "\"");
				}
				
				dotChecked = true;
				dot_pos = in.position();
			}else if(cur >= '0' || cur <='9'){
				if(dotChecked){
					value += fast_atof_table[Math.min(in.position() - dot_pos, fast_atof_table.length - 1)] * (cur - '0');
				}else{
					value = value * 10.0 + (cur - '0');
				}
			}else if(cur =='E' || cur == 'e'){
				value *= Math.pow(10.0, strtoul10_64(in));
				break;
			}
		}
		
		if(negtive)
			value = -value;
		
		return value;
	}
	
	/** Provides a fast function for converting a string into a float,
	 * about 4 times faster than {@link java.lang.Double.parseDouble(String)}.
	 * If you find any bugs, please send them to me, niko (at) irrlicht3d.org.
	 */
	public static double fast_atoreal_move(byte[] c, int offset, int[] out_offset, boolean check_comma){
		double f;
        int[] out_and_max = new int[2];
		int _c = c[offset] & 0xFF;
		boolean inv = (_c=='-');
		if (inv || _c=='+') {
//			++c;
			offset++;
			_c = c[offset] & 0xFF;
		}

		f = strtoul10_64 ( c, offset, out_and_max);
		offset = out_and_max[0];
		_c = c[offset] & 0xFF;
		
		int next = 0;
		if(check_comma && _c == ','){
			next = c[offset + 1] & 0xFF;
		}
		if (_c == '.' || (check_comma && _c == ',' && next >= '0' && next <= '9')) // allow for commas, too
		{
//			++c;
			offset++;
			if(offset < c.length)
				_c = c[offset] & 0xFF;
			else
				_c = 0;

			// NOTE: The original implementation is highly inaccurate here. The precision of a single
			// IEEE 754 float is not high enough, everything behind the 6th digit tends to be more 
			// inaccurate than it would need to be. Casting to double seems to solve the problem.
			// strtol_64 is used to prevent integer overflow.

			// Another fix: this tends to become 0 for long numbers if we don't limit the maximum 
			// number of digits to be read. AI_FAST_ATOF_RELAVANT_DECIMALS can be a value between
			// 1 and 15.
			int diff =  15/*AI_FAST_ATOF_RELAVANT_DECIMALS*/;
			out_and_max[1] = diff;
			double pl = strtoul10_64 ( c, offset, out_and_max );
			offset = out_and_max[0];
			if(offset < c.length)
				_c = c[offset] & 0xFF;
			diff = out_and_max[1];
			
			pl *= fast_atof_table[diff];
			f += pl;
		}

		// A major 'E' must be allowed. Necessary for proper reading of some DXF files.
		// Thanks to Zhao Lei to point out that this if() must be outside the if (*c == '.' ..)
		if (_c == 'e' || _c == 'E')	{

//			++c;
			offset++;
			if(offset < c.length)
				_c = c[offset] & 0xFF;
			else
				throw new NumberFormatException();
			
			final boolean einv = (_c=='-');
			if (einv || _c=='+') {
//				++c;
				offset++;
				if(offset == c.length)
					throw new NumberFormatException();
			}

			// The reason float constants are used here is that we've seen cases where compilers
			// would perform such casts on compile-time constants at runtime, which would be
			// bad considering how frequently fast_atoreal_move<float> is called in Assimp.
			double exp = strtoul10_64(c, offset, out_and_max);
			offset = out_and_max[0];
//			_c = c[offset] & 0xFF;
			if (einv) {
				exp = -exp;
			}
			f *= Math.pow(10.0, exp);
		}

		if (inv) {
			f = -f;
		}
//		out = f;
		if(out_offset != null)
			out_offset[0] = offset;
		return f;
	}
	
	/**
	 * Convert a byte[] array into a integer.
	 * @param in
	 * @param offset
	 * @param out_offset
	 * @return
	 */
	public static int strtoul10(byte[] in, int offset, int[] out_offset){
		int value = 0;
		while(offset < in.length){
			int _in = in[offset] &0xFF;
			if ( _in < '0' || _in > '9' )
				break;

			value = ( value * 10 ) + ( _in - '0' );
			offset++;
		}
		
		if(out_offset != null)
			out_offset[0] = offset;
		
		return value;
	}
	
	public static int assimp_itoa10(byte[] out, int offset, int max, int number){
		// write the unary minus to indicate we have a negative number
		int written = 1;
		if (number < 0 && written < max)	{
//			*out++ = '-';
			out[offset++] = '-';
			++written;
			number = -number;
		}

		// We begin with the largest number that is not zero. 
		int cur = 1000000000; // 2147483648
		boolean mustPrint = false;
		while (written < max)	{

			final int digit = number / cur;
			if (mustPrint || digit > 0 || 1 == cur)	{
				// print all future zeroes from now
				mustPrint = true;	

				out[offset++] = (byte) ('0'+(char)(digit));

				++written;
				number -= digit*cur;
				if (1 == cur) {
					break;
				}
			}
			cur /= 10;
		}

		// append a terminal zero
//		*out++ = '\0';
		return written-1;
	}
	
	public static int sizeof(Class<?> clazz){
		Field[] fields = clazz.getDeclaredFields();
		
		int size = 0;
		for(Field f : fields){
			Class<?> type = f.getType();
			if(type == int.class || type == float.class){
				size += 4;
			}else if(type == long.class || type == double.class){
				size += 8;
			}else if(type == short.class || type == char.class){
				size += 2;
			}else if(type == byte.class || type == boolean.class){
				size += 1;
			}else{  // other reference type
				size += 4;
			}
		}
		
		return size;
	}
	
	@SuppressWarnings("unchecked")
	public static<T extends Copyable<T>> T[] copyOf(T[] arr){
		if(arr == null)
			return null;
		Class<?> clazz = arr.getClass().getComponentType();
		T[] dest = (T[]) Array.newInstance(clazz, arr.length);
		
		for(int i = 0; i < arr.length; i++)
			dest[i] = arr[i].copy();
		
		return dest;
	}
	
	public static FloatBuffer copyOf(FloatBuffer arr){
		if(arr == null)
			return null;
		
		int oldPosition = arr.position();
		int length = arr.remaining();
		if(length == 0)
			return null;
		
		if(arr.isDirect()){
			FloatBuffer out = MemoryUtil.createFloatBuffer(arr.remaining(), true);
			out.put(arr);
			out.flip();
			arr.position(oldPosition);
			return out;
		}else{
			float[] _dest = new float[length];
			arr.get(_dest);
			arr.position(oldPosition);
			return FloatBuffer.wrap(_dest);
		}
	}
	
	public static IntBuffer copyOf(IntBuffer arr){
		if(arr == null)
			return null;
		
		int length = arr.remaining();
		if(length == 0)
			return null;
		
		if(arr.isDirect()){
			IntBuffer out = MemoryUtil.createIntBuffer(arr.remaining(), true);
			int oldPosition = arr.position();
			out.put(arr);
			out.flip();
			arr.position(oldPosition);
			return out;
		}else{
			int[] _dest = new int[length];
			arr.get(_dest);
			return IntBuffer.wrap(_dest);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static<T> T[] initArray(T[] arr){
		if(arr == null)
			return null;
		
		Class<?> clazz = arr.getClass().getComponentType();
		try {
			for(int i = 0; i < arr.length; i++)
				if(arr[i] == null)
					arr[i] = (T) clazz.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return arr;
	}
	
	public static<T> T back(List<T> list){
		int size = list.size();
		if(size > 0)
			return list.get(size - 1);
		else
			return null;
	}
	
	public static Quaternion aiQuaterniont(float fPitch, float fYaw, float fRoll, Quaternion dest){
		final float fSinPitch = (float)(Math.sin(fPitch*0.5f));
		final float fCosPitch = (float)(Math.cos(fPitch*0.5f));
		final float fSinYaw = (float)(Math.sin(fYaw*0.5f));
		final float fCosYaw = (float)(Math.cos(fYaw*0.5f));
		final float fSinRoll = (float)(Math.sin(fRoll*0.5f));
		final float fCosRoll = (float)(Math.cos(fRoll*0.5f));
		final float fCosPitchCosYaw = (fCosPitch*fCosYaw);
		final float fSinPitchSinYaw = (fSinPitch*fSinYaw);
		float x = fSinRoll * fCosPitchCosYaw     - fCosRoll * fSinPitchSinYaw;
		float y = fCosRoll * fSinPitch * fCosYaw + fSinRoll * fCosPitch * fSinYaw;
		float z = fCosRoll * fCosPitch * fSinYaw - fSinRoll * fSinPitch * fCosYaw;
		float w = fCosRoll * fCosPitchCosYaw     + fSinRoll * fSinPitchSinYaw;
		
		if(dest == null)
			dest = new Quaternion(x, y, z, w);
		else
			dest.set(x, y, z, w);
		
		return dest;
	}
	
	/** Convert just one hex digit
	 * Return value is UINT_MAX if the input character is not a hex digit.*/
	public static int hexDigitToDecimal(char in)
	{
		int out = -1;
		if (in >= '0' && in <= '9')
			out = in - '0';

		else if (in >= 'a' && in <= 'f')
			out = 10 + in - 'a';

		else if (in >= 'A' && in <= 'F')
			out = 10 + in - 'A';

		// return value is UINT_MAX if the input is not a hex digit
		return out;
	}
	
	public static boolean equals(ByteBuffer buf, CharSequence string, int str_offset, int length){
		if(buf.remaining() < length)
			return false;
		
		if(string.length() - str_offset < length)
			return false;
		
		int buf_offset = buf.position();
		for(int i = 0; i < length; i++){
			if(buf.get(buf_offset + i) != string.charAt(str_offset + i))
				return false;
		}
		
		return true;
	}
	
	public static boolean equals(CharSequence l, CharSequence r){
		if(l == null || r == null)
			return false;
		
		if(l == r)
			return true;
		
		int llen = l.length();
		if(llen != r.length())
			return false;
		
		for(int i = 0; i < llen; i++){
			if(l.charAt(i) != r.charAt(i))
				return false;
		}
		
		return true;
	}
	
	public static boolean equals(byte[] l, int offset, CharSequence r){
		int length = r.length();
		if(l.length - offset < length)
			return false;
		
		for(int i = 0; i < length; i++){
			if(l[offset + i] != r.charAt(i))
				return false;
		}
		
		return true;
	}
	
	// Used for codegen.
	public static String getClipboard(){
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		try {
			return (String) t.getTransferData(DataFlavor.stringFlavor);
		} catch (UnsupportedFlavorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	public static void genLoadBytebuffer(Class<?> clazz){
		StringBuilder sb = new StringBuilder();
		sb.append("int old_pos = buf.position();\n");
		Field[] fields = clazz.getDeclaredFields();
		for(Field field : fields){
			if((field.getModifiers() & Modifier.STATIC) != 0)
				// igore the static field.
				continue;
			
			final Class<?> type = field.getType();
			final String name = filterCharater(field.getName(), "[]");
			
			if(type.isArray()){
				Class<?> cmpClass = type.getComponentType();
				String typename = cmpClass.getSimpleName();
				final String upperCase = Character.toUpperCase(typename.charAt(0)) + typename.substring(1);
				if(cmpClass == byte.class){
					sb.append("buf.get(").append(name).append(");\n");
				}else if(cmpClass.isPrimitive()){
					sb.append("for(int i = 0; i < ").append(name).append(".length; i++)\n");
					sb.append("\t").append(name).append("[i] = buf.get").append(upperCase).append("();\n");
				}else{
					// igoren the other types.
					System.out.println("Unable to resolve the type: " + cmpClass.getName());
				}
			}else if(type.isPrimitive()){ //primitive type
				String typename = type.getSimpleName();
				final String upperCase = Character.toUpperCase(typename.charAt(0)) + typename.substring(1);
				if(type == byte.class){
					sb.append(name).append(" = buf.get();\n");
				}else{
					sb.append(name).append(" = buf.get").append(upperCase).append("();\n");
				}
			}
		}
		
		sb.append("\nbuf.position(old_pos);\n");
		System.out.println(sb);
	}
	
	public static void genStructCopy(Class<?> clazz){
		String template = FileUtils.loadTextFromClassPath(AssUtil.class, "gen_template.txt").toString();
		StringBuilder out = new StringBuilder(512);
		List<String> lines = new ArrayList<String>();
		
		parseClass(clazz, lines);
		
		boolean first = true;
		for(String line : lines){
			if(!first){
				out.append("\t\t");
			}else{
				first = false;
			}
			
			out.append(line).append('\n');
		}
		
		String className = clazz.getSimpleName();
		template = String.format(template, className, className, className, className);
		System.out.println(template.replace("##", out));
	}
	
	public static String filterCharater(String source, String chars){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < source.length(); i++){
			char c = source.charAt(i);
			if(chars.indexOf(c) <0)
				sb.append(c);
		}
		
		return sb.toString();
	}
	
	private static void parseClass(Class<?> clazz, List<String> lines){
		String[] array = {
			"java.lang.Integer", "java.lang.Byte", "java.lang.Boolean",
			"java.lang.Character", "java.lang.Float", "java.lang.Double",
			"java.lang.Short", "java.lang.String","java.lang.Long"
		};
		
		Arrays.sort(array);
		Field[] fields = clazz.getDeclaredFields();
		for(Field field : fields){
			if((field.getModifiers() & Modifier.STATIC) != 0)
				// igore the static field.
				continue;
			
			final Class<?> type = field.getType();
			final String name = filterCharater(field.getName(), "[]");
			final boolean isFinal = (field.getModifiers() & Modifier.FINAL) != 0;
			if(type.isPrimitive() || type == String.class || (type.isInterface() && type != List.class)){
				lines.add(String.format("%s = o.%s;", name, name));
			}else{
				if(type.isArray()){ // The field is a array.
					Class<?> compType = type.getComponentType();
					if(compType.isPrimitive() || compType == String.class || compType.isInterface()){
						if(!isFinal){
							lines.add(String.format("if(o.%s != null){", name));
							lines.add(String.format("\t%s = new %s[o.%s.length];", name, compType.getSimpleName(), name));
							lines.add(String.format("\t%s = Arrays.copyOf(o.%s, o.%s.length);", name, name, name));
							lines.add("}");
						}else{
							lines.add(String.format("System.arraycopy(o.%s, 0, %s, 0, o.%s.length);", name, name, name));
						}
					}else if(compType.isArray()){  // The field ia two dimension array.
						if(!isFinal){
							lines.add(String.format("if(o.%s != null){", name));
							lines.add(String.format("\t%s = new %s[o.%s.length][];", name, compType.getSimpleName(), name));
							lines.add(String.format("\tfor(int i = 0; i < o.%s.length; i++){", name));
							lines.add(String.format("\t\tif(o.%s[i] != null)", name));
							lines.add(String.format("\t\t\t%s[i] = Arrays.copyOf(o.%s[i], o.%s[i].length);", name, name, name));
							lines.add("\t}");
							lines.add("}");
						}else{
							lines.add(String.format("for(int i = 0; i < o.%s.length; i++)", name));
							lines.add(String.format("\tSystem.arraycopy(o.%s[i], 0, %s[i], 0, o.%s[i].length);", name,name,name));
						}
					}else{ 
						// object array.
						if(!isFinal){
							lines.add(String.format("if(o.%s != null){", name));
							lines.add(String.format("\t%s = Arrays.copyOf(o.%s, o.%s.length);", name, name, name));
						}else{
							String simpleName = compType.getSimpleName();
//							lines.add(String.format("System.arraycopy(o.%s, 0, %s, 0, o.%s.length);", name, name, name));
							lines.add(String.format("for(int i = 0; i < o.%s.length; i++)", name));
							lines.add(String.format("\t%s[i] = new %s(o.%s[i]);", name, simpleName, name));
						}
					}
				}else{
					boolean isList = false;
					if(type == List.class || type == ArrayList.class || type == LinkedList.class){
						isList = true;
					}
					
					if(!isList){
						Class<?>[] interfaces = type.getInterfaces();
						for(Class<?> inter : interfaces){
							if(inter == Collection.class){
								isList = true;
								break;
							}
						}
					}
					
					if(isList){
						String genricType = null;
						String simpleName = null;
						String typename = field.getGenericType().getTypeName();
						int i1 = typename.indexOf('<');
						if(i1 > 0){
							int i2 = typename.lastIndexOf('>');
							genricType = typename.substring(i1 + 1, i2);
							int last  =genricType.lastIndexOf('.');
							simpleName = genricType.substring(last + 1);
						}
						lines.add(String.format("%s.clear();", name));
						if(genricType == null || Arrays.binarySearch(array, genricType) >=0){
							lines.add(String.format("%s.addAll(o.%s);", name, name));
						}else{
							lines.add(String.format("for(int i = 0; i < o.%s.size(); i++)", name));
							lines.add(String.format("\t%s.add(new %s(o.%s.get(i)));", name, simpleName, name));
						}
					}else{
						if(isFinal){
							lines.add(String.format("%s.set(o.%s);", name, name));
						}else{
							lines.add(String.format("%s = o.%s;", name, name));
						}
					}
				}
			}
		}
	}

	public static String makeString(Object arg, Object...args) {
		if(args == null || args.length == 0)
			return arg.toString();
		
		StringBuilder sb = new StringBuilder(64);
		sb.append(arg);
		
		for(Object obj : args){
			if(obj instanceof Object[])
				sb.append(Arrays.toString((Object[])obj));
			else
				sb.append(obj.toString());
		}
		return sb.toString();
	}
	
	@SuppressWarnings("unchecked")
	public static<T> T[] toArray(Collection<T> c, Class<T> cls){
//		Method[] methods = AssUtil.class.getMethods();
//		Method toArrayMethod = null;
//		for(Method m : methods){
//			if(m.getName().equals("toArray")){
//				toArrayMethod = m;
//				break;
//			}
//		}
//		
//		Type type = toArrayMethod.getGenericParameterTypes()[0];
//		System.out.println(type);
		
		if(c != null && c.size() > 0){
			T[] out = (T[]) Array.newInstance(cls, c.size());
			c.toArray(out);
			return out;
		}else{
			return null;
		}
	}
	
	public static int findfirstNull(Object[] array){
		int first = 0;
		int end = array.length;
		if(array[first] == null) return 0;
		
		while(first < end){
			int middle = (first + end) >> 1;
			Object obj = array[middle];
			if(obj == null){
				if(middle - first <= 1)
					return middle;
				end = middle;
			}else{
				first = middle;
				if(end - first == 1)
					return array[end] == null ? end : -1;
			}
		}
		
		return -1;
	}
	
	public static int findfirstZero(byte[] array){
		int first = 0;
		int end = array.length;
		if(array[first] == 0) return 0;
		
		while(first < end){
			int middle = (first + end) >> 1;
			byte obj = array[middle];
			if(obj == 0){
				if(middle - first <= 1)
					return middle;
				end = middle;
			}else{
				first = middle;
				if(end - first == 1)
					return array[end] == 0 ? end : -1;
			}
		}
		
		return -1;
	}
	
	public static int parseInt(String s){
		return Integer.parseInt(s);
	}
	
	public static float parseFloat(String s){
		return Float.parseFloat(s);
	}
	
	public static<T> void resize(ArrayList<T> list, int size, Class<? extends T> clazz){
		int count = size - list.size();
		list.ensureCapacity(size);
		while(count -- > 0)
			try {
				list.add(clazz == null ? null : clazz.newInstance());
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
	}
	
	public static<T> void resize(List<T> list, int size, Class<? extends T> clazz){
		int count = size - list.size();
		while(count -- > 0)
			try {
				list.add(clazz == null ? null : clazz.newInstance());
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
	}
	
	public static byte hexOctetToDecimal(String data, int offset) {
		char b0 = data.charAt(offset);
		char b1 = data.charAt(offset + 1);
		return (byte)(hexDigitToDecimal((char)(b0 << 4)) + hexDigitToDecimal(b1));
	}
	
	public static boolean isEmpty(CharSequence string){
		return string == null || string.length() == 0;
	}
	
	public static boolean isEmpty(Collection<?> c){
		return c == null || c.isEmpty();
	}
	
	public static boolean isEmpty(Map<?, ?> c){
		return c == null || c.isEmpty();
	}
	
	public static boolean isEmpty(Buffer buf){
		return buf == null || buf.remaining() == 0;
	}
	
	public static<K> int getInt(Object2IntMap<K> map, K key, int defaultValue){
		return map != null ? map.getInt(key) : defaultValue;
	}

	public static int size(Collection<?> c) {
		return c != null ? c.size() : 0;
	}
	
	public static int accumulate(IntList list, int fist, int end, int value){
		while(fist != end){
			value += list.getInt(fist ++);
		}
		
		return value;
	}
	
	public static IntArrayList reserve(IntArrayList list, int capacity){
		if(list == null)
			list = new IntArrayList(capacity);
		else
			list.ensureCapacity(capacity);
		
		return list;
	}
	
	public static<T> ArrayList<T> reserve(ArrayList<T> list, int capacity){
		if(list == null)
			list = new ArrayList<T>(capacity);
		else
			list.ensureCapacity(capacity);
		
		return list;
	}
	
	/**
	 * Assigns to every element in the range starting at result the partial sum of the corresponding elements in the range [first,last).<p>
	 * If x represents an element in [first,last) and y represents an element in result, the ys can be calculated as:
	 * <pre>
	 * y0 = x0 
	 * y1 = x0 + x1 
	 * y2 = x0 + x1 + x2 
	 * y3 = x0 + x1 + x2 + x3 
	 * y4 = x0 + x1 + x2 + x3 + x4 
	 * ... ... ...
     * </pre>
	 * @param src
	 * @param first
	 * @param last
	 * @param dst
	 * @param result
	 * @return An index pointing to past the last element of the destination sequence where resulting elements have been stored, or result if [first,last) is an empty range
	 */
	public static int partial_sum(int[] src, int first, int last, int[] dst, int result){
		if(first != last){
			int val = src[first];
			dst[result] = val;
			
			while(++first != last){
				val = val + src[first];
				dst[++result] = val;
			}
			++result;
		}
		
		return result;
	}
	
	/** Returns an iterator pointing to the first element in the range [first,last) which compares greater than val. */
	public static int upper_bound(int[] src, int first, int last, int val){
		int it;
		int count, step;
		count = last - first;
		while(count > 0){
			it = first; step = count/2; it+= step;
			if(!(val < src[it])) // or: if (!comp(val,*it)), for version (2)
			{ first = ++it; count -= step+ 1;}
			else count = step;
		}
		
		return first;
	}
	
	/** Returns an iterator pointing to the first element in the range [first,last) which does not compare less than val. */
	public static int lower_bound(int[] src, int first, int last, int val){
		int it;
		int count, step;
		count = last - first;
		
		while(count > 0){
			it = first; step = count/2; it += step;
			if(src[it] < val){
				first = ++it;
				count-= step + 1;
			}else
				count = step;
		}
		
		return first;
	}
	
	public interface UnaryPredicateInt{
		boolean accept(int value);
	}
	
	/**
	 * Returns the number of elements in the range [first,last) for which pred is true.<p>
	 * The range used is [first,last), which contains all the elements between first and last, including the element pointed by first but not the element pointed by last.
	 * @param src The data source.
	 * @param first The index pointing to the initial position of the source array.
	 * @param last The index pointing to the final position of the source array.
	 * @param pred 
	 * @return
	 */
	public static int count_if(int[] src, int first, int last, UnaryPredicateInt pred){
		int ret = 0;
		while(first != last){
			if(pred.accept(src[first])) ++ ret;
			++first;
		}
		
		return ret;
	}
	
	public interface BinaryPredicateInt{
		boolean accept(int l, int r);
	}
	
	public static boolean equals(int[] left, int left_offset, int[] right, int right_offset, int length){
		for(int i = 0; i < length; i++)
			if(left[left_offset + i] != right[right_offset + i])
				return false;
		return true;
	}
	
	public static boolean equals(int[] left, int left_offset, int[] right, int right_offset, int length, BinaryPredicateInt pred){
		if(pred == null)
			return equals(left, left_offset, right, right_offset, length);
		
		for(int i = 0; i < length; i++)
			if(!pred.accept(left[left_offset + i], right[right_offset + i]))
				return false;
		return true;
	}
	
	public interface BinaryPredicateLong{
		boolean accept(long l, long r)/*{ return l == r;}*/;
	}
	
	public static boolean equals(long[] left, int left_offset, long[] right, int right_offset, int length){
		for(int i = 0; i < length; i++)
			if(left[left_offset + i] != right[right_offset + i])
				return false;
		return true;
	}
	
	public static boolean equals(long[] left, int left_offset, long[] right, int right_offset, int length, BinaryPredicateLong pred){
		if(pred == null)
			return equals(left, left_offset, right, right_offset, length);
		
		for(int i = 0; i < length; i++)
			if(!pred.accept(left[left_offset + i], right[right_offset + i]))
				return false;
		return true;
	}
	
	public static boolean isIdentity(Matrix4f mat){
		// Use a small epsilon to solve floating-point inaccuracies
		final float epsilon = 10e-3f;
		return (mat.m10 <= epsilon && mat.m10 >= -epsilon &&
				mat.m20 <= epsilon && mat.m20 >= -epsilon &&
				mat.m30 <= epsilon && mat.m30 >= -epsilon &&
				mat.m01 <= epsilon && mat.m01 >= -epsilon &&
			    mat.m21 <= epsilon && mat.m21 >= -epsilon &&
	    		mat.m31 <= epsilon && mat.m31 >= -epsilon &&
				mat.m02 <= epsilon && mat.m02 >= -epsilon &&
				mat.m12 <= epsilon && mat.m12 >= -epsilon &&
				mat.m32 <= epsilon && mat.m32 >= -epsilon &&
				mat.m03 <= epsilon && mat.m03 >= -epsilon &&
				mat.m13 <= epsilon && mat.m13 >= -epsilon &&
				mat.m23 <= epsilon && mat.m23 >= -epsilon &&
				mat.m00 <= 1.f+epsilon && mat.m00 >= 1.f-epsilon &&
				mat.m11 <= 1.f+epsilon && mat.m11 >= 1.f-epsilon &&
				mat.m22 <= 1.f+epsilon && mat.m22 >= 1.f-epsilon &&
				mat.m33 <= 1.f+epsilon && mat.m33 >= 1.f-epsilon);
	}
	
	public static String getNextLine(ByteBuffer buffer, byte[] cache, boolean trim){
		if(buffer.remaining() == 0)
			return null;
		
		int cursor = buffer.position();
		
		while(cursor < buffer.limit() && buffer.get(cursor) == '\n')
			cursor++;
		
		int start = cursor;
		byte b;
		while(cursor < buffer.limit() && (b = buffer.get(cursor)) != '\n')
			cursor++;
		
		int end = cursor < buffer.limit() ? cursor + 1/* Skip the char '\n'*/ : cursor;
		int length;
		if(trim){
			while(start < cursor){
				b = buffer.get(start);
				if(b == ' '|| b == '\t'){
					start ++;
				}else{
					break;
				}
			}
			
			do{
				b = buffer.get(-- cursor);
			}while(cursor > start && (b ==' '|| b == '\t'));
		}
		
		length = cursor - start;
		if(cache == null || length > cache.length){
			cache = new byte[length];
		}
		
		buffer.position(start);
		buffer.get(cache).position(end);
		return new String(cache, 0, length);
	}
	
	public static boolean isNumeric(String string){
		try {
			Double.parseDouble(string);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/** A function for creating a rotation matrix that rotates a vector called
	 * "from" into another vector called "to".<p>
	 * Input : from[3], to[3] which both must be *normalized* non-zero vectors<br>
	 * Output: mtx[3][3] -- a 3x3 matrix in colum-major form<br>
	 * Authors: Tomas Möller, John Hughes<br>
	 *          "Efficiently Building a Matrix to Rotate One Vector to Another"<br>
	 *          Journal of Graphics Tools, 4(4):1-4, 1999
	 */
	public static Matrix4f fromToMatrix(ReadableVector3f from, ReadableVector3f to, Matrix4f out){
		if(out == null)
			out = new Matrix4f();
		
		final float e = Vector3f.dot(from, to);
		final float f = (e < 0)? -e:e;

		if (f > (1.0f) - (0.00001f))     /* "from" and "to"-vector almost parallel */
		{
			Vector3f u,v;     /* temporary storage vectors */
			Vector3f x;       /* vector most nearly orthogonal to "from" */
			
			u = new Vector3f();
			v = new Vector3f();
			x = new Vector3f();

			x.x = (from.getX() > 0.0)? from.getX() : -from.getX();
			x.y = (from.getY() > 0.0)? from.getY() : -from.getY();
			x.z = (from.getZ() > 0.0)? from.getZ() : -from.getZ();

			if (x.x < x.y)
			{
				if (x.x < x.z)
				{
					x.x = (1.0f); x.y = x.z = (0.0f);
				}
				else
				{
					x.z = (1.0f); x.y = x.z = (0.0f);
				}
			}
			else
			{
				if (x.y < x.z)
				{
					x.y = (1.0f); x.x = x.z = (0.0f);
				}
				else
				{
					x.z = (1.0f); x.x = x.y = (0.0f);
				}
			}

			u.x = x.x - from.getX(); u.y = x.y - from.getY(); u.z = x.z - from.getZ();
			v.x = x.x - to.getX();   v.y = x.y - to.getY();   v.z = x.z - to.getZ();

			final float c1 = (2.0f) / Vector3f.dot(u , u);
			final float c2 = (2.0f) / Vector3f.dot(v , v);
			final float c3 = (c1 * c2)  * Vector3f.dot(u , v);

			for (int i = 0; i < 3; i++) 
			{
				for (int j = 0; j < 3; j++) 
				{
					float ff/*mtx[i][j]*/ =  - c1 * u.get(i) * u.get(j) - c2 * v.get(i) * v.get(j)
						+ c3 * v.get(i) * u.get(j);
					out.set(i, j, ff, false);
				}
//				mtx[i][i] += (1.0);
			}
			
			out.m00 += 1f;
			out.m11 += 1f;
			out.m22 += 1f;
		}
		else  /* the most common case, unless "from"="to", or "from"=-"to" */
		{
//			const aiVector3D v = from ^ to;
			Vector3f v = Vector3f.cross(from, to, null);
			/* ... use this hand optimized version (9 mults less) */
			final float h = 1f/(1f + e);      /* optimization by Gottfried Chen */
			final float hvx = h * v.x;
			final float hvz = h * v.z;
			final float hvxy = hvx * v.y;
			final float hvxz = hvx * v.z;
			final float hvyz = hvz * v.y;
			out.m00 = e + hvx * v.x;
			out.m10 = hvxy - v.z;
			out.m20 = hvxz + v.y;

			out.m01 = hvxy + v.z;
			out.m11 = e + h * v.y * v.y;
			out.m21 = hvyz - v.x;

			out.m02 = hvxz - v.y;
			out.m12 = hvyz + v.x;
			out.m22 = e + hvz * v.z;
		}
		
		out.m30 = out.m31 = out.m32 = 0f;
		out.m03 = out.m13 = out.m23 = 0f;
		out.m33 = 1f;
		
		return out;
	}
	
	public static boolean inrange(int var, int low, int high){
		return var > low && var <= high;
	}
	
	public static String toString(byte[] bytes){
		int end = findfirstZero(bytes);
		return end == 0 ? "" : new String(bytes,0,end);
	}
	
	public static String toString(FloatBuffer buf, int cmp_size){
		if(buf == null)
			return " ";
		StringBuilder sb = new StringBuilder(buf.limit() - buf.position());
		int count = (buf.limit() - buf.position()) / cmp_size;
		for(int i = 0; i < count; i++){
			sb.append('(');
			for(int j = 0; j < cmp_size - 1; j++){
				int offset = i * cmp_size + j;
				sb.append(buf.get(buf.position() + offset)).append(',');
			}
			sb.append(buf.get(buf.position() + i * cmp_size + cmp_size - 1));
			sb.append(')');
			sb.append(' ');
		}
		
		return sb.toString();
	}
	
	public static String toString(FloatBuffer[] bufs, int cmp_size){
		StringBuilder sb = new StringBuilder(1024);
		for(FloatBuffer buf : bufs){
			sb.append('[').append(toString(buf, cmp_size)).append(']').append(' ');
		}
		
		return sb.toString();
	}
}