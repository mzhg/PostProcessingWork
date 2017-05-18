package jet.opengl.postprocessing.core;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

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
    public Texture2D shadowMapTexture;

    public Texture2D outputTexture;
    public boolean colorDepthCombined;

    public Matrix4f viewMat;
    public Matrix4f projMat;
    public float    fov;
    public boolean  ortho;

    /** The light position in world space.*/
    public Vector3f lightPos;
    public Vector3f lightDirection;
    public Matrix4f lightProjMat;
    public Matrix4f lightViewMat;  // TODO should be a array.

    private Matrix4f viewProjMatrix;
    private boolean  bViewProjSetted = false;
    private Matrix4f viewProjInvertMatrix;
    private boolean  bViewProjInvertSetted = false;
    private Matrix4f projInvertMatrix;
    private boolean bProjInvertSetted = false;

    private Matrix4f lightViewProjMatrix;
    private boolean  bLightViewProjSetted = false;

    private final Vector3f cameraPos = new Vector3f();

    public float cameraNear, cameraFar;

    void reset(){
        viewMat = null;
        projMat = null;
        bViewProjSetted = false;
        bViewProjInvertSetted = false;
        bProjInvertSetted = false;
        bLightViewProjSetted = false;
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

    public void setLightViewProjMatrix(Matrix4f matrix){
        if(matrix != null){
            bLightViewProjSetted = true;
            if(lightViewProjMatrix == null)
                lightViewProjMatrix = new Matrix4f(matrix);
            else
                lightViewProjMatrix.load(matrix);
        }else{
            bLightViewProjSetted = false;
        }
    }

    public Matrix4f getLightViewProjMatrix(){
        if(bLightViewProjSetted){
            return lightViewProjMatrix;
        }else{
            if(lightViewMat == null || lightProjMat == null){
                throw new NullPointerException("lightViewMat or lightProjMat is null.");
            }

            if(lightViewProjMatrix == null){
                lightViewProjMatrix = new Matrix4f();
            }

            Matrix4f.mul(lightProjMat, lightViewMat, lightViewProjMatrix);
            bLightViewProjSetted = true;
            return lightViewProjMatrix;
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

    public ReadableVector3f getCameraPos(){
        Matrix4f.decompseRigidMatrix(viewMat, cameraPos, null, null);
        return cameraPos;
    }
}
