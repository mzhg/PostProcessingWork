package nv.visualFX.cloth.libs.dx;

import nv.visualFX.cloth.libs.DxContextManagerCallback;

/**
 * acquires cuda context for the lifetime of the instance<p></p>
 * Created by mazhen'gui on 2017/9/12.
 */

class DxContextLock {
    private DxContextManagerCallback mContextManager;

    public DxContextLock(DxFactory factory){
        mContextManager=factory.mContextManager;
        acquire();
    }
    public DxContextLock(DxContextManagerCallback contextManager){
        mContextManager = contextManager;
        acquire();
    }

    public void acquire(){
        mContextManager.acquireContext();
    }

    public void release(){
        mContextManager.releaseContext();
    }

}
