package uk.gov.hmcts.reform.ia.casecreator;

import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.document.utils.InMemoryMultipartFile;
import uk.gov.hmcts.reform.ia.casecreator.idam.IdamTokens;

import java.util.Collections;

/**
 * This class supersedes DMDocumentManagementUploader. Its usage is driven by a feature flag.
 */
@Service
public class CdamDocumentManagementUploader {

    @Autowired
    CaseDocumentClientApi caseDocumentClientApi;

    @SneakyThrows
    public Document upload(Resource resource, String contentType, IdamTokens idamTokens) {
        final String serviceAuthorizationToken = idamTokens.getServiceAuthorization();
        final String accessToken = idamTokens.getIdamOauth2Token();

        MultipartFile file = new InMemoryMultipartFile(
            resource.getFilename(),
            resource.getFilename(),
            contentType,
            ByteStreams.toByteArray(resource.getInputStream())
        );

        DocumentUploadRequest testDoc = new DocumentUploadRequest(Classification.RESTRICTED.toString(),"Asylum","IA",
                Collections.singletonList(file));

        UploadResponse uploadResponse = caseDocumentClientApi.uploadDocuments(
            accessToken,
            serviceAuthorizationToken,
            testDoc
        );

        uk.gov.hmcts.reform.ccd.document.am.model.Document uploadedDocument = uploadResponse.getDocuments()
                                                                                  .stream().findFirst().orElseThrow(() ->
                                                                                                                        new IllegalStateException("Document cannot be uploaded, please try again"));

        System.out.println("Document - url: " + uploadedDocument.links.self.href);
        System.out.println("Document - binary url: " + uploadedDocument.links.binary.href);

        return new Document(
            uploadedDocument.links.self.href,
            uploadedDocument.links.binary.href,
            uploadedDocument.originalDocumentName
        );
    }
}

