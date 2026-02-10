package uk.gov.hmcts.reform.ia.casecreator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import io.restassured.http.Headers;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ia.casecreator.idam.IdamTokens;
import uk.gov.hmcts.reform.ia.casecreator.idam.UserInfo;
import uk.gov.hmcts.reform.ia.casecreator.services.AuthorizationHeaders;
import uk.gov.hmcts.reform.ia.casecreator.services.AuthorizationHeadersProvider;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static uk.gov.hmcts.reform.ia.casecreator.DocumentNames.NOTICE_OF_APPEAL_PDF;
import static uk.gov.hmcts.reform.ia.casecreator.services.AuthorizationHeadersProvider.AUTHORIZATION;
import static uk.gov.hmcts.reform.ia.casecreator.services.AuthorizationHeadersProvider.SERVICE_AUTHORIZATION;

@Service
public class CcdCaseCreator {

    private Collection<Resource> documentResources;
    private final CdamDocumentManagementUploader cdamDocumentManagementUploader;
    private final AuthorizationHeadersProvider authorizationHeadersProvider;
    private final CoreCaseDataApi coreCaseDataApi;
    private final String coreCaseDataJurisdictionId;
    private final String coreCaseDataCaseTypeId;

    private String idamUsername;
    private String idamPassword;
    private String idamUserRole;

    //private final List<String> stateList = Arrays.asList("pendingPayment_noRemission", "pendingPayment_hasRemission",
    //        "appealSubmitted", "awaitingRespondentEvidence", "caseUnderReview", "listing", "prepareForHearing", "decision", "decided",
    //        "ftpaSubmitted", "ftpaDecided_HO_AP", "ftpaDecided", "remitted", "ended");

    //private final List<String> stateList = Arrays.asList("appealSubmitted");
    private final List<String> stateList = Arrays.asList("awaitingRespondentEvidenceWithHOBundle");
    @Autowired
    public CcdCaseCreator(CdamDocumentManagementUploader cdamDocumentManagementUploader,
                          AuthorizationHeadersProvider authorizationHeadersProvider,
                          CoreCaseDataApi coreCaseDataApi,
                          @Value("${core_case_data.jurisdictionId}") String coreCaseDataJurisdictionId,
                          @Value("${core_case_data.caseTypeId}") String coreCaseDataCaseTypeId,
                          @Value("${migration.idam.username}") String idamUsername,
                          @Value("${migration.idam.password}") String idamPassword,
                          @Value("${idam.user.role}") String idamuserRole
                          ) {
        this.cdamDocumentManagementUploader = cdamDocumentManagementUploader;
        this.authorizationHeadersProvider = authorizationHeadersProvider;
        this.coreCaseDataJurisdictionId = coreCaseDataJurisdictionId;
        this.coreCaseDataCaseTypeId = coreCaseDataCaseTypeId;
        this.coreCaseDataApi = coreCaseDataApi;
        this.idamUsername = idamUsername;
        this.idamPassword = idamPassword;
        this.idamUserRole = idamuserRole;

        if (idamUserRole.isEmpty()) {
            throw new IllegalArgumentException("Property idam_user_role is invalid");
        }
    }

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";

    public void getHeaders() {
        Headers authorizationHeaders = authorizationHeadersProvider
                .getAuthorizationHeaders(idamUserRole);

        System.out.println(ANSI_BLUE + "Authorization: " + ANSI_RESET + authorizationHeaders.getValue(AUTHORIZATION));
        System.out.println(ANSI_BLUE + "ServiceAuthorization: " + ANSI_RESET  + authorizationHeaders.getValue(SERVICE_AUTHORIZATION));
    }

