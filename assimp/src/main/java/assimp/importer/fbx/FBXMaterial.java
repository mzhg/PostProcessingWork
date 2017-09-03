package assimp.importer.fbx;

import java.util.HashMap;
import java.util.List;

import assimp.common.AssUtil;

/** DOM class for generic FBX materials */
final class FBXMaterial extends FBXObject{

	String shading;
	boolean multilayer;
	PropertyTable props;

	HashMap<String, FBXTexture> textures;
	HashMap<String, LayeredTexture> layeredTextures;
	
	public FBXMaterial(long id, Element element, Document doc, String name) {
		super(id, element, name);
		
		Scope sc = Parser.getRequiredScope(element);
		
		Element ShadingModel = sc.get("ShadingModel");
		Element MultiLayer = sc.get("MultiLayer");

		if(MultiLayer != null) {
			multilayer = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(MultiLayer,0)) != 0;
		}

		if(ShadingModel != null) {
			shading = Parser.parseTokenAsStringSafe(Parser.getRequiredToken(ShadingModel,0));
		}
		else {
			FBXUtil.DOMWarning("shading mode not specified, assuming phong",element);
			shading = "phong";
		}

		String templateName = null;

		final String sh = shading;
		if(!FBXUtil.strcmp(sh,"phong")) {
			templateName = "Material.FbxSurfacePhong";
		}
		else if(!FBXUtil.strcmp(sh,"lambert")) {
			templateName = "Material.FbxSurfaceLambert";
		}
		else {
			FBXUtil.DOMWarning("shading mode not recognized: " + shading,element);
		}

		props = Document.getPropertyTable(doc,templateName,element,sc,false);

		// resolve texture links
//		const std::vector<const Connection*>& conns = doc.GetConnectionsByDestinationSequenced(ID());
		List<Connection> conns = doc.getConnectionsByDestinationSequenced(ID());
//		BOOST_FOREACH(const Connection* con, conns) {
		for(Connection con : conns){

			// texture link to properties, not objects
			if (/*!con->PropertyName().length()*/ AssUtil.isEmpty(con.propertyName())) {
				continue;
			}

			FBXObject ob = con.sourceObject();
			if(ob == null) {
				FBXUtil.DOMWarning("failed to read source object for texture link, ignoring",element);
				continue;
			}

//			const Texture* const tex = dynamic_cast<const Texture*>(ob);
//			if(!tex) {
//				const LayeredTexture* const layeredTexture = dynamic_cast<const LayeredTexture*>(ob);
//				if(!layeredTexture) {
//					DOMWarning("source object for texture link is not a texture or layered texture, ignoring",&element);
//					continue;
//				}
//				const std::string& prop = con->PropertyName();
//				if (layeredTextures.find(prop) != layeredTextures.end()) {
//					DOMWarning("duplicate layered texture link: " + prop,&element);
//				}
//
//				layeredTextures[prop] = layeredTexture;
//				((LayeredTexture*)layeredTexture)->fillTexture(doc);
//			}
//			else
//			{
//				const std::string& prop = con->PropertyName();
//				if (textures.find(prop) != textures.end()) {
//					DOMWarning("duplicate texture link: " + prop,&element);
//				}
//
//				textures[prop] = tex;
//			}
			
			if(ob instanceof FBXTexture){
				FBXTexture tex = (FBXTexture)ob;
				String prop = con.propertyName();
				if(textures.get(prop) != null)
					FBXUtil.DOMWarning("duplicate texture link: " + prop,element);
				
				textures.put(prop, tex);
			}else if(ob instanceof LayeredTexture){
				LayeredTexture layeredTexture = (LayeredTexture)ob;
				
				String prop = con.propertyName();
				if (layeredTextures.get(prop) != /*layeredTextures.end()*/ null) {
					FBXUtil.DOMWarning("duplicate layered texture link: " + prop,element);
				}

				layeredTextures.put(prop, layeredTexture);
				layeredTexture.fillTexture(doc);
			}else{
				FBXUtil.DOMWarning("source object for texture link is not a texture or layered texture, ignoring",element);
			}
		}
	}
	
	String getShadingModel() {	return shading;}

	boolean isMultilayer() {	return multilayer;}

	PropertyTable props() {
//		ai_assert(props.get());
		return props;
	}

	HashMap<String, FBXTexture> textures() {	return textures;}

	HashMap<String, LayeredTexture> layeredTextures() {	return layeredTextures;}
}
