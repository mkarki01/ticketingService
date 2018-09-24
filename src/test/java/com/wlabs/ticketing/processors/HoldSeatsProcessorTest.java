package com.wlabs.ticketing.processors;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.wlabs.ticketing.model.Seat;
import com.wlabs.ticketing.model.Seat.Status;
import com.wlabs.ticketing.model.SeatHold;
import com.wlabs.ticketing.model.Theater;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ HoldSeatsProcessor.class, Executors.class })
@PowerMockIgnore("javax.management.*")
public class HoldSeatsProcessorTest {

	@Mock
	ReentrantReadWriteLock reentrantReadWritereadLockMock;

	@Mock
	ReentrantReadWriteLock.WriteLock writeLockMock;

	@Mock
	ReentrantReadWriteLock.ReadLock readLockMock;

	Theater theater;

	List<Seat> listOfSeats;

	Map<Integer, SeatHold> seatsHoldMap;

	@Mock
	Seat seat;

	@Mock
	Stream<Seat> stream;

	HoldSeatsProcessor seatHolderSpy;

	@Mock
	ExecutorService executorServiceMock;

	@Mock
	Future<Integer> availableSeatsCalcualtorResult;

	@Mock
	Future<Void> releaseExpiredHoldProcessorResult;
	
	@Mock
	Future<Void> releaseDuplicateHoldProcessorResult;

	@Mock
	AvailableSeatsProcessor availableSeatsRetrieverMock;

	@Mock
	ReleaseExpiredHoldProcessor releaseExpiredHoldProcessorMock;
	
	@Mock
	ReleaseDuplicateHoldProcessor releaseDuplicateHoldProcessorMock;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		PowerMockito.whenNew(ReentrantReadWriteLock.class).withNoArguments().thenReturn(reentrantReadWritereadLockMock);
		when(reentrantReadWritereadLockMock.writeLock()).thenReturn(writeLockMock);
		when(reentrantReadWritereadLockMock.readLock()).thenReturn(readLockMock);
		PowerMockito.mockStatic(Executors.class);
		when(Executors.newFixedThreadPool(1)).thenReturn(executorServiceMock);
		when(availableSeatsRetrieverMock.getTheater()).thenReturn(theater);
		when(releaseExpiredHoldProcessorMock.getTheater()).thenReturn(theater);
		when(releaseDuplicateHoldProcessorMock.getTheater()).thenReturn(theater);
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

	@Test
	public void testHoldSeatReturnsSeatsFromRightSideOfTheStageWhenAll10Available() throws Exception {
		initializeTheater(10, 10);
		seatsHoldMap = new HashMap<>();
		givenMockProcessors();

		seatHolderSpy = spy(new HoldSeatsProcessor(theater, seatsHoldMap, 4, "tester@test.com"));
		when(seatHolderSpy.getTheater()).thenReturn(theater);

		when(availableSeatsCalcualtorResult.get()).thenReturn(10);
		when(releaseExpiredHoldProcessorResult.get()).thenReturn(null);
		when(writeLockMock.tryLock()).thenReturn(true);
		when(readLockMock.tryLock()).thenReturn(true);
		when(seatHolderSpy.getSeatsHoldMap()).thenReturn(seatsHoldMap);
		when(seatHolderSpy.getTheater()).thenReturn(theater);

		SeatHold seatHold = seatHolderSpy.call();
		assertThat(seatHold, CoreMatchers.notNullValue());
		assertThat(seatsHoldMap.size(), CoreMatchers.equalTo(1));
		assertThat(seatHold.getHeldSeats().size(), CoreMatchers.equalTo(4));
		int x = 4;
		for (Seat seat : seatHold.getHeldSeats()) {
			assertThat(seat.getCol(), CoreMatchers.equalTo(++x));
			assertThat(seat.getRow(), CoreMatchers.equalTo(0));
			assertTrue(seat.getHeldTime().isBefore(Instant.now()));
			assertThat(seat.getStatus(), CoreMatchers.equalTo(Status.HELD));
		}
	}

	@Test
	public void testHoldSeatReturnsSeatsFromLeftSideOfTheStageWhenOnlyLeftSideAvailable() throws Exception {
		givenSeatsAvailableOnLeftSideOnly(10, 10);
		seatsHoldMap = new HashMap<>();
		givenMockProcessors();
		seatHolderSpy = spy(new HoldSeatsProcessor(theater, seatsHoldMap, 4, "tester@test.com"));
		when(seatHolderSpy.getTheater()).thenReturn(theater);
		when(availableSeatsCalcualtorResult.get()).thenReturn(10);
		when(releaseExpiredHoldProcessorResult.get()).thenReturn(null);
		when(writeLockMock.tryLock()).thenReturn(true);
		when(readLockMock.tryLock()).thenReturn(true);
		when(seatHolderSpy.getSeatsHoldMap()).thenReturn(seatsHoldMap);
		when(seatHolderSpy.getTheater()).thenReturn(theater);

		SeatHold seatHold = seatHolderSpy.call();
		assertThat(seatHold, CoreMatchers.notNullValue());
		assertThat(seatsHoldMap.size(), CoreMatchers.equalTo(1));
		assertThat(seatHold.getHeldSeats().size(), CoreMatchers.equalTo(4));
		int x = 0;
		for (Seat seat : seatHold.getHeldSeats()) {
			assertThat(seat.getCol(), CoreMatchers.equalTo(x));
			assertThat(seat.getRow(), CoreMatchers.equalTo(0));
			assertTrue(seat.getHeldTime().isBefore(Instant.now()));
			assertThat(seat.getStatus(), CoreMatchers.equalTo(Status.HELD));
			x++;
		}
	}

