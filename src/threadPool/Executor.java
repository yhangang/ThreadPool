package threadPool;

public interface Executor {
	void setTask(Task task);

	Task getTask();

	void startTask();
}
