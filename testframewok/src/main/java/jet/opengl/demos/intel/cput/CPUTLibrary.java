package jet.opengl.demos.intel.cput;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2018/1/13.
 */

public final class CPUTLibrary {
    private CPUTLibrary(){}

    public static void InitlizeCPUT(){
        CPUTAssetLibrary.CreateAssetLibrary();

        // If the WARP or Reference rasterizer is being used, the performance is probably terrible.
        // we throw up a dialog right after drawing the loading screen in CPUTCreateWindowAndContext
        // warning about that perf problem

        // call the DeviceCreated callback/backbuffer/etc creation

        CPUTRenderStateBlock pBlock = new CPUTRenderStateBlockDX11();
        pBlock.CreateNativeResources();
        CPUTRenderStateBlock.SetDefaultRenderStateBlock( pBlock );

        // Create the per-frame constant buffer.
        /*D3D11_BUFFER_DESC bd = {0};
        bd.ByteWidth = sizeof(CPUTFrameConstantBuffer);
        bd.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        bd.Usage = D3D11_USAGE_DYNAMIC;
        bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;*/

        BufferGL pPerFrameConstantBuffer = new BufferGL();
        /*hr = (CPUT_DX11::GetDevice())->CreateBuffer( &bd, NULL, &pPerFrameConstantBuffer );
        ASSERT( !FAILED( hr ), _L("Error creating constant buffer.") );
        CPUTSetDebugName( pPerFrameConstantBuffer, _L("Per-Frame Constant buffer") );*/
        pPerFrameConstantBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, CPUTFrameConstantBuffer.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        String name = "$cbPerFrameValues";
        CPUTBuffer mpPerFrameConstantBuffer = new CPUTBufferDX11( name, pPerFrameConstantBuffer );
        CPUTAssetLibrary.GetAssetLibrary().AddConstantBuffer( name, mpPerFrameConstantBuffer );
    }

    public static void ReleaseCPUT(){
        CPUTAssetLibrary.DeleteAssetLibrary();
    }
}
