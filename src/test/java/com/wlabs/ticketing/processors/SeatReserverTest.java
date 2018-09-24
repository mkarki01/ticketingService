package com.wlabs.ticketing.processors;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import com.wlabs.ticketing.model.Seat;
import com.wlabs.ticketing.model.Seat.Status;
import com.wlabs.ticketing.model.SeatHold;
import com.wlabs.ticketing.model.Theater;

public class SeatReserverTest {

	@Mock
	ReentrantReadWriteLock reentrantReadWriteLockMock;

	@Mock
	ReentrantReadWriteLock.WriteLock lockMock;

	
	Theater theater;

	List<Seat> listOfSeats;
	
	Map<Integer,SeatHold> seatsHoldMap;

	@Mock
	Seat seat;

	@Mock
	Stream<Seat> stream;

	ReserveSeatsProcessor reserveSeatsProcessorSpy;

	ExecutorService executorService;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		initializeTheater();
		Seat seat = spy(Seat.builder().col(1).row(1).heldTime(Instant.now().minus(5, ChronoUnit.MINUTES)).status(Status.HELD).build());
		SeatHold seatHold = SeatHold.builder().emailAddress("tester@test.com").seatHoldId(123123).heldTime(seat.getHeldTime()).heldSeats(Arrays.asList(seat)).build();
		seatsHoldMap = new HashMap<>();
		seatsHoldMap.put(123123, seatHold);
		reserveSeatsProcessorSpy = spy(new ReserveSeatsProcessor(theater, seatsHoldMap, 123123, "tester@test.com"));
		Whitebox.setInternalState(reserveSeatsProcessorSpy, "lock", reentrantReadWriteLockMock);
		when(reentrantReadWriteLockMock.writeLock()).thenReturn(lockMock);
		executorService = Executors.newFixedThreadPool(1);

	}


	private void initializeTheater() {
		listOfSeats = new ArrayList<>();
		for(int i=0; i<10; i++)
		{
			for (int j=0; j<10; j++)
			{
				if(i==1 && j ==1)
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.HELD).heldTime(Instant.now().minus(5, ChronoUnit.MINUTES)).build());
				else
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.AVAILABLE).build());

			}
		}
		theater = spy(Theater.builder().listOfSeats(listOfSeats).numberOfColumns(10).numberOfRows(10).build());
	}
	

	@Test
	public void verifyWhenTryLockReturnsFalseGetTheaterNotCalled() throws Exception {
	    when(lockMock.tryLock()).thenReturn(false);
		final Future<String> seatReserverFutureResult = executorService.submit(reserveSeatsProcessorSpy);
		String confirmationNumber = seatReserverFutureResult.get();
		verify(reserveSeatsProcessorSpy, times(0)).getTheater();
		assertThat(confirmationNumber, CoreMatchers.notNullValue());
	}

	public void verifyReserveSeatIsCalledAndCountOfSeatsCorrect() throws Exception {
		when(lockMock.tryLock()).thenReturn(true);

		assertThat(seatsHoldMap.size(), CoreMatchers.equalTo(1));
		String confirmationNumber = reserveSeatsProcessorSpy.call();
		assertThat(confirmationNumber, CoreMatchers.notNullValue());
		verify(reserveSeatsProcessorSpy, atLeast(1)).reserveSeat(any(Seat.class));
		assertThat(seatsHoldMap.size(), CoreMatchers.equalTo(0));
		List<Seat> seatList = reserveSeatsProcessorSpy.getTheater().getListOfSeats();
		int count = Math.toIntExact(seatList.stream().filter(x -> x.getStatus().equals(Status.RESERVED)).count());
		assertThat(count, CoreMatchers.equalTo(1));
		int heldCount = Math.toIntExact(seatList.stream().filter(x -> x.getStatus().equals(Status.HELD)).count());
		assertThat(heldCount, CoreMatchers.equalTo(0));		
		int countAvailable = Math.toIntExact(seatList.stream().filter(x -> x.getStatus().equals(Status.AVAILABLE)).count());
		assertThat(countAvailable, CoreMatchers.equalTo(99));
	}

}
