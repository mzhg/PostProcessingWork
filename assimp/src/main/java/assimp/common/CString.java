package assimp.common;

import java.nio.ByteBuffer;

public class CString implements CharSequence{

	private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
	ByteBuffer buffer = EMPTY_BUFFER;
	
	public CString() {
	}
	
	public CString(String string) {
		buffer = ByteBuffer.wrap(string.getBytes());
	}
	
	public CString(ByteBuffer buffer){
		this.buffer = buffer.slice();
	}
	
	CString(ByteBuffer buffer, Void unused){
		this.buffer = buffer.slice();
	}
	
	public static CString wrap(byte[] chars){
		return new CString(ByteBuffer.wrap(chars, 0, chars.length), null);
	}
	
	public static CString wrap(byte[] chars, int offset, int length){
		return new CString(ByteBuffer.wrap(chars, offset, length), null);
	}
	
	@Override
	public int length() { return buffer.remaining();}

	@Override
	public char charAt(int index) { return (char) buffer.get(index);}

	@Override
	public CString subSequence(int start, int end) {
		return subSequence(buffer, start, end);
	}
	
	public ByteBuffer getData(){ return buffer;}
	
	public static CString subSequence(ByteBuffer buffer, int start, int end){
		int oldpos = buffer.position();
		int limit  = buffer.limit();
		
		if(oldpos != start)
			buffer.position(start);
		if(limit != end)
			buffer.limit(end);
		ByteBuffer buf = buffer.slice();
		if(oldpos != start)
			buffer.position(oldpos);
		if(limit != end)
			buffer.limit(limit);
		
		return new CString(buf, null);
	}
	
	@Override
	public String toString() {
		int length = buffer.remaining();
		if(length == 0)
			return "";
		
		if(buffer.hasArray()){
			return new String(buffer.array(), buffer.arrayOffset(), length);
		}else{
			byte[] bytes = new byte[length];
			int oldpos = buffer.position();
			buffer.get(bytes);
			buffer.position(oldpos);
			return new String(bytes);
		}
	}
	
	public boolean isDirect(){ return buffer != null ? buffer.isDirect() : false;}
	
	public int indexOf(int ch, int fromIndex){
		final int max = buffer.remaining();
        if (fromIndex < 0) {
            fromIndex = 0;
        } else if (fromIndex >= max) {
            // Note: fromIndex might be near -1>>>1.
            return -1;
        }
        
        if(buffer.hasArray()){
        	final byte[] value = buffer.array();
        	final int offset = buffer.arrayOffset();
            for (int i = fromIndex; i < max; i++) {
                if (value[i + offset] == ch) {
                    return i;
                }
            }
        }else{
        	for(int i = fromIndex; i < max; i++){
        		if(buffer.get(i) == ch)
        			return i;
        	}
        }
        
        return -1;
	}
	
	public static void main(String[] args) {
		ByteBuffer buf = ByteBuffer.wrap(new byte[20]);
		buf.position(10);
		ByteBuffer newBuf = buf.slice();
		
		
		System.out.println("old buf:");
		System.out.println("position = " + buf.position() + ", limit = " + buf.limit()+ ", capacity = " + buf.capacity());
		System.out.println();
		System.out.println("new buf: ");
		System.out.println("position = " + newBuf.position() + ", limit = " + newBuf.limit() + ", capacity = " + newBuf.capacity());
	}

}
