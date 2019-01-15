package jet.opengl.demos.nvidia.waves.wavelets;

import jet.opengl.postprocessing.util.StackFloat;

final class Grid {
    private float[] m_data;
    private final int[] m_dimensions = new int[4];


    void resize(int n0, int n1, int n2, int n3){
        m_dimensions[0] = n0;
        m_dimensions[1] = n1;
        m_dimensions[2] = n2;
        m_dimensions[3] = n3;

        m_data = new float[n0 * n1 * n2 * n3];
    }

    float get(int i0, int i1, int i2, int i3){
        return m_data[i3 +
                dimension(3) * (i2 + dimension(2) * (i1 + dimension(1) * i0))];
    }

    void set(int i0, int i1, int i2, int i3, float v){
        m_data[i3 +
                dimension(3) * (i2 + dimension(2) * (i1 + dimension(1) * i0))] = v;
    }

    void incre(int i0, int i1, int i2, int i3, float v){
        m_data[i3 +
                dimension(3) * (i2 + dimension(2) * (i1 + dimension(1) * i0))] += v;
    }

    int dimension(int dim) {
        return m_dimensions[dim];
    }
}
