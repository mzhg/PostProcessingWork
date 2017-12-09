package jet.opengl.demos.nvidia.hbaoplus;

final class InputViewport {

	float topLeftX;
	float topLeftY;
	float width;
	float height;
	float minDepth;
	float maxDepth;
	boolean rectCoversFullInputTexture;
	
	static boolean hasValidDimensions(GFSDK_SSAO_InputViewport V)
    {
        return (V.width != 0 && V.height != 0);
    }

    static boolean hasValidDepthRange(GFSDK_SSAO_InputViewport V)
    {
        // According to the DX11 spec:
        // Viewport MinDepth and MaxDepth must both be in the range [0.0f...1.0f], and MinDepth must be less-than or equal-to MaxDepth.
        // Viewport parameters are validated in the runtime such that values outside these ranges will never be passed to the DDI.
        return (V.minDepth >= 0.f && V.minDepth <= 1.f &&
                V.maxDepth >= 0.f && V.maxDepth <= 1.f &&
                V.minDepth <= V.maxDepth);
    }

    static boolean coversFullTexture(GFSDK_SSAO_InputViewport V, UserTextureDesc Texture)
    {
        return (V.topLeftX == 0.f &&
                V.topLeftY == 0.f &&
                V.width  == Texture.width &&
                V.height == Texture.height);
    }

    GFSDK_SSAO_Status init(GFSDK_SSAO_InputViewport Viewport, UserTextureDesc Texture)
    {
        if (!Viewport.enable)
        {
            return initFromTexture(Texture);
        }

        return initFromViewport(Viewport, Texture);
    }

    GFSDK_SSAO_Status initFromTexture(GFSDK_SSAO_InputViewport Viewport, UserTextureDesc Texture)
    {
        if (Viewport.enable)
        {
        	throw new IllegalArgumentException(GFSDK_SSAO_Status.GFSDK_SSAO_GL_UNSUPPORTED_VIEWPORT.name());
        }

        return initFromTexture(Texture);
    }
    
    private GFSDK_SSAO_Status initFromTexture(UserTextureDesc Texture)
    {
        topLeftX   = 0.f;
        topLeftY   = 0.f;
        width      = Texture.width;
        height     = Texture.height;
        minDepth   = 0.f;
        maxDepth   = 1.f;
        rectCoversFullInputTexture = true;

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }

    private GFSDK_SSAO_Status initFromViewport(GFSDK_SSAO_InputViewport Viewport, UserTextureDesc Texture)
    {
        if (!hasValidDimensions(Viewport))
        {
        	throw new IllegalArgumentException(GFSDK_SSAO_Status.GFSDK_SSAO_INVALID_VIEWPORT_DIMENSIONS.name());        
        }

        if (!hasValidDepthRange(Viewport))
        {
        	throw new IllegalArgumentException(GFSDK_SSAO_Status.GFSDK_SSAO_INVALID_VIEWPORT_DEPTH_RANGE.name());
        }

        topLeftX = Viewport.topLeftX;
        topLeftY = Viewport.topLeftY;
        width    = Viewport.width;
        height   = Viewport.height;
        minDepth = Viewport.minDepth;
        maxDepth = Viewport.maxDepth;

        width  = Math.min(width,  Texture.width  - topLeftX);
        height = Math.min(height, Texture.height - topLeftY);

        rectCoversFullInputTexture = coversFullTexture(Viewport, Texture);

        return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
    }
}
