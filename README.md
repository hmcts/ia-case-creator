## IA Case Creator ##

Tool to create IA case in CCD.

### Build ###
```bash
./gradlew clean build
```

A jar ia-case-creator-[version].jar file will be built in the build/libs directory. Copy this jar file to an empty
directory and create yaml files for the environments you want to connect to. Each file should be called 
application-[env name].yaml and go in the same directory as the jar. 

It should contain values for

```yaml
core_case_data:
  api:
    url: 

auth:
  idam:
    client:
      baseUrl: 
  provider:
    service:
      client:
        baseUrl: 

idam:
  url: 
  s2s-auth:
    totp_secret: 
    url: 
  oauth2:
    user:
      email: 
      password: 
    client:
      id:
      secret: 
    redirectUrl:    
```

### Operation ###
```bash
java -jar ia-case-creator.jar
Required arguments
    --spring.profiles.active=[env name] need a application-[env name].yaml file in the same directory as the jar
Optional arguments
    --h --help This usage message
    --file=[Path to base json file] json file to use as the base of case leave out for a default case
    --multiple=[number] number of cases to create
    --headers just prints out auth headers
```

To change the values entered in CCD you can provide a base JSON file of the form and set the argument --file=filename
when you run the script.
```json
{
  "appellantHasFixedAddress": "No",
  "legalRepReferenceNumber": "some-ref",
  "appealResponseDescription": "This is the appeal response",
  "appealGroundsForDisplay": [
    "protectionRefugeeConvention"
  ],
  "legalRepresentativeDocuments": [
    {
      "id": "3",
      "value": {
        "tag": "caseArgument",
        "document": {
          "document_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/c9ef225a-2759-459d-a62a-4eb9893c2f8b",
          "document_filename": "CaseArgument.pdf",
          "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/c9ef225a-2759-459d-a62a-4eb9893c2f8b/binary"
        },
        "description": "This is the case argument",
        "dateUploaded": "2019-08-22"
      }
    },
    {
      "id": "2",
      "value": {
        "tag": "caseArgument",
        "document": {
          "document_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/bf188593-d281-4fb9-9c06-31b247919be2",
          "document_filename": "CaseArgumentEvidence.pdf",
          "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/bf188593-d281-4fb9-9c06-31b247919be2/binary"
        },
        "description": "The is the case argument evidence",
        "dateUploaded": "2019-08-22"
      }
    },
    {
      "id": "1",
      "value": {
        "tag": "appealSubmission",
        "document": {
          "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/f67e0798-a58e-417d-afb9-cd21f9742f00",
          "document_filename": "PA 53816 2019-Gonzlez-appeal-form.PDF",
          "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/f67e0798-a58e-417d-afb9-cd21f9742f00/binary"
        },
        "description": "",
        "dateUploaded": "2019-08-22"
      }
    }
  ],
  "hasOtherAppeals": "No",
  "appealGroundsHumanRights": {
    "values": []
  },
  "newMatters": "Birth of a child",
  "caseArgumentAvailable": "Yes",
  "caseArgumentDescription": "This is the case argument",
  "appealReferenceNumber": "PA/53816/2019",
  "uploadAddendumEvidenceActionAvailable": "No",
  "appealType": "protection",
  "appellantGivenNames": "José",
  "appellantFamilyName": "González",
  "hearingCentre": "taylorHouse",
  "appellantNationalities": [
    {
      "id": "48e203e6-004c-48c5-be8c-5052538578e8",
      "value": {
        "code": "FI"
      }
    }
  ],
  "homeOfficeDecisionDate": "2019-08-22",
  "changeDirectionDueDateActionAvailable": "Yes",
  "submissionOutOfTime": "No",
  "respondentDocuments": [
    {
      "id": "3",
      "value": {
        "tag": "appealResponse",
        "document": {
          "document_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/07579367-bf17-4da4-b7c8-c7781b2128a3",
          "document_filename": "AppealResponse.pdf",
          "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/07579367-bf17-4da4-b7c8-c7781b2128a3/binary"
        },
        "description": "This is the appeal response",
        "dateUploaded": "2019-08-22"
      }
    },
    {
      "id": "2",
      "value": {
        "tag": "appealResponse",
        "document": {
          "document_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/ed62d5f5-485a-4b70-b5c8-54b0f17a9713",
          "document_filename": "AppealResponseEvidence.pdf",
          "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/ed62d5f5-485a-4b70-b5c8-54b0f17a9713/binary"
        },
        "description": "This is the appeal response evidence",
        "dateUploaded": "2019-08-22"
      }
    },
    {
      "id": "1",
      "value": {
        "tag": "respondentEvidence",
        "document": {
          "document_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/e8997310-d91a-4088-95ef-0c88224ab4b3",
          "document_filename": "RespondentEvidence.pdf",
          "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/e8997310-d91a-4088-95ef-0c88224ab4b3/binary"
        },
        "description": "This is the respondent evidence",
        "dateUploaded": "2019-08-22"
      }
    }
  ],
  "appealResponseEvidence": [
    {
      "id": "b2a7a7d4-461e-4f59-92f3-d4e011240935",
      "value": {
        "document": {
          "document_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/ed62d5f5-485a-4b70-b5c8-54b0f17a9713",
          "document_filename": "AppealResponseEvidence.pdf",
          "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/ed62d5f5-485a-4b70-b5c8-54b0f17a9713/binary"
        },
        "description": "This is the appeal response evidence"
      }
    }
  ],
  "legalRepDeclaration": [
    "hasDeclared"
  ],
  "appealGroundsProtection": {
    "values": [
      "protectionRefugeeConvention"
    ]
  },
  "appealResponseDocument": {
    "document_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/07579367-bf17-4da4-b7c8-c7781b2128a3",
    "document_filename": "AppealResponse.pdf",
    "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/07579367-bf17-4da4-b7c8-c7781b2128a3/binary"
  },
  "appellantDateOfBirth": "1999-12-31",
  "checklist": {
    "checklist1": [
      "isAdult"
    ],
    "checklist2": [
      "isNotDetained"
    ],
    "checklist3": [
      "isNotFamilyAppeal"
    ],
    "checklist4": [
      "isWithinPostcode"
    ],
    "checklist5": [
      "isResidingInUK"
    ],
    "checklist6": [
      "isNotStateless"
    ]
  },
  "caseArgumentEvidence": [
    {
      "id": "e389bdf1-111d-4f34-bbf7-05a71584d650",
      "value": {
        "document": {
          "document_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/bf188593-d281-4fb9-9c06-31b247919be2",
          "document_filename": "CaseArgumentEvidence.pdf",
          "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/bf188593-d281-4fb9-9c06-31b247919be2/binary"
        },
        "description": "The is the case argument evidence"
      }
    }
  ],
  "appealResponseAvailable": "Yes",
  "currentCaseStateVisibleToCaseOfficer": "listing",
  "directions": [
    {
      "id": "5",
      "value": {
        "tag": "legalRepresentativeHearingRequirements",
        "dateDue": "2019-08-27",
        "parties": "legalRepresentative",
        "dateSent": "2019-08-22",
        "explanation": "Your appeal is going to a hearing. Login to submit your hearing requirements on the overview tab.\n\nNext steps\nThe case officer will review your hearing requirements and try to accommodate them. You will then be sent a hearing date.\nIf you do not supply your hearing requirements within 5 days, we may not be able to accommodate your needs for the hearing.\n"
      }
    },
    {
      "id": "4",
      "value": {
        "tag": "legalRepresentativeReview",
        "dateDue": "2019-08-27",
        "parties": "legalRepresentative",
        "dateSent": "2019-08-22",
        "explanation": "The respondent has replied to your appeal argument and evidence. You must now review their response.\n\nNext steps\nYou have 5 days to review the response. If you want to respond to what the Home Office has said, you should email the case officer.\n\nIf you do not respond within 5 days, the case will automatically go to hearing."
      }
    },
    {
      "id": "3",
      "value": {
        "tag": "respondentReview",
        "dateDue": "2019-09-05",
        "parties": "respondent",
        "dateSent": "2019-08-22",
        "explanation": "You must now review this case.\n\nYou have 14 days to review the appeal argument and evidence. You must explain whether the appellant\u0027s appeal argument makes a valid case for overturning the original protection decision.\n\nYou must respond to the case officer and tell them:\n- whether you oppose all or part of the appellant\u0027s case\n- what your grounds are for opposing the case\n- which of the issues are agreed or not agreed\n- whether there are any further issues you wish to raise\n- whether you are prepared to withdraw to grant or reconsider\n- whether the appeal can be resolved without a hearing\n\nYou may find it helpful to use the response template provided.\n\nNext steps\n\nIf you do not respond in time, the case officer will decide how the case should proceed."
      }
    },
    {
      "id": "2",
      "value": {
        "tag": "buildCase",
        "dateDue": "2019-09-19",
        "parties": "legalRepresentative",
        "dateSent": "2019-08-22",
        "explanation": "You must now build your case by uploading your appeal argument and evidence.\n\nAdvice on writing an appeal argument\nYou must write a full argument that references:\n- all the evidence you have or plan to rely on, including any witness statements\n- the grounds and issues of the case\n- any new matters\n- any legal authorities you plan to rely on and why they are applicable to your case\n\nYour argument must explain why you believe the respondent\u0027s decision is wrong. You must provide all the information for the Home Office to conduct a thorough review of their decision at this stage.\n\nNext steps\nOnce you have uploaded your appeal argument and all evidence, submit your case. The case officer will then review everything you\u0027ve added. If your case looks ready, the case officer will send it to the respondent for their review. The respondent then has 14 days to respond."
      }
    },
    {
      "id": "1",
      "value": {
        "tag": "respondentEvidence",
        "dateDue": "2019-09-05",
        "parties": "respondent",
        "dateSent": "2019-08-22",
        "explanation": "A notice of appeal has been lodged against this asylum decision.\n\nYou must now send all documents to the case officer. The case officer will send them to the other party. You have 14 days to supply these documents.\n\nYou must include:\n- the notice of decision\n- any other document provided to the appellant giving reasons for that decision\n- any statements of evidence\n- the application form\n- any record of interview with the appellant in relation to the decision being appealed\n- any other unpublished documents on which you rely\n- the notice of any other appealable decision made in relation to the appellant"
      }
    }
  ],
  "appellantNameForDisplay": "José González",
  "appellantTitle": "Mr",
  "caseArgumentDocument": {
    "document_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/c9ef225a-2759-459d-a62a-4eb9893c2f8b",
    "document_filename": "CaseArgument.pdf",
    "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal:443/documents/c9ef225a-2759-459d-a62a-4eb9893c2f8b/binary"
  },
  "sendDirectionActionAvailable": "Yes",
  "hasNewMatters": "Yes",
  "homeOfficeReferenceNumber": "A123456"
}
```
