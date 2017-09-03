package assimp.importer.collada;

import org.lwjgl.util.vector.Vector3f;

/** A collada light source. */
final class COLLight {

	//! Type of the light source aiLightSourceType + ambient
	int mType;

	//! Color of the light
	final Vector3f mColor = new Vector3f();

	//! Light attenuation
	float mAttConstant = 1.0f,mAttLinear,mAttQuadratic;

	//! Spot light falloff
	float mFalloffAngle = 180.0f;
	float mFalloffExponent;

	// -----------------------------------------------------
	// FCOLLADA extension from here

	//! ... related stuff from maja and max extensions
	float mPenumbraAngle = COLEnum.ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET;
	float mOuterAngle    = COLEnum.ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET;

	//! Common light intensity
	float mIntensity = 1.0f;
}
