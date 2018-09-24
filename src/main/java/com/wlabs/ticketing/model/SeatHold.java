package com.wlabs.ticketing.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
public class SeatHold
{
    int seatHoldId;
    List<Seat> heldSeats;
    String emailAddress;
    Instant heldTime;
}
