package jet.opengl.demos.intel.oit;

import jet.opengl.demos.intel.cput.CPUTAssetLibrary;
import jet.opengl.demos.intel.cput.CPUTMaterial;
import jet.opengl.demos.intel.cput.CPUTRenderParameters;
import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.demos.intel.va.VaDirectXTools;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

final class CPUTGeometrySprite implements Disposeable{
    private BufferGL          mpVertexBuffer;
    private CPUTMaterial      mpMaterial;
    private ID3D11InputLayout mpInputLayout;

    void CreateSprite(
            float          spriteX /*= -1.0f*/,
            float          spriteY /*= -1.0f*/,
            float          spriteWidth  /*= 2.0f*/,
            float          spriteHeight /*= 2.0f*/,
            String         spriteMaterialName /*= cString(_L("Sprite")*/
            ){
        // Create resources so we can draw a sprite using the render target as a texture
        mpMaterial = CPUTAssetLibrary.GetAssetLibrary().GetMaterial( spriteMaterialName, false );

        // Define the input layout
        D3D11_INPUT_ELEMENT_DESC pLayout[] =
        {
                VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "POSITION", 0, GLenum.GL_RGB32F, 0,  0, 0,0),
                VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 0, GLenum.GL_RG32F,    0, 12, 0, 0 ),
        };
//        CPUTVertexShaderDX11 *pVertexShader = ((CPUTMaterialEffectDX11*)mpMaterial->GetMaterialEffects()[0])->GetVertexShader();
//        ID3D11Device *pD3dDevice = CPUT_DX11::GetDevice();
//        if(pVertexShader)
        {
//            CPUTInputLayoutCacheDX11::GetInputLayoutCache()->GetLayout( pD3dDevice, pLayout, pVertexShader, &mpInputLayout);
            mpInputLayout = ID3D11InputLayout.createInputLayoutFrom(pLayout);

            // ***************************************************
            // Create Vertex Buffers
            // ***************************************************
//            D3D11_BUFFER_DESC bd;
//            bd.Usage = D3D11_USAGE_DYNAMIC;
//            bd.ByteWidth = sizeof(SpriteVertex) * 1; // 2 tris, 3 verts each vertices
//            bd.BindFlags = D3D11_BIND_VERTEX_BUFFER;
//            bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
//            bd.MiscFlags = 0;
//
//            SpriteVertex pVertices[] = {
//                    {  spriteX,    -spriteY, 1.0f,   spriteWidth, spriteHeight },
//            };
//            D3D11_SUBRESOURCE_DATA initialData;
//            initialData.pSysMem = pVertices;
//            initialData.SysMemPitch = sizeof( SpriteVertex );
//            initialData.SysMemSlicePitch = 0;
//
//            result = pD3dDevice->CreateBuffer( &bd, &initialData, &mpVertexBuffer );
//            ASSERT( SUCCEEDED(result), _L("Failed creating render target debug-sprite vertex buffer") );
//            CPUTSetDebugName( mpVertexBuffer, _L("CPUTGeometrySprite vertex buffer") );
            float[] pVertices = {spriteX,    -spriteY, 1.0f,   spriteWidth, spriteHeight};
            mpVertexBuffer = new BufferGL();
            mpVertexBuffer.initlize(GLenum.GL_ARRAY_BUFFER, pVertices.length * 4, CacheBuffer.wrap(pVertices), GLenum.GL_STATIC_DRAW);
            mpVertexBuffer.setName("CPUTGeometrySprite vertex buffer");
        }
    }
    void DrawSprite( CPUTRenderParameters renderParams ) { DrawSprite( renderParams, mpMaterial ); }
    void DrawSprite( CPUTRenderParameters renderParams, CPUTMaterial material ){
        // TODO: Should we warn here?
        // If it doesn't draw, make sure you created it with createDebugSprite == true
        if( mpVertexBuffer != null)
        {
//            ID3D11DeviceContext *pContext = ((CPUTRenderParametersDX*)&renderParams)->mpContext;

            material.GetMaterialEffects()[0]->SetRenderStates(renderParams);

            /*UINT stride = sizeof( SpriteVertex );
            UINT offset = 0;
            pContext->IASetVertexBuffers( 0, 1, &mpVertexBuffer, &stride, &offset );

            // Set the input layout
            pContext->IASetInputLayout( mpInputLayout );

            // Set primitive topology
            pContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_POINTLIST );

            pContext->Draw( 1, 0 );*/

            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, mpVertexBuffer.getBuffer());
            mpInputLayout.bind();

            gl.glDrawArrays(GLenum.GL_POINTS, 0, 1);
            mpInputLayout.unbind();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        }
    }

    @Override
    public void dispose() {
        SAFE_RELEASE(mpVertexBuffer);
        SAFE_RELEASE(mpMaterial); // TODO
    }
}
