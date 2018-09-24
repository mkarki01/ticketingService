# Ticket Service Coding Challenge
a simple ticket service that facilitates the discovery, temporary hold, and final reservation of seats within a high-demand performance venue.

## Design
### Assumptions
 * Current status of the seats (sold, held)  will be saved on a highly efficient database system and retrieved when one of the service methods are invoked.
 * Best seats are in the front of the theater.
 * If the requested number of seats are available but the may be spread across a single row or multiple rows closest to the stage.
 * Row closest to the stage is row 0
 * How long a seat can be held is set to 10 minutes but could be made configurable 
 * Can only have one hold per email

### Implementation 
 * There are four processors that do individual tasks as needed.
 * When numSeatsAvailable service operation is called, service implementation will get the status of theater and  hold and will invoke AvailableSeatsProcessor to get the count of available seats. It will also invoke RleaseSeatsProcessor before getting the count if any seat holds need to be released.
 
 * When findAndHoldSeats is service operation is called, service implementation will get the status of theater and hold and will invoke AvailableSeatsProcessor to get the count of available seats. 
 * It will also invoke RleaseSeatsProcessor before getting the count if any seat holds need to be released. 
 * Then, it will loop through every row starting from the first row, split the row in the middle and search for requested number of seats on the right half, if not enough seats are found, then it will look in the left half. 
 * If not found in the left half, it will see if they can be found on entire row even if they are not together. 
 * If not enough found in that row, it will start accumulating the empty seats and search in the next row. 
 * It will repeat the same process as before searching on the right side, left side, entire row, if not found it will try to find the seats in accumulated empty seats. If enough seats are found then returns a Hold, if not throws a NotEnoughSeatsFoundException;  * This situation is only possible if AvailableSeatsProcessor returned enough seats but when trying to hold they were not available.
 
 * When findAndHoldSeats is called any seats that are in HELD status will be changed to RESERVED and SeatHold is removed from the list.
 
## Build/Test

### Prerequisites
* Java  1.8 **JDK** Installed
* Git 2.9.0+ (tested with 2.9.0)
* Maven 3.3.0+

### Steps
* Clone repository or download as zip
* extract and from terminal navigate to ticketing folder.
* enter "mvn test" at the terminal to test
