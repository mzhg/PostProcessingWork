package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.util.Vector;

import jet.opengl.postprocessing.util.LogUtil;

public class CPUTScene {
    public static final int MAX_NUM_ASSETS = 100;


    protected CPUTConfigFile   mSceneFile;
    protected final CPUTAssetSet[]     mpAssetSetList = new CPUTAssetSet[MAX_NUM_ASSETS]; // an stl::vector may be better here
    protected int      mNumAssetSets;
//    float3            mMinExtent, mMaxExtent;
    protected final Vector3f mMinExtent = new Vector3f(Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE);
    protected final Vector3f mMaxExtent = new Vector3f(-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE);
//    float3            mSceneBoundingBoxCenter, mSceneBoundingBoxHalf;

    protected final Vector3f mSceneBoundingBoxCenter = new Vector3f();
    protected final Vector3f mSceneBoundingBoxHalf = new Vector3f();

    //
    // Loads the asset sets listed in the file. Calculates the bounding box/extents for the scene.
    //
    void LoadScene(String sceneFileName) throws IOException {

        mSceneFile.LoadFile(sceneFileName);
        /*if (CPUTFAILED(result)) {
            DEBUG_PRINT(_L("Failed to load scene: %s"), sceneFileName.data());
            return result;
        }*/

        CPUTConfigBlock pAssetsBlock = mSceneFile.GetBlockByName("Assets");
        if (pAssetsBlock == null) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "Failed to load Assets");
            return;
        }

        int numAssets = pAssetsBlock.ValueCount();
        if (numAssets <= 0) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "Failed to load Assets");
            return;
        }

        CPUTAssetSet pAssetSet = null;
        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();
        for (int i = 0; i < numAssets; ++i) {
            CPUTConfigEntry pEntry = pAssetsBlock.GetValue(i);
            if (pEntry == null) {
                continue;
            }

            /*std::string resolvedAssetNameAndPath;
            char * mediaDirectory = cs2s(pAssetLibrary->GetMediaDirectoryName());
            CPUTFileSystem::ResolveAbsolutePathAndFilename(mediaDirectory + pEntry->NameAsString(), &resolvedAssetNameAndPath);
            delete mediaDirectory;*/
            String resolvedAssetNameAndPath = pAssetLibrary.GetMaterialDirectory() + pEntry.NameAsString();

            //
            // Extract the set file name off the end of the path
            //
            String  delimiters = "\\/";
            int pos = resolvedAssetNameAndPath.lastIndexOf(delimiters);
            if (pos == -1) {
                // then there are no directories in the path provided. There should always be at least /asset/
            }
            String assetFileName = resolvedAssetNameAndPath.substring(pos + 1); // +1 to skip the '/' or '\' character

            //
            // the assetname will always end in /asset/name.set
            //
            pos = resolvedAssetNameAndPath.lastIndexOf("asset");
            if (pos == /*std::string::npos*/ -1) {
                pos = resolvedAssetNameAndPath.lastIndexOf("Asset");
            }
            if (pos == /*std::string::npos*/ -1) {
                // then the set file is not in the correct folder
            }
            String assetFilePath = resolvedAssetNameAndPath.substring(0, pos);

            pAssetLibrary.SetAssetSetDirectoryName(assetFilePath);

            pAssetSet  = pAssetLibrary.GetAssetSet(resolvedAssetNameAndPath, true); // need to state that this is the fully qualified path name so CPUT will not append a .set extension
            if (pAssetSet == null)
                LogUtil.e(LogUtil.LogType.DEFAULT, "Failed to load Assets");
//            ASSERTA( pAssetSet, "Failed loading" + assetFilePath);
            mpAssetSetList[mNumAssetSets] = pAssetSet;
            mNumAssetSets++;

            assert (mNumAssetSets <= MAX_NUM_ASSETS): "Number of Assets in scene file exceeds MAX_NUM_ASSETS";
        }

        CalculateBoundingBox();
    }

    //
    // Adds the given asset set to the scene. Increments reference count of the asset set.
    //
    public void AddAssetSet(CPUTAssetSet pAssetSet)
    {
        mpAssetSetList[mNumAssetSets] = pAssetSet;
        mNumAssetSets++;
//        pAssetSet->AddRef();

        // TODO: Calculating the bounding box can be expensive.
        // If it proves to be in the future, switch to a dirty flag, and update only on get.
        CalculateBoundingBox();
    }

    //
    // Renders each asset set in the scene by calling its renderrecursive function
    //
    public void Render(CPUTRenderParameters renderParameters, int materialIndex/*=0*/){
        for (int i = 0; i < mNumAssetSets; ++i)
        {
            mpAssetSetList[i].RenderRecursive(renderParameters, materialIndex);
        }
    }

    //
    // Update frames
    //
    public void Update(float dt){
        for (int i = 0; i < mNumAssetSets; ++i) {
            mpAssetSetList[i].UpdateRecursive(dt);
        }
    }

    //
    // Gets the min/max extents for the entire scene. The extents encompass all asset sets loaded into the scene.
    //
    public void GetSceneExtents(Vector3f pMinExtent, Vector3f pMaxExtent) {
        pMinExtent.set(mMinExtent);
        pMaxExtent.set(mMaxExtent);
    }

    //
    // Gets the bounding box for the scene as a center point and half vector. The box encompasses all asset sets loaded into the scene.
    //
    public void GetBoundingBox(Vector3f pCenter, Vector3f pHalf) {
        pCenter.set(mSceneBoundingBoxCenter);
        pHalf  .set(mSceneBoundingBoxHalf);
    }

    //
    // Returns a pointer to the asset set at the given index. No range checking is performed.
    //
    public CPUTAssetSet GetAssetSet(int assetIndex)
    {
        assert(assetIndex < mNumAssetSets): "Invalid assetIndex";

        return mpAssetSetList[assetIndex];
    }

    //
    // Returns the number of asset sets in the scene
    //
    public int GetNumAssetSets() { return mNumAssetSets; }

    //
    // Calculates boudning box as both min/max extents and center/half vector for all objects in the scene.
    //
    protected void CalculateBoundingBox(){
        mMinExtent.x = mMinExtent.y = mMinExtent.z =  Float.MAX_VALUE;
        mMaxExtent.x = mMaxExtent.y = mMaxExtent.z = -Float.MAX_VALUE;

        for (int i = 0; i < mNumAssetSets; ++i) {
            Vector3f lookAtPoint = new Vector3f(0.0f, 0.0f, 0.0f);
            Vector3f half = new Vector3f(1.0f, 1.0f, 1.0f);
            mpAssetSetList[i].GetBoundingBox( lookAtPoint, half );

//            mMinExtent = Min( (lookAtPoint - half), mMinExtent );
//            mMaxExtent = Max( (lookAtPoint + half), mMaxExtent );

            mMinExtent.x = Math.min(lookAtPoint.x - half.x, mMinExtent.x);
            mMinExtent.y = Math.min(lookAtPoint.y - half.y, mMinExtent.y);
            mMinExtent.z = Math.min(lookAtPoint.z - half.z, mMinExtent.z);

            mMaxExtent.x = Math.max(lookAtPoint.x + half.x, mMaxExtent.x);
            mMaxExtent.y = Math.max(lookAtPoint.y + half.y, mMaxExtent.y);
            mMaxExtent.z = Math.max(lookAtPoint.z + half.z, mMaxExtent.z);
        }

        /*mSceneBoundingBoxCenter  = (mMaxExtent + mMinExtent) * 0.5f;
        mSceneBoundingBoxHalf    = (mMaxExtent - mMinExtent) * 0.5f;*/

        Vector3f.add(mMaxExtent, mMinExtent, mSceneBoundingBoxCenter);  mSceneBoundingBoxCenter.scale(0.5f);
        Vector3f.sub(mMaxExtent, mMinExtent, mSceneBoundingBoxHalf);    mSceneBoundingBoxHalf.scale(0.5f);
    }
}
