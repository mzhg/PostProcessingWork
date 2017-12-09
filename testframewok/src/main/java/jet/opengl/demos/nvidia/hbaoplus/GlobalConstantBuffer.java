package jet.opengl.demos.nvidia.hbaoplus;

import org.lwjgl.util.vector.Matrix4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

final class GlobalConstantBuffer extends BaseConstantBuffer{

	final GlobalConstantStruct m_Data = new GlobalConstantStruct();
	
	public GlobalConstantBuffer() {
		super(GlobalConstantStruct.SIZE, BINDING_POINT_GLOBAL_UBO);
		
		m_Data.u4BuildVersion[0] = GFSDK_SSAO_Version.MAJOR;
		m_Data.u4BuildVersion[1] = GFSDK_SSAO_Version.MINOR;
		m_Data.u4BuildVersion[2] = GFSDK_SSAO_Version.BRANCH;
		m_Data.u4BuildVersion[3] = GFSDK_SSAO_Version.REVISION;
	}
	
	void setAOParameters(GFSDK_SSAO_Parameters Params, InputDepthInfo InputDepth){
		setAORadiusConstants(Params, InputDepth);
	    setBlurConstants(Params.blur, InputDepth);
	    setDepthThresholdConstants(Params.depthThreshold);

	    m_Data.fPowExponent = Numeric.clamp(Params.powerExponent, 1.f, 4.f);
	    m_Data.fNDotVBias = Numeric.clamp(Params.bias, 0.0f, 0.5f);

	    final float AOAmountScaleFactor = 1.f / (1.f - m_Data.fNDotVBias);
	    m_Data.fSmallScaleAOAmount = Numeric.clamp(Params.smallScaleAO, 0.f, 2.f) * AOAmountScaleFactor * 2.f;
	    m_Data.fLargeScaleAOAmount = Numeric.clamp(Params.largeScaleAO, 0.f, 2.f) * AOAmountScaleFactor;
	}
	
    void SetRenderMask(int RenderMask){
    	switch (RenderMask)
        {
        case GFSDK_SSAO_RenderMask.GFSDK_SSAO_RENDER_DEBUG_NORMAL_X:
            m_Data.iDebugNormalComponent = 0;
            break;
        case GFSDK_SSAO_RenderMask.GFSDK_SSAO_RENDER_DEBUG_NORMAL_Y:
            m_Data.iDebugNormalComponent = 1;
            break;
        case GFSDK_SSAO_RenderMask.GFSDK_SSAO_RENDER_DEBUG_NORMAL_Z:
            m_Data.iDebugNormalComponent = 2;
            break;
        default:
            m_Data.iDebugNormalComponent = 3;
            break;
        }
    }
    
    void setDepthData(InputDepthInfo InputDepth){
    	setDepthLinearizationConstants(InputDepth);
        setViewportConstants(InputDepth);
        setProjectionConstants(InputDepth);
    }
    
    void setResolutionConstants(Viewports Viewports){
    	m_Data.f2InvFullResolution.x = 1.f / Viewports.fullRes.width;
        m_Data.f2InvFullResolution.y = 1.f / Viewports.fullRes.height;
        m_Data.f2InvQuarterResolution.x = 1.f / Viewports.quarterRes.width;
        m_Data.f2InvQuarterResolution.y = 1.f / Viewports.quarterRes.height;
    }
    void setNormalData(GFSDK_SSAO_InputNormalData NormalData){
    	Matrix4f WorldToView = (NormalData.worldToViewMatrix);

//        for (UINT Row = 0; Row < 3; ++Row)
//        {
//            for (UINT Col = 0; Col < 3; ++Col)
//            {
//                m_Data.f44NormalMatrix.Data[Row * 4 + Col] = WorldToView(Row, Col);
//            }
//        }
    	m_Data.f44NormalMatrix.load(WorldToView);

        m_Data.fNormalDecodeScale = NormalData.decodeScale;
        m_Data.fNormalDecodeBias = NormalData.decodeBias;
    }
    
    void setDepthLinearizationConstants(InputDepthInfo InputDepth){
    	// In Shaders/Src/LinearizeDepth_Common.hlsl:
        // float ViewDepth = 1.0 / (NormalizedDepth * g_fLinearizeDepthA + g_fLinearizeDepthB);

        // Inverse projection from [0,1] to [ZNear,ZFar]
        // W = 1 / [(1/ZFar - 1/ZNear) * Z + 1/ZNear]
        final float InverseZNear = InputDepth.projectionMatrixInfo.getInverseZNear();
        final float InverseZFar  = InputDepth.projectionMatrixInfo.getInverseZFar();
        m_Data.fLinearizeDepthA = InverseZFar - InverseZNear;
        m_Data.fLinearizeDepthB = InverseZNear;

//        ASSERT((0.f * m_Data.fLinearizeDepthA + m_Data.fLinearizeDepthB) != 0.f);
//        ASSERT((1.f * m_Data.fLinearizeDepthA + m_Data.fLinearizeDepthB) != 0.f);
    }
    
