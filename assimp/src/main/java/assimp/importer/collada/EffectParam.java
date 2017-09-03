package assimp.importer.collada;

/** A param for an effect. Might be of several types, but they all just refer to each other, so I summarize them */
final class EffectParam {

	int mType;
	/**  to which other thing the param is referring to. */
	String mReference; 
}
