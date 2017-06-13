package com.nvidia.developer.opengl.demos.os;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.List;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/6/7.
 */

final class SceneRaycastSelector extends RaycastSelector{
    static final int EVENT_INVALID_ID = -1;

    private Ray m_Ray;
    private Drawable m_pSelectedDrawable;
    private final Matrix4f m_model = new Matrix4f();
    private final Vector3f[] m_RectPos = new Vector3f[4];

    SceneRaycastSelector(OnTouchEventListener listener, Ray ray) {
        super(listener);
        m_Ray = ray;

        m_RectPos[0] = new Vector3f(-1,-1, 0);
        m_RectPos[1] = new Vector3f(+1,-1, 0);
        m_RectPos[2] = new Vector3f(-1,+1, 0);
        m_RectPos[3] = new Vector3f(+1,+1, 0);
    }

    void update(List<Drawable> drawables, float dt){
        if (!isActive())
            return;

//        RayCastResult result;
//        glm::mat4 transform = glm::inverse(pScene->GetCamera()->GetViewMatrix());
//        m_Ray.Transform(transform);
//        m_PhysicalWorld->RaySingleCast(m_Ray, *pScene, result);
        Drawable result = getIntersectDrawable(drawables, m_Ray);
        if (result != null) {
//        SpaLOGI("SceneRaycastSelector:Hit: %s\n", result.mHited->GetName().c_str());
            if (m_pSelectedDrawable == null) {  // m_pSelectedDrawable is nullptr
                m_pSelectedDrawable = result;
                m_WatchingTime = 0.0f;
                if (m_pTouchEventListener != null) {
                    m_pTouchEventListener.OnEnter(EVENT_INVALID_ID, m_pSelectedDrawable);
                }
            }else if (m_pSelectedDrawable != result){  // The hited drawable has changed!
                if (m_pTouchEventListener != null) {
                    m_pTouchEventListener.OnLeval(EVENT_INVALID_ID, m_pSelectedDrawable);
                }

                m_pSelectedDrawable = result;
                m_WatchingTime = 0.0f;
                if (m_pTouchEventListener != null) {
                    m_pTouchEventListener.OnEnter(EVENT_INVALID_ID, m_pSelectedDrawable);
                }
            }else if (m_pSelectedDrawable == result){ // still watching
                if (testDeltaAngle(m_Ray.m_dir)){
                    m_WatchingTime += dt;
                    onWatching(EVENT_INVALID_ID, m_pSelectedDrawable);
                }else{  //
                    m_WatchingTime = 0.0f;
                }
            }
        } else if (m_pSelectedDrawable != null){
//        SpaLOGI("SceneRaycastSelector:No Hit0: \n");
            if (m_pTouchEventListener != null) {
                m_pTouchEventListener.OnLeval(EVENT_INVALID_ID, m_pSelectedDrawable);
            }

            m_pSelectedDrawable = null;
        }else{
//        SpaLOGI("SceneRaycastSelector:No Hit1: \n");
        }
    }

    private Drawable getIntersectDrawable(List<Drawable> drawables, Ray ray){
        for(Drawable drawable : drawables){
            if(!drawable.isVisible())
                // igore the invisible drawable.
                continue;

            drawable.transform.toMatrix(m_model);

            m_RectPos[0].set(-1,-1, 0);
            m_RectPos[1].set(+1,-1, 0);
            m_RectPos[2].set(-1,+1, 0);
            m_RectPos[3].set(+1,+1, 0);

            for(Vector3f v: m_RectPos){
                Matrix4f.transformVector(m_model, v, v);
            }

            boolean intesected = Numeric.testRayIntersectWithTriangle(m_Ray.m_orig, m_Ray.m_dir,
                    m_RectPos[0], m_RectPos[1], m_RectPos[2], null);
            if(!intesected){
                intesected = Numeric.testRayIntersectWithTriangle(m_Ray.m_orig, m_Ray.m_dir,
                        m_RectPos[1], m_RectPos[2], m_RectPos[3], null);
            }

            if(intesected){
                return drawable;
            }
        }

        return null;
    }
}
