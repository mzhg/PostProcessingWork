package jet.opengl.demos.nvidia.hbaoplus;

class InputDepthInfo {

	GFSDK_SSAO_DepthTextureType depthTextureType = GFSDK_SSAO_DepthTextureType.GFSDK_SSAO_HARDWARE_DEPTHS;
	float metersToViewSpaceUnits = 0.f;
	final ProjectionMatrixInfo projectionMatrixInfo = new ProjectionMatrixInfo();
	final InputViewport viewport = new InputViewport();
	final UserTextureDesc texture = new UserTextureDesc();
	
	GFSDK_SSAO_Status setData(GFSDK_SSAO_InputDepthData depthData)
    {
        GFSDK_SSAO_Status Status;

        Status = projectionMatrixInfo.init(depthData.projectionMatrix);  // TODO Error
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            return Status;
        }

        Status = texture.init(depthData.fullResDepthTexture);
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            return Status;
        }

        Status = viewport.init(depthData.viewport, texture);
        if (Status != GFSDK_SSAO_Status.GFSDK_SSAO_OK)
        {
            return Status;
        }

        depthTextureType = depthData.depthTextureType;
        metersToViewSpaceUnits = Math.max(depthData.metersToViewSpaceUnits, 0.f);

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
}
