package jet.opengl.demos.gpupro.cloud;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/7/6.
 */

final class CCloudGrid {
    int      m_pVB;       // vertex buffer of the grid
    int      m_pIB;       // index buffer of the grid
    int      m_pDecl;     // vertex declaration
    Texture2D m_pCloudTex; // density texture

    int m_nVertices;            // the number of vertices
    int m_nTriangles;           // the number of triangles

    final Vector2f m_vStartXZ = new Vector2f();      // minimum x,z position
    final Vector2f m_vCellSizeXZ = new Vector2f();   // cell width in x and z axis.
    float m_fDefaultHeight;      // cloud height above the view position
    float m_fFallOffHeight;      // delta height.

    float m_fCloudCover;         // cloud cover
    final Vector2f m_vVelocity = new Vector2f();     // wind velocity
    final Vector2f m_vOffset = new Vector2f();       // current uv offset
    final BoundingBox m_bound = new BoundingBox();   // bounding box of the grid

    private GLFuncProvider gl;

    void Create(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        int nCellNumX = 16;
        int nCellNumZ = 16;

        // Create Buffers
        gl.glBindVertexArray(0);
        CreateVertexBuffer(nCellNumX, nCellNumZ);
        CreateIndexBuffer( nCellNumX, nCellNumZ);

        // Create vertex declaration
//        static const D3DVERTEXELEMENT9 s_elements[] = {
//                { 0,  0,  D3DDECLTYPE_FLOAT4, D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_POSITION, 0 },
//                D3DDECL_END()
//        };
//        HRESULT hr = pDev->CreateVertexDeclaration( s_elements, &m_pDecl );
//        if (FAILED(hr)) {
//            return FALSE;
//        }
        m_pDecl = gl.glGenVertexArray();
        gl.glBindVertexArray(m_pDecl);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVB);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 0, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pIB);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);

        // create texture
//        D3DXCreateTextureFromFile( pDev, L"res/Cloud.bmp", &m_pCloudTex );
        try {
            m_pCloudTex = TextureUtils.createTexture2DFromFile("gpupro/Cloud/textures/Cloud.bmp", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //--------------------------------------------------------------------------------------
    void CreateVertexBuffer( int nCellNumX, int nCellNumZ)
    {
        m_vStartXZ.set( -20000.0f, -20000.0f );
        m_vCellSizeXZ.set( (80000.0f)/nCellNumX, (80000.0f)/nCellNumZ );

        m_nVertices = (nCellNumX + 1)*(nCellNumZ + 1);

//        HRESULT hr;
//        hr = pDev->CreateVertexBuffer(sizeof(S_VERTEX)*m_nVertices, D3DUSAGE_WRITEONLY, 0, D3DPOOL_DEFAULT, &m_pVB, NULL);
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }
//        S_VERTEX* pVertices = NULL;
//        hr = m_pVB->Lock( 0, 0, (VOID**)&pVertices, 0 );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }

        FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(4 * m_nVertices);
        // The vertex buffer includes only x and z index in the grid and they are scaled in the vertex shader.
        // The height y is computed in the vertex shader using horizontal distance from view point.
        float fScaleX = 1.0f/(float)(nCellNumX+1);
        float fScaleZ = 1.0f/(float)(nCellNumZ+1);
        for (int z = 0; z < nCellNumZ+1; ++z) {
            for (int x = 0; x < nCellNumX+1; ++x) {
                float _x = (float)x;     // x index
                float _z = (float)z;     // z index
                float _u = x * fScaleX;  // texture coordinate u
                float _v = z * fScaleZ;  // texture coordinate v
//                ++pVertices;
                buffer.put(_x).put(_z).put(_u).put(_v);
            }
        }

        buffer.flip();
        m_pVB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVB);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, buffer, GLenum.GL_STATIC_DRAW);

        // Initialize x and z components of the bounding box
        // MaxY is changed at every frame according to the eye height.
        m_bound._min.set( m_vStartXZ.x, 0.0f, m_vStartXZ.y );
        Vector2f vEndXZ = new Vector2f( m_vCellSizeXZ.x * nCellNumX, m_vCellSizeXZ.y * nCellNumZ );
