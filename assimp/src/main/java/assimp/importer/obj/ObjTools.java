package assimp.importer.obj;

import java.nio.ByteBuffer;

import assimp.common.AssUtil;

class ObjTools {
	
	static final int BUFFERSIZE = 2048;
	
	byte[] m_buffer = new byte[BUFFERSIZE];
	
	ByteBuffer data;
	//!	Data iterator showing to the current position in data buffer
	int m_DataIt;
	//!	Data iterator to end of buffer
	int m_DataItEnd;
	
	//!	Current line in file
	int[] m_uiLine = new int[1];
	
	public ObjTools(ByteBuffer buffer) {
		data = buffer;
		m_DataIt = buffer.position();
		m_DataItEnd = buffer.limit();
	}
	
	/**	@brief	Skips a line
	 *	@param	it		Iterator set to current position
	 *	@param	end		Iterator set to end of scratch buffer for readout
	 *	@param	uiLine	Current linenumber in format
	 *	@return	Current-iterator with new position
	 */
	int skipLine(int it, int end, int[] uiLine){
		while ( !isEndOfBuffer( it, end ) && !isNewLine(data.get(it)) )
			++it;
		if ( it != end )
		{
			++it;
			++uiLine[0];
		}
		// fix .. from time to time there are spaces at the beginning of a material line
		while ( it != end && (data.get(it) == '\t' || data.get(it) == ' ') )
			++it;
		return it;
	}
	
	static boolean isEndOfBuffer(  int it, int end )
	{
		if ( it == end )
		{
			return true;
		}
		else
		{
			end--;
		}
		return ( it == end );	
	}
	
	/**	
	 *  Returns true, fi token id a new line marking token.
	 *	@param	token	Token to search in
	 *	@return	true, if token is a newline token.
	 */
	static boolean isNewLine( byte token )
	{
		return ( token == '\n' || token == '\f' || token == '\r' );
	}
	
	/** Returns pointer a next token
	 *	@param	pBuffer	Pointer to data buffer
	 *	@param	pEnd	Pointer to end of buffer
	 *	@return	Pointer to next token
	 */
	static int getNextToken(byte[] data, int pBuffer, int pEnd){
		while ( !isEndOfBuffer( pBuffer, pEnd ) )
		{
			if ( isSeparator( data[pBuffer] ) )
				break;
			pBuffer++;
		}
		return getNextWord( data, pBuffer, pEnd );
	}
	
	/** Returns pointer a next token
	 *	@param	pBuffer	Pointer to data buffer
	 *	@param	pEnd	Pointer to end of buffer
	 *	@return	Pointer to next token
	 */
	int getNextToken(int pBuffer, int pEnd){
		while ( !isEndOfBuffer( pBuffer, pEnd ) )
		{
			if ( isSeparator( data.get(pBuffer) ) )
				break;
			pBuffer++;
		}
		return getNextWord(pBuffer, pEnd );
	}
	
	/** @brief	Returns true, if token is a space on any supported platform
	*	@param	token	Token to search in
	*	@return	true, if token is a space			
	*/
    static boolean isSeparator(byte token )
	{
		return ( token == ' ' || 
				token == '\n' || 
				token == '\f' || 
				token == '\r' ||
				token == '\t' );
	}
    
    /**	@brief	Returns next word separated by a space
	 *	@param	pBuffer	Pointer to data buffer
	 *	@param	pEnd	Pointer to end of buffer
	 *	@return	Pointer to next space
	 */
	static int getNextWord( byte[] data, int pBuffer, int pEnd )
	{
		while ( !isEndOfBuffer( pBuffer, pEnd ) )
		{
			if ( !isSeparator( data[pBuffer] ) || isNewLine( data[pBuffer] ) )
				break;
			pBuffer++;
		}
		return pBuffer;
	}
	
	/**	@brief	Returns next word separated by a space
	 *	@param	pBuffer	Pointer to data buffer
	 *	@param	pEnd	Pointer to end of buffer
	 *	@return	Pointer to next space
	 */
	int getNextWord(int pBuffer, int pEnd )
	{
		while ( !isEndOfBuffer( pBuffer, pEnd ) )
		{
			if ( !isSeparator( data.get(pBuffer) ) || isNewLine( data.get(pBuffer) ) )
				break;
			pBuffer++;
		}
		return pBuffer;
	}
	
