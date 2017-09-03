package assimp.importer.fbx;

import java.util.List;

/** DOM class for layered FBX textures */
final class LayeredTexture extends FBXObject{
	
//	enum BlendMode
//	{
	static final int 
		BlendMode_Translucent = 0,
		BlendMode_Additive    = 1,
		BlendMode_Modulate    = 2,
		BlendMode_Modulate2   = 3,
		BlendMode_Over        = 4,
		BlendMode_Normal      = 5,
		BlendMode_Dissolve    = 6,
		BlendMode_Darken      = 7,
		BlendMode_ColorBurn   = 8,
		BlendMode_LinearBurn  = 9,
		BlendMode_DarkerColor = 10,
		BlendMode_Lighten     = 11,
		BlendMode_Screen      = 12,
		BlendMode_ColorDodge  = 13,
		BlendMode_LinearDodge = 14,
		BlendMode_LighterColor= 15,
		BlendMode_SoftLight   = 16,
		BlendMode_HardLight   = 17,
		BlendMode_VividLight  = 18,
		BlendMode_LinearLight = 19,
		BlendMode_PinLight    = 20,
		BlendMode_HardMix     = 21,
		BlendMode_Difference  = 22,
		BlendMode_Exclusion   = 23,
		BlendMode_Subtract    = 24,
		BlendMode_Divide      = 25,
		BlendMode_Hue         = 26,
		BlendMode_Saturation  = 27,
		BlendMode_Color       = 28,
		BlendMode_Luminosity  = 29,
		BlendMode_Overlay     = 30,
		BlendMode_BlendModeCount = 31;
//	};
	
	private FBXTexture texture;
	private int blendMode = BlendMode_Modulate;
	private float alpha = 1;

	public LayeredTexture(long id, Element element, Document doc, String name) {
		super(id, element, name);
		
		Scope sc = Parser.getRequiredScope(element);

		Element BlendModes = sc.get("BlendModes");
		Element Alphas = sc.get("Alphas");

		
		if(BlendModes!=null)
		{
			blendMode = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(BlendModes,0));
		}
		if(Alphas!=null)
		{
			alpha = Parser.parseTokenAsFloatSafe(Parser.getRequiredToken(Alphas,0));
		}
	}
	
	//Can only be called after construction of the layered texture object due to construction flag.
	void fillTexture(Document doc){
		List<Connection> conns = doc.getConnectionsByDestinationSequenced(ID());
		for(int i = 0; i < conns.size();++i)
		{
			Connection con = conns.get(i);

//			const Object* const ob = con->SourceObject();
			final FBXObject ob = con.sourceObject();
			if(ob == null) {
				FBXUtil.DOMWarning("failed to read source object for texture link, ignoring",element);
				continue;
			}

//			const Texture* const tex = dynamic_cast<const Texture*>(ob);
//			texture = tex;
			
			if(ob instanceof FBXTexture){
				texture = (FBXTexture)ob;
			}
		}
	}

	FBXTexture getTexture() {	return texture;}
	int getBlendMode()		{	return blendMode;}
	float alpha()			{	return alpha;}
}
