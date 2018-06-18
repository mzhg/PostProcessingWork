package jet.opengl.demos.intel.fluid.scene;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Scene node representing a light.<p></p>
 * Created by Administrator on 2018/3/14 0014.
 */

public class Light extends SceneNodeBase {
    public static final int sTypeId = 1818850405 /*'lite'*/ ;  ///< Type identifier for light scene nodes

    private LightTypeE  mLightType =LightTypeE.DIRECTIONAL             ;   ///< Type of light this is
    private final Vector4f mDirection = new Vector4f(0.0f , 0.0f , 1.0f , 0.0f);   ///< Direction light travels.  NOTE: That differs from OpenGL's directional light, which points toward the light source.
    private final Vector4f mAmbientColor = new Vector4f(0.0f , 0.0f , 0.0f , 1.0f);   ///< Color of contribution from this light to ambient light term
    private final Vector4f mDiffuseColor = new Vector4f(1.0f , 1.0f , 1.0f , 1.0f);   ///< Color of contribution from this light to diffuse light term
    private final Vector4f mSpecularColor = new Vector4f(1.0f , 1.0f , 1.0f , 1.0f);   ///< Color of contribution from this light to specular light term
    private float mRange = Float.MAX_VALUE;   ///< Range, in world units, of this light's influence
    private float mSpotFalloff            ;   ///< Exponent of intensity falloff between inner and outer angle
    private float mSpotInnerAngle         ;   ///< Angle over which bright center of spotlight shines (like an inverse umbra)
    private float mSpotOuterAngle =180.0f        ;   ///< Total angle over which spotlight shines (like an inverse penumbra)
    private float mConstAttenuation       ;   ///< Contant term of intensity attenuation formula
    private float mLinearAttenuation      ;   ///< Coefficient of intensity attenuation term linearly proportional to distance
    private float mQuadraticAttenuation   ;   ///< Coefficient of intensity attenuation term proportional to distance squared

    /// Light varieties.
    enum LightTypeE
    {
        AMBIENT     ,   ///< Ambient light; affects ambient materials.  No position or direction.
        POINT       ,   ///< Point light; has a position and shines in all directions.
        SPOT        ,   ///< Spotlight; has a position and a direction.
        DIRECTIONAL ,   ///< Direction light; has no position (effectively infinitely far away) but has a direction.
        NUM_LIGHT_TYPES ///< Number of light varieties
    } ;

    public Light( ISceneManager sceneManager ){
        super(sceneManager, sTypeId);
        setPosition( new Vector4f( 0.0f , 0.0f , 2.0f , 1.0f ) ) ;
    }

    public void render(){
        renderChildren() ;
    }


    /// Set type of light this is.
    public void setLightType(LightTypeE lightType )   { mLightType = lightType ; }

    /// Return type of light this is.
    public  LightTypeE getLightType()              { return mLightType ; }

    /// Set world-space direction directional/spot light shines, relative to parent transform.
    /// TODO: FIXME: Direction should correspond to the SceneNode's orientation, specifically to the x-axis, i.e. to a vector initially along the x-axis, transformed by the orientation matrix.  This should correspond to the 1st column vector in the orientation matrix.
    public void setDirection(ReadableVector3f direction )
    {
        mDirection.set(direction);
        mDirection.w = 0.0f ;
    }

    /// Return world-space direction directional/spot light shines, relative to parent transform.
    /// TODO: FIXME: Direction should correspond to the SceneNode's orientation, specifically to the x-axis, i.e. to a vector initially along the x-axis, transformed by the orientation matrix.  This should correspond to the 1st column vector in the orientation matrix.
    public ReadableVector3f getDirection() { return mDirection ; }

    /// Set light ambient color.
    public void setAmbientColor( ReadableVector4f ambientColor ) { mAmbientColor.set(ambientColor);}

    /// Return Color of contribution from this light to ambient light term.
    public ReadableVector4f getAmbientColor()                { return mAmbientColor ; }

    /// Set light diffuse color.
    public void setDiffuseColor(ReadableVector4f diffuseColor )   { mDiffuseColor.set(diffuseColor);}

    /// Return Color of contribution from this light to diffuse light term.
    public ReadableVector4f getDiffuseColor()                { return mDiffuseColor ; }

    /// Return Color of contribution from this light to specular light term.
    public ReadableVector4f getSpecularColor()                { return mSpecularColor ; }

    /// Return Range, in world units, of this light's influence.
    public float getRange()                       { return mRange ; }

    /// Return Exponent of intensity falloff between inner and outer angle.
    public float getSpotFalloff()                 { return mSpotFalloff ; }

    /// Return Angle over which bright center of spotlight shines (like an inverse umbra).
    public float getSpotInnerAngle()              { return mSpotInnerAngle ; }

    /// Return Total angle over which spotlight shines (like an inverse penumbra).
    public float getSpotOuterAngle()              { return mSpotOuterAngle ; }

    /// Set intensity attenuation coefficients
    public void setAttenuation( float a0 , float a1 , float a2 )
    {
        mConstAttenuation = a0 ;
        mLinearAttenuation = a1 ;
        mQuadraticAttenuation = a2 ;
    }

    /// Return contant term of intensity attenuation formula.
    public float getConstAttenuation()           { return mConstAttenuation ; }

    /// Return coefficient of intensity attenuation term linearly proportional to distance.
    public float getLinearAttenuation()           { return mLinearAttenuation ; }

    /// Return coefficient of intensity attenuation term proportional to distance squared.
    public float getQuadracticAttenuation()       { return mQuadraticAttenuation ; }
}
