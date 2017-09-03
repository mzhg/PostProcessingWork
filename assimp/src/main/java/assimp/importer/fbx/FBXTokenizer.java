package assimp.importer.fbx;

import java.nio.ByteBuffer;
import java.util.List;

import assimp.common.DeadlyImportError;
import assimp.common.ParsingUtil;

final class FBXTokenizer {
	
	// tab width for logging columns
	static final int ASSIMP_FBX_TAB_WIDTH = 4;

	private FBXTokenizer(){}
	
	// signal tokenization error, this is always unrecoverable. Throws DeadlyImportError.
	static void tokenizeError(String message, int line, int column)
	{
		throw new DeadlyImportError(FBXUtil.addLineAndColumn("FBX-Tokenize",message,line,column));
	}
	
	// process a potential data token up to 'cur', adding it to 'output_tokens'. 
	// ------------------------------------------------------------------------------------------------
	static void processDataToken( List<Token> output_tokens, ByteBuffer input, int start, int end/*const char*& start, const char*& end*/,
						  int line, int column){
		processDataToken(output_tokens, input, start, end, line, column, Token.TokenType_DATA, false);
	}
	
	// process a potential data token up to 'cur', adding it to 'output_tokens'. 
	// ------------------------------------------------------------------------------------------------
	static void processDataToken( List<Token> output_tokens, ByteBuffer input, int start, int end/*const char*& start, const char*& end*/,
						  int line, int column, int type /*= TokenType_DATA*/, boolean must_have_token /*= false*/)
	{
		if (start != 0 && end != 0) {
			// sanity check:
			// tokens should have no whitespace outside quoted text and [start,end] should
			// properly delimit the valid range.
			boolean in_double_quotes = false;
//			for (const char* c = start; c != end + 1; ++c) {
			for (int i = start; i < end + 1; ++i){
				byte c = input.get(i);
				if (c == '\"') {
					in_double_quotes = !in_double_quotes;
				}

				if (!in_double_quotes && ParsingUtil.isSpaceOrNewLine(c)) {
					tokenizeError("unexpected whitespace in token", line, column);
				}
			}

			if (in_double_quotes) {
				tokenizeError("non-terminated double quotes", line, column);
			}

			output_tokens.add(new Token(input, start,end + 1,type,line,column));
		}
		else if (must_have_token) {
			tokenizeError("unexpected character, expected data token", line, column);
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	static void tokenize(List<Token> output_tokens, ByteBuffer input)
	{
//		ai_assert(input);

		// line and column numbers numbers are one-based
		int line = 1;
		int column = 1;

		boolean comment = false;
		boolean in_double_quotes = false;
		boolean pending_data_token = false;
		
//		const char* token_begin = NULL, *token_end = NULL;
		int token_begin = 0, token_end = 0;
//		for (const char* cur = input;*cur;column += (*cur == '\t' ? ASSIMP_FBX_TAB_WIDTH : 1), ++cur) {
		for (int cur = 0; cur < input.limit(); ++cur){
//			const char c = *cur;
			final byte c = input.get(cur);

			if (ParsingUtil.isLineEnd(c)) {
				comment = false;

				column = 0;
				++line;
			}

			if(comment) {
				continue;
			}

			if(in_double_quotes) {
				if (c == '\"') {
					in_double_quotes = false;
					token_end = cur;

					processDataToken(output_tokens,input,token_begin,token_end,line,column);
					token_begin = token_end = 0;
					pending_data_token = false;
				}
				continue;
			}

			switch(c)
			{
			case '\"':
				if (token_begin > 0) {
					tokenizeError("unexpected double-quote", line, column);
				}
				token_begin = cur;
				in_double_quotes = true;
				continue;

			case ';':
				processDataToken(output_tokens,input, token_begin,token_end,line,column);
				token_begin = token_end = 0;
				comment = true;
				continue;

			case '{':
				processDataToken(output_tokens,input, token_begin,token_end, line, column);
				token_begin = token_end = 0;
				output_tokens.add(new Token(input, cur,cur+1,Token.TokenType_OPEN_BRACKET,line,column));
				continue;

			case '}':
				processDataToken(output_tokens,input, token_begin,token_end,line,column);
				token_begin = token_end = 0;
				output_tokens.add(new Token(input, cur,cur+1,Token.TokenType_CLOSE_BRACKET,line,column));
				continue;
			
			case ',':
				if (pending_data_token) {
					processDataToken(output_tokens,input, token_begin,token_end,line,column,Token.TokenType_DATA,true);
					token_begin = token_end = 0;
				}
				output_tokens.add(new Token(input, cur,cur+1,Token.TokenType_COMMA,line,column));
				continue;

			case ':':
				if (pending_data_token) {
					processDataToken(output_tokens,input,token_begin,token_end,line,column,Token.TokenType_KEY,true);
					token_begin = token_end = 0;
				}
				else {
					tokenizeError("unexpected colon", line, column);
				}
				continue;
			}
			
			if (ParsingUtil.isSpaceOrNewLine(c)) {

				if (token_begin > 0) {
					// peek ahead and check if the next token is a colon in which
					// case this counts as KEY token.
					int type = Token.TokenType_DATA;
//					for (const char* peek = cur;  *peek && isSpaceOrNewLine(*peek); ++peek) {
					for (int peek = cur, v = input.get(peek); peek < input.limit() && ParsingUtil.isSpaceOrNewLine((byte)v); ++peek){
						v = input.get(peek);
						if (v == ':') {
							type = Token.TokenType_KEY;
							cur = peek;
							break;
						}
					}

					processDataToken(output_tokens,input, token_begin,token_end,line,column,type, false);
					token_begin = token_end = 0;
				}

				pending_data_token = false;
			}
			else {
				token_end = cur;
				if (token_begin == 0) {
					token_begin = cur;
				}

				pending_data_token = true;
			}
			
			column += (input.get(cur) == '\t' ? ASSIMP_FBX_TAB_WIDTH : 1);
		}
	}
}
