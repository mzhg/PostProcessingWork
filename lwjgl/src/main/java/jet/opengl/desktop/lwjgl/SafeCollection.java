package jet.opengl.desktop.lwjgl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class SafeCollection<E> implements Collection<E>{

	Collection<E> data;
	ArrayList<E> added = new ArrayList<E>();
	ArrayList<E> removed = new ArrayList<E>();
	transient boolean locked;
	
	public SafeCollection(Collection<E> list){
		if(list == null)
			throw new NullPointerException("list is null.");
		this.data = list;
	}
	
	protected void lock(){ locked = true; System.out.println("begin lock");}
	protected void unlock() {locked = false; System.out.println("release lock");}
	public boolean isLocked() {return locked; }
	
	@Override
	public int size() {
		return data.size() + added.size() - removed.size();
	}
	
	protected synchronized void flush(){
		if(added.size() > 0 ) {data.addAll(added); added.clear();}
		if(removed.size() > 0) {data.removeAll(removed); removed.clear();}
	}

	@Override
	public boolean isEmpty() {
		// need more pricise.
		return size() == 0;
	}

	@Override
	public boolean contains(Object o) {
		return data.contains(o) || added.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		if(locked) throw new DataLockedException();
		flush();
		return new IteratorDeletor<E>(data.iterator());
	}

	@Override
	public Object[] toArray() {
		if(locked) throw new DataLockedException();
		flush();
		
		return data.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		if(locked) throw new DataLockedException();
		flush();
		
		return data.toArray(a);
	}

	@Override
	public boolean add(E e) {
		if(locked){
//			removed.remove(e);
			return added.add(e);
		}else{
			flush();
			return data.add(e);
		}
	}

	// It takes O(n) time in worse case on list-based structure.
	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object o) {
		if(locked){
			boolean b = added.remove(o);
			if(!b && data.contains(o)){
				removed.add((E)o);
				return true;
			}else if(b)
				return true;
			else
				return false;  // not actual right.
		}else{
			flush();
			return data.remove(o);
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for(Object o : c){
			boolean contained = data.contains(o) || added.contains(o);
			if(!contained)
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if(locked){
//			removed.remove(e);
		    return added.addAll(c);
		}else{
			flush();
			return data.addAll(c);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean removeAll(Collection<?> c) {
		if(locked){
			boolean result = true;
			for(Object o : c){
				boolean b = added.remove(o);
				if(!b && data.contains(o)){
					removed.add((E)o);
				}else if(!b){
					result = false;
				}
			}
			
			return result;
		}else{
			flush();
			
			return data.removeAll(c);
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if(locked) throw new DataLockedException();
		flush();
		
		return data.retainAll(c);
	}

	@Override
	public void clear() {
		if(locked) throw new DataLockedException();
		
		data.clear();
		added.clear();
		removed.clear();
	}
	
	public static void main(String[] args) {
		SafeCollection<Integer> list = new SafeCollection<Integer>(new LinkedList<Integer>());
		for(int i = 0; i < 10; i++){
			list.add(i);
		}
		
		System.out.println("prepare to iterator the collections.");
		for(Integer i : list){
			System.out.println("iterator: " + i);
			
			if(Math.random() < 0.5)
				list.add(i + 100);
		}
		
		System.out.println();
		System.out.println("prepare to iterator the collections2.");
		for(Integer i : list){
			System.out.println("iterator: " + i);
		}
		
		for(Integer i : list){
			if(Math.random() < 0.5){
				System.out.println("removed: " + i);
				list.remove(i);
			}
		}
		
		System.out.println();
		System.out.println("prepare to iterator the collections3.");
		for(Integer i : list){
			System.out.println("iterator: " + i);
		}
	}
	
	@SuppressWarnings("hiding")
	private final class IteratorDeletor<E> implements Iterator<E>{
		Iterator<E> instance;
		
		public IteratorDeletor(Iterator<E> instance) {
			this.instance = instance;
			
			lock();
		}
		
		@Override
		public boolean hasNext() {
		    boolean b = instance.hasNext();
		    if(!b) unlock();
		    
		    return b;
		}

		@Override
		public E next() {
			return instance.next();
		}

		@Override
		public void remove() {
			instance.remove();
		}
	}

}
