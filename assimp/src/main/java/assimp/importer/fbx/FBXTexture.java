package assimp.importer.fbx;

import org.lwjgl.util.vector.Vector2f;

/** DOM class for generic FBX textures */
final class FBXTexture extends FBXObject{

	private final Vector2f uvTrans =new Vector2f();
	private final Vector2f uvScaling = new Vector2f(1.0f, 1.0f);

	private String type;
	private String relativeFileName;
	private String fileName;
	private String alphaSource;
	private PropertyTable props;

	private int[] crop = new int[4];
	
	public FBXTexture(long id, Element element, Document doc, String name) {
		super(id, element, name);
		
		Scope sc = Parser.getRequiredScope(element);

		Element Type = sc.get("Type");
		Element FileName = sc.get("FileName");
		Element RelativeFilename = sc.get("RelativeFilename");
		Element ModelUVTranslation = sc.get("ModelUVTranslation");
		Element ModelUVScaling = sc.get("ModelUVScaling");
		Element Texture_Alpha_Source = sc.get("Texture_Alpha_Source");
		Element Cropping = sc.get("Cropping");

		if(Type != null) {
			type = Parser.parseTokenAsStringSafe(Parser.getRequiredToken(Type,0));
		}

		if(FileName != null) {
			fileName = Parser.parseTokenAsStringSafe(Parser.getRequiredToken(FileName,0));
		}

		if(RelativeFilename != null) {
			relativeFileName = Parser.parseTokenAsStringSafe(Parser.getRequiredToken(RelativeFilename,0));
		}

		if(ModelUVTranslation != null) {
			uvTrans.set(Parser.parseTokenAsFloatSafe(Parser.getRequiredToken(ModelUVTranslation,0)),
					Parser.parseTokenAsFloatSafe(Parser.getRequiredToken(ModelUVTranslation,1))
			);
		}

		if(ModelUVScaling != null) {
			uvScaling.set(Parser.parseTokenAsFloatSafe(Parser.getRequiredToken(ModelUVScaling,0)),
					Parser.parseTokenAsFloatSafe(Parser.getRequiredToken(ModelUVScaling,1))
			);
		}

		if(Cropping != null) {
			crop[0] = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Cropping,0));
			crop[1] = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Cropping,1));
			crop[2] = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Cropping,2));
			crop[3] = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Cropping,3));
		}
		else {
			// vc8 doesn't support the crop() syntax in initialization lists
			// (and vc9 WARNS about the new (i.e. compliant) behaviour).
			crop[0] = crop[1] = crop[2] = crop[3] = 0;
		}

		if(Texture_Alpha_Source != null) {
			alphaSource = Parser.parseTokenAsStringSafe(Parser.getRequiredToken(Texture_Alpha_Source,0));
		}

		props = Document.getPropertyTable(doc,"Texture.FbxFileTexture",element,sc,false);
	}
	
	String type() {	return type;}

	String fileName() {	return fileName;}

	String relativeFilename() {	return relativeFileName;}

	String alphaSource() {	return alphaSource;}

	Vector2f uvTranslation() {	return uvTrans;}

	Vector2f uvScaling() {	return uvScaling;}

	PropertyTable props() {	return props;}

	// return a 4-tuple 
	int[] crop() {	return crop;}
}
