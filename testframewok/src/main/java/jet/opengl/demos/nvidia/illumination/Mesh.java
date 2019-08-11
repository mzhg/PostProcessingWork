package jet.opengl.demos.nvidia.illumination;

import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshIndexBufferHeader;
import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshMesh;
import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshSubset;
import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshVertexBufferHeader;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmeshMaterial;
import com.nvidia.developer.opengl.models.sdkmesh.VertexElement9;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.NvImage;
import jet.opengl.postprocessing.util.StringUtils;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class Mesh extends SDKmesh{
    static final int
            ALL_ALPHA = 0,
            NO_ALPHA = 1,
            WITH_ALPHA = 2;

    private int m_numMeshes;
    private int[] m_numSubsets;
    private Vector3f[][] m_BoundingBoxCenterSubsets;
    private Vector3f[][] m_BoundingBoxExtentsSubsets;

    private int m_numMaterials;
    private TextureGL[] pAlphaMaskRV11s;

    private void RenderFrameSubsetBounded(int iFrame,
                                          boolean bAdjacent,
//                                   ID3D11DeviceContext* pd3dDeviceContext,
                                          ReadableVector3f minSize, ReadableVector3f maxSize,
                                          int iDiffuseSlot,
                                          int iNormalSlot,
                                          int iSpecularSlot,
                                          int iAlphaSlot,
                                          int alphaState){
        if( m_pStaticMeshData == null || m_pFrameArray == null )
            return;

        if( m_pFrameArray[iFrame].mesh != INVALID_MESH )
        {
            RenderMeshSubsetBounded( m_pFrameArray[iFrame].mesh,
                    bAdjacent,
                    /*pd3dDeviceContext,*/
                    minSize, maxSize,
                    iDiffuseSlot,
                    iNormalSlot,
                    iSpecularSlot,
                    iAlphaSlot,
                    alphaState);
        }

        // Render our children
        if( m_pFrameArray[iFrame].childFrame != INVALID_FRAME )
            RenderFrameSubsetBounded( m_pFrameArray[iFrame].childFrame, bAdjacent, /*pd3dDeviceContext,*/ minSize, maxSize, iDiffuseSlot,
                    iNormalSlot, iSpecularSlot, iAlphaSlot, alphaState );

        // Render our siblings
        if( m_pFrameArray[iFrame].siblingFrame != INVALID_FRAME )
            RenderFrameSubsetBounded( m_pFrameArray[iFrame].siblingFrame, bAdjacent, /*pd3dDeviceContext,*/ minSize, maxSize,  iDiffuseSlot,
                    iNormalSlot, iSpecularSlot, iAlphaSlot, alphaState );
    }

    private static boolean IsErrorResource(Object res) {
        if(res == null)
            return true;
        if (res instanceof Integer){
            return ((Integer)res).intValue() == 0;
        }

        return false;
    }

    private void RenderMeshSubsetBounded( int iMesh,
                                          boolean bAdjacent,
//                                  ID3D11DeviceContext* pd3dDeviceContext,
                                          ReadableVector3f minExtentsSize, ReadableVector3f maxExtentsSize,
                                          int iDiffuseSlot,
                                          int iNormalSlot,
                                          int iSpecularSlot,
                                          int iAlphaSlot,
                                          int alphaState){
        if( 0 < getOutstandingBufferResources() )
            return;

        SDKMeshMesh pMesh = m_pMeshArray[iMesh];

        /*int Strides[MAX_D3D11_VERTEX_STREAMS];
        int Offsets[MAX_D3D11_VERTEX_STREAMS];
        ID3D11Buffer* pVB[MAX_D3D11_VERTEX_STREAMS];*/

        if( pMesh.numVertexBuffers > MAX_D3D10_VERTEX_STREAMS )
            return;

        /*for( int i = 0; i < pMesh.numVertexBuffers; i++ )
        {
            pVB[i] = m_pVertexBufferArray[ pMesh.vertexBuffers[i] ].pVB11;
            Strides[i] = ( UINT )m_pVertexBufferArray[ pMesh.VertexBuffers[i] ].StrideBytes;
            Offsets[i] = 0;
        }*/

        SDKMeshIndexBufferHeader[] pIndexBufferArray;
        if( bAdjacent )
            pIndexBufferArray = m_pAdjacencyIndexBufferArray;
        else
            pIndexBufferArray = m_pIndexBufferArray;

//        ID3D11Buffer* pIB = pIndexBufferArray[ pMesh.indexBuffer ].pIB11;
        int pIB = pIndexBufferArray[ pMesh.indexBuffer ].buffer;
        int ibFormat = GLenum.GL_UNSIGNED_SHORT;
        switch( pIndexBufferArray[ pMesh.indexBuffer ].indexType )
        {
            case IT_16BIT:
                ibFormat = GLenum.GL_UNSIGNED_SHORT;
                break;
            case IT_32BIT:
                ibFormat = GLenum.GL_UNSIGNED_INT;
                break;
        };

        /*pd3dDeviceContext.IASetVertexBuffers( 0, pMesh.NumVertexBuffers, pVB, Strides, Offsets );
        pd3dDeviceContext.IASetIndexBuffer( pIB, ibFormat, 0 );*/
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, pIB);

        for( int i = 0; i < pMesh.numVertexBuffers; i++ )
        {
            SDKMeshVertexBufferHeader vertexBuffer = m_pVertexBufferArray[ pMesh.vertexBuffers[i] ];
            int pVB = vertexBuffer.buffer;
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, pVB);
            for(int j = 0; j < vertexBuffer.decl.length; j++){
                VertexElement9 element = vertexBuffer.decl[j];
                if(element.stream >= 0 && element.stream < MAX_D3D10_VERTEX_STREAMS){
                    gl.glEnableVertexAttribArray(element.stream);
                    gl.glVertexAttribPointer(element.stream, element.size, GLenum.GL_FLOAT, false, (int)vertexBuffer.strideBytes, element.offset);
                }
            }
        }

        SDKMeshSubset pSubset;
        SDKmeshMaterial pMat;
        int PrimType;

        for( int subset = 0; subset < pMesh.numSubsets; subset++ )
        {
            pSubset = m_pSubsetArray[ pMesh.pSubsets[subset] ];

            if( m_BoundingBoxExtentsSubsets[iMesh][subset].x>=minExtentsSize.getX() && m_BoundingBoxExtentsSubsets[iMesh][subset].x<maxExtentsSize.getX() &&
                    m_BoundingBoxExtentsSubsets[iMesh][subset].y>=minExtentsSize.getY() && m_BoundingBoxExtentsSubsets[iMesh][subset].y<maxExtentsSize.getY() &&
                    m_BoundingBoxExtentsSubsets[iMesh][subset].z>=minExtentsSize.getZ() && m_BoundingBoxExtentsSubsets[iMesh][subset].z<maxExtentsSize.getZ() )
                if(  alphaState==ALL_ALPHA ||
                        (alphaState==NO_ALPHA && ( pSubset.materialID>=m_numMaterials || (IsErrorResource( pAlphaMaskRV11s[pSubset.materialID])) || pAlphaMaskRV11s[pSubset.materialID]==null) ) || //if we want no alpha then either the texture should not exist, or it should be invalid or it should be null
                        (alphaState==WITH_ALPHA && pSubset.materialID<m_numMaterials && !IsErrorResource( pAlphaMaskRV11s[pSubset.materialID] ) && pAlphaMaskRV11s[pSubset.materialID]!=null )
          )
            {

                PrimType = getPrimitiveType10( pSubset.primitiveType );
                if( bAdjacent )
                {
                    switch( PrimType )
                    {
                        case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST:
                            PrimType = GLenum.GL_TRIANGLES_ADJACENCY;
                            break;
                        case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP:
                            PrimType = GLenum.GL_TRIANGLE_STRIP_ADJACENCY;
                            break;
                        case D3D10_PRIMITIVE_TOPOLOGY_LINELIST:
                            PrimType = GLenum.GL_LINES_ADJACENCY;
                            break;
                        case D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP:
                            PrimType = GLenum.GL_LINE_STRIP_ADJACENCY;
                            break;
                    }
                }

                /*pd3dDeviceContext.IASetPrimitiveTopology( PrimType );*/

                pMat = m_pMaterialArray[ pSubset.materialID ];
                if( iDiffuseSlot != INVALID_SAMPLER_SLOT && !IsErrorResource( pMat.pDiffuseTexture11 ) ) {
//                    pd3dDeviceContext.PSSetShaderResources(iDiffuseSlot, 1, & pMat.pDiffuseRV11 );
                    gl.glActiveTexture(GLenum.GL_TEXTURE0 + iDiffuseSlot);
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pDiffuseTexture11);
                } else if( iNormalSlot != INVALID_SAMPLER_SLOT && !IsErrorResource( pMat.pNormalTexture11 ) ) {
//                pd3dDeviceContext.PSSetShaderResources(iNormalSlot, 1, & pMat.pNormalRV11 );
                    gl.glActiveTexture(GLenum.GL_TEXTURE0 + iNormalSlot);
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pNormalTexture11);
                }else if( iSpecularSlot != INVALID_SAMPLER_SLOT && !IsErrorResource( pMat.pSpecularTexture11 ) ) {
//                    pd3dDeviceContext.PSSetShaderResources(iSpecularSlot, 1, & pMat.pSpecularRV11 );
                    gl.glActiveTexture(GLenum.GL_TEXTURE0 + iSpecularSlot);
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pSpecularTexture11);
                }

                if(alphaState==ALL_ALPHA || alphaState==WITH_ALPHA )
                    if( iAlphaSlot != INVALID_SAMPLER_SLOT && pSubset.materialID<m_numMaterials && !IsErrorResource( pAlphaMaskRV11s[pSubset.materialID] ) ) {
//                        pd3dDeviceContext.PSSetShaderResources(iAlphaSlot, 1, & pAlphaMaskRV11s[pSubset.materialID] );
                        gl.glActiveTexture(GLenum.GL_TEXTURE0 + iAlphaSlot);
                        gl.glBindTexture(pAlphaMaskRV11s[pSubset.materialID].getTarget(), pAlphaMaskRV11s[pSubset.materialID].getTexture());
                    }


                int IndexCount = ( int )pSubset.indexCount;
                int IndexStart = ( int )pSubset.indexStart;
                int VertexStart = ( int )pSubset.vertexStart;
                if( bAdjacent )
                {
                    IndexCount *= 2;
                    IndexStart *= 2;
                }

                /*pd3dDeviceContext.DrawIndexed( IndexCount, IndexStart, VertexStart );*/
                gl.glDrawElementsBaseVertex(PrimType, IndexCount, ibFormat, IndexStart, VertexStart);

                // TODO unbind the textures.
            }
        }

        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        for( int i = 0; i < pMesh.numVertexBuffers; i++ )
        {
            SDKMeshVertexBufferHeader vertexBuffer = m_pVertexBufferArray[ pMesh.vertexBuffers[i] ];
            for(int j = 0; j < vertexBuffer.decl.length; j++){
                VertexElement9 element = vertexBuffer.decl[j];
                if(element.stream >= 0 && element.stream < MAX_D3D10_VERTEX_STREAMS){
                    gl.glDisableVertexAttribArray(element.stream);
                }
            }
        }
    }

    void initializeAlphaMaskTextures(){
        m_numMaterials = m_pMeshHeader.numMaterials;
        pAlphaMaskRV11s = new TextureGL[m_numMaterials];
        for( int i=0;i<m_numMaterials; i++)
            pAlphaMaskRV11s[i] = null;
    }

    //try to load an alpha map that has the same name prefix as the diffuse map
    void LoadAlphaMasks( /*ID3D11Device* pd3dDevice,*/ String stringToRemove, String stringToAdd ) throws IOException{
        SDKmeshMaterial[] pMaterials = m_pMaterialArray;
        int numMaterials = m_pMeshHeader.numMaterials;

        {
            for( int m = 0; m < numMaterials; m++ )
            {
                // load textures
                if( !StringUtils.isEmpty(pMaterials[m].diffuseTexture) && (pMaterials[m].pNormalTexture11==0 /*|| IsErrorResource(pMaterials[m].pNormalRV11)*/) )
                {
                    /*string diffuseString = std::string(&pMaterials[m].DiffuseTexture[0],260);
                    unsigned int diffStringSize = unsigned int(diffuseString.size());*/
                    String diffuseString = pMaterials[m].diffuseTexture;

                    /*size_t found = -1;
                    found=diffuseString.rfind(stringToRemove);
                    if (found!=string::npos && found!=-1)
                    {
                        diffuseString.replace (found,stringToRemove.size(),stringToAdd);

                        //pMaterials[m].NormalTexture[0] = *diffuseString.c_str();

                        sprintf_s( strPath, MAX_PATH, "%s%s", m_strPath, diffuseString.c_str() );
                        if( FAILED( DXUTGetGlobalResourceCache().CreateTextureFromFile( pd3dDevice, DXUTGetD3D11DeviceContext(),
                                strPath,
                                &pMaterials[m].pNormalRV11 ) ) )
                        pMaterials[m].pNormalRV11 = ( ID3D11ShaderResourceView* )ERROR_RESOURCE_VALUE;
                    }*/

                    String newDiffuse = diffuseString.replace(stringToRemove, stringToAdd);
                    if(newDiffuse.compareTo(diffuseString) != 0){
                        String path = m_strPath + newDiffuse;
                        if(FileUtils.g_IntenalFileLoader.exists(path)) {
                            int texture = NvImage.uploadTextureFromDDSFile(path);
                            pAlphaMaskRV11s[m] = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, texture);
                        }else {
                            pAlphaMaskRV11s[m] = null;
                        }
                    }

                }
            }
        }
    }

    //try to load a normal map that has the same name prefix as the diffuse map
    void LoadNormalmaps( /*ID3D11Device* pd3dDevice,*/ String stringToRemove, String stringToAdd) throws IOException{

        SDKmeshMaterial[] pMaterials = m_pMaterialArray;
        int numMaterials = m_pMeshHeader.numMaterials;

        {
            for( int m = 0; m < numMaterials; m++ )
            {
                // load textures
                if( !StringUtils.isEmpty(pMaterials[m].diffuseTexture) && (pMaterials[m].pNormalTexture11==0 /*|| IsErrorResource(pMaterials[m].pNormalRV11)*/) )
                {
                    /*string diffuseString = std::string(&pMaterials[m].DiffuseTexture[0],260);
                    unsigned int diffStringSize = unsigned int(diffuseString.size());*/
                    String diffuseString = pMaterials[m].diffuseTexture;

                    /*size_t found = -1;
                    found=diffuseString.rfind(stringToRemove);
                    if (found!=string::npos && found!=-1)
                    {
                        diffuseString.replace (found,stringToRemove.size(),stringToAdd);

                        //pMaterials[m].NormalTexture[0] = *diffuseString.c_str();

                        sprintf_s( strPath, MAX_PATH, "%s%s", m_strPath, diffuseString.c_str() );
                        if( FAILED( DXUTGetGlobalResourceCache().CreateTextureFromFile( pd3dDevice, DXUTGetD3D11DeviceContext(),
                                strPath,
                                &pMaterials[m].pNormalRV11 ) ) )
                        pMaterials[m].pNormalRV11 = ( ID3D11ShaderResourceView* )ERROR_RESOURCE_VALUE;
                    }*/

                    String newDiffuse = diffuseString.replace(stringToRemove, stringToAdd);
                    if(newDiffuse.compareTo(diffuseString) != 0){
                        String path = m_strPath + newDiffuse;
                        if(FileUtils.g_IntenalFileLoader.exists(path))
                            pMaterials[m].pNormalTexture11 = NvImage.uploadTextureFromDDSFile(path);
                        else
                            pMaterials[m].pNormalTexture11 =0;
                    }

                }
            }
        }
    }

    void initializeDefaultNormalmaps(/*ID3D11Device* pd3dDevice,*/ String mapName) throws IOException{
        SDKmeshMaterial[] pMaterials = m_pMaterialArray;
        int numMaterials = m_pMeshHeader.numMaterials;

        for( int m = 0; m < numMaterials; m++ )
        {
            if( pMaterials[m].pNormalTexture11==0 || pMaterials[m].pNormalTexture11==/*( ID3D11ShaderResourceView* )*/ERROR_RESOURCE_VALUE)
            {
                /*sprintf_s( strPath, MAX_PATH, "%s%s", m_strPath, mapName.c_str() );
                if( FAILED( DXUTGetGlobalResourceCache().CreateTextureFromFile( pd3dDevice, DXUTGetD3D11DeviceContext(),
                        strPath,
                        &pMaterials[m].pNormalRV11 ) ) )
                pMaterials[m].pNormalRV11 = ( ID3D11ShaderResourceView* )ERROR_RESOURCE_VALUE;*/
                String path = m_strPath + mapName; // String.format("%s%s", , mapName);
                if(FileUtils.g_IntenalFileLoader.exists(path))
                    pMaterials[m].pNormalTexture11 = NvImage.uploadTextureFromDDSFile(path);
                else
                    pMaterials[m].pNormalTexture11 =0;
            }
        }
    }

    void RenderBounded( /*ID3D11DeviceContext* pd3dDeviceContext,*/ ReadableVector3f minSize, ReadableVector3f maxSize,
                        int iDiffuseSlot /*= INVALID_SAMPLER_SLOT*/,
                        int iNormalSlot /*= INVALID_SAMPLER_SLOT*/,
                        int iSpecularSlot /*= INVALID_SAMPLER_SLOT*/,
                        int iAlphaSlot /*= INVALID_SAMPLER_SLOT*/,
                        int alphaState /*= ALL_ALPHA*/){

        RenderFrameSubsetBounded( 0, false, /*pd3dDeviceContext,*/ minSize, maxSize, iDiffuseSlot, iNormalSlot, iSpecularSlot, iAlphaSlot, alphaState );
    }

    final void RenderBounded( /*ID3D11DeviceContext* pd3dDeviceContext,*/ ReadableVector3f minSize, ReadableVector3f maxSize,
                        int iDiffuseSlot){
        RenderBounded(minSize, maxSize, iDiffuseSlot, INVALID_SAMPLER_SLOT,INVALID_SAMPLER_SLOT,INVALID_SAMPLER_SLOT, ALL_ALPHA);
    }

    final void RenderBounded( /*ID3D11DeviceContext* pd3dDeviceContext,*/ ReadableVector3f minSize, ReadableVector3f maxSize,
                        int iDiffuseSlot /*= INVALID_SAMPLER_SLOT*/,
                        int iNormalSlot /*= INVALID_SAMPLER_SLOT*/){
        RenderBounded(minSize, maxSize, iDiffuseSlot, iNormalSlot,INVALID_SAMPLER_SLOT,INVALID_SAMPLER_SLOT, ALL_ALPHA);
    }

    final void RenderSubsetBounded( int iMesh,
                              int subset,
//                              ID3D11DeviceContext* pd3dDeviceContext,
                              ReadableVector3f minExtentsSize, ReadableVector3f maxExtentsSize,
                              boolean bAdjacent/*=false*/,
                              int iDiffuseSlot){
        RenderSubsetBounded(iMesh, subset, minExtentsSize, maxExtentsSize, bAdjacent, iDiffuseSlot,
                INVALID_SAMPLER_SLOT, INVALID_SAMPLER_SLOT, INVALID_SAMPLER_SLOT, ALL_ALPHA );
    }


    void RenderSubsetBounded( int iMesh,
                              int subset,
//                              ID3D11DeviceContext* pd3dDeviceContext,
                              ReadableVector3f minExtentsSize, ReadableVector3f maxExtentsSize,
                              boolean bAdjacent/*=false*/,
                              int iDiffuseSlot /*= INVALID_SAMPLER_SLOT*/,
                              int iNormalSlot /*= INVALID_SAMPLER_SLOT*/,
                              int iSpecularSlot/* = INVALID_SAMPLER_SLOT*/,
                              int iAlphaSlot /*= INVALID_SAMPLER_SLOT*/,
                              int alphaState /*= ALL_ALPHA*/){
        if( 0 < getOutstandingBufferResources() )
            return;

        if((int)subset>=m_numSubsets[iMesh]) return;

        SDKMeshMesh pMesh = m_pMeshArray[iMesh];

        /*UINT Strides[MAX_D3D11_VERTEX_STREAMS];
        UINT Offsets[MAX_D3D11_VERTEX_STREAMS];
        ID3D11Buffer* pVB[MAX_D3D11_VERTEX_STREAMS];*/

        if( pMesh.numVertexBuffers > MAX_D3D10_VERTEX_STREAMS )
            return;

        for( int i = 0; i < pMesh.numVertexBuffers; i++ )
        {
            /*pVB[i] = m_pVertexBufferArray[ pMesh->VertexBuffers[i] ].pVB11;
            Strides[i] = ( UINT )m_pVertexBufferArray[ pMesh->VertexBuffers[i] ].StrideBytes;
            Offsets[i] = 0;*/
            SDKMeshVertexBufferHeader vertexBuffer = m_pVertexBufferArray[ pMesh.vertexBuffers[i] ];
            int pVB = vertexBuffer.buffer;
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, pVB);
            for(int j = 0; j < vertexBuffer.decl.length; j++){
                VertexElement9 element = vertexBuffer.decl[j];
                if(element.stream >= 0 && element.stream < MAX_D3D10_VERTEX_STREAMS){
                    gl.glEnableVertexAttribArray(element.stream);
                    gl.glVertexAttribPointer(element.stream, element.size, GLenum.GL_FLOAT, false, (int)vertexBuffer.strideBytes, element.offset);
                }
            }
        }

        SDKMeshIndexBufferHeader[] pIndexBufferArray;
        if( bAdjacent )
            pIndexBufferArray = m_pAdjacencyIndexBufferArray;
        else
            pIndexBufferArray = m_pIndexBufferArray;

        /*ID3D11Buffer* pIB = pIndexBufferArray[ pMesh->IndexBuffer ].pIB11;*/
        int pIB = pIndexBufferArray[ pMesh.indexBuffer ].buffer;
        int ibFormat = GLenum.GL_UNSIGNED_SHORT;
        switch( pIndexBufferArray[ pMesh.indexBuffer ].indexType )
        {
            case IT_16BIT:
                ibFormat = GLenum.GL_UNSIGNED_SHORT;
                break;
            case IT_32BIT:
                ibFormat = GLenum.GL_UNSIGNED_INT;
                break;
        };

        /*pd3dDeviceContext->IASetVertexBuffers( 0, pMesh->NumVertexBuffers, pVB, Strides, Offsets );
        pd3dDeviceContext->IASetIndexBuffer( pIB, ibFormat, 0 );*/
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, pIB);

        SDKMeshSubset pSubset;
        SDKmeshMaterial pMat;
        int PrimType;

        pSubset = m_pSubsetArray[ pMesh.pSubsets[subset] ];


        if( m_BoundingBoxExtentsSubsets[iMesh][subset].x>=minExtentsSize.getX() && m_BoundingBoxExtentsSubsets[iMesh][subset].x<maxExtentsSize.getX() &&
                m_BoundingBoxExtentsSubsets[iMesh][subset].y>=minExtentsSize.getY() && m_BoundingBoxExtentsSubsets[iMesh][subset].y<maxExtentsSize.getY() &&
                m_BoundingBoxExtentsSubsets[iMesh][subset].z>=minExtentsSize.getZ() && m_BoundingBoxExtentsSubsets[iMesh][subset].z<maxExtentsSize.getZ() )
        {

            PrimType = getPrimitiveType10( pSubset.primitiveType );
            if( bAdjacent )
            {
                /*switch( PrimType )
                {
                    case D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST:
                        PrimType = D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST_ADJ;
                        break;
                    case D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP:
                        PrimType = D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP_ADJ;
                        break;
                    case D3D11_PRIMITIVE_TOPOLOGY_LINELIST:
                        PrimType = D3D11_PRIMITIVE_TOPOLOGY_LINELIST_ADJ;
                        break;
                    case D3D11_PRIMITIVE_TOPOLOGY_LINESTRIP:
                        PrimType = D3D11_PRIMITIVE_TOPOLOGY_LINESTRIP_ADJ;
                        break;
                }*/

                if( bAdjacent )
                {
                    switch( PrimType )
                    {
                        case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST:
                            PrimType = GLenum.GL_TRIANGLES_ADJACENCY;
                            break;
                        case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP:
                            PrimType = GLenum.GL_TRIANGLE_STRIP_ADJACENCY;
                            break;
                        case D3D10_PRIMITIVE_TOPOLOGY_LINELIST:
                            PrimType = GLenum.GL_LINES_ADJACENCY;
                            break;
                        case D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP:
                            PrimType = GLenum.GL_LINE_STRIP_ADJACENCY;
                            break;
                    }
                }
            }

            /*pd3dDeviceContext->IASetPrimitiveTopology( PrimType );*/

            pMat = m_pMaterialArray[ pSubset.materialID ];
            if( iDiffuseSlot != INVALID_SAMPLER_SLOT && !IsErrorResource( pMat.pDiffuseTexture11 ) ) {
//                pd3dDeviceContext -> PSSetShaderResources(iDiffuseSlot, 1, & pMat -> pDiffuseRV11 );
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + iDiffuseSlot);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pDiffuseTexture11);
            }else if( iNormalSlot != INVALID_SAMPLER_SLOT && !IsErrorResource( pMat.pNormalTexture11 ) ) {
//                pd3dDeviceContext -> PSSetShaderResources(iNormalSlot, 1, & pMat -> pNormalRV11 );
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + iNormalSlot);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pNormalTexture11);
            }else if( iSpecularSlot != INVALID_SAMPLER_SLOT && !IsErrorResource( pMat.pSpecularTexture11 ) ) {
//                pd3dDeviceContext -> PSSetShaderResources(iSpecularSlot, 1, & pMat -> pSpecularRV11 )
                ;gl.glActiveTexture(GLenum.GL_TEXTURE0 + iSpecularSlot);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pSpecularTexture11);
            }

            int IndexCount = ( int )pSubset.indexCount;
            int IndexStart = ( int )pSubset.indexStart;
            int VertexStart = ( int )pSubset.vertexStart;
            if( bAdjacent )
            {
                IndexCount *= 2;
                IndexStart *= 2;
            }

