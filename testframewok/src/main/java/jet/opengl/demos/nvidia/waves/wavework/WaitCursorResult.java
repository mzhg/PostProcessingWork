package jet.opengl.demos.nvidia.waves.wavework;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

enum WaitCursorResult {
    WaitCursorResult_Failed,	    // Something bad happened
    WaitCursorResult_Succeeded,	    // The cursor is ready to advance
    WaitCursorResult_None			// The cursor is not ready to advance because there were no kicks in-flight
}
