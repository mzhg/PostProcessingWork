package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/11/21.
 */

public abstract class VaSky extends VaDrawableRenderingModule{

    // these are calculated from azimuth & elevation, but smoothly interpolated to avoid sudden changes
    private final Vector3f      m_sunDirTargetL0 = new Vector3f();    // directly calculated from azimuth & elevation
    private final Vector3f      m_sunDirTargetL1 = new Vector3f();    // lerped to m_sunDirTargetL0
    private final Vector3f      m_sunDir = new Vector3f();            // final, lerped to m_sunDirTargetL1

    private final Settings      m_settings = new Settings();

    protected VaSky(){
        assert( VaRenderingCore. IsInitialized() );

        /*m_sunDir = vaVector3( 0.0f, 0.0f, 0.0f );
        m_sunDirTargetL0 = vaVector3( 0.0f, 0.0f, 0.0f );
        m_sunDirTargetL1 = vaVector3( 0.0f, 0.0f, 0.0f );*/

        m_settings.SunAzimuth           = 0.320f;//0.0f / 180.0f * (float)VA_PI;
        m_settings.SunElevation         = 15.0f / 180.0f * Numeric.PI;
        m_settings.SkyColorLowPow       = 6.0f;
        m_settings.SkyColorLowMul       = 1.0f;
        m_settings.SkyColorLow          .set( 0.4f, 0.4f, 0.9f, 0.0f );
        m_settings.SkyColorHigh         .set( 0.0f, 0.0f, 0.6f, 0.0f );
        m_settings.SunColorPrimary      .set( 1.0f, 1.0f, 0.9f, 0.0f );
        m_settings.SunColorSecondary    .set( 1.0f, 1.0f, 0.7f, 0.0f );
        m_settings.SunColorPrimaryPow   = 500.0f;
        m_settings.SunColorPrimaryMul   = 2.5f;
        m_settings.SunColorSecondaryPow = 5.0f;
        m_settings.SunColorSecondaryMul = 0.2f;

        m_settings.FogColor             .set( 0.4f, 0.4f, 0.9f );
        m_settings.FogDistanceMin       = 100.0f;
        m_settings.FogDensity           = 0.0007f;
    }

    public Settings                 GetSettings( )          { return m_settings; }
    public ReadableVector3f         GetSunDir( )            { return m_sunDir; }

    // Time independent lerp function. The bigger the lerpRate, the faster the lerp!
    private static float TimeIndependentLerpF(float deltaTime, float lerpRate)
    {
        return (float) (1.0 - Math.exp( -Math.abs(deltaTime*lerpRate) ));
    }

    public void Tick( float deltaTime, VaLighting lightingToUpdate ){
        // this smoothing is not needed here, but I'll leave it in anyway
        final float someValue = 10000000.0f;
        float lf = TimeIndependentLerpF( deltaTime, someValue );

        final Vector3f sunDirTargetL0    = m_sunDirTargetL0;
        final Vector3f sunDirTargetL1    = m_sunDirTargetL1;
        final Vector3f sunDir            = m_sunDir;

        if( sunDir.x < 1e-5f )
            lf = 1.0f;

        /*vaMatrix4x4 mCameraRot;
        vaMatrix4x4 mRotationY = vaMatrix4x4::RotationY( m_settings.SunElevation );
        vaMatrix4x4 mRotationZ = vaMatrix4x4::RotationZ( m_settings.SunAzimuth );
        mCameraRot = mRotationY * mRotationZ;
        sunDirTargetL0 = -mCameraRot.GetRotationX();*/

        final Matrix4f mCameraRot = CacheBuffer.getCachedMatrix();
        mCameraRot.setIdentity();
        mCameraRot.rotate(m_settings.SunAzimuth, Vector3f.Z_AXIS);
        mCameraRot.rotate(m_settings.SunElevation, Vector3f.Y_AXIS);
        sunDirTargetL0.x = -mCameraRot.m00;
        sunDirTargetL0.y = -mCameraRot.m01;
        sunDirTargetL0.z = -mCameraRot.m02;

        /*sunDirTargetL1 = vaMath::Lerp( sunDirTargetL1, sunDirTargetL0, lf );
        sunDir = vaMath::Lerp( sunDir, sunDirTargetL1, lf );*/
        Vector3f.mix(sunDirTargetL1, sunDirTargetL0, lf, sunDirTargetL1);
        Vector3f.mix(sunDir, sunDirTargetL1, lf, sunDir);

        /*sunDirTargetL0 = sunDirTargetL0.Normalize();
        sunDirTargetL1 = sunDirTargetL1.Normalize();
        sunDir = sunDir.Normalize();*/
        sunDirTargetL0.normalise();
        sunDirTargetL1.normalise();
        sunDir.normalise();

//        m_sunDirTargetL0= sunDirTargetL0;
//        m_sunDirTargetL1= sunDirTargetL1;
//        m_sunDir        = sunDir;

//        vaVector3 ambientLightColor = vaVector3( 0.1f, 0.1f, 0.1f );

        if( lightingToUpdate != null )
        {
            lightingToUpdate.SetDirectionalLightDirection( -m_sunDir.x, -m_sunDir.y, -m_sunDir.z );
            lightingToUpdate.SetFogParams( m_settings.FogColor, m_settings.FogDistanceMin, m_settings.FogDensity );
        }
    }

    public static class Settings
    {
        public float                       SunAzimuth;
        public float                       SunElevation;

        public final Vector4f              SkyColorLow = new Vector4f();
        public final Vector4f              SkyColorHigh = new Vector4f();

        public final Vector4f              SunColorPrimary = new Vector4f();
        public final Vector4f              SunColorSecondary = new Vector4f();

        public float                       SkyColorLowPow;
        public float                       SkyColorLowMul;

        public float                       SunColorPrimaryPow;
        public float                       SunColorPrimaryMul;
        public float                       SunColorSecondaryPow;
        public float                       SunColorSecondaryMul;

        public final Vector3f              FogColor = new Vector3f();
        public float                       FogDistanceMin;
        public float                       FogDensity;
    };
}
