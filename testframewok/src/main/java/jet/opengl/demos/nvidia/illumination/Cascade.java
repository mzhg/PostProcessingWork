package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

class Cascade {
    float m_cascadeScale;
    final Vector3f m_cascadeTranslate = new Vector3f();

    Cascade(float cascadeScale, Vector3f cascadeTranslate) { m_cascadeScale = cascadeScale; m_cascadeTranslate.set(cascadeTranslate); };

    float getCascadeScale(int level){ return level==0?1:level*m_cascadeScale; }

    Vector3f getCascadeTranslate(int level) {
//        return m_cascadeTranslate*(float)level;
        return Vector3f.scale(m_cascadeTranslate, level, null);
    }

    void setCascadeScale(float scale){ m_cascadeScale = scale; }

    void setCascadeTranslate(ReadableVector3f translate){ m_cascadeTranslate.set(translate); }
}
