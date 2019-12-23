package jet.opengl.demos.gpupro.culling;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

final class ForwardRenderer extends Renderer{
    private GLSLProgram mShadingProg;
    private BufferGL mInstanceBuffer;
    private Matrix4f tmp = new Matrix4f();

    private final FloatBuffer mLineData = BufferUtils.createFloatBuffer(6 * 12);

    private GLSLProgram mLineRender;

    @Override
    void onCreate() {
        super.onCreate();

        final String root = "gpupro/Culling/shaders/";
        mShadingProg = GLSLProgram.createProgram(root + "ShadingVS.vert", root + "ShadingPS.frag", null);
        mLineRender = GLSLProgram.createProgram(root + "ShadingVS.vert", root + "LinePS.frag", null);

        mInstanceBuffer = new BufferGL();
        mInstanceBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, Matrix4f.SIZE * 64 * 2, null, GLenum.GL_DYNAMIC_DRAW);
    }

    @Override
    public void render(Scene scene,boolean clearFBO) {
        setOutputRenderTaget();
        if(clearFBO) {
            gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.1f, 0.1f, 0.1f, 1f));
            gl.glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 1.f, 0);
        }
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);

        mShadingProg.enable();
        GLSLUtil.setMat4(mShadingProg, "gProj", scene.mProj);
        GLSLUtil.setMat4(mShadingProg, "gView", scene.mView);
        GLSLUtil.setFloat3(mShadingProg, "gEyePos", scene.mEye);

        /*for(Model model : scene.mModels){
            model.mMaterial.apply(mShadingProg);

            for(Mesh mesh : model.mMeshes){
                Matrix4f world = Matrix4f.mul(model.mWorld, mesh.mWorld, tmp);
                mInstanceBuffer.update(0, CacheBuffer.wrap(world));  // pass-in the world matrix
//                GLSLUtil.setMat4(mShadingProg, "gModel", world);

                Matrix4f.getNormalMatrix(world, world);
                mInstanceBuffer.update(Matrix4f.SIZE * 64, CacheBuffer.wrap(world));  // pass in the normal matrix
//                GLSLUtil.setMat4(mShadingProg, "gNormal", world);

                gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mInstanceBuffer.getBuffer());
                *//*gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, mesh.mVertexs.getBuffer());
                gl.glEnableVertexAttribArray(0);
                gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 32, 0);
                gl.glEnableVertexAttribArray(1);
                gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, 32, 12);
                gl.glEnableVertexAttribArray(0);
                gl.glVertexAttribPointer(2, 2, GLenum.GL_FLOAT, false, 32, 24);
                gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, mesh.mIndices.getBuffer());
                gl.glDrawElements(mesh.mPology, mesh.mIndices.getBuffer());*//*
                mesh.mVao.bind();
                mesh.mVao.draw(GLenum.GL_TRIANGLES);
                mesh.mVao.unbind();
            }
        }*/

        final int numMeshes = scene.mExpandMeshes.size();
        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
            Mesh mesh = scene.mExpandMeshes.get(meshIdx);
            if(scene.mExpandMeshVisible.get(meshIdx) && mesh.frameNumber < mFrameNumber){
                Material material = scene.mMaterials.get(scene.mMeshMaterials.get(meshIdx));  // the material that the mesh related to
                Model model = scene.mModels.get(scene.mMeshModels.get(meshIdx));

                material.apply(mShadingProg);

                Matrix4f world = Matrix4f.mul(model.mWorld, mesh.mWorld, tmp);
                mInstanceBuffer.update(0, CacheBuffer.wrap(world));  // pass-in the world matrix

                Matrix4f.getNormalMatrix(world, world);
                mInstanceBuffer.update(Matrix4f.SIZE * 64, CacheBuffer.wrap(world));  // pass in the normal matrix

                gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mInstanceBuffer.getBuffer());

                mesh.mVao.bind();
                mesh.mVao.draw(GLenum.GL_TRIANGLES);
                mesh.mVao.unbind();

                mesh.frameNumber = mFrameNumber;
            }
        }

        mShadingProg.printOnce();

        // draw lines
        /*mLineRender.enable();
        GLSLUtil.setMat4(mLineRender, "gProj", scene.mProj);
        GLSLUtil.setMat4(mLineRender, "gView", scene.mView);

        mInstanceBuffer.update(0, CacheBuffer.wrap(Matrix4f.IDENTITY));  // pass-in the world matrix
        mInstanceBuffer.update(Matrix4f.SIZE * 64, CacheBuffer.wrap(Matrix4f.IDENTITY));  // pass in the normal matrix
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mInstanceBuffer.getBuffer());

        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
            Mesh mesh = scene.mExpandMeshes.get(meshIdx);
            if(scene.mExpandMeshVisible.get(meshIdx) && mesh.frameNumber == mFrameNumber){
                renderAABBLines(mesh.mAABB);
            }
        }*/

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, 0);
    }

    private void renderAABBLines(BoundingBox aabb){
        // bottom
        mLineData.clear();
        final Vector3f min = aabb._min;
        final Vector3f max = aabb._max;

        mLineData.put(min.x).put(min.y).put(min.z);  // B0
        mLineData.put(max.x).put(min.y).put(min.z);  // B1

        mLineData.put(max.x).put(min.y).put(max.z);  // B2
        mLineData.put(max.x).put(min.y).put(min.z);  // B1

        mLineData.put(max.x).put(min.y).put(max.z);  // B2
        mLineData.put(min.x).put(min.y).put(max.z);  // B3

        mLineData.put(min.x).put(min.y).put(min.z);  // B0
        mLineData.put(min.x).put(min.y).put(max.z);  // B3

        mLineData.put(min.x).put(max.y).put(min.z);  // T0
        mLineData.put(max.x).put(max.y).put(min.z);  // T1

        mLineData.put(max.x).put(max.y).put(max.z);  // T2
        mLineData.put(max.x).put(max.y).put(min.z);  // T1

        mLineData.put(max.x).put(max.y).put(max.z);  // T2
        mLineData.put(min.x).put(max.y).put(max.z);  // T3

        mLineData.put(min.x).put(max.y).put(min.z);  // T0
        mLineData.put(min.x).put(max.y).put(max.z);  // T3

        mLineData.put(min.x).put(max.y).put(min.z);  // T0
        mLineData.put(min.x).put(min.y).put(min.z);  // B0

        mLineData.put(max.x).put(min.y).put(min.z);  // B1
        mLineData.put(max.x).put(max.y).put(min.z);  // T1

        mLineData.put(max.x).put(max.y).put(max.z);  // T2
        mLineData.put(max.x).put(min.y).put(max.z);  // B2

        mLineData.put(min.x).put(min.y).put(max.z);  // B3
        mLineData.put(min.x).put(max.y).put(max.z);  // T3

        mLineData.flip();

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);

        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 0, mLineData);
        gl.glEnableVertexAttribArray(0);

        gl.glDrawArrays(GLenum.GL_LINES, 0, 24);
        gl.glDisableVertexAttribArray(0);
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
