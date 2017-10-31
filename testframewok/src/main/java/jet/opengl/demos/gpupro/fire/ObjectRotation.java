package jet.opengl.demos.gpupro.fire;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

final class ObjectRotation {
    private int origx;
    private int origy;
    private final Quaternion q_orientation = new Quaternion(); // current rotation matrix in the form of a quaternion
    private final Matrix4f m_orientation = new Matrix4f();

    private double delta;
    private double sin_d, cos_d;
    private double sin_d1, cos_d1;

    private final Quaternion n_rotate = new Quaternion();
    private double[] rot_vec = new double[2];

    public ObjectRotation(float delta_angle) {
        delta = delta_angle;
        sin_d = Math.sin(delta);
        cos_d = Math.cos(delta);
        sin_d1 = Math.sin(delta/5.0);
        cos_d1 = Math.cos(delta/5.0);
    }

    void setRotationSpeed(float delta_angle){
        delta = delta_angle;
        sin_d = Math.sin(delta);
        cos_d = Math.cos(delta);
        sin_d1 = Math.sin(delta/5.0);
        cos_d1 = Math.cos(delta/5.0);
    }

    void reset(){
        m_orientation.setIdentity();
    }

    void rotate(int x, int y, boolean rotate, boolean control , Vector3f d_x, Vector3f d_y)
    {
        rot_vec[1] = (x-origx);
        rot_vec[0] = (y-origy);
        origx = x;
        origy = y;

        if (rotate) {
            double dl = rot_vec[0]*rot_vec[0] + rot_vec[1]*rot_vec[1];

            if(dl>0.0){
                dl=(float) Math.sqrt(dl);
                rot_vec[1] /= dl;
                rot_vec[0] /= dl;

                n_rotate.x = (float) (( d_x.x * rot_vec[0] + d_y.x * rot_vec[1] ) * sin_d);
                n_rotate.y = (float) (( d_x.y * rot_vec[0] + d_y.y * rot_vec[1] ) * sin_d);
                n_rotate.z = (float) (( d_x.z * rot_vec[0] + d_y.z * rot_vec[1] ) * sin_d);
                n_rotate.w = (float) cos_d;

//	    		q_orientation = n_rotate*q_orientation;
//                q_orientation.mul(n_rotate, q_orientation);
                Quaternion.mul(n_rotate, q_orientation, q_orientation);

//	    		QuaternionToMatrix(q_orientation,m_orientation);
                q_orientation.toMatrix(m_orientation);
            }

        }

        if(control){
//	    	CVector3 temp = Cross(d_x,d_y)*(-sin_d1*rot_vec[1]);
            Vector3f temp = Vector3f.cross(d_x, d_y, null);
            temp.scale((float) (-sin_d1*rot_vec[1]));

            n_rotate.x = temp.x;
            n_rotate.y = temp.y;
            n_rotate.z = temp.z;
            n_rotate.w = (float) cos_d1;


//	    	q_orientation = n_rotate*q_orientation;
//            q_orientation.mul(n_rotate, q_orientation);
            Quaternion.mul(n_rotate, q_orientation, q_orientation);

//	    	QuaternionToMatrix(q_orientation,m_orientation);
            q_orientation.toMatrix(m_orientation);
        }
    }

    void set_prev_position(int origx_,int origy_){
        origx	= origx_;
        origy = origy_;
    }
}
