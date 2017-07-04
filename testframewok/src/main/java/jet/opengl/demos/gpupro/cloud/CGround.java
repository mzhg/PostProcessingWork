package jet.opengl.demos.gpupro.cloud;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/7/4.
 */
//--------------------------------------------------------------------------------------
// Ground object
//  A uniform grid terrain.
//--------------------------------------------------------------------------------------
final class CGround implements Disposeable{
    static final int TEX_BLEND = 0;
    static final int TEX_GROUND0 = 1;
    static final int TEX_GROUND1 = 2;
    static final int TEX_GROUND2 = 3;
    static final int TEX_GROUND3 = 4;
    static final int TEX_GROUND4 = 5;
    static final int TEX_NUM = 6;

    int       m_pIB;            // index buffer
    int      m_pVB;            // vertex buffer
//    LPDIRECT3DVERTEXDECLARATION9 m_pDecl;          // vertex declaration
    Texture2D[] m_pTex = new Texture2D[TEX_NUM];  // ground textures

    Texture2D           m_pCloudShadow;   // shadowmap
    Matrix4f m_pW2Shadow;      // world to shadow map matrix
    SSceneParamter        m_pSceneParam;    // scene parameters
    float[]                       m_pfHeight;       // heightmap
    GLSLProgram m_shader;         // ground shader

    int                         m_nVertices;      // the number of vertices
    int                         m_nTriangles;     // the number of triangles

    int                         m_nCellNumX;      // the number of cells in X direction
    int                         m_nCellNumZ;      // the number of cells in Z direction
    final Vector2f m_vCellSizeXZ = new Vector2f();    // cell width in x and z direction
    final Vector2f m_vStartXZ    = new Vector2f();       // minimum x z position
    final BoundingBox m_bound = new BoundingBox();          // bounding box


    void create(
            SSceneParamter pSceneParam,
            String ptcHeightmap,
            Texture2D pCloudShadowMap,
            Matrix4f pShadowMatrix){
        Delete();

        // Loading heightmap (BITMAP);
        LoadHeightmap( /*pDev,*/ ptcHeightmap );


        // Cerate vertex declaration
//        static const D3DVERTEXELEMENT9 s_elements[] = {
//                { 0,  0,  D3DDECLTYPE_FLOAT3, D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_POSITION, 0 },
//                { 0, 12,  D3DDECLTYPE_FLOAT3, D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_NORMAL  , 0 },
//                { 0, 24,  D3DDECLTYPE_FLOAT4, D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_TEXCOORD, 0 },
//                D3DDECL_END()
//        };
//        hr = pDev->CreateVertexDeclaration( s_elements, &m_pDecl );
//        if (FAILED(hr)) {
//            return FALSE;
//        }

        // Create textures
        final String ptchTex[] = {
            "GroundBlend.bmp",
            "Ground0.dds",
            "Ground1.dds",
            "Ground2.dds",
            "Ground3.dds",
            "Ground4.dds",
        };

        for (int i = 0; i < TEX_NUM; ++i) {
//            D3DXCreateTextureFromFile( pDev, ptchTex[i], &m_pTex[i]);
//            if ( FAILED(hr) ) {
//                return FALSE;
//            }
            // TODO
        }

        // Set pointers for shadowmap matrix and texture
        m_pW2Shadow = pShadowMatrix;
        m_pCloudShadow = pCloudShadowMap;

        // Copy pointer of scene parameter.
        m_pSceneParam = pSceneParam;

        // Create shaders
        CreateShaders( /*pDev*/ );
    }

    void Delete(){

    }

    void Draw(/*LPDIRECT3DDEVICE9 pDev*/){

    }

    float GetHeight(float x, float z) {
        if (x < m_vStartXZ.x || z < m_vStartXZ.y) {
            return 0.0f;
        }
        if (m_vStartXZ.x + m_nCellNumX * m_vCellSizeXZ.x < x ||
                m_vStartXZ.y + m_nCellNumZ * m_vCellSizeXZ.y < z) {
            return 0.0f;
        }

        // compute x,z index and ratio in the cell
        float fX = (x - m_vStartXZ.x) / m_vCellSizeXZ.x;
        float fZ = (z - m_vStartXZ.y) / m_vCellSizeXZ.y;
        int nX = (int)fX;
        int nZ = (int)fZ;
        fX -= nX;
        fZ -= nZ;
        int nVertexX = m_nCellNumX+1;
        int n = nZ*nVertexX + nX;

        if (fX + fZ <= 1.0f) {
            // upper left triangle in a cell
            return m_pfHeight[n] + (m_pfHeight[n+1] - m_pfHeight[n]) * fX + (m_pfHeight[n+nVertexX] - m_pfHeight[n]) * fZ;
        }
        else {
            // bottom right triangle in a cell
            return m_pfHeight[n+nVertexX+1] + (m_pfHeight[n+1] - m_pfHeight[n+nVertexX+1]) * (1.0f-fZ) + (m_pfHeight[n+nVertexX] - m_pfHeight[n+nVertexX+1]) * (1.0f-fX);
        }
    }

    inline VOID GetCenterPosition(D3DXVECTOR3& vCenter) const;
    inline const SBoundingBox& GetBoundingBox() const;

    void SetShaderConstants(/*LPDIRECT3DDEVICE9 pDev*/){

    }
    BOOL CreateShaders(LPDIRECT3DDEVICE9 pDev);
    BOOL CreateIndexBuffer(LPDIRECT3DDEVICE9 pDev, USHORT nWidth, USHORT nHeight);
    BOOL CreateVertexBuffer(LPDIRECT3DDEVICE9 pDev, UINT nWidth, UINT nHeight, const BYTE* pData);
    BOOL LoadHeightmap(LPDIRECT3DDEVICE9 pDev, const char* ptchHeightmap);
}
