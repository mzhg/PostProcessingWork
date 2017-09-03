package assimp.importer.fbx;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import assimp.common.AssUtil;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;

final class FBXUtil {

	private FBXUtil(){}
	
	// ------------------------------------------------------------------------------------------------
	static String tokenTypeString(int t)
	{
		switch(t) {
			case Token.TokenType_OPEN_BRACKET:
				return "TOK_OPEN_BRACKET";
		
			case Token.TokenType_CLOSE_BRACKET:
				return "TOK_CLOSE_BRACKET";

			case Token.TokenType_DATA:
				return "TOK_DATA";

			case Token.TokenType_COMMA:
				return "TOK_COMMA";

			case Token.TokenType_KEY:
				return "TOK_KEY";

			case Token.TokenType_BINARY_DATA:
				return "TOK_BINARY_DATA";
		}

//		ai_assert(false);
		return "";
	}
		

	// ------------------------------------------------------------------------------------------------
	static String addOffset(String prefix, String text, int offset)
	{
//		return static_cast<std::string>( (Formatter::format(),prefix," (offset 0x",std::hex,offset,") ",text) );
		return prefix + " (offset 0x" + Integer.toHexString(offset) + ") " + text;
	}

	// ------------------------------------------------------------------------------------------------
	static String addLineAndColumn(String prefix, String text, int line, int column)
	{
		return AssUtil.makeString(prefix," (line ",line,", col ",column,") ",text);
	}

	// ------------------------------------------------------------------------------------------------
	static String addTokenText(String prefix, String text, Token tok)
	{
		if(tok.isBinary()) {
//			return static_cast<std::string>( (Formatter::format(),prefix,
//				" (",TokenTypeString(tok->Type()),
//				", offset 0x", std::hex, tok->Offset(),") ",
//				text) );
			
			return AssUtil.makeString(prefix, " (", tokenTypeString(tok.type()), ", offset 0x", Integer.toHexString(tok.offset()) + ") ", text);
		}
		
//		return static_cast<std::string>( (Formatter::format(),prefix,
//			" (",TokenTypeString(tok->Type()),
//			", line ",tok->Line(),
//			", col ",tok->Column(),") ",
//			text) );
		
		return AssUtil.makeString(prefix, " (", tokenTypeString(tok.type()),", line ", tok.line(), ", col ", tok.column(), ") ", text);
	}
	
	// ------------------------------------------------------------------------------------------------
	// signal DOM construction error, this is always unrecoverable. Throws DeadlyImportError.
	static void DOMError(String message, Token token)
	{
		throw new DeadlyImportError(addTokenText("FBX-DOM",message,token));
	}

	// ------------------------------------------------------------------------------------------------
	static void DOMError(String message, Element element /*= NULL*/)
	{
		if(element != null) {
			DOMError(message,element.keyToken());
		}
		throw new DeadlyImportError("FBX-DOM " + message);
	}
	
	static void DOMError(String message)
	{
		DOMError(message, (Element)null);
	}

	// ------------------------------------------------------------------------------------------------
	// print warning, do return
	static void DOMWarning(String message, Token token)
	{
		if(DefaultLogger.LOG_OUT) {
			DefaultLogger.warn(addTokenText("FBX-DOM",message,token));
		}
	}

	// ------------------------------------------------------------------------------------------------
	static void DOMWarning(String message, Element element /*= NULL*/)
	{
		if(element != null) {
			DOMWarning(message,element.keyToken());
			return;
		}
		if(DefaultLogger.LOG_OUT) {
			DefaultLogger.warn("FBX-DOM: " + message);
		}
	}
	
	static void DOMWarning(String message/*, Element element = NULL*/)
	{
		DOMWarning(message, (Element)null);
	}

	static boolean strncmp(ByteBuffer buf, CharSequence string){
		return !AssUtil.equals(buf, string, 0, buf.remaining());
	}
	
	static boolean strcmp(CharSequence l , CharSequence r){
		return !l.equals(r);
	}
	
	static<T> void put(Long2ObjectMap<List<T>> map, long key, T value){
		List<T> list = map.get(key);
		if(list == null){
			list = new LinkedList<T>();
			map.put(key, list);
		}
		list.add(value);
	}
	
	static<K, T> void put(Map<K, List<T>> map, K key, T value){
		List<T> list = map.get(key);
		if(list == null){
			list = new LinkedList<T>();
			map.put(key, list);
		}
		list.add(value);
	}
}