//            pd3dDeviceContext->DrawIndexed( IndexCount, IndexStart, VertexStart );
            gl.glDrawElementsBaseVertex(PrimType, IndexCount, ibFormat, IndexStart, VertexStart);
        }

        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        for( int i = 0; i < pMesh.numVertexBuffers; i++ )
        {
            SDKMeshVertexBufferHeader vertexBuffer = m_pVertexBufferArray[ pMesh.vertexBuffers[i] ];
            for(int j = 0; j < vertexBuffer.decl.length; j++){
                VertexElement9 element = vertexBuffer.decl[j];
                if(element.stream >= 0 && element.stream < MAX_D3D10_VERTEX_STREAMS){
                    gl.glDisableVertexAttribArray(element.stream);
                }
            }
        }

        if(iDiffuseSlot != INVALID_SAMPLER_SLOT){
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + iDiffuseSlot);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        }

        if(iNormalSlot != INVALID_SAMPLER_SLOT){
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + iNormalSlot);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        }

        if(iSpecularSlot != INVALID_SAMPLER_SLOT){
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + iSpecularSlot);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        }
    }

    void ComputeSubmeshBoundingVolumes(){
        SDKMeshSubset pSubset = null;
        int PrimType;
        Vector3f lower = new Vector3f();
        Vector3f upper = new Vector3f();

        m_numMeshes = m_pMeshHeader.numMeshes;
        m_numSubsets = new int[m_numMeshes];

        SDKMeshMesh currentMesh = m_pMeshArray[0];
        int tris = 0;

        m_BoundingBoxCenterSubsets = new Vector3f[m_pMeshHeader.numMeshes][];
        m_BoundingBoxExtentsSubsets = new Vector3f[m_pMeshHeader.numMeshes][];

        final float FLT_MAX = Float.MAX_VALUE;
        for (int meshi=0; meshi < m_pMeshHeader.numMeshes; ++meshi)
        {
            currentMesh = getMesh( meshi );

            int indsize;
            if (m_pIndexBufferArray[currentMesh.indexBuffer].indexType == IT_16BIT )
            {
                indsize = 2;
            }else
            {
                indsize = 4;
            }

            m_BoundingBoxCenterSubsets[meshi] = new Vector3f[currentMesh.numSubsets];
            m_BoundingBoxExtentsSubsets[meshi] = new Vector3f[currentMesh.numSubsets];

            m_numSubsets[meshi] = currentMesh.numSubsets;

            for( int subset = 0; subset < currentMesh.numSubsets; subset++ )
            {
                lower.x = FLT_MAX; lower.y = FLT_MAX; lower.z = FLT_MAX;
                upper.x = -FLT_MAX; upper.y = -FLT_MAX; upper.z = -FLT_MAX;

                pSubset = getSubset( meshi, subset );

                PrimType = getPrimitiveType10( pSubset.primitiveType );
                assert( PrimType == D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST );// only triangle lists are handled.

                int IndexCount = ( int )pSubset.indexCount;
                int IndexStart = ( int )pSubset.indexStart;

                /*UINT *ind = ( UINT * )m_ppIndices[currentMesh.indexBuffer];
                FLOAT *verts =  ( FLOAT* )m_ppVertices[currentMesh.vertexBuffers[0]];*/
                int ind_offset = m_ppIndices[currentMesh.indexBuffer];
                int verts_offset = m_ppVertices[currentMesh.vertexBuffers[0]];
                int stride = (int)m_pVertexBufferArray[currentMesh.vertexBuffers[0]].strideBytes;
                assert (stride % 4 == 0);
                stride /=4;
                for (int vertind = IndexStart; vertind < IndexStart + IndexCount; ++vertind)
                {
                    int current_ind=0;
                    if (indsize == 2) {
                        int ind_div2 = vertind / 2;
                        current_ind = /*ind[ind_div2]*/ Numeric.getInt(m_pStaticMeshData, ind_offset + (ind_div2 << 2));
                        if (vertind %2 ==0) {
                            current_ind = current_ind << 16;
                            current_ind = current_ind >> 16;
                        }else {
                            current_ind = current_ind >> 16;
                        }
                    }else {
                        current_ind = /*ind[vertind]*/Numeric.getInt(m_pStaticMeshData, ind_offset + (vertind << 2));
                    }
                    tris++;
//                    D3DXVECTOR3 *pt = (D3DXVECTOR3*)&(verts[stride * current_ind]);
                    float ptx = Numeric.getFloat(m_pStaticMeshData, verts_offset + ((stride * current_ind) << 2) + 0);
                    float pty = Numeric.getFloat(m_pStaticMeshData, verts_offset + ((stride * current_ind) << 2) + 4);
                    float ptz = Numeric.getFloat(m_pStaticMeshData, verts_offset + ((stride * current_ind) << 2) + 8);
                    if (ptx < lower.x) {
                        lower.x = ptx;
                    }
                    if (pty < lower.y) {
                        lower.y = pty;
                    }
                    if (ptz < lower.z) {
                        lower.z = ptz;
                    }
                    if (ptx > upper.x) {
                        upper.x = ptx;
                    }
                    if (pty > upper.y) {
                        upper.y = pty;
                    }
                    if (ptz > upper.z) {
                        upper.z = ptz;
                    }
                }//end for loop over vertices

                /*D3DXVECTOR3 half = upper - lower;
                half *=0.5f;
                m_BoundingBoxCenterSubsets[meshi][subset] = lower + half;
                m_BoundingBoxExtentsSubsets[meshi][subset] = half*/;
                Vector3f half = m_BoundingBoxExtentsSubsets[meshi][subset] = new Vector3f();
                Vector3f.sub(upper, lower, half);
                half.scale(0.5f);

                m_BoundingBoxCenterSubsets[meshi][subset] = Vector3f.add(lower, half, null);

            }//end for loop over subsets
        }//end for loop over meshes
    }


    public int getNumSubsets(int iMesh)
    {
        if(iMesh<m_numMeshes)
            return m_numSubsets[iMesh];
        return 0;
    }
}
