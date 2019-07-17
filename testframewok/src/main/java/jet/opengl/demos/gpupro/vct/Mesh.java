package jet.opengl.demos.gpupro.vct;

import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.obj.NvGLModel;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

class Mesh implements Disposeable {

    private int mSelfShape;  // quad
    private GLVAO mShape;
    private NvGLModel mModel;

    Mesh(int shape){  mSelfShape = shape; }
    Mesh(GLVAO shape){  mShape = shape; }
    Mesh(NvGLModel model) {mModel = model;}

     void draw(){
        if(mShape != null) {
            mShape.bind();
            mShape.draw(GLenum.GL_TRIANGLES);
            mShape.unbind();
        }else if(mModel != null)
            mModel.drawElements(0, 1);
        else if(mSelfShape != 0){
            final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            gl.glBindVertexArray(mSelfShape);
            gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
            gl.glBindVertexArray(0);
        }else{
            throw new NullPointerException();
        }
     }

    @Override
    public void dispose() {
        if(mShape != null){
            mShape.dispose();
            mShape = null;
        }else if(mModel != null){
            mModel.dispose();
            mModel = null;
        }else if(mSelfShape != 0){
            final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            gl.glDeleteVertexArray(mSelfShape);
            mSelfShape = 0;
        }
    }
}