    public void createCase(String ccdDefinitionFile) throws IOException {

        Headers authorizationHeaders = authorizationHeadersProvider
                .getAuthorizationHeaders(idamUserRole);
        String userToken = authorizationHeaders.getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationHeadersProvider.getUserInfo(userToken);

        documentResources =
                BinaryResourceLoader
                        .load("/documents/*")
                        .values();

        for (String state : stateList) {

            Document noticeOfAppealDocument = getDocument(NOTICE_OF_APPEAL_PDF, authorizationHeaders, userInfo);
            StartEventResponse createAppeal = idamUserRole.equals("citizen") ?
                startCaseForCitizen(authorizationHeaders, userInfo,"ariaCreateCase") :
                startCaseForCaseworker(authorizationHeaders, userInfo, "ariaCreateCase");

            InputStream caseStream = (ccdDefinitionFile == null) ?
                    getClass().getClassLoader().getResourceAsStream("json/preview/" + state + ".json") :
                    getStreamFromFile(ccdDefinitionFile);

            String iaData = IOUtils.toString(caseStream, Charset.defaultCharset().name());

            iaData = iaData.replace("DOCUMENT_BINARY_URL",  noticeOfAppealDocument.getDocumentBinaryUrl()  );
            iaData = iaData.replace("DOCUMENT_URL",   noticeOfAppealDocument.getDocumentUrl()  );


            Map data = new ObjectMapper().readValue(iaData, Map.class);

            CaseDataContent caseDataContent = CaseDataContent.builder()
                    .eventToken(createAppeal.getToken())
                    .event(Event.builder()
                            .id(createAppeal.getEventId())
                            .summary("summary")
                            .description("description")
                            .build())
                    .data(data)
                    .build();

            CaseDetails caseDetails = idamUserRole.equals("citizen") ?
                    submitForCitizen(authorizationHeaders, userInfo, caseDataContent) :
                    submitForCaseworker(authorizationHeaders, userInfo, caseDataContent);

            System.out.println(ANSI_BLUE + "case id: " + ANSI_RESET + caseDetails.getId()
                    + ANSI_BLUE + " case state: " + ANSI_RESET + state);

    //        loadCase(caseDetails.getId() + "", idamTokens);
        }
    }

    public void loadCase(String caseId) {
        Headers authorizationHeaders = authorizationHeadersProvider
                .getAuthorizationHeaders(idamUserRole);
        String userToken = authorizationHeaders.getValue(AUTHORIZATION);
        String serviceToken = authorizationHeaders.getValue(AUTHORIZATION);


        loadCase(caseId, userToken, serviceToken);
    }

    public void loadCase(String caseId, String userToken, String serviceToken) {
        System.out.println("Loading [" + caseId + "]");
        UserInfo userInfo = authorizationHeadersProvider.getUserInfo(userToken);

        CaseDetails aCase = idamUserRole.equals("citizen") ?
                coreCaseDataApi.readForCitizen(userToken, serviceToken, userInfo.getUid(), coreCaseDataJurisdictionId, coreCaseDataCaseTypeId, caseId):
                coreCaseDataApi.readForCaseWorker(userToken, serviceToken, userInfo.getUid(), coreCaseDataJurisdictionId, coreCaseDataCaseTypeId, caseId);

        prettyPrintCase(aCase);
    }

    public void loadCases() {
        Headers authorizationHeaders = authorizationHeadersProvider
                .getAuthorizationHeaders(idamUserRole);
        String userToken = authorizationHeaders.getValue(AUTHORIZATION);
        String serviceToken = authorizationHeaders.getValue(AUTHORIZATION);
        loadCases(userToken, serviceToken);
    }
    public void loadCases(String userToken, String serviceToken) {

        UserInfo userInfo = authorizationHeadersProvider.getUserInfo(userToken);

        List<CaseDetails> caseDetails = idamUserRole.equals("citizen") ?
                coreCaseDataApi.searchForCitizen(userToken, serviceToken, userInfo.getUid(), coreCaseDataJurisdictionId, coreCaseDataCaseTypeId, new HashMap<>()):
                coreCaseDataApi.searchForCaseworker(userToken, serviceToken, userInfo.getUid(), coreCaseDataJurisdictionId, coreCaseDataCaseTypeId, new HashMap<>());

        for (CaseDetails caseDetail : caseDetails) {
            prettyPrintCase(caseDetail);
        }
    }

