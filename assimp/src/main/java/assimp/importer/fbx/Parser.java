package assimp.importer.fbx;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.lwjgl.util.vector.Matrix4f;

import assimp.common.AssUtil;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;

/** FBX parsing class, takes a list of input tokens and generates a hierarchy
 *  of nested #Scope instances, representing the fbx DOM.*/
final class Parser {

	private static ArrayDeque<String> error_stacks;
    private ArrayList<Token> tokens;
	
	private Token last, current;
	private int /*TokenList::const_iterator*/ cursor;
	private Scope root;

	private final boolean is_binary;
	
	/** Parse given a token list. Does not take ownership of the tokens -
	 *  the objects must persist during the entire parser lifetime */
	public Parser(ArrayList<Token> tokens,boolean is_binary) {
		this.tokens = tokens;
		this.is_binary = is_binary;
		root = new Scope(this, true);
	}
	
	static void init(){
		if(error_stacks == null)
			error_stacks = new ArrayDeque<String>();
	}
	
	static void clear(){
		error_stacks = null;
	}
	
	static String get_error(){
		return error_stacks.pollLast();
	}
	
	Token advanceToNextToken(){
		last = current;
		if (cursor == /*tokens.end()*/ tokens.size()) {
			current = null;
		}
		else {
			current = tokens.get(cursor++);
		}
		return current;
	}

	Token lastToken() { return last;}
	Token currentToken() { return current;}
	
	Scope getRootScope() {	return root;}

	boolean isBinary() {	return is_binary;}
	
	static int parseTokenStrInt32(Token t){
		ByteBuffer contents = t.contents();
		int old_position = contents.position();
		contents.position(t.begin());
		final long id = AssUtil.strtoul10_64(contents);
		if (contents.position() != t.end()) {
			error_stacks.add("failed to parse ID (text)");
			contents.position(old_position);
			return 0;
		}
		contents.position(old_position);
		
		return (int) id;
	}
	
	static long parseTokenStrLong64(Token t, int offset){
		ByteBuffer contents = t.contents();
		int old_position = contents.position();
		contents.position(t.begin() + offset);
		final long id = AssUtil.strtoul10_64(contents);
		if (contents.position() > t.end()) {
			error_stacks.add("failed to parse ID (text)");
			contents.position(old_position);
			return 0L;
		}
		contents.position(old_position);
		
		return id;
	}
	
	static double parseTokenStrDouble64(Token t){
		ByteBuffer contents = t.contents();
		int old_position = contents.position();
		contents.position(t.begin());
		final double d = AssUtil.fast_atoreal_move(contents, false);
		if (contents.position() > t.end()) {
			parseError("failed to parse ID (text)", t);
			contents.position(old_position);
			return 0L;
		}
		contents.position(old_position);
		
		return d;
	}
	
	/** token parsing - this happens when building the DOM out of the parse-tree*/
	static long parseTokenAsID(Token t){
		if (t.type() != Token.TokenType_DATA) {
			error_stacks.add("expected TOK_DATA token");
			return 0L;
		}

		if(t.isBinary())
		{
			int /*const char**/ data = t.begin();
			if (t.get(data) != 'L') {
				error_stacks.add("failed to parse ID, unexpected data type, expected L(ong) (binary)");
				return 0L;
			}

//			ai_assert(t.end() - data == 9);

//			BE_NCONST uint64_t id = *reinterpret_cast<const uint64_t*>(data+1);
//			AI_SWAP8(id);
			final long id = t.contents().getLong(data + 1);
			return id;
		}

		// XXX: should use size_t here
//		int length = /*static_cast<unsigned int>*/(t.end() - t.begin());
//		ai_assert(length > 0);

//		const char* out;
//		const uint64_t id = strtoul10_64(t.begin(),&out,&length);
		return parseTokenStrLong64(t, 0);
	}
	
