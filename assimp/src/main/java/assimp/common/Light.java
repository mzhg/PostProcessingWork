package assimp.common;

import org.lwjgl.util.vector.Vector3f;

/** Helper structure to describe a light source.<p>
*
*  Assimp supports multiple sorts of light sources, including
*  directional, point and spot lights. All of them are defined with just
*  a single structure and distinguished by their parameters.
*  Note - some file formats (such as 3DS, ASE) export a "target point" -
*  the point a spot light is looking at (it can even be animated). Assimp
*  writes the target point as a subnode of a spotlights's main node,
*  called "<spotName>.Target". However, this is just additional information
*  then, the transformation tracks of the main node make the
*  spot light already point in the right direction.
*/
public class Light implements NamedObject, Copyable<Light>{
	
	// This size doesn't include the mName and mType variables.
	static final int _SIZE = 20 * 4;

	/** The name of the light source.<p>
	 *
	 *  There must be a node in the scenegraph with the same name.
	 *  This node specifies the position of the light in the scene
	 *  hierarchy and can be animated.
	 */
	public String mName;

	/** The type of the light source.<p>
	 *
	 * aiLightSource_UNDEFINED is not a valid value for this member.
	 */
	public LightSourceType mType;

	/** Position of the light source in space. Relative to the
	 *  transformation of the node corresponding to the light.<p>
	 *
	 *  The position is undefined for directional lights.
	 */
	public final Vector3f mPosition = new Vector3f();

	/** Direction of the light source in space. Relative to the
	 *  transformation of the node corresponding to the light.<p>
	 *
	 *  The direction is undefined for point lights. The vector
	 *  may be normalized, but it needn't.
	 */
	public final Vector3f mDirection = new Vector3f();

	/** Constant light attenuation factor. <p>
	 *
	 *  The intensity of the light source at a given distance 'd' from
	 *  the light's position is
	 *  <pre>
	 *  Atten = 1/( att0 + att1 * d + att2 * d*d)
	 *  </pre>
	 *  This member corresponds to the att0 variable in the equation.
	 *  Naturally undefined for directional lights.
	 */
	public float mAttenuationConstant;

	/** Linear light attenuation factor. <p>
	 *
	 *  The intensity of the light source at a given distance 'd' from
	 *  the light's position is
	 *  <pre>
	 *  Atten = 1/( att0 + att1 * d + att2 * d*d)
	 *  </pre>
	 *  This member corresponds to the att1 variable in the equation.
	 *  Naturally undefined for directional lights.
	 */
	public float mAttenuationLinear = 1.0f;

	/** Quadratic light attenuation factor. <p>
	 *  
	 *  The intensity of the light source at a given distance 'd' from
	 *  the light's position is
	 *  <pre>
	 *  Atten = 1/( att0 + att1 * d + att2 * d*d)
	 *  </pre>
	 *  This member corresponds to the att2 variable in the equation.
	 *  Naturally undefined for directional lights.
	 */
	public float mAttenuationQuadratic;

	/** Diffuse color of the light source<p>
	 *
	 *  The diffuse light color is multiplied with the diffuse 
	 *  material color to obtain the final color that contributes
	 *  to the diffuse shading term.
	 */
	public final Vector3f mColorDiffuse = new Vector3f();

	/** Specular color of the light source<p>
	 *
	 *  The specular light color is multiplied with the specular
	 *  material color to obtain the final color that contributes
	 *  to the specular shading term.
	 */
	public final Vector3f mColorSpecular = new Vector3f();

	/** Ambient color of the light source<p>
	 *
	 *  The ambient light color is multiplied with the ambient
	 *  material color to obtain the final color that contributes
	 *  to the ambient shading term. Most renderers will ignore
	 *  this value it, is just a remaining of the fixed-function pipeline
	 *  that is still supported by quite many file formats.
	 */
	public final Vector3f mColorAmbient = new Vector3f();

	/** Inner angle of a spot light's light cone.<p>
	 *
	 *  The spot light has maximum influence on objects inside this
	 *  angle. The angle is given in radians. It is 2PI for point 
	 *  lights and undefined for directional lights.
	 */
	public float mAngleInnerCone = (float)(Math.PI * 2.0);

	/** Outer angle of a spot light's light cone.<p>
	 *
	 *  The spot light does not affect objects outside this angle.
	 *  The angle is given in radians. It is 2PI for point lights and 
	 *  undefined for directional lights. The outer angle must be
	 *  greater than or equal to the inner angle.
	 *  It is assumed that the application uses a smooth
	 *  interpolation between the inner and the outer cone of the
	 *  spot light. 
	 */
	public float mAngleOuterCone = (float)(Math.PI * 2.0);
	
	@Override
	public String getName() { return mName;}

	@Override
	public Light copy() {
		Light light = new Light();
		light.mAngleInnerCone = mAngleInnerCone;
		light.mAngleOuterCone = mAngleOuterCone;
		light.mAttenuationConstant = mAngleInnerCone;
		light.mAttenuationLinear = mAttenuationLinear;
		light.mAttenuationQuadratic = mAttenuationQuadratic;
		light.mColorAmbient.set(mColorAmbient);
		light.mColorDiffuse.set(mColorDiffuse);
		light.mColorSpecular.set(mColorSpecular);
		light.mDirection.set(mDirection);
		light.mName = mName;
		light.mPosition.set(mPosition);
		light.mType = mType;
		return light;
	}
}
