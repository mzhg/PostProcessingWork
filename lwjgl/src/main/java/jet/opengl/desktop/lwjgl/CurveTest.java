package jet.opengl.desktop.lwjgl;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Numeric;

public class CurveTest {

    public static void main(String[] a){
        Vector3f N = new Vector3f();
        N.x = Numeric.random(-1, -0.5f);
        N.y = Numeric.random(-1, -0.5f);
        N.z = Numeric.random(0, 1);
        N.normalise();

        float cosFeiMax = (float) Math.acos(N.z);
        if(cosFeiMax < 0)
            throw new IllegalArgumentException();


        float t = Numeric.random(0, 0.499999f);
        float theta = 2 * Numeric.PI * t;
        float fei = cosFeiMax + (Numeric.PI - 2 * cosFeiMax) * 2 * t;

        Vector3f v = new Vector3f();
        v.x = (float) (Math.sin(fei) * Math.cos(theta));
        v.y = (float) (Math.sin(fei) * Math.sin(theta));
        v.z = (float) Math.cos(fei);

        System.out.println("NdotV = " + Vector3f.dot(N,v));

        float thetaN = (float) Math.atan2(N.y,N.x);
        float tanFei = (float) Math.tan(fei);
        float C = (float) Math.sqrt((N.x*N.x+N.y*N.y)/(N.z*N.z)) * tanFei;
        theta = (float) (Math.acos(-1/C) + thetaN);


        v.x = (float) (Math.sin(fei) * Math.cos(theta));
        v.y = (float) (Math.sin(fei) * Math.sin(theta));
        v.z = (float) Math.cos(fei);

        System.out.println("NdotV = " + Vector3f.dot(N,v));
    }
}
