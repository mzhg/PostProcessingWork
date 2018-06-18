package jet.opengl.demos.intel.fluid.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Technique to render a mesh.<p></p>
 * Created by Administrator on 2018/3/13 0013.
 */

public class Technique {
    private Effect       mParentEffect   ;
    private final ArrayList<Pass> mPasses = new ArrayList<>();   ///< List of passes when rendering mesh

    public Technique( Effect parentEffect ){
        mParentEffect = parentEffect;

        addNewPass();
    }

    public Pass  addNewPass(){
        Pass newPass = new Pass( this ) ;
        mPasses.add( newPass ) ;
        return newPass ;
    }

    public List<Pass> getPasses() { return mPasses ; }

    public void clearPasses(){
        mPasses.clear();
    }
}
