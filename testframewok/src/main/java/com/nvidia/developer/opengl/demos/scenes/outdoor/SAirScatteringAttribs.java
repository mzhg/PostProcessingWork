package com.nvidia.developer.opengl.demos.scenes.outdoor;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

final class SAirScatteringAttribs implements Readable, Writable{

	// Angular Rayleigh scattering coefficient contains all the terms exepting 1 + cos^2(Theta):
    // Pi^2 * (n^2-1)^2 / (2*N) * (6+3*Pn)/(6-7*Pn)
    final Vector4f f4AngularRayleighSctrCoeff = new Vector4f();   // 0
    // Total Rayleigh scattering coefficient is the integral of angular scattering coefficient in all directions
    // and is the following:
    // 8 * Pi^3 * (n^2-1)^2 / (3*N) * (6+3*Pn)/(6-7*Pn)
    final Vector4f f4TotalRayleighSctrCoeff = new Vector4f();     // 1
    final Vector4f f4RayleighExtinctionCoeff = new Vector4f();    // 2

    // Note that angular scattering coefficient is essentially a phase function multiplied by the
    // total scattering coefficient
    final Vector4f f4AngularMieSctrCoeff = new Vector4f();        // 3
    final Vector4f f4TotalMieSctrCoeff   = new Vector4f();        // 4
    final Vector4f f4MieExtinctionCoeff  = new Vector4f();        // 5

    final Vector4f f4TotalExtinctionCoeff = new Vector4f();       // 6
    // Cornette-Shanks phase function (see Nishita et al. 93) normalized to unity has the following form:
    // F(theta) = 1/(4*PI) * 3*(1-g^2) / (2*(2+g^2)) * (1+cos^2(theta)) / (1 + g^2 - 2g*cos(theta))^(3/2)
    final Vector4f f4CS_g = new Vector4f(); // x == 3*(1-g^2) / (2*(2+g^2))    //7
                   							// y == 1 + g^2
                   							// z == -2*g

    // Air molecules and aerosols are assumed to be distributed
    // between 6360 km and 6420 km
    /*final*/ float fEarthRadius = 6360000.f;
    /*final*/ float fAtmTopHeight = 80000.f;
    final Vector2f f2ParticleScaleHeight = new Vector2f(7994.f, 1200.f);  // 8
    
    /*final*/ float fTurbidity = 1.02f;
    /*final*/ float fAtmTopRadius = fEarthRadius + fAtmTopHeight;
    /*final*/ float m_fAerosolPhaseFuncG = 0.76f;
    
    public SAirScatteringAttribs() {}
	
	public SAirScatteringAttribs(SAirScatteringAttribs o) {
		set(o);
	}
	
