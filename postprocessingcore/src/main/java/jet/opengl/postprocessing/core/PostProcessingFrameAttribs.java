package jet.opengl.postprocessing.core;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.Recti;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public class PostProcessingFrameAttribs {
    public final Recti viewport = new Recti();
    public final Recti clipRect = new Recti();
    public Texture2D sceneColorTexture;
    public Texture2D sceneDepthTexture;
    public Texture2D outputTexture;
    public boolean colorDepthCombined;

    public Matrix4f viewMat;
    public Matrix4f projMat;
    public float    fov;
    public boolean  ortho;

    private Matrix4f viewProjMatrix;
    private boolean  bViewProjSetted = false;
    private Matrix4f viewProjInvertMatrix;
    private boolean  bViewProjInvertSetted = false;
    private Matrix4f projInvertMatrix;
    private boolean bProjInvertSetted = false;

    public float cameraNear, cameraFar;

    void reset(){
        viewMat = null;
        projMat = null;
        bViewProjSetted = false;
        bViewProjInvertSetted = false;
        bProjInvertSetted = false;
    }

    public void setViewProjMatrix(Matrix4f matrix){
        if(matrix != null){
            bViewProjSetted = true;
            if(viewProjMatrix == null)
                viewProjMatrix = new Matrix4f(matrix);
            else
                viewProjMatrix.load(matrix);
        }else{
            bViewProjSetted = false;
        }
    }

    public Matrix4f getViewProjMatrix(){
       if(bViewProjSetted){
           return viewProjMatrix;
       }else{
           if(viewMat == null || projMat == null){
               throw new NullPointerException("viewMat or projMat is null.");
           }

           if(viewProjMatrix == null){
               viewProjMatrix = new Matrix4f();
           }

           Matrix4f.mul(projMat, viewMat, viewProjMatrix);
           bViewProjSetted = true;
           return viewProjMatrix;
       }
    }

    public Matrix4f getViewProjInvertMatrix(){
        if(bViewProjInvertSetted){
            return viewProjInvertMatrix;
        }

        Matrix4f viewProjMat = getViewProjMatrix();
        if(viewProjInvertMatrix == null){
            viewProjInvertMatrix = new Matrix4f();
        }
        Matrix4f.invert(viewProjMat, viewProjInvertMatrix);
        bViewProjInvertSetted = true;
        return viewProjInvertMatrix;
    }

    /**
     * Get the invers of the projection matrix.
     * @return
     */
    public Matrix4f getProjInvertMatrix(){
        if(bProjInvertSetted){
            return projInvertMatrix;
        }

        if(projMat == null)
            throw new NullPointerException("projection matrix is null.");

        if(projInvertMatrix == null){
            projInvertMatrix = new Matrix4f();
        }
        Matrix4f.invert(projMat, projInvertMatrix);
        bProjInvertSetted = true;
        return projInvertMatrix;
    }
}
