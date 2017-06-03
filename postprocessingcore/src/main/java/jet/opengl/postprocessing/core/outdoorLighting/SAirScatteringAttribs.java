package jet.opengl.postprocessing.core.outdoorLighting;

import org.lwjgl.util.vector.Vector4f;

final class SAirScatteringAttribs{

	// Angular Rayleigh scattering coefficient contains all the terms exepting 1 + cos^2(Theta):
    // Pi^2 * (n^2-1)^2 / (2*N) * (6+3*Pn)/(6-7*Pn)
    final Vector4f f4AngularRayleighSctrCoeff = new Vector4f();
    // Total Rayleigh scattering coefficient is the integral of angular scattering coefficient in all directions
    // and is the following:
    // 8 * Pi^3 * (n^2-1)^2 / (3*N) * (6+3*Pn)/(6-7*Pn)
    final Vector4f f4TotalRayleighSctrCoeff = new Vector4f();
    final Vector4f f4RayleighExtinctionCoeff = new Vector4f();

    // Note that angular scattering coefficient is essentially a phase function multiplied by the
    // total scattering coefficient
    final Vector4f f4AngularMieSctrCoeff = new Vector4f();
    final Vector4f f4TotalMieSctrCoeff   = new Vector4f();
    final Vector4f f4MieExtinctionCoeff  = new Vector4f();

    final Vector4f f4TotalExtinctionCoeff = new Vector4f();
    // Cornette-Shanks phase function (see Nishita et al. 93) normalized to unity has the following form:
    // F(theta) = 1/(4*PI) * 3*(1-g^2) / (2*(2+g^2)) * (1+cos^2(theta)) / (1 + g^2 - 2g*cos(theta))^(3/2)
    final Vector4f f4CS_g = new Vector4f(); // x == 3*(1-g^2) / (2*(2+g^2))    //7
                   							// y == 1 + g^2
                   							// z == -2*g
}
