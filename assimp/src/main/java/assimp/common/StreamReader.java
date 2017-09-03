package assimp.common;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

//--------------------------------------------------------------------------------------------
/** Wrapper class around IOStream to allow for consistent reading of binary data in both 
*  little and big endian format. Don't attempt to instance the template directly. Use 
*  StreamReaderLE to read from a little-endian stream and StreamReaderBE to read from a 
*  BE stream. The class expects that the endianess of any input data is known at 
*  compile-time, which should usually be true (#BaseImporter::ConvertToUTF8 implements
*  runtime endianess conversions for text files). <p>
*
*  XXX switch from unsigned int for size types to size_t? or ptrdiff_t?*/
//--------------------------------------------------------------------------------------------
public class StreamReader implements Closeable{

//	private int current;
//	private int end;
//	private int limit;
	private ByteBuffer data;
	private InputStream in;
	
	public StreamReader(InputStream in, boolean le)  throws IOException{
		this.in = in;
		int count = in.available();
		
		if(Runtime.getRuntime().freeMemory() / count > 8){
			byte[] _data = new byte[count];
			in.read(_data);
			data = ByteBuffer.wrap(_data);
		}else{
			data = ByteBuffer.allocateDirect(count);
			byte[] _data = new byte[8 * 1024];
			while((count = in.read(_data)) > 0){
				data.put(_data, 0, count);
			}
			data.flip();
		}
		
		data.order(le ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
	}
	
	public StreamReader(ByteBuffer buf, boolean le){
		data = buf;
		data.order(le ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
	}
	
	/** Read a float from the stream  
	 * @ */
	public float getF4(){ return data.getFloat();}

	// ---------------------------------------------------------------------
	/** Read a double from the stream  
	 * @ */
	public double getF8(){	return data.getDouble();}

	// ---------------------------------------------------------------------
	/** Read a signed 16 bit integer from the stream 
	 * @ */
	public short getI2() 	{	return data.getShort();}

	// ---------------------------------------------------------------------
	/** Read a signed 8 bit integer from the stream 
	 * @ */
	public byte getI1() 	{	return data.get();}

	// ---------------------------------------------------------------------
	/** Read an signed 32 bit integer from the stream 
	 * @ */
	public int getI4() 	{ return data.getInt();}

	// ---------------------------------------------------------------------
	/** Read a signed 64 bit integer from the stream 
	 * @ */
	public long getI8() 	{ return data.getLong();}
	
	// ---------------------------------------------------------------------
	/** Get the remaining stream size (to the end of the srream) */
	public int getRemainingSize() {
		return data.capacity() - data.position();
	}

	// ---------------------------------------------------------------------
	/** Get the remaining stream size (to the current read limit). The
	 *  return value is the remaining size of the stream if no custom
	 *  read limit has been set. */
	public int getRemainingSizeToLimit()  {
		return (data.limit() - data.position());
	}


	// ---------------------------------------------------------------------
	/** Increase the file pointer (relative seeking)  */
	public void incPtr(int plus)	{
		int current = data.position();
		current += plus;
		if (current > data.limit()) {
			throw new DeadlyImportError("End of file or read limit was reached");
		}else{
			data.position(current);
		}
	}
	
	// ---------------------------------------------------------------------
	/** Get the current file pointer */
	public ByteBuffer getPtr()	{ return data.slice(); }

	// ---------------------------------------------------------------------
	/** Set current file pointer (Get it from #GetPtr). This is if you
	 *  prefer to do pointer arithmetics on your own or want to copy 
	 *  large chunks of data at once. 
	 *  @param p The new pointer, which is validated against the size
	 *    limit and buffer boundaries. */
	public void setPtr(int p)	{
		try {
			data.position(p);
		} catch (Exception e) {
			throw new DeadlyImportError("End of file or read limit was reached");
		}
	}

	// ---------------------------------------------------------------------
	/** Get the current offset from the beginning of the file */
	public int getCurrentPos(){
		return data.position();
	}

	public void setCurrentPos(int pos) {
		setPtr(/*buffer +*/ pos);
	}
	
	/** Copy n bytes to an external buffer
	 *  @param out Destination for copying
	 *  @param offset the offset of destination to write.
	 *  @param len Number of bytes to copy */
	public void copyAndAdvance(byte[] out, int offset, int len){
		data.get(out, offset, len);
	}
	
	/** Copy n bytes to an external buffer
	 *  @param out Destination for copying
	 *  @param offset the offset of destination to write.
	 *  @param len Number of bytes to copy */
	public void copyAndAdvance(ByteBuffer buf){
		buf.put(buf).flip();
	}

	// ---------------------------------------------------------------------
	/** Setup a temporary read limit
	 * 
	 *  @param limit Maximum number of bytes to be read from
	 *    the beginning of the file. Specifying UINT_MAX
	 *    resets the limit to the original end of the stream. */
	public void setReadLimit(int _limit)	{
		if (-1 == _limit) {
//			limit = end;
			data.limit(data.capacity());
			return;
		}

//		limit = /*buffer +*/ _limit;
//		if (limit > end) {
//			throw new DeadlyImportError("StreamReader: Invalid read limit");
//		}
		
		try {
			data.limit(_limit);
		} catch (Exception e) {
			throw new DeadlyImportError("StreamReader: Invalid read limit");
		}
	}

	// ---------------------------------------------------------------------
	/** Get the current read limit in bytes. Reading over this limit
	 *  accidentially raises an exception.  */
	public int getReadLimit(){
		return data.limit();
	}

	// ---------------------------------------------------------------------
	/** Skip to the read limit in bytes. Reading over this limit
	 *  accidentially raises an exception. */
	public void skipToReadLimit()	{
//		current = limit;
		data.position(data.limit());
	}

	@Override
	public void close() throws IOException {
		if(in != null)
			in.close();
	}
}
