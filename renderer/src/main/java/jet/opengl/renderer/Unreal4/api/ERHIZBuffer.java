package jet.opengl.renderer.Unreal4.api;

public interface ERHIZBuffer {
    int
            // Before changing this, make sure all math & shader assumptions are correct! Also wrap your C++ assumptions with
            //		static_assert(ERHIZBuffer::IsInvertedZBuffer(), ...);
            // Shader-wise, make sure to update Definitions.usf, HAS_INVERTED_Z_BUFFER
            FarPlane = /*0*/1,
            NearPlane = /*1*/0;

    // 'bool' for knowing if the API is using Inverted Z buffer
//    IsInverted = (int32)((int32)ERHIZBuffer::FarPlane < (int32)ERHIZBuffer::NearPlane),

    boolean IsInverted = FarPlane < NearPlane;
}
