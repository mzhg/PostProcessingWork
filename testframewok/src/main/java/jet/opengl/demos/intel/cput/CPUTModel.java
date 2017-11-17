package jet.opengl.demos.intel.cput;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.WritableVector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/11/13.
 */
public abstract class CPUTModel extends CPUTRenderNode implements Disposeable{
    protected CPUTMesh     []mpMesh;
    protected CPUTMaterial []mpMaterial;
    protected CPUTMaterial  mpShadowCastMaterial;

    protected int           mMeshCount;
    protected boolean           mIsRenderable = true;
    protected final Vector3f mBoundingBoxCenterObjectSpace = new Vector3f();
    protected final Vector3f mBoundingBoxHalfObjectSpace = new Vector3f();
    protected final Vector3f mBoundingBoxCenterWorldSpace = new Vector3f();
    protected final Vector3f mBoundingBoxHalfWorldSpace = new Vector3f();
    protected CPUTMesh      mpBoundingBoxMesh;
    protected CPUTMaterial  mpBoundingBoxMaterial;

    public final boolean               IsRenderable() { return mIsRenderable; }
    public final void               SetRenderable(boolean isRenderable) { mIsRenderable = isRenderable; }
    public  boolean       IsModel() { return true; }
    public final void               GetBoundsObjectSpace(WritableVector3f pCenter, WritableVector3f pHalf){
        pCenter.set(mBoundingBoxCenterObjectSpace.getX(), mBoundingBoxCenterObjectSpace.getY(), mBoundingBoxCenterObjectSpace.getZ());
        pHalf  .set(mBoundingBoxHalfObjectSpace.getX(), mBoundingBoxHalfObjectSpace.getY(), mBoundingBoxHalfObjectSpace.getZ());
    }
    public final void               GetBoundsWorldSpace(WritableVector3f pCenter, WritableVector3f pHalf){
        pCenter.set(mBoundingBoxCenterWorldSpace.getX(), mBoundingBoxCenterWorldSpace.getY(), mBoundingBoxCenterWorldSpace.getZ());
        pHalf  .set(mBoundingBoxHalfWorldSpace.getX(), mBoundingBoxHalfWorldSpace.getY(), mBoundingBoxHalfWorldSpace.getZ());
    }
    public final void               UpdateBoundsWorldSpace(){
        // If an object is rigid, then it's object-space bounding box doesn't change.
        // However, if it moves, then it's world-space bounding box does change.
        // Call this function when the model moves

        Matrix4f pWorld =  GetWorldMatrix();
        /*float4 center    =  float4(mBoundingBoxCenterObjectSpace, 1.0f); // W = 1 because we want the xlation (i.e., center is a position)
        float4 half      =  float4(mBoundingBoxHalfObjectSpace,   0.0f); // W = 0 because we don't want xlation (i.e., half is a direction)

        // TODO: optimize this
        float4 positions[8] = {
            center + float4( 1.0f, 1.0f, 1.0f, 0.0f ) * half,
                    center + float4( 1.0f, 1.0f,-1.0f, 0.0f ) * half,
                    center + float4( 1.0f,-1.0f, 1.0f, 0.0f ) * half,
                    center + float4( 1.0f,-1.0f,-1.0f, 0.0f ) * half,
                    center + float4(-1.0f, 1.0f, 1.0f, 0.0f ) * half,
                    center + float4(-1.0f, 1.0f,-1.0f, 0.0f ) * half,
                    center + float4(-1.0f,-1.0f, 1.0f, 0.0f ) * half,
                    center + float4(-1.0f,-1.0f,-1.0f, 0.0f ) * half
        };

        float4 minPosition( FLT_MAX,  FLT_MAX,  FLT_MAX, 1.0f );
        float4 maxPosition(-FLT_MAX, -FLT_MAX, -FLT_MAX, 1.0f );
        for( UINT ii=0; ii<8; ii++ )
        {
            float4 position = positions[ii] * *pWorld;
            minPosition = Min( minPosition, position );
            maxPosition = Max( maxPosition, position );
        }*/
        BoundingBox boundingBox = new BoundingBox();
        boundingBox.setFromExtent(mBoundingBoxCenterObjectSpace, mBoundingBoxHalfObjectSpace);
        BoundingBox.transform(pWorld, boundingBox, boundingBox);

        /*mBoundingBoxCenterWorldSpace = (maxPosition + minPosition) * 0.5f;
        mBoundingBoxHalfWorldSpace   = (maxPosition - minPosition) * 0.5f;*/
        ReadableVector3f minExtent = boundingBox._min;
        ReadableVector3f maxExtent = boundingBox._max;
        Vector3f.linear(maxExtent, 0.5f, minExtent, +0.5f, mBoundingBoxCenterWorldSpace);
        Vector3f.linear(maxExtent, 0.5f, minExtent, -0.5f, mBoundingBoxHalfWorldSpace);
    }

