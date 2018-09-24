package com.wlabs.ticketing.processors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
public class HoldSeatsProcessor extends Processor implements Callable<SeatHold> {

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	int requestedSeatCount = 0;
	String requestEmail = "";

	public HoldSeatsProcessor(Theater theater, Map<Integer, SeatHold> seatsHoldMap, int requestedSeatCount, String requestEmail) {
		super(theater, seatsHoldMap);
		this.requestedSeatCount = requestedSeatCount;
		this.requestEmail = requestEmail;
	}

	@Override
	public SeatHold call() throws Exception {

		removeAnyPreviousHoldForSameEmail();

		int numSeatsAvailable = getNumAvailablSeats();
		log.debug("numberOfAvailableSeats={}", numSeatsAvailable);
		SeatHold seatHold = null;
		ExecutorService executorService = Executors.newFixedThreadPool(1);

		final Future<Void> releaseSeatsProcessorResult = executorService.submit(new ReleaseExpiredHoldProcessor(super.getTheater(), super.getSeatsHoldMap()));
		releaseSeatsProcessorResult.get();

		final Future<Integer> availableSeatsProcessorResult = executorService.submit(new AvailableSeatsProcessor(super.getTheater(), super.getSeatsHoldMap()));
		executorService.shutdown();
		try {
			numSeatsAvailable = availableSeatsProcessorResult.get();
			if (numSeatsAvailable >= requestedSeatCount) {
				seatHold = getSeatHold();
			}
			else
			{
				throw new NotEnoughSeatsFoundException();
			}
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		} catch (ExecutionException e) {
			log.error(e.getMessage());
		}

		return seatHold;
	}
	private void removeAnyPreviousHoldForSameEmail() {
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		final Future<Void> releaseDuplicteHoldProcessorResult = executorService.submit(new ReleaseDuplicateHoldProcessor(super.getTheater(), super.getSeatsHoldMap(), requestEmail));
		executorService.shutdown();
		try {
			releaseDuplicteHoldProcessorResult.get();
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		} catch (ExecutionException e) {
			log.error(e.getMessage());
		}
	}

	private int getNumAvailablSeats() {
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		final Future<Integer> availableSeatsCalcualtorResult = executorService.submit(new AvailableSeatsProcessor(super.getTheater(), super.getSeatsHoldMap()));
		executorService.shutdown();
		try {
			int availableSeats = availableSeatsCalcualtorResult.get().intValue();
			log.debug("availableSeats={}", availableSeats);
			return availableSeats;

		} catch (InterruptedException e) {
			log.error(e.getMessage());
		} catch (ExecutionException e) {
			log.error(e.getMessage());
		}
		return 0;
	}

	private SeatHold getSeatHold() throws NotEnoughSeatsFoundException{
		List<Seat> heldSeats = new ArrayList<>();
		boolean acquired = lock.writeLock().tryLock();
		if (acquired) {
			try {
				List<Seat> listOfSeats = getTheater().getListOfSeats();
				List<Seat> carryOver = new ArrayList<>();
				int startingSeat = 0;

				do {
					List<Seat> singleRow = listOfSeats.stream().skip(startingSeat)
							.limit(getTheater().getNumberOfColumns()).collect(Collectors.toList());
					int halfWay = (int) Math.ceil((double) getTheater().getNumberOfRows() / 2);
					List<Seat> rightHalf = listOfSeats.stream().skip(halfWay).limit(halfWay)
							.collect(Collectors.toList());
					long availableSeatsOnLeftHalf = 0;
					long availableSeatsOnRightHalf = rightHalf.stream()
							.filter(x -> x.getStatus().equals(Status.AVAILABLE)).count();
					if (requestedSeatCount <= availableSeatsOnRightHalf) {
						log.debug("found {} seats on right side", requestedSeatCount);
						heldSeats = rightHalf.stream().filter(x -> x.getStatus().equals(Status.AVAILABLE))
								.limit(requestedSeatCount).map(x -> holdAndGet(x)).collect(Collectors.toList());
					} else {
						List<Seat> leftHalf = listOfSeats.stream().skip(0).limit(halfWay).collect(Collectors.toList());
						availableSeatsOnLeftHalf = leftHalf.stream().filter(x -> x.getStatus().equals(Status.AVAILABLE))
								.count();
						if (requestedSeatCount <= availableSeatsOnLeftHalf) {
							log.debug("found {} seats on left side", requestedSeatCount);
							heldSeats = leftHalf.stream().filter(x -> x.getStatus().equals(Status.AVAILABLE))
									.limit(requestedSeatCount).map(x -> holdAndGet(x)).collect(Collectors.toList());
						} else {
							if (requestedSeatCount <= (availableSeatsOnRightHalf + availableSeatsOnLeftHalf)) {
								log.debug("found {} seats spread across the row", requestedSeatCount);
								heldSeats = singleRow.stream().filter(x -> x.getStatus().equals(Status.AVAILABLE))
										.limit(requestedSeatCount).map(x -> holdAndGet(x)).collect(Collectors.toList());
							} else {
								List<Seat> temp = singleRow.stream().filter(x -> x.getStatus().equals(Status.AVAILABLE))
										.collect(Collectors.toList());
								if (temp.size() > 0) {
									log.debug("requested {} seats found", requestedSeatCount, temp.size());
									carryOver.addAll(temp);
								}
								if (requestedSeatCount <= carryOver.size()) {
									log.debug(
											"found {} seats spread across multiple rows, but best seat due to proximity to stage",
											requestedSeatCount);
									heldSeats = carryOver.stream().filter(x -> x.getStatus().equals(Status.AVAILABLE))
											.limit(requestedSeatCount).map(x -> holdAndGet(x))
											.collect(Collectors.toList());
								}
							}
						}
					}
					startingSeat = startingSeat + getTheater().getNumberOfColumns() + 1;

				} while (heldSeats.size() == 0 && startingSeat < listOfSeats.size());
				if(heldSeats.size() == 0)
				{
					throw new NotEnoughSeatsFoundException();
				}
				getTheater().setListOfSeats(updatedList(listOfSeats, heldSeats));
			} finally {
				lock.writeLock().unlock();
			}
		}
		SeatHold seatHold = buildSeatHold(heldSeats);
		if(this.getSeatsHoldMap() != null)
		{
			this.getSeatsHoldMap().put(seatHold.getSeatHoldId(), seatHold);
		}
		else
		{
			Map<Integer, SeatHold> newSeatHoldMap = new HashMap<>();
			newSeatHoldMap.put(seatHold.getSeatHoldId(), seatHold);
			this.setSeatsHoldMap(newSeatHoldMap);
		}
		return seatHold;
	}

	private SeatHold buildSeatHold(List<Seat> heldSeats) {
		return SeatHold.builder().heldSeats(heldSeats).seatHoldId(Instant.now().getNano()).emailAddress(requestEmail)
				.heldTime(Instant.now()).build();
	}

	private Seat holdAndGet(Seat seat) {
		return Seat.builder().row(seat.getRow()).col(seat.getCol()).heldTime(Instant.now()).status(Status.HELD).build();
	}

	private List<Seat> updatedList(List<Seat> allSeats, List<Seat> heldSeats) {

		for (int i = 0; i < allSeats.size(); i++) {
			for (Seat heldSeat : heldSeats) {
				if (allSeats.get(i).getRow() == heldSeat.getRow() && allSeats.get(i).getCol() == heldSeat.getCol()) {
					allSeats.get(i).setHeldTime(heldSeat.getHeldTime());
					allSeats.get(i).setStatus(heldSeat.getStatus());
				}
			}
		}
		return allSeats;
	}

}
