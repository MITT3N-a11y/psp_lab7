package by.charity.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ThreadPoolManager {
    private static final Logger logger = Logger.getLogger(ThreadPoolManager.class.getName());
    private static ThreadPoolManager instance;

    private ExecutorService executorService;

    private ThreadPoolManager() {}


    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    public void init(int poolSize) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
        logger.info("Пул потоков инициализирован, размер: " + poolSize);
    }

    public void submit(Runnable task) {
        executorService.submit(task);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Пул потоков остановлен");
    }
}