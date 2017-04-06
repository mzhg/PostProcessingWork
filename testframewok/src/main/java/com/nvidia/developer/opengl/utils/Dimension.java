package com.nvidia.developer.opengl.utils;

/**
 * Created by mazhen'gui on 2017/3/21.
 */

public class Dimension {
    public int width;
    public int height;

    public Dimension(){}
    public void setSize(int width, int height) {
        this.width = width; this.height = height;
    }
    public Dimension(int width, int height){ this.width = width; this.height = height;}

    public void setWidth(int width) {this.width =width;}
    public int getWidth() { return width;}

    public void setHeight(int height) {this.height =height;}
    public int getHeight() { return height;}


}
