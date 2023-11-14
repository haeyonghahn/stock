package com.example.stock.facade;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.example.stock.service.StockService;

@Component
public class RedissonLockStockFacade {

	private final RedissonClient redissonClient;
	private final StockService stockService;

	public RedissonLockStockFacade(RedissonClient redissonClient, StockService stockService) {
		this.redissonClient = redissonClient;
		this.stockService = stockService;
	}

	public void decrease(Long id, Long quantity) {
		RLock lock = redissonClient.getLock(id.toString());

		try {
			/**
			 * 몇 초동안 lock 획득을 시도할 것인지,
			 * 그리고 몇 초 동안 점유할 것인지 설정을 해준다.
			 */
			boolean available = lock.tryLock(35, 1, TimeUnit.SECONDS);

			if (!available) {
				System.out.println("lock 획득 실패");
				return;
			}

			stockService.decrease(id, quantity);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
