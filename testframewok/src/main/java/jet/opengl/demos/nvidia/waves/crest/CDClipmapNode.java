package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Transform;

final class CDClipmapNode {
    int meshIndex = -1;
    final Transform transform = new Transform();
    // only for the debug
    String name;

    int sortingOrder;

    CDClipmapNode(String name){
        this.name = name;
    }

}
