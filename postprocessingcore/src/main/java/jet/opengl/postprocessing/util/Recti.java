package jet.opengl.postprocessing.util;

import org.lwjgl.util.vector.Vector2i;

import java.nio.IntBuffer;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public class Recti {
    public int x,y,width, height;

    public Recti(){}

    public Recti(int x, int y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isValid(){ return width > 0 && height > 0;}
    public void zero(){x=y = width = height = 0;}
    public boolean equals(Recti rect){
        return (x == rect.x && y == rect.y&&
                width == rect.width && height == rect.height);
    }

    public void addPoint(int x, int y){
        if(!isValid()){
            set(x,y,1,1);
        }else{
            int maxx = Math.max(x, getMaxX());
            int maxy = Math.max(y, getMaxY());

            int minx = Math.min(x, this.x);
            int miny = Math.min(y, this.y);

            set(minx,miny,maxx- minx,maxy-miny);
        }
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

    public int getMaxX(){
        return x + width;
    }

    public int getMaxY(){
        return y + height;
    }

    public Vector2i size(){
        return new Vector2i(width, height);
    }

    public int getMin()
    {
        return Math.min(x,y);
    }

    @Override
    public String toString() {
        return "Recti{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }

    public int area(){
        return width * height;
    }

    public Recti load(IntBuffer buf){
        x = buf.get();
        y = buf.get();
        width = buf.get();
        height = buf.get();

        return this;
    }
}
