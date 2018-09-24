package com.wlabs.ticketing.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Theater {

	int numberOfRows;
	int numberOfColumns;
	List<Seat> listOfSeats;

}
