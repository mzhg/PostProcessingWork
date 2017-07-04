package jet.opengl.demos.os;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/6/7.
 */

abstract class RaycastSelector {

    OnTouchEventListener m_pTouchEventListener;

    float m_WatchingTime;
    final Vector3f m_LastDirection = new Vector3f(0.0f, 0.0f, -1.0f);

    private float m_DeltaAngle;
    private float m_WatchingThreshold = 3.0f;
    private boolean m_IsActived = true;

    RaycastSelector(OnTouchEventListener listener) {
        m_pTouchEventListener = listener;
        m_DeltaAngle = (float)Math.toRadians(30.0f);
    }

    void setTouchEventListener(OnTouchEventListener listener)
    {
        m_pTouchEventListener = listener;
    }

    OnTouchEventListener getTouchEventListener() { return m_pTouchEventListener;}

    void setDeltaAngle(float angleInRadians) {m_DeltaAngle = angleInRadians;}
    float getDeltaAngle() { return m_DeltaAngle;}

    void setWatchingThreshold(float threshold) {m_WatchingThreshold = threshold;}
    float getWatchingThreshold() { return m_WatchingThreshold;}

    void enable() {m_IsActived = true;}
    void disable() {m_IsActived = false;}
    boolean isActive() { return m_IsActived;}

    // return true if the changed angle < m_DeltaAngle
    boolean testDeltaAngle(ReadableVector3f newDir){
        float angleBetwwen = Vector3f.angle(m_LastDirection, newDir);
        boolean result = angleBetwwen < m_DeltaAngle;
        m_LastDirection.set(newDir);
        return result;
    }

    void onWatching(int id, Drawable drawable){
        if (m_pTouchEventListener != null && m_WatchingTime > m_WatchingThreshold){
            m_pTouchEventListener.OnWatching(id, drawable, m_WatchingTime);
        }
    }
}
