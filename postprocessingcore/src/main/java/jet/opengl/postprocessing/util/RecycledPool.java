package jet.opengl.postprocessing.util;

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

    public interface ObjectCreater<T>{
        T newObject();
    }
}
