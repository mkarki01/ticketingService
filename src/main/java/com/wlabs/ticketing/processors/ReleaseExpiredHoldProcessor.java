package com.wlabs.ticketing.processors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.wlabs.ticketing.model.Seat;
import com.wlabs.ticketing.model.Seat.Status;
import com.wlabs.ticketing.model.SeatHold;
import com.wlabs.ticketing.model.Theater;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReleaseExpiredHoldProcessor extends Processor implements Callable<Void> {

	private static final int MAX_HOLD_TIME_ALLOWED_IN_MINUTES = 10;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	public Void call() {

		boolean acquired = lock.writeLock().tryLock();
		List<Seat> seatList = new ArrayList<>();
		if (acquired) {
			try {
				Theater theater = getTheater();
				seatList = theater.getListOfSeats().stream().map(x -> freeIfNeeded(x)).collect(Collectors.toList());
				theater.setListOfSeats(seatList);
				removeExpiredHold();
			} finally {
				lock.writeLock().unlock();
			}
		}

		return null;
	}

	private Seat freeIfNeeded(Seat seat) {
		if (seat.getStatus().equals(Status.HELD)) {
			Instant timeToFreeUp = seat.getHeldTime().plus(MAX_HOLD_TIME_ALLOWED_IN_MINUTES, ChronoUnit.MINUTES);
			if (seat.getStatus().equals(Status.HELD) && Instant.now().isAfter(timeToFreeUp)) {
				seat.setStatus(Status.AVAILABLE);
				log.debug("seatFreed={}{}", seat.getRow(), seat.getCol());

			}
		}
		return seat;
	}

	private void removeExpiredHold() {
		Map<Integer, SeatHold> mp = getSeatsHoldMap();
		if (mp != null) {
			Iterator it = mp.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, SeatHold> seatHoldEntry = (Map.Entry<Integer, SeatHold>) it.next();
				SeatHold seatHold = seatHoldEntry.getValue();
				if (seatHold.getHeldTime() != null) {
					if (Instant.now().isAfter(seatHoldEntry.getValue().getHeldTime()
							.plus(MAX_HOLD_TIME_ALLOWED_IN_MINUTES, ChronoUnit.MINUTES))) {
						{
							it.remove();
						}
					}
				}
			}
		}
	}

	public ReleaseExpiredHoldProcessor(Theater theater, Map<Integer, SeatHold> seatsHoldMap) {
		super(theater, seatsHoldMap);
	}

}