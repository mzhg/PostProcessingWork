package jet.opengl.demos.gpupro.lpv;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;

final class Grid {
    final Vector3f min = new Vector3f();
    final Vector3f max = new Vector3f();
    final Vector3f center = new Vector3f();
    final Vector3f offset = new Vector3f();
    final Vector3f centerToMin = new Vector3f();
    final Vector3f centerToMax = new Vector3f();
    final Vector3f origMin = new Vector3f();
    final Vector3f origMax = new Vector3f();
    final Vector3i dimensions = new Vector3i();
    float scale, cellSize;
    int level;
    final Matrix4f m = new Matrix4f();

    Grid() {};
    Grid(Vector3f max, Vector3f min, float scale, int level){
        this.max.set(max);
        this.min.set(min);
        this.scale = scale;
        this.level = level;
        setUp();
    }

    Grid(Grid old, float scale, int level){
        this.max.set(old.max);
        this.min.set(old.min);
        this.scale = scale;
        this.level = level;
        setUp();
    }
    Vector3f getMin() { return min; };
    Vector3f getMax() { return max; };
    Vector3i getDimensions() { return dimensions; };
    float getScale() { return scale; };
    float getCellSize() { return cellSize; };
    void translateGrid(Vector3f pos, Vector3f dir){
        //new center = camera pos
//        glm::vec4 newCenter = glm::vec4(pos, 1.0);
//        glm::vec3 centerToNewCenter = glm::vec3(newCenter) - center;
        float centerToNewCenterX = pos.x - center.x;
        float centerToNewCenterY = pos.y - center.y;
        float centerToNewCenterZ = pos.z - center.z;

        int snapX = (int) (centerToNewCenterX / cellSize + 0.5);
        int snapY = (int) (centerToNewCenterY / cellSize + 0.5);
        int snapZ = (int) (centerToNewCenterZ / cellSize + 0.5);

        int xOffset = (int) (center.x + (snapX)*cellSize);
        int yOffset = (int) (center.y + (snapY)*cellSize);
        int zOffset = (int) (center.z + (snapZ)*cellSize);
        Vector3f offset = new Vector3f(xOffset, yOffset, zOffset);
        Vector3f newMin = Vector3f.add(offset ,centerToMin, null);
        Vector3f newMax = Vector3f.add(offset,centerToMax, null);
        //-------
        float halfDisplacement = 0.8f * LightPropagationVolumeDemo.MAX_GRID_SIZE * 0.5f * cellSize;
//        glm::vec3 displacement = dir*glm::vec3(halfDisplacement);
        float displacementX = dir.x * halfDisplacement;
        float displacementY = dir.y * halfDisplacement;
        float displacementZ = dir.z * halfDisplacement;
        int snapXDisp = (int) (displacementX / cellSize + 0.5);
        int snapYDisp = (int) (displacementY / cellSize + 0.5);
        int snapZDisp = (int) (displacementZ / cellSize + 0.5);
        //glm::vec3 displacedMin = newMin + displacement;
        //glm::vec3 displacedMax = newMax + displacement;
        Vector3f offsetDisp = new Vector3f(snapXDisp * cellSize, snapYDisp * cellSize, snapZDisp * cellSize);
        Vector3f.add(newMin, offsetDisp, min);
        Vector3f.add(newMax, offsetDisp, max);

//        min = displacedMin;
//        max = displacedMax;
    }
    Matrix4f getModelMatrix() { return m; }
    Vector3f getCenter() { return center; }

    void setUp() {
        center.set(0,0,0);
        cellSize = LightPropagationVolumeDemo.MAX_CELL_SIZE * scale;
//        min = center - glm::vec3(LightPropagationVolumeDemo.MAX_GRID_SIZE*0.5 * cellSize);
//        max = center + glm::vec3(LightPropagationVolumeDemo.MAX_GRID_SIZE*0.5f * cellSize);

        Vector3f.sub(center, LightPropagationVolumeDemo.MAX_GRID_SIZE*0.5f * cellSize, min);
        Vector3f.add(center, LightPropagationVolumeDemo.MAX_GRID_SIZE*0.5f * cellSize, max);
//        centerToMin = min - center;
//        centerToMax = max - center;
        Vector3f.sub(min, center, centerToMin);
        Vector3f.sub(max, center, centerToMax);

//        std::cout << "centerToMin: " << centerToMin.x << "," << centerToMin.y << "," << centerToMin.z << std::endl;
//        std::cout << "centerToMax: " << centerToMax.x << "," << centerToMax.y << "," << centerToMax.z << std::endl;

        dimensions.x = LightPropagationVolumeDemo.MAX_GRID_SIZE;
        dimensions.y = LightPropagationVolumeDemo.MAX_GRID_SIZE;
        dimensions.z = LightPropagationVolumeDemo.MAX_GRID_SIZE;

//        std::cout << "Max: " << max.x << "," << max.y << "," << max.z << std::endl;
//        std::cout << "Min: " << min.x << "," << min.y << "," << min.z << std::endl;
//        std::cout << "center: " << center.x << "," << center.y << "," << center.z << std::endl;
//        std::cout << "Cellsize: " << cellSize << std::endl;
    }
}
