package com.wlabs.ticketing.processors;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.wlabs.ticketing.model.Seat;
import com.wlabs.ticketing.model.Seat.Status;
import com.wlabs.ticketing.model.SeatHold;
import com.wlabs.ticketing.model.Theater;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AvailableSeatsProcessor extends Processor implements Callable<Integer> {

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	public AvailableSeatsProcessor(Theater theater, Map<Integer, SeatHold> seatsHoldMap) {
		super(theater, seatsHoldMap);
	}
	

	public Integer call() {

		boolean acquired = lock.readLock().tryLock();
		int count = 0;
		if (acquired) {
			try {
				freeUpHeldSeats(seatsHoldMap);
				List<Seat> seatList = getTheater().getListOfSeats();
				count = Math.toIntExact(seatList.stream().filter(x -> x.getStatus().equals(Status.AVAILABLE)).count());
			} finally {
				lock.readLock().unlock();
			}
		}
		return count;
	}
}
