package jet.opengl.postprocessing.util;

import java.util.Arrays;

/**
 * Created by mazhen'gui on 2017/5/20.
 */

public class RecycledPool<T> {
    private ObjectCreater<T> m_creater;
    private Object[] dataArray;
    private int m_index;

    public RecycledPool(ObjectCreater<T> creater, int count){
        m_creater = creater;
        dataArray = new Object[count];
    }

    public T obtain(){
        T obj = (T)dataArray[m_index];
        if(obj == null){
            obj = m_creater.newObject();
            dataArray[m_index] = obj;
        }

        m_index = ((++m_index) % dataArray.length);
        return obj;
    }

    public void ensureCapacity(int capacity){
        if(capacity > dataArray.length){
            dataArray = Arrays.copyOf(dataArray, capacity);
        }
    }

    public interface ObjectCreater<T>{
        T newObject();
    }
}
