package com.wlabs.ticketing.service;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.wlabs.ticketing.model.Seat;
import com.wlabs.ticketing.model.Seat.Status;
import com.wlabs.ticketing.model.SeatHold;
import com.wlabs.ticketing.model.Theater;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TicketServiceImplTest {

	Theater theater;

	List<Seat> listOfSeats;

	Map<Integer, SeatHold> seatsHoldMap;

	TicketServiceImpl tickingServiceImpl;

	@Before
	public void setup() throws Exception {

		initializeTheater(10, 10);
		seatsHoldMap = new HashMap<>();

		tickingServiceImpl = new TicketServiceImpl();

		Whitebox.setInternalState(tickingServiceImpl, "theater", theater);
		Whitebox.setInternalState(tickingServiceImpl, "seatsHoldMap", seatsHoldMap);

	}

	@Test
	public void testNumAvailableSeats() throws Exception {

		int numSeatsAvailable = tickingServiceImpl.numSeatsAvailable();
		log.debug("numSeatsAvailable={}", numSeatsAvailable);
		assertThat(numSeatsAvailable, CoreMatchers.equalTo(99));
	}

	@Test
	public void testfindAndHoldSeats() throws Exception {
		int numSeatsAvailable = tickingServiceImpl.numSeatsAvailable();
		log.debug("numSeatsAvailable={}", numSeatsAvailable);
		assertThat(numSeatsAvailable, CoreMatchers.equalTo(99));
		
		SeatHold seatHold = tickingServiceImpl.findAndHoldSeats(2, "test@test.com");
		assertThat(seatHold, CoreMatchers.notNullValue());
		assertThat(seatHold.getHeldSeats(), CoreMatchers.notNullValue());
		assertThat(seatHold.getHeldSeats().size(), CoreMatchers.equalTo(2));
		assertThat(seatHold.getSeatHoldId(), CoreMatchers.notNullValue());
		assertThat(seatHold.getHeldTime(), CoreMatchers.notNullValue());
		numSeatsAvailable = tickingServiceImpl.numSeatsAvailable();
		
		log.debug("numSeatsAvailable={}", numSeatsAvailable);
		assertThat(numSeatsAvailable, CoreMatchers.equalTo(97));
	}

	
	@Test
	public void testOnlyOneHoldPerEmailExists() throws Exception {
		int numSeatsAvailable = tickingServiceImpl.numSeatsAvailable();
		log.debug("numSeatsAvailable={}", numSeatsAvailable);
		assertThat(numSeatsAvailable, CoreMatchers.equalTo(99));
		
		SeatHold seatHold = tickingServiceImpl.findAndHoldSeats(3, "test@test.com");
		numSeatsAvailable = tickingServiceImpl.numSeatsAvailable();		
		log.debug("numSeatsAvailable={}", numSeatsAvailable);
		assertThat(numSeatsAvailable, CoreMatchers.equalTo(96));
		
		seatHold = tickingServiceImpl.findAndHoldSeats(2, "test@test.com");
		assertThat(seatHold, CoreMatchers.notNullValue());
		assertThat(seatHold.getHeldSeats(), CoreMatchers.notNullValue());
		assertThat(seatHold.getHeldSeats().size(), CoreMatchers.equalTo(2));
		assertThat(seatHold.getSeatHoldId(), CoreMatchers.notNullValue());
		assertThat(seatHold.getHeldTime(), CoreMatchers.notNullValue());
		numSeatsAvailable = tickingServiceImpl.numSeatsAvailable();
		
		log.debug("numSeatsAvailable={}", numSeatsAvailable);
		assertThat(numSeatsAvailable, CoreMatchers.equalTo(97));
	}
	
	@Test
	public void testReserveSeats() throws Exception {
		initializeSeatHold();
		Whitebox.setInternalState(tickingServiceImpl, "seatsHoldMap", seatsHoldMap);

		String confirmationCode = tickingServiceImpl.reserveSeats(123123, "test2@test.com");
		assertThat(confirmationCode, CoreMatchers.notNullValue());
		assertThat(confirmationCode, CoreMatchers.notNullValue());
	}

	

	
	
	private void initializeTheater(int rowCount, int colCount) {
		listOfSeats = new ArrayList<>();
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < colCount; j++) {
				if (i == 0 && j == 0)
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.HELD)
							.heldTime(Instant.now().minus(5, ChronoUnit.MINUTES)).build());
				else
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.AVAILABLE).build());

			}
		}
		theater = spy(Theater.builder().listOfSeats(listOfSeats).numberOfColumns(10).numberOfRows(10).build());
	}

	private void initializeSeatHold() {
		seatsHoldMap = new HashMap<>();
		List<Seat> heldSeats = new ArrayList<>();
		heldSeats.add(Seat.builder().col(0).row(0).build());
		SeatHold seatHold = SeatHold.builder().heldSeats(heldSeats).emailAddress("test2@test.com").seatHoldId(123123)
				.heldTime(Instant.now()).build();
		seatsHoldMap.put(123123, seatHold);
	}

}
