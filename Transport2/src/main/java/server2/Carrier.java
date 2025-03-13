package server2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Carrier {
    private int id;
    private String name;
    private String transportType;
    private String website;

}
