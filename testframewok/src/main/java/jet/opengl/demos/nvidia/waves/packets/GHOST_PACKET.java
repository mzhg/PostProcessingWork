package jet.opengl.demos.nvidia.waves.packets;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.shader.ShaderType;

final class GHOST_PACKET {
    static final int SIZE = Vector4f.SIZE * 3;
    final Vector2f pos = new Vector2f();					// 2D position
    final Vector2f dir = new Vector2f();					// current movement direction
    float		speed;					// speed of the packet
    float		envelope;				// envelope size for this packet
    float		bending;				// point used for circular arc bending of the wave function inside envelope
    float		k;						// k = current (representative) wavenumber(s)
    float		phase;					// phase of the representative wave inside the envelope
    float		dPhase;					// phase speed relative to group speed inside the envelope
    float		ampOld;					// amplitude from last timestep, will be smoothly adjusted in each timestep to meet current desired amplitude
    float		dAmp;					// change in amplitude in each timestep (waves travel PACKET_BLEND_TRAVEL_FACTOR*envelopesize in space until they disappear)

}
