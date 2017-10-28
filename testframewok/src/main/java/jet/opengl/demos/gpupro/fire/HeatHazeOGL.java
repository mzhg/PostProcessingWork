package jet.opengl.demos.gpupro.fire;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Random;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

final class HeatHazeOGL extends HeatHaze{
    private int d_fProfile;
    private GLSLProgram d_fProgHeatHazeDist, d_fProgUseHeatHazeDist;

    private int d_baseDistMap,
            d_inputToDist, d_blurredInputToDist, d_distortionMap;
    private Texture2D d_baseDistMapTexNo;
    private final int[] d_fxTextures = new int[3];
    private GLFuncProvider gl;
    private final Random random = new Random();
    private final Matrix4f model = new Matrix4f();

    boolean init(Vector3f r_f, Vector3f r_c, Vector3f d_c, Vector3f u_c, float timeScale, float timeGapClouds,
                 float halfSizeInit, float halfSizeAmpl, float initHorizSpiralRadius, float spiralParameter,
                 float spiralSpeedAmpl, float vertSpeedInit, float vertSpeedAmpl, float turbulenceSpeed,
                 float lifetimeAmpl, int baseDistTexSize, int maxDistort, float radiusDistParam, float amplDistParam,
                 float slopeDistParam, float vertNoDistParam, float rescaleDistTex, int inputToDistTexNo,
                 int blurredInputToDistTexNo, int distortionMapTexNo){

        d_fxTextures[0] = inputToDistTexNo;
        d_fxTextures[1] = blurredInputToDistTexNo;
        d_fxTextures[2] = distortionMapTexNo;

        gl = GLFuncProviderFactory.getGLFuncProvider();

        if(!super.init(timeScale,timeGapClouds,halfSizeInit,halfSizeAmpl,
                initHorizSpiralRadius,spiralParameter,spiralSpeedAmpl,
                vertSpeedInit,vertSpeedAmpl,turbulenceSpeed,lifetimeAmpl,
                r_f,r_c,d_c,u_c)) return false;

        int center = baseDistTexSize/2;

        baseDistTexSize*=rescaleDistTex;
        center*=rescaleDistTex;
        radiusDistParam*=rescaleDistTex;
        amplDistParam*=rescaleDistTex;
        slopeDistParam/=rescaleDistTex;

        int coord_X, coord_Y;
        float alpha,  scaling, scaling2;
        float temp_var;

        random.setSeed(10086);
//				typedef float typeRGB[3];
//				typeRGB *table = new typeRGB[baseDistTexSize*baseDistTexSize];
        FloatBuffer table = CacheBuffer.getCachedFloatBuffer(baseDistTexSize * baseDistTexSize * 3);

        for(int j=0;j<baseDistTexSize;j++)
            for(int i=0;i<baseDistTexSize;i++){
                coord_X = i - center;
                coord_Y = j - center;

                if(coord_X==0){
                    if(coord_Y>=0) alpha = 3.14159265f * 0.5f;
                    else alpha = -3.14159265f * 0.5f;
                }
                else {
                    alpha = (float) Math.atan((float)coord_Y/(float)coord_X);
                }

                scaling = (float) (0.5 - 1.0/3.14159265 * Math.atan(slopeDistParam*(Math.sqrt((float)(coord_X*coord_X + coord_Y*coord_Y))-radiusDistParam-amplDistParam*Math.cos(vertNoDistParam*alpha))));

                temp_var = 0.25f * baseDistTexSize*baseDistTexSize - coord_X*coord_X - coord_Y*coord_Y;
                if(temp_var>0)
                    scaling2 = (float) (2.0 * Math.sqrt(temp_var)/baseDistTexSize);
                else scaling2 = 0.0f;

                float x/*table[j*baseDistTexSize+i][0]*/ = ( scaling2*scaling*maxDistort*((float)random.nextDouble() - 0.5f) )/255;
                float y/*table[j*baseDistTexSize+i][1]*/ = ( scaling2*scaling*maxDistort*((float)random.nextDouble() - 0.5f) )/255;
                float gaussian = (float)(coord_X*coord_X + coord_Y*coord_Y)/(radiusDistParam*radiusDistParam);
                float z/*table[j*baseDistTexSize+i][2]*/ = (float) ((gaussian<1) ? 0.8 * Math.exp(-gaussian/0.36) : 0.0);

                table.put(x).put(y).put(z);
            }
        table.flip();

        /*GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,ARBTextureFloat.GL_RGB16F_ARB,baseDistTexSize,baseDistTexSize,0, GL11.GL_RGB, GL11.GL_FLOAT,table);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV,GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);*/

        TextureDataDesc initData = new TextureDataDesc(GLenum.GL_RGB, GLenum.GL_FLOAT, table);
        d_baseDistMapTexNo = TextureUtils.createTexture2D(new Texture2DDesc(baseDistTexSize, baseDistTexSize, GLenum.GL_RGB16F), initData);
        gl.glBindTexture(d_baseDistMapTexNo.getTarget(), d_baseDistMapTexNo.getTexture());
        gl.glTexParameteri(d_baseDistMapTexNo.getTarget(), GLenum.GL_TEXTURE_MAX_LEVEL, 0);
        gl.glBindTexture(d_baseDistMapTexNo.getTarget(), 0);

        /*d_fProgHeatHazeDist = CgGL.cgCreateProgramFromFile(d_cgContext, CgGL.CG_SOURCE, file_dir + "f_heatHazeDistortion.cg", d_fProfile, "f_heatHazeDistortion", null);
        d_baseDistMap = CgGL.cgGetNamedParameter(d_fProgHeatHazeDist,"baseDistMap");
        CgGL.cgGLSetTextureParameter(d_baseDistMap,d_baseDistMapTexNo);
        CgGL.cgGLLoadProgram(d_fProgHeatHazeDist);*/

        try {
            d_fProgHeatHazeDist = GLSLProgram.createFromFiles("gpupro/Fire/shaders/HeatHazeDistortionVS.vert", "gpupro/Fire/shaders/HeatHazeDistortionPS.frag");
            d_baseDistMap = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*d_fProgUseHeatHazeDist = CgGL.cgCreateProgramFromFile(d_cgContext, CgGL.CG_SOURCE, file_dir + "f_useHeatHaze.cg", d_fProfile, "f_useHeatHaze", null);
        d_inputToDist = CgGL.cgGetNamedParameter(d_fProgUseHeatHazeDist,"inputToDist");
        CgGL.cgGLSetTextureParameter(d_inputToDist,d_fxTextures[0]);
        d_blurredInputToDist = CgGL.cgGetNamedParameter(d_fProgUseHeatHazeDist,"blurredInputToDist");
        CgGL.cgGLSetTextureParameter(d_blurredInputToDist,d_fxTextures[1]);
        d_distortionMap = CgGL.cgGetNamedParameter(d_fProgUseHeatHazeDist,"distortionMap");
        CgGL.cgGLSetTextureParameter(d_distortionMap,d_fxTextures[2]);
        CgGL.cgGLLoadProgram(d_fProgUseHeatHazeDist);*/

        try {
            d_fProgUseHeatHazeDist = GLSLProgram.createFromFiles("gpupro/Fire/shaders/UseHeatHazeVS.vert", "gpupro/Fire/shaders/UseHeatHazePS.frag");
            d_inputToDist = 0;
            d_blurredInputToDist = 1;
            d_distortionMap = 2;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    void attachHeatHazeObjects(){
        /*CgGL.cgGLEnableProfile(d_fProfile);
        CgGL.cgGLBindProgram(d_fProgUseHeatHazeDist);
        CgGL.cgGLEnableTextureParameter(d_inputToDist);
        CgGL.cgGLEnableTextureParameter(d_blurredInputToDist);
        CgGL.cgGLEnableTextureParameter(d_distortionMap);*/

        d_fProgUseHeatHazeDist.enable();
        gl.glActiveTexture(GLenum.GL_TEXTURE2);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, d_fxTextures[2]);
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, d_fxTextures[1]);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, d_fxTextures[0]);
    }

