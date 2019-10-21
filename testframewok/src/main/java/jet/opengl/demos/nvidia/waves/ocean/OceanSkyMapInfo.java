package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.texture.TextureGL;

final class OceanSkyMapInfo {
    String m_SkyDomeFileName;
    String m_ReflectFileName;
    TextureGL m_pSkyDomeSRV;
    TextureGL m_pReflectionSRV;
    float m_Orientation;
    final Vector3f m_GrossColor = new Vector3f();
}
