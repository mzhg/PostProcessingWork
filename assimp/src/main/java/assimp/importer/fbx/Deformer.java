package assimp.importer.fbx;

/** DOM class for deformers */
class Deformer extends FBXObject{

	private PropertyTable props;
	
	public Deformer(long id, Element element, Document doc, String name) {
		super(id, element, name);
	}
	
	PropertyTable props() {	return props;}
}
