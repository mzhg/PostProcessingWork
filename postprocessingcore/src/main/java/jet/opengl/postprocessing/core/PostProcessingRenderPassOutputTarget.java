package jet.opengl.postprocessing.core;

/**
 * Created by mazhen'gui on 2017/5/11.
 */

public enum PostProcessingRenderPassOutputTarget {
    /** Use the recycled texture(created by the {@link RenderTexturePool}) as the output  */
    DEFAULT,
    /** Use the intenal created texture as the output */
    INTERNAL,
    /** Use the defualt framebuffer as the ouput. */
    SCREEN,
    /** Use the input color texture as the output. */
    SOURCE_COLOR
}
