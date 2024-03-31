package com.example.stock.facade;

import com.example.stock.domain.Stock;
import com.example.stock.facade.NamedLockStockFacade;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NamedLockStockFacadeTest {
    @Autowired
    private NamedLockStockFacade optimisticLockStockFacade;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void before() {
        stockRepository.saveAndFlush(new Stock(1L, 100L));
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }

    @Test
    public void 동시에_100개의_요청() throws InterruptedException {
        int threadCount = 100; // 생성할 스레드 개수
        ExecutorService executorService = Executors.newFixedThreadPool(32); // 최대 32개의 스레드를 가질 수 있는 스레드 풀, 동시에 실행할 수 있는 스레드의 최대 개수를 제한한다
        CountDownLatch latch = new CountDownLatch(threadCount); // 특정 횟수만큼의 작업이 완료될 때까지 대기하는데 사용된다

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> { // 작업을 제출한다
                try {
                    optimisticLockStockFacade.decrease(1L, 1L);
                } finally {
                    latch.countDown(); // 작업이 완료되었음을 알린다
                }
            });
        }

        latch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();
        assertEquals(0, stock.getQuantity());
    }
}