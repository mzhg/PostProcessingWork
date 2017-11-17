package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.WritableVector3f;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public class CPUTRenderNode implements Disposeable{
    protected String             mName;
    protected CPUTRenderNode     mpParent;
    protected CPUTRenderNode     mpChild;
    protected CPUTRenderNode     mpSibling;
    protected boolean            mWorldMatrixDirty = true;
    protected final Matrix4f     mWorldMatrix = new Matrix4f(); // transform of this object combined with it's parent(s) transform(s)
    protected final Matrix4f     mParentMatrix = new Matrix4f();   // transform of this object relative to it's parent
    protected String             mPrefix;

    public final void Scale(float xx, float yy, float zz)
    {
        /*float4x4 scale(
            xx, 0.0f, 0.0f, 0.0f,
            0.0f,   yy, 0.0f, 0.0f,
            0.0f, 0.0f,   zz, 0.0f,
            0.0f, 0.0f, 0.0f,    1
        );
        mParentMatrix = mParentMatrix * scale;*/
        mParentMatrix.m00 = xx * mParentMatrix.m00;
        mParentMatrix.m10 = xx * mParentMatrix.m10;
        mParentMatrix.m20 = xx * mParentMatrix.m20;
        mParentMatrix.m30 = xx * mParentMatrix.m30;

        mParentMatrix.m01 = yy * mParentMatrix.m01;
        mParentMatrix.m11 = yy * mParentMatrix.m11;
        mParentMatrix.m21 = yy * mParentMatrix.m21;
        mParentMatrix.m31 = yy * mParentMatrix.m31;

        mParentMatrix.m02 = zz * mParentMatrix.m02;
        mParentMatrix.m12 = zz * mParentMatrix.m12;
        mParentMatrix.m22 = zz * mParentMatrix.m22;
        mParentMatrix.m32 = zz * mParentMatrix.m32;

        MarkDirty();
    }

    public final void Scale(float xx)
    {
        Scale(xx,xx,xx);
    }

    public final void SetPosition(float x, float y, float z)
    {
        mParentMatrix.m30 = x;
        mParentMatrix.m31 = y;
        mParentMatrix.m32 = z;
        MarkDirty();
    }

    public final void SetPosition(ReadableVector3f position)
    {
        SetPosition(position.getX(), position.getY(), position.getZ());
    }

    public final void GetPosition(WritableVector3f position)
    {
        position.setX(mParentMatrix.m30);
        position.setY(mParentMatrix.m31);
        position.setZ(mParentMatrix.m32);
    }

    public final void GetLook( WritableVector3f look )
    {
        look.setX(-mParentMatrix.m20);
        look.setY(-mParentMatrix.m21);
        look.setZ(-mParentMatrix.m22);
    }

    public final void GetUp( WritableVector3f up )
    {
        up.setX(mParentMatrix.m10);
        up.setY(mParentMatrix.m11);
        up.setZ(mParentMatrix.m12);
    }

    public final void GetRight(WritableVector3f right){
        right.setX(mParentMatrix.m00);
        right.setY(mParentMatrix.m01);
        right.setZ(mParentMatrix.m02);
    }

    public int      ReleaseRecursive(){
        // Recursively release our children and siblings
        if( mpChild !=null)
        {
/*#ifdef OUTPUT_DEBUG_INFO
            OutputDebugString( _L("Child") );
#endif*/
            {
                mpChild.ReleaseRecursive();
                mpChild = null;
            }
        }
        if( mpSibling !=null)
        {
/*#ifdef OUTPUT_DEBUG_INFO
            OutputDebugString( _L("Sibling:") );
#endif*/
            /*int refCount = mpSibling->ReleaseRecursive();
            if( !refCount )*/
            {
                mpSibling.ReleaseRecursive();
                mpSibling = null;
            }
        }
        return /*CPUTRefCount::Release()*/0;
    }
    public final void             SetName(String name) { mName = name;};
    public final void             SetParent(CPUTRenderNode pParent) {mpParent = pParent;}
    public final CPUTRenderNode  GetParent()       { return mpParent; }
    public final CPUTRenderNode  GetChild()        { return mpChild; }
    public final CPUTRenderNode  GetSibling()      { return mpSibling; }
    public final String         GetName()         { return mName;};
    public final String         GetPrefix()       { return mPrefix; }
    public final void             SetPrefix( String prefix ) { mPrefix = prefix; }
    public boolean     IsModel()         { return false; }
    public final Matrix4f        GetParentMatrix() { return mParentMatrix; }

    public final Matrix4f        GetWorldMatrix(){
        if(mWorldMatrixDirty)
        {
            if(null!=mpParent)
            {
                Matrix4f pParentWorldMatrix = mpParent.GetWorldMatrix();
//                mWorldMatrix = mParentMatrix * *pParentWorldMatrix;
                Matrix4f.mul(pParentWorldMatrix, mParentMatrix, mWorldMatrix);
            }
            else
            {
                mWorldMatrix.load(mParentMatrix);
            }
            mWorldMatrixDirty = false;
        }

        return mWorldMatrix;
    }

    // Recursively visit all sub-nodes in breadth-first mode and mark their
    // cumulative transforms as dirty
    //-----------------------------------------------------------------------------
    public final void             MarkDirty(){
        mWorldMatrixDirty = true;

        if(mpSibling != null)
        {
            mpSibling.MarkDirty();
        }

        if(mpChild != null)
        {
            mpChild.MarkDirty();
        }
    }

    public final void             AddChild(CPUTRenderNode pNode){
        if( mpChild !=null)
        {
            mpChild.AddSibling( pNode );
        }
        else
        {
//            pNodeAddRef();
            mpChild = pNode;
        }
    }
    public final void             AddSibling(CPUTRenderNode pNode){
        if( mpSibling !=null)
        {
            mpSibling.AddSibling( pNode );
        }
        else
        {
            mpSibling = pNode;
//            pNode.AddRef();
        }
    }
    public void     Update( float deltaSeconds /*= 0.0f*/ ){}

    /**
     * Update - recursively visit all sub-nodes in breadth-first mode
     * Likely used for animation with a frame# or timestamp passed in
     * so that the update routine would calculate the new transforms
     * and called before Render() function
     * @param deltaSeconds
     */
    public void     UpdateRecursive( float deltaSeconds ){
        // TODO: Need to Update this node first.
        Update(deltaSeconds);

        if(mpSibling != null)
        {
            mpSibling.UpdateRecursive(deltaSeconds);
        }
        if(mpChild != null)
        {
            mpChild.UpdateRecursive(deltaSeconds);
        }
    }
    public void     Render(CPUTRenderParameters renderParams){}
    public void     RenderShadow(CPUTRenderParameters renderParams){}
    public void     RenderAVSMShadowed(CPUTRenderParameters renderParams){}

    /**
     * RenderRecursive - recursively visit all sub-nodes in breadth-first mode
     * @param renderParams
     */
    public void     RenderRecursive(CPUTRenderParameters renderParams){
        Render(renderParams);

        if(mpChild != null)
        {
            mpChild.RenderRecursive(renderParams);
            CPUTRenderNode pNode = mpChild.GetSibling();
            while(pNode!=null)
            {
                pNode.RenderRecursive(renderParams);
                pNode = pNode.GetSibling();
            }
        }
    }

    /** RenderRecursive - recursively visit all sub-nodes in breadth-first mode */
    public void     RenderShadowRecursive(CPUTRenderParameters renderParams){
        RenderShadow(renderParams);

        if(mpChild != null)
        {
            mpChild.RenderShadowRecursive(renderParams);
            CPUTRenderNode pNode = mpChild.GetSibling();
            while(pNode != null)
            {
                pNode.RenderShadowRecursive(renderParams);
                pNode = pNode.GetSibling();
            }
        }
    }

    /** RenderRecursive - recursively visit all sub-nodes in breadth-first mode */
    public void     RenderAVSMShadowedRecursive(CPUTRenderParameters renderParams){
        RenderAVSMShadowed(renderParams);

        if(mpChild != null)
        {
            mpChild.RenderAVSMShadowedRecursive(renderParams);
            CPUTRenderNode pNode = mpChild.GetSibling();
            while(pNode != null)
            {
                pNode.RenderAVSMShadowedRecursive(renderParams);
                pNode = pNode.GetSibling();
            }
        }
    }

    public final void             SetParentMatrix(Matrix4f parentMatrix)
    {
        mParentMatrix.load(parentMatrix);
        MarkDirty();
    }
    public final void LoadParentMatrixFromParameterBlock( CPUTConfigBlock pBlock )
    {
        // get and set the transform
        final float[] row = new float[4];
        CPUTConfigEntry pColumn = pBlock.GetValueByName("matrixColumn0");
        CPUTConfigEntry pRow    = pBlock.GetValueByName("matrixRow0");
        Matrix4f parentMatrix = new Matrix4f();
        if( pColumn.IsValid() )
        {
            /*pBlock->GetValueByName(_L("matrixColumn0"))->ValueAsFloatArray(&pMatrix[0][0], 4);
            pBlock->GetValueByName(_L("matrixColumn1"))->ValueAsFloatArray(&pMatrix[1][0], 4);
            pBlock->GetValueByName(_L("matrixColumn2"))->ValueAsFloatArray(&pMatrix[2][0], 4);
            pBlock->GetValueByName(_L("matrixColumn3"))->ValueAsFloatArray(&pMatrix[3][0], 4);
            parentMatrix = float4x4((float*)&pMatrix[0][0]);*/
            pBlock.GetValueByName("matrixColumn0").ValueAsFloatArray(row, 0, row.length);   parentMatrix.setColumn(0, row, 0);
            pBlock.GetValueByName("matrixColumn1").ValueAsFloatArray(row, 0, row.length);   parentMatrix.setColumn(1, row, 0);
            pBlock.GetValueByName("matrixColumn2").ValueAsFloatArray(row, 0, row.length);   parentMatrix.setColumn(2, row, 0);
            pBlock.GetValueByName("matrixColumn3").ValueAsFloatArray(row, 0, row.length);   parentMatrix.setColumn(3, row, 0);
        } else if( pRow.IsValid() )
        {
            /*pBlock->GetValueByName(_L("matrixRow0"))->ValueAsFloatArray(&pMatrix[0][0], 4);
            pBlock->GetValueByName(_L("matrixRow1"))->ValueAsFloatArray(&pMatrix[1][0], 4);
            pBlock->GetValueByName(_L("matrixRow2"))->ValueAsFloatArray(&pMatrix[2][0], 4);
            pBlock->GetValueByName(_L("matrixRow3"))->ValueAsFloatArray(&pMatrix[3][0], 4);
            parentMatrix = float4x4((float*)&pMatrix[0][0]);*/
            pBlock.GetValueByName("matrixRow0").ValueAsFloatArray(row, 0, row.length);   parentMatrix.setRow(0, row, 0);
            pBlock.GetValueByName("matrixRow1").ValueAsFloatArray(row, 0, row.length);   parentMatrix.setRow(1, row, 0);
            pBlock.GetValueByName("matrixRow2").ValueAsFloatArray(row, 0, row.length);   parentMatrix.setRow(2, row, 0);
            pBlock.GetValueByName("matrixRow3").ValueAsFloatArray(row, 0, row.length);   parentMatrix.setRow(3, row, 0);
        } else
        {
            /*float identity[16] = { 1.f, 0.f, 0.f, 0.f,   0.f, 1.f, 0.f, 0.f,   0.f, 0.f, 1.f, 0.f,    0.f, 0.f, 0.f, 1.f };
            parentMatrix = float4x4(identity);*/
        }
        SetParentMatrix(parentMatrix);   // set the relative transform, marking child transform's dirty
    }

    public void GetBoundingBoxRecursive(Vector3f pCenter, Vector3f pHalf)
    {
        if(mpChild!=null)   { mpChild.GetBoundingBoxRecursive(   pCenter, pHalf ); }
        if(mpSibling!=null) { mpSibling.GetBoundingBoxRecursive( pCenter, pHalf ); }
    }

    @Override
    public void dispose() {}

    protected static String _L(String s) { return s;}
}
