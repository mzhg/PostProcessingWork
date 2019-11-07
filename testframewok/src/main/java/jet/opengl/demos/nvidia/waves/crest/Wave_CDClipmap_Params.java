package jet.opengl.demos.nvidia.waves.crest;

public class Wave_CDClipmap_Params {
    /** The offset required to place the surface at sea level*/
    public float sea_level = 0;

    /** Min number of verts / shape texels per wave. Ranged 2 to 16. defulat is 3.*/
    public float minTexelsPerWave = 3;

    /** The smallest scale the ocean can be. */
    public float minScale = 2f;

    /** The largest scale the ocean can be (-1 for unlimited).*/
    public float maxScale = 256f;

    /** Resolution of ocean LOD data. Use even numbers like 256 or 384. This is 4x the old 'Base Vert Density' param, so if you used 64 for this param, set this to 256.*/
    public int lodDataResolution = 384;

    /** How much of the water shape gets tessellated by geometry. If set to e.g. 4, every geometry quad will span 4x4 LOD data texels. Use power of 2 values like 1, 2, 4...*/
    public int geometryDownSampleFactor = 4;
    /** Number of ocean tile scales/LODs to generate, Range(2, LodDataMgr.MAX_LOD_COUNT)]*/
    public int lodCount = 7;
    /** Whether to generate ocean geometry tiles uniformly (with overlaps). */
    public boolean uniformTiles = false;
    /** Disable generating a wide strip of triangles at the outer edge to extend ocean to edge of view frustum.*/
    public boolean disableSkirt = false;
    /** Move ocean with viewpoint */
    public boolean followViewpoint = true;
    /** Drops the height for maximum ocean detail based on waves. This means if there are big waves, max detail level is reached at a lower height, which can help visual range when there are very large waves and camera is at sea level.*/
    public float dropDetailHeightBasedOnWaves = 0.2f;

    public void set(Wave_CDClipmap_Params ohs){
        this.sea_level = ohs.sea_level;
        this.minTexelsPerWave = ohs.minTexelsPerWave;
        this.minScale = ohs.minScale;
        this.maxScale = ohs.maxScale;
        this.sea_level = ohs.sea_level;
        this.lodDataResolution = ohs.lodDataResolution;
        this.geometryDownSampleFactor = ohs.geometryDownSampleFactor;
        this.lodCount = ohs.lodCount;
        this.uniformTiles = ohs.uniformTiles;
        this.disableSkirt = ohs.disableSkirt;
        this.followViewpoint = ohs.followViewpoint;
        this.dropDetailHeightBasedOnWaves = ohs.dropDetailHeightBasedOnWaves;
    }
}
