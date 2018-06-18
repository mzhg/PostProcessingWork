package jet.opengl.demos.intel.fluid.render;

/**
 * Format of a generic vertex, used as a an intermediate format.

 Use this format to populate a vertex buffer from a generic file
 format (such as Wavefront OBJ) or procedurally, then pass that
 generic buffer to the platform-specific implementations to translate
 that into a format ready to render.

 \note   This is a temporary, quick-and-dirty hack.  More proper
 alternatives to this include the following:

 -   Support "vertex declarations" as noted above.

 From there, multiple options follow:

 -   Use the generic format directly.  This would work in
 combination with using facilities in the underlying
 rendering API (such as OpenGL or Direct3D) to support
 supplying offset+stride information, so that the
 "generic format" would be identical to the
 platform-specific format.  This has the nice benefit
 that game-ready assets could be the same for all
 platforms.  But it relies on the rendering API to
 support offset+index facilities, which many legacy
 systems (such as D3D8, used by original Xbox, and
 probably version of OpenGL that precede vertex buffer
 arrays, possibly including OpenGL-ES) do not.

 -   Provide an abstract interface for each field of a vertex.
 Each platform-specific VertexBuffer would implement an
 accessor, given the vertex index and vertex element.
 This would entail a virtual call for each access, which
 would be incredibly slow, but it would work for all
 platforms and would only be slow when populating vertex
 buffers, which presumably would only happen at load time.

 -   Omit in-game support for generic memory formats.

 From there, multiple options follow:

 -   Each platform would supply a translator from a generic
 file format to a platform-specific memory format.

 -   The external asset conditioning pipeline must convert
 the assets from a generic file format to a game-ready
 format.

 This "generic intermediary" approach could work in conjunction
 with having the asset pipeline prepare platform-specific
 game-ready assets.  The generic form could be relegated to rapid
 prototyping or iterative development of assets, where the
 platform-specific, game-ready form would be meant for shipping.

 In practice, all of these approaches could work.  For platforms
 which allow use of generic memory formats, the translators would
 not be necessary -- thereby potentially reducing the amount of
 platform-specific code.  And for platforms that do not support
 generic memory formats, fall back to relying on translators,
 either in-game or exporter.

 Note that the in-game and exporter translators should use the
 same code -- but it should be possible to "disable" the in-game
 code, for compiling "ship" versions, to reduce code size.
 *
 * Created by Administrator on 2018/3/13 0013.
 */

public class GenericVertex {
    public static final int SIZE = 15 * 4;

    public float ts, tt, tu, tv    ;   ///< 4D texture coordinates
    public float cr, cg, cb, ca    ;   ///< color in RGBA form
    public float nx, ny, nz        ;   ///< surface normal unit vector
    public float px, py, pz, pw    ;   ///< untransformed (world-space) position
}
