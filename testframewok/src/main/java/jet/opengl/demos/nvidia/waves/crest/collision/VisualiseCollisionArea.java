package jet.opengl.demos.nvidia.waves.crest.collision;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.nvidia.waves.crest.MonoBehaviour;
import jet.opengl.postprocessing.util.Rectf;

public class VisualiseCollisionArea extends MonoBehaviour {
    float _objectWidth = 0f;

    SamplingData _samplingData = new SamplingData();

    float[] _resultHeights = new float[s_steps * s_steps];

    static float s_radius = 5f;
    static final int s_steps = 10;

    void Update()
    {
        if (OceanRenderer.Instance == null || OceanRenderer.Instance.CollisionProvider == null)
        {
            return;
        }

        ICollProvider collProvider = OceanRenderer.Instance.CollisionProvider;
        Rectf thisRect = new Rectf(transform.getPositionX() - s_radius * s_steps / 2f, transform.getPositionZ() - s_radius * s_steps / 2f, s_radius * s_steps / 2f, s_radius * s_steps / 2f);
        if (!collProvider.GetSamplingData(thisRect, _objectWidth, _samplingData))
        {
            return;
        }

        Vector3f[] samplePositions = new Vector3f[s_steps * s_steps];
        for (int i = 0; i < s_steps; i++)
        {
            for (int j = 0; j < s_steps; j++)
            {
                samplePositions[j * s_steps + i] = new Vector3f(((i + 0.5f) - s_steps / 2f) * s_radius, 0f, ((j + 0.5f) - s_steps / 2f) * s_radius);
                samplePositions[j * s_steps + i].x += transform.getPositionX();
                samplePositions[j * s_steps + i].z += transform.getPositionZ();
            }
        }

        if (collProvider.RetrieveSucceeded(collProvider.Query(hashCode(), _samplingData, samplePositions, _resultHeights, null, null)))
        {
            for (int i = 0; i < s_steps; i++)
            {
                for (int j = 0; j < s_steps; j++)
                {
                    Vector3f result = samplePositions[j * s_steps + i];
                    result.y = _resultHeights[j * s_steps + i];

                    DebugDrawCross(result, 1f, new Vector3f(0, 1, 0), 0);
                }
            }
        }

        collProvider.ReturnSamplingData(_samplingData);
    }

    public static void DebugDrawCross(Vector3f pos, float r, Vector3f col, float duration)
    {
//        Debug.DrawLine(pos - Vector3.up * r, pos + Vector3.up * r, col, duration);
//        Debug.DrawLine(pos - Vector3.right * r, pos + Vector3.right * r, col, duration);
//        Debug.DrawLine(pos - Vector3.forward * r, pos + Vector3.forward * r, col, duration);
    }
}
