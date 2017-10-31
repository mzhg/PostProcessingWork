package jet.opengl.demos.gpupro.fire;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayDeque;
import java.util.Iterator;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

class HeatHaze {
    static float timePassed = 0;

    ArrayDeque<Cloud> d_clouds_p = new ArrayDeque<>();

    float d_timeScale;					// time multiplier
    float d_timeGapClouds;				// rate for heat clouds emission
    float d_halfSizeInit;				// initial halfsize of heat clouds
    float d_halfSizeAmpl;				// rate of change for halfsize
    float d_initHorizSpiralRadius;		// initial radius of spiralling movement of heat clouds
    float d_spiralParameter;			// parameter of spiralling movement of heat clouds
    float d_spiralSpeedAmpl;			// maximal speed of spiralling movement within horizontal plane
    float d_vertSpeedInit;				// initial vertical speed of heat clouds
    float d_vertSpeedAmpl;				// rate of change for vertical speed
    float d_turbulenceSpeed;
    float d_lifetimeInit;				// initial lifetime for single heat cloud
    float d_lifetimeAmpl;				// rate of change for lifetime
    boolean d_visible;						// indicates if the volume object is in front of the camera
    final Matrix4f d_CloudFacePerpVisDirRot = new Matrix4f();

    final Vector3f d_HeatHazeLocation = new Vector3f();

    boolean init(float timeScale, float timeGapClouds, float halfSizeInit, float halfSizeAmpl,
                 float initHorizSpiralRadius, float spiralParameter, float spiralSpeedAmpl, float vertSpeedInit,
                 float vertSpeedAmpl, float turbulenceSpeed, float lifetimeAmpl, Vector3f r_f, Vector3f r_c, Vector3f d_c, Vector3f u_c){
        d_timeScale = timeScale;
        d_timeGapClouds = timeGapClouds;
        d_halfSizeInit=halfSizeInit;
        d_halfSizeAmpl=halfSizeAmpl;
        d_initHorizSpiralRadius = initHorizSpiralRadius;
        d_spiralParameter = spiralParameter;
        d_spiralSpeedAmpl = spiralSpeedAmpl;
        d_vertSpeedInit=vertSpeedInit;
        d_vertSpeedAmpl=vertSpeedAmpl;
        d_turbulenceSpeed = turbulenceSpeed;
        d_lifetimeAmpl=lifetimeAmpl;

        d_lifetimeInit = 1.0f;
//        for(int i=0;i<16;i++){
//            if(( i%5 )!=0)
//                d_CloudFacePerpVisDirRot[i] = 0.0f;
//            else d_CloudFacePerpVisDirRot[i] = 1.0f;
//        }
        update(r_f, r_c, d_c, u_c);
        return true;
    }

    void update(Vector3f r_f, Vector3f r_c, Vector3f d_c, Vector3f u_c){
        d_visible = true;

        d_HeatHazeLocation.set(r_f);

//			CVector3 r_cf = r_c - r_f;
        Vector3f r_cf = Vector3f.sub(r_c, r_f, null);

        float r_cf_d_c = Vector3f.dot(r_cf,d_c);

//			float len_r_cf2 = Vector3f.dot(r_cf,r_cf);
//			float len_r_cf = (float) Math.sqrt(len_r_cf2);

        if( r_cf_d_c > 0.0f) d_visible = false;
        else {
            Vector3f v_Z = d_c; //new Vector3f( d_c[0], d_c[1], d_c[2]);

            Vector3f v_X = new Vector3f( u_c.y * v_Z.z - u_c.z * v_Z.y , u_c.z * v_Z.x - u_c.x * v_Z.z , u_c.x * v_Z.y - u_c.y * v_Z.x );
            float temp = (float) Math.sqrt(Vector3f.dot(v_X,v_X));
            v_X.x/=temp;
            v_X.y/=temp;
            v_X.z/=temp;

            d_CloudFacePerpVisDirRot.m00 = v_X.x;
            d_CloudFacePerpVisDirRot.m01 = v_X.y;
            d_CloudFacePerpVisDirRot.m02 = v_X.z;

            d_CloudFacePerpVisDirRot.m10 = v_Z.y * v_X.z - v_Z.z * v_X.y;
            d_CloudFacePerpVisDirRot.m11 = v_Z.z * v_X.x - v_Z.x * v_X.z;
            d_CloudFacePerpVisDirRot.m12 = v_Z.x * v_X.y - v_Z.y * v_X.x;

            d_CloudFacePerpVisDirRot.m20 = v_Z.x;
            d_CloudFacePerpVisDirRot.m21 = v_Z.y;
            d_CloudFacePerpVisDirRot.m22 = v_Z.z;
        }
    }

    void proceed(float time){
        time*=d_timeScale;

//			d_cloudsIter = d_clouds_p.begin();
        Iterator<Cloud> it = d_clouds_p.iterator();
        while(it.hasNext() /*d_cloudsIter < d_clouds_p.end()*/){
            Cloud cloud = it.next();
            cloud.d_halfSize = d_halfSizeAmpl*time/cloud.d_halfSize + cloud.d_halfSize;

            cloud.d_lifetime_left += d_lifetimeAmpl*time;

            if( cloud.d_lifetime_left < 0 ){
//					++d_cloudsIter;
//					d_clouds_p.pop_front();
                it.remove();
                continue;
            }

            cloud.d_vert_speed += d_vertSpeedAmpl*time;

            cloud.d_position[0] += ( d_spiralParameter * cloud.d_position[0] - cloud.d_horiz_spiral_speed * cloud.d_position[2] ) * time;
            cloud.d_position[1] += cloud.d_vert_speed*time;
            cloud.d_position[2] += ( d_spiralParameter * cloud.d_position[2] + cloud.d_horiz_spiral_speed * cloud.d_position[0] ) * time;

            cloud.d_rotation += cloud.d_rotation_speed*time;
//				++d_cloudsIter;
        }

        timePassed += time;
        int no_of_new = (int) (timePassed/d_timeGapClouds);
        float rest = 0;
        if( no_of_new>0 ){
            rest = timePassed - no_of_new*d_timeGapClouds;
            timePassed = 0;
        }
        float time_shift;
        float ran_angl;
        for(int i=0;i<no_of_new;i++){
            time_shift = i*d_timeGapClouds + rest;
            ran_angl = 6.28318531f * Numeric.random();

            float x = (float) (d_initHorizSpiralRadius*Math.cos(ran_angl));
            float z = (float) (d_initHorizSpiralRadius*Math.sin(ran_angl));
            float temp_speed = d_vertSpeedInit + d_vertSpeedAmpl*time_shift;
            float temp_spiral_speed = d_spiralSpeedAmpl * /*(int)(rand()/RAND_MAX + 0.5)*/ (Numeric.random() + 0.5f);  // TODO

            d_clouds_p.push(
                    new Cloud(
                            x + ( d_spiralParameter * x - temp_spiral_speed * z ) * time_shift,
                            temp_speed*time_shift,
                            z + ( d_spiralParameter * z + temp_spiral_speed * x ) * time_shift,
                            temp_spiral_speed,
                            temp_speed,
                            360.0f*Numeric.random(),
                            d_turbulenceSpeed*(Numeric.random()-0.5f),
                            d_halfSizeAmpl*time_shift/d_halfSizeInit + d_halfSizeInit,
                            d_lifetimeInit + d_lifetimeAmpl*time_shift)

            );
        }
    }
}
