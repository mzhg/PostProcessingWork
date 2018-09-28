package jet.opengl.desktop.lwjgl;

import org.lwjgl.util.vector.Vector3f;

public class StuffTest {

    public static void main(String[] args){
        Vector3f v1 = new Vector3f(1,1,2);
        Vector3f v2 = new Vector3f(1,-1,1);

        Vector3f v = Vector3f.cross(v1, v2, null);
//        System.out.println(v);

        Vector3f normal = Vector3f.computeNormal(new Vector3f(1,0,0), new Vector3f(0,-2,0),new Vector3f(0,0,4), null);
        System.out.println(normal);
    }
}
