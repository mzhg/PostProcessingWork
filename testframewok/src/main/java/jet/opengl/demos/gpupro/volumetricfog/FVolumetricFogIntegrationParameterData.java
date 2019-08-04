package jet.opengl.demos.gpupro.volumetricfog;

import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;

import jet.opengl.postprocessing.texture.Texture3D;

final class FVolumetricFogIntegrationParameterData {
    boolean bTemporalHistoryIsValid;
    Vector4f[] FrameJitterOffsetValues;
    Texture3D VBufferARenderTarget;
    Texture3D VBufferBRenderTarget;
    Texture3D LightScatteringRenderTarget;
}