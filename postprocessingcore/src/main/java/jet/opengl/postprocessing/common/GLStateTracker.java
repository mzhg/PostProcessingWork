package jet.opengl.postprocessing.common;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public class GLStateTracker {

    private ImageBinding[] imageBindings;

    private static GLStateTracker instance;
    private GLStateTracker(){}

    public static GLStateTracker getInstance(){
        if(instance == null){
            instance = new GLStateTracker();
        }

        return instance;
    }

    public void saveStates(){

    }

    public void restoreStates(){

    }

    public void bindImage(int unit, int textureID, int level, boolean layered, int layer,int  access,int format){

    }

    public void bindTexture(int unit, int target, int textureID, int sampler){

    }

    @SuppressWarnings({"NoteSafe"})
    public void bindBuffer(int target, int buffer){

    }

    // bind the texture to current units
    public void bindTexture(int target, int textureID, int sampler){

    }

    public void bindVAO(int vao) {
    }

    public void bindFramebuffer(int target, int m_framebuffer) {
    }

    private static final class StateDesc{

    }

    private static final class ImageBinding{
        int textureID;
        int access;
        int unit;
    }
}