    public final int                GetMeshCount()  { return mMeshCount; }
    public CPUTMesh                 GetMesh( int ii ) { return mpMesh[ii]; }
    public abstract int LoadModel(CPUTConfigBlock pBlock, /*int *pParentID,*/ CPUTModel pMasterModel/*=NULL*/)throws IOException;
    public  final void         LoadModelPayload(String file) throws IOException{
        byte[] binary = FileUtils.loadBytes(file);
        int position = 0;

        // set up for mesh creation loop
        int meshIndex = 0;
        while (position < binary.length){
            // TODO: rearrange while() to avoid if(eof).  Should perform only one branch per loop iteration, not two
            CPUTRawMeshData vertexFormatDesc = new CPUTRawMeshData();
            position = vertexFormatDesc.Read(binary, position);

            assert ( meshIndex < mMeshCount) : ("Actual mesh count doesn't match stated mesh count");

            // create the mesh.
            CPUTMesh pMesh = mpMesh[meshIndex];

            // always a triangle list (at this point)
            pMesh.SetMeshTopology(GLenum.GL_TRIANGLES);

            // get number of data blocks in the vertex element (pos,norm,uv,etc)
            // YUCK! TODO: Use fixed-size array of elements
            CPUTBufferInfo[] pVertexElementInfo = new CPUTBufferInfo[vertexFormatDesc.mFormatDescriptorCount];
            // pMesh->SetBounds(vertexFormatDesc.mBboxCenter, vertexFormatDesc.mBboxHalf);

            // running count of each type of  element
            int positionStreamCount=0;
            int normalStreamCount=0;
            int texCoordStreamCount=0;
            int tangentStreamCount=0;
            int binormalStreamCount=0;
            int colorStreamCount=0;

            int RunningOffset = 0;
            for(int ii=0; ii<vertexFormatDesc.mFormatDescriptorCount; ii++)
            {
                // lookup the CPUT data type equivalent
                pVertexElementInfo[ii].mElementType = CPUTMesh.CPUT_FILE_ELEMENT_TYPE_TO_CPUT_TYPE_CONVERT[vertexFormatDesc.mpElements[ii].mVertexElementType];
                assert (pVertexElementInfo[ii].mElementType !=CPUTMesh.CPUT_UNKNOWN ) : ".MDL file load error.  This model file has an unknown data type in it's model data.";
                // calculate the number of elements in this stream block (i.e. F32F32F32 = 3xF32)
                pVertexElementInfo[ii].mElementComponentCount = vertexFormatDesc.mpElements[ii].mElementSizeInBytes/CPUTMesh.CPUT_DATA_FORMAT_SIZE[pVertexElementInfo[ii].mElementType];
                // store the size of each element type in bytes (i.e. 3xF32, each element = F32 = 4 bytes)
                pVertexElementInfo[ii].mElementSizeInBytes = vertexFormatDesc.mpElements[ii].mElementSizeInBytes;
                // store the number of elements (i.e. 3xF32, 3 elements)
                pVertexElementInfo[ii].mElementCount = vertexFormatDesc.mVertexCount;
                // calculate the offset from the first element of the stream - assumes all blocks appear in the vertex stream as the order that appears here
                pVertexElementInfo[ii].mOffset = RunningOffset;
                RunningOffset = RunningOffset + pVertexElementInfo[ii].mElementSizeInBytes;

                // extract the name of stream
                pVertexElementInfo[ii].mpSemanticName = CPUTMesh.CPUT_VERTEX_ELEMENT_SEMANTIC_AS_STRING[ii];

                switch(vertexFormatDesc.mpElements[ii].mVertexElementSemantic)
                {
                    case CPUTMesh.CPUT_VERTEX_ELEMENT_POSITON:
                        pVertexElementInfo[ii].mpSemanticName = "POSITION";
                        pVertexElementInfo[ii].mSemanticIndex = positionStreamCount++;
                        break;
                    case CPUTMesh.CPUT_VERTEX_ELEMENT_NORMAL:
                        pVertexElementInfo[ii].mpSemanticName = "NORMAL";
                        pVertexElementInfo[ii].mSemanticIndex = normalStreamCount++;
                        break;
                    case CPUTMesh.CPUT_VERTEX_ELEMENT_TEXTURECOORD:
                        pVertexElementInfo[ii].mpSemanticName = "TEXCOORD";
                        pVertexElementInfo[ii].mSemanticIndex = texCoordStreamCount++;
                        break;
                    case CPUTMesh.CPUT_VERTEX_ELEMENT_TANGENT:
                        pVertexElementInfo[ii].mpSemanticName = "TANGENT";
                        pVertexElementInfo[ii].mSemanticIndex = tangentStreamCount++;
                        break;
                    case CPUTMesh.CPUT_VERTEX_ELEMENT_BINORMAL:
                        pVertexElementInfo[ii].mpSemanticName = "BINORMAL";
                        pVertexElementInfo[ii].mSemanticIndex = binormalStreamCount++;
                        break;
                    case CPUTMesh.CPUT_VERTEX_ELEMENT_VERTEXCOLOR:
                        pVertexElementInfo[ii].mpSemanticName = "COLOR";
                        pVertexElementInfo[ii].mSemanticIndex = colorStreamCount++;
                        break;
                    default:
                        String errorString = ("Invalid vertex semantic in: '")+file+("'\n");
                        /*TRACE(errorString.c_str());
                        ASSERT(0, errorString);*/
                        throw new IllegalArgumentException(errorString);
                }
            }

            // Index buffer
            CPUTBufferInfo indexDataInfo = new CPUTBufferInfo();
            indexDataInfo.mElementType           = (vertexFormatDesc.mIndexType == CPUTMesh.tUINT32) ? CPUTMesh.CPUT_U32 : CPUTMesh.CPUT_U16;
            indexDataInfo.mElementComponentCount = 1;
            indexDataInfo.mElementSizeInBytes    = (vertexFormatDesc.mIndexType == CPUTMesh.tUINT32) ? /*sizeof(UINT32)*/4 : /*sizeof(UINT16)*/2;
            indexDataInfo.mElementCount          = vertexFormatDesc.mIndexCount;
            indexDataInfo.mOffset                = 0;
            indexDataInfo.mSemanticIndex         = 0;
            indexDataInfo.mpSemanticName         = null;

            if( pVertexElementInfo[0].mElementCount>0 && indexDataInfo.mElementCount>0 )
            {
                pMesh.CreateNativeResources(
                        this,
                        meshIndex,
                        vertexFormatDesc.mFormatDescriptorCount,
                        pVertexElementInfo,
                        vertexFormatDesc.mpVertices,
                        indexDataInfo,
                        vertexFormatDesc.mpIndices
            );
                /*if(CPUTFAILED(result))
                {
                    return result;
                }*/
            }
//            delete [] pVertexElementInfo;
            pVertexElementInfo = null;
            ++meshIndex;
        }

        if(position != binary.length){
            throw new IllegalArgumentException();
        }
    }
    public void       SetMaterial(int ii, CPUTMaterial pMaterial){
        // TODO: ASSSERT that ii is in range

        // release old material pointer
        CommonUtil.safeRelease( mpMaterial[ii] );

        mpMaterial[ii] = pMaterial;
        if(mpMaterial[ii] !=null)
        {
//            mpMaterial[ii].AddRef();
        }
    }

