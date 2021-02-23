package gr.uaegean.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MinEduLDAPResponse {

    private String username;
    private String name;
    private String surname;
    private String email;

}
