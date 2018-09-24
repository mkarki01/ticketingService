package com.wlabs.ticketing.service;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.wlabs.ticketing.model.SeatHold;
import com.wlabs.ticketing.model.Theater;
import com.wlabs.ticketing.processors.AvailableSeatsProcessor;
import com.wlabs.ticketing.processors.HoldSeatsProcessor;
import com.wlabs.ticketing.processors.ReserveSeatsProcessor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TicketServiceImpl implements TicketService {


	private Theater theater;
	
	private Map<Integer, SeatHold> seatsHoldMap;

	@Override
	public int numSeatsAvailable() {
		int numSeatsAvailable = 0;

		ExecutorService executorService = Executors.newFixedThreadPool(1);
		AvailableSeatsProcessor processor = new AvailableSeatsProcessor(theater, seatsHoldMap);
		final Future<Integer> availableSeatsCalcualtorResult = executorService.submit(processor);
		executorService.shutdown();
		try {
			numSeatsAvailable = availableSeatsCalcualtorResult.get();
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		} catch (ExecutionException e) {
			log.error(e.getMessage());
		}

		return numSeatsAvailable;
	}

	@Override
	public SeatHold findAndHoldSeats(int numSeats, String customerEmail) {
		SeatHold seatHold = null ;

		ExecutorService executorService = Executors.newFixedThreadPool(1);

		final Future<SeatHold> seatHolderResult = executorService.submit(new HoldSeatsProcessor(theater, seatsHoldMap, numSeats, customerEmail));

		executorService.shutdown();
		try {
			seatHold = seatHolderResult.get();
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		} catch (ExecutionException e) {
			log.error(e.getMessage());
		}

		return seatHold;
	}

	@Override
	public String reserveSeats(int seatHoldId, String customerEmail) {
		String confirmationCode = null ;

		ExecutorService executorService = Executors.newFixedThreadPool(1);

		final Future<String> seatReserver = executorService.submit(new ReserveSeatsProcessor(theater, seatsHoldMap, seatHoldId, customerEmail));

		executorService.shutdown();
		try {
			confirmationCode = seatReserver.get();
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		} catch (ExecutionException e) {
			log.error(e.getMessage());
		}

		return confirmationCode;
	}
	
	
}
