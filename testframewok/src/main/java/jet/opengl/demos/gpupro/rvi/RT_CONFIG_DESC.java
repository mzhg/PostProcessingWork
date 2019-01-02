package jet.opengl.demos.gpupro.rvi;

import java.util.Arrays;

import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.StringUtils;

final class RT_CONFIG_DESC {
    int firstColorBufferIndex; // index of first render-target to render into
    int numColorBuffers; // number of render-targets to render into
    int numStructuredBuffers; // number of structured-buffers to write into
    DX11_STRUCTURED_BUFFER[] structuredBuffers = new DX11_STRUCTURED_BUFFER[8]; // structured-buffers to write into
    boolean computeTarget; // true, when using corresponding render-target for compute shader

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RT_CONFIG_DESC that = (RT_CONFIG_DESC) o;

        if (firstColorBufferIndex != that.firstColorBufferIndex) return false;
        if (numColorBuffers != that.numColorBuffers) return false;
        if (numStructuredBuffers != that.numStructuredBuffers) return false;
        if (computeTarget != that.computeTarget) return false;
        return CommonUtil.equals(structuredBuffers, that.structuredBuffers);
    }

    @Override
    public int hashCode() {
        int result = firstColorBufferIndex;
        result = 31 * result + numColorBuffers;
        result = 31 * result + numStructuredBuffers;
        result = 31 * result + Arrays.hashCode(structuredBuffers);
        result = 31 * result + (computeTarget ? 1 : 0);
        return result;
    }
}
