package threadPool;

import inf.Executor;
import inf.Task;

import java.util.LinkedList;

public class ThreadPool2 {
	public static int minPoolSize = 3;

	private boolean closed;
	private LinkedList<Executor> pool;
	private LinkedList<Executor> runningPool;
	private static ThreadPool2 instance;

	/**
	 * 私有化构造函数
	 */
	private ThreadPool2() {
		closed = false;
		pool = new LinkedList<Executor>();
		runningPool = new LinkedList<Executor>();
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
	public static ThreadPool2 getInstance() {
		if (instance == null) {
			synchronized (ThreadPool2.class) {
				instance = new ThreadPool2();
			}
		}
		return instance;
	}

	public void destroy() {
		 synchronized (pool) {
		 closed = true;
		 pool.notifyAll();
		 pool.clear();
		 }
		 synchronized (runningPool) {
			 runningPool.clear();
			 }
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
		synchronized (runningPool) {
			runningPool.addFirst(executor);
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
				synchronized (runningPool) {
					runningPool.remove(this);
				}
			}
		}
	}
}
