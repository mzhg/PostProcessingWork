package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public class VaCameraControllerFocusLocationsFlythrough implements VaCameraControllerBase {

    protected final List<Keyframe> m_keys = new ArrayList<>();
    protected int                  m_currentKeyIndex;
    protected float                m_currentKeyTimeRemaining;

    protected float                m_userParam0;
    protected float                m_userParam1;

    protected boolean              m_fixedUp;
    protected final Vector3f       m_fixedUpVec = new Vector3f();

    public static final class Keyframe
    {
        public final Vector3f          Position = new Vector3f();
        public final Quaternion        Orientation = new Quaternion();
        public float                   ShowTime;
        public float                   UserParam0;
        public float                   UserParam1;

        public Keyframe(ReadableVector3f position, Quaternion orientation, float showTime, float userParam0 /*= 0.0f*/, float userParam1 /*= 0.0f*/ )// : Position(position), Orientation( orientation ), ShowTime(showTime), UserParam0( userParam0 ), UserParam1(userParam1) { }
        {
            Position.set(position);
            Orientation.set(orientation);

            ShowTime = showTime;
            UserParam0 = userParam0;
            UserParam1 = userParam1;
        }
    }

    public void AddKey( Keyframe key )                          { if(key != null) m_keys.add( key ); }
    public void ResetTime( )                                    { m_currentKeyIndex = -1; m_currentKeyTimeRemaining = 0.0f; }

    public void       SetFixedUp( boolean enabled, ReadableVector3f upVec /*= vaVector3( 0.0f, 0.0f, 1.0f )*/ )   { m_fixedUp = enabled; m_fixedUpVec.set(upVec); }
    public final void SetFixedUp( boolean enabled)   { m_fixedUp = enabled; m_fixedUpVec.set(Vector3f.Z_AXIS); }

    @Override
    public void CameraAttached(VaCameraBase camera) {

    }

    @Override
    public void CameraTick(float deltaTime, VaCameraBase camera, boolean hasFocus) {
        if( m_keys.size( ) == 0 )
            return;

        m_currentKeyTimeRemaining -= deltaTime;

        while( m_currentKeyTimeRemaining < 0 )
        {
            m_currentKeyIndex = ( m_currentKeyIndex + 1 ) % m_keys.size( );
            m_currentKeyTimeRemaining += m_keys.get(m_currentKeyIndex).ShowTime;
        }

        Keyframe  currentKey = m_keys.get(m_currentKeyIndex);
        Keyframe  nextKey = m_keys.get(( m_currentKeyIndex + 1 ) % m_keys.size( ));

        float lerpK = Numeric.smoothstep( 1.0f - m_currentKeyTimeRemaining / currentKey.ShowTime );

//        vaVector3 pos = currentKey.Position * ( 1.0f - lerpK ) + nextKey.Position * lerpK;
        m_userParam0 = currentKey.UserParam0 * ( 1.0f - lerpK ) + nextKey.UserParam0 * lerpK;
        m_userParam1 = currentKey.UserParam1 * ( 1.0f - lerpK ) + nextKey.UserParam1 * lerpK;
//        vaQuaternion rot = vaQuaternion::Slerp( currentKey.Orientation, nextKey.Orientation, lerpK );
        final Vector3f pos = CacheBuffer.getCachedVec3();
        final Vector3f currentUp = CacheBuffer.getCachedVec3();
        final Quaternion rot = CacheBuffer.getCachedQuat();
        final Quaternion tmp0 = CacheBuffer.getCachedQuat();

        Vector3f.linear(currentKey.Position, 1.0f-lerpK, nextKey.Position, lerpK, pos);
        Quaternion.slerp(currentKey.Orientation, nextKey.Orientation, lerpK, rot);

        try{
            if( m_fixedUp )
            {
//                ReadableVector3f currentUp = rot.GetAxisY();
                Quaternion.getAxisY(rot, currentUp);

                /*vaVector3 rotAxis   = vaVector3::Cross( currentUp, m_fixedUpVec );
                float rotAngle      = vaVector3::AngleBetweenVectors( currentUp, m_fixedUpVec );*/
                float rotAngle = Vector3f.angle(currentUp, m_fixedUpVec);
                Vector3f rotAxis = Vector3f.cross(currentUp, m_fixedUpVec, currentUp);

//                rot *= vaQuaternion::RotationAxis( rotAxis, rotAngle );
                tmp0.setFromAxisAngle(rotAxis, rotAngle);
                Quaternion.mul(tmp0, rot, rot);  // TODO

            }

            float lf = TimeIndependentLerpF( deltaTime, 5.0f / (currentKey.ShowTime+2.0f) );

//            pos = vaMath::Lerp( camera.GetPosition(), pos, lf );
//            rot = vaQuaternion::Slerp( camera.GetOrientation(), rot, lf );
            Vector3f.mix(camera.GetPosition(), pos, lf, pos);
            Quaternion.slerp(camera.GetOrientation(), rot, lf, rot);

            camera.SetPosition( pos );
            camera.SetOrientation( rot );
        }finally {
            CacheBuffer.free(pos);
            CacheBuffer.free(rot);
            CacheBuffer.free(tmp0);
            CacheBuffer.free(currentUp);
        }
    }

    // Time independent lerp function. The bigger the lerpRate, the faster the lerp!
    private static float TimeIndependentLerpF(float deltaTime, float lerpRate)
    {
        return (float) (1.0 - Math.exp( -Math.abs(deltaTime*lerpRate) ));
    }
}
