package jet.opengl.demos.intel.fluid.collision;

/**
 * Created by Administrator on 2018/4/25 0025.
 */

public class SphereShape extends ShapeBase {
    public static final int sShapeType = /*'stsp'*/1937011568 ;

    /** Construct sphere.
     */
    public SphereShape()
    {
        super(sShapeType, false);
    }


    /** Construct sphere given a position and a radius.
     */
    public SphereShape( float radius )
//            : ShapeBase( sShapeType , radius )
    {
        super(sShapeType, radius);
        assert ( radius >= 0.0f ) ;
    }
}
