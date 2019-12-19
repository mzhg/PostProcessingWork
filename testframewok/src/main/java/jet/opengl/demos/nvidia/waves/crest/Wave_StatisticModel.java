package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Vector2f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;

final class Wave_StatisticModel extends Wave_Simulation_Common_Input {

    private BufferGL m_constantBuffer;
    private ConstantBuffer m_ConstantData = new ConstantBuffer();

    Wave_StatisticModel() {
        super(ShaderManager.getInstance().getProgram("StatisticModel"), null);

        m_constantBuffer = new BufferGL();
        m_constantBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, ConstantBuffer.SIZE, null, GLenum.GL_DYNAMIC_DRAW);
    }

    @Override
    protected void update() {
//        m_ConstantData.m_time =
    }

    private static final class ConstantBuffer {
        static final int SIZE = 20 * 4;

        int m_resolution = 512;
        int m_resolution_plus_one = 513;
        int m_half_resolution = 256;
        int m_half_resolution_plus_one = 257;

        int m_resolution_plus_one_squared_minus_one = 263168;
        int m_32_minus_log2_resolution = 23;

        float m_window_in = 10f;
        float m_window_out = 512;

        final Vector2f m_wind_dir = new Vector2f(-0.8f, -0.6f);
        float m_frequency_scale = 0.89884526f;
        float m_linear_scale = 0.040463652f;

        float m_wind_scale = -0.14142129f;
        float m_root_scale = -8.900197E-10f;
        float m_power_scale = 0;

        float m_time = 0.32f;

        float m_choppy_scale = 1;

        ByteBuffer store(ByteBuffer buf) {
            buf.putInt(m_resolution);
            buf.putInt(m_resolution_plus_one);
            buf.putInt(m_half_resolution);
            buf.putInt(m_half_resolution_plus_one);

            buf.putInt(m_resolution_plus_one_squared_minus_one);
            buf.putInt(m_32_minus_log2_resolution);
            buf.putInt(0);
            buf.putInt(0);

            buf.putFloat(m_window_in);
            buf.putFloat(m_window_out);
            m_wind_dir.store(buf);

            buf.putFloat(m_frequency_scale);
            buf.putFloat(m_linear_scale);
            buf.putFloat(m_wind_scale);
            buf.putFloat(m_root_scale);

            buf.putFloat(m_power_scale);
            buf.putFloat(m_time);
            buf.putFloat(m_choppy_scale);
            buf.putFloat(0);

            return buf;
        }
    }
}
