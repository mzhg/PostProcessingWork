package jet.opengl.postprocessing.util;

import org.lwjgl.util.vector.Matrix2f;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.List;

public class CacheBuffer {

	/* default to allocate 128kb memory. */
	private static final int INIT_CAPACITY = 128 * 1024;
	private static ByteBuffer nativeBuffer;
	
	private static IntBuffer intBuffer;
	private static ShortBuffer shortBuffer;
	private static FloatBuffer floatBuffer;
	private static DoubleBuffer doubleBuffer;
	private static LongBuffer longBuffer;
	
	static{
		remolloc(INIT_CAPACITY);
	}
	
	private static void remolloc(int size){
		if(nativeBuffer == null || nativeBuffer.capacity() < size){
			nativeBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
			
			intBuffer = nativeBuffer.asIntBuffer();
			longBuffer = nativeBuffer.asLongBuffer();
			shortBuffer = nativeBuffer.asShortBuffer();
			floatBuffer = nativeBuffer.asFloatBuffer();
			doubleBuffer = nativeBuffer.asDoubleBuffer();
		}
	}
	
	public static ByteBuffer getCachedByteBuffer(int size){
		if(size < 0)
			throw new IllegalArgumentException("size < 0, size = " + size);
		if(size > nativeBuffer.capacity())
			remolloc(size);
		
		nativeBuffer.position(0).limit(size);
		return nativeBuffer;
	}
	
	public static ByteBuffer[] getCachedByteBuffer(int[] bufSize){
		ByteBuffer[] bufs = new ByteBuffer[bufSize.length];
		
		int totalSize = 0;
		for(int i = 0; i < bufSize.length;i++){
			if(bufSize[i] < 0)
				throw new IllegalArgumentException("size < 0, index = " + i +", size = " + bufSize[i]);
			totalSize += bufSize[i];
		}
		
		if(totalSize > nativeBuffer.capacity())
			remolloc(totalSize); // make sure there have enough capacity.
		
		int offset = 0;
		for(int i = 0; i < bufSize.length; i++){
			nativeBuffer.position(offset).limit(offset + bufSize[i]);
			bufs[i] = nativeBuffer.slice();
		}
		
		nativeBuffer.clear();
		return bufs;
	}
	
	public static FloatBuffer getCachedFloatBuffer(int size){
		if(size < 0)
			throw new IllegalArgumentException("size < 0, size = " + size);
		if(size > floatBuffer.capacity())
			remolloc(size << 2);
		
		floatBuffer.position(0).limit(size);
		return floatBuffer;
	}
	
	public static IntBuffer getCachedIntBuffer(int size){
		if(size < 0)
			throw new IllegalArgumentException("size < 0, size = " + size);
		if(size > intBuffer.capacity())
			remolloc(size << 2);
		
		intBuffer.position(0).limit(size);
		return intBuffer;
	}
	
	public static ShortBuffer getCachedShortBuffer(int size){
		if(size < 0)
			throw new IllegalArgumentException("size < 0, size = " + size);
		if(size > shortBuffer.capacity())
			remolloc(size << 1);
		
		shortBuffer.position(0).limit(size);
		return shortBuffer;
	}
	
	public static DoubleBuffer getCachedDoubleBuffer(int size){
		if(size < 0)
			throw new IllegalArgumentException("size < 0, size = " + size);
		if(size > doubleBuffer.capacity())
			remolloc(size << 3);
		
		doubleBuffer.position(0).limit(size);
		return doubleBuffer;
	}
	
	public static LongBuffer getCachedLongBuffer(int size){
		if(size < 0)
			throw new IllegalArgumentException("size < 0, size = " + size);
		if(size > longBuffer.capacity())
			remolloc(size << 3);
		
		longBuffer.position(0).limit(size);
		return longBuffer;
	}
	
	public static IntBuffer wrap(int i){
		intBuffer.clear();
		intBuffer.put(i).flip();
		return intBuffer;
	}
	
	public static FloatBuffer wrap(float f){
		floatBuffer.clear();
		floatBuffer.put(f).flip();
		return floatBuffer;
	}

	public static FloatBuffer wrap(ReadableVector4f v){
		return wrap(v.getX(), v.getY(), v.getZ(), v.getW());
	}
	
	public static FloatBuffer wrap(float x, float y, float z, float w){
		FloatBuffer buffer = getCachedFloatBuffer(4);
		buffer.put(x).put(y).put(z).put(w).flip();
		return buffer;
	}
	
	public static IntBuffer wrap(int x, int y, int z, int w){
		IntBuffer buffer = getCachedIntBuffer(4);
		buffer.put(x).put(y).put(z).put(w).flip();
		return buffer;
	}

	public static FloatBuffer wrap(float[] data, int offset, int length){
		FloatBuffer buffer = getCachedFloatBuffer(length);
		buffer.put(data, offset, length).flip();
		return buffer;
	}
	
	public static IntBuffer wrap(int[] data, int offset, int length){
		IntBuffer buffer = getCachedIntBuffer(length);
		buffer.put(data, offset, length).flip();
		return buffer;
	}
	
