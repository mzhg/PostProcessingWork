package assimp.common;

import java.nio.FloatBuffer;

import org.lwjgl.util.vector.Vector3f;

final class BufferPerformanceTest {
	
	static final int N = 2000000;

	public static void main(String[] args) {
		FloatBuffer tmp = MemoryUtil.createFloatBuffer(1, true);
		tmp = MemoryUtil.createFloatBuffer(1, false);
		tmp.put(0);
		new Vector3f();
		System.out.println("prepare to begin!");
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		timeMeasure("vecAgainstVec", vecAgainstVec);
		timeMeasure("arrAgainstArr", arrAgainstArr);
		timeMeasure("heapAgainstHeap", heapAgainstHeap);
		timeMeasure("nativeAgainstNative", nativeAgainstNative);
	}
	
	static void timeMeasure(String msg, Runnable task){
		long startTime = System.currentTimeMillis();
		task.run();
		System.out.println(msg + " Consume time: " + (System.currentTimeMillis() - startTime) * 0.001);
		System.gc();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}
	
	private static final Runnable vecAgainstVec = new Runnable() {
		@Override
		public void run() {
			FloatBuffer result = MemoryUtil.createFloatBuffer(N * 3, true);
			Vector3f dest = new Vector3f();
			Vector3f[] left = new Vector3f[N];
			Vector3f[] right = new Vector3f[N];
			long heapMem = Runtime.getRuntime().freeMemory();
			long startTime = System.currentTimeMillis();
			for(int i = 0; i < N;i++){
				left[i] = new Vector3f(random(), random(), random());
				right[i] = new Vector3f(random(), random(), random());
			}
			
			System.out.println("Allocate vectors consume time: " + (System.currentTimeMillis() - startTime) * 0.001);
			
			for(int i = 0; i < N; i++){
				op(left[i], right[i], dest);
				dest.store(result);
			}
			result.flip();
			System.out.println("vecAgainstVec taken momery: " + (heapMem - Runtime.getRuntime().freeMemory())/1024l + "KB");
		}
	};
	
	private static final Runnable arrAgainstArr = new Runnable() {
		@Override
		public void run() {
			long heapMem = Runtime.getRuntime().freeMemory();
			FloatBuffer result = MemoryUtil.createFloatBuffer(N * 3, true);
			Vector3f dest = new Vector3f();
//			Vector3f[] left = new Vector3f[N];
//			Vector3f[] right = new Vector3f[N];
			
			float[] left = new float[N * 3];
			float[] right = new float[N * 3];
			for(int i = 0; i < left.length; i++){
				left[i] = random();
				right[i] = random();
			}
			
			Vector3f l =new Vector3f();
			Vector3f r =new Vector3f();
			for(int i = 0; i < N;i++){
//				left[i] = new Vector3f(random(), random(), random());
//				right[i] = new Vector3f(random(), random(), random());
				int index = 3 * i;
				l.x = left[index];
				l.y = left[index + 1];
				l.z = left[index + 2];
				
				r.x = right[index];
				r.y = right[index + 1];
				r.z = right[index + 2];
				op(l, r, dest);
				dest.store(result);
			}
			result.flip();
			System.out.println("arrAgainstArr taken momery: " + (heapMem  -Runtime.getRuntime().freeMemory())/1024.0 + "KB");
		}
	};
	
	private static final Runnable nativeAgainstNative = new Runnable() {
		@Override
		public void run() {
			long heapMem = Runtime.getRuntime().freeMemory();
			FloatBuffer result = MemoryUtil.createFloatBuffer(N * 3, true);
			FloatBuffer right = MemoryUtil.createFloatBuffer(N * 3, true);
			FloatBuffer left = MemoryUtil.createFloatBuffer(N * 3, true);
			Vector3f dest = new Vector3f();
			Vector3f l =new Vector3f();
			Vector3f r =new Vector3f();
			while(left.remaining() > 0){
				right.put(random());
				left.put(random());
			}
			
			right.flip();
			left.flip();
			
			for(int i = 0; i < N;i++){
				l.load(left);
				r.load(right);
				op(l, r, dest);
				
				dest.store(result);
			}
			right.flip();
			left.flip();
			result.flip();
			System.out.println("nativeAgainstNative taken momery: " + (heapMem - Runtime.getRuntime().freeMemory())/1024f + "KB");
		}
	};
	
	private static final Runnable heapAgainstHeap = new Runnable() {
		@Override
		public void run() {
			long heapMem = Runtime.getRuntime().freeMemory();
			FloatBuffer result = MemoryUtil.createFloatBuffer(N * 3, true);
			FloatBuffer right = MemoryUtil.createFloatBuffer(N * 3, false);
			FloatBuffer left = MemoryUtil.createFloatBuffer(N * 3, false);
			Vector3f dest = new Vector3f();
			Vector3f l =new Vector3f();
			Vector3f r =new Vector3f();
			while(left.remaining() > 0){
				right.put(random());
				left.put(random());
			}
			
			right.flip();
			left.flip();
			
			for(int i = 0; i < N;i++){
				l.load(left);
				r.load(right);
				op(l, r, dest);
				
				dest.store(result);
			}
			right.flip();
			left.flip();
			result.flip();
			System.out.println("heapAgainstHeap taken momery: " + (heapMem - Runtime.getRuntime().freeMemory())/1024f + "KB");
		}
	};
	
	static float random(){ return (float)Math.random();}
	
	static void op(Vector3f left, Vector3f right, Vector3f dest){
		Vector3f.cross(left, right, dest);
	}
	
}
