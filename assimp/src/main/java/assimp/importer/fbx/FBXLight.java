package assimp.importer.fbx;

import org.lwjgl.util.vector.Vector3f;

/** DOM base class for FBX limb node markers attached to a node */
final class FBXLight extends NodeAttribute{

//	enum Type
//	{
	static final int 
		Type_Point = 0,
		Type_Directional = 1,
		Type_Spot = 2,
		Type_Area = 3,
		Type_Volume = 4,

		Type_MAX = 5; // end-of-enum sentinel
//	};

//	enum Decay
//	{
	static final int 
		Decay_None = 0,
		Decay_Linear = 1,
		Decay_Quadratic = 2,
		Decay_Cubic = 3,

		Decay_MAX = 4; // end-of-enum sentinel
//	};
	
	public FBXLight(long id, Element element, Document doc, String name) {
		super(id, element, doc, name);
	}
	
	Vector3f color() { return PropertyTable.propertyGet(props(), "Color", new  Vector3f(1,1,1));}

	int lightType() {
		final int ival = PropertyTable.propertyGet(props(), "LightType", Integer.valueOf( 0));
		if (ival < 0 || ival >= Type_MAX/*AI_CONCAT(type, _MAX)*/) {
			assert(0 >= 0 && 0 < Type_MAX/*AI_CONCAT(type, _MAX)*/);
			return (0);
		}
		return (ival);
	}

	boolean castLightOnObject() { return PropertyTable.propertyGet(props(), "CastLightOnObject",  false);}

	boolean drawVolumetricLight() { return PropertyTable.propertyGet(props(), "DrawVolumetricLight",  true);}

	boolean drawGroundProjection() { return PropertyTable.propertyGet(props(), "DrawGroundProjection",  true);}

	boolean drawFrontFacingVolumetricLight() { return PropertyTable.propertyGet(props(), "DrawFrontFacingVolumetricLight",  false);}

	float intensity() { return PropertyTable.propertyGet(props(), "Intensity",  1.0f);}

	float innerAngle() { return PropertyTable.propertyGet(props(), "InnerAngle",  0.0f);}

	float outerAngle() { return PropertyTable.propertyGet(props(), "OuterAngle",  45.0f);}

	int fog() { return PropertyTable.propertyGet(props(), "Fog",  50);}

	int decayType() {
		final int ival = PropertyTable.propertyGet(props(), "DecayType", Integer.valueOf( 0));
		if (ival < 0 || ival >= Decay_MAX/*AI_CONCAT(type, _MAX)*/) {
			assert(0 >= 0 && 0 < Decay_MAX/*AI_CONCAT(type, _MAX)*/);
			return (0);
		}
		return (ival);
	}

	int decayStart() { return PropertyTable.propertyGet(props(), "DecayStart",  0);}

	String fileName() { return PropertyTable.propertyGet(props(), "FileName", "");}

	boolean enableNearAttenuation() { return PropertyTable.propertyGet(props(), "EnableNearAttenuation",  false);}

	float nearAttenuationStart() { return PropertyTable.propertyGet(props(), "NearAttenuationStart",  0.0f);}

	float nearAttenuationEnd() { return PropertyTable.propertyGet(props(), "NearAttenuationEnd",  0.0f);}

	boolean enableFarAttenuation() { return PropertyTable.propertyGet(props(), "EnableFarAttenuation",  false);}

	float farAttenuationStart() { return PropertyTable.propertyGet(props(), "FarAttenuationStart",  0.0f);}

	float farAttenuationEnd() { return PropertyTable.propertyGet(props(), "FarAttenuationEnd",  0.0f);}

	boolean castShadows() { return PropertyTable.propertyGet(props(), "CastShadows",  true);}

	Vector3f shadowColor() { return PropertyTable.propertyGet(props(), "ShadowColor", new  Vector3f(0,0,0));}

	int areaLightShape() { return PropertyTable.propertyGet(props(), "AreaLightShape",  0);}

	float leftBarnDoor() { return PropertyTable.propertyGet(props(), "LeftBarnDoor",  20.0f);}

	float rightBarnDoor() { return PropertyTable.propertyGet(props(), "RightBarnDoor",  20.0f);}

	float topBarnDoor() { return PropertyTable.propertyGet(props(), "TopBarnDoor",  20.0f);}

	float bottomBarnDoor() { return PropertyTable.propertyGet(props(), "BottomBarnDoor",  20.0f);}

	boolean enableBarnDoor() { return PropertyTable.propertyGet(props(), "EnableBarnDoor",  true);}
}
