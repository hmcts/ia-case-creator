package uk.gov.hmcts.reform.ia.casecreator.services;

import io.restassured.http.Headers;
import uk.gov.hmcts.reform.ia.casecreator.idam.UserInfo;

public interface AuthorizationHeaders {
    Headers getAuthorizationHeaders(String credentials);

    UserInfo getUserInfo(String userToken);
}
