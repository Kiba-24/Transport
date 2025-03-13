package server2;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Route {
    private int id;
    private Carrier carrier;
    private City departureCity;
    private City arrivalCity;
    private int durationMinutes;
    private double basePrice;
    @Override
    public String toString() {
        return String.format("%s → %s (%s, %d мин, %.2f руб)",
                departureCity.getName(),
                arrivalCity.getName(),
                carrier.getName(),
                durationMinutes,
                basePrice);
    }
    public String getTransportType() {
        return carrier.getTransportType();
    }

}