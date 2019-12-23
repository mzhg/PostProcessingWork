package jet.opengl.demos.gpupro.culling;

interface OcclusionTester {

    void newFrame(int frameNumber);

    void cullingCoarse(Renderer renderer, Scene scene);

    void cullingFine(Renderer renderer, Scene scene);
}
