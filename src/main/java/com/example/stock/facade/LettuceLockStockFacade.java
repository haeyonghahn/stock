package com.example.stock.facade;

import org.springframework.stereotype.Component;

import com.example.stock.repository.RedisLockRepository;
import com.example.stock.service.StockService;

@Component
public class LettuceLockStockFacade {

	private final RedisLockRepository redisLockRepository;
	private final StockService stockService;

	public LettuceLockStockFacade(RedisLockRepository redisLockRepository, StockService stockService) {
		this.redisLockRepository = redisLockRepository;
		this.stockService = stockService;
	}

	public void decrease(Long id, Long quantity) throws InterruptedException {
		/**
		 * lock 획득에 실패했다면 Thread Sleep을 이용해서
		 * 100ms 텀을 주고 lock 획득을 재시도
		 * 이렇게 해야 redis에 갈 수 있는 부하를 줄여줄 수 있다.
		 */
		while (!redisLockRepository.lock(id)) {
			Thread.sleep(100);
		}

		/**
		 * 로직이 모두 종료되었다면 lock을 해제한다.
		 */
		try {
			stockService.decrease(id, quantity);
		} finally {
			redisLockRepository.unlock(id);
		}
	}
}
