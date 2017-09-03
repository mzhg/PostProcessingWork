package assimp.importer.fbx;

import java.nio.ByteBuffer;
import java.util.List;

import assimp.common.DeadlyImportError;

/** Represents a delay-parsed FBX objects. Many objects in the scene
 *  are not needed by assimp, so it makes no sense to parse them
 *  upfront. */
final class LazyObject {

	static final int BEING_CONSTRUCTED = 1;
	static final int FAILED_TO_CONSTRUCT = 2;
	
	private final Document doc;
	private final Element element;
	private FBXObject object;
	
	private long id;
	private int flags;
	
	public LazyObject(long id, Element element, Document document) {
		this.id = id;
		this.doc = document;
		this.element = element;
	}
	
	FBXObject get(boolean dieOnError){
		if(isBeingConstructed() || failedToConstruct()) {
			return null;
		}

		if (object != null) {
			return object;
		}

		// if this is the root object, we return a dummy since there
		// is no root object int he fbx file - it is just referenced
		// with id 0.
		if(id == 0L) {
//			object = (new Object(id, element, "Model::RootNode"));
//			return object.get();
			object = new FBXObject(id, element, "Model::RootNode");
			return object;
		}

		Token key = element.keyToken();
		List<Token> tokens = element.tokens();

		if(tokens.size() < 3) {
			FBXUtil.DOMError("expected at least 3 tokens: id, name and class tag",element);
		}

		final Token token1 = tokens.get(1);
		String name = Parser.parseTokenAsString(token1);
		String err = Parser.get_error();
		if (err != null) {
			FBXUtil.DOMError(err,element);
		} 

		// small fix for binary reading: binary fbx files don't use
		// prefixes such as Model:: in front of their names. The
		// loading code expects this at many places, though!
		// so convert the binary representation (a 0x0001) to the
		// double colon notation.
		if(token1.isBinary()) {
			for (int i = 0; i < name.length(); ++i) {
				if (name.charAt(i) == 0x0 && name.charAt(i+1) == 0x1) {
					name = name.substring(i+2) + "::" + name.substring(0,i);
				}
			}
		}

		final String classtag = Parser.parseTokenAsString(tokens.get(2));
		err = Parser.get_error();
		if (err != null) {
			FBXUtil.DOMError(err,element);
		} 

		// prevent recursive calls
		flags |= BEING_CONSTRUCTED;

		try {
			// this needs to be relatively fast since it happens a lot,
			// so avoid constructing strings all the time.
//			const char* obtype = key.begin();
//			const size_t length = static_cast<size_t>(key.end()-key.begin());
			ByteBuffer obtype = key.contents();
			if (!FBXUtil.strncmp(obtype,"Geometry")) {
				if (!FBXUtil.strcmp(classtag,"Mesh")) {
					object=new MeshGeometry(id,element,name,doc);
				}
			}
			else if (!FBXUtil.strncmp(obtype,"NodeAttribute")) {
				if (!FBXUtil.strcmp(classtag,"Camera")) {
					object = (new FBXCamera(id,element,doc,name));
				}
				else if (!FBXUtil.strcmp(classtag,"CameraSwitcher")) {
					object = (new CameraSwitcher(id,element,doc,name));
				}
				else if (!FBXUtil.strcmp(classtag,"Light")) {
					object = (new FBXLight(id,element,doc,name));
				}
				else if (!FBXUtil.strcmp(classtag,"Null")) {
					object = (new Null(id,element,doc,name));
				}
				else if (!FBXUtil.strcmp(classtag,"LimbNode")) {
					object = (new LimbNode(id,element,doc,name));
				}
			}
			else if (!FBXUtil.strncmp(obtype,"Deformer")) {
				if (!FBXUtil.strcmp(classtag,"Cluster")) {
					object = (new Cluster(id,element,doc,name));
				}
				else if (!FBXUtil.strcmp(classtag,"Skin")) {
					object = (new Skin(id,element,doc,name));
				}
			}
			else if (!FBXUtil.strncmp(obtype,"Model")) {
				// FK and IK effectors are not supported
				if (FBXUtil.strcmp(classtag,"IKEffector") && FBXUtil.strcmp(classtag,"FKEffector")) {
					object = (new Model(id,element,doc,name));
				}
			}
			else if (!FBXUtil.strncmp(obtype,"Material")) {
				object = (new FBXMaterial(id,element,doc,name));
			}
			else if (!FBXUtil.strncmp(obtype,"Texture")) {
				object = (new FBXTexture(id,element,doc,name));
			}
			else if (!FBXUtil.strncmp(obtype,"LayeredTexture")) {
				object = (new LayeredTexture(id,element,doc,name));
			}
			else if (!FBXUtil.strncmp(obtype,"AnimationStack")) {
				object = (new AnimationStack(id,element,name,doc));
			}
			else if (!FBXUtil.strncmp(obtype,"AnimationLayer")) {
				object = (new AnimationLayer(id,element,name,doc));
			}
			// note: order matters for these two
			else if (!FBXUtil.strncmp(obtype,"AnimationCurve")) {
				object = (new AnimationCurve(id,element,name,doc));
			}
			else if (!FBXUtil.strncmp(obtype,"AnimationCurveNode")) {
				object = (new AnimationCurveNode(id,element,name,doc, null));
			}	
		}
		catch(Exception ex) {
			flags &= ~BEING_CONSTRUCTED;
			flags |= FAILED_TO_CONSTRUCT;

			if(dieOnError || doc.settings().strictMode) {
				throw new DeadlyImportError(ex);
			}

			// note: the error message is already formatted, so raw logging is ok
//			if(!DefaultLogger::isNullLogger()) {
//				DefaultLogger::get()->error(ex.what());
//			}
			return null;
		}

		if (/*!object.get()*/ object == null) {
			//DOMError("failed to convert element to DOM object, class: " + classtag + ", name: " + name,&element);
		}

		flags &= ~BEING_CONSTRUCTED;
		return object;
	}
	
	long ID() {	return id;}

	boolean isBeingConstructed() {	return (flags & BEING_CONSTRUCTED) != 0;}

	boolean failedToConstruct() {	return (flags & FAILED_TO_CONSTRUCT) != 0;}

	Element getElement() {	return element;}

	Document getDocument() {	return doc;}
	
}
