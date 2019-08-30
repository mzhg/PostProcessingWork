package jet.opengl.renderer.Unreal4.utils;

public class FResolveRect {
    public int X1 = -1;

    public int Y1 = -1;

    public int X2 = -1;

    public int Y2 = -1;

    public FResolveRect(){}

    // e.g. for a a full 256 x 256 area starting at (0, 0) it would be
    // the values would be 0, 0, 256, 256
    public FResolveRect(int InX1, int InY1, int InX2, int InY2)
    {
        X1 = InX1;
        Y1 = InY1;
        X2 = InX2;
        Y2 = InY2;
    }



    public FResolveRect(FResolveRect Other)
    {
        Set(Other);
    }

    public void Set(FResolveRect Other){
        X1 = Other.X1;
        Y1 = Other.Y1;
        X2 = Other.X2;
        Y2 = Other.Y2;
    }

    public void Reset(){
        X1 = -1;
        Y1 = -1;
        X2 = -1;
        Y2 = -1;
    }

    public boolean IsValid()
    {
        return X1 >= 0 && Y1 >= 0 && X2 - X1 > 0 && Y2 - Y1 > 0;
    }
}
