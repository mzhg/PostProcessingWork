package jet.opengl.demos.nvidia.waves.crest;

/**
 * The comments below illustrate case when BASE_VERT_DENSITY = 2. The ocean mesh is built up from these patches. Rotational symmetry
 * is used where possible to eliminate combinations. The slim variants are used to eliminate overlap between patches.
 */
enum PatchType {
    /// <summary>
    /// Adds no skirt. Used in interior of highest detail LOD (0)
    ///
    ///    1 -------
    ///      |  |  |
    ///  z   -------
    ///      |  |  |
    ///    0 -------
    ///      0     1
    ///         x
    ///
    /// </summary>
    Interior,

    /// <summary>
    /// Adds a full skirt all of the way around a patch
    ///
    ///      -------------
    ///      |  |  |  |  |
    ///    1 -------------
    ///      |  |  |  |  |
    ///  z   -------------
    ///      |  |  |  |  |
    ///    0 -------------
    ///      |  |  |  |  |
    ///      -------------
    ///         0     1
    ///            x
    ///
    /// </summary>
    Fat,

    /// <summary>
    /// Adds a skirt on the right hand side of the patch
    ///
    ///    1 ----------
    ///      |  |  |  |
    ///  z   ----------
    ///      |  |  |  |
    ///    0 ----------
    ///      0     1
    ///         x
    ///
    /// </summary>
    FatX,

    /// <summary>
    /// Adds a skirt on the right hand side of the patch, removes skirt from top
    /// </summary>
    FatXSlimZ,

    /// <summary>
    /// Outer most side - this adds an extra skirt on the left hand side of the patch,
    /// which will point outwards and be extended to Zfar
    ///
    ///    1 --------------------------------------------------------------------------------------
    ///      |  |  |                                                                              |
    ///  z   --------------------------------------------------------------------------------------
    ///      |  |  |                                                                              |
    ///    0 --------------------------------------------------------------------------------------
    ///      0     1
    ///         x
    ///
    /// </summary>
    FatXOuter,

    /// <summary>
    /// Adds skirts at the top and right sides of the patch
    /// </summary>
    FatXZ,

    /// <summary>
    /// Adds skirts at the top and right sides of the patch and pushes them to horizon
    /// </summary>
    FatXZOuter,

    /// <summary>
    /// One less set of verts in x direction
    /// </summary>
    SlimX,

    /// <summary>
    /// One less set of verts in both x and z directions
    /// </summary>
    SlimXZ,

    /// <summary>
    /// One less set of verts in x direction, extra verts at start of z direction
    ///
    ///      ----
    ///      |  |
    ///    1 ----
    ///      |  |
    ///  z   ----
    ///      |  |
    ///    0 ----
    ///      0     1
    ///         x
    ///
    /// </summary>
    SlimXFatZ,
}