    void detachHeatHazeObjects(){
//        CgGL.cgGLDisableProfile(d_fProfile);

        d_fProgUseHeatHazeDist.disable();
        gl.glActiveTexture(GLenum.GL_TEXTURE2);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
    }

    void placeHeatHaze(){
//        GL11.glTranslatef(d_HeatHazeLocation.x, d_HeatHazeLocation.y, d_HeatHazeLocation.z);
        model.translate(d_HeatHazeLocation.x, d_HeatHazeLocation.y, d_HeatHazeLocation.z);
    }

    void makeHeatHazeDistTex(Matrix4f rotationMatrix, Matrix4f viewPoj){
        /*CgGL.cgGLEnableProfile(d_fProfile);
        CgGL.cgGLBindProgram(d_fProgHeatHazeDist);
        CgGL.cgGLEnableTextureParameter(d_baseDistMap);*/
        d_fProgHeatHazeDist.enable();
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(d_baseDistMapTexNo.getTarget(), d_baseDistMapTexNo.getTexture());

        gl.glBlendEquation(GLenum.GL_FUNC_ADD);
        gl.glBlendFunc(GLenum.GL_SRC_ALPHA,GLenum.GL_ONE);
        gl.glEnable(GLenum.GL_BLEND);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);

        gl.glEnableVertexAttribArray(1);
        gl.glEnableVertexAttribArray(0);

