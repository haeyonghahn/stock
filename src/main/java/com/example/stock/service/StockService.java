package com.example.stock.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;

@Service
public class StockService {

	private final StockRepository stockRepository;

	public StockService(StockRepository stockRepository) {
		this.stockRepository = stockRepository;
	}

	// @Transactional
	public synchronized void decrease(Long id, Long quantity) {
		// Stock 조회
		// 재고를 감소한뒤
		// 갱신된 값을 저장하도록 한다.
		Stock stock = stockRepository.findById(id).orElseThrow();
		stock.decrease(quantity);

		stockRepository.saveAndFlush(stock);
	}
}
