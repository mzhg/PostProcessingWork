package jet.opengl.postprocessing.util;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public class Recti {
    public int x,y,width, height;

    public boolean isValid(){ return width > 0 && height > 0;}
    public void zero(){x=y = width = height = 0;}
}