        Iterator<Cloud> it = d_clouds_p.iterator();
        while(it.hasNext()){
            Cloud cloud = it.next();
            /*GL11.glPushMatrix();
            GL11.glTranslatef(cloud.d_position[0], cloud.d_position[1], cloud.d_position[2]);
            GL13.glMultTransposeMatrix(wrap16(rotationMatrix));
            GL11.glMultMatrix(wrap16(d_CloudFacePerpVisDirRot));
            GL11.glRotatef(cloud.d_rotation,0,0,1);*/
            model.setTranslate(cloud.d_position[0], cloud.d_position[1], cloud.d_position[2]);
            Matrix4f.mul(model, rotationMatrix, model);   // TODO rotationMatrix = rotationMatrix * d_CloudFacePerpVisDirRot
            model.rotate(cloud.d_rotation,0,0,1);
            Matrix4f.mul(viewPoj, model, model);

            int location = d_fProgHeatHazeDist.getUniformLocation("g_ModelViewProj");
            gl.glUniformMatrix4fv(location, false, CacheBuffer.wrap(model));

            /*GL11.glBegin(GL11.GL_QUADS);
            {
                ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE0_ARB, 0, 0);
                ARBMultitexture.glMultiTexCoord1fARB(ARBMultitexture.GL_TEXTURE1_ARB, cloud.d_lifetime_left);
                GL11.glVertex3f(-cloud.d_halfSize, -cloud.d_halfSize, 0.0f);

                ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE0_ARB, 1, 0);
                ARBMultitexture.glMultiTexCoord1fARB(ARBMultitexture.GL_TEXTURE1_ARB, cloud.d_lifetime_left);
                GL11.glVertex3f(cloud.d_halfSize, -cloud.d_halfSize, 0.0f);

                ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE0_ARB, 1, 1);
                ARBMultitexture.glMultiTexCoord1fARB(ARBMultitexture.GL_TEXTURE1_ARB, cloud.d_lifetime_left);
                GL11.glVertex3f(cloud.d_halfSize, cloud.d_halfSize, 0.0f);

                ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE0_ARB, 0, 1);
                ARBMultitexture.glMultiTexCoord1fARB(ARBMultitexture.GL_TEXTURE1_ARB, cloud.d_lifetime_left);
                GL11.glVertex3f(-cloud.d_halfSize, cloud.d_halfSize, 0.0f);
            }
            GL11.glEnd();
            GL11.glPopMatrix();*/
            FloatBuffer arrayBuffer = CacheBuffer.getCachedFloatBuffer(5 * 4);
            arrayBuffer.put(0).put(0);
            arrayBuffer.put(cloud.d_lifetime_left);
            arrayBuffer.put(-cloud.d_halfSize).put(-cloud.d_halfSize);

            arrayBuffer.put(1).put(0);
            arrayBuffer.put(cloud.d_lifetime_left);
            arrayBuffer.put(cloud.d_halfSize).put(-cloud.d_halfSize);

            arrayBuffer.put(1).put(1);
            arrayBuffer.put(cloud.d_lifetime_left);
            arrayBuffer.put(cloud.d_halfSize).put(cloud.d_halfSize);

            arrayBuffer.put(0).put(1);
            arrayBuffer.put(cloud.d_lifetime_left);
            arrayBuffer.put(-cloud.d_halfSize).put(cloud.d_halfSize);
            arrayBuffer.flip();

            gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, 5 * 4, arrayBuffer);
            arrayBuffer.position(3);
            gl.glVertexAttribPointer(0, 2, GLenum.GL_FLOAT, false, 5 * 4, arrayBuffer);

            gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);
        }

        gl.glDisableVertexAttribArray(1);
        gl.glDisableVertexAttribArray(0);

        gl.glDisable(GLenum.GL_BLEND);
//        CgGL.cgGLDisableProfile(d_fProfile);
    }
}
