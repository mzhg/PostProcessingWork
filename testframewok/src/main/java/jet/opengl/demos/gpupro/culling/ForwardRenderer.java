package jet.opengl.demos.gpupro.culling;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.util.CacheBuffer;

final class ForwardRenderer extends Renderer{
    private GLSLProgram mShadingProg;
    private BufferGL mInstanceBuffer;
    private Matrix4f tmp = new Matrix4f();

    @Override
    void onCreate() {
        super.onCreate();

        final String root = "gpupro/Culling/shaders/";
        mShadingProg = GLSLProgram.createProgram(root + "ShadingVS.vert", root + "ShadingPS.frag", null);
        mInstanceBuffer = new BufferGL();
        mInstanceBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, Matrix4f.SIZE * 64 * 2, null, GLenum.GL_DYNAMIC_DRAW);
    }

    @Override
    public void render(Scene scene) {
        setOutputRenderTaget();

        mShadingProg.enable();
        GLSLUtil.setMat4(mShadingProg, "gProj", scene.mProj);
        GLSLUtil.setMat4(mShadingProg, "gView", scene.mView);
        GLSLUtil.setFloat3(mShadingProg, "gEyePos", scene.mEye);

        gl.glBindVertexArray(0);

        for(Model model : scene.mModels){
            model.mMaterial.apply(mShadingProg);

            for(Mesh mesh : model.mMeshes){
                Matrix4f world = Matrix4f.mul(model.mWorld, mesh.mWorld, tmp);
                mInstanceBuffer.update(0, CacheBuffer.wrap(world));  // pass-in the world matrix

                Matrix4f.getNormalMatrix(world, world);
                mInstanceBuffer.update(Matrix4f.SIZE * 64, CacheBuffer.wrap(world));  // pass in the normal matrix

                gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mInstanceBuffer.getBuffer());
                /*gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, mesh.mVertexs.getBuffer());
                gl.glEnableVertexAttribArray(0);
                gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 32, 0);
                gl.glEnableVertexAttribArray(1);
                gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, 32, 12);
                gl.glEnableVertexAttribArray(0);
                gl.glVertexAttribPointer(2, 2, GLenum.GL_FLOAT, false, 32, 24);
                gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, mesh.mIndices.getBuffer());
                gl.glDrawElements(mesh.mPology, mesh.mIndices.getBuffer());*/
                mesh.mVao.bind();
                mesh.mVao.draw(GLenum.GL_TRIANGLES);
                mesh.mVao.unbind();
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
