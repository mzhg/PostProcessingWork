package jet.opengl.desktop.lwjgl;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

//还可以构造射线与内塞尔曲线相交的仿射矩阵
public class AffineMatrix {

    static Matrix4f constructBaycenterMatrix(ReadableVector3f v0, ReadableVector3f v1, ReadableVector3f v2, Matrix4f out){
        if(out == null)
            out = new Matrix4f();

        out.setColumn(0, v0.getX(), v0.getY(), v0.getZ(), 1);
        out.setColumn(1, v1.getX(), v1.getY(), v1.getZ(), 1);
        out.setColumn(2, v2.getX(), v2.getY(), v2.getZ(), 1);
        out.setColumn(3, 1, 1, 1, 0);

        out.invert();

        return out;
    }

    static Matrix4f constructRayTraingleMatrix(ReadableVector3f v0, ReadableVector3f v1, ReadableVector3f v2,  ReadableVector3f d, Matrix4f out){
        if(out == null)
            out = new Matrix4f();

        out.setColumn(0, v0.getX(), v0.getY(), v0.getZ(), 1);
        out.setColumn(1, v1.getX(), v1.getY(), v1.getZ(), 1);
        out.setColumn(2, v2.getX(), v2.getY(), v2.getZ(), 1);
        out.setColumn(3, -d.getX(), -d.getY(), -d.getZ(), 0);

        out.invert();
        return out;
    }

    public static void main(String[] args) {
        Vector3f v0 = new Vector3f(10, 100, 25);
        Vector3f v1 = new Vector3f(-10, 257, 36);
        Vector3f v2 = new Vector3f(-17, 15, -35);

        Vector3f d = new Vector3f(1,1,1);
        Vector4f o = new Vector4f(-20, 100, 15, 1);
        d.normalise();

        Vector4f point = new Vector4f();

        {
            Matrix4f baycenterM = constructBaycenterMatrix(v0, v1, v2, null);

            Vector3f baycenter = new Vector3f(0.2f, 0.3f, 0.5f);
            point.x = baycenter.x * v0.x + baycenter.y * v1.x + baycenter.z * v2.x;
            point.y = baycenter.x * v0.y + baycenter.y * v1.y + baycenter.z * v2.y;
            point.z = baycenter.x * v0.z + baycenter.y * v1.z + baycenter.z * v2.z;
            point.w = 1;

            Vector4f bc = Matrix4f.transform(baycenterM, point, null);
            System.out.println("Recover baycenter: " + bc);
            System.out.println();
        }

        {
            v0.z = v1.z = v2.z = 0;
            Matrix4f rayTriangleM = constructRayTraingleMatrix(v0, v1, v2, d,null);

            Vector4f tc = Matrix4f.transform(rayTriangleM, o, null);
            System.out.println("Recover rayTriangle baycenter and t: " + tc);

            point.x = tc.x * v0.x + tc.y * v1.x + tc.z * v2.x;
            point.y = tc.x * v0.y + tc.y * v1.y + tc.z * v2.y;
            point.z = tc.x * v0.z + tc.y * v1.z + tc.z * v2.z;

            System.out.println("Recover Collision Point by baycenter: " + point);
            System.out.println("Recover Collision Point by ray: " + Vector3f.linear(o, d, tc.w, null));
            System.out.println("The sum of the baycenter of Recover Collision Point: " + (tc.x +tc.y+tc.z));
        }

    }
}