	@Test
	public void testHoldSeatReturnsSeatsSpreadAcrossARow() throws Exception {
		givenMiddleSeatsReserved(10, 10);
		seatsHoldMap = new HashMap<>();
		givenMockProcessors();
		seatHolderSpy = spy(new HoldSeatsProcessor(theater, seatsHoldMap, 4, "tester@test.com"));
		when(seatHolderSpy.getTheater()).thenReturn(theater);
		when(availableSeatsCalcualtorResult.get()).thenReturn(10);
		when(releaseExpiredHoldProcessorResult.get()).thenReturn(null);
		when(writeLockMock.tryLock()).thenReturn(true);
		when(readLockMock.tryLock()).thenReturn(true);
		when(seatHolderSpy.getSeatsHoldMap()).thenReturn(seatsHoldMap);
		when(seatHolderSpy.getTheater()).thenReturn(theater);

		SeatHold seatHold = seatHolderSpy.call();
		assertThat(seatHold, CoreMatchers.notNullValue());
		assertThat(seatsHoldMap.size(), CoreMatchers.equalTo(1));
		assertThat(seatHold.getHeldSeats().size(), CoreMatchers.equalTo(4));
		for (Seat seat : seatHold.getHeldSeats()) {
			assertThat(seat.getRow(), CoreMatchers.equalTo(0));
			assertTrue(seat.getHeldTime().isBefore(Instant.now()));
			assertThat(seat.getStatus(), CoreMatchers.equalTo(Status.HELD));
		}
	}

	@Test
	public void testHoldSeatReturnsSeatsSpreadAcrossMultipleRows() throws Exception {
		givenLessThanFourSeatsPerRow(10, 10);
		seatsHoldMap = new HashMap<>();
		givenMockProcessors();

		seatHolderSpy = spy(new HoldSeatsProcessor(theater, seatsHoldMap, 4, "tester@test.com"));
		when(seatHolderSpy.getTheater()).thenReturn(theater);
		when(availableSeatsCalcualtorResult.get()).thenReturn(10);
		when(releaseExpiredHoldProcessorResult.get()).thenReturn(null);
		when(writeLockMock.tryLock()).thenReturn(true);
		when(readLockMock.tryLock()).thenReturn(true);
		when(seatHolderSpy.getSeatsHoldMap()).thenReturn(seatsHoldMap);
		when(seatHolderSpy.getTheater()).thenReturn(theater);

		SeatHold seatHold = seatHolderSpy.call();
		assertThat(seatHold, CoreMatchers.notNullValue());
		assertThat(seatsHoldMap.size(), CoreMatchers.equalTo(1));
		assertThat(seatHold.getHeldSeats().size(), CoreMatchers.equalTo(4));
		for (Seat seat : seatHold.getHeldSeats()) {
			assertTrue(seat.getHeldTime().isBefore(Instant.now()));
			assertThat(seat.getStatus(), CoreMatchers.equalTo(Status.HELD));
		}
	}

	@Test(expected = NotEnoughSeatsFoundException.class)
	public void testHoldSeatDoesNotReturnSeatsIfMoreThanAvailableRequested() throws Exception {
		givenLessThanFourSeatsPerRow(10, 10);
		seatsHoldMap = new HashMap<>();
		givenMockProcessors();
		
		seatHolderSpy = spy(new HoldSeatsProcessor(theater, seatsHoldMap, 100, "tester@test.com"));
		when(seatHolderSpy.getTheater()).thenReturn(theater);
		when(executorServiceMock.submit(availableSeatsRetrieverMock)).thenReturn(availableSeatsCalcualtorResult);
		when(executorServiceMock.submit(releaseExpiredHoldProcessorMock)).thenReturn(releaseExpiredHoldProcessorResult);
		when(availableSeatsCalcualtorResult.get()).thenReturn(70);
		when(releaseExpiredHoldProcessorResult.get()).thenReturn(null);
		when(writeLockMock.tryLock()).thenReturn(true);
		when(readLockMock.tryLock()).thenReturn(true);
		when(seatHolderSpy.getSeatsHoldMap()).thenReturn(seatsHoldMap);
		when(seatHolderSpy.getTheater()).thenReturn(theater);

		SeatHold seatHold = seatHolderSpy.call();
	}

