package jet.opengl.demos.amdfx.geometry;

public final class GeometryFX_FilterStatistics {
    public long trianglesProcessed;
    public long trianglesRendered;
    public long trianglesCulled;
    public long clustersProcessed;
    public long clustersRendered;
    public long clustersCulled;

    public void reset(){
        trianglesProcessed = 0;
        trianglesRendered = 0;
        trianglesCulled = 0;
        clustersProcessed = 0;
        clustersRendered = 0;
        clustersCulled = 0;
    }
}