	static int parseTokenAsDim(Token t){
		// same as ID parsing, except there is a trailing asterisk
		if (t.type() != Token.TokenType_DATA) {
			error_stacks.add("expected TOK_DATA token");
			return 0;
		}

		if(t.isBinary())
		{
			int /*const char**/ data = t.begin();
			if (t.get(data) != 'L') {
				error_stacks.add("failed to parse ID, unexpected data type, expected L(ong) (binary)");
				return 0;
			}

//			ai_assert(t.end() - data == 9);
//			BE_NCONST uint64_t id = *reinterpret_cast<const uint64_t*>(data+1);
//			AI_SWAP8(id);
//			return static_cast<size_t>(id);
			
			return (int) t.contents().getLong(data + 1);
		}

		int begin = t.begin();
		if(/**t.begin()*/t.get(begin) != '*') {
			error_stacks.add("expected asterisk before array dimension");
			return 0;
		}

		// XXX: should use size_t here
		int length = /*static_cast<unsigned int>*/(t.end() - t.begin());
		if(length == 0) {
			error_stacks.add("expected valid integer number after asterisk");
			return 0;
		}

//		const char* out;
//		const size_t id = static_cast<size_t>(strtoul10_64(t.begin() + 1,&out,&length));
//		if (out > t.end()) {
//			err_out = "failed to parse ID";
//			return 0;
//		}

		return (int) parseTokenStrLong64(t, 1);
	}

	static float parseTokenAsFloat(Token t){
		if (t.type() != Token.TokenType_DATA) {
			parseError("expected TOK_DATA token", t);
			return 0.0f;
		}

		if(t.isBinary())
		{
			int /*const char**/ data = t.begin();
			char c = t.get(data);
			if (/*data[0]*/c != 'F' && /*data[0]*/c != 'D') {
				parseError("failed to parse F(loat) or D(ouble), unexpected data type (binary)", t);
				return 0.0f;
			}

			if (/*data[0]*/c == 'F') {
				// Actual size validation happens during Tokenization so
				// this is valid as an assertion.
//				ai_assert(t.end() - data == /*sizeof(float)*/ + 1);
				// Initially, we did reinterpret_cast, breaking strict aliasing rules.
				// This actually caused trouble on Android, so let's be safe this time.
				// https://github.com/assimp/assimp/issues/24
				
//				float out_float;
//				::memcpy(&out_float, data+1, sizeof(float));
//				return out_float;
				return t.contents().getFloat(data + 1);
			}
			else {
//				ai_assert(t.end() - data == sizeof(double) + 1);
//				
//				// Same
//				double out_double;
//				::memcpy(&out_double, data+1, sizeof(double));
//				return static_cast<float>(out_double);
				
				return (float) t.contents().getDouble(data + 1);
			}
		}

		// need to copy the input string to a temporary buffer
		// first - next in the fbx token stream comes ',', 
		// which fast_atof could interpret as decimal point.
//	#define MAX_FLOAT_LENGTH 31
//		char temp[MAX_FLOAT_LENGTH + 1];
//		const size_t length = static_cast<size_t>(t.end()-t.begin());
//		std::copy(t.begin(),t.end(),temp);
//		temp[std::min(static_cast<size_t>(MAX_FLOAT_LENGTH),length)] = '\0';
//
//		return fast_atof(temp);
		return (float) parseTokenStrDouble64(t);
	}
	
	// ------------------------------------------------------------------------------------------------
	// signal parse error, this is always unrecoverable. Throws DeadlyImportError.
	static void parseError(String message, Token token)
	{
		throw new DeadlyImportError(FBXUtil.addTokenText("FBX-Parser",message,token));
	}

	// ------------------------------------------------------------------------------------------------
	static void parseError(String message, Element element)
	{
		if(element != null) {
			parseError(message,element.keyToken());
		}
		throw new DeadlyImportError("FBX-Parser " + message);
	}


	// ------------------------------------------------------------------------------------------------
	// print warning, do return
	static void parseWarning(String message, Token token)
	{
		if(DefaultLogger.LOG_OUT) {
			DefaultLogger.warn(FBXUtil.addTokenText("FBX-Parser",message,token));
		}
	}

	// ------------------------------------------------------------------------------------------------
	static void parseWarning(String message, Element element)
	{
		if(element != null) {
			parseWarning(message,element.keyToken());
			return;
		}
		if(DefaultLogger.LOG_OUT) {
			DefaultLogger.warn("FBX-Parser: " + message);
		}
	}
	
