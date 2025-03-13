package server2;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class City {
    private int id;
    private String name;
    private String country;
    private String timezone;
}
