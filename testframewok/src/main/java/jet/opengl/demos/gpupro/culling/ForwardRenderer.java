package jet.opengl.demos.gpupro.culling;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashSet;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

final class ForwardRenderer extends Renderer{
    private GLSLProgram mShadingProg;
    private BufferGL mInstanceBuffer;
    private Matrix4f tmp = new Matrix4f();

    private final FloatBuffer mLineData = BufferUtils.createFloatBuffer(6 * 12);

    private GLSLProgram mLineRender;
    private Texture2D mPickResults;
    private BufferGL mPickResultStaging;
    private final HashSet<Integer> mPickObjectID = new HashSet<>();

    private GLSLProgram mSihouetteFrontRender;
    private GLSLProgram mSihouetteBackRender;
    private GLSLProgram mSihouetteGSRender;

    @Override
    void onCreate() {
        super.onCreate();

        final String root = "gpupro/Culling/shaders/";
        mShadingProg = GLSLProgram.createProgram(root + "ShadingVS.vert", root + "ShadingPS.frag", null);
        mLineRender = GLSLProgram.createProgram(root + "ShadingVS.vert", root + "LinePS.frag", null);

        mSihouetteBackRender = GLSLProgram.createProgram(root + "SilhouetteBackVS.vert", root + "SilhouettePS.frag", Macro.asMacros("RENDER_FACE", 2));
        mSihouetteBackRender.setName("SilhouetteBack");
        mSihouetteFrontRender = GLSLProgram.createProgram(root + "SilhouetteFrontVS.vert", root + "SilhouettePS.frag", Macro.asMacros("RENDER_FACE", 1));
        mSihouetteFrontRender.setName("SilhouetteFront");
        mSihouetteGSRender = GLSLProgram.createProgram(root + "ShadingVS.vert", root + "SilhouetteGS.gemo", root + "SilhouettePS.frag", Macro.asMacros("RENDER_FACE", 0));
        mSihouetteGSRender.setName("SilhouetteGS");
        mInstanceBuffer = new BufferGL();
        mInstanceBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, Matrix4f.SIZE * 64 * 2, null, GLenum.GL_DYNAMIC_DRAW);
    }

    @Override
    void onResize(int width, int height) {
        super.onResize(width, height);

        int resultWidth = width / 4;
        int resultHeight = height / 4;
        mPickResults = TextureUtils.resizeTexture2D(mPickResults, resultWidth, resultHeight, GLenum.GL_R32UI);

        int totalBytes = resultWidth * resultHeight * 4;
        if(mPickResultStaging == null || mPickResultStaging.getBufferSize() < totalBytes){
            mPickResultStaging = new BufferGL();
            mPickResultStaging.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, totalBytes, null, GLenum.GL_DYNAMIC_READ);
        }
    }

    @Override
    public void renderSolid(Scene scene,RenderInput input, RenderOutput output) {
        setOutputRenderTaget();
        if(input.clearFBO) {
            gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.1f, 0.1f, 0.1f, 1f));
            gl.glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 1.f, 0);
        }
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        renderFace(false);

        mShadingProg.enable();
        GLSLUtil.setMat4(mShadingProg, "gProj", scene.mProj);
        GLSLUtil.setMat4(mShadingProg, "gView", scene.mView);
        GLSLUtil.setFloat3(mShadingProg, "gEyePos", scene.mEye);

        boolean bNeedPicking = input.pickType != null && input.pickType != PickType.None;
        if(bNeedPicking){
            GLSLUtil.setInt(mShadingProg, "gPickType", input.pickType.ordinal());
            GLSLUtil.setInt4(mShadingProg, "gPickRect", input.pickRect);

            gl.glBindImageTexture(0, mPickResults.getTexture(), 0, false, 0, GLenum.GL_READ_WRITE, mPickResults.getFormat());
            gl.glClearTexImage(mPickResults.getTexture(), 0, GLenum.GL_RED_INTEGER, GLenum.GL_UNSIGNED_INT, CacheBuffer.wrap(-1,-1,-1,-1));
            GLCheck.checkError();
        }else{
            GLSLUtil.setInt(mShadingProg, "gPickType", 0);
        }

        final int numMeshes = scene.mSolidMeshes.size();
        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
            Mesh mesh = scene.mExpandMeshes.get(meshIdx);

            GLSLUtil.setInt(mShadingProg, "gObjectID", mesh.objectID);

            if(scene.mExpandMeshVisible.get(meshIdx) && mesh.frameNumber < mFrameNumber){
                Material material = scene.mMaterials.get(scene.mMeshMaterials.get(meshIdx));  // the material that the mesh related to
                if(material.mTransparency)
                    continue;

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

        // handle the picking results
        if(bNeedPicking){
            gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_READ_WRITE, mPickResults.getFormat());
            GLCheck.checkError();

            retrivePickResult(input, output);
        }

        renderPickedObjects(scene,input.pickedRenderType);

        gl.glDisable(GLenum.GL_CULL_FACE);
    }

    private void renderFace(boolean front){
        gl.glEnable(GLenum.GL_CULL_FACE);
        gl.glFrontFace(GLenum.GL_CCW);
        gl.glCullFace(front ? GLenum.GL_BACK : GLenum.GL_FRONT);
    }

    @Override
    void renderTransparency(Scene scene, RenderInput input, RenderOutput output) {
        setOutputRenderTaget();
        if(!input.writeFBO) {
            gl.glColorMask(false, false, false, false);
        }
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);

        input.transparencyRenderProg.enable();
        GLSLUtil.setMat4(input.transparencyRenderProg, "gProj", scene.mProj);
        GLSLUtil.setMat4(input.transparencyRenderProg, "gView", scene.mView);
        GLSLUtil.setFloat3(input.transparencyRenderProg, "gEyePos", scene.mEye);

        final int numMeshes = scene.mSolidMeshes.size();
        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
            Mesh mesh = scene.mExpandMeshes.get(meshIdx);

            if(scene.mExpandMeshVisible.get(meshIdx) && mesh.frameNumber < mFrameNumber){
                Material material = scene.mMaterials.get(scene.mMeshMaterials.get(meshIdx));  // the material that the mesh related to
                if(!material.mTransparency)
                    continue;

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

        if(!input.writeFBO) {
            gl.glColorMask(true, true, true, true);
        }

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, 0);
    }

    private void retrivePickResult(RenderInput input, RenderOutput output){
        // Reading results.
        gl.glBindBuffer(GLenum.GL_PIXEL_PACK_BUFFER, mPickResultStaging.getBuffer());
        gl.glGetTextureImage(mPickResults.getTexture(), 0, GLenum.GL_RED_INTEGER, GLenum.GL_UNSIGNED_INT, mPickResultStaging.getBufferSize(), 0);
        gl.glBindBuffer(GLenum.GL_PIXEL_PACK_BUFFER, 0);
        GLCheck.checkError();

        int width, height;
        ByteBuffer results;
        if(input.pickType == PickType.Single){
            width = height = 1;
            results = mPickResultStaging.map(0, 4, GLenum.GL_MAP_READ_BIT);
        }else{
            width = mPickResults.getWidth();
            height = mPickResults.getHeight();
            results = mPickResultStaging.map(GLenum.GL_MAP_READ_BIT);
        }

        output.pickResults.clear();
        mPickObjectID.clear();
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                int index = j * width + i;

                int objID = results.getInt(index * 4);
                if(objID != -1){
                    if(mPickObjectID.add(objID)){
                        output.pickResults.push(objID);
                    }
                }
            }
        }

        mPickResultStaging.unmap();
    }

    private void renderPickedObjects(Scene scene, PickedRenderType renderType){
        if(mPickObjectID.isEmpty())
            return;

        if(renderType == PickedRenderType.Wireframe){
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthFunc(GLenum.GL_LEQUAL);
            gl.glDepthMask(false);
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
            gl.glLineWidth(2.f);

            gl.glDisable(GLenum.GL_CULL_FACE);

            mLineRender.enable();
            GLSLUtil.setMat4(mLineRender, "gProj", scene.mProj);
            GLSLUtil.setMat4(mLineRender, "gView", scene.mView);
            GLSLUtil.setFloat3(mLineRender, "gEyePos", scene.mEye);
            GLSLUtil.setInt(mLineRender, "gPickType", 0);
            GLSLUtil.setFloat4(mLineRender, "gColor", 1,1,1,1);  // white color

            drawPickedMeshes(scene);

            gl.glDepthFunc(GLenum.GL_LEQUAL);
            gl.glDepthMask(true);
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
            gl.glLineWidth(1.f);
        }else if(renderType == PickedRenderType.Silhouette){
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthFunc(GLenum.GL_LEQUAL);
            gl.glDepthMask(false);

            renderFace(true);

            mSihouetteGSRender.enable();
            GLSLUtil.setMat4(mSihouetteGSRender, "gProj", scene.mProj);
            GLSLUtil.setMat4(mSihouetteGSRender, "gView", scene.mView);
            GLSLUtil.setFloat3(mSihouetteGSRender, "gEyePos", scene.mEye);
            GLSLUtil.setFloat3(mSihouetteGSRender, "gCameraForward", scene.mCameraForward);
            GLSLUtil.setInt(mSihouetteGSRender, "gPickType", 0);
            GLSLUtil.setFloat4(mSihouetteGSRender, "gColor", 1,1,1,1);  // white color

            drawPickedMeshes(scene);
            mSihouetteGSRender.printOnce();

            gl.glDisable(GLenum.GL_CULL_FACE);
            gl.glDepthMask(true);
        }
    }

    private void drawPickedMeshes(Scene scene){
        final int numMeshes = scene.mSolidMeshes.size();
        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
            Mesh mesh = scene.mExpandMeshes.get(meshIdx);

            GLSLUtil.setInt(mLineRender, "gObjectID", mesh.objectID);

            if(mPickObjectID.contains(mesh.objectID)){
                Model model = scene.mModels.get(scene.mMeshModels.get(meshIdx));
                Matrix4f world = Matrix4f.mul(model.mWorld, mesh.mWorld, tmp);
                mInstanceBuffer.update(0, CacheBuffer.wrap(world));  // pass-in the world matrix

                Matrix4f.getNormalMatrix(world, world);
                mInstanceBuffer.update(Matrix4f.SIZE * 64, CacheBuffer.wrap(world));  // pass in the normal matrix

                gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, mInstanceBuffer.getBuffer());
                mesh.mVao.bind();
                mesh.mVao.draw(GLenum.GL_TRIANGLES);
                mesh.mVao.unbind();
            }
        }
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