	static int parseTokenAsIntSafe(Token t){
		int i = parseTokenAsInt(t);
		String error = get_error();
		if(error != null)
			parseError(error, t);
		return i;
	}
	
	static float parseTokenAsFloatSafe(Token t){
		float f = parseTokenAsFloat(t);
		String error = get_error();
		if(error != null)
			parseError(error, t);
		return f;
	}
	
	static int parseTokenAsInt(Token t){
		if(t.type() == Token.TokenType_DATA){
			error_stacks.add("expected TOK_DATA token");
			return 0;
		}
		
		if(t.isBinary()){
			int data = t.begin();
			if(t.get(data) != 'I'){
				error_stacks.add("failed to parse I(nt), unexpected data type (binary)");
				return 0;
			}
			
			return t.contents().getInt(data + 1);
		}
		
		return parseTokenStrInt32(t);
	}
	
	static String parseTokenAsStringSafe(Token t){
		String s = parseTokenAsString(t);
		String error = get_error();
		if(error != null)
			parseError(error, t);
		return s;
	}
	
	static long parseTokenAsIDSafe(Token t){
		long l = parseTokenAsID(t);
		String error = get_error();
		if(error != null)
			parseError(error, t);
		return l;
	}
	
	static String parseTokenAsString(Token t){
		if(t.type() == Token.TokenType_DATA){
			error_stacks.add("expected TOK_DATA token");
			return "";
		}
		
		if(t.isBinary()){
			int data = t.begin();
			if(t.get(data) != 'S'){
				error_stacks.add("failed to parse S(tring), unexpected data type (binary)");
				return "";
			}
			
			ByteBuffer buf = t.contents();
			// read string length
			int len = buf.getInt(data + 1);
			return AssUtil.getString(buf, data + 5, len);
		}
		
		final int length = t.end() - t.begin();
		if(length < 2) {
			error_stacks.add("token is too short to hold a string");
			return "";
		}

		int s = t.begin(), e = t.end() - 1;
		if (t.get(s) != '\"' || t.get(e) != '\"') {
			error_stacks.add("expected double quoted string");
			return "";
		}

//		return std::string(s+1,length-2);
		return AssUtil.getString(t.contents(), s+1, e - 1);
	}
	
	// read the type code and element count of a binary data array and stop there
	static long readBinaryDataArrayHead(ByteBuffer data,  /*char& type, uint32_t& count,*/ Element el)
	{
		if (/*static_cast<size_t>*/(/*end-data.position()*/data.remaining()) < 5) {
			parseError("binary data array is too short, need five (5) bytes for type signature and element count",el);
		}

		// data type
//		type = *data;
		int type = data.get() & 0xFF;

		// read number of elements
//		BE_NCONST uint32_t len = *reinterpret_cast<const uint32_t*>(data+1);
//		AI_SWAP4(len);

//		count = len;
//		data += 5;
		int count = data.getInt();
		return AssUtil.encode(type, count);
	}
	
	// read binary data array, assume cursor points to the 'compression mode' field (i.e. behind the header)
	static boolean readBinaryDataArray(int type, int count, ByteBuffer data, /*const char*int end, */ByteArrayList buff, 
		Element el)
	{
		/*ai_assert(static_cast<size_t>(end-data) >= 4)*/ assert data.remaining() >= 4; // runtime check for this happens at tokenization stage
//
//		BE_NCONST uint32_t encmode = *reinterpret_cast<const uint32_t*>(data);
//		AI_SWAP4(encmode);
//		data += 4;
//
//		// next comes the compressed length
//		BE_NCONST uint32_t comp_len = *reinterpret_cast<const uint32_t*>(data);
//		AI_SWAP4(comp_len);
//		data += 4;
//
//		ai_assert(data + comp_len == end);
		final int encmode = data.getInt();
		final int comp_len = data.getInt();
		assert data.position() + comp_len == data.limit();

		// determine the length of the uncompressed data by looking at the type signature
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
//			ai_assert(false);
			break;
		};

		final int full_length = stride * count;

