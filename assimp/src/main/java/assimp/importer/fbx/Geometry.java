package assimp.importer.fbx;

import java.util.List;

import assimp.common.AssUtil;

/** DOM base class for all kinds of FBX geometry */
abstract class Geometry extends FBXObject{

	private Skin skin;
	
	public Geometry(long id, Element element, String name, Document doc) {
		super(id, element, name);
		
		List<Connection> conns = doc.getConnectionsByDestinationSequenced(ID(),"Deformer");
//		BOOST_FOREACH(const Connection* con, conns) {
		for(Connection con : conns){
			Skin sk = processSimpleConnection(con, false, "Skin -> Geometry", element);
			if(sk != null) {
				skin = sk;
				break;
			}
		}
	}
	
	/** Get the Skin attached to this geometry or NULL */
	Skin deformerSkin() { return skin;}

	// ------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	static<T> T processSimpleConnection(Connection con, boolean is_object_property_conn, String name, Element element)
	{
		if (is_object_property_conn && AssUtil.isEmpty(con.propertyName())) {
			FBXUtil.DOMWarning("expected incoming " + name+
				" link to be an object-object connection, ignoring",
				element
				);
			return null;
		}
		else if (!is_object_property_conn && !AssUtil.isEmpty(con.propertyName())/*con.PropertyName().length()*/) {
			FBXUtil.DOMWarning("expected incoming " + name +
				" link to be an object-property connection, ignoring",
				element
				);
			return null;
		}

//		if(is_object_property_conn && propNameOut) {
			// note: this is ok, the return value of PropertyValue() is guaranteed to 
			// remain valid and unchanged as long as the document exists.
//			*propNameOut = con.PropertyName().c_str();  TODO
//		}

		final FBXObject ob = con.sourceObject();
		if(ob == null) {
			FBXUtil.DOMWarning("failed to read source object for incoming" + name +
				" link, ignoring",
				element);
			return null;
		}

		try {
			return (T)ob;
		} catch (ClassCastException e) {
			return null;
		}
	}
}
