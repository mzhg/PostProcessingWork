#include "AtmosphereCommon.glsl"

layout(location = 0) out vec3 delta_rayleigh;
layout(location = 1) out vec3 delta_mie;
layout(location = 2) out vec4 scattering;
layout(location = 3) out vec3 single_mie_scattering;

uniform mat3 luminance_from_radiance;
uniform int layer;

/*
<p>where <code>ray_r_mu_intersects_ground</code> should be true iif the ray
defined by $r$ and $\mu$ intersects the ground. We don't compute it here with
<code>RayIntersectsGround</code> because the result could be wrong for rays
very close to the horizon, due to the finite precision and rounding errors of
floating point operations. And also because the caller generally has more robust
ways to know whether a ray intersects the ground or not (see below).

<p>Finally, we will also need the transmittance between a point in the
atmosphere and the Sun. The Sun is not a point light source, so this is an
integral of the transmittance over the Sun disc. Here we consider that the
transmittance is constant over this disc, except below the horizon, where the
transmittance is 0. As a consequence, the transmittance to the Sun can be
computed with <code>GetTransmittanceToTopAtmosphereBoundary</code>, times the
fraction of the Sun disc which is above the horizon.

<p>This fraction varies from 0 when the Sun zenith angle $\theta_s$ is larger
than the horizon zenith angle $\theta_h$ plus the Sun angular radius $\alpha_s$,
to 1 when $\theta_s$ is smaller than $\theta_h-\alpha_s$. Equivalently, it
varies from 0 when $\mu_s=\cos\theta_s$ is smaller than
$\cos(\theta_h+\alpha_s)\approx\cos\theta_h-\alpha_s\sin\theta_h$ to 1 when
$\mu_s$ is larger than
$\cos(\theta_h-\alpha_s)\approx\cos\theta_h+\alpha_s\sin\theta_h$. In between,
the visible Sun disc fraction varies approximately like a smoothstep (this can
be verified by plotting the area of <a
href="https://en.wikipedia.org/wiki/Circular_segment">circular segment</a> as a
function of its <a href="https://en.wikipedia.org/wiki/Sagitta_(geometry)"
>sagitta</a>). Therefore, since $\sin\theta_h=r_{\mathrm{bottom}}/r$, we can
approximate the transmittance to the Sun with the following function:
*/
vec3 GetTransmittanceToSun( in AtmosphereParameters atmosphere, float r, float mu_s)
{
    float sin_theta_h = atmosphere.bottom_radius / r;
    float cos_theta_h = -sqrt(max(1.0 - sin_theta_h * sin_theta_h, 0.0));
    return GetTransmittanceToTopAtmosphereBoundary(atmosphere, r, mu_s) *
        smoothstep(-sin_theta_h * atmosphere.sun_angular_radius / rad, sin_theta_h * atmosphere.sun_angular_radius / rad, mu_s - cos_theta_h);
}

