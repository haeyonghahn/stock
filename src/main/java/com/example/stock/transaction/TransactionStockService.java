package com.example.stock.transaction;

import com.example.stock.service.StockService;

public class TransactionStockService {

	private final StockService stockService;

	public TransactionStockService(StockService stockService) {
		this.stockService = stockService;
	}

	public void decrease(Long id, Long quantity) {
		startTransaction();
		stockService.decrease(id, quantity);
		endTransaction();
	}

	public void startTransaction() {
		System.out.println("Transaction Start");
	}

	public void endTransaction() {
		System.out.println("Commit");
	}
}
