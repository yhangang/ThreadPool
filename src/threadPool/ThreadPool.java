package threadPool;

import inf.Executor;
import inf.Task;

import java.util.LinkedList;

public class ThreadPool {
	public static int minPoolSize = 3;

	private boolean closed;
	private LinkedList<Executor> pool;
	private static ThreadPool instance;

	/**
	 * 私有化构造函数
	 */
	private ThreadPool() {
		closed = false;
		pool = new LinkedList<Executor>();
		for (int i = 0; i < minPoolSize; i++) {
			Executor executor = new ExecutorImpl();
			pool.add(executor);
			((ExecutorImpl) executor).start();
		}
		System.out.println("线程池建立，线程数量：" + pool.size());
	}

	/**
	 * 单例模式
	 * 
	 * @return
	 */
	public static ThreadPool getInstance() {
		if (instance == null) {
			synchronized (ThreadPool.class) {
				instance = new ThreadPool();
			}
		}
		return instance;
	}

	public void destroy() {
//		 synchronized (pool) {
//		 closed = true;
//		 pool.notifyAll();
//		 pool.clear();
//		 
//		 }
	}

	public Executor getExecutor() {
		Executor executor = null;
		synchronized (pool) {
			while (pool.size() < 1) {
				try {
					pool.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			executor = (Executor) pool.removeFirst();
			System.out.println("获取线程，剩余线程数量：" + pool.size());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return executor;
	}

	private class ExecutorImpl extends Thread implements Executor {
		private Task task;
		private Object lock = new Object();
		

		public ExecutorImpl() {
		}

		public Task getTask() {
			return task;
		}

		public void setTask(Task task) {
			this.task = task;
		}

		public void startTask() {
			synchronized (lock) {
				lock.notify();
			}
		}

		public void run() {
			while (!closed) {
				synchronized (lock) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				getTask().execute();
				synchronized (pool) {
					pool.addFirst(this);
					pool.notifyAll();
					System.out.println("归还线程，剩余线程数量：" + pool.size());
				}
			}
		}
	}
}
