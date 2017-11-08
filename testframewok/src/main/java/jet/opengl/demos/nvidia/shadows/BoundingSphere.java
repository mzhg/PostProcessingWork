package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.util.List;

/**
 * Created by mazhen'gui on 2017/11/8.
 */

final class BoundingSphere {
    final Vector3f centerVec = new Vector3f();
    float       radius;

    BoundingSphere(){}

    void set(BoundingSphere ohs){
        centerVec.set(ohs.centerVec);
        radius = ohs.radius;
    }

    BoundingSphere(List<Vector3f> points){
        centerVec.set(points.get(0));
        radius = 0;

        Vector3f cVec = new Vector3f();
        for(int i = 1; i < points.size(); i++){
            ReadableVector3f tmp = points.get(i);
//            D3DXVECTOR3 cVec = tmp - centerVec;
            Vector3f.sub(tmp, centerVec, cVec);
            float d = /*D3DXVec3Dot( &cVec, &cVec )*/cVec.lengthSquared();
            if ( d > radius*radius )
            {
                d = (float) Math.sqrt(d);
                float r = 0.5f * (d+radius);
                float scale = (r-radius) / d;
//                centerVec = centerVec + scale*cVec;
                Vector3f.linear(centerVec, cVec, scale, centerVec);
                radius = r;
            }
        }
    }

    BoundingSphere( BoundingBox box )
    {
        /*D3DXVECTOR3 radiusVec;
        centerVec = 0.5f * (box->maxPt + box->minPt);
        radiusVec = box->maxPt - centerVec;
        float len = D3DXVec3Length(&radiusVec);
        radius = len;*/
        box.center(centerVec);
        radius = Vector3f.distance(centerVec, box._max);
    }
}
