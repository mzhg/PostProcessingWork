package assimp.common;

import org.lwjgl.util.vector.Vector3f;

/**
 * Container for holding metadata.
 *
 * Metadata is a key-value store using string keys and values.
 */
public class Metadata {

	/** Arrays of keys, may not be null. Entries in this array may not be null as well. */
	public String[] mKeys;

	/** Arrays of values, may not be null. Entries in this array may be null if the
	  * corresponding property key has no assigned value. */
	public MetadataEntry[] mValues;
	
	/** Length of the mKeys and mValues arrays, respectively */
	public int getNumProperties() { return mKeys != null ? mKeys.length : 0;}
	
	public void set(int index, String key, Object value){
		// Set metadata key
		mKeys[index] = key;

		// Copy the given value to the dynamic storage
		mValues[index].mData = value;
		
		// Measure the data type
		if(value instanceof Integer){
			mValues[index].mType = MetadataType.AI_INT;
		}else if(value instanceof Float){
			mValues[index].mType = MetadataType.AI_FLOAT;
		}else if(value instanceof Long){
			mValues[index].mType = MetadataType.AI_UINT64;
		}else if(value instanceof Boolean){
			mValues[index].mType = MetadataType.AI_BOOL;
		}else if(value instanceof Vector3f){
			mValues[index].mType = MetadataType.AI_AIVECTOR3D;
		}else if(value instanceof String){
			mValues[index].mType = MetadataType.AI_AISTRING;
		}else{
			throw new IllegalArgumentException("Unaccept Type: " + value.getClass().getName());
		}
	}
	
//	public void set(int index, String key, int value){
//		// Set metadata key
//		mKeys[index] = key;
//
//		// Set metadata type
//		mValues[index].mType = MetadataType.AI_INT;
//		// Copy the given value to the dynamic storage
//		mValues[index].mData = value;
//	}
//	
//	public void set(int index, String key, float value){
//		// Set metadata key
//		mKeys[index] = key;
//
//		// Set metadata type
//		mValues[index].mType = MetadataType.AI_FLOAT;
//		// Copy the given value to the dynamic storage
//		mValues[index].mData = value;
//	}
//	
//	public void set(int index, String key, long value){
//		// Set metadata key
//		mKeys[index] = key;
//
//		// Set metadata type
//		mValues[index].mType = MetadataType.AI_UINT64;
//		// Copy the given value to the dynamic storage
//		mValues[index].mData = value;
//	}
//	
//	public void set(int index, String key, boolean value){
//		// Set metadata key
//		mKeys[index] = key;
//
//		// Set metadata type
//		mValues[index].mType = MetadataType.AI_BOOL;
//		// Copy the given value to the dynamic storage
//		mValues[index].mData = value;
//	}
//	
//	public void set(int index, String key, String value){
//		// Set metadata key
//		mKeys[index] = key;
//
//		// Set metadata type
//		mValues[index].mType = MetadataType.AI_AISTRING;
//		// Copy the given value to the dynamic storage
//		mValues[index].mData = value;
//	}
//	
//	public void set(int index, String key, Vector3f value){
//		// Set metadata key
//		mKeys[index] = key;
//
//		// Set metadata type
//		mValues[index].mType = MetadataType.AI_AIVECTOR3D;
//		// Copy the given value to the dynamic storage
//		mValues[index].mData = new Vector3f(value);
//	}
	
	public Object get(int index){ return mValues[index].mData;}
	
	public Object get(String key) {
		int numProperties = getNumProperties();
		for(int i = 0; i < numProperties; i++){
			if(mKeys[i] == key || (key != null && mKeys[i].equals(key))){
				return get(i);
			}
		}
		
		return null;
	}
}
