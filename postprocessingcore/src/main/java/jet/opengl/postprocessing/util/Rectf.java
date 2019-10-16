package jet.opengl.postprocessing.util;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector2i;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public class Rectf {
    public float x,y,width, height;

    public Rectf(){}

    public Rectf(float x, float y, float width, float height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isValid(){ return width > 0 && height > 0;}
    public void zero(){x=y = width = height = 0;}
    public boolean equals(Rectf rect){
        return (x == rect.x && y == rect.y&&
                width == rect.width && height == rect.height);
    }

    public void set(Rectf o){
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

    public float getMaxX(){
        return x + width;
    }

    public float getMaxY(){
        return y + height;
    }

    public Vector2f size(){
        return new Vector2f(width, height);
    }

    public float getMin()
    {
        return Math.min(x,y);
    }

    @Override
    public String toString() {
        return "Rectf{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }

    public float area(){
        return width * height;
    }

    public boolean contains(float x, float y) {
        return x >= this.x && x <= this.x + width &&
           y >=  this.y && y <= this.y + height;
    }
}