    private void prettyPrintCase(CaseDetails aCase) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter2())
                .setPrettyPrinting().create();

        String json = gson.toJson(aCase);

        System.out.println(json);
    }

    public static class LocalDateTypeAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public JsonElement serialize(final LocalDate date, final Type typeOfSrc,
                                     final JsonSerializationContext context) {
            return new JsonPrimitive(date.format(formatter));
        }

        @Override
        public LocalDate deserialize(final JsonElement json, final Type typeOfT,
                                     final JsonDeserializationContext context) throws JsonParseException {
            return LocalDate.parse(json.getAsString(), formatter);
        }
    }

    public class LocalDateTimeTypeAdapter2 implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d::MMM::uuuu HH::mm::ss");

        @Override
        public JsonElement serialize(LocalDateTime localDateTime, Type srcType,
                                     JsonSerializationContext context) {

            return new JsonPrimitive(formatter.format(localDateTime));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT,
                                         JsonDeserializationContext context) throws JsonParseException {

            return LocalDateTime.parse(json.getAsString(), formatter);
        }
    }

    private FileInputStream getStreamFromFile(String ccdDefinitionFile) throws FileNotFoundException {
        System.out.println("Loading case from [" + ccdDefinitionFile + "]");
        return new FileInputStream(ccdDefinitionFile);
    }

    private StartEventResponse startCaseForCaseworker(Headers authorizationHeaders, UserInfo userInfo,  String eventId) {
        return coreCaseDataApi.startForCaseworker(
                authorizationHeaders.getValue(AUTHORIZATION),
                authorizationHeaders.getValue(SERVICE_AUTHORIZATION),
                userInfo.getUid(),
                coreCaseDataJurisdictionId,
                coreCaseDataCaseTypeId,
                eventId);
    }

    private CaseDetails submitForCaseworker(Headers authorizationHeaders, UserInfo userInfo,  CaseDataContent caseDataContent) {

        return coreCaseDataApi.submitForCaseworker(
                authorizationHeaders.getValue(AUTHORIZATION),
                authorizationHeaders.getValue(SERVICE_AUTHORIZATION),
                userInfo.getUid(),
                coreCaseDataJurisdictionId,
                coreCaseDataCaseTypeId,
                true,
                caseDataContent
        );
    }

    private StartEventResponse startCaseForCitizen(Headers authorizationHeaders, UserInfo userInfo, String eventId) {
        return coreCaseDataApi.startForCitizen(
                authorizationHeaders.getValue(AUTHORIZATION),
                authorizationHeaders.getValue(SERVICE_AUTHORIZATION),
                userInfo.getUid(),
                coreCaseDataJurisdictionId,
                coreCaseDataCaseTypeId,
                eventId);
    }

    private CaseDetails submitForCitizen(Headers authorizationHeaders,UserInfo userInfo,  CaseDataContent caseDataContent) {
        return coreCaseDataApi.submitForCitizen(
                authorizationHeaders.getValue(AUTHORIZATION),
                authorizationHeaders.getValue(SERVICE_AUTHORIZATION),
                userInfo.getUid(),
                coreCaseDataJurisdictionId,
                coreCaseDataCaseTypeId,
                true,
                caseDataContent
        );
    }

    public Document uploadDocument(DocumentNames document, Headers authorizationHeaders, UserInfo userInfo) {

        IdamTokens idamTokens = IdamTokens.builder()
                .idamOauth2Token(authorizationHeaders.getValue(AUTHORIZATION))
                .serviceAuthorization(authorizationHeaders.getValue(SERVICE_AUTHORIZATION))
                .userId(userInfo.getUid())
                .build();

        Optional<Resource> maybeResource = documentResources.stream()
                .filter(res -> {
                    String filename = formatFileName(res.getFilename());
                    return filename.equals(document.toString());
                }).findFirst();

        if (maybeResource.isPresent()) {

            Resource documentResource = maybeResource.get();

            String filename = documentResource.getFilename().toUpperCase();


            String contentType;

            if (filename.endsWith(".PDF")) {
                contentType = "application/pdf";

            } else if (filename.endsWith(".DOC")) {
                contentType = "application/msword";

            } else if (filename.endsWith(".DOCX")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

            } else {
                throw new RuntimeException("Missing content type mapping for document: " + filename);
            }
            System.out.println("uploading document.....");
            return cdamDocumentManagementUploader.upload(documentResource, contentType, idamTokens);

        } else {
            throw new IllegalStateException(
                    String.format("Resource for document '{}' not found", document));
        }
    }

    public Document getDocument(DocumentNames document, Headers authorizationHeaders, UserInfo userInfo) {
        return uploadDocument(document, authorizationHeaders, userInfo);
    }

    private String formatFileName(String fileName) {
        return fileName
                .replace(".", "_")
                .replace("-", "_")
                .toUpperCase();
    }

    private String toJsonString(Object object) {
        String json = null;

        try {
            json = new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }
}
