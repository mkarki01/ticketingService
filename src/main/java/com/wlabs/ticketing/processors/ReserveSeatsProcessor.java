package com.wlabs.ticketing.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.wlabs.ticketing.model.Seat;
import com.wlabs.ticketing.model.Seat.Status;
import com.wlabs.ticketing.model.SeatHold;
import com.wlabs.ticketing.model.Theater;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class ReserveSeatsProcessor extends Processor implements Callable<String> {

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	Integer seatHoldId;
	String customerEmail;
	
	public ReserveSeatsProcessor(Theater theater, Map<Integer, SeatHold> seatsHoldMap, Integer seatHoldId, String customerEmail) {
		super(theater, seatsHoldMap);
		this.seatHoldId = seatHoldId;
		this.customerEmail = customerEmail;
	}



	public String call() {

		boolean acquired = lock.writeLock().tryLock();
		List<Seat> seatList = new ArrayList<>();
		if (acquired) {
			try {
				seatList = getTheater().getListOfSeats().stream().map(x -> reserveSeat(x)).collect(Collectors.toList());
				this.getTheater().setListOfSeats(seatList);
				this.getSeatsHoldMap().remove(seatHoldId);
			} finally {
				lock.writeLock().unlock();
			}
		}
		return UUID.randomUUID().toString();
	}

	public Seat reserveSeat(Seat seat) {
		SeatHold seatHold = this.getSeatsHoldMap().get(seatHoldId);
		for (Seat heldSeat : seatHold.getHeldSeats()) {
			if (heldSeat.getRow() == seat.getRow() && heldSeat.getCol() == seat.getCol()) {
				seat.setStatus(Status.RESERVED);
			}
		}
		return seat;
	}

}
