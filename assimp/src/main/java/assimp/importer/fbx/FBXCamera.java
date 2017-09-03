package assimp.importer.fbx;

import org.lwjgl.util.vector.Vector3f;

/** DOM base class for FBX cameras attached to a node */
final class FBXCamera extends NodeAttribute{

	public FBXCamera(long id, Element element, Document doc, String name) {
		super(id, element, doc, name);
	}

	Vector3f position() { return PropertyTable.propertyGet(props(), "Position", new  Vector3f(0,0,0));}

	Vector3f upVector() { return PropertyTable.propertyGet(props(), "UpVector", new  Vector3f(0,1,0));}

	Vector3f interestPosition() { return PropertyTable.propertyGet(props(), "InterestPosition", new  Vector3f(0,0,0));}

	float aspectWidth() { return PropertyTable.propertyGet(props(), "AspectWidth",  1.0f);}

	float aspectHeight() { return PropertyTable.propertyGet(props(), "AspectHeight",  1.0f);}

	float filmWidth() { return PropertyTable.propertyGet(props(), "FilmWidth",  1.0f);}

	float filmHeight() { return PropertyTable.propertyGet(props(), "FilmHeight",  1.0f);}

	float filmAspectRatio() { return PropertyTable.propertyGet(props(), "FilmAspectRatio",  1.0f);}

	int apertureMode() { return PropertyTable.propertyGet(props(), "ApertureMode",  0);}

	float fieldOfView() { return PropertyTable.propertyGet(props(), "FieldOfView",  1.0f);}

	float focalLength() { return PropertyTable.propertyGet(props(), "FocalLength",  1.0f);}
}
