package jet.opengl.demos.gpupro.cloud;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.NvImage;

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
    int      m_pVAO;
//    LPDIRECT3DVERTEXDECLARATION9 m_pDecl;          // vertex declaration
    Texture2D[] m_pTex = new Texture2D[TEX_NUM];  // ground textures

    Texture2D           m_pCloudShadow;   // shadowmap
    Matrix4f m_pW2Shadow;      // world to shadow map matrix
    SSceneParamter        m_pSceneParam;    // scene parameters
    float[]                       m_pfHeight;       // heightmap
    RenderTechnique m_shader;         // ground shader

    int                         m_nVertices;      // the number of vertices
    int                         m_nTriangles;     // the number of triangles

    int                         m_nCellNumX;      // the number of cells in X direction
    int                         m_nCellNumZ;      // the number of cells in Z direction
    final Vector2f m_vCellSizeXZ = new Vector2f();    // cell width in x and z direction
    final Vector2f m_vStartXZ    = new Vector2f();       // minimum x z position
    final BoundingBox m_bound = new BoundingBox();          // bounding box

    final SScatteringShaderParameters m_params = new SScatteringShaderParameters();
    private GLFuncProvider gl;
    private int m_repeatSampler;

    void create(
            SSceneParamter pSceneParam,
            String ptcHeightmap,
            Texture2D pCloudShadowMap,
            Matrix4f pShadowMatrix){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        Delete();

        SamplerDesc desc = new SamplerDesc();
        desc.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
        desc.wrapR = GLenum.GL_MIRRORED_REPEAT;
        desc.wrapS = GLenum.GL_MIRRORED_REPEAT;
        desc.wrapT = GLenum.GL_MIRRORED_REPEAT;
        m_repeatSampler = SamplerUtils.createSampler(desc);

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
            try {
                if(i == 0){
                    m_pTex[i] = TextureUtils.createTexture2DFromFile("gpupro/Cloud/textures/" + ptchTex[i], true);
                }else{
                    int textureID = NvImage.uploadTextureFromDDSFile("gpupro/Cloud/textures/" + ptchTex[i]);
                    m_pTex[i] = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, textureID);
                }
                GLCheck.checkError();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    private boolean m_printOnceprogram;
    void Draw(/*LPDIRECT3DDEVICE9 pDev*/){
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glBindVertexArray(m_pVAO);

        SetShaderConstants();

        // TODO binding textures.
        for(int i = 0; i < 6; i++){
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + i);
            gl.glBindTexture(m_pTex[i].getTarget(), m_pTex[i].getTexture());
            gl.glBindSampler(0, m_repeatSampler);
        }

        gl.glDrawElements(GLenum.GL_TRIANGLE_STRIP, m_nTriangles + 2, GLenum.GL_UNSIGNED_SHORT, 0);
        gl.glBindVertexArray(0);

        if(!m_printOnceprogram){
            m_shader.setName("Render Ground");
            m_shader.printPrograminfo();
        }

        for(int i = 5; i >= 0; i--){
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + i);
            gl.glBindTexture(m_pTex[i].getTarget(), 0);
            gl.glBindSampler(0, 0);
        }

        m_printOnceprogram = true;
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

    void GetCenterPosition(Vector3f vCenter) {

    }
    BoundingBox GetBoundingBox() {
        return m_bound;
    }

    void SetShaderConstants(/*LPDIRECT3DDEVICE9 pDev*/){
        m_shader.enable();
        m_shader.setL2W(Matrix4f.IDENTITY);

        if(m_pW2Shadow != null)
            m_shader.setL2S(m_pW2Shadow);
        else
            m_shader.setL2S(Matrix4f.IDENTITY);

        if(m_pSceneParam != null){
            // transform local coordinate to projection.
            m_shader.setL2C(m_pSceneParam.m_viewProj);

            // view position
            m_shader.setEye(m_pSceneParam.m_Eye);

            // Set light
            m_shader.setLitDir(m_pSceneParam.m_vLightDir);
            m_shader.setLitCol(m_pSceneParam.m_vLightColor);
            m_shader.setAmb(m_pSceneParam.m_vAmbientLight);

            // Set scattering parameters
            m_pSceneParam.getShaderParam(m_params);
            m_shader.setScat(m_params);
        }

        // Set material colors
//        D3DXVECTOR4 vDiffuse( 1.0f, 1.0f, 1.0f, 1.0f );
//        D3DXVECTOR4 vSpecular( 1.0f, 1.0f, 1.0f, 32.0f );
//        m_shader.SetPSValue( pDev, PS_CONST_MATERIAL_DIFFUSE, &vDiffuse, sizeof(FLOAT)*4 );
//        m_shader.SetPSValue( pDev, PS_CONST_MATERIAL_SPECULAR, &vSpecular, sizeof(FLOAT)*4 );
        // TODO
        m_shader.setSpc(new Vector4f(1,1,1,32));
        m_shader.setDif(new Vector4f(1,1,1,1));
    }

    void CreateShaders(/*LPDIRECT3DDEVICE9 pDev*/){
        m_shader = new RenderTechnique("GroundVS.vert", "GroundPS.frag");
    }

    void CreateIndexBuffer(int nWidth, int nHeight){
        int nNumIndex = (2*nWidth)*(nHeight-1) + 2*(nHeight-2);

//        HRESULT hr = pDev->CreateIndexBuffer( sizeof(USHORT)*nNumIndex, D3DUSAGE_WRITEONLY, D3DFMT_INDEX16, D3DPOOL_DEFAULT, &m_pIB, NULL);
//        if (FAILED(hr)) {
//            return FALSE;
//        }
//
//        USHORT* pIndex = NULL;
//        hr = m_pIB->Lock( 0, 0, (VOID**)&pIndex, 0 );
//        if (FAILED(hr)) {
//            return FALSE;
//        }
        m_pIB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pIB);
        ShortBuffer pIndex = CacheBuffer.getCachedShortBuffer(nNumIndex);
        for (short i = 0; i+1 < nHeight; ++i) {
            for (short j = 0; j < nWidth; ++j) {
                pIndex.put((short)(i*nWidth+j));
                pIndex.put((short)((i+1)*nWidth+j));
            }
            if (i+2 < nHeight) {
//                *pIndex++ = (i+1)*nWidth+(nWidth-1);
//                *pIndex++ = (i+1)*nWidth;
                pIndex.put((short)((i+1)*nWidth+(nWidth-1)));
                pIndex.put((short)((i+1)*nWidth));
            }
        }
        pIndex.flip();
        gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, pIndex, GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        m_nTriangles = nNumIndex-2;
    }

    void CreateVertexBuffer(int nWidth, int nHeight, byte[] pData){
        m_nCellNumX = nWidth-1;
        m_nCellNumZ = nHeight-1;

        int nVertices = nWidth*nHeight;
//        HRESULT hr = pDev->CreateVertexBuffer(sizeof(S_VERTEX)*nVertices,
//        D3DUSAGE_WRITEONLY, 0, D3DPOOL_DEFAULT, &m_pVB, NULL);
//        if (FAILED(hr)) {
//            return FALSE;
//        }
//        S_VERTEX* pV;
//        hr = m_pVB->Lock( 0, 0, (VOID**)&pV, 0 );
//        if (FAILED(hr)) {
//            return FALSE;
//        }
        FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(S_VERTEX.SIZE * nVertices/4);
        S_VERTEX pV = new S_VERTEX();

        // store height of vertices
        float fBottom = 0.0f;
        float fHeightScale = 10.0f;
        m_pfHeight = new float[nHeight*nWidth];
        for (int i = 0; i < nHeight*nWidth; ++i) {
            m_pfHeight[i] = fBottom + Numeric.unsignedByte(pData[i]) *fHeightScale;
        }

        // compute vertex position, texture coordinates and normal vector.
        Vector3f vNormal = new Vector3f();
        Vector2f vStart = new Vector2f( 0.0f, 0.0f );
        m_vCellSizeXZ.set( 100.0f, 100.0f );

        for (int i = 0; i < nHeight; ++i) {
            for (int j = 0; j < nWidth; ++j) {
                // grid position
                pV.fPos[0] = vStart.x + m_vCellSizeXZ.x * j;
                pV.fPos[1] = m_pfHeight[i*nWidth+j];
                pV.fPos[2] = vStart.y + m_vCellSizeXZ.y* i;
                // texture coordinate
                pV.fTex[0] = (float)j/(float)(nWidth-1);                    // texcoord u for ground textures
                pV.fTex[1] = (float)i/(float)(nHeight-1);                    // texcoord v for ground textures
                pV.fTex[2] = (float)j/(float)(nWidth-1);  // texcoord u for a blend texture
                pV.fTex[3] = (float)i/(float)(nHeight-1); // texcoord v for a blend texture

                // compute normal vector
                // x
                float subX0 = 0.0f;
                float subX1 = 0.0f;
                if (0 < j) {
                    subX0 = (m_pfHeight[i*nWidth+j] - m_pfHeight[i*nWidth+j-1]);
                }
                if (j+1 < nWidth) {
                    subX1 = (m_pfHeight[i*nWidth+j+1] - m_pfHeight[i*nWidth+j]);
                }
                float lenX0 = (float)Math.sqrt( m_vCellSizeXZ.x*m_vCellSizeXZ.x + subX0*subX0 );
                float lenX1 = (float)Math.sqrt( m_vCellSizeXZ.x*m_vCellSizeXZ.x + subX1*subX1 );
                float dxdy = (lenX1 * subX0 + lenX0 * subX1) / (m_vCellSizeXZ.x*(lenX0 + lenX1));
                // z
                float subZ0 = 0.0f;
                float subZ1 = 0.0f;
                if (0 < i) {
                    subZ0 = (m_pfHeight[i*nWidth+j] - m_pfHeight[(i-1)*nWidth+j]);
                }
                if (i+1 < nHeight) {
                    subZ1 = (m_pfHeight[(i+1)*nWidth+j] - m_pfHeight[i*nWidth+j]);
                }
                float lenZ0 = (float)Math.sqrt( m_vCellSizeXZ.y*m_vCellSizeXZ.y + subZ0*subZ0 );
                float lenZ1 = (float)Math.sqrt( m_vCellSizeXZ.y*m_vCellSizeXZ.y + subZ1*subZ1 );
                float dzdy = (lenZ1 * subZ0 + lenZ0 * subZ1) / (m_vCellSizeXZ.y*(lenZ0 + lenZ1));

                vNormal.set( -dxdy, 1, -dzdy );
                vNormal.normalise();
                pV.fNormal[0] = vNormal.x;
                pV.fNormal[1] = vNormal.y;
                pV.fNormal[2] = vNormal.z;

//                ++pV;
                pV.store(buffer);
            }
        }

        buffer.flip();
        m_nVertices = nVertices;

        m_pVB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVB);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, buffer, GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        // Initialize the bounding box
        m_bound._min.set( m_vStartXZ.x, fBottom, m_vStartXZ.y );
        m_bound._max.set( m_vStartXZ.x + m_vCellSizeXZ.x * m_nCellNumX, fBottom + 255.0f*fHeightScale, m_vStartXZ.y + m_vCellSizeXZ.y * m_nCellNumZ );
    }

    private void createVAO(){
        m_pVAO = gl.glGenVertexArray();
        gl.glBindVertexArray(m_pVAO);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVB);
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, S_VERTEX.SIZE, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, S_VERTEX.SIZE, 12);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(2, 4, GLenum.GL_FLOAT, false, S_VERTEX.SIZE, 24);
        gl.glEnableVertexAttribArray(2);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pIB);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
    }

    void LoadHeightmap(String ptchHeightmap){
        short bfType;

        final SFileHeader fileHeader = new SFileHeader();
        final SInfoHeader infoHeader = new SInfoHeader();

        try {
            byte[] data = FileUtils.loadBytes(ptchHeightmap);
            int position = 0;
            bfType = Numeric.getShort(data, position); position += 2;
            position = fileHeader.load(data, position);
            position = infoHeader.load(data, position);

            if (infoHeader.biBitCount != 8) {
                // unsupported.
                throw new IllegalArgumentException();
            }

            // skip palette assuming grey scale bitmap.
//            size_t nHeaderSize = sizeof(SFileHeader) + sizeof(USHORT) + sizeof(SInfoHeader);
//            size_t szSkip = fileHeader.bfOffBits - nHeaderSize;
//            if (0 < szSkip) {
//                fseek( pFile, (long)szSkip, SEEK_CUR );
//            }
            int nHeaderSize =SFileHeader.SIZE + 2 + SInfoHeader.SIZE;
            int szSkip = fileHeader.bfOffBits - nHeaderSize;
            if (0 < szSkip) {
                position += szSkip;
            }

            int nImageSize = infoHeader.biBitCount/8*infoHeader.biWidth*infoHeader.biHeight;
            byte[] pData = new byte[nImageSize];
            // load pixel data
            int nHeight = infoHeader.biHeight;
            int nWidth  = infoHeader.biWidth;
            int nPitch = 4*((nWidth+3)/4);
            for ( int i = nHeight-1; 0 <= i; --i ) {
//                szRead = fread( &pData[ i*nWidth ], 1, nWidth, pFile );
                System.arraycopy(data, position, pData, i * nWidth, nWidth);
                position += nWidth;
                if ( 0 < nPitch - nWidth ) {
//                    fseek( pFile, nPitch - nWidth, SEEK_CUR );
                    position += nPitch - nWidth;
                }
            }
//            fclose(pFile);

            // Create vertex buffer.
            gl.glBindVertexArray(0);
            CreateVertexBuffer(infoHeader.biWidth, infoHeader.biHeight, pData);
            CreateIndexBuffer(infoHeader.biWidth, infoHeader.biHeight);

            createVAO();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {

    }

    private static final class S_VERTEX {
        static final int SIZE = 10 * 4;

        final float[] fPos = new float[3];
        final float[] fNormal = new float[3];
        final float[] fTex = new float[4];

        void store(FloatBuffer buffer){
            buffer.put(fPos).put(fNormal).put(fTex);
        }
    }

    private static final class SFileHeader
    {
        static final int SIZE = 12;
        int      bfSize;
        short    bfReserved1;
        short    bfReserved2;
        int      bfOffBits;

        int load(byte[] data, int position){
            bfSize = Numeric.getInt(data, position); position += 4;
            bfReserved1 = Numeric.getShort(data, position); position += 2;
            bfReserved2 = Numeric.getShort(data, position); position += 2;
            bfOffBits = Numeric.getInt(data, position); position += 4;
            return position;
        }
    };

    private static final class SInfoHeader
    {
        static final int SIZE = 9*4+2*2;

        int      biSize;
        int       biWidth;
        int       biHeight;
        short    biPlanes;
        short    biBitCount;
        int      biCompression;
        int      biSizeImage;
        int       biXPelsPerMeter;
        int       biYPelsPerMeter;
        int      biClrUsed;
        int      biClrImportant;

        int load(byte[] data, int position){
            biSize = Numeric.getInt(data, position); position += 4;
            biWidth = Numeric.getInt(data, position); position += 4;
            biHeight = Numeric.getInt(data, position); position += 4;

            biPlanes = Numeric.getShort(data, position); position += 2;
            biBitCount = Numeric.getShort(data, position); position += 2;

            biCompression = Numeric.getInt(data, position); position += 4;
            biSizeImage = Numeric.getInt(data, position); position += 4;
            biXPelsPerMeter = Numeric.getInt(data, position); position += 4;
            biYPelsPerMeter = Numeric.getInt(data, position); position += 4;
            biClrUsed = Numeric.getInt(data, position); position += 4;
            biClrImportant = Numeric.getInt(data, position); position += 4;
            return position;
        }
    };
}
