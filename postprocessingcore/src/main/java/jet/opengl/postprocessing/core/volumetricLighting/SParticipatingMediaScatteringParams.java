package jet.opengl.postprocessing.core.volumetricLighting;

import org.lwjgl.util.vector.Vector4f;

final class SParticipatingMediaScatteringParams{

	// Atmospheric light scattering constants
    final Vector4f f4TotalRayleighBeta = new Vector4f();
    final Vector4f f4AngularRayleighBeta = new Vector4f();
    final Vector4f f4TotalMieBeta = new Vector4f();
    final Vector4f f4AngularMieBeta = new Vector4f();
 // = float4(1 - HG_g*HG_g, 1 + HG_g*HG_g, -2*HG_g, 1.0);
    final Vector4f f4HG_g = new Vector4f(); 
    final Vector4f f4SummTotalBeta = new Vector4f();
}
