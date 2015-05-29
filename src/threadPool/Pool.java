package threadPool;

public interface Pool {
	Executor getExecutor();

	void destroy();
}
