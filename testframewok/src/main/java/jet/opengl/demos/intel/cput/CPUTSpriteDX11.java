package jet.opengl.demos.intel.cput;

import java.nio.FloatBuffer;

import jet.opengl.demos.intel.va.VaDirectXTools;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

public class CPUTSpriteDX11 extends CPUTSprite {

    protected BufferGL          mpVertexBuffer;
    protected ID3D11InputLayout mpInputLayout;

    @Override
    public void DrawSprite(CPUTRenderParameters renderParams, CPUTMaterial material) {
        // TODO: Should we warn here?
        // If it doesn't draw, make sure you created it with createDebugSprite == true
        if( mpVertexBuffer != null)
        {
//            ID3D11DeviceContext *pContext = ((CPUTRenderParametersDX*)&renderParams)->mpContext;

            int finalMaterialIndex =  material.GetCurrentEffect();
            assert finalMaterialIndex< material.GetMaterialEffectCount(): "material index out of range.";
            CPUTMaterialEffect pMaterialEffect = (CPUTMaterialEffect)(material.GetMaterialEffects()[finalMaterialIndex]);
            pMaterialEffect.SetRenderStates(renderParams);

            /*int stride = sizeof( SpriteVertex );
            int offset = 0;
            pContext->IASetVertexBuffers( 0, 1, &mpVertexBuffer, &stride, &offset );

            // Set the input layout
            pContext->IASetInputLayout( mpInputLayout );

            // Set primitive topology
            pContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );

            pContext->Draw( 6, 0 );*/
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, mpVertexBuffer.getBuffer());
            mpInputLayout.bind();

            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 6);
            mpInputLayout.unbind();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        }
    }

    @Override
    public void dispose() {
        SAFE_RELEASE(mpMaterial);
        SAFE_RELEASE( mpVertexBuffer );
//        SAFE_RELEASE( mpInputLayout );
    }

    static CPUTSprite CreateSpriteDX11(
            float          spriteX /*= -1.0f*/,
            float          spriteY /*= -1.0f*/,
            float          spriteWidth  /*= 2.0f*/,
            float          spriteHeight /*= 2.0f*/,
            String spriteMaterialName /*= cString(_L("Sprite"))*/
    ){
        CPUTSpriteDX11 pCPUTSprite =  new CPUTSpriteDX11();

        // Create resources so we can draw a sprite using the render target as a texture
        pCPUTSprite.mpMaterial = CPUTAssetLibrary.GetAssetLibrary().GetMaterial( spriteMaterialName, false );

        // Define the input layout
        D3D11_INPUT_ELEMENT_DESC pLayout[] =
        {
                VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "POSITION", 0, GLenum.GL_RGB32F, 0,  0, 0,0),
                VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 0, GLenum.GL_RG32F,    0, 12, 0, 0 ),
        };

//        ShaderProgram pVertexShader = ((CPUTMaterialEffectDX11*) pCPUTSprite-> mpMaterial->GetMaterialEffects()[0])->GetVertexShader();  TODO
//        CPUTInputLayoutCacheDX11::GetInputLayoutCache()->GetLayout( pD3dDevice, pLayout, pVertexShader, &pCPUTSprite->mpInputLayout);
        pCPUTSprite.mpInputLayout = ID3D11InputLayout.createInputLayoutFrom(pLayout);

        // ***************************************************
        // Create Vertex Buffers
        // ***************************************************
        /*D3D11_BUFFER_DESC bd;
        bd.Usage = D3D11_USAGE_DYNAMIC;
        bd.ByteWidth = sizeof(SpriteVertex) * 6; // 2 tris, 3 verts each vertices
        bd.BindFlags = D3D11_BIND_VERTEX_BUFFER;
        bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        bd.MiscFlags = 0;

        SpriteVertex pVertices[] = {
                {  left,    top, 1.0f,   0.0f, 0.0f },
                { right,    top, 1.0f,   1.0f, 0.0f },
                {  left, bottom, 1.0f,   0.0f, 1.0f },

                { right,    top, 1.0f,   1.0f, 0.0f },
                { right, bottom, 1.0f,   1.0f, 1.0f },
                {  left, bottom, 1.0f,   0.0f, 1.0f }
        };
        D3D11_SUBRESOURCE_DATA initialData;
        initialData.pSysMem = pVertices;
        initialData.SysMemPitch = sizeof( SpriteVertex );
        initialData.SysMemSlicePitch = 0;

        result = pD3dDevice->CreateBuffer( &bd, &initialData, &pCPUTSprite->mpVertexBuffer );
        ASSERT( SUCCEEDED(result), _L("Failed creating render target debug-sprite vertex buffer") );
        CPUTSetDebugName( pCPUTSprite->mpVertexBuffer, _L("CPUTSprite vertex buffer") );*/

        final float top    = -spriteY; //-1.0f;
        final float bottom = -spriteY - spriteHeight; // 1.0f;
        final float left   =  spriteX; //-1.0f;
        final float right  =  spriteX + spriteWidth; // 1.0f;
        float[] pVertices = {
              left,    top, 1.0f,   0.0f, 0.0f ,
              right,    top, 1.0f,   1.0f, 0.0f ,
              left, bottom, 1.0f,   0.0f, 1.0f ,

              right,    top, 1.0f,   1.0f, 0.0f ,
              right, bottom, 1.0f,   1.0f, 1.0f ,
              left, bottom, 1.0f,   0.0f, 1.0f
        };

        FloatBuffer buffer = CacheBuffer.wrap(pVertices);
        pCPUTSprite.mpVertexBuffer = new BufferGL();
        pCPUTSprite.mpVertexBuffer.initlize(GLenum.GL_ARRAY_BUFFER, pVertices.length * 4, buffer, GLenum.GL_STATIC_DRAW);

        return pCPUTSprite;
    }
}