/*
<h3 id="single_scattering">Single scattering</h3>

<p>The single scattered radiance is the light arriving from the Sun at some
point after exactly one scattering event inside the atmosphere (which can be due
to air molecules or aerosol particles; we exclude reflections from the ground,
computed <a href="#irradiance">separately</a>). The following sections describe
how we compute it, how we store it in a precomputed texture, and how we read it
back.

<h4 id="single_scattering_computation">Computation</h4>

<p>Consider the Sun light scattered at a point $\bq$ by air molecules before
arriving at another point $\bp$ (for aerosols, replace "Rayleigh" with "Mie"
below):

<svg height="190px" width="340px">
  <style type="text/css"><![CDATA[
    circle { fill: #000000; stroke: none; }
    path { fill: none; stroke: #000000; }
    text { font-size: 16px; font-style: normal; font-family: Sans; }
    .vector { font-weight: bold; }
  ]]></style>
  <path d="m 0,66 a 600,600 0 0 1 340,0"/>
  <path d="m 0,150 a 520,520 0 0 1 340,0"/>
  <path d="m 170,180 0,-165"/>
  <path d="m 250,180 30,-165"/>
  <path d="m 170,90 -30,-60"/>
  <path d="m 155,70 0,-10 8,6" />
  <path d="m 270,70 -20,-40" style="stroke-width:2;"/>
  <path d="m 170,90 100,-20" style="stroke-width:2;"/>
  <path d="m 270,70 75,-15" />
  <path d="m 170,65 a 25,25 0 0 1 25,20" style="stroke-dasharray:4,2;"/>
  <path d="m 170,30 a 60,60 1 0 0 -26.8,6.3" style="stroke-dasharray:4,2;"/>
  <path d="m 255,40 a 35,35 0 0 1 21,-3.2" style="stroke-dasharray:4,2;"/>
  <path d="m 258,45 a 30,30 0 0 1 41,19" style="stroke-dasharray:4,2;"/>
  <circle cx="170" cy="90" r="2.5"/>
  <circle cx="270" cy="70" r="2.5"/>
  <text x="155" y="105" class="vector">p</text>
  <text x="275" y="85" class="vector">q</text>
  <text x="130" y="70" class="vector">ω<tspan
      dy="2" style="font-size:10px;font-weight:normal;">s</tspan></text>
  <text x="155" y="164">r</text>
  <text x="265" y="165">r<tspan dy="2" style="font-size:10px">d</tspan></text>
  <text x="220" y="95">d</text>
  <text x="190" y="65">μ</text>
  <text x="145" y="25">μ<tspan dy="2" style="font-size:10px">s</tspan></text>
  <text x="290" y="45">ν</text>
  <text x="250" y="20">μ<tspan dy="2" style="font-size:10px">s,d</tspan></text>
</svg>

<p>The radiance arriving at $\bp$ is the product of:
<ul>
<li>the solar irradiance at the top of the atmosphere,</li>
<li>the transmittance between the Sun and $\bq$ (i.e. the fraction of the Sun
light at the top of the atmosphere that reaches $\bq$),</li>
<li>the Rayleigh scattering coefficient at $\bq$ (i.e. the fraction of the
light arriving at $\bq$ which is scattered, in any direction),</li>
<li>the Rayleigh phase function (i.e. the fraction of the scattered light at
$\bq$ which is actually scattered towards $\bp$),</li>
<li>the transmittance between $\bq$ and $\bp$ (i.e. the fraction of the light
scattered at $\bq$ towards $\bp$ that reaches $\bp$).</li>
</ul>

<p>Thus, by noting $\bw_s$ the unit direction vector towards the Sun, and with
the following definitions:
<ul>
<li>$r=\Vert\bo\bp\Vert$,</li>
<li>$d=\Vert\bp\bq\Vert$,</li>
<li>$\mu=(\bo\bp\cdot\bp\bq)/rd$,</li>
<li>$\mu_s=(\bo\bp\cdot\bw_s)/r$,</li>
<li>$\nu=(\bp\bq\cdot\bw_s)/d$</li>
</ul>
the values of $r$ and $\mu_s$ for $\bq$ are
<ul>
<li>$r_d=\Vert\bo\bq\Vert=\sqrt{d^2+2r\mu d +r^2}$,</li>
<li>$\mu_{s,d}=(\bo\bq\cdot\bw_s)/r_d=((\bo\bp+\bp\bq)\cdot\bw_s)/r_d=
(r\mu_s + d\nu)/r_d$</li>
</ul>
and the Rayleigh and Mie single scattering components can be computed as follows
(note that we omit the solar irradiance and the phase function terms, as well as
the scattering coefficients at the bottom of the atmosphere - we add them later
on for efficiency reasons):
*/
void ComputeSingleScatteringIntegrand(in AtmosphereParameters atmosphere, float r, float mu, float mu_s, float nu, float d, bool ray_r_mu_intersects_ground,
out vec3 rayleigh, out vec3 mie)
{
    float r_d = ClampRadius(atmosphere, sqrt(d * d + 2.0 * r * mu * d + r * r));
    float mu_s_d = ClampCosine((r * mu_s + d * nu) / r_d);
    vec3 transmittance = GetTransmittance(atmosphere, r, mu, d, ray_r_mu_intersects_ground) * GetTransmittanceToSun(atmosphere, r_d, mu_s_d);
    rayleigh = transmittance * GetProfileDensity(atmosphere.rayleigh_density, r_d - atmosphere.bottom_radius);
    mie = transmittance * GetProfileDensity(atmosphere.mie_density, r_d - atmosphere.bottom_radius);
}

/**
The single scattering integral can then be computed as follows (using
the <a href="https://en.wikipedia.org/wiki/Trapezoidal_rule">trapezoidal
rule</a>):
*/
void ComputeSingleScattering( in AtmosphereParameters atmosphere, float r, float mu, float mu_s, float nu, bool ray_r_mu_intersects_ground, out vec3 rayleigh, out vec3 mie)
{
    assert(r >= atmosphere.bottom_radius && r <= atmosphere.top_radius);
    assert(mu >= -1.0 && mu <= 1.0);
    assert(mu_s >= -1.0 && mu_s <= 1.0);
    assert(nu >= -1.0 && nu <= 1.0);
    const int SAMPLE_COUNT = 50;
    float dx = DistanceToNearestAtmosphereBoundary(atmosphere, r, mu, ray_r_mu_intersects_ground) / float(SAMPLE_COUNT);
    vec3 rayleigh_sum = vec3(0.0);
    vec3 mie_sum = vec3(0.0);
    for (int i = 0; i <= SAMPLE_COUNT; ++i)
    {
        float d_i = float(i) * dx;
        vec3 rayleigh_i;
        vec3 mie_i;
        ComputeSingleScatteringIntegrand(atmosphere, r, mu, mu_s, nu, d_i, ray_r_mu_intersects_ground, rayleigh_i, mie_i);
        float weight_i = (i == 0 || i == SAMPLE_COUNT) ? 0.5 : 1.0;
        rayleigh_sum += rayleigh_i * weight_i;
        mie_sum += mie_i * weight_i;
    }
    rayleigh = rayleigh_sum * dx * atmosphere.solar_irradiance * atmosphere.rayleigh_scattering;
    mie = mie_sum * dx * atmosphere.solar_irradiance * atmosphere.mie_scattering;
}

/**
With this mapping, we can finally write a function to precompute a texel of the single scattering in a 3D texture:
*/
void ComputeSingleScatteringTexture(in AtmosphereParameters atmosphere,in vec3 frag_coord, out vec3 rayleigh, out vec3 mie)
{
    float r;
    float mu;
    float mu_s;
    float nu;
    bool ray_r_mu_intersects_ground;
    GetRMuMuSNuFromScatteringTextureFragCoord(atmosphere, frag_coord, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    ComputeSingleScattering(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground, rayleigh, mie);
}

void main()
{
    ComputeSingleScatteringTexture(ATMOSPHERE, vec3(gl_FragCoord.xy, layer + 0.5), delta_rayleigh, delta_mie);
    scattering = vec4(delta_rayleigh.rgb * luminance_from_radiance, (delta_mie * luminance_from_radiance).r);
    single_mie_scattering =  delta_mie * luminance_from_radiance;
}