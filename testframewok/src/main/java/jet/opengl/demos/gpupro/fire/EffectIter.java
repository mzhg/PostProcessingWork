package jet.opengl.demos.gpupro.fire;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

final class EffectIter {
    EffectSequence d_seq;   // reference to the collection of frames

    int d_counter;	// holds reference number of the current frame
    int d_angle;	// holds current angle for rotation of the volume effect
    // see Fig.5 in VFA document
    int d_angleBoost; // angle used to update d_angle
    // after each cycle of iterations
    int d_threshold = -1; // number of the last frame in the sequence

    public EffectIter(EffectSequence seq) {
        d_seq = seq;
    }

    public float getAngle() { return d_angle; }

    void passData(){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glDrawArrays(GLenum.GL_POINTS,d_seq.d_skip_vert[d_counter],
            d_seq.d_no_of_vert[d_counter]);
    }

    void incr(){
        if(d_counter==d_threshold) {
            d_counter = 0;
            d_angle += d_angleBoost;
            d_angle = d_angle%360;
        }
        else d_counter++;
    }

    void decr(){
        if(d_counter==0) {
            d_counter = d_threshold;
            d_angle -= d_angleBoost;
            d_angle = d_angle%360;
        }
        else d_counter--;
    }

    boolean init( int shift, int angle_shift, int starting_angle){
        d_threshold = d_seq.d_no_of_frames-1;
        if(d_threshold<0){
            System.err.println("initialize collection for this iterator first");
            return false;
        }

        d_angleBoost=angle_shift;
        if(shift>d_seq.d_no_of_frames) {
            System.err.println("shift bigger than no of frames");
            return false;
        }
        if(shift<0) {
            System.err.println("shift below zero");
            return false;
        }
        if(shift==0) d_counter = 0;
        else d_counter = d_seq.d_no_of_frames - shift;
        d_angle = starting_angle;
        return true;
    }
}
