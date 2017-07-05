package jet.opengl.demos.gpupro.cloud;

import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;

import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/7/4.
 */
//--------------------------------------------------------------------------------------
// SScatteringShaderParameters
//      Parameters for scattering in pixel shader
//
//   Scattering equation is
//      L(s,theta) = L0 * Fex(s) + Lin(s,theta).
//   where,
//      Fex(s) = exp( -s * (Br+Bm) )
//      Lin(s,theta) = (Esun * ((Br(theta)+Bm(theta))/(Br+Bm)) + ambient)* (1.0f - exp( -s * (Br+Bm) ))
//      Br(theta) = 3/(16*PI) * Br * (1+cos^2(theta))
//      Bm(theta) = 1/(4*PI) * Bm * ((1-g)^2/(1+g^2-2*g*cos(theta))^(3/2))
//
//   Distance light goes through the atomosphere in a certain ray is
//      Distance(phi) = -R*sin(phi) + sqrt( (R*sin(phi))^2 + h * (2*R+h) )
//   where,
//      R   : Earth radius
//      h   : atomosphere height
//      phi : angle between a ray vector and a horizontal plane.
//--------------------------------------------------------------------------------------
final class SScatteringShaderParameters {
    final Vector4f vRayleigh = new Vector4f();  // rgb : 3/(16*PI) * Br           w : -2*g
    final Vector4f vMie      = new Vector4f();  // rgb : 1/(4*PI) * Bm * (1-g)^2  w : (1+g^2)
    final Vector4f vESun     = new Vector4f();  // rgb : Esun/(Br+Bm)             w : R
    final Vector4f vSum      = new Vector4f();  // rgb : (Br+Bm)                  w : h(2R+h)
    final Vector4f vAmbient  = new Vector4f();  // rgb : ambient

    FloatBuffer toFloats(){
        FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(4 * 5);
        vRayleigh.store(buffer);
        vMie.store(buffer);
        vESun.store(buffer);
        vSum.store(buffer);
        vAmbient.store(buffer);

        buffer.flip();
        return buffer;
    }
}
