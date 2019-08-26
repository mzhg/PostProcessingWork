package jet.opengl.postprocessing.util;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.util.Random;

public class BoundingSphere {
    public final Vector3f Center = new Vector3f();
    public float          Radius;

    public BoundingSphere( ) { }
    public BoundingSphere(ReadableVector3f center, float radius ) /*: Center(center), Radius( radius )*/ {
        Center.set(center);
        Radius = radius;
    }

    public Vector3f RandomPointOnSurface(Random randomGeneratorToUse /*= vaRandom::Singleton*/ ) {
//        return vaVector3::RandomNormal( ) * Radius + Center;
        Vector3f out = new Vector3f();
        float lengthSquared;
        do{
            if(randomGeneratorToUse != null){
                out.x = 2.0f * randomGeneratorToUse.nextFloat() - 1.0f;
                out.y = 2.0f * randomGeneratorToUse.nextFloat() - 1.0f;
                out.z = 2.0f * randomGeneratorToUse.nextFloat() - 1.0f;
            }else{
                out.x = Numeric.random(-1,+1);
                out.y = Numeric.random(-1,+1);
                out.z = Numeric.random(-1,+1);
            }

            lengthSquared = out.lengthSquared();
        }while (lengthSquared < Numeric.EPSILON);

        out.scale((float) (Radius/Math.sqrt(lengthSquared)));
        return Vector3f.add(Center, out, out);
    }
    public Vector3f RandomPointInside( Random randomGeneratorToUse /*= vaRandom::Singleton*/ ) {
//        return vaVector3::RandomNormal( ) * ( vaMath::Pow( randomGeneratorToUse.NextFloat( ), 1.0f / 3.0f ) * Radius ) + Center;
        Vector3f out = new Vector3f();
        float lengthSquared;
        do{
            if(randomGeneratorToUse != null){
                out.x = 2.0f * randomGeneratorToUse.nextFloat() - 1.0f;
                out.y = 2.0f * randomGeneratorToUse.nextFloat() - 1.0f;
                out.z = 2.0f * randomGeneratorToUse.nextFloat() - 1.0f;
            }else{
                out.x = Numeric.random(-1,+1);
                out.y = Numeric.random(-1,+1);
                out.z = Numeric.random(-1,+1);
            }

            lengthSquared = out.lengthSquared();
        }while (lengthSquared < Numeric.EPSILON);

        double scale = Math.pow(randomGeneratorToUse != null ? randomGeneratorToUse.nextDouble() : Math.random(), 1.0/3.0);
        out.scale((float) (scale * Radius/Math.sqrt(lengthSquared)));
        return Vector3f.add(Center, out, out);
    }
}
