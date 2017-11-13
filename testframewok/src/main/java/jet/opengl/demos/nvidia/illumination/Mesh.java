package jet.opengl.demos.nvidia.illumination;

import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.texture.TextureGL;

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
    private Vector3f[] m_BoundingBoxCenterSubsets;
    private Vector3f[] m_BoundingBoxExtentsSubsets;

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

    }

    private void RenderMeshSubsetBounded( int iMesh,
                                          boolean bAdjacent,
//                                  ID3D11DeviceContext* pd3dDeviceContext,
                                          ReadableVector3f minSize, ReadableVector3f maxSize,
                                          int iDiffuseSlot,
                                          int iNormalSlot,
                                          int iSpecularSlot,
                                          int iAlphaSlot,
                                          int alphaState){

    }

    void initializeAlphaMaskTextures(){

    }

    void LoadAlphaMasks( /*ID3D11Device* pd3dDevice,*/ String stringToRemove, String stringToAdd ){

    }

    void LoadNormalmaps( /*ID3D11Device* pd3dDevice,*/ String stringToRemove, String stringToAdd){

    }

    void initializeDefaultNormalmaps(/*ID3D11Device* pd3dDevice,*/ String mapName){

    }

    void RenderBounded( /*ID3D11DeviceContext* pd3dDeviceContext,*/ ReadableVector3f minSize, ReadableVector3f maxSize,
                        int iDiffuseSlot /*= INVALID_SAMPLER_SLOT*/,
                        int iNormalSlot /*= INVALID_SAMPLER_SLOT*/,
                        int iSpecularSlot /*= INVALID_SAMPLER_SLOT*/,
                        int iAlphaSlot /*= INVALID_SAMPLER_SLOT*/,
                        int alphaState /*= ALL_ALPHA*/){

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

    }

    void ComputeSubmeshBoundingVolumes(){

    }


    public int getNumSubsets(int iMesh)
    {
        if(iMesh<m_numMeshes)
            return m_numSubsets[iMesh];
        return 0;
    }
}
