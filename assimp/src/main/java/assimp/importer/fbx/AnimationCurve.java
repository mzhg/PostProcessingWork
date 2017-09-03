package assimp.importer.fbx;

import assimp.common.AssUtil;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

/** Represents a FBX animation curve (i.e. a 1-dimensional set of keyframes and values therefor) */
final class AnimationCurve extends FBXObject{
	
	private LongArrayList keys;
	private FloatArrayList values;
	private FloatArrayList attributes;
	private IntArrayList flags;
	
	public AnimationCurve(long id, Element element, String name, Document doc) {
		super(id, element, name);
		
		Scope sc = Parser.getRequiredScope(element);
		Element KeyTime = Parser.getRequiredElement(sc,"KeyTime", null);
		Element KeyValueFloat = Parser.getRequiredElement(sc,"KeyValueFloat", null);

		keys = LongArrayList.wrap(AssUtil.EMPTY_LONG);
		
		Parser.parseVectorDataArray(keys, KeyTime);
		Parser.parseVectorDataArray1f(values, KeyValueFloat);

		if(keys.size() != values.size()) {
			FBXUtil.DOMError("the number of key times does not match the number of keyframe values",KeyTime);
		}
		
		// check if the key times are well-ordered
//		if(!std::equal(keys.begin(), keys.end() - 1, keys.begin() + 1, std::less<KeyTimeList::value_type>())) {
//			FBXUtil.DOMError("the keyframes are not in ascending order",KeyTime);
//		}
		if(!AssUtil.equals(keys.elements(), 0, keys.elements(), 1, keys.size() - 1, new AssUtil.BinaryPredicateLong() {
			@Override
			public boolean accept(long l, long r) {
				return l < r;
			}
		})){
			FBXUtil.DOMError("the keyframes are not in ascending order",KeyTime);
		}

		Element KeyAttrDataFloat = sc.get("KeyAttrDataFloat");
		if(KeyAttrDataFloat != null) {
			attributes = FloatArrayList.wrap(AssUtil.EMPTY_FLOAT);
			Parser.parseVectorDataArray1f(attributes, KeyAttrDataFloat);
		}

		Element KeyAttrFlags = sc.get("KeyAttrFlags");
		if(KeyAttrFlags != null) {
			flags = IntArrayList.wrap(AssUtil.EMPTY_INT);
			Parser.parseVectorDataArray1i(flags, KeyAttrFlags);
		}
	}
	
	/** get list of keyframe positions (time).
	 *  Invariant: |GetKeys()| > 0 */
	LongArrayList getKeys() { return keys;}

	/** get list of keyframe values. 
	  * Invariant: |GetKeys()| == |GetValues()| && |GetKeys()| > 0*/
	FloatArrayList getValues() {	return values;}

	FloatArrayList getAttributes() {	return attributes;}

	IntArrayList getFlags() {	return flags;}
}
