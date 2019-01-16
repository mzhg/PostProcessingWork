package jet.opengl.demos.nvidia.waves.packets;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

final class WAVE_PACKET {
    static final int SIZE = Vector4f.SIZE * 14;
    // positions, directions, speed of the tracked vertices
    final Vector2f    pos1,pos2,pos3;				// 2D position
    final Vector2f	dir1,dir2,dir3;				// current movement direction
    float		speed1,speed2,speed3;		// speed of the particle
    final Vector2f	pOld1,pOld2,pOld3;			// position in last timestep (needed to handle bouncing)
    final Vector2f	dOld1,dOld2,dOld3;			// direction in last timestep (needed to handle bouncing)
    float		sOld1,sOld2,sOld3;			// speed in last timestep (needed to handle bouncing)
    final Vector2f	midPos;						// middle position (tracked each timestep, used for rendering)
    final Vector2f	travelDir;					// travel direction (tracked each timestep, used for rendering)
    float		bending;					// point used for circular arc bending of the wave function inside envelope

    // bouncing and sliding
    boolean		bounced1, bounced2, bounced3;	// indicates if this vertex bounced in this timestep
    boolean		sliding3;					// indicates if the 3rd vertex is "sliding" (used for diffraction)
    boolean		use3rd;						// indicates if the third vertex is present (it marks a (potential) sliding point)
    // wave function related
    float		phase;						// phase of the representative wave inside the envelope, phase speed vs. group speed
    float		phOld;						// old phase
    float		E;							// wave energy flux for this packet (determines amplitude)
    float		envelope;					// envelope size for this packet
    float		k,w0;						// w0 = angular frequency, k = current wavenumber
    float		k_L,w0_L,k_H,w0_H;			// w0 = angular frequency, k = current wavenumber,  L/H are for lower/upper boundary
    float		d_L,d_H;					// d = travel distance to reference wave (gets accumulated over time),  L/H are for lower/upper boundary
    float		ampOld;						// amplitude from last timestep, will be smoothly adjusted in each timestep to meet current desired amplitude
    float		dAmp;						// amplitude change in each timestep (depends on desired waveheight so all waves (dis)appear with same speed)
    // serial deletion step variable
    boolean		toDelete;					// used internally for parallel deletion criterion computation

    WAVE_PACKET() {
        pos1 = new Vector2f();
        pos2 = new Vector2f();
        pos3 = new Vector2f();

        dir1 = new Vector2f();
        dir2 = new Vector2f();
        dir3 = new Vector2f();

        pOld1 = new Vector2f();
        pOld2 = new Vector2f();
        pOld3 = new Vector2f();

        dOld1 = new Vector2f();
        dOld2 = new Vector2f();
        dOld3 = new Vector2f();

        midPos = new Vector2f();
        travelDir = new Vector2f();
    }
}
