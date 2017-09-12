package nv.visualFX.cloth.libs;

/**
 * Callback interface to manage the DirectX context/device used for compute<p></p>
 * Created by mazhen'gui on 2017/9/12.
 */

public interface DxContextManagerCallback {
    /**
     * Acquire the D3D context for the current thread
     *
     * Acquisitions are allowed to be recursive within a single thread.
     * You can acquire the context multiple times so long as you release
     * it the same count.
     */
    void acquireContext();

    /**
     * Release the D3D context from the current thread
     */
    void releaseContext();

    /**
     * Return if exposed buffers (only cloth particles at the moment)
     * are created with D3D11_RESOURCE_MISC_SHARED_KEYEDMUTEX.
     *
     * The user is responsible to query and acquire the mutex of all
     * corresponding buffers.
     * todo: We should acquire the mutex locally if we continue to
     * allow resource sharing across devices.
     */
    boolean synchronizeResources();
}
