package jet.opengl.demos.intel.cput;

/**
 * Created by mazhen'gui on 2017/11/14.
 */

public class CPUTNullNode extends CPUTRenderNode{
    public int LoadNullNode(CPUTConfigBlock pBlock/*, int *pParentID*/){
        // set the null/group node name
        mName = pBlock.GetValueByName("name").ValueAsString();

        // get the parent ID
        int parentID = pBlock.GetValueByName("parent").ValueAsInt();

        LoadParentMatrixFromParameterBlock( pBlock );
        return parentID;
    }
}
