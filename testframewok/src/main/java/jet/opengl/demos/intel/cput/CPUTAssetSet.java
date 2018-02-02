package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.demos.scene.CameraData;
import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/11/14.
 */

public abstract class CPUTAssetSet implements Disposeable{
    protected CPUTRenderNode []mppAssetList;
    protected int             mAssetCount;
    protected CPUTNullNode    mpRootNode;
    protected CameraData      mpFirstCamera;
    protected int             mCameraCount;

    public static CPUTAssetSet CreateAssetSet(String name, String absolutePathAndFilename ) throws IOException{
        return CPUTAssetSetDX11.CreateAssetSetDX11( name, absolutePathAndFilename );
    }

    public int               GetAssetCount() { return mAssetCount; }
    public int               GetCameraCount() { return mCameraCount; }

    public CPUTRenderNode         GetAssetByIndex(int index){
        /*ASSERT( NULL != ppRenderNode, _L("Invalid NULL parameter") );
        *ppRenderNode = mppAssetList[index];
        mppAssetList[index]->AddRef();
        return CPUT_SUCCESS;*/
        return mppAssetList[index];
    }
    public CPUTRenderNode GetRoot() { /*if(mpRootNode){mpRootNode->AddRef();} */return mpRootNode; }
    public void  SetRoot( CPUTNullNode pRoot) { SAFE_RELEASE(mpRootNode); mpRootNode = pRoot; }
    public CameraData GetFirstCamera() {  return mpFirstCamera; } // TODO: Consider supporting indexed access to each asset type
    public void  RenderRecursive(CPUTRenderParameters renderParams){
        if(mpRootNode != null)
        {
            mpRootNode.RenderRecursive(renderParams);
        }
    }

    public void RenderShadowRecursive(CPUTRenderParameters renderParams){
        if(mpRootNode != null)
        {
            mpRootNode.RenderShadowRecursive(renderParams);
        }
    }

    public void RenderAVSMShadowedRecursive(CPUTRenderParameters renderParams){
        if(mpRootNode != null)
        {
            mpRootNode.RenderAVSMShadowedRecursive(renderParams);
        }
    }

//    public void               UpdateRecursive( float deltaSeconds );
    public abstract void LoadAssetSet(String name) throws IOException;
    public void               GetBoundingBox(Vector3f pCenter, Vector3f pHalf){
//        *pCenter = *pHalf = float3(0.0f);
        pCenter.set(0,0,0);
        pHalf.set(0,0,0);
        if(mpRootNode != null)
        {
            mpRootNode.GetBoundingBoxRecursive(pCenter, pHalf);
        }
    }

    @Override
    public void dispose() {
        // Deleteing the asset set implies recursively releasing all the assets in the hierarchy
        if(mpRootNode!=null && mpRootNode.ReleaseRecursive() == 0)
        {
            mpRootNode = null;
        }
    }
}
