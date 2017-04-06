package com.nvidia.developer.opengl.app;

/**
 * Basic platform functionality interface.
 * <p>
 * Interface class representing basic platform functionalities. This is merely
 * the interface; the application framework must implement the
 * platform-dependent functionalities along with a method of retrieving an
 * instance
 *
 * @author Nvidia 2014-9-12 17:58
 *
 */
public interface NvGLAppContext {
    /**
     * Set the swap interval if supported by the platform.
     *
     * @param interval
     *            the number of VSYNCs to wait between swaps (0 == never wait)
     * @return true if the platform can support swap interval, false if not
     */
    public boolean setSwapInterval(int interval);

    /**
     * Surface width.
     *
     * @return the surface width in pixels
     */
    public int width();

    /**
     * Surface height.
     *
     * @return the surface height in pixels
     */
    public int height();

    /**
     * The selected [E]GL configuration.
     *
     * @return the selected configuration information for the platform
     */
    public NvEGLConfiguration getConfiguration();

    /**
     * Request exit.<br>
     * Function to allow the app to request to exit.<br>
     * Applications should NOT assume that they will immediately exit.
     * Applications cannot stop processing events in their mainloop until
     * #isAppRunning returns false.
     */
    public void requestExit();

    /**
     * Set app title.
     * <p>
     * Allows an application to set the title to be used by the platform and
     * framework as needed.
     *
     * @param title
     *            a string containing a user-readable application name
     */
    public void setAppTitle(String title);

    public void showDialog(String msg, String errorStr);

    /**
     * Get the app title name.
     * @return
     */
    public String getAppTitle();
}
