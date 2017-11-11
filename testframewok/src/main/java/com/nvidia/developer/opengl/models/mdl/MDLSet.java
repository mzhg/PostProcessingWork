package com.nvidia.developer.opengl.models.mdl;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by mazhen'gui on 2017/11/11.
 */

final class MDLSet {
    int node;
    String type;
    String name;
    int parent;
    final Matrix4f matrix = new Matrix4f();
    final Vector3f boundingBoxCenter = new Vector3f();
    final Vector3f boundingBoxHalf = new Vector3f();
    int meshcount;
    final List<String> materialNames = new ArrayList<>();
    final List<Properties> materials = new ArrayList<>();


    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MDLSet{");
        sb.append("node=").append(node).append('\n');
        sb.append("type='").append(type).append('\'').append('\n');
        sb.append("name='").append(name).append('\'').append('\n');
        sb.append("parent=").append(parent).append('\n');
        sb.append("matrix=").append(matrix).append('\n');
        sb.append("boundingBoxCenter=").append(boundingBoxCenter).append('\n');
        sb.append("boundingBoxHalf=").append(boundingBoxHalf).append('\n');
        sb.append("meshcount=").append(meshcount).append('\n');
        sb.append("materialNames=").append(materialNames).append('\n');
        sb.append("materials=").append(materials).append('\n');
        sb.append('}');
        return sb.toString();
    }
}
