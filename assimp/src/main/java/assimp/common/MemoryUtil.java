package assimp.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class MemoryUtil {

	public static FloatBuffer createFloatBuffer(int count, boolean natived){
		if(natived)
			return ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		else
			return FloatBuffer.allocate(count);
	}
	
	public static ByteBuffer createByteBuffer(int count, boolean natived){
		if(natived)
			return ByteBuffer.allocateDirect(count).order(ByteOrder.nativeOrder());
		else
			return ByteBuffer.allocate(count).order(ByteOrder.nativeOrder());
	}
	
	public static IntBuffer createIntBuffer(int count, boolean natived){
		if(natived)
			return ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
		else
			return IntBuffer.allocate(count);
	}
	
	public static void arraycopy(FloatBuffer src, int src_offset, FloatBuffer dst, int dst_offset, int length){
		for(int i = 0; i < length; i++){
			dst.put(dst_offset + i, src.get(src_offset + i));
		}
	}
	
	public static FloatBuffer enlarge(FloatBuffer src, int count){
		if(src.remaining() > count)
			return src;
		
		if(src.capacity() -src.position() >= count){
			src.limit(src.capacity());
			return src;
		}
		
		int position = src.position();
		int capacity = src.capacity();
		
		while(capacity - position < count){
			capacity *= 2;
			if(capacity < 0)
				throw new OutOfMemoryError("The buffer too large!");
		}
		
		src.flip();
		FloatBuffer newBuf = createFloatBuffer(capacity, src.isDirect());
		newBuf.put(src);
		return newBuf;
	}
	
	public static FloatBuffer refCopy(FloatBuffer src, boolean natived){
		if(AssUtil.isEmpty(src))
			return null;
		
		if((src.isDirect() && natived) || (!src.isDirect() && !natived)){
			return src.slice();
		}else{
			FloatBuffer dst = createFloatBuffer(src.remaining(), natived);
			int src_pos = src.position();
			dst.put(src).flip();
			src.flip().position(src_pos);
			return dst;
		}
	}
	
	public static FloatBuffer subBuffer(FloatBuffer buffer, int start, int end){
		int oldpos = buffer.position();
		int oldlim = buffer.limit();
		
		buffer.position(start);
		buffer.limit(end);
		
		FloatBuffer newBuf = buffer.slice();
		buffer.position(oldpos);
		buffer.limit(oldlim);
		return newBuf;
	}
}
