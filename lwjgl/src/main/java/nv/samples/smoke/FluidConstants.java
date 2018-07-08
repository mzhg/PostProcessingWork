package nv.samples.smoke;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by Administrator on 2018/7/8 0008.
 */

final class FluidConstants {
    int         fluidType = Fluid.FT_SMOKE;
    boolean        advectAsTemperature = false;
    boolean        treatAsLiquidVelocity = false;

    int         drawTextureNumber = 1;

    float       textureWidth;
    float       textureHeight;
    float       textureDepth;

    float       liquidHeight = 24;

// NOTE: The spacing between simulation grid cells is \delta x  = 1, so it is omitted everywhere
    float       timestep                = 1.0f;
    float       decay                   = 1.0f; // this is the (1.0 - dissipation_rate). dissipation_rate >= 0 ==> decay <= 1
    float       rho                     = 1.2f; // rho = density of the fluid
    float       viscosity               = 5e-6f;// kinematic viscosity
    float       vortConfinementScale    = 0.0f; // this is typically a small value >= 0
    final Vector3f gravity              = new Vector3f();    // note this is assumed to be given as pre-multiplied by the timestep, so it's really velocity: cells per step
    float       temperatureLoss         = 0.003f;// a constant amount subtracted at every step when advecting a quatnity as tempterature

    float       radius;
    final Vector3f center = new Vector3f();
    final Vector4f color = new Vector4f();

    final Vector4f      obstBoxVelocity = new Vector4f(0, 0, 0, 0);
    final Vector3f      obstBoxLBDcorner = new Vector3f();
    final Vector3f      obstBoxRTUcorner = new Vector3f();

    //parameters for attenuating velocity based on porous obstacles.
//these values are not hooked into CPP code yet, and so this option is not used currently
    boolean        doVelocityAttenuation = false;
    float       maxDensityAmount = 0.7f;
    float       maxDensityDecay = 0.95f;
}
