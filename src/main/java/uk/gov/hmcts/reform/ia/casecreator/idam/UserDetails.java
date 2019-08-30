package uk.gov.hmcts.reform.ia.casecreator.idam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDetails {
    String id;

    public UserDetails(@JsonProperty("id") String id) {
        this.id = id;
    }
}
