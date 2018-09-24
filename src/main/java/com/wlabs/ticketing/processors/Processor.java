package com.wlabs.ticketing.processors;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.wlabs.ticketing.model.SeatHold;
import com.wlabs.ticketing.model.Theater;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
@AllArgsConstructor
public abstract class Processor {

	protected Theater theater;

	protected Map<Integer, SeatHold> seatsHoldMap;

	protected Map<Integer, SeatHold> getSeatsHoldMap() {
		return this.seatsHoldMap;
	}

	protected Theater getTheater() {
		return this.theater;
	}

	public void freeUpHeldSeats(Map<Integer, SeatHold> holdmap) {
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		final Future<Void> releaserResult = executorService.submit(new ReleaseExpiredHoldProcessor(theater, holdmap));
		executorService.shutdown();
		try {
			releaserResult.get();
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		} catch (ExecutionException e) {
			log.error(e.getMessage());
		}
	}

}
