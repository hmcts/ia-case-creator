package uk.gov.hmcts.reform.ia.casecreator;

import feign.Response;

public class ErrorDecoder implements feign.codec.ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        return new CaseCreatorFeignException(methodKey, response);
    }
}
