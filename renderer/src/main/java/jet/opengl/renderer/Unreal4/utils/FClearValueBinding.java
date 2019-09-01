package jet.opengl.renderer.Unreal4.utils;

import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector4f;

import java.util.Objects;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.renderer.Unreal4.api.ERHIZBuffer;

public class FClearValueBinding {

    //DO NOT USE THE STATIC FLINEARCOLORS TO INITIALIZE THIS STUFF.
//Static init order is undefined and you will likely end up with bad values on some platforms.
    public static FClearValueBinding None = new FClearValueBinding(EClearBinding.ENoneBound);
    public static FClearValueBinding Black = new FClearValueBinding(new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
    public static FClearValueBinding White = new FClearValueBinding(Vector4f.ONE);
    public static FClearValueBinding Transparent = new FClearValueBinding(Vector4f.ZERO);
    public static FClearValueBinding DepthOne = new FClearValueBinding(1.0f, 0);
    public static FClearValueBinding DepthZero = new FClearValueBinding(0.0f, 0);
    public static FClearValueBinding DepthNear = new FClearValueBinding((float) ERHIZBuffer.NearPlane, 0);
    public static FClearValueBinding DepthFar = new FClearValueBinding((float)ERHIZBuffer.FarPlane, 0);
    public static FClearValueBinding Green = new FClearValueBinding(new Vector4f(0.0f, 1.0f, 0.0f, 1.0f));
    // Note: this is used as the default normal for DBuffer decals.  It must decode to a value of 0 in DecodeDBufferData.
    public static FClearValueBinding DefaultNormal8Bit = new FClearValueBinding(new Vector4f(128.0f / 255.0f, 128.0f / 255.0f, 128.0f / 255.0f, 1.0f));

    private float Depth;
    private int Stencil;
    private float ColorR, ColorG, ColorB, ColorA;
    private EClearBinding ColorBinding = EClearBinding.EColorBound;

    public FClearValueBinding()
//		: ColorBinding(EClearBinding::EColorBound)
    {
//        Value.Color[0] = 0.0f;
//        Value.Color[1] = 0.0f;
//        Value.Color[2] = 0.0f;
//        Value.Color[3] = 0.0f;
    }

    public FClearValueBinding(EClearBinding NoBinding)
//		: ColorBinding(NoBinding)
    {
//        check(ColorBinding == EClearBinding::ENoneBound);

        ColorBinding = NoBinding;
        if(ColorBinding != EClearBinding.ENoneBound)
            throw new IllegalArgumentException();
    }

    public FClearValueBinding(ReadableVector4f InClearColor)
//		: ColorBinding(EClearBinding::EColorBound)
    {
        /*Value.Color[0] = InClearColor.R;
        Value.Color[1] = InClearColor.G;
        Value.Color[2] = InClearColor.B;
        Value.Color[3] = InClearColor.A;*/

        ColorR = InClearColor.getX();
        ColorG = InClearColor.getY();
        ColorB = InClearColor.getZ();
        ColorA = InClearColor.getW();
    }

    public FClearValueBinding(float DepthClearValue, int StencilClearValue /*= 0*/)
//		: ColorBinding(EClearBinding::EDepthStencilBound)
    {
        ColorBinding = EClearBinding.EDepthStencilBound;
        Depth = DepthClearValue;
        Stencil = StencilClearValue;
    }

    public void GetClearColor(Vector4f OutColor)
    {
//        ensure(ColorBinding == EClearBinding::EColorBound);
//        return FLinearColor(Value.Color[0], Value.Color[1], Value.Color[2], Value.Color[3]);
        if(ColorBinding != EClearBinding.EColorBound)
            throw new IllegalArgumentException();

        OutColor.set(ColorR, ColorG, ColorB, ColorA);
    }

    /*void GetDepthStencil(float& OutDepth, uint32& OutStencil) const
    {
        ensure(ColorBinding == EClearBinding::EDepthStencilBound);
        OutDepth = Value.DSValue.Depth;
        OutStencil = Value.DSValue.Stencil;
    }*/

    public float GetDepth(){
        if(ColorBinding != EClearBinding.EDepthStencilBound)
            throw new IllegalArgumentException();

        return Depth;
    }

    public int GetStencil(){
        if(ColorBinding != EClearBinding.EDepthStencilBound)
            throw new IllegalArgumentException();

        return Stencil;
    }

    public boolean equals(FClearValueBinding Other)
    {
        if (ColorBinding == Other.ColorBinding)
        {
            if (ColorBinding == EClearBinding.EColorBound)
            {
                /*return
                        Value.Color[0] == Other.Value.Color[0] &&
                                Value.Color[1] == Other.Value.Color[1] &&
                                Value.Color[2] == Other.Value.Color[2] &&
                                Value.Color[3] == Other.Value.Color[3];*/

                return
                        ColorR == Other.ColorR &&
                        ColorG == Other.ColorG &&
                        ColorB == Other.ColorB &&
                        ColorA == Other.ColorA;
            }
            if (ColorBinding == EClearBinding.EDepthStencilBound)
            {
//                return
//                        Value.DSValue.Depth == Other.Value.DSValue.Depth &&
//                                Value.DSValue.Stencil == Other.Value.DSValue.Stencil;
                return Depth == Other.Depth && Stencil == Other.Stencil;
            }
            return true;
        }
        return false;
    }


}
