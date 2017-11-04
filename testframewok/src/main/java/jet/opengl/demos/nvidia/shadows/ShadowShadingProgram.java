package jet.opengl.demos.nvidia.shadows;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/11/4.
 */

public abstract class ShadowShadingProgram extends GLSLProgram {

    /**
     * Subclass must call this method to initlize the resources.
     */
    protected final void initShadow(){

    }

    public void setFilter(ShadowScene.ShadowMapFiltering filter){

    }

}
