package jet.opengl.demos.intel.cput;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/11/15.
 */

public interface ID3D11InputLayout {

    void bind();

    void unbind();

    static ID3D11InputLayout createInputLayoutFrom(D3D11_INPUT_ELEMENT_DESC[] descs){
        final int count = descs.length;
        final int[] types = new int[count];
        final int[] sizes = new int[count];
        final int[] offsets = new int[count];
        int strideInBytes = 0;

        for(int i = 0; i < count; i++){
            offsets[i] = strideInBytes;

            D3D11_INPUT_ELEMENT_DESC desc = descs[i];
            switch (desc.Format){
                case GLenum.GL_RGB32F:
                    types[i] = GLenum.GL_FLOAT;
                    sizes[i] = 3;
                    strideInBytes += 3 * 4;
                    break;
                case GLenum.GL_RGBA8:
                    types[i] = GLenum.GL_UNSIGNED_BYTE;
                    sizes[i] = 4;
                    strideInBytes += 4;
                    break;
                case GLenum.GL_RGBA32F:
                    types[i] = GLenum.GL_FLOAT;
                    sizes[i] = 4;
                    strideInBytes += 4 * 4;
                    break;
                case GLenum.GL_RG32F:
                    types[i] = GLenum.GL_FLOAT;
                    sizes[i] = 2;
                    strideInBytes += 2 * 4;
                    break;
                case GLenum.GL_R32UI:
                    types[i] = GLenum.GL_UNSIGNED_INT;
                    sizes[i] = 1;
                    strideInBytes += 4;
                    break;
                case GLenum.GL_RG16F:
                    types[i] = GLenum.GL_HALF_FLOAT;
                    sizes[i] = 2;
                    strideInBytes += 2 * 2;
                    break;
                case GLenum.GL_R32F:
                    types[i] = GLenum.GL_FLOAT;
                    sizes[i] = 1;
                    strideInBytes += 4;
                    break;
                case GLenum.GL_NONE:
                    break;
                default:
                    throw new IllegalArgumentException("Unkown format: " + Integer.toHexString(desc.Format));
            }
        }

        int finalStrideInBytes = strideInBytes;
        return new ID3D11InputLayout() {
            final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            @Override
            public void bind() {
                for(int i = 0; i < count; i++){
                    if(types[i] == GLenum.GL_UNSIGNED_BYTE){
                        gl.glVertexAttribPointer(i, sizes[i], types[i], true, finalStrideInBytes, offsets[i]);
                    }else if (types[i] == GLenum.GL_FLOAT){
                        gl.glVertexAttribPointer(i, sizes[i], types[i], false, finalStrideInBytes, offsets[i]);
                    }else {
                        throw new IllegalArgumentException("Unsupport type: " + Integer.toHexString(types[i]));
                    }

                    gl.glEnableVertexAttribArray(i);
                }
            }

            @Override
            public void unbind() {
                for(int i = 0; i < count; i++){
                    gl.glDisableVertexAttribArray(i);
                }
            }
        };
    }
}
