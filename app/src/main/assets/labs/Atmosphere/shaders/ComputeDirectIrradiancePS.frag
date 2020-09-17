#include "AtmosphereCommon.glsl"

layout(location = 0) out vec3 delta_irradiance;
layout(location = 1) out vec3 irradiance;

const vec2 IRRADIANCE_TEXTURE_SIZE = vec2(IRRADIANCE_TEXTURE_WIDTH, IRRADIANCE_TEXTURE_HEIGHT);

/*
<h4 id="multiple_scattering_lookup">Lookup</h4>

<p>Likewise, we can simply reuse the lookup function <code>GetScattering</code>
implemented for single scattering to read a value from the precomputed textures
for multiple scattering. In fact, this is what we did above in the
<code>ComputeScatteringDensity</code> and <code>ComputeMultipleScattering</code>
functions.

<h3 id="irradiance">Ground irradiance</h3>

<p>The ground irradiance is the Sun light received on the ground after $n \ge 0$
bounces (where a bounce is either a scattering event or a reflection on the
ground). We need this for two purposes:
<ul>
<li>while precomputing the $n$-th order of scattering, with $n \ge 2$, in order
to compute the contribution of light paths whose $(n-1)$-th bounce is on the
ground (which requires the ground irradiance after $n-2$ bounces - see the
<a href="#multiple_scattering_computation">Multiple scattering</a>
section),</li>
<li>at rendering time, to compute the contribution of light paths whose last
bounce is on the ground (these paths are excluded, by definition, from our
precomputed scattering textures)</li>
</ul>

<p>In the first case we only need the ground irradiance for horizontal surfaces
at the bottom of the atmosphere (during precomputations we assume a perfectly
spherical ground with a uniform albedo). In the second case, however, we need
the ground irradiance for any altitude and any surface normal, and we want to
precompute it for efficiency. In fact, as described in our
<a href="https://hal.inria.fr/inria-00288758/en">paper</a> we precompute it only
for horizontal surfaces, at any altitude (which requires only 2D textures,
instead of 4D textures for the general case), and we use approximations for
non-horizontal surfaces.

<p>The following sections describe how we compute the ground irradiance, how we
store it in a precomputed texture, and how we read it back.

<h4 id="irradiance_computation">Computation</h4>

<p>The ground irradiance computation is different for the direct irradiance,
i.e. the light received directly from the Sun, without any intermediate bounce,
and for the indirect irradiance (at least one bounce). We start here with the
direct irradiance.

<p>The irradiance is the integral over an hemisphere of the incident radiance,
times a cosine factor. For the direct ground irradiance, the incident radiance
is the Sun radiance at the top of the atmosphere, times the transmittance
through the atmosphere. And, since the Sun solid angle is small, we can
approximate the transmittance with a constant, i.e. we can move it outside the
irradiance integral, which can be performed over (the visible fraction of) the
Sun disc rather than the hemisphere. Then the integral becomes equivalent to the
ambient occlusion due to a sphere, also called a view factor, which is given in
<a href="http://webserver.dmt.upm.es/~isidoro/tc3/Radiation%20View%20factors.pdf
">Radiative view factors</a> (page 10). For a small solid angle, these complex
equations can be simplified as follows:
*/
vec3 ComputeDirectIrradiance(in AtmosphereParameters atmosphere, float r, float mu_s)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu_s >= -1.0 && mu_s <= 1.0);

    float alpha_s = atmosphere.sun_angular_radius / rad;
    float average_cosine_factor = mu_s < -alpha_s ? 0.0 : (mu_s > alpha_s ? mu_s : (mu_s + alpha_s) * (mu_s + alpha_s) / (4.0 * alpha_s));
    return atmosphere.solar_irradiance * GetTransmittanceToTopAtmosphereBoundary(atmosphere, r, mu_s) * average_cosine_factor;
}

vec3 ComputeDirectIrradianceTexture(in AtmosphereParameters atmosphere, in vec2 frag_coord)
{
    float r;
    float mu_s;
    GetRMuSFromIrradianceTextureUv(atmosphere, frag_coord / IRRADIANCE_TEXTURE_SIZE, r, mu_s);
    return ComputeDirectIrradiance(atmosphere, r, mu_s);
}

void main()
{
    delta_irradiance = ComputeDirectIrradianceTexture(ATMOSPHERE, gl_FragCoord.xy);
    irradiance = vec3(0.0);
}