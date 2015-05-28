package test;

import inf.Executor;
import inf.Task;
import threadPool.ThreadPool2;

public class ThreadTest {
	class MyTask implements Task {
		String name;

		MyTask(String name) {
			this.name = name;
		}

		@Override
		public void execute() {
			// TODO Auto-generated method stub
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Task" + name + " finished!");
		}

	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ThreadTest test = new ThreadTest();
		ThreadPool2 pool = ThreadPool2.getInstance();
		for(int i=0;i<10;i++){
			Executor executor = (Executor) pool.getExecutor();
			MyTask mytask = test.new MyTask(String.valueOf(i));
			executor.setTask(mytask);
			executor.startTask();
		}
		pool.destroy();
		

	}

}
