package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/20.
 */

public class VaViewport {
    public int   X;
    public int   Y;
    public int   Width;
    public int   Height;
    public float MinDepth = 0;
    public float MaxDepth = 1;

    public VaViewport( ) /*: X( 0 ), Y( 0 ), Width( 0 ), Height( 0 ), MinDepth( 0 ), MaxDepth( 1.0f )*/ { }

    public VaViewport( int width, int height ) /*: X( 0 ), Y( 0 ), Width( width ), Height( height ), MinDepth( 0 ), MaxDepth( 1.0f )*/ {
        Width = width;
        Height = height;
    }

    public VaViewport( int x, int y, int width, int height ) /*: X( x ), Y( y ), Width( width ), Height( height ), MinDepth( 0 ), MaxDepth( 1.0f )*/ {
        X = x;
        Y = y;
        Width = width;
        Height = height;
    }

    public VaViewport(VaViewport ohs){
        Set(ohs);
    }

    public VaViewport Reset(){
        X = 0;
        Y = 0;
        Width = 0;
        Height = 0;
        MinDepth = 0;
        MaxDepth = 1;

        return this;
    }

    public void Set(VaViewport ohs){
        if(this == ohs)
            return;

        X = ohs.X;
        Y = ohs.Y;
        Width = ohs.Width;
        Height = ohs.Height;
        MinDepth = ohs.MinDepth;
        MaxDepth = ohs.MaxDepth;
    }
}
