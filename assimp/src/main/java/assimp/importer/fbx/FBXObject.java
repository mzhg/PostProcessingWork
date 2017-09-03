package assimp.importer.fbx;

/** Base class for in-memory (DOM) representations of FBX objects */
class FBXObject {

	Element element;
	String name;
	long id;
	
	public FBXObject(long id, Element element, String name) {
		this.id = id;
		this.element = element;
		this.name = name;
	}
	
	Element sourceElement() { return element;}
	String name() { return name;}
	long ID() { return id;}
}