	public static FloatBuffer wrap(float[] data){
		FloatBuffer buffer = getCachedFloatBuffer(data.length);
		buffer.put(data, 0, data.length).flip();
		return buffer;
	}
	
	public static ShortBuffer wrap(short[] data, int offset, int length){
		ShortBuffer buffer = getCachedShortBuffer(length);
		buffer.put(data, offset, length).flip();
		return buffer;
	}
	
	public static IntBuffer wrap(int[] data){
		IntBuffer buffer = getCachedIntBuffer(data.length);
		buffer.put(data, 0, data.length).flip();
		return buffer;
	}
	
	public static LongBuffer wrap(long[] data){
		LongBuffer buffer = getCachedLongBuffer(data.length);
		buffer.put(data, 0, data.length).flip();
		return buffer;
	}
	
	public static LongBuffer wrap(long[] data, int offset, int length){
		LongBuffer buffer = getCachedLongBuffer(length);
		buffer.put(data, offset, length).flip();
		return buffer;
	}
	
	public static DoubleBuffer wrap(double[] data){
		DoubleBuffer buffer = getCachedDoubleBuffer(data.length);
		buffer.put(data, 0, data.length).flip();
		return buffer;
	}
	
	public static DoubleBuffer wrap(double[] data, int offset, int length){
		DoubleBuffer buffer = getCachedDoubleBuffer(length);
		buffer.put(data, offset, length).flip();
		return buffer;
	}
	
	public static FloatBuffer wrap(float[][] data){
		int totalSize = 0;
		for(int i = 0; i < data.length;i++)
			totalSize += data[i].length;
		
		FloatBuffer buffer = getCachedFloatBuffer(totalSize);
		for(int i = 0; i < data.length;i++)
		  buffer.put(data[i], 0, data[i].length);
		buffer.flip();
		return buffer;
	}
	
	public static ShortBuffer wrap(short[] data){
		ShortBuffer buffer = getCachedShortBuffer(data.length);
		buffer.put(data, 0, data.length).flip();
		return buffer;
	}

	public static ByteBuffer wrapToBytes(short[] data){
		wrap(data);
		return getCachedByteBuffer(data.length * 2);
	}

	public static ByteBuffer wrapToBytes(int[] data){
		wrap(data);
		return getCachedByteBuffer(data.length * 4);
	}

	public static ByteBuffer wrapToBytes(float[] data){
		wrap(data);
		return getCachedByteBuffer(data.length << 2);
	}

	public static ByteBuffer wrapToBytes(long[] data){
		wrap(data);
		return getCachedByteBuffer(data.length << 3);
	}

	public static ByteBuffer wrapToBytes(double[] data){
		wrap(data);
		return getCachedByteBuffer(data.length << 3);
	}
	
	public static ByteBuffer wrap(byte[] data, int offset, int length){
		ByteBuffer buffer = getCachedByteBuffer(length);
		buffer.put(data, offset, length).flip();
		return buffer;
	}
	
	public static ByteBuffer wrap(byte[] data){
		ByteBuffer buffer = getCachedByteBuffer(data.length);
		buffer.put(data, 0, data.length).flip();
		return buffer;
	}
	
	public static ByteBuffer wrap(CharSequence str){
		int length = str.length();
		ByteBuffer buffer = getCachedByteBuffer(length);
		
		for(int i = 0; i < str.length(); i++)
			buffer.put((byte)str.charAt(i));
		buffer.flip();
		return buffer;
	}
	
