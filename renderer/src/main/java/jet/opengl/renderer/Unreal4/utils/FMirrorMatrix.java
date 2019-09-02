package jet.opengl.renderer.Unreal4.utils;

import org.lwjgl.util.vector.Matrix4f;

/**
 * Mirrors a point about an abitrary plane
 */
public class FMirrorMatrix extends Matrix4f {

    /**
     * Constructor. Updated for the fact that our FPlane uses Ax+By+Cz=D.
     *
     * @param Plane source plane for mirroring (assumed normalized)
     */
    public FMirrorMatrix(FPlane Plane){
        set(
                /*FPlane(*/ -2.f*Plane.X*Plane.X + 1.f,	-2.f*Plane.Y*Plane.X,		-2.f*Plane.Z*Plane.X,		0.f /*)*/,
                /*FPlane(*/ -2.f*Plane.X*Plane.Y,			-2.f*Plane.Y*Plane.Y + 1.f,	-2.f*Plane.Z*Plane.Y,		0.f /*)*/,
                /*FPlane(*/ -2.f*Plane.X*Plane.Z,			-2.f*Plane.Y*Plane.Z,		-2.f*Plane.Z*Plane.Z + 1.f,	0.f /*)*/,
                /*FPlane(*/  2.f*Plane.X*Plane.W,			 2.f*Plane.Y*Plane.W,		 2.f*Plane.Z*Plane.W,		1.f /*)*/
        ); // TODO Need transpose ?
    }
}
