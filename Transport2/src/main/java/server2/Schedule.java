package server2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Schedule {
    private Route route;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private int availableSeats;

    public String getRouteInfoWithSeats() {
        // Вычисляем реальное время в пути
        long travelMinutes = Duration.between(departureTime, arrivalTime).toMinutes();
        return String.format("%s → %s | %s - %s | В пути: %d мин | Мест: %d",
                route.getDepartureCity().getName(),
                route.getArrivalCity().getName(),
                departureTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                arrivalTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                travelMinutes,
                availableSeats);
    }
}