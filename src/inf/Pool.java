package inf;

public interface Pool {
	Executor getExecutor();

	void destroy();
}
