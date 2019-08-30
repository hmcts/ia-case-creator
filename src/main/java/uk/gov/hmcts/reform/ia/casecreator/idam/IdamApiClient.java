package uk.gov.hmcts.reform.ia.casecreator.idam;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "idam-api",
        url = "${idam.url}"
)
public interface IdamApiClient {
    @RequestMapping(
            method = {RequestMethod.POST},
            value = {"/oauth2/authorize"},
            consumes = {"application/x-www-form-urlencoded"}
    )
    Authorize authorizeCodeType(@RequestHeader("Authorization") String var1, @RequestParam("response_type") String var2, @RequestParam("client_id") String var3, @RequestParam("redirect_uri") String var4, @RequestBody String var5);

    @RequestMapping(
            method = {RequestMethod.POST},
            value = {"/oauth2/token"},
            consumes = {"application/x-www-form-urlencoded"}
    )
    Authorize authorizeToken(@RequestParam("code") String var1, @RequestParam("grant_type") String var2, @RequestParam("redirect_uri") String var3, @RequestParam("client_id") String var4, @RequestParam("client_secret") String var5, @RequestBody String var6);

    @RequestMapping(
            method = {RequestMethod.GET},
            value = {"/details"}
    )
    UserDetails getUserDetails(@RequestHeader("Authorization") String var1);
}
