package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaBoundingBox {
    public final Vector3f Min = new Vector3f();
    public final Vector3f Size = new Vector3f();

//    public static VaBoundingBox             Degenerate = new VaBoundingBox();

    public VaBoundingBox( ) { }
    public VaBoundingBox(ReadableVector3f bmin, ReadableVector3f bsize ) /*: Min(bmin), Size( bsize ) { }*/{
        Min.set(bmin);
        Size.set(bsize);
    }

    public Vector3f                       Center( )  { return Vector3f.linear(Min , Size , 0.5f,null); }
    public Vector3f                       Max( )  { return Vector3f.add(Min , Size, null); }

    public boolean  Save( VaStream outStream ) throws IOException{
        outStream.WriteFloat(Min.x);
        outStream.WriteFloat(Min.y);
        outStream.WriteFloat(Min.z);

        outStream.WriteFloat(Size.x);
        outStream.WriteFloat(Size.y);
        outStream.WriteFloat(Size.z);

        return true;
    }

    public boolean  Load( VaStream inStream ) throws IOException {
        Min.x = inStream.ReadFloat();
        Min.y = inStream.ReadFloat();
        Min.z = inStream.ReadFloat();
        Size.x = inStream.ReadFloat();
        Size.y = inStream.ReadFloat();
        Size.z = inStream.ReadFloat();

        return true;
    }

    public static VaBoundingBox  Combine( VaBoundingBox a, VaBoundingBox b ){
        Vector3f bmaxA = /*a.Min + a.Size*/ a.Max();
        Vector3f bmaxB = /*b.Min + b.Size*/ b.Max();

        Vector3f finalMin = Vector3f.min( a.Min, b.Min, null );
        Vector3f finalMax = Vector3f.max( bmaxA, bmaxB, null );

        return new VaBoundingBox( finalMin, Vector3f.sub(finalMax, finalMin, finalMax) );
    }
}
