package assimp.common;

import org.lwjgl.util.vector.Vector2f;

/** Defines how an UV channel is transformed.<p>
*
*  This is just a helper structure for the #AI_MATKEY_UVTRANSFORM key.
*  See its documentation for more details. <p>
*
*  Typically you'll want to build a matrix of this information. However,
*  we keep separate scaling/translation/rotation values to make it
*  easier to process and optimize UV transformations internally.
*/
public class UVTransform {
	
	/** The size of the UVTransform. */
	public static final int SIZE = 5 * 4;
	
	/** Translation on the u and v axes. <p>
	 *
	 *  The default value is (0|0).
	 */
	public final Vector2f mTranslation = new Vector2f();

	/** Scaling on the u and v axes. <p>
	 *
	 *  The default value is (1|1).
	 */
	public final Vector2f mScaling = new Vector2f(1, 1);

	/** Rotation - in counter-clockwise direction.<p>
	 *
	 *  The rotation angle is specified in radians. The
	 *  rotation center is 0.5f|0.5f. The default value
    *  0.f.
	 */
	public float mRotation;
	
	public void load(float[] a, int offset){
		mTranslation.x = a[offset++];
		mTranslation.y = a[offset++];
		mScaling.x = a[offset++];
		mScaling.y = a[offset++];
		
		if(a.length - offset >= 5){
			mRotation = a[offset++];
		}
	}
}
