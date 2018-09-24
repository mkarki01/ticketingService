package com.wlabs.ticketing.processors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import com.wlabs.ticketing.model.Seat;
import com.wlabs.ticketing.model.Seat.Status;
import com.wlabs.ticketing.model.SeatHold;
import com.wlabs.ticketing.model.Theater;

public class AvailableSeatsProcessorTest {

	@Mock
	ReentrantReadWriteLock reentrantReadWriteLockMock;

	@Mock
	ReentrantReadWriteLock.ReadLock lockMock;

	Theater theater;
	List<Seat> listOfSeats;
	AvailableSeatsProcessor availableSeatsProcessor;

	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		availableSeatsProcessor = new AvailableSeatsProcessor(getTheaterStatus(),new HashMap<Integer, SeatHold>());
		Whitebox.setInternalState(availableSeatsProcessor, "lock", reentrantReadWriteLockMock);
		when(reentrantReadWriteLockMock.readLock()).thenReturn(lockMock);

	}


	private Theater getTheaterStatus() {
		listOfSeats = new ArrayList<>();
		for(int i=0; i<10; i++)
		{
			for (int j=0; j<10; j++)
			{
				if(i==0 && j ==0)
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.HELD).heldTime(Instant.now().minus(5, ChronoUnit.MINUTES)).build());
				else
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.AVAILABLE).build());

			}
		}
		return Theater.builder().listOfSeats(listOfSeats).numberOfColumns(10).numberOfRows(10).build();
	}
	

	@Test
	public void whenCannotLockThenNumberOfRowsReturnedIsZero() throws Exception {
		when(lockMock.tryLock()).thenReturn(false);
		int numOfRows = availableSeatsProcessor.call();
		assertThat(numOfRows, equalTo(0));

	}

	@Test
	public void whenAvalilableSeatsRetriverTaskIsSubmittedAndGetMethodCalledOnFutureResultReturnsIntegerValueLessThanTotalNumberOfSeats()
			throws InterruptedException, ExecutionException {
		when(lockMock.tryLock()).thenReturn(true);
		int numOfRows = availableSeatsProcessor.call();
		assertThat(numOfRows, equalTo(99));


	}


}
