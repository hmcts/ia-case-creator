package uk.gov.hmcts.reform.ia.casecreator.idam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IdamTokens {
    String idamOauth2Token;
    String serviceAuthorization;
    final String userId;
}
