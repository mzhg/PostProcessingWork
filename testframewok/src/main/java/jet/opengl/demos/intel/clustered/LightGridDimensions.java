package jet.opengl.demos.intel.clustered;

final class LightGridDimensions {
    int width;
    int height;
    int depth;

    LightGridDimensions(int _width, int _height, int _depth){
        width = _width;
        height = _height;
        depth = _depth;
    }

    void set(LightGridDimensions ohs){
        width = ohs.width;
        height = ohs.height;
        depth = ohs.depth;
    }

    void set(int _width, int _height, int _depth){
        width = _width;
        height = _height;
        depth = _depth;
    }

    // cell: 4x4x4 entries or 2x2x1 packed entries
    int cellIndex(int x, int y, int z) {
        assert((width | height | depth) % 4 == 0) :"dimensions must be cell-aligned" ;
        assert(x >= 0 && y >= 0 && z >= 0);
        assert(x < width / 4 && y < height / 4 && z < depth / 4);

        return (y*width / 4 + x)*depth / 4 + z;
    }
}