	/**	Get next word from given line
	 *	@param	it		set to current position
	 *	@param	end		set to end of scratch buffer for readout
	 *	@param	pBuffer	Buffer for next word
	 *	@param	length	Buffer length
	 *	@return	Current-iterator with new position
	 */
	int copyNextWord( int it, int end, byte[] pBuffer, int length )
	{
		int index = 0;
		it = getNextWord( it, end );
		while ( !isSeparator(data.get(it) ) && !isEndOfBuffer( it, end ) )
		{
			pBuffer[index] = data.get(it) ;
			index++;
			if (index == length-1)
				break;
			++it;
		}
		pBuffer[ index ] = '\0';
		return it;
	}
	
	/**	@brief	Get next float from given line
	 *	@param	it		set to current position
	 *	@param	end		set to end of scratch buffer for readout
	 *	@param	value	Separated float value.
	 *	@return	Current-iterator with new position
	 */
     int getFloat( int it, int end, float[] value)
	{
//		static const size_t BUFFERSIZE = 1024;
//		char buffer[ BUFFERSIZE ];
		it = copyNextWord( it, end, m_buffer, BUFFERSIZE );
		value[0] = AssUtil.fast_atof( m_buffer );

		return it;
	}
     
   ///	Method to copy the new delimited word in the current line.
 	void copyNextWord(byte[] pBuffer, int length){
 		int index = 0;
 		m_DataIt = getNextWord(m_DataIt, m_DataItEnd);
 		while ( m_DataIt != m_DataItEnd && !isSeparator(data.get(m_DataIt)) )
 		{
 			pBuffer[index] = data.get(m_DataIt);
 			index++;
 			if (index == length-1)
 				break;
 			++m_DataIt;
 		}

// 		ai_assert(index < length);
 		pBuffer[index] = '\0';
 	}
 	///	Method to copy the new line.
 	void copyNextLine(byte[] pBuffer, int length){
 		int index = 0;

 		// some OBJ files have line continuations using \ (such as in C++ et al)
 		boolean continuation = false;
 		for (;m_DataIt != m_DataItEnd && index < length-1; ++m_DataIt) 
 		{
 			final byte c = data.get(m_DataIt);
 			if (c == '\\') {
 				continuation = true;
 				continue;
 			}
 			
 			if (c == '\n' || c == '\r') {
 				if(continuation) {
 					pBuffer[ index++ ] = ' ';
 					continue;
 				}
 				break;
 			}

 			continuation = false;
 			pBuffer[ index++ ] = c;
 		}
// 		ai_assert(index < length);
 		pBuffer[ index ] = '\0';
 	}
 	
 	static boolean isLineEnd( byte in)
 	{
 		return (in == '\r' || in == '\n' || in == '\0');
 	}
 	
 	/**	Get a name from the current line. Preserve space in the middle,
	 *    but trim it at the end.
	 *	@param	it		set to current position
	 *	@param	end		set to end of scratch buffer for readout
	 *	@param	name	Separated name
	 *	@return	Current-iterator with new position
	 */
	int getName( int it, int end, String[] name )
	{
		if ( isEndOfBuffer( it, end ) )
			return end;
		
//		char *pStart = &( *it );
		int pStart = it;
		while ( !isEndOfBuffer( it, end ) && !isNewLine(data.get(it) ) ) {
			++it;
		}

		while(isEndOfBuffer( it, end ) || isNewLine( data.get(it) ) || isSeparator(data.get(it))) {
			--it;
		}
		++it;

		// Get name
		// if there is no name, and the previous char is a separator, come back to start
		while (it < pStart) {
			++it;
		}
//		std::string strName( pStart, &(*it) );
		String strName = getName(pStart, it);
		if ( strName.isEmpty())
			return it;
		else if(name != null)
			name[0]= strName;
		
		return it;
	}
	
	String getName(int start, int end){
		int pos = data.position();
		data.position(start);
		data.get(m_buffer, 0, end - start);
		data.position(pos);
		return new String(m_buffer, 0, end - start);
	}
}