    @Override
    public void GetBoundingBoxRecursive( Vector3f pCenter, Vector3f pHalf)
    {
        if( /**pHalf == float3(0.0f)*/  pHalf.isZero())
        {
            pCenter.set(mBoundingBoxCenterWorldSpace);
            pHalf  .set(mBoundingBoxHalfWorldSpace);
        }
        else
        {
            Vector3f minExtent = Vector3f.sub(pCenter, pHalf, null);
            Vector3f maxExtent = Vector3f.add(pCenter, pHalf, null);
            minExtent = Vector3f.min( Vector3f.sub(mBoundingBoxCenterWorldSpace , mBoundingBoxHalfWorldSpace, pCenter), minExtent, minExtent );
            maxExtent = Vector3f.max( Vector3f.add(mBoundingBoxCenterWorldSpace , mBoundingBoxHalfWorldSpace, pHalf), maxExtent, maxExtent );
            /**pCenter = (minExtent + maxExtent) * 0.5f;
            *pHalf   = (maxExtent - minExtent) * 0.5f;*/
            Vector3f.linear(maxExtent, 0.5f, minExtent, +0.5f, pCenter);
            Vector3f.linear(maxExtent, 0.5f, minExtent, -0.5f, pHalf);
        }
        if(mpChild !=null)   { mpChild.GetBoundingBoxRecursive(   pCenter, pHalf ); }
        if(mpSibling != null) { mpSibling.GetBoundingBoxRecursive( pCenter, pHalf ); }
    }
}