		if(encmode == 0) {
//			ai_assert(full_length == comp_len);

			// plain data, no compression
//			std::copy(data, end, buff.begin());
			return false;
		}
		else if(encmode == 1) {
			// zlib/deflate, next comes ZIP head (0x78 0x01)
			// see http://www.ietf.org/rfc/rfc1950.txt
			
//			z_stream zstream;
//			zstream.opaque = Z_NULL;
//			zstream.zalloc = Z_NULL;
//			zstream.zfree  = Z_NULL;
//			zstream.data_type = Z_BINARY;
//
//			// http://hewgill.com/journal/entries/349-how-to-decompress-gzip-stream-with-zlib
//			inflateInit(&zstream);
//
//			zstream.next_in   = reinterpret_cast<Bytef*>( const_cast<char*>(data) );
//			zstream.avail_in  = comp_len;
//
//			zstream.avail_out = buff.size();
//			zstream.next_out = reinterpret_cast<Bytef*>(&*buff.begin());
//			const int ret = inflate(&zstream, Z_FINISH);
//
//			if (ret != Z_STREAM_END && ret != Z_OK) {
//				ParseError("failure decompressing compressed data section");
//			}
//
//			// terminate zlib
//			inflateEnd(&zstream);
			
			buff.size(full_length);
			int offset = data.position();
			byte[] bytes = null;
			if(data.hasArray()){
				bytes = data.array();
			}else{
				bytes = new byte[comp_len];
				data.get(bytes);
			}
			try(ByteArrayInputStream bytesReader = new ByteArrayInputStream(bytes, offset, comp_len);
					ZipInputStream in = new ZipInputStream(bytesReader)){
				in.read(buff.elements());
			} catch (IOException e) {
				// 
			}
			
			return true;
		}
//	#ifdef ASSIMP_BUILD_DEBUG
		else {
			// runtime check for this happens at tokenization stage
//			ai_assert(false);
//			assert false;
			throw new DeadlyImportError("invalid encode mode: " + encmode);
		}
//	#endif

//		data += comp_len;
//		ai_assert(data == end);
	}

	/* wrapper around ParseTokenAsXXX() with DOMError handling */
