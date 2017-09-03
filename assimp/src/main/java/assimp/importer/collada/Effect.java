package assimp.importer.collada;

import java.util.Map;

import org.lwjgl.util.vector.Vector4f;

/** A collada effect. Can contain about anything according to the Collada spec,
but we limit our version to a reasonable subset. */
final class Effect {

	// Shading mode
	int mShadeType;

	// Colors
//	aiColor4D mEmissive, mAmbient, mDiffuse, mSpecular,
//		mTransparent, mReflective;
	final Vector4f mEmissive = new Vector4f(0, 0, 0, 1);
	final Vector4f mAmbient = new Vector4f(0.1f, 0.1f, 0.1f, 1);
	final Vector4f mDiffuse = new Vector4f(0.6f, 0.6f, 0.6f, 1);
	final Vector4f mSpecular = new Vector4f(0.4f, 0.4f, 0.4f, 1);
	final Vector4f mTransparent = new Vector4f(0.f, 0.f, 0.f, 1);
	final Vector4f mReflective = new Vector4f();

	// Textures
	Sampler mTexEmissive, mTexAmbient, mTexDiffuse, mTexSpecular,
		mTexTransparent, mTexBump, mTexReflective;

	// Scalar factory
	float mShininess = 10.0f, mRefractIndex = 1.f, mReflectivity =1.f;
	float mTransparency;

	// local params referring to each other by their SID
//	typedef std::map<std::string, Collada::EffectParam> ParamLibrary;
//	ParamLibrary mParams;
	Map<String, EffectParam> mParams;

	// MAX3D extensions
	// ---------------------------------------------------------
	// Double-sided?
	boolean mDoubleSided, mWireframe, mFaceted;
}
