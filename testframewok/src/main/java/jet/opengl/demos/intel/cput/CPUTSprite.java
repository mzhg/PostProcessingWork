package jet.opengl.demos.intel.cput;

import jet.opengl.postprocessing.common.Disposeable;

public abstract class CPUTSprite implements Disposeable{
    protected final class SpriteVertex
    {
        final float[] mpPos = new float[3];
        final float[] mpUV = new float[2];
    };

    protected CPUTMaterial  mpMaterial;

    public final void DrawSprite( CPUTRenderParameters renderParams ) { DrawSprite( renderParams, mpMaterial ); }
    public abstract void DrawSprite( CPUTRenderParameters renderParams, CPUTMaterial material );

    @Override
    public void dispose() {
        SAFE_RELEASE(mpMaterial);
    }

    public static CPUTSprite CreateSprite(
            float          spriteX /*= -1.0f*/,
            float          spriteY /*= -1.0f*/,
            float          spriteWidth  /*= 2.0f*/,
            float          spriteHeight /*= 2.0f*/,
            String materialName /*= cString(_L("Sprite"))*/
            ){
        return CPUTSpriteDX11.CreateSpriteDX11( spriteX,spriteY,spriteWidth,spriteHeight,materialName );
    }
}