    void setProjectionConstants(InputDepthInfo InputDepth){
    	final float InvFocalLenX  = InputDepth.projectionMatrixInfo.getTanHalfFovX();
        final float InvFocalLenY  = InputDepth.projectionMatrixInfo.getTanHalfFovY();
        m_Data.f2UVToViewA.x =  2.f * InvFocalLenX;
        m_Data.f2UVToViewA.y = -2.f * InvFocalLenY;
        m_Data.f2UVToViewB.x = -1.f * InvFocalLenX;
        m_Data.f2UVToViewB.y =  1.f * InvFocalLenY;
    }
    
    void setViewportConstants(InputDepthInfo InputDepth){
    	// In Shaders/Src/LinearizeDepth_Common.hlsl:
        // float NormalizedDepth = saturate(g_fInverseDepthRangeA * HardwareDepth + g_fInverseDepthRangeB);

        if (InputDepth.depthTextureType == GFSDK_SSAO_DepthTextureType.GFSDK_SSAO_HARDWARE_DEPTHS_SUB_RANGE)
        {
            // Inverse viewport depth range from [MinZ,MaxZ] to [0,1]
            // Z = (HardwareZ - MinZ) / (MaxZ - MinZ)
            final float MinZ  = InputDepth.viewport.minDepth;
            final float MaxZ  = InputDepth.viewport.maxDepth;
            m_Data.fInverseDepthRangeA = 1.f / (MaxZ - MinZ);
            m_Data.fInverseDepthRangeB = -MinZ * m_Data.fInverseDepthRangeA;
        }
        else
        {
            m_Data.fInverseDepthRangeA = 1.f;
            m_Data.fInverseDepthRangeB = 0.f;
        }

        m_Data.f2InputViewportTopLeft.x = InputDepth.viewport.topLeftX;
        m_Data.f2InputViewportTopLeft.y = InputDepth.viewport.topLeftY;
    }

    void setBlurConstants(GFSDK_SSAO_BlurParameters Params, InputDepthInfo InputDepth){
    	float BaseSharpness = Math.max(Params.sharpness, 0.f);
        BaseSharpness /= InputDepth.metersToViewSpaceUnits;

        if (Params.SharpnessProfile.enable)
        {
            m_Data.fBlurViewDepth0 = Math.max(Params.SharpnessProfile.foregroundViewDepth, 0.f);
            m_Data.fBlurViewDepth1 = Math.max(Params.SharpnessProfile.backgroundViewDepth, m_Data.fBlurViewDepth0 + Numeric.EPSILON);
            m_Data.fBlurSharpness0 = BaseSharpness * Math.max(Params.SharpnessProfile.foregroundSharpnessScale, 0.f);
            m_Data.fBlurSharpness1 = BaseSharpness;
        }
        else
        {
            m_Data.fBlurSharpness0 = BaseSharpness;
            m_Data.fBlurSharpness1 = BaseSharpness;
            m_Data.fBlurViewDepth0 = 0.f;
            m_Data.fBlurViewDepth1 = 1.f;
        }
    }
    
    void setAORadiusConstants(GFSDK_SSAO_Parameters Params, InputDepthInfo InputDepth){
    	final float RadiusInMeters = Math.max(Params.radius, Numeric.EPSILON);
        final float R = RadiusInMeters * InputDepth.metersToViewSpaceUnits;
        m_Data.fR2 = R * R;
        m_Data.fNegInvR2 = -1.f / m_Data.fR2;

        final float TanHalfFovy = InputDepth.projectionMatrixInfo.getTanHalfFovY();
        m_Data.fRadiusToScreen = R * 0.5f / TanHalfFovy * InputDepth.viewport.height;

        final float BackgroundViewDepth = Math.max(Params.backgroundAO.backgroundViewDepth, Numeric.EPSILON);
        m_Data.fBackgroundAORadiusPixels = m_Data.fRadiusToScreen / BackgroundViewDepth;

        final float ForegroundViewDepth = Math.max(Params.foregroundAO.foreGroundViewDepth, Numeric.EPSILON);
        m_Data.fForegroundAORadiusPixels = m_Data.fRadiusToScreen / ForegroundViewDepth;
    }
    
    void setDepthThresholdConstants(GFSDK_SSAO_DepthThreshold Params){
    	if (Params.enable)
        {
            m_Data.fViewDepthThresholdNegInv = -1.f / Math.max(Params.maxViewDepth, Numeric.EPSILON);
            m_Data.fViewDepthThresholdSharpness = Math.max(Params.sharpness, 0.f);
        }
        else
        {
            m_Data.fViewDepthThresholdNegInv = 0.f;
            m_Data.fViewDepthThresholdSharpness = 1.f;
        }
    }
	
	void updateBuffer(int renderMask){
		ByteBuffer buf = CacheBuffer.getCachedByteBuffer(GlobalConstantStruct.SIZE);
		m_Data.store(buf);
		buf.flip();
		
		updateCB(buf);
	}

}