	public static ByteBuffer wrap(String str, String charset){
		byte[] data = null;
		try {
			data = str.getBytes(charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return wrap(data);
	}
	
	public static FloatBuffer wrap(Matrix4f mat){
		FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(16);
		mat.store(buffer);
		buffer.flip();
		return buffer;
	}

	public static FloatBuffer wrap(Matrix4f[] mats){
		FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(16 * mats.length);
		for(int i= 0; i < mats.length; i++){
			mats[i].store(buffer);
		}
		buffer.flip();
		return buffer;
	}
	
	public static FloatBuffer wrap(Matrix3f mat){
		FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(9);
		mat.store(buffer);
		buffer.flip();
		return buffer;
	}
	
	public static FloatBuffer wrap(Matrix2f mat){
		FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(4);
		mat.store(buffer);
		buffer.flip();
		return buffer;
	}

	public static FloatBuffer wrap(List<Vector3f> vertexs) {
		FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(3 * vertexs.size());
		for(Vector3f v : vertexs)
			v.store(buffer);
		buffer.flip();
		
		return buffer;
	}

	public static FloatBuffer wrap(Vector2f[] vec){
		int size = vec == null ? 0 : vec.length;
		FloatBuffer buffer = getCachedFloatBuffer(size * 2);
		for(int i = 0; i < size; i++){
			if(vec[i] != null)
				vec[i].store(buffer);
			else
				buffer.put(0).put(0);
		}

		buffer.flip();
		return buffer;
	}
	
	public static FloatBuffer wrap(Vector3f[] vec){
		int size = vec == null ? 0 : vec.length;
		FloatBuffer buffer = getCachedFloatBuffer(size * 3);
		for(int i = 0; i < size; i++){
			if(vec[i] != null)
				vec[i].store(buffer);
			else
				buffer.put(0).put(0).put(0);
		}
		
		buffer.flip();
		return buffer;
	}

	public static FloatBuffer wrap(Vector4f[] vec){
		int size = vec == null ? 0 : vec.length;
		FloatBuffer buffer = getCachedFloatBuffer(size * 4);
		for(int i = 0; i < size; i++){
			if(vec[i] != null)
				vec[i].store(buffer);
			else
				buffer.put(0).put(0).put(0).put(0);
		}

		buffer.flip();
		return buffer;
	}
	
	public static ByteBuffer wrap(Buffer buf){
		if(buf == null){
			ByteBuffer result = getCachedByteBuffer(0);
			return result;
		}
		
		ByteBuffer result = null;
		buf.mark();
		if(buf instanceof ByteBuffer){
			result = getCachedByteBuffer(buf.remaining());
			result.put((ByteBuffer)buf).flip();
		}else if(buf instanceof ShortBuffer){
			result = getCachedByteBuffer(buf.remaining() << 1);
			ShortBuffer proxyBuf = getCachedShortBuffer(buf.remaining());
			proxyBuf.put((ShortBuffer)buf).flip();
		}else if(buf instanceof IntBuffer){
			result = getCachedByteBuffer(buf.remaining() << 2);
			IntBuffer proxyBuf = getCachedIntBuffer(buf.remaining());
			proxyBuf.put((IntBuffer)buf).flip();
		}else if(buf instanceof LongBuffer){
//			return length << 3;
			result = getCachedByteBuffer(buf.remaining() << 3);
			LongBuffer proxyBuf = getCachedLongBuffer(buf.remaining());
			proxyBuf.put((LongBuffer)buf).flip();
		}else if(buf instanceof FloatBuffer){
//			return length << 2;
			result = getCachedByteBuffer(buf.remaining() << 2);
			FloatBuffer proxyBuf = getCachedFloatBuffer(buf.remaining());
			proxyBuf.put((FloatBuffer)buf).flip();
		}else if(buf instanceof DoubleBuffer){
//			return length << 3;
			result = getCachedByteBuffer(buf.remaining() << 3);
			DoubleBuffer proxyBuf = getCachedDoubleBuffer(buf.remaining());
			proxyBuf.put((DoubleBuffer)buf).flip();
		}else if(buf instanceof CharBuffer){
			throw new IllegalArgumentException("Unkown support the CharBuffer so far.");
		}
		buf.reset();  // reset the position.
		
		if(result == null)
			result = getCachedByteBuffer(0);
		
		return result;
	}
	
	public static int sizeOf(Buffer buf){
		if(buf == null)
			return 0;
		
		int length = buf.remaining();
		if(buf instanceof ByteBuffer){
			return length;
		}else if(buf instanceof ShortBuffer){
			return length << 1;
		}else if(buf instanceof IntBuffer){
			return length << 2;
		}else if(buf instanceof LongBuffer){
			return length << 3;
		}else if(buf instanceof FloatBuffer){
			return length << 2;
		}else if(buf instanceof DoubleBuffer){
			return length << 3;
		}else if(buf instanceof CharBuffer){
			return length << 1;
		}else
			throw new IllegalArgumentException("Unkown type buffer: " + buf.getClass().getName());
		
	}

	public static ByteBuffer wrapPrimitiveArray(Object data) {
		if(data == null)
			return getCachedByteBuffer(0);

		Class<?> clz = data.getClass();
		if(!clz.isArray()){
			throw  new IllegalArgumentException("data is not an array!");
		}

		Class<?> cmp_type = clz.getComponentType();
		if(cmp_type.isPrimitive() || cmp_type.getSuperclass() == Number.class){
			if(data instanceof byte[]){
				return wrap((byte[])data);
			}else if(data instanceof short[]){
				return wrapToBytes((short[])data);
			}else if(data instanceof int[]){
				return wrapToBytes((int[])data);
			}else if(data instanceof float[]){
				return wrapToBytes((float[])data);
			}else if(data instanceof long[]){
				return wrapToBytes((long[])data);
			}else if(data instanceof double[]){
				return wrapToBytes((double[])data);
			}else{
				throw new IllegalArgumentException("Unrecoginizd the type: " + cmp_type.getName());
			}
		}else{
			throw new IllegalArgumentException("can't recogine the componemt of the data");
		}
	}

	public static ByteBuffer put(ByteBuffer dest, float[] src){
		for(int i = 0; i < src.length; i++){
			dest.putFloat(src[i]);
		}

		return dest;
	}

	public static ByteBuffer put(ByteBuffer dest, int[] src){
		for(int i = 0; i < src.length; i++){
			dest.putInt(src[i]);
		}

		return dest;
	}

	public static ByteBuffer put(ByteBuffer dest, Readable[] src){
		for(int i = 0; i < src.length; i++){
			src[i].store(dest);
		}

		return dest;
	}
}
