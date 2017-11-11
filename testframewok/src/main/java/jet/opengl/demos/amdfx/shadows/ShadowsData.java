package jet.opengl.demos.amdfx.shadows;

import org.lwjgl.util.vector.Vector2f;

final class ShadowsData {

	final Camera                             m_Viewer = new Camera();
    final Vector2f                           m_Size = new Vector2f(); // Viewer Depth Buffer Size
    final Vector2f                           m_SizeInv = new Vector2f(); // Viewer Depth Buffer Size
    
    final LightData[]                        m_Light = new LightData[ShadowFX_Desc.m_MaxLightCount];
    int                                      m_ActiveLightCount;
    float                                    pad0, pad1, pad2;
}
