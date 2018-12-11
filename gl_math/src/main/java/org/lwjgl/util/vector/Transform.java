package org.lwjgl.util.vector;

public class Transform {
    private float x,y,z;  // position
    private float scaleX = 1, scaleY = 1, scaleZ = 1;
    private float rotX, rotY, rotZ, rotW = 1;

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
