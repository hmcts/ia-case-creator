package uk.gov.hmcts.reform.ia.casecreator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
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
import uk.gov.hmcts.reform.ia.casecreator.idam.IdamService;
import uk.gov.hmcts.reform.ia.casecreator.idam.IdamTokens;
import uk.gov.hmcts.reform.idam.client.IdamClient;

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

@Service
public class CcdCaseCreator {

    private Collection<Resource> documentResources;
    private final CdamDocumentManagementUploader cdamDocumentManagementUploader;
    private final IdamService idamService;
    private final CoreCaseDataApi coreCaseDataApi;
    private final String idamToken;
    private final String userId;
    private final String idamUserRole;
    private final String coreCaseDataJurisdictionId;
    private final String coreCaseDataCaseTypeId;

    private final IdamClient idamClient;

    private String idamUsername;
    private String idamPassword;

    @Autowired
    public CcdCaseCreator(CdamDocumentManagementUploader cdamDocumentManagementUploader, IdamService idamService,
                          CoreCaseDataApi coreCaseDataApi,
                          @Value("${core_case_data.jurisdictionId}") String coreCaseDataJurisdictionId,
                          @Value("${core_case_data.caseTypeId}") String coreCaseDataCaseTypeId,
                          @Value("${idam_token}") String idamToken,
                          @Value("${idam_user_id}") String userId,
                          @Value("${idam_user_role}") String idamUserRole,
                          IdamClient idamClient,
                          @Value("${migration.idam.username}") String idamUsername,
                          @Value("${migration.idam.password}") String idamPassword
                          ) {
        this.cdamDocumentManagementUploader = cdamDocumentManagementUploader;
        this.idamService = idamService;
        this.coreCaseDataJurisdictionId = coreCaseDataJurisdictionId;
        this.coreCaseDataCaseTypeId = coreCaseDataCaseTypeId;
        this.coreCaseDataApi = coreCaseDataApi;
        this.idamToken = "Bearer " + idamToken;
        this.userId = userId;
        this.idamUserRole = idamUserRole;
        this.idamClient = idamClient;
        this.idamUsername = idamUsername;
        this.idamPassword = idamPassword;

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

        documentResources =
                BinaryResourceLoader
                        .load("/documents/*")
                        .values();

        String userToken = idamClient.authenticateUser(idamUsername, idamPassword);
        System.out.println(userToken);

        String serviceAuthorizationToken = idamService.generateServiceAuthorization();
        IdamTokens idamTokens = IdamTokens.builder()
                .idamOauth2Token(userToken)
                .serviceAuthorization(serviceAuthorizationToken)
                .userId(userId)
                .build();

        Document noticeOfAppealDocument = getDocument(NOTICE_OF_APPEAL_PDF, idamTokens);

        StartEventResponse createAppeal = idamUserRole.equals("citizen") ?
                startCaseForCitizen(idamTokens, "createDLRMCase") :
                startCaseForCaseworker(idamTokens, "createDLRMCase");

        Long saaa = createAppeal.getCaseDetails().getId();

        InputStream caseStream = (ccdDefinitionFile == null) ?
                getClass().getClassLoader().getResourceAsStream("json/new_example.json") :
                getStreamFromFile(ccdDefinitionFile);

        String iaData = IOUtils.toString(caseStream, Charset.defaultCharset().name());
        iaData = iaData.replace("\"{$NOTICE_OF_DECISION_DOCUMENT}\"", toJsonString(noticeOfAppealDocument));


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

    public Document uploadDocument(DocumentNames document, IdamTokens idamTokens) {

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

    public Document getDocument(DocumentNames document, IdamTokens idamTokens) {
        return uploadDocument(document, idamTokens);
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
