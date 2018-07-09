package jet.opengl.demos.intel.cput;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public abstract class CPUTRenderStateBlock implements Disposeable{
    protected String mMaterialName;

    public static CPUTRenderStateBlock CreateRenderStateBlock( String name, String absolutePathAndFilename ){
        return null;
    }
    static CPUTRenderStateBlock GetDefaultRenderStateBlock() { return /*mpDefaultRenderStateBlock*/ null; }
    static void SetDefaultRenderStateBlock( CPUTRenderStateBlock pBlock ) {
//        SAFE_RELEASE( mpDefaultRenderStateBlock ); mpDefaultRenderStateBlock = pBlock;
    }

    public abstract void LoadRenderStateBlock(String fileName);
    public abstract void SetRenderStates(CPUTRenderParameters renderParams);
    public abstract void CreateNativeResources();
}
