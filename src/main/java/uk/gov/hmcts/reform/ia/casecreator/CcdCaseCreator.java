package uk.gov.hmcts.reform.ia.casecreator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ia.casecreator.idam.IdamService;
import uk.gov.hmcts.reform.ia.casecreator.idam.IdamTokens;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CcdCaseCreator {

    private final IdamService idamService;
    private final CoreCaseDataApi coreCaseDataApi;
    private final String idamToken;
    private final String userId;
    private final String idamUserRole;
    private final String coreCaseDataJurisdictionId;
    private final String coreCaseDataCaseTypeId;

    @Autowired
    public CcdCaseCreator(IdamService idamService,
                          CoreCaseDataApi coreCaseDataApi,
                          @Value("${core_case_data.jurisdictionId}") String coreCaseDataJurisdictionId,
                          @Value("${core_case_data.caseTypeId}") String coreCaseDataCaseTypeId,
                          @Value("${idam_token}") String idamToken,
                          @Value("${idam_user_id}") String userId,
                          @Value("${idam_user_role}") String idamUserRole
                          ) {
        this.idamService = idamService;
        this.coreCaseDataJurisdictionId = coreCaseDataJurisdictionId;
        this.coreCaseDataCaseTypeId = coreCaseDataCaseTypeId;
        this.coreCaseDataApi = coreCaseDataApi;
        this.idamToken = "Bearer " + idamToken;
        this.userId = userId;
        this.idamUserRole = idamUserRole;

        if (!idamUserRole.equals("citizen") && !idamUserRole.equals("caseworker")) {
            throw new IllegalArgumentException("Property idam_user_role must be either 'citizen' or 'caseworker' but was [" + idamUserRole + "]");
        }
    }

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";

    public void getHeaders() {
        IdamTokens idamTokens = idamService.getIdamTokens();

        System.out.println(ANSI_BLUE + "Authorization: " + ANSI_RESET + idamTokens.getIdamOauth2Token());
        System.out.println(ANSI_BLUE + "ServiceAuthorization: " + ANSI_RESET  + idamTokens.getServiceAuthorization());
    }

    public void createCase(String ccdDefinitionFile) throws IOException {
//        IdamTokens idamTokens = idamService.getIdamTokens();
        String serviceAuthorizationToken = idamService.generateServiceAuthorization();
        IdamTokens idamTokens = IdamTokens.builder()
                .idamOauth2Token(idamToken)
                .serviceAuthorization(serviceAuthorizationToken)
                .userId(userId)
                .build();

        StartEventResponse startAppeal = idamUserRole.equals("citizen") ?
                startCaseForCitizen(idamTokens, "startAppeal") :
                startCaseForCaseworker(idamTokens, "startAppeal");


        InputStream caseStream = (ccdDefinitionFile == null) ?
                getClass().getClassLoader().getResourceAsStream("json/ia_ccd_case.json") :
                getStreamFromFile(ccdDefinitionFile);

        String iaData = IOUtils.toString(caseStream, Charset.defaultCharset().name());

        Map data = new ObjectMapper().readValue(iaData, Map.class);

        CaseDataContent caseDataContent = CaseDataContent.builder()
                .eventToken(startAppeal.getToken())
                .event(Event.builder()
                        .id(startAppeal.getEventId())
                        .summary("summary")
                        .description("description")
                        .build())
                .data(data)
                .build();

        CaseDetails caseDetails = idamUserRole.equals("citizen") ?
                submitForCitizen(idamTokens, caseDataContent) :
                submitForCaseworker(idamTokens, caseDataContent);

        System.out.println(ANSI_BLUE + "case id: " + ANSI_RESET + caseDetails.getId());

        loadCase(caseDetails.getId() + "", idamTokens);
    }

    public void loadCase(String caseId) {
        String serviceAuthorizationToken = idamService.generateServiceAuthorization();
        IdamTokens idamTokens = IdamTokens.builder()
                .idamOauth2Token(idamToken)
                .serviceAuthorization(serviceAuthorizationToken)
                .userId(userId)
                .build();

        loadCase(caseId, idamTokens);
    }

    public void loadCase(String caseId, IdamTokens idamTokens) {
        System.out.println("Loading [" + caseId + "]");

        CaseDetails aCase = idamUserRole.equals("citizen") ?
                coreCaseDataApi.readForCitizen(idamTokens.getIdamOauth2Token(), idamTokens.getServiceAuthorization(), idamTokens.getUserId(), coreCaseDataJurisdictionId, coreCaseDataCaseTypeId, caseId):
                coreCaseDataApi.readForCaseWorker(idamTokens.getIdamOauth2Token(), idamTokens.getServiceAuthorization(), idamTokens.getUserId(), coreCaseDataJurisdictionId, coreCaseDataCaseTypeId, caseId);

        prettyPrintCase(aCase);
    }

    public void loadCases() {
        String serviceAuthorizationToken = idamService.generateServiceAuthorization();
        IdamTokens idamTokens = IdamTokens.builder()
                .idamOauth2Token(idamToken)
                .serviceAuthorization(serviceAuthorizationToken)
                .userId(userId)
                .build();
        loadCases(idamTokens);
    }
    public void loadCases(IdamTokens idamTokens) {
        List<CaseDetails> caseDetails = idamUserRole.equals("citizen") ?
                coreCaseDataApi.searchForCitizen(idamTokens.getIdamOauth2Token(), idamTokens.getServiceAuthorization(), userId, coreCaseDataJurisdictionId, coreCaseDataCaseTypeId, new HashMap<>()):
                coreCaseDataApi.searchForCaseworker(idamTokens.getIdamOauth2Token(), idamTokens.getServiceAuthorization(), userId, coreCaseDataJurisdictionId, coreCaseDataCaseTypeId, new HashMap<>());

        for (CaseDetails caseDetail : caseDetails) {
            prettyPrintCase(caseDetail);
        }
    }

    private void prettyPrintCase(CaseDetails aCase) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String json = gson.toJson(aCase);

        System.out.println(json);
    }

    private FileInputStream getStreamFromFile(String ccdDefinitionFile) throws FileNotFoundException {
        System.out.println("Loading case from [" + ccdDefinitionFile + "]");
        return new FileInputStream(ccdDefinitionFile);
    }

    private StartEventResponse startCaseForCaseworker(IdamTokens idamTokens, String eventId) {
        return coreCaseDataApi.startForCaseworker(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                coreCaseDataJurisdictionId,
                coreCaseDataCaseTypeId,
                eventId);
    }

    private CaseDetails submitForCaseworker(IdamTokens idamTokens, CaseDataContent caseDataContent) {
        return coreCaseDataApi.submitForCaseworker(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                coreCaseDataJurisdictionId,
                coreCaseDataCaseTypeId,
                true,
                caseDataContent
        );
    }

    private StartEventResponse startCaseForCitizen(IdamTokens idamTokens, String eventId) {
        return coreCaseDataApi.startForCitizen(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                coreCaseDataJurisdictionId,
                coreCaseDataCaseTypeId,
                eventId);
    }

    private CaseDetails submitForCitizen(IdamTokens idamTokens, CaseDataContent caseDataContent) {
        return coreCaseDataApi.submitForCitizen(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                coreCaseDataJurisdictionId,
                coreCaseDataCaseTypeId,
                true,
                caseDataContent
        );
    }
}
