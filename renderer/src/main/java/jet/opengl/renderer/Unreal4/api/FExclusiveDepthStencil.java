package jet.opengl.renderer.Unreal4.api;

public interface FExclusiveDepthStencil {
    int
            // don't use those directly, use the combined versions below
            // 4 bits are used for depth and 4 for stencil to make the hex value readable and non overlapping
            DepthNop =		0x00,
            DepthRead =		0x01,
            DepthWrite =	0x02,
            DepthMask =		0x0f,
            StencilNop =	0x00,
            StencilRead =	0x10,
            StencilWrite =	0x20,
            StencilMask =	0xf0,

    // use those:
    DepthNop_StencilNop = DepthNop + StencilNop,
            DepthRead_StencilNop = DepthRead + StencilNop,
            DepthWrite_StencilNop = DepthWrite + StencilNop,
            DepthNop_StencilRead = DepthNop + StencilRead,
            DepthRead_StencilRead = DepthRead + StencilRead,
            DepthWrite_StencilRead = DepthWrite + StencilRead,
            DepthNop_StencilWrite = DepthNop + StencilWrite,
            DepthRead_StencilWrite = DepthRead + StencilWrite,
            DepthWrite_StencilWrite = DepthWrite + StencilWrite;
}
