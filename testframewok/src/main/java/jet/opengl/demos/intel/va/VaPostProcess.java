package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/11/22.
 */

public abstract class VaPostProcess extends VaRenderingModuleImpl {
    protected VaTexture m_comparisonResultsGPU;
    protected VaTexture m_comparisonResultsCPU;

    protected VaPostProcess(){

    }

    public abstract void SaveTextureToDDSFile( VaDrawContext drawContext, String path, VaTexture texture );
    public abstract void SaveTextureToPNGFile( VaDrawContext drawContext, String path, VaTexture texture );
    public abstract void CopyResource( VaDrawContext drawContext, VaTexture destinationTexture, VaTexture sourceTexture );

    public abstract void DrawTexturedQuad( VaDrawContext drawContext, VaTexture texture );
    public abstract void StretchRect(VaDrawContext drawContext, VaTexture texture, ReadableVector4f srcRect, ReadableVector4f dstRect, boolean linearFilter );

    public abstract Vector4f CompareImages(VaDrawContext drawContext, VaTexture textureA, VaTexture textureB );
}
