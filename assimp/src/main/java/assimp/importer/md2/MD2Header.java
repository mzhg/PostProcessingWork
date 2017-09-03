package assimp.importer.md2;

import java.nio.ByteBuffer;

/** Data structure for the MD2 main header */
final class MD2Header {

	int magic; 
	int version; 
	int skinWidth; 
	int skinHeight; 
	int frameSize; 
	int numSkins; 
	int numVertices; 
	int numTexCoords; 
	int numTriangles; 
	int numGlCommands; 
	int numFrames; 
	int offsetSkins; 
	int offsetTexCoords; 
	int offsetTriangles; 
	int offsetFrames; 
	int offsetGlCommands; 
	int offsetEnd;
	
	void load(ByteBuffer buf){
		magic = buf.getInt();
		version = buf.getInt();
		skinWidth = buf.getInt();
		skinHeight = buf.getInt();
		frameSize = buf.getInt();
		numSkins = buf.getInt();
		numVertices = buf.getInt();
		numTexCoords = buf.getInt();
		numTriangles = buf.getInt();
		numGlCommands = buf.getInt();
		numFrames = buf.getInt();
		offsetSkins = buf.getInt();
		offsetTexCoords = buf.getInt();
		offsetTriangles = buf.getInt();
		offsetFrames = buf.getInt();
		offsetGlCommands = buf.getInt();
		offsetEnd = buf.getInt();
	}
}
