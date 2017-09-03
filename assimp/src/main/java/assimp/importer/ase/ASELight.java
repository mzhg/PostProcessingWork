package assimp.importer.ase;

import org.lwjgl.util.vector.Vector3f;

/** Helper structure to represent an ASE light source */
final class ASELight extends BaseNode{

//	enum LightType
//	{
	static final int 
		OMNI = 0,
		TARGET = 1,
		FREE = 2,
		DIRECTIONAL = 3;
//	};
	
	int mLightType= OMNI;
	final Vector3f mColor = new Vector3f(1.f, 1.f, 1.f);
	float mIntensity = 1.0f; // light is white by default
	float mAngle = 45.f; // in degrees
	float mFalloff = 0.f;
	
	public ASELight() {
		super(LIGHT);
	}
}
