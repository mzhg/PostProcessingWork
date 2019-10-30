package org.lwjgl.util.vector;

public class Transform {
    private float x,y,z;  // position
    private float scaleX = 1, scaleY = 1, scaleZ = 1;
    private float rotX, rotY, rotZ, rotW = 1;

    public void set(Transform ohs){
        x = ohs.x;
        y = ohs.y;
        z = ohs.z;
        scaleX = ohs.scaleX;
        scaleY = ohs.scaleY;
        scaleZ = ohs.scaleZ;
        rotX = ohs.rotX;
        rotY = ohs.rotY;
        rotZ = ohs.rotZ;
        rotW = ohs.rotW;
    }

    public void setIdentity(){
        x=y=z=0;
        scaleX = scaleY=scaleZ = 1;
        rotX = rotY = rotZ = 0;
        rotW = 1;
    }

    public void setPosition(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3f getPosition(Vector3f pos){
        if(pos != null){
            pos.set(x,y,z);
        }else{
            pos = new Vector3f(x,y,z);
        }

        return pos;
    }

    public float getPositionX() { return x;}
    public float getPositionY() { return y;}
    public float getPositionZ() { return z;}

    public float getScaleX() { return scaleX;}
    public float getScaleY() { return scaleY;}
    public float getScaleZ() { return scaleZ;}

    public void setScale(float scaleX, float scaleY, float scaleZ){
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }

    public void setRotation(float x, float y, float z, float w){
        rotX = x;
        rotY = y;
        rotZ = z;
        rotW = w;
    }

    public void setRotation(Quaternion quat){
        rotX = quat.x;
        rotY = quat.y;
        rotZ = quat.z;
        rotW = quat.w;
    }

    public Matrix4f getMatrix(Matrix4f mat){
        if(mat == null) mat = new Matrix4f();

        mat.setIdentity();
        mat.m30 = x;
        mat.m31 = y;
        mat.m32 = z;

        Quaternion.toMatrix4f(rotX,rotY,rotZ,rotW, mat);
        mat.scale(scaleX, scaleY, scaleZ);

        return mat;
    }
}
