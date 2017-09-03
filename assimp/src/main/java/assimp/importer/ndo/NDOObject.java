package assimp.importer.ndo;

import java.util.ArrayList;

final class NDOObject {

	String name;
	
	final ArrayList<Edge> edges = new ArrayList<Edge>();
	final ArrayList<NDOFace> faces = new ArrayList<NDOFace>();
	final ArrayList<Vertex> vertices = new ArrayList<Vertex>();
}
