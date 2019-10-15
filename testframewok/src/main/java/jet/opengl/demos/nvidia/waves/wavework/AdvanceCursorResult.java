package jet.opengl.demos.nvidia.waves.wavework;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

enum AdvanceCursorResult {
    AdvanceCursorResult_Failed,	        // Something bad happened
    AdvanceCursorResult_Succeeded,	    // The cursor was advanced
    AdvanceCursorResult_None,			// The cursor was not advanced because there were no kicks in-flight
    AdvanceCursorResult_WouldBlock		// The cursor was not advanced because although there was a kick in-flight,
    // the function was called in non-blocking mode and the in-flight kick is not
    // yet ready
}
