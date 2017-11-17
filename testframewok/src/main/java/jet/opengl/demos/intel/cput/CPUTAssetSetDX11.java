package jet.opengl.demos.intel.cput;

import java.io.IOException;

/**
 * Created by mazhen'gui on 2017/11/14.
 */

final class CPUTAssetSetDX11 extends CPUTAssetSet{
    @Override
    public void LoadAssetSet(String filename) throws IOException {
        // if not found, load the set file
        CPUTConfigFile ConfigFile = new CPUTConfigFile();
        ConfigFile.LoadFile(filename);
        /*if( !CPUTSUCCESS(result) )
        {
            return result;
        }*/
        // ASSERT( CPUTSUCCESS(result), _L("Failed loading set file '") + name + _L("'.") );

        mAssetCount = ConfigFile.BlockCount() + 1; // Add one for the implied root node
        mppAssetList = new CPUTRenderNode[mAssetCount];
        mppAssetList[0] = mpRootNode;
//        mpRootNode->AddRef();

        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();

        for(int ii=0; ii<mAssetCount-1; ii++) // Note: -1 because we added one for the root node (we don't load it)
        {
            CPUTConfigBlock pBlock = ConfigFile.GetBlock(ii);
//            ASSERT(pBlock != NULL, _L("Cannot find block"));
            String nodeType = pBlock.GetValueByName("type").ValueAsString();
            CPUTRenderNode pParentNode = null;

            // TODO: use Get*() instead of Load*() ?
            String name = pBlock.GetValueByName("name").ValueAsString();

            int parentIndex;
            CPUTRenderNode pNode = null;
            if(0==nodeType.compareTo("null"))
            {
                pNode  = pNode = new CPUTNullNode();
                parentIndex = ((CPUTNullNode)pNode).LoadNullNode(pBlock);
                pParentNode = mppAssetList[parentIndex+1];
                String parentPrefix = pParentNode.GetPrefix();
                pNode.SetPrefix( parentPrefix + "." + name );
                pAssetLibrary.AddNullNode(parentPrefix + name, (CPUTNullNode)pNode);
                // Add this null's name to our prefix
                // Append this null's name to our parent's prefix
                pNode.SetParent( pParentNode );
                pParentNode.AddChild( pNode );
            }
            else if(0==nodeType.compareTo("model"))
            {
                CPUTConfigEntry pValue = pBlock.GetValueByName( "instance" );
                CPUTModelDX11 pModel = new CPUTModelDX11();
                if( pValue == CPUTConfigEntry.sNullConfigValue )
                {
                    // Not found.  So, not an instance.
                    parentIndex = pModel.LoadModel(pBlock, null);
                }
                else
                {
                    int instance = pValue.ValueAsInt();
                    parentIndex = pModel.LoadModel(pBlock, (CPUTModel)mppAssetList[instance+1]);
                }
                pParentNode = mppAssetList[parentIndex+1];
                pModel.SetParent( pParentNode );
                pParentNode.AddChild( pModel );
                String parentPrefix = pParentNode.GetPrefix();
                pModel.SetPrefix( parentPrefix );
                pAssetLibrary.AddModel(parentPrefix + name, pModel);

                pModel.UpdateBoundsWorldSpace();

/*#ifdef SUPPORT_DRAWING_BOUNDING_BOXES
                // Create a mesh for rendering the bounding box
                // TODO: There is definitely a better way to do this.  But, want to see the bounding boxes!
                pModel->CreateBoundingBoxMesh();
#endif*/
                    pNode = pModel;
            }
            else if(0==nodeType.compareTo("light"))
            {
                pNode = new CPUTLight();
                parentIndex = ((CPUTLight)pNode).LoadLight(pBlock);
                pParentNode = mppAssetList[parentIndex+1]; // +1 because we added a root node to the start
                pNode.SetParent( pParentNode );
                pParentNode.AddChild( pNode );
                String parentPrefix = pParentNode.GetPrefix();
                pNode.SetPrefix( parentPrefix );
                pAssetLibrary.AddLight(parentPrefix + name, (CPUTLight)pNode);
            }
            else if(0==nodeType.compareTo("camera"))
            {
                pNode = new CPUTCamera();
                parentIndex = ((CPUTCamera)pNode).LoadCamera(pBlock);
                pParentNode = mppAssetList[parentIndex+1]; // +1 because we added a root node to the start
                pNode.SetParent( pParentNode );
                pParentNode.AddChild( pNode );
                String parentPrefix = pParentNode.GetPrefix();
                pNode.SetPrefix( parentPrefix );
                pAssetLibrary.AddCamera(parentPrefix + name, (CPUTCamera)pNode);
                if( mpFirstCamera == null) {
//                    mpFirstCamera = (CPUTCamera)pNode;  TODO how to assigin the CPUTCamera to a CameraData
//                    mpFirstCamera->AddRef();
                }
                ++mCameraCount;
            }
            else
            {
//                ASSERT(0,_L("Unsupported node type '") + nodeType + _L("'."));
                throw new IllegalArgumentException("Unsupported node type '" + nodeType + "'.");
            }

            // Add the node to our asset list (i.e., the linear list, not the hierarchical)
            mppAssetList[ii+1] = pNode;
            // Don't AddRef.Creating it set the refcount to 1.  We add it to the list, and then we're done with it.
            // Net effect is 0 (+1 to add to list, and -1 because we're done with it)
            // pNode->AddRef();
        }
    }

    static CPUTAssetSet CreateAssetSetDX11( String name, String absolutePathAndFilename ) throws IOException{
        CPUTAssetLibraryDX11 pAssetLibrary = ((CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary());

        // Create the root node.
        CPUTNullNode pRootNode = new CPUTNullNode();
        pRootNode.SetName("_CPUTAssetSetRootNode_");

        // Create the asset set, set its root, and load it
        CPUTAssetSet   pNewAssetSet = new CPUTAssetSetDX11();
        pNewAssetSet.SetRoot( pRootNode );
        pAssetLibrary.AddNullNode( name + "_Root", pRootNode );

        /*CPUTResult result =*/ pNewAssetSet.LoadAssetSet(absolutePathAndFilename);
        /*if( CPUTSUCCESS(result) )*/
        {
            pAssetLibrary.AddAssetSet(name, pNewAssetSet);
            return pNewAssetSet;
        }
        /*ASSERT( CPUTSUCCESS(result), _L("Error loading AssetSet\n'")+absolutePathAndFilename+_L("'"));
        pNewAssetSet->Release();
        return NULL;*/
    }

    @Override
    public void dispose() {
        super.dispose();

        // Release all the elements in the asset list.  Note that we don't
        // recursively delete the hierarchy here.
        // We release the entries here because this class is where we add them.
        // TODO: Howevere, all derivations will have this, so perhaps it should go in the base.
        for( int ii=0; ii<mAssetCount; ii++ )
        {
            SAFE_RELEASE( mppAssetList[ii] );
        }
        mppAssetList = null;
    }
}
