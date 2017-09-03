package assimp.importer.dxf;

import java.util.ArrayList;

import org.lwjgl.util.vector.Vector3f;

//keeps track of all geometry in a single BLOCK.
final class Block {

	ArrayList<PolyLine> lines /*= new ArrayList<PolyLine>()*/;
	ArrayList<InsertBlock> insertions /*= new ArrayList<InsertBlock>()*/;
	
	String name;
	final Vector3f base = new Vector3f();
}
