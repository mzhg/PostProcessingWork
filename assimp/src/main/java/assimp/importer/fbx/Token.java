package assimp.importer.fbx;

import java.nio.ByteBuffer;

import assimp.common.AssUtil;

final class Token {

	/* Rough classification for text FBX tokens used for constructing the
	 *  basic scope hierarchy. */
//	enum TokenType
//	{
     /** { */
	static final int TokenType_OPEN_BRACKET = 0;
		
	 /** } */
	static final int TokenType_CLOSE_BRACKET = 1;

	/** '"blablubb"', '2', '*14' - very general token class,
	 further processing happens at a later stage. */
	static final int TokenType_DATA = 2;

		//
	static final int TokenType_BINARY_DATA =3;

	/** ,*/
	static final int TokenType_COMMA = 4;

	/** blubb: */
	static final int TokenType_KEY = 5;
//	};
	
	private static final int BINARY_MARKER = -1;
	
	private ByteBuffer contents;
	private String s_contents;
	private int sbegin;
	private int send;
	private int type;
	private int line;
	private int column;
	
	public Token(ByteBuffer contents, int sbegin, int send, int type, int line, int column) {
		this.contents = contents;
		this.sbegin = sbegin;
		this.send = send;
		this.type = type;
		this.line = line;
		this.column = column;
	}
	
	public Token(ByteBuffer contents, int sbegin, int send, int type, int offset) {
		this.contents = contents;
		this.sbegin = sbegin;
		this.send = send;
		this.type = type;
		this.line = offset;
	}
	
	String stringContents() {
//		return std::string(begin(),end());
		if(s_contents == null){
//			int pos = contents.position();
//			contents.position(sbegin);
//			byte[] bytes = new byte[send - sbegin];
//			contents.get(bytes).position(pos);
			s_contents = AssUtil.getString(contents, sbegin, send - sbegin);
		}
		
		return s_contents;
	}

	boolean isBinary() {
		return column == BINARY_MARKER;
	}
	
	ByteBuffer contents() { contents.position(sbegin).limit(send); return contents; }

	int begin() { return sbegin;}

	int end() { return send;}

	int type() {	return type;}
	
	char get(int index) { return (char) contents.get(index);}
	
	int offset() {
//		ai_assert(IsBinary());
		return line;
	}

	int line() {
//		ai_assert(!IsBinary());
		return line;
	}

	int column() {
//		ai_assert(!IsBinary());
		return column;
	}
}
