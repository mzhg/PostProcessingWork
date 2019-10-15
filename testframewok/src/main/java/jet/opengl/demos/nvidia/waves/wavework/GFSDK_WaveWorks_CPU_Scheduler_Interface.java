package jet.opengl.demos.nvidia.waves.wavework;

/**
 * * This interface can be used to cause WaveWorks CPU simulation tasks to be handled
 * by the client's own scheduler.<p></p>
 *
 * First, note that you will need to provide one instance of this interface per
 * WaveWorks simulation. The semantics of the interface imply a little bit of
 * statefulness which evolves over a simulation cycle, therefore it is necessary
 * to have a unique scheduler object per simulation<p></p>
 *
 * A single simulation cycle consists of:<ul>
 *
 *   <li> one or more 'push' calls to queue the initial simulation tasks
 *
 *   <li> a call to 'kick()', which is a signal that the initial tasks are fully
 *      queued and simulation can commence
 *
 *   <li> scheduler calls 'taskHandler' for each queued task. This *may* result
 *      in further 'push' calls to queue further work
 *
 *   <li> the simulation cycle is complete when a 'taskHandler' call exits and
 *      there are no tasks left in the queue
 *
 *   <li> WaveWorks will either poll or wait for the simulation cycle to complete
 *      via isWorkDone() or waitForWorkDone(), depending on the calls made to
 *      the WaveWorks API by the client</ul>
 *
 * No more than ONE simulation cycle will be scheduled at a time. WaveWorks will
 * wait for the current cycle to complete before attempting to push tasks for a new
 * cycle.<p></p>
 *
 * Created by Administrator on 2017/7/23 0023.
 */

public interface GFSDK_WaveWorks_CPU_Scheduler_Interface {
    // Queue a single item of work
    void push(int taskData);

    // Queue a batch of 'n' items of work
    void push(int[] taskData, int n);

    // Queue a single item of work but insert at a random location in the existing work
    // queue (reason: this gives better perf on some platforms by 'relaxing' the memory bus)
    void pushRandom(int taskData);

    // Wait until the current simulation cycle is out of tasks and all handlers have
    // returned
    void waitForWorkDone();

    // Test whether the current simulation cycle is out of tasks and all handlers have
    // returned
    boolean isWorkDone();

    // Signal the scheduler to begin work on a new simulation cycle
//    typedef void (*ProcessTaskFn)(void* pContext, gfsdk_U32 taskData);

    public interface ProcessTaskFn{
        void process(Object pContext, int taskData);
    }

    boolean kick(ProcessTaskFn taskHandler, Object pContext);
}
