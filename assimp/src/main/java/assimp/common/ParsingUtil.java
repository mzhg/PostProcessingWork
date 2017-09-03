package assimp.common;

import java.nio.ByteBuffer;

public class ParsingUtil {

	ByteBuffer buffer;
	int curr;
	
	public ParsingUtil(ByteBuffer buffer) {
		this.buffer = buffer;
	}
	
	public int getCurrent(){ return curr;}
	public void setCurrent(int curr){ 
		if(curr < 0 || curr > buffer.limit())
			throw new IllegalArgumentException();
		
		this.curr = curr;
	}
	
	public boolean skipSpaces(){
		if(curr == buffer.limit())
			return false;
		
		byte c = buffer.get(curr);
		while (c == ' ' || c == '\t')
		{
			curr++;
			if(c < buffer.limit())
				c = buffer.get(curr);
			else
				return false;
		}
		return !isLineEnd(c);
	}
	
	/**
	 * Return the current value ranged from 0 to 255.<br>
	 * If the cursor reached the end, this method will return 0.
	 * @return
	 */
	public int get() {
		if(curr == buffer.limit())
			return 0;
		return buffer.get(curr) & 0xFF;
	}
	
	public int get(int index){
		try {
			return buffer.get(index);
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
	}
	
	/** Return true means the str doesn't equals the current buffer. */
	public boolean strncmp(String str){
		if(str == null || str.isEmpty())
			return true;
		
		if(str.length() > buffer.limit() - curr)
			return true;
		
		for(int i = 0; i < str.length(); i++){
			if(buffer.get(i + curr) != str.charAt(i))
				return true;
		}
		
		return false;
	}
	
	/** Case-ignoring version of strncmp. */
	public boolean strncmpi(String str){
		if(str == null || str.isEmpty())
			return true;
		
		if(str.length() > buffer.limit() - curr)
			return true;
		
		for(int i = 0; i < str.length(); i++){
			char c1 = Character.toUpperCase((char) buffer.get(i + curr));
			char c2 = Character.toUpperCase(str.charAt(i));
			if(c1 != c2)
				return true;
		}
		
		return false;
	}
	
	// ------------------------------------------------------------------------------------
	// Convert a string in octal format to a number
	// ------------------------------------------------------------------------------------
	public int strtoul8()
	{
		int limit = buffer.limit();
		if(curr == limit)
			return 0;
		int value = 0;
		byte _in = buffer.get(curr);
		boolean running = true;
		while ( running )
		{
			if ( _in < '0' || _in > '7' )
				break;

			value = ( value << 3 ) + ( _in - '0' );
			curr++;
			if(curr == limit)
				return value;
			_in = buffer.get(curr);
		}
		return value;
	}

	// ------------------------------------------------------------------------------------
	// Convert a string in hex format to a number
	// ------------------------------------------------------------------------------------
	public int strtoul16()
	{
		int limit = buffer.limit();
		if(curr == limit)
			return 0;
		int value = 0;
		byte _in = buffer.get(curr);
		boolean running = true;
		while ( running )
		{
			if ( _in >= '0' && _in <= '9' )
			{
				value = ( value << 4 ) + ( _in - '0' );
			}
			else if (_in >= 'A' && _in <= 'F')
			{
				value = ( value << 4 ) + ( _in - 'A' ) + 10;
			}
			else if (_in >= 'a' && _in <= 'f')
			{
				value = ( value << 4 ) + ( _in - 'a' ) + 10;
			}
			else break;
			curr++;
			if(curr == limit)
				return value;
			_in = buffer.get(curr);
		}
		
		return value;
	}
	
	public boolean tokenMatch(String token){
		if(!strncmp(token) && isSpaceOrNewLine(buffer.get(curr + token.length()))){
			curr += token.length() + 1;
			return true;
		}
		
		return false;
	}
	
	/** Case-ignoring version of tokenMatch */
	public boolean tokenMatchI(String token){
		if(!strncmpi(token) && isSpaceOrNewLine(buffer.get(curr + token.length()))){
			curr += token.length() + 1;
			return true;
		}
		
		return false;
	}
	
	public static boolean isSpaceOrNewLine(byte c){
		return isSpace(c) || isLineEnd(c);
	}
	
	public static boolean isLineEnd(byte c) {
		return (c == '\r' || c == '\n' || c == '\0');
	}
	
	public static boolean isSpace(byte c) {
		return (c == ' ' || c == '\t' || c == '\n');
	}
	
	public boolean isSpace() { return isSpace(buffer.get(curr));}
	
	public boolean hasNext() { return curr < buffer.limit(); }
	
	public int remaining() { return buffer.limit() - curr;}
	
	public int strtoul10(){
		int value = 0;
		int limit = buffer.limit();
		boolean positive = true;
		boolean first = true;
		while(curr < limit){
			int _in = buffer.get(curr) &0xFF;
			if(first){
				first = false;
				if(_in == '+'){
					positive = true;
					curr++;
					continue;
				}else if(_in =='-'){
					positive = false;
					curr++;
					continue;
				}
			}
			
			if ( _in < '0' || _in > '9' )
				break;

			value = ( value * 10 ) + ( _in - '0' );
			curr++;
		}
		
		return positive ? value : -value;
	}
	
	public boolean ac_skip_to_next_token(){
		if(!skipSpaces()){
			DefaultLogger.error("AC3D: Unexpected EOF/EOL");
			return true;
		}
		return false;
	}
	
	public long strtoul10_64(int count){
		int cur = 0;
		long value = 0;
		int limit = buffer.limit();
		
		if(curr == limit){
			return 0;
		}

		int _in = buffer.get(curr) & 0xFF;
		if ( _in < '0' || _in > '9' )
				throw new NumberFormatException();

		while (curr < limit)
		{
			if ( _in < '0' || _in > '9' )
				break;

			final long new_value = ( value * 10 ) + ( _in - '0' );
			
			if (new_value < value) /* numeric overflow, we rely on you */
				throw new NumberFormatException();

			value = new_value;

//			++in;
			++cur;
			++curr;
			if(curr == limit)
				break;
			_in = buffer.get(curr) & 0xFF;
			
			if (count == cur) {
//				/* skip to end */
				while (_in >= '0' && _in <= '9'){
//					++in;
					++curr; 
					if(curr == limit)
						break;
					_in = buffer.get(curr) & 0xFF;
				}
				return value;
			}
		}
		return value;
	}
	
	public boolean ac_checked_load_float_array(String name, float[] out){
		if(ac_skip_to_next_token())
			return true;
		
		if(name.length() > 0){
			if(strncmp(name) || !isSpace(buffer.get(curr+name.length()))){
				DefaultLogger.error("AC3D: Unexpected EOF/EOL");
				return true;
			}
			
			curr += name.length() + 1;
		}
		
		int num = out.length;
		for (int i = 0; i < num;++i)
		{
//			AssUtil.fast_atoreal_move(c, offset, out_offset, check_comma)
			if(ac_skip_to_next_token())
				return true;
//			buffer = fast_atoreal_move<float>(buffer,((float*)out)[i]);
			out[i] = (float)fast_atoreal_move(true);
		}
		
		return false;
	}
	
	public boolean skipSpacesAndLineEnd(){
		int c = get();
		while (c == ' ' || c == '\t' ||
				c == '\r' || c == '\n'){
			curr++;
			c = get();
		}
		return c != '\0';
	}
	
	public String getString(int len){ return getString(curr, curr + len);}
	
	public String getString(int start, int end){
		int len = end - start;
		if(len == 0)
			return "";
		
		byte[] str = new byte[len];
		buffer.position(start);
		buffer.get(str);
		buffer.position(0);
		return new String(str);
	}
	
	/** Provides a fast function for converting a string into a float,
	 * about 4 times faster than {@link java.lang.Double#parseDouble(String)}.
	 * If you find any bugs, please send them to me, niko (at) irrlicht3d.org.
	 */
	public double fast_atoreal_move(boolean check_comma){
		double f;
		if(curr == buffer.limit())
			return 0.0;
		
		int _c = buffer.get(curr) & 0xFF;
		boolean inv = (_c=='-');
		if (inv || _c=='+') {
//			++c;
			curr++;
			_c = buffer.get(curr) & 0xFF;
		}

		f = strtoul10_64 (15);
		_c = buffer.get(curr) & 0xFF;
		
		int next = 0;
		if(check_comma && _c == ','){
			next = buffer.get(curr + 1) & 0xFF;
		}
		if (_c == '.' || (check_comma && _c == ',' && next >= '0' && next <= '9')) // allow for commas, too
		{
//			++c;
			curr++;
			if(curr < buffer.limit())
				_c = buffer.get(curr) & 0xFF;
			else
				_c = 0;

			// NOTE: The original implementation is highly inaccurate here. The precision of a single
			// IEEE 754 float is not high enough, everything behind the 6th digit tends to be more 
			// inaccurate than it would need to be. Casting to double seems to solve the problem.
			// strtol_64 is used to prevent integer overflow.

			// Another fix: this tends to become 0 for long numbers if we don't limit the maximum 
			// number of digits to be read. AI_FAST_ATOF_RELAVANT_DECIMALS can be a value between
			// 1 and 15.
			int diff =  curr/*AI_FAST_ATOF_RELAVANT_DECIMALS*/;
//			out_and_max[1] = diff;
			double pl = strtoul10_64 (15);
			if(curr < buffer.limit())
				_c = buffer.get(curr) & 0xFF;
//			diff = out_and_max[1];
			
			diff = curr - diff;
			pl *= AssUtil.fast_atof_table[diff];
			f += pl;
		}

		// A major 'E' must be allowed. Necessary for proper reading of some DXF files.
		// Thanks to Zhao Lei to point out that this if() must be outside the if (*c == '.' ..)
		if (_c == 'e' || _c == 'E')	{

//			++c;
			curr++;
			if(curr < buffer.limit())
				_c = buffer.get(curr) & 0xFF;
			else
				throw new NumberFormatException();
			
			final boolean einv = (_c=='-');
			if (einv || _c=='+') {
//				++c;
				curr++;
				if(curr == buffer.limit())
					throw new NumberFormatException();
			}

			// The reason float constants are used here is that we've seen cases where compilers
			// would perform such casts on compile-time constants at runtime, which would be
			// bad considering how frequently fast_atoreal_move<float> is called in Assimp.
			double exp = strtoul10_64(15);
//			offset = out_and_max[0];
//			_c = buffer.get(curr) & 0xFF;
			if (einv) {
				exp = -exp;
			}
			f *= Math.pow(10.0, exp);
		}

		if (inv) {
			f = -f;
		}
		return f;
	}
	
	public boolean ac_get_string(String[] out){
		curr++;
		if(curr >= buffer.limit()) {
			DefaultLogger.error("AC3D: Unexpected EOF/EOL in string");
			out[0] = "ERROR";
			return true;
		}
		
		final int sz = curr;
		byte c = buffer.get(curr);
		while('\"' != c){
			if(isLineEnd(c)){
				DefaultLogger.error("AC3D: Unexpected EOF/EOL in string");
				out[0] = "ERROR";
				break;
			}
			curr ++;
			if(curr == buffer.limit()){
				DefaultLogger.error("AC3D: Unexpected EOF/EOL in string");
				out[0] = "ERROR";
				break;
			}
			c = buffer.get(curr);
		}
		
		if(curr == buffer.limit() || isLineEnd(c)){
			return true;
		}
		
		byte[] name = new byte[curr -sz];
		buffer.position(sz);
		buffer.get(name);
		curr++;
		out[0] = new String(name);
		return false;
	}

	public boolean skipLine(){
		byte c = buffer.get(curr);
		
		while (c != '\r' && c != '\n' && c != '\0'){
			curr++;
			if(c < buffer.limit())
				c = buffer.get(curr);
			else
				return false;
		}

		// files are opened in binary mode. Ergo there are both NL and CR
		while (c == '\r' || c == '\n')
		{
			curr++;
			if(c < buffer.limit())
				c = buffer.get(curr);
			else
				return false;
		}
		
		return curr != buffer.limit();
	}

	public boolean isNumeric() {
		byte in = buffer.get(curr);
		return ( in >= '0' && in <= '9' ) || '-' == in || '+' == in;
	}

	public void deCre() {
		curr -- ;
	}
	
	/** Increase the cursor. Return true means the cursor reach the end. */
	public boolean inCre(){ 
		curr++; 
		if(curr > buffer.limit()){
			curr = buffer.limit();
			return true;
		}
		
		return false;
	}
	
	public void inCre(int count){
		curr += count;
	}
	
	// ------------------------------------------------------------------------------------
	// Parse a C++-like integer literal - hex and oct prefixes.
	// 0xNNNN - hex
	// 0NNN   - oct
	// NNN    - dec
	// ------------------------------------------------------------------------------------
	public int strtoul_cppstyle()
	{
		int limit = buffer.limit();
		if(curr == limit)
			return 0;
		byte _in = buffer.get(curr);
		
		if ('0' == _in)
		{
			curr++;
			if(curr == limit)
				return 0;
			_in = buffer.get(curr);
			if('x' == _in) {
				curr ++;
				return strtoul16();
			}else{
				return strtoul8();
			}
		}
		return strtoul10();
	}
	
	public void skip(int count){
		setCurrent(curr + count);
	}
}
