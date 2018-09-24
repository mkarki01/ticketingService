package com.wlabs.ticketing.processors;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import com.wlabs.ticketing.model.Seat;
import com.wlabs.ticketing.model.Seat.Status;
import com.wlabs.ticketing.model.SeatHold;
import com.wlabs.ticketing.model.Theater;

public class ReleaseExpiredHoldProcessorTest {

	@Mock
	ReentrantReadWriteLock reentrantReadWriteLockMock;

	@Mock
	ReentrantReadWriteLock.WriteLock writeLock;

	@Mock
	Theater theater;
	
	@Mock
	Map<Integer, SeatHold>  seatsHoldMap;
	List<Seat> listOfSeats;

	@Mock
	Seat seat;
	
	@Mock
	Stream<Seat> stream;
	
	ReleaseExpiredHoldProcessor releaseSeatsProcessorSpy;
	
	ExecutorService executorService;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		releaseSeatsProcessorSpy = spy(new ReleaseExpiredHoldProcessor(theater, seatsHoldMap));
		Whitebox.setInternalState(releaseSeatsProcessorSpy, "lock", reentrantReadWriteLockMock);
		when(reentrantReadWriteLockMock.writeLock()).thenReturn(writeLock);
		executorService = Executors.newFixedThreadPool(1);

	}

	@Test
	public void verifyWhenTryLockReturnsFalseGetTheaterNotCalled() throws Exception {
		when(writeLock.tryLock()).thenReturn(false);
		final Future<Void> availableSeatsCalcualtorResult = executorService.submit(releaseSeatsProcessorSpy);
		availableSeatsCalcualtorResult.get();
		verify(releaseSeatsProcessorSpy, times(0)).getTheater();
		verify(releaseSeatsProcessorSpy, times(0)).setTheater(theater);

	}

	@Test
	public void verifyWhenTryLockReturnsTrueExpiredHoldIsReleased() throws Exception {
		when(writeLock.tryLock()).thenReturn(true);
		when(releaseSeatsProcessorSpy.getTheater()).thenReturn(theater);
		when(seat.getCol()).thenReturn(1);
		when(seat.getRow()).thenReturn(1);
		when(seat.getHeldTime()).thenReturn(Instant.now().minus(11,ChronoUnit.MINUTES));
		when(seat.getStatus()).thenReturn(Status.HELD);
		listOfSeats = new ArrayList<>();
		listOfSeats.add(seat);
		when(theater.getListOfSeats()).thenReturn(listOfSeats);

		final Future<Void> availableSeatsCalcualtorResult = executorService.submit(releaseSeatsProcessorSpy);
		availableSeatsCalcualtorResult.get();
		verify(theater, times(1)).getListOfSeats();
		verify(seat, times(1)).setStatus(Status.AVAILABLE);

	}
	
	@Test
	public void verifyWhenTryLockReturnsTrueUnexpiredHoldIsNotReleased() throws Exception {
		when(writeLock.tryLock()).thenReturn(true);
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		when(releaseSeatsProcessorSpy.getTheater()).thenReturn(theater);
		when(seat.getCol()).thenReturn(1);
		when(seat.getRow()).thenReturn(1);
		when(seat.getHeldTime()).thenReturn(Instant.now().minus(9,ChronoUnit.MINUTES));
		when(seat.getStatus()).thenReturn(Status.HELD);
		listOfSeats = new ArrayList<>();
		listOfSeats.add(seat);
		when(theater.getListOfSeats()).thenReturn(listOfSeats);

		final Future<Void> availableSeatsCalcualtorResult = executorService.submit(releaseSeatsProcessorSpy);
		availableSeatsCalcualtorResult.get();
		verify(theater, times(1)).getListOfSeats();
		verify(seat, times(0)).setStatus(Status.AVAILABLE);
	}

}
