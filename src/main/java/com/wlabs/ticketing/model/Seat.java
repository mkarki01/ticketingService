package com.wlabs.ticketing.model;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class Seat
{

    public enum Status
    {
        HELD, RESERVED, AVAILABLE;
    }
    int row;
    int col;
    Status status = Status.AVAILABLE;
    Instant heldTime = null;
    
}
