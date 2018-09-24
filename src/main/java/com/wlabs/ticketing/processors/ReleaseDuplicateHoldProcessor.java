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
public class ReleaseDuplicateHoldProcessor extends Processor implements Callable<Void> {

	private static final int MAX_HOLD_TIME_ALLOWED_IN_MINUTES = 10;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private String email;

	public Void call() {

		boolean acquired = lock.writeLock().tryLock();
		if (acquired) {
			try {
				process();
			} finally {
				lock.writeLock().unlock();
			}
		}

		return null;
	}

	private void process() {
		Map<Integer, SeatHold> mp = getSeatsHoldMap();
		if (mp != null) {
			Iterator it = mp.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, SeatHold> seatHoldEntry = (Map.Entry<Integer, SeatHold>) it.next();
				SeatHold seatHold = seatHoldEntry.getValue();
				if (seatHold != null && seatHold.getEmailAddress().equals(email)) {
					Theater theater = getTheater();
					List<Seat> seatList = theater.getListOfSeats().stream().map((x) -> freeIfNeeded(x, seatHold))
							.collect(Collectors.toList());
					theater.setListOfSeats(seatList);

					it.remove();
				}
			}
		}
	}

	private Seat freeIfNeeded(Seat seat, SeatHold seatHold) {
		for (Seat heldSeat : seatHold.getHeldSeats()) {
			{
				if (heldSeat.getCol() == seat.getCol() && heldSeat.getRow() == seat.getRow()) {
					seat.setStatus(Status.AVAILABLE);
				}
			}
		}
		return seat;
	}

	public ReleaseDuplicateHoldProcessor(Theater theater, Map<Integer, SeatHold> seatsHoldMap, String email) {
		super(theater, seatsHoldMap);
		this.email = email;
	}

}