package jet.opengl.demos.nvidia.shadows;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.BoundingBox;

/**
 * Created by mazhen'gui on 2017/11/8.
 */

final class BoundingCone {

    final Vector3f direction = new Vector3f(0.f, 0.f, 1.f);
    final Vector3f apex = new Vector3f(0.f, 0.f, 0.f);
    float       fovy = 0.0f;
    float       fovx = 0.0f;
    float       fNear = 0.001f;
    float       fFar = 1.f;
    final Matrix4f m_LookAt = new Matrix4f();

    BoundingCone(List<BoundingBox> boxes, Matrix4f projection, Vector3f _apex){
        apex.set(_apex);
        ReadableVector3f yAxis = Vector3f.Y_AXIS;
        ReadableVector3f zAxis = Vector3f.Z_AXIS;
        ReadableVector3f negZAxis = Vector3f.Z_AXIS_NEG;
        switch (boxes.size())
        {
            case 0:
            {
                direction.set(negZAxis);
                fovx = 0.f;
                fovy = 0.f;
                m_LookAt.setIdentity();
                break;
            }
            default:
            {
                int i, j;


                //  compute a tight bounding sphere for the vertices of the bounding boxes.
                //  the vector from the apex to the center of the sphere is the optimized view direction
                //  start by xforming all points to post-projective space
                /*std::vector<D3DXVECTOR3> ppPts;
                ppPts.reserve(boxes->size() * 8);*/
                ArrayList<Vector3f> ppPts = new ArrayList<>(boxes.size() * 8);

                for (i=0; i<boxes.size(); i++)
                {
                    for (j=0; j<8; j++)
                    {
                        /*D3DXVECTOR3 tmp = (*boxes)[i].Point(j);
                        D3DXVec3TransformCoord(&tmp, &tmp, projection);
                        ppPts.push_back(tmp);*/
                        Vector3f tmp = boxes.get(i).corner(j, (Vector3f) null);
                        Matrix4f.transformCoord(projection, tmp, tmp);
                        ppPts.add(tmp);
                    }
                }

                //  get minimum bounding sphere
                BoundingSphere bSphere = new BoundingSphere( ppPts );

                float min_cosTheta = 1.f;

                /*direction = bSphere.centerVec - apex;
                D3DXVec3Normalize(&direction, &direction);*/
                Vector3f.sub(bSphere.centerVec, apex, direction);
                direction.normalise();

                ReadableVector3f axis = yAxis;

                if ( Math.abs(Vector3f.dot(yAxis, direction)) > 0.99f )
                axis = zAxis;

//                D3DXMatrixLookAtLH(&m_LookAt, &apex, &(apex+direction), &axis);
                Matrix4f.lookAt(apex, Vector3f.add(apex, direction, null), axis, m_LookAt);

                fNear = 1e32f;
                fFar = 0.f;

                Vector3f tmp = new Vector3f();
                float maxx=0.f, maxy=0.f;
                for (i=0; i<ppPts.size(); i++)
                {
                    /*D3DXVECTOR3 tmp;
                    D3DXVec3TransformCoord(&tmp, &ppPts[i], &m_LookAt);*/
                    Matrix4f.transformVector(m_LookAt, ppPts.get(i), tmp);
                    maxx = Math.max(maxx, Math.abs(tmp.x / tmp.z));
                    maxy = Math.max(maxy, Math.abs(tmp.y / tmp.z));
                    fNear = Math.min(fNear, tmp.z);
                    fFar  = Math.max(fFar, tmp.z);
                }

                fovx = (float) Math.atan(maxx);
                fovy = (float) Math.atan(maxy);
                break;
            }
        } // switch
    }

    BoundingCone(List<BoundingBox> boxes, Matrix4f projection, Vector3f _apex, Vector3f _direction){

    }

    BoundingCone(List<Vector3f> points, Vector3f _apex, Vector3f _direction){

    }
}
