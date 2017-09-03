package assimp.importer.fbx;

/** DOM class for generic FBX NoteAttribute blocks. NoteAttribute's just hold a property table,
 *  fixed members are added by deriving classes. */
class NodeAttribute extends FBXObject{

	private PropertyTable props;

	public NodeAttribute(long id, Element element, Document doc, String name) {
		super(id, element, name);
		
		Scope sc = Parser.getRequiredScope(element);

		String classname = Parser.parseTokenAsStringSafe(Parser.getRequiredToken(element,2));

		// hack on the deriving type but Null/LimbNode attributes are the only case in which
		// the property table is by design absent and no warning should be generated
		// for it.
		final boolean is_null_or_limb = !FBXUtil.strcmp(classname, "Null") || !FBXUtil.strcmp(classname, "LimbNode");
		props = Document.getPropertyTable(doc,"NodeAttribute.Fbx" + classname,element,sc, is_null_or_limb);
	}
	
	PropertyTable props() { return props;}
}