//	uint64_t ParseTokenAsID(Token t);
//	size_t ParseTokenAsDim(Token t);
//	float ParseTokenAsFloat(Token t);
//	int ParseTokenAsInt(Token t);
//	std::string ParseTokenAsString(Token t);

	/* read data arrays */
	static void parseVectorDataArray3f(FloatArrayList out, Element el){
		out.clear();

		List<Token> tok = el.tokens();
		if(tok == null || tok.isEmpty()) {
			parseError("unexpected empty element",el);
		}
		
		Token tok0 = tok.get(0);
		if(tok0.isBinary()) {
//			const char* data = tok[0]->begin(), *end = tok[0]->end();
			ByteBuffer data = tok0.contents();
			int position = data.position();
			int limit = data.limit();
			data.position(tok0.begin());
			data.limit(tok0.end());

//			char type;
//			uint32_t count;
			long value = readBinaryDataArrayHead(data,/* end, type, count,*/ el);
			int type = AssUtil.decodeFirst(value);
			int count = AssUtil.decodeSecond(value);
			if(count % 3 != 0) {
				parseError("number of floats is not a multiple of three (3) (binary)",el);
			}

			if(count == 0) {
				return;
			}

			if (type != 'd' && type != 'f') {
				parseError("expected float or double array (binary)",el);
			}

			ByteArrayList buff = new ByteArrayList(0);
			boolean compressed = readBinaryDataArray(type, count, data, /*end, */buff, el);
			
//			ai_assert(data == end);
//			ai_assert(buff.size() == count * (type == 'd' ? 8 : 4));
			assert (!compressed ||(buff.size() == count * (type == 'd' ? 8 : 4)));

//			final int count3 = count / 3;
			out.size(count);

			int size = 0;
			float[] out_put = out.elements();
			if (type == 'd') {
//				const double* d = reinterpret_cast<const double*>(&buff[0]);
//				for (unsigned int i = 0; i < count3; ++i, d += 3) {
//					out.push_back(aiVector3D(static_cast<float>(d[0]),
//						static_cast<float>(d[1]),
//						static_cast<float>(d[2])));
//				}
				if(compressed){
					byte[] bytes = buff.elements();
					for(int i = 0; i < count; i++){
						out_put[size++] = (float) AssUtil.getDouble(bytes, i << 3);
					}
				}else{
					while(data.remaining() > 0){
						out_put[size++] = (float) data.getDouble();
					}
					
					assert data.remaining() == 0;
				}
			}
			else if (type == 'f') {
//				const float* f = reinterpret_cast<const float*>(&buff[0]);
//				for (unsigned int i = 0; i < count3; ++i, f += 3) {
//					out.push_back(aiVector3D(f[0],f[1],f[2]));
//				}
				
				if(compressed){
					byte[] bytes = buff.elements();
					for(int i = 0; i < count; i++){
						out_put[size++] = AssUtil.getFloat(bytes, i << 2);
					}
				}else{
					while(data.remaining() > 0){
						out_put[size++] = data.getFloat();
					}
				}
			}
			data.position(position).limit(limit);
			return;
		}

		final int dim = parseTokenAsDim(tok0);

		// may throw bad_alloc if the input is rubbish, but this need
		// not to be prevented - importing would fail but we wouldn't
		// crash since assimp handles this case properly.
		out.size(dim * 3);

		Scope scope = getRequiredScope(el);
		Element a = getRequiredElement(scope,"a",el);

		if (a.tokens().size() % 3 != 0) {
			parseError("number of floats is not a multiple of three (3)",el);
		}
//		for (TokenList::const_iterator it = a.Tokens().begin(), end = a.Tokens().end(); it != end; ) {
		int i = 0;
		float[] out_put = out.elements();
		for (Token it : a.tokens()) {
//			aiVector3D v;
//			v.x = ParseTokenAsFloat(**it++);
//			v.y = ParseTokenAsFloat(**it++);
//			v.z = ParseTokenAsFloat(**it++);
//
//			out.push_back(v);
			out_put[i++] = parseTokenAsFloat(it);
		}
	}
	static void parseVectorDataArray4f(FloatArrayList out, Element el){
		// TODO 这部分代码必须优化一下
	}
	
	static void parseVectorDataArray2f(FloatArrayList out, Element el){
		// TODO 这部分代码必须优化一下
	}
	static void parseVectorDataArray1i(IntArrayList out, Element el){
		// TODO 这部分代码必须优化一下
	}
	static void parseVectorDataArray1f(FloatArrayList out, Element el){
		// TODO 这部分代码必须优化一下
	}
//	void ParseVectorDataArray(std::vector<unsigned int>& out, Element el);
	static void parseVectorDataArray(LongArrayList out, Element e){
		// TODO 这部分代码必须优化一下
	}

	

	/** extract a required element from a scope, abort if the element cannot be found*/
	static Element getRequiredElement(Scope sc, String index, Element element){
		Element el = sc.get(index);
		if(el == null)
			parseError("did not find required element \"" + index + "\"",element);
		
		return el;
	}

	/** extract required compound scope */
	static Scope getRequiredScope(Element el){
		Scope s = el.compound();
		if(s == null)
			parseError("expected compound scope", el);
		
		return s;
	}
	
	/** get token at a particular index */
	static Token getRequiredToken(Element el, int index){
		List<Token> t = el.tokens();
		if(index >= t.size())
			parseError("missing token at index " + index, el);
		
		return t.get(index);
	}

	static Matrix4f readMatrix(Element element, Matrix4f result){
		FloatArrayList values = new FloatArrayList(16);
		parseVectorDataArray1f(values,element);

		if(values.size() != 16) {
			parseError("expected 16 matrix elements", (Element)null);
		}
		
		if(result == null)
			result = new Matrix4f();

//		result.a1 = values[0];
//		result.a2 = values[1];
//		result.a3 = values[2];
//		result.a4 = values[3];
//
//		result.b1 = values[4];
//		result.b2 = values[5];
//		result.b3 = values[6];
//		result.b4 = values[7];
//
//		result.c1 = values[8];
//		result.c2 = values[9];
//		result.c3 = values[10];
//		result.c4 = values[11];
//
//		result.d1 = values[12];
//		result.d2 = values[13];
//		result.d3 = values[14];
//		result.d4 = values[15];
//
//		result.Transpose();
		result.load(values.elements(), 0);
		return result;
	}
}