	public void set(SAirScatteringAttribs o){
		f4AngularRayleighSctrCoeff.set(o.f4AngularRayleighSctrCoeff);
		f4TotalRayleighSctrCoeff.set(o.f4TotalRayleighSctrCoeff);
		f4RayleighExtinctionCoeff.set(o.f4RayleighExtinctionCoeff);
		f4AngularMieSctrCoeff.set(o.f4AngularMieSctrCoeff);
		f4TotalMieSctrCoeff.set(o.f4TotalMieSctrCoeff);
		f4MieExtinctionCoeff.set(o.f4MieExtinctionCoeff);
		f4TotalExtinctionCoeff.set(o.f4TotalExtinctionCoeff);
		f4CS_g.set(o.f4CS_g);
		
//		fEarthRadius = o.fEarthRadius;
//		fAtmTopHeight = o.fAtmTopHeight;
//		f2ParticleScaleHeight.set(o.f2ParticleScaleHeight);
//		fTurbidity = o.fTurbidity;
//		fAtmTopRadius = o.fAtmTopRadius;
//		m_fAerosolPhaseFuncG = o.m_fAerosolPhaseFuncG;

	}
	
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
//		result = prime * result + ((f2ParticleScaleHeight == null) ? 0 : f2ParticleScaleHeight.hashCode());
		result = prime * result + ((f4AngularMieSctrCoeff == null) ? 0 : f4AngularMieSctrCoeff.hashCode());
		result = prime * result + ((f4AngularRayleighSctrCoeff == null) ? 0 : f4AngularRayleighSctrCoeff.hashCode());
		result = prime * result + ((f4CS_g == null) ? 0 : f4CS_g.hashCode());
		result = prime * result + ((f4MieExtinctionCoeff == null) ? 0 : f4MieExtinctionCoeff.hashCode());
		result = prime * result + ((f4RayleighExtinctionCoeff == null) ? 0 : f4RayleighExtinctionCoeff.hashCode());
		result = prime * result + ((f4TotalExtinctionCoeff == null) ? 0 : f4TotalExtinctionCoeff.hashCode());
		result = prime * result + ((f4TotalMieSctrCoeff == null) ? 0 : f4TotalMieSctrCoeff.hashCode());
		result = prime * result + ((f4TotalRayleighSctrCoeff == null) ? 0 : f4TotalRayleighSctrCoeff.hashCode());
//		result = prime * result + Float.floatToIntBits(fAtmTopHeight);
//		result = prime * result + Float.floatToIntBits(fAtmTopRadius);
//		result = prime * result + Float.floatToIntBits(fEarthRadius);
//		result = prime * result + Float.floatToIntBits(fTurbidity);
//		result = prime * result + Float.floatToIntBits(m_fAerosolPhaseFuncG);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SAirScatteringAttribs other = (SAirScatteringAttribs) obj;
//		if (f2ParticleScaleHeight == null) {
//			if (other.f2ParticleScaleHeight != null)
//				return false;
//		} else if (!f2ParticleScaleHeight.equals(other.f2ParticleScaleHeight))
//			return false;
		if (f4AngularMieSctrCoeff == null) {
			if (other.f4AngularMieSctrCoeff != null)
				return false;
		} else if (!f4AngularMieSctrCoeff.equals(other.f4AngularMieSctrCoeff))
			return false;
		if (f4AngularRayleighSctrCoeff == null) {
			if (other.f4AngularRayleighSctrCoeff != null)
				return false;
		} else if (!f4AngularRayleighSctrCoeff.equals(other.f4AngularRayleighSctrCoeff))
			return false;
		if (f4CS_g == null) {
			if (other.f4CS_g != null)
				return false;
		} else if (!f4CS_g.equals(other.f4CS_g))
			return false;
		if (f4MieExtinctionCoeff == null) {
			if (other.f4MieExtinctionCoeff != null)
				return false;
		} else if (!f4MieExtinctionCoeff.equals(other.f4MieExtinctionCoeff))
			return false;
		if (f4RayleighExtinctionCoeff == null) {
			if (other.f4RayleighExtinctionCoeff != null)
				return false;
		} else if (!f4RayleighExtinctionCoeff.equals(other.f4RayleighExtinctionCoeff))
			return false;
		if (f4TotalExtinctionCoeff == null) {
			if (other.f4TotalExtinctionCoeff != null)
				return false;
		} else if (!f4TotalExtinctionCoeff.equals(other.f4TotalExtinctionCoeff))
			return false;
		if (f4TotalMieSctrCoeff == null) {
			if (other.f4TotalMieSctrCoeff != null)
				return false;
		} else if (!f4TotalMieSctrCoeff.equals(other.f4TotalMieSctrCoeff))
			return false;
		if (f4TotalRayleighSctrCoeff == null) {
			if (other.f4TotalRayleighSctrCoeff != null)
				return false;
		} else if (!f4TotalRayleighSctrCoeff.equals(other.f4TotalRayleighSctrCoeff))
			return false;
//		if (Float.floatToIntBits(fAtmTopHeight) != Float.floatToIntBits(other.fAtmTopHeight))
//			return false;
//		if (Float.floatToIntBits(fAtmTopRadius) != Float.floatToIntBits(other.fAtmTopRadius))
//			return false;
//		if (Float.floatToIntBits(fEarthRadius) != Float.floatToIntBits(other.fEarthRadius))
//			return false;
//		if (Float.floatToIntBits(fTurbidity) != Float.floatToIntBits(other.fTurbidity))
//			return false;
//		if (Float.floatToIntBits(m_fAerosolPhaseFuncG) != Float.floatToIntBits(other.m_fAerosolPhaseFuncG))
//			return false;
		return true;
	}

	public void print(){
    	System.out.println("f4AngularRayleighSctrCoeff = " + f4AngularRayleighSctrCoeff);
    	System.out.println("f4TotalRayleighSctrCoeff = " + f4TotalRayleighSctrCoeff);
    	System.out.println("f4RayleighExtinctionCoeff = " + f4RayleighExtinctionCoeff);
    	
    	System.out.println("f4AngularMieSctrCoeff = " + f4AngularMieSctrCoeff);
    	System.out.println("f4TotalMieSctrCoeff = " + f4TotalMieSctrCoeff);
    	System.out.println("f4MieExtinctionCoeff = " + f4MieExtinctionCoeff);
    	
    	System.out.println("f4TotalExtinctionCoeff = " + f4TotalExtinctionCoeff);
    	System.out.println("f4CS_g = " + f4CS_g);
    }
    
    public ByteBuffer store(ByteBuffer buf){
    	f4AngularRayleighSctrCoeff.store(buf);
    	f4TotalRayleighSctrCoeff.store(buf);
    	f4RayleighExtinctionCoeff.store(buf);
    	f4AngularMieSctrCoeff.store(buf);
    	f4TotalMieSctrCoeff.store(buf);
    	f4MieExtinctionCoeff.store(buf);
    	f4TotalExtinctionCoeff.store(buf);
    	f4CS_g.store(buf);
    	buf.putFloat(fEarthRadius);
    	buf.putFloat(fAtmTopHeight);
    	f2ParticleScaleHeight.store(buf);
    	buf.putFloat(fTurbidity);
    	buf.putFloat(fAtmTopRadius);
    	buf.putFloat(m_fAerosolPhaseFuncG);
    	return buf;
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("SAirScatteringAttribs:\n");
    	sb.append("f4AngularRayleighSctrCoeff = ").append(f4AngularRayleighSctrCoeff).append('\n');
    	sb.append("f4TotalRayleighSctrCoeff = ").append(f4TotalRayleighSctrCoeff).append('\n');
    	sb.append("f4RayleighExtinctionCoeff = ").append(f4RayleighExtinctionCoeff).append('\n');
    	sb.append("f4AngularMieSctrCoeff = ").append(f4AngularMieSctrCoeff).append('\n');
    	sb.append("f4TotalMieSctrCoeff = ").append(f4TotalMieSctrCoeff).append('\n');
    	sb.append("f4MieExtinctionCoeff = ").append(f4MieExtinctionCoeff).append('\n');
    	sb.append("f4TotalExtinctionCoeff = ").append(f4TotalExtinctionCoeff).append('\n');
    	sb.append("f4CS_g = ").append(f4CS_g).append('\n');
    	sb.append("fEarthRadius = ").append(fEarthRadius).append('\n');
    	sb.append("fAtmTopHeight = ").append(fAtmTopHeight).append('\n');
    	sb.append("f2ParticleScaleHeight = ").append(f2ParticleScaleHeight).append('\n');
    	sb.append("fTurbidity = ").append(fTurbidity).append('\n');
    	sb.append("fAtmTopRadius = ").append(fAtmTopRadius).append('\n');
    	sb.append("m_fAerosolPhaseFuncG = ").append(m_fAerosolPhaseFuncG).append('\n');
    	return sb.toString();
    }

	@Override
	public Writable load(ByteBuffer buf) {
		f4AngularRayleighSctrCoeff.load(buf);
		f4TotalRayleighSctrCoeff.load(buf);
		f4RayleighExtinctionCoeff.load(buf);
		f4AngularMieSctrCoeff.load(buf);
		f4TotalMieSctrCoeff.load(buf);
		f4MieExtinctionCoeff.load(buf);
		f4TotalExtinctionCoeff.load(buf);
		f4CS_g.load(buf);
		fEarthRadius = buf.getFloat();
		fAtmTopHeight = buf.getFloat();
		f2ParticleScaleHeight.load(buf);
		fTurbidity = buf.getFloat();
		fAtmTopRadius = buf.getFloat();
		m_fAerosolPhaseFuncG = buf.getFloat();
		return null;
	}
}
