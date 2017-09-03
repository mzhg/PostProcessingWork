package assimp.importer.fbx;

import java.util.ArrayList;
import java.util.List;

import assimp.common.AssUtil;

/** Represents a FBX animation stack (i.e. a list of animation layers) */
final class AnimationStack extends FBXObject{

	private PropertyTable props;
	private ArrayList<AnimationLayer> layers;
	
	public AnimationStack(long id, Element element, String name, Document doc) {
		super(id, element, name);
		
		Scope sc = Parser.getRequiredScope(element);

		// note: we don't currently use any of these properties so we shouldn't bother if it is missing
		props = Document.getPropertyTable(doc,"AnimationStack.FbxAnimStack",element,sc, true);

		// resolve attached animation layers
		List<Connection> conns = doc.getConnectionsByDestinationSequenced(ID(),"AnimationLayer");
		layers = new ArrayList<>(conns.size());

//		BOOST_FOREACH(const Connection* con, conns) {
		for(Connection con : conns){

			// link should not go to a property
			if (!AssUtil.isEmpty(con.propertyName())) {
				continue;
			}

			final FBXObject ob = con.sourceObject();
			if(ob == null) {
				FBXUtil.DOMWarning("failed to read source object for AnimationLayer->AnimationStack link, ignoring",element);
				continue;
			}

			if(ob instanceof AnimationLayer){
				AnimationLayer anim = (AnimationLayer)(ob);
				layers.add(anim);
			}else
				FBXUtil.DOMWarning("source object for ->AnimationStack link is not an AnimationLayer",element);
		}
	}
	
	PropertyTable props() {	return props;}
	
	ArrayList<AnimationLayer> layers(){ return layers;}

	long localStart() { return PropertyTable.propertyGet(props(), "LocalStart",  0L);}

	long localStop() { return PropertyTable.propertyGet(props(), "LocalStop",  0L);}

	long referenceStart() { return PropertyTable.propertyGet(props(), "ReferenceStart",  0L);}

	long referenceStop() { return PropertyTable.propertyGet(props(), "ReferenceStop",  0L);}
}
