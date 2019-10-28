package jet.opengl.demos.nvidia.waves.crest.helpers;

import org.lwjgl.util.vector.Transform;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.nvidia.waves.crest.MonoBehaviour;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.shapes.ShapeGerstnerBatched;

/**
 * This script translates all objects in the world to keep the camera near the origin in order to prevent spatial jittering due to limited
 * floating-point precision. The script detects when the camera is further than 'threshold' units from the origin in one or more axes, at which
 * point it moves everything so that the camera is back at the origin. There is also an option to disable physics beyond a certain point. This
 * script should normally be attached to the viewpoint, typically the main camera.
 */
public class FloatingOrigin extends MonoBehaviour {
//    [Tooltip("Use a power of 2 to avoid pops in ocean surface geometry."), SerializeField]
    public float _threshold = 16384f;
//        [Tooltip("Set to zero to disable."), SerializeField]
    float _physicsThreshold = 1000.0f;

        /*[SerializeField]*/ float _defaultSleepThreshold = 0.14f;

//        [Tooltip("Optionally provide a list of transforms to avoid doing a FindObjectsOfType() call."), SerializeField]
    Transform[] _overrideTransformList = null;
//        [Tooltip("Optionally provide a list of particle systems to avoid doing a FindObjectsOfType() call."), SerializeField]
//    ParticleSystem[] _overrideParticleSystemList = null;
//        [Tooltip("Optionally provide a list of rigidbodies to avoid doing a FindObjectsOfType() call."), SerializeField]
//    Rigidbody[] _overrideRigidbodyList = null;
//        [Tooltip("Optionally provide a list of Gerstner components to avoid doing a FindObjectsOfType() call."), SerializeField]
    ShapeGerstnerBatched[] _overrideGerstnerList = null;

//    ParticleSystem.Particle[] _particleBuffer = null;

    void LateUpdate()
    {
        Vector3f newOrigin = new Vector3f();
        if (Math.abs(transform.getPositionX()) > _threshold) newOrigin.x += transform.getPositionX();
        if (Math.abs(transform.getPositionZ()) > _threshold) newOrigin.z += transform.getPositionZ();

        if (!newOrigin.isZero())
        {
            MoveOrigin(newOrigin);
        }
    }

    void MoveOrigin(Vector3f newOrigin)
    {
        MoveOriginTransforms(newOrigin);
        MoveOriginParticles(newOrigin);
        MoveOriginOcean(newOrigin);

        MoveOriginDisablePhysics();
    }

    /// <summary>
    /// Move transforms to recenter around new origin
    /// </summary>
    void MoveOriginTransforms(Vector3f newOrigin)
    {
        Transform[] transforms = (_overrideTransformList != null && _overrideTransformList.length > 0) ? _overrideTransformList : /*FindObjectsOfType<Transform>()*/null;
        for (Transform t : transforms)
        {
            /*if (t.parent == null)
            {
                t.position -= newOrigin;
            }*/
        }
    }

    /// <summary>
    /// Move all particles that are simulated in world space
    /// </summary>
    void MoveOriginParticles(Vector3f newOrigin)
    {
        /*var pss = (_overrideParticleSystemList != null && _overrideParticleSystemList.Length > 0) ? _overrideParticleSystemList : FindObjectsOfType<ParticleSystem>();
        foreach (var sys in pss)
        {
            if (sys.main.simulationSpace != ParticleSystemSimulationSpace.World) continue;

            var particlesNeeded = sys.main.maxParticles;
            if (particlesNeeded <= 0) continue;

            var wasPaused = sys.isPaused;
            var wasPlaying = sys.isPlaying;

            if (!wasPaused)
            {
                sys.Pause();
            }

            // Ensure a sufficiently large array in which to store the particles
            if (_particleBuffer == null || _particleBuffer.Length < particlesNeeded)
            {
                _particleBuffer = new ParticleSystem.Particle[particlesNeeded];
            }

            // Update the particles
            var num = sys.GetParticles(_particleBuffer);
            for (var i = 0; i < num; i++)
            {
                _particleBuffer[i].position -= newOrigin;
            }
            sys.SetParticles(_particleBuffer, num);

            if (wasPlaying)
            {
                sys.Play();
            }
        }*/
    }

    /// <summary>
    /// Notify ocean of origin shift
    /// </summary>
    void MoveOriginOcean(Vector3f newOrigin)
    {
        if (OceanRenderer.Instance!= null)
        {
            /*var fos = OceanRenderer.Instance.GetComponentsInChildren<IFloatingOrigin>();  todo
            foreach (var fo in fos)
            {
                fo.SetOrigin(newOrigin);
            }

            // Gerstner components
            var gerstners = _overrideGerstnerList != null && _overrideGerstnerList.Length > 0 ? _overrideGerstnerList : FindObjectsOfType<ShapeGerstnerBatched>();
            foreach (var gerstner in _overrideGerstnerList)
            {
                gerstner.SetOrigin(newOrigin);
            }*/
        }
    }

    /// <summary>
    /// Disable physics outside radius
    /// </summary>
    void MoveOriginDisablePhysics()
    {
        if (_physicsThreshold > 0f)
        {
            /*var physicsThreshold2 = _physicsThreshold * _physicsThreshold;
            var rbs = (_overrideRigidbodyList != null && _overrideRigidbodyList.Length > 0) ? _overrideRigidbodyList : FindObjectsOfType<Rigidbody>();
            foreach (var rb in rbs)
            {
                if (rb.gameObject.transform.position.sqrMagnitude > physicsThreshold2)
                {
                    rb.sleepThreshold = float.MaxValue;
                }
                else
                {
                    rb.sleepThreshold = _defaultSleepThreshold;
                }
            }*/
        }
    }
}
