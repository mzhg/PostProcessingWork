package assimp.importer.fbx;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import assimp.common.AssUtil;
import assimp.common.DeadlyImportError;

final class FBXBinaryTokenizer {
	
	static final int BLOCK_SENTINEL_LENGTH = 13;

	private FBXBinaryTokenizer(){}
	
	// signal tokenization error, this is always unrecoverable. Throws DeadlyImportError.
	static void tokenizeError(String message, int offset)
	{
		throw new DeadlyImportError(FBXUtil.addOffset("FBX-Tokenize",message,offset));
	}
	
	static void tokenizeError(String message,  ByteBuffer buffer/*const char* begin, const char* cursor*/)
	{
		tokenizeError(message, buffer.position());
	}
	
	static void tokenizeBinary(ArrayList<Token> output_tokens, ByteBuffer input){
		int length = input.remaining();
		if(length < 0x1b) {
			tokenizeError("file is too short",0);
		}

//			const char* cursor = input + 0x1b;
//
//			while (cursor < input + length) {
//				if(!ReadScope(output_tokens, input, cursor, input + length)) {
//					break;
//				}
//			}
		input.position(input.position() + 0x1b);
		while(input.remaining() > 0){
			if(!readScope(output_tokens, input)){
				break;
			}
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	static int readWord(ByteBuffer input/*const char* input, const char*& cursor, const char* end*/)
	{
		if(/*Offset(cursor, end)*/ input.remaining() < 4) {
			tokenizeError("cannot ReadWord, out of bounds",input);
		} 

		int word = /**reinterpret_cast<final int*>(cursor)*/ input.getInt();
//		AI_SWAP4(word);

//		cursor += 4;
		assert word > 0;
		return word;
	}
	
	// ------------------------------------------------------------------------------------------------
	static int readByte(ByteBuffer input/*const char* input, const char*& cursor, const char* end*/)
	{
		if(/*Offset(cursor, end)*/input.remaining() < 1) {
			tokenizeError("cannot ReadByte, out of bounds",input/*, cursor*/);
		} 

//		uint8_t word = *reinterpret_cast<const uint8_t*>(cursor);
//		++cursor;
		byte word = input.get();

		return word & 0xFF;
	}

	// ------------------------------------------------------------------------------------------------
	static long readString(ByteBuffer input, /*const char*& sbegin_out, const char*& send_out, const char* input, const char*& cursor, const char* end, */
		boolean long_length /*= false*/,
		boolean allow_null /*= false*/)
	{
		final int len_len = long_length ? 4 : 1;
		if(/*Offset(cursor, end)*/input.remaining() < len_len) {
			tokenizeError("cannot ReadString, out of bounds reading length",input/*, cursor*/);
		} 

		final int length = long_length ? readWord(input/*, cursor, end*/) : readByte(input/*, cursor, end*/);

		if (/*Offset(cursor, end)*/input.remaining() < length) {
			tokenizeError("cannot ReadString, length is out of bounds",input/*, cursor*/);
		}

//		sbegin_out = cursor;
//		cursor += length;
//
//		send_out = cursor;
		
		int first = input.position();
		int second = length;
		long returnValue = AssUtil.encode(first, second);

		if(!allow_null) {
			for (int i = 0; i < length; ++i) {
				if(/*sbegin_out[i]*/input.get() == '\0') {
					tokenizeError("failed ReadString, unexpected NUL character in string",input);
				}
			}
		}else{
			input.position(length + input.position());
		}

		return returnValue;
	}
	
	// ------------------------------------------------------------------------------------------------
	static long readData(ByteBuffer input/*const char*& sbegin_out, const char*& send_out, const char* input, const char*& cursor, const char* end*/)
	{
		if(/*Offset(cursor, end)*/input.remaining() < 1) {
			tokenizeError("cannot ReadData, out of bounds reading length",input/*, cursor*/);
		} 

//		const char type = *cursor;
//		sbegin_out = cursor++;
		char type = (char)input.get();
		int sbegin_out = input.position();
		int cursor = 0;
		switch(type)
		{
			// 16 bit int
		case 'Y':
			cursor += 2;
			break;

			// 1 bit bool flag (yes/no)
		case 'C':
			cursor += 1;
			break;

			// 32 bit int
		case 'I':
			// <- fall thru

			// float
		case 'F':
			cursor += 4;
			break;

			// double
		case 'D':
			cursor += 8;
			break;

			// 64 bit int
		case 'L':
			cursor += 8;
			break;

			// note: do not write cursor += ReadWord(...cursor) as this would be UB

			// raw binary data
		case 'R':	
		{
//			final int length = ReadWord(input, cursor, end);
			final int length = readWord(input);
			assert length > 0;
			cursor += length;
			break;
		}

		case 'b': 
			// TODO: what is the 'b' type code? Right now we just skip over it /
			// take the full range we could get
//			cursor = end;
			input.position(input.limit());
			break;

			// array of *
		case 'f':
		case 'd':
		case 'l':
		case 'i':	{
		
			final int length = readWord(input/*, cursor, end*/);
			final int encoding = readWord(input/*, cursor, end*/);

			final int comp_len = readWord(input/*, cursor, end*/);

			// compute length based on type and check against the stored value
			if(encoding == 0) {
				int stride = 0;
				switch(type)
				{
				case 'f':
				case 'i':
					stride = 4;
					break;

				case 'd':
				case 'l':
					stride = 8;
					break;

				default:
					assert(false);
				};
	            assert(stride > 0);
				if(length * stride != comp_len) {
					tokenizeError("cannot ReadData, calculated data stride differs from what the file claims",input/*, cursor*/);
				}
			}
			// zip/deflate algorithm (encoding==1)? take given length. anything else? die
			else if (encoding != 1) {			
				tokenizeError("cannot ReadData, unknown encoding",input/*, cursor*/);
			}
			cursor += comp_len;
			break;
		}

			// string
		case 'S': {
//			const char* sb, *se;
			// 0 characters can legally happen in such strings
			readString(/*sb, se,*/ input, /*cursor, end,*/ true, true);
			break;
		}
		default:
			tokenizeError("cannot ReadData, unexpected type code: " + /*std::string(&type, 1)*/ type,input/*, cursor*/);
		}

		if(input.position() + cursor > input.limit()) {
			tokenizeError("cannot ReadData, the remaining size is too small for the data type: " + /*std::string(&type, 1)*/type,input/*, cursor*/);
		} 

		// the type code is contained in the returned range
		input.position(input.position() + cursor);
		int send_out = input.position();
		
		return AssUtil.encode(sbegin_out, send_out);
	}
	
	// ------------------------------------------------------------------------------------------------
	static boolean readScope(ArrayList<Token> output_tokens, ByteBuffer input/*const char* input, const char*& cursor, const char* end*/)
	{
		// the first word contains the offset at which this block ends
		final int end_offset = readWord(input/*, cursor, end*/);
		
		// we may get 0 if reading reached the end of the file -
		// fbx files have a mysterious extra footer which I don't know 
		// how to extract any information from, but at least it always 
		// starts with a 0.
		if(end_offset == 0) {
			return false;
		}

		if(end_offset > /*Offset(input, end)*/ input.remaining()) {
			tokenizeError("block offset is out of range",input/*, cursor*/);
		}
		else if(end_offset < /*Offset(input, cursor)*/input.position()) {
			tokenizeError("block offset is negative out of range",input/*, cursor*/);
		}

		// the second data word contains the number of properties in the scope
		final int prop_count = readWord(input/*, cursor, end*/);

		// the third data word contains the length of the property list
		final int prop_length = readWord(input/*, cursor, end*/);

		// now comes the name of the scope/key
//		const char* sbeg, *send;
		long code = readString(/*sbeg, send, */input/*, cursor, end*/, false, false);
		int sbeg = AssUtil.decodeFirst(code);
		int send = sbeg + AssUtil.decodeSecond(code);
		output_tokens.add(new Token(input, sbeg, send, Token.TokenType_KEY, input.position()/*Offset(input, cursor)*/ ));

		// now come the individual properties
//		const char* begin_cursor = cursor;
		int begin_cursor = input.position();
		input.limit(input.position() + prop_length);
		for (int i = 0; i < prop_count; ++i) {
			code = readData(/*sbeg, send, */input/*, cursor, begin_cursor + prop_length*/);
			sbeg = AssUtil.decodeFirst(code);
			send = sbeg + AssUtil.decodeSecond(code);

			output_tokens.add(new Token(input, sbeg, send, Token.TokenType_DATA, input.position()/*Offset(input, cursor)*/ ));

			if(i != prop_count-1) {
				output_tokens.add(new Token(input, input.position(), input.position() + 1, Token.TokenType_COMMA, input.position()/*Offset(input, cursor)*/ ));
			}
		}

		if (/*Offset(begin_cursor, cursor)*/input.position() - begin_cursor != prop_length) {
			tokenizeError("property length not reached, something is wrong",input/*, cursor*/);
		}

		// at the end of each nested block, there is a NUL record to indicate
		// that the sub-scope exists (i.e. to distinguish between P: and P : {})
		// this NUL record is 13 bytes long.
//	#define BLOCK_SENTINEL_LENGTH 13

		if (/*Offset(input, cursor)*/input.position() < end_offset) {

			if (end_offset - /*Offset(input, cursor)*/input.position() < BLOCK_SENTINEL_LENGTH) {
				tokenizeError("insufficient padding bytes at block end",input/*, cursor*/);
			}

			output_tokens.add(new Token(input, input.position(), input.position() + 1, Token.TokenType_OPEN_BRACKET, input.position()/*Offset(input, cursor)*/ ));

			// XXX this is vulnerable to stack overflowing ..
			while(/*Offset(input, cursor)*/input.position() < end_offset - BLOCK_SENTINEL_LENGTH) {
				input.limit(end_offset - BLOCK_SENTINEL_LENGTH);
				readScope(output_tokens, input/*, cursor, input + end_offset - BLOCK_SENTINEL_LENGTH*/);
			}
			output_tokens.add(new Token(input, input.position(), input.position() + 1, Token.TokenType_CLOSE_BRACKET, input.position() /*Offset(input, cursor)*/ ));

//			for (int i = 0; i < BLOCK_SENTINEL_LENGTH; ++i) {
//				if(cursor[i] != '\0') {
//					TokenizeError("failed to read nested block sentinel, expected all bytes to be 0",input, cursor);
//				}
//			}
//			cursor += BLOCK_SENTINEL_LENGTH;
			input.position(input.position() + BLOCK_SENTINEL_LENGTH);
		}

		if (/*Offset(input, cursor)*/input.position() != end_offset) {
			tokenizeError("scope length not reached, something is wrong",input/*, cursor*/);
		}

		return true;
	}
}
