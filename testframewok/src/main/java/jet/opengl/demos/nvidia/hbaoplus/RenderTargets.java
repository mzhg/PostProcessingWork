package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.Numeric;

final class RenderTargets {

	private int m_FullWidth = 0;
	private int m_FullHeight = 0;

    private final RTTexture2D m_FullResAOZTexture 						= new RTTexture2D();
    private final RTTexture2D m_FullResAOZTexture2 						= new RTTexture2D();
    private final RTTexture2D m_FullResNormalTexture					= new RTTexture2D();
    private final RTTexture2D m_FullResViewDepthTexture					= new RTTexture2D();
    private final RTTexture2DArray m_QuarterResAOTextureArray			= new RTTexture2DArray(16);
    private final RTTexture2DArray m_QuarterResViewDepthTextureArray	= new RTTexture2DArray(16);
    
    void releaseResources(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        m_FullResAOZTexture.safeRelease(/*GL*/);
        m_FullResAOZTexture2.safeRelease(/*GL*/);
        m_FullResNormalTexture.safeRelease(/*GL*/);
        m_FullResViewDepthTexture.safeRelease(/*GL*/);
        m_QuarterResAOTextureArray.safeRelease(/*GL*/);
        m_QuarterResViewDepthTextureArray.safeRelease(/*GL*/);
    }

    void release(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        releaseResources(/*GL*/);
    }

    void setFullResolution(int width, int height)
    {
        m_FullWidth = width;
        m_FullHeight = height;
    }

    int getFullWidth()
    {
        return m_FullWidth;
    }

    int getFullHeight()
    {
        return m_FullHeight;
    }

    RTTexture2D getFullResAOZTexture(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        m_FullResAOZTexture.createOnce(/*GL,*/ m_FullWidth, m_FullHeight, GLenum.GL_RG16F);
        return m_FullResAOZTexture;
    }

    RTTexture2D getFullResAOZTexture2(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        m_FullResAOZTexture2.createOnce(/*GL,*/ m_FullWidth, m_FullHeight, GLenum.GL_RG16F);
        return m_FullResAOZTexture2;
    }

    RTTexture2D getFullResViewDepthTexture(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        m_FullResViewDepthTexture.createOnce(/*GL,*/ m_FullWidth, m_FullHeight, GLenum.GL_R32F);
        return m_FullResViewDepthTexture;
    }

    int getViewDepthTextureFormat(GFSDK_SSAO_DepthStorage DepthStorage)
    {
        return (DepthStorage == GFSDK_SSAO_DepthStorage.GFSDK_SSAO_FP16_VIEW_DEPTHS) ? GLenum.GL_R16F : GLenum.GL_R32F;
    }

    RTTexture2DArray getQuarterResViewDepthTextureArray(/*const GFSDK_SSAO_GLFunctions& GL,*/ RenderOptions Options)
    {
        m_QuarterResViewDepthTextureArray.createOnce(/*GL,*/ Numeric.divideAndRoundUp(m_FullWidth,4), Numeric.divideAndRoundUp(m_FullHeight,4),
        		getViewDepthTextureFormat(Options.depthStorage));
        return m_QuarterResViewDepthTextureArray;
    }

    RTTexture2DArray getQuarterResAOTextureArray(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        m_QuarterResAOTextureArray.createOnce(/*GL,*/ Numeric.divideAndRoundUp(m_FullWidth,4), Numeric.divideAndRoundUp(m_FullHeight,4), GLenum.GL_R8);
        return m_QuarterResAOTextureArray;
    }

    RTTexture2D getFullResNormalTexture(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        m_FullResNormalTexture.createOnce(/*GL,*/ m_FullWidth, m_FullHeight, GLenum.GL_RGBA8);
        return m_FullResNormalTexture;
    }

    void createOnceAll(/*const GFSDK_SSAO_GLFunctions& GL,*/ RenderOptions Options)
    {
        getFullResViewDepthTexture(/*GL*/);
        getFullResNormalTexture(/*GL*/);
        getQuarterResViewDepthTextureArray(/*GL,*/ Options);
        getQuarterResAOTextureArray(/*GL*/);

        if (Options.blur.enable)
        {
            getFullResAOZTexture(/*GL*/);
            getFullResAOZTexture2(/*GL*/);
        }
    }

    GFSDK_SSAO_Status preCreate(/*const GFSDK_SSAO_GLFunctions& GL,*/ RenderOptions Options)
    {
//#if ENABLE_EXCEPTIONS
//        try
//        {
//            CreateOnceAll(GL, Options);
//        }
//        catch (...)
//        {
//            ReleaseResources(GL);
//
//            return GFSDK_SSAO_GL_RESOURCE_CREATION_FAILED;
//        }
//#else
        createOnceAll(/*GL,*/ Options);
//#endif

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }

    int GetCurrentAllocatedVideoMemoryBytes()
    {
        return m_FullResAOZTexture.getAllocatedSizeInBytes() +
               m_FullResAOZTexture2.getAllocatedSizeInBytes() +
               m_FullResNormalTexture.getAllocatedSizeInBytes() +
               m_FullResViewDepthTexture.getAllocatedSizeInBytes() +
               m_QuarterResAOTextureArray.getAllocatedSizeInBytes() +
               m_QuarterResViewDepthTextureArray.getAllocatedSizeInBytes();
    }
}
