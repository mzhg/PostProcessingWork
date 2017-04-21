package jet.opengl.postprocessing.util;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public class Recti {
    public int x,y,width, height;

    public boolean isValid(){ return width > 0 && height > 0;}
    public void zero(){x=y = width = height = 0;}
    public boolean equals(Recti rect){
        return x != rect.x || y != rect.y||
                width != rect.width || height != rect.height;
    }

    public void set(Recti o){
        x =o.x;
        y =o.y;
        width =o.width;
        height =o.height;
    }

    public void set(int x, int y, int width, int height){
        this.x =x;
        this.y =y;
        this.width =width;
        this.height =height;
    }
}
