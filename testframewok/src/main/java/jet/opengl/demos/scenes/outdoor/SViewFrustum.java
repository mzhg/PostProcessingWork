package jet.opengl.demos.scenes.outdoor;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

final class SViewFrustum {

	final Vector4f leftPlane = new Vector4f();
	final Vector4f rightPlane = new Vector4f();
	final Vector4f bottomPlane = new Vector4f();
	final Vector4f topPlane = new Vector4f();
	final Vector4f nearPlane = new Vector4f();
	final Vector4f farPlane = new Vector4f();
	
	final Vector4f[] array = {
			leftPlane, rightPlane, bottomPlane, topPlane, nearPlane, farPlane
	};
	
	void extractViewFrustumPlanesFromMatrix(Matrix4f Matrix)
	{
	    // For more details, see Gribb G., Hartmann K., "Fast Extraction of Viewing Frustum Planes from the 
	    // World-View-Projection Matrix" (the paper is available at 
	    // http://www2.ravensoft.com/users/ggribb/plane%20extraction.pdf)

		Matrix4f.extractFrustumPlanes(Matrix, array);
		
		// Left clipping plane 
//	    LeftPlane.Normal.x = Matrix._14 + Matrix._11; 
//		LeftPlane.Normal.y = Matrix._24 + Matrix._21; 
//		LeftPlane.Normal.z = Matrix._34 + Matrix._31; 
//		LeftPlane.Distance = Matrix._44 + Matrix._41;
//
//		// Right clipping plane 
//		RightPlane.Normal.x = Matrix._14 - Matrix._11; 
//		RightPlane.Normal.y = Matrix._24 - Matrix._21; 
//		RightPlane.Normal.z = Matrix._34 - Matrix._31; 
//		RightPlane.Distance = Matrix._44 - Matrix._41;
//
//		// Top clipping plane 
//		TopPlane.Normal.x = Matrix._14 - Matrix._12; 
//		TopPlane.Normal.y = Matrix._24 - Matrix._22; 
//		TopPlane.Normal.z = Matrix._34 - Matrix._32; 
//		TopPlane.Distance = Matrix._44 - Matrix._42;
//
//		// Bottom clipping plane 
//		BottomPlane.Normal.x = Matrix._14 + Matrix._12; 
//		BottomPlane.Normal.y = Matrix._24 + Matrix._22; 
//		BottomPlane.Normal.z = Matrix._34 + Matrix._32; 
//		BottomPlane.Distance = Matrix._44 + Matrix._42;
//
//		// Near clipping plane 
//		NearPlane.Normal.x = Matrix._13; 
//		NearPlane.Normal.y = Matrix._23; 
//		NearPlane.Normal.z = Matrix._33; 
//		NearPlane.Distance = Matrix._43;
//
//		// Far clipping plane 
//		FarPlane.Normal.x = Matrix._14 - Matrix._13; 
//		FarPlane.Normal.y = Matrix._24 - Matrix._23; 
//		FarPlane.Normal.z = Matrix._34 - Matrix._33; 
//		FarPlane.Distance = Matrix._44 - Matrix._43; 
	}
	
	boolean isBoxVisible(BoundingBox Box)
	{
//	    SPlane3D *pPlanes = (SPlane3D *)&ViewFrustum;
	    // If bounding box is "behind" some plane, then it is invisible
	    // Otherwise it is treated as visible
	    for(int iViewFrustumPlane = 0; iViewFrustumPlane < 6; iViewFrustumPlane++)
	    {
//	        SPlane3D *pCurrPlane = pPlanes + iViewFrustumPlane;
	    	Vector4f pCurrPlane = array[iViewFrustumPlane];
	        Vector4f pCurrNormal = pCurrPlane;
	        
	        float MaxPointX = (pCurrNormal.x > 0) ? Box.xMax() : Box.xMin();
	        float MaxPointY = (pCurrNormal.y > 0) ? Box.yMax() : Box.yMin();
	        float MaxPointZ = (pCurrNormal.z > 0) ? Box.zMax() : Box.zMin();
	        
//	        float DMax = D3DXVec3Dot( &MaxPoint, pCurrNormal ) + pCurrPlane->Distance;
	        float DMax = MaxPointX * pCurrNormal.x + MaxPointY * pCurrNormal.y + MaxPointZ * pCurrNormal.z + pCurrPlane.w;

	        if( DMax < 0 )
	            return false;
	    }

	    return true;
	}

}
