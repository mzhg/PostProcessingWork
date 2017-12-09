package jet.opengl.demos.nvidia.hbaoplus;

import org.lwjgl.util.vector.Matrix4f;

final class InputNormalInfo {

	final UserTextureDesc texture = new UserTextureDesc();
	
	GFSDK_SSAO_Status setData(GFSDK_SSAO_InputNormalData normalData)
    {
        if (!UserTextureDesc.hasValidTextureTarget(normalData.fullResNormalTexture))
        {
            return GFSDK_SSAO_Status.GFSDK_SSAO_GL_INVALID_TEXTURE_TARGET;
        }

        if (!isValid(normalData.worldToViewMatrix))
        {
            return GFSDK_SSAO_Status.GFSDK_SSAO_INVALID_WORLD_TO_VIEW_MATRIX;
        }

        texture.init(normalData.fullResNormalTexture);

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
	
	static boolean isValid(Matrix4f WorldToViewMatrix)
    {
		Matrix4f m = (WorldToViewMatrix);

        // Necessary condition for the matrix to be a valid LookAt matrix
        // Note: the matrix may contain a uniform scaling, so we do not check m(3,3)
        return (m.m03 == 0.f &&
                m.m13 == 0.f &&
                m.m23 == 0.f);
    }
}
