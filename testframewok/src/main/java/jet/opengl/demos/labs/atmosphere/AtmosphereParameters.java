package jet.opengl.demos.labs.atmosphere;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

final class AtmosphereParameters implements Readable {
    static final int SIZE = Vector4f.SIZE * 8 + DensityProfile.SIZE * 3;

    final Vector3f solar_irradiance = new Vector3f();
    float sun_angular_radius;

    final Vector3f rayleigh_scattering = new Vector3f();
    float bottom_radius;

    final Vector3f mie_scattering = new Vector3f();
    float top_radius;

    final Vector3f mie_extinction = new Vector3f();
    float mie_phase_function_g;

    final Vector3f ground_albedo= new Vector3f();
    float mu_s_min;

    final DensityProfile rayleigh_density = new DensityProfile();

    final DensityProfile mie_density = new DensityProfile();

    final DensityProfile absorption_density = new DensityProfile();
    final Vector3f absorption_extinction = new Vector3f();
    float padding;

    final Vector3f SKY_SPECTRAL_RADIANCE_TO_LUMINANCE = new Vector3f();
    float p0;
    final Vector3f SUN_SPECTRAL_RADIANCE_TO_LUMINANCE = new Vector3f();
    float p1;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        solar_irradiance.store(buf);     buf.putFloat(sun_angular_radius);
        rayleigh_scattering.store(buf);  buf.putFloat(bottom_radius);
        mie_scattering.store(buf);       buf.putFloat(top_radius);
        mie_extinction.store(buf);       buf.putFloat(mie_phase_function_g);
        ground_albedo.store(buf);        buf.putFloat(mu_s_min);

        rayleigh_density.store(buf);
        mie_density.store(buf);
        absorption_density.store(buf);

        absorption_extinction.store(buf);   buf.putFloat(padding);
        SKY_SPECTRAL_RADIANCE_TO_LUMINANCE.store(buf);   buf.putFloat(p0);
        SUN_SPECTRAL_RADIANCE_TO_LUMINANCE.store(buf);   buf.putFloat(p1);
        return buf;
    }
}
