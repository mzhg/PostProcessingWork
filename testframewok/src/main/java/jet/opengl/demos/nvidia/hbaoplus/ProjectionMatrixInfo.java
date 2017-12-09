package jet.opengl.demos.nvidia.hbaoplus;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.util.Numeric;

class ProjectionMatrixInfo {

	private float m_ZNear;
	private float m_ZFar;
	private float m_TanHalfFovX;
	private float m_TanHalfFovY;
	
	GFSDK_SSAO_Status init(Matrix4f projectionMatrix){
		if (!isValid(projectionMatrix))
	    {
	        return GFSDK_SSAO_Status.GFSDK_SSAO_INVALID_PROJECTION_MATRIX;
	    }

	    Matrix4f m = (projectionMatrix);

	    // In matrices generated with D3DXMatrixPerspectiveFovRH
	    // A = zf/(zn-zf)
	    // B = zn*zf/(zn-zf)
	    // C = -1
	    float A = m.m22;
	    float B = m.m32;
	    float C = m.m23;

	    // In matrices generated with D3DXMatrixPerspectiveFovLH
	    // A = -zf/(zn-zf)
	    // B = zn*zf/(zn-zf)
	    // C = 1
	    if (C == 1.f)
	    {
	        A = -A;
	    }

	    // Rely on INFs to be generated in case of any divisions by zero
//	    m_ZNear = (API == API_GL) ? (B / (A - 1.f)) : (B / A);
	    m_ZNear = (B / (A - 1.f));
	    m_ZFar = B / (A + 1.f);
	    
	    // Some matrices may use negative m00 or m11 terms to flip X/Y axises
	    m_TanHalfFovX = 1.f / Math.abs(m.m00);
	    m_TanHalfFovY = 1.f / Math.abs(m.m11);
	    
//	    System.out.println("m_ZNear = " + m_ZNear);
//	    System.out.println("m_ZFar = " + m_ZFar);
//	    System.out.println("m_TanHalfFovX = " + m_TanHalfFovX);
//	    System.out.println("m_TanHalfFovY = " + m_TanHalfFovY);
	    return GFSDK_SSAO_Status.GFSDK_SSAO_OK;
	}
    
	static boolean isValid(Matrix4f projectionMatrix){
		Matrix4f m = projectionMatrix;
		// Do not check m(2,0) and m(2,1) to allow off-centered projections
	    // Do not check m(2,2) to allow reverse infinite projections
	    return (m.m00 != 0.0f && m.m01 == 0.0f && m.m02 == 0.0f && m.m03 == 0.0f &&
	            m.m10 == 0.0f && m.m11 != 0.0f && m.m12 == 0.0f && m.m13 == 0.0f &&
	            Math.abs(m.m23) == 1.0f &&
	            m.m30 == 0.0f && m.m31 == 0.0f && m.m32 != 0.0f && m.m33 == 0.0f);
    }

    // Clamp to EPSILON to avoid any divisions by 0.f
    float getInverseZNear()
    {
        return Math.max(1.f / m_ZNear, Numeric.EPSILON);
    }
    
    float getInverseZFar()
    {
        return Math.max(1.f / m_ZFar, Numeric.EPSILON);
    }

    float getTanHalfFovX()
    {
        return m_TanHalfFovX;
    }
    
    float getTanHalfFovY()
    {
        return m_TanHalfFovY;
    }

    void getDepthRange(GFSDK_SSAO_ProjectionMatrixDepthRange pDepthRange)
    {
        pDepthRange.zNear = 1.f / getInverseZNear();
        pDepthRange.zFar = 1.f / getInverseZFar();
    }
}
