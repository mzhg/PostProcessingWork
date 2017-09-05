package jet.opengl.demos.nvidia.face.sample;

import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/9/5.
 */

final class Material {
    SHADER			m_shader;					// Which shader is this?

    Texture2D[] m_aSrv = new Texture2D[4];					// Textures
    final int[]				m_textureSlots = new int[4];			// Slots where to bind the textures
    final float[]			m_constants=new float[24];			// Data for CB_SHADER constant buffer - includes
    //   FaceWorks constant buffer data as well
}