//        D3DXVec2Add( &vEndXZ, &vEndXZ, &m_vStartXZ );
        Vector2f.add(vEndXZ, m_vStartXZ, vEndXZ);
        m_bound._max.set( vEndXZ.x, 0.0f, vEndXZ.y  );
    }

    //--------------------------------------------------------------------------------------
    void CreateIndexBuffer(int nCellNumX, int nCellNumZ)
    {
        int nNumIndex = (nCellNumX+2) * 2 * nCellNumZ - 2;

//        HRESULT hr = pDev->CreateIndexBuffer( sizeof(USHORT)*nNumIndex, D3DUSAGE_WRITEONLY, D3DFMT_INDEX16, D3DPOOL_DEFAULT, &m_pIB, NULL);
//        if (FAILED(hr)) {
//            return FALSE;
//        }

        ShortBuffer pIndex = CacheBuffer.getCachedShortBuffer(nNumIndex);
//        hr = m_pIB->Lock( 0, 0, (VOID**)&pIndex, 0 );
//        if (FAILED(hr)) {
//            return FALSE;
//        }

        int nVertexNumX = nCellNumX+1;
        for ( int x = nCellNumX; 0 <= x; --x ) {
//            *pIndex++ = x;
//            *pIndex++ = nVertexNumX + x;
            pIndex.put((short)x);
            pIndex.put((short)(nVertexNumX + x));
        }
        for ( int z = 1; z < nCellNumZ; ++z ) {
//            *pIndex++ = z*nVertexNumX;
//            *pIndex++ = z*nVertexNumX + nCellNumX;
            pIndex.put((short)(z*nVertexNumX));
            pIndex.put((short)(z*nVertexNumX + nCellNumX));
            for ( int x = nCellNumX; 0 <= x; --x ) {
//                *pIndex++ = z*nVertexNumX + x;
//                *pIndex++ = (z+1)*nVertexNumX + x;

                pIndex.put((short)(z*nVertexNumX + x));
                pIndex.put((short)((z+1)*nVertexNumX + x));
            }
        }
        pIndex.flip();

//        m_pIB->Unlock();
        m_nTriangles = nNumIndex-2;
        m_pIB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pIB);
        gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, pIndex, GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);

//        return TRUE;
    }

    void Delete(){

    }

    //--------------------------------------------------------------------------------------
// Update cloud position.
//  The cloud is animated by scrolling uv
//--------------------------------------------------------------------------------------
    void Update(float dt, SSceneParamter parameters){
// increment uv scrolling parameters
//        D3DXVECTOR2 vec;
//        D3DXVec2Scale( &vec, &m_vVelocity, dt );
//        D3DXVec2Add( &m_vOffset, &m_vOffset, &vec );
        m_vOffset.x = m_vVelocity.x * dt;
        m_vOffset.y = m_vVelocity.y * dt;

        // Adjust the height so that clouds are always above.
        // cloud height = m_fDefaultHeight + m_fFallOffHeight * squaredistance_in_horizontal
        float fRange = 0.5f * parameters.m_far;
        float fHeight = fRange * 0.12f;
        m_fDefaultHeight = fHeight + parameters.m_Eye.y;
        m_fFallOffHeight  = - ( 0.1f / fRange ) * (  parameters.m_Eye.y / fHeight + 1.0f );

        // Update Bounding Box
        m_bound._max.y = m_fDefaultHeight;
    }
    void Draw(){
        gl.glBindVertexArray(m_pDecl);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(m_pCloudTex.getTarget(), m_pCloudTex.getTexture());

        gl.glDrawElements(GLenum.GL_TRIANGLE_STRIP, m_nTriangles+2, GLenum.GL_UNSIGNED_SHORT, 0);
        gl.glBindTexture(m_pCloudTex.getTarget(), 0);
        gl.glBindVertexArray(0);
    }

    void SetCloudCover(float fCloudCover){ m_fCloudCover = fCloudCover;}
    float GetCurrentCloudCover() { return m_fCloudCover;}
    BoundingBox GetBoundingBox() { return m_bound;}

    void GetVSParam(SVSParam param){
        param.vUVParam.set( 5.0f, 5.0f, m_vOffset.x, m_vOffset.y );
        param.vXZParam.set( m_vCellSizeXZ.x, m_vCellSizeXZ.y, m_vStartXZ.x, m_vStartXZ.y );
        param.vHeight.set( m_fFallOffHeight, m_fDefaultHeight );
    }
}
