package myThreadPool;

/*
 * ThreadPool.java   JDK1.5以上版本
 */
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/*
 * 线程池没有设置最小线程数,因为线程如果在m_keepAliveTime秒内没有接收到任务,线程就自动终止
 * 线程池调用shutdown后线程池资源已释放，如果再调用此实例的方法则会重新创建线一个程池
 */
public class MyThreadPool {
	// 分配任务的线程
	private Thread m_thread = null;
	// 当前线程数量
	private int m_curPoolSize = 0;
	// 最大线程数量(尽量不要超过最大连接数)
	private int m_maxPoolSize = 50;
	// 终止前多余的空闲线程等待新任务的最长时间(秒)
	private int m_keepAliveTime = 600; // 10分钟
	// 最大限制数量设置为200
	private static final int m_maxLimitedNum = 200;
	// 单例模式的线程池
	private static MyThreadPool m_threadPool = null;
	// 任务队列
	private LinkedBlockingQueue<Runnable> m_taskQueue = new LinkedBlockingQueue<Runnable>();
	// 空闲线程队列
	private LinkedBlockingQueue<WorkThread> m_idleQueue = new LinkedBlockingQueue<WorkThread>();
	// 繁忙线程队列
	private ConcurrentLinkedQueue<WorkThread> m_busyQueue = new ConcurrentLinkedQueue<WorkThread>();
	// log日志对象
	private static Logger m_logger = Logger.getLogger(MyThreadPool.class);

	/**
	 * 只允许一个实例
	 */
	private MyThreadPool() {
		// 启动任务分配线程
		m_thread = new Thread(new Runnable() {
			// 执行任务线程
			private WorkThread m_taskThread = null;

			public void run() {
				// 任务分配
				while (true) {
					try {
						// 获取队头任务
						Runnable task = m_taskQueue.take();
						// 获取空闲线程
						m_taskThread = null;
						if (!m_idleQueue.isEmpty()
								|| getCurPoolSize() >= getMaxPoolSize()) {
							m_taskThread = m_idleQueue.take();
						} else {
							m_taskThread = new WorkThread();
							m_taskThread.start();
							synchronized (this) {
								m_curPoolSize++;
							}
						}
						// 将线程加入繁忙队列并执行任务
						m_busyQueue.offer(m_taskThread);
						m_taskThread.execute(task);
					} catch (InterruptedException e) {
						break; // 线程中断则停止分配任务并释放线程池
					} catch (Exception e) {
						m_logger.error("任务分配发生异常:" + e.getMessage());
					}
				}

				// 终止繁忙线程池的线程
				while ((m_taskThread = m_busyQueue.poll()) != null) {
					try {
						m_taskThread.interrupt();
						m_taskThread.join();
						m_logger.info("线程" + m_taskThread.getId() + "已终止");
					} catch (Exception e) {
						m_logger.error("线程" + m_taskThread.getId() + "终止异常:"
								+ e.getMessage());
					}
				}

				// 终止空闲线程池的线程
				while ((m_taskThread = m_idleQueue.poll()) != null) {
					try {
						m_taskThread.interrupt();
						m_taskThread.join();
						m_logger.info("线程" + m_taskThread.getId() + "已终止");
					} catch (Exception e) {
						m_logger.error("线程" + m_taskThread.getId() + "终止异常:"
								+ e.getMessage());
					}
				}

				m_logger.info("线程" + Thread.currentThread().getId() + "已终止");
			}
		});
		m_thread.start();
	}

	/**
	 * 单例模式
	 * 
	 * @return 线程池对象
	 */
	public static MyThreadPool getInstance() {
		if (m_threadPool == null) {
			synchronized (MyThreadPool.class) {
				if (m_threadPool == null) {
					m_threadPool = new MyThreadPool();
				}
			}
		}
		return m_threadPool;
	}

	/**
	 * 设置最大线程数量
	 * 
	 * @param maxSize
	 *            最大线程池数量
	 */
	public synchronized void setMaxPoolSize(int maxSize) {
		m_maxPoolSize = maxSize > 0 ? (maxSize > m_maxLimitedNum ? m_maxLimitedNum
				: maxSize)
				: 0;
	}

	/**
	 * 获取最大线程数量
	 * 
	 * @return 最大线程数量
	 */
	public synchronized int getMaxPoolSize() {
		return m_maxPoolSize;
	}

	/**
	 * 获取当前线程数量
	 * 
	 * @return 当前线程数量
	 */
	public synchronized int getCurPoolSize() {
		return m_curPoolSize;
	}

	/**
	 * 执行Runnable任务
	 * 
	 * @param task
	 *            任务
	 */
	public void execute(Runnable task) {
		try {
			m_taskQueue.put(task);
		} catch (InterruptedException e) {
			m_logger.error("执行任务<" + task.getClass().getName() + ">被中断:"
					+ e.getMessage());
		}
	}

	/**
	 * 关闭线程池，释放线程资源
	 */
	public synchronized void shutdown() {
		// 中断分配任务线程
		try {
			m_thread.interrupt();
			m_thread.join();
		} catch (InterruptedException e) {
			m_logger.error("线程" + m_thread.getId() + "已终止");
		}

		// 单例线程池置空
		m_threadPool = null;
	}

	/**
	 * 内部工作线程类
	 */
	protected class WorkThread extends Thread {
		// 线程执行的任务
		private LinkedBlockingQueue<Runnable> m_task = new LinkedBlockingQueue<Runnable>();

		/**
		 * 提交任务给线程
		 * 
		 * @param task
		 *            任务
		 */
		public void execute(Runnable task) {
			try {
				m_task.put(task);
			} catch (InterruptedException e) {
				m_logger.error("执行任务<" + task.getClass().getName() + ">被中断:"
						+ e.getMessage());
			}
		}

		/**
		 * 执行任务
		 */
		public void run() {
			while (true) {
				Runnable task = null;
				try {
					// 获取定时任务(m_keepAliveTime内无任务线程自动终止)
					task = m_task.poll(m_keepAliveTime, TimeUnit.SECONDS);
				} catch (Exception e) {
					break; // 异常则线程退出
				}

				// 任务为空则释放线程
				if (task == null) {
					break;
				}

				try {
					// 执行线程任务
					task.run();
				} catch (Exception e) {
					// 任务执行异常输出日志
					m_logger.error("任务<" + task.getClass().getName() + ">执行异常:"
							+ e.getMessage());
				}
				// 将线程从繁忙队列移除
				m_busyQueue.remove(this);
				// 将线程移到空闲队列
				m_idleQueue.offer(this);
			}

			synchronized (this) {
				m_curPoolSize--;
			}
			// 从队列中移除此线程
			m_busyQueue.remove(this);
			m_idleQueue.remove(this);
			this.interrupt();
		}
	}

	public static void main(String argv[]) {

		try {
			m_logger.info("--------------------");
			MyThreadPool.getInstance().execute(new Thread() {
				@Override
				public void run() {
					System.out.println("啦啦啦！");
				}
			});
			MyThreadPool.getInstance().execute(new Thread() {
				@Override
				public void run() {
					System.out.println("噢噢噢！");
				}
			});
			MyThreadPool.getInstance().execute(new Thread() {
				@Override
				public void run() {
					System.out.println("哦哦哦！");
				}
			});

			Thread.sleep(3 * 1000);
			m_logger.info(MyThreadPool.getInstance().getCurPoolSize());
			// MyThreadPool.getInstance().shutdown();
			m_logger.info("--------------------");

		} catch (Exception e) {
			m_logger.info("异常:" + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("over");
	}
}