	@Test
	public void testHoldSeatReturnsSeatsFromSecondRowGivenFirstRowReserved() throws Exception {
		givenFirstRowNotAvailable(10, 10);
		seatsHoldMap = new HashMap<>();
		givenMockProcessors();
		
		seatHolderSpy = spy(new HoldSeatsProcessor(theater, seatsHoldMap, 4, "tester@test.com"));
		when(seatHolderSpy.getTheater()).thenReturn(theater);
		when(availableSeatsCalcualtorResult.get()).thenReturn(10);
		when(releaseExpiredHoldProcessorResult.get()).thenReturn(null);
		when(writeLockMock.tryLock()).thenReturn(true);
		when(readLockMock.tryLock()).thenReturn(true);
		when(seatHolderSpy.getSeatsHoldMap()).thenReturn(seatsHoldMap);
		when(seatHolderSpy.getTheater()).thenReturn(theater);

		SeatHold seatHold = seatHolderSpy.call();
		assertThat(seatHold, CoreMatchers.notNullValue());
		assertThat(seatsHoldMap.size(), CoreMatchers.equalTo(1));
		assertThat(seatHold.getHeldSeats().size(), CoreMatchers.equalTo(4));
		for (Seat seat : seatHold.getHeldSeats()) {
			assertThat(seat.getRow(), CoreMatchers.equalTo(1));
			assertTrue(seat.getHeldTime().isBefore(Instant.now()));
			assertThat(seat.getStatus(), CoreMatchers.equalTo(Status.HELD));
		}
	}

	private void givenMockProcessors() throws Exception {
		PowerMockito.whenNew(AvailableSeatsProcessor.class).withArguments(theater, seatsHoldMap)
				.thenReturn(availableSeatsRetrieverMock);
		PowerMockito.whenNew(ReleaseExpiredHoldProcessor.class).withArguments(theater, seatsHoldMap)
				.thenReturn(releaseExpiredHoldProcessorMock);
		PowerMockito.whenNew(ReleaseDuplicateHoldProcessor.class).withArguments(theater, seatsHoldMap, "tester@test.com")
		.thenReturn(releaseDuplicateHoldProcessorMock);
		when(executorServiceMock.submit(availableSeatsRetrieverMock)).thenReturn(availableSeatsCalcualtorResult);
		when(executorServiceMock.submit(releaseExpiredHoldProcessorMock)).thenReturn(releaseExpiredHoldProcessorResult);
		when(executorServiceMock.submit(releaseDuplicateHoldProcessorMock)).thenReturn(releaseDuplicateHoldProcessorResult);
	}

	private void givenFirstRowNotAvailable(int rowCount, int colCount) {
		listOfSeats = new ArrayList<>();
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < colCount; j++) {
				if (i == 0)
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.RESERVED)
							.heldTime(Instant.now().minus(5, ChronoUnit.MINUTES)).build());
				else
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.AVAILABLE).build());

			}
		}
		theater = spy(Theater.builder().listOfSeats(listOfSeats).numberOfColumns(10).numberOfRows(10).build());
	}

	private void givenSeatsAvailableOnLeftSideOnly(int rowCount, int colCount) {
		listOfSeats = new ArrayList<>();
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < colCount; j++) {
				if (j > colCount / 2)
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.RESERVED)
							.heldTime(Instant.now().minus(5, ChronoUnit.MINUTES)).build());
				else
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.AVAILABLE).build());

			}
		}
		theater = spy(Theater.builder().listOfSeats(listOfSeats).numberOfColumns(10).numberOfRows(10).build());
	}

	private void givenMiddleSeatsReserved(int rowCount, int colCount) {
		listOfSeats = new ArrayList<>();
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < colCount; j++) {
				if (j == colCount / 2 || j == ((colCount / 2) + 1) || j == ((colCount / 2) - 1)
						|| j == ((colCount / 2) + 2))
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.RESERVED)
							.heldTime(Instant.now().minus(5, ChronoUnit.MINUTES)).build());
				else
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.AVAILABLE).build());

			}
		}
		theater = spy(Theater.builder().listOfSeats(listOfSeats).numberOfColumns(10).numberOfRows(10).build());
	}

	private void givenLessThanFourSeatsPerRow(int rowCount, int colCount) {
		listOfSeats = new ArrayList<>();
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < colCount; j++) {
				if (j <= colCount / 2 || j == ((colCount / 2) + 1) || j == ((colCount / 2) + 2))
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.RESERVED)
							.heldTime(Instant.now().minus(5, ChronoUnit.MINUTES)).build());
				else
					listOfSeats.add(Seat.builder().row(i).col(j).status(Status.AVAILABLE).build());

			}
		}
		theater = spy(Theater.builder().listOfSeats(listOfSeats).numberOfColumns(10).numberOfRows(10).build());
	}

}
