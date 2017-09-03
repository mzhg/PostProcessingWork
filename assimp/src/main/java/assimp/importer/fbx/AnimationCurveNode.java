package assimp.importer.fbx;

import java.util.HashMap;
import java.util.List;

import assimp.common.AssUtil;

/** Represents a FBX animation curve (i.e. a 1-dimensional set of keyframes and values therefor) */
final class AnimationCurveNode extends FBXObject{

	private FBXObject target;
	private PropertyTable props;
	private HashMap<String, AnimationCurve> curves;

	private String prop;
	private final Document doc;
	
	/** the optional whitelist specifies a list of property names for which the caller
	wants animations for. If the curve node does not match one of these, std::range_error
	will be thrown. */
	public AnimationCurveNode(long id, Element element, String name, Document doc, String target_prop_whitelist) {
		super(id, element, name);
		this.doc = doc;
		
		Scope sc = Parser.getRequiredScope(element);
		
		// find target node
		String whitelist[] = {"Model","NodeAttribute"};
		List<Connection> conns = doc.getConnectionsBySourceSequenced(ID(),whitelist);

//		BOOST_FOREACH(const Connection* con, conns) {
		for(Connection con : conns){

			// link should go for a property
			if (AssUtil.isEmpty(con.propertyName())) {
				continue;
			}

			if(target_prop_whitelist != null) {
				String s = con.propertyName();
				boolean ok = target_prop_whitelist.contains(s);
//				for (int i = 0; i < whitelist_size; ++i) {
//					if (!strcmp(s, target_prop_whitelist[i])) {
//						ok = true;
//						break;
//					}
//				}

				if (!ok) {
					throw new IllegalArgumentException("AnimationCurveNode target property is not in whitelist");
				}
			}

			final FBXObject ob = con.destinationObject();
			if(ob == null) {
				FBXUtil.DOMWarning("failed to read destination object for AnimationCurveNode->Model link, ignoring",element);
				continue;
			}

			// XXX support constraints as DOM class
			//ai_assert(dynamic_cast<const Model*>(ob) || dynamic_cast<const NodeAttribute*>(ob));
			target = ob; 
//			if(!target) {
//				continue;
//			}

			prop = con.propertyName();
			break;
		}

		if(target == null) {
			FBXUtil.DOMWarning("failed to resolve target Model/NodeAttribute/Constraint for AnimationCurveNode",element);
		}

		props = Document.getPropertyTable(doc,"AnimationCurveNode.FbxAnimCurveNode",element,sc,false);
	}
	
	PropertyTable props() {
		return props;
	}


	HashMap<String, AnimationCurve> curves(){
		if(curves == null) {
			curves = new HashMap<>();
			// resolve attached animation curves
			final List<Connection> conns = doc.getConnectionsByDestinationSequenced(ID(),"AnimationCurve");

//			BOOST_FOREACH(const Connection* con, conns) {
			for (Connection con : conns){

				// link should go for a property
				if (AssUtil.isEmpty(con.propertyName())) {
					continue;
				}

				final FBXObject ob = con.sourceObject();
				if(ob == null) {
					FBXUtil.DOMWarning("failed to read source object for AnimationCurve->AnimationCurveNode link, ignoring",element);
					continue;
				}

				if(ob instanceof AnimationCurve){
					final AnimationCurve anim = (AnimationCurve)ob;
//					curves[con->PropertyName()] = anim;
					curves.put(con.propertyName(), anim);
				}else{
					FBXUtil.DOMWarning("source object for ->AnimationCurveNode link is not an AnimationCurve",element);
					continue;
				}
				
			}
		}

		return curves;
	}

	/** Object the curve is assigned to, this can be null if the
	 *  target object has no DOM representation or could not
	 *  be read for other reasons.*/
	FBXObject target()  {	return target;}

	Model targetAsModel(){
		if(target instanceof Model){
			return (Model)target;
		}
		
		return null;
	}

	NodeAttribute targetAsNodeAttribute() {
		if(target instanceof NodeAttribute){
			return (NodeAttribute)target;
		}
		
		return null;
	}

	/** Property of Target() that is being animated*/
	String targetProperty() { return prop;}

}
