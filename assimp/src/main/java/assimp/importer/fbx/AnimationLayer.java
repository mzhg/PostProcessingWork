package assimp.importer.fbx;

import java.util.ArrayList;
import java.util.List;

import assimp.common.AssUtil;

/** Represents a FBX animation layer (i.e. a list of node animations) */
final class AnimationLayer extends FBXObject{

	private PropertyTable props;
	private Document doc;
	
	public AnimationLayer(long id, Element element, String name, Document doc) {
		super(id, element, name);
		this.doc = doc;
		
		Scope sc = Parser.getRequiredScope(element);

		// note: the props table here bears little importance and is usually absent
		props = Document.getPropertyTable(doc,"AnimationLayer.FbxAnimLayer",element,sc, true);
	}

	PropertyTable props() {	return props;}
	
	/** the optional whitelist specifies a list of property names for which the caller
	wants animations for. Curves not matching this list will not be added to the
	animation layer. */
	ArrayList<AnimationCurveNode> nodes(String target_prop_whitelist){
		// resolve attached animation nodes
		final List<Connection> conns = doc.getConnectionsByDestinationSequenced(ID(),"AnimationCurveNode");
//		nodes.reserve(conns.size());
		ArrayList<AnimationCurveNode> nodes = new ArrayList<>(conns.size());

//		BOOST_FOREACH(const Connection* con, conns) {
		for (Connection con : conns){

			// link should not go to a property
			if (!AssUtil.isEmpty(con.propertyName())) {
				continue;
			}

			FBXObject ob = con.sourceObject();
			if(ob == null) {
				FBXUtil.DOMWarning("failed to read source object for AnimationCurveNode->AnimationLayer link, ignoring",element);
				continue;
			}

			if(ob instanceof AnimationCurveNode){
				AnimationCurveNode anim = (AnimationCurveNode)(ob);

				if( !AssUtil.isEmpty(target_prop_whitelist)) {
					String s = anim.targetProperty();
//					bool ok = false;
//					for (size_t i = 0; i < whitelist_size; ++i) {
//						if (!strcmp(s, target_prop_whitelist[i])) {
//							ok = true;
//							break;
//						}
//					}
					
					boolean ok = target_prop_whitelist.contains(s);
					if(!ok) {
						continue;
					}
				}
				nodes.add(anim);
			}else
				FBXUtil.DOMWarning("source object for ->AnimationLayer link is not an AnimationCurveNode",element);
		}

		return nodes; // pray for NRVO
	}
	
}
