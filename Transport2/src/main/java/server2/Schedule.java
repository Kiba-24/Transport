package server2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Schedule {
    private Route route;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private int availableSeats;
}
