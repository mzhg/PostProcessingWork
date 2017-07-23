package jet.opengl.demos.nvidia.waves;

/**
 *  Just like any other GL client, WaveWorks must use a GL texture unit each time it binds a texture
 * as a shader input. The app therefore needs to reserve a small pool of texture units for WaveWorks
 * to use when setting simulation state for GL rendering, in order to avoid clashes with the app's
 * own simultaneous use of texture units. All the slots in the pool must be allocated with valid
 * zero-based GL texture unit indices, without repeats or clashes.<p></p>
 *
 * There is no particular requirement for the contents of the pool to be consistent from one invocation
 * to the next. The app just needs to ensure that it does not use any of the texture units in the
 * pool for as long as the graphics state set by WaveWorks is required to persist.<p></p>
 *
 * The actual amount of reserved texture units depends on whether the library was set up to use OpenGL
 * Texture Arrays or not, and can be queried using GFSDK_WaveWorks_Simulation_GetTextureUnitCountGL2()<p></p>

 * Created by Administrator on 2017/7/23 0023.
 */

public class GFSDK_WaveWorks_Simulation_GL_Pool {
    public static final int MaxNumReservedTextureUnits=8;

    public final int[] Reserved_Texture_Units=new int[MaxNumReservedTextureUnits];
}
