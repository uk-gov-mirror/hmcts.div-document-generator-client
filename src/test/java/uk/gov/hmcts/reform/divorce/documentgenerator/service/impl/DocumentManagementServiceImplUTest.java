package uk.gov.hmcts.reform.divorce.documentgenerator.service.impl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.divorce.documentgenerator.config.TemplateConfiguration;
import uk.gov.hmcts.reform.divorce.documentgenerator.domain.response.FileUploadResponse;
import uk.gov.hmcts.reform.divorce.documentgenerator.domain.response.GeneratedDocumentInfo;
import uk.gov.hmcts.reform.divorce.documentgenerator.factory.PDFGenerationFactory;
import uk.gov.hmcts.reform.divorce.documentgenerator.service.EvidenceManagementService;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DocumentManagementServiceImplUTest {

    private static final String A_TEMPLATE = "a-certain-template";
    private static final String A_TEMPLATE_FILE_NAME = "fileName.pdf";
    private static final String TEST_AUTH_TOKEN = "someToken";
    private static final byte[] TEST_GENERATED_DOCUMENT = new byte[] {1};

    private FileUploadResponse expectedFileUploadResponse;

    @Rule
    public ExpectedException expectedException = none();

    @Mock
    private PDFGenerationFactory pdfGenerationFactory;

    @Mock
    private PDFGenerationServiceImpl pdfGenerationService;//TODO - this is wrong, but it passes the tests for now

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private TemplateConfiguration templateConfiguration;

    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    @InjectMocks
    private DocumentManagementServiceImpl classUnderTest;

    @Before
    public void setUp() {
        expectedFileUploadResponse = new FileUploadResponse(HttpStatus.OK);
        expectedFileUploadResponse.setFileUrl("someUrl");
        expectedFileUploadResponse.setMimeType("someMimeType");
        expectedFileUploadResponse.setCreatedOn("someCreatedDate");
    }

    @Test
    public void givenTemplateNameIsAosInvitation_whenGenerateAndStoreDocument_thenProceedAsExpected() {
        when(pdfGenerationFactory.getGeneratorService(A_TEMPLATE)).thenReturn(pdfGenerationService);
        when(pdfGenerationService.generate(eq(A_TEMPLATE), any())).thenReturn(TEST_GENERATED_DOCUMENT);
        when(templateConfiguration.getFileNameByTemplateName(A_TEMPLATE)).thenReturn(A_TEMPLATE_FILE_NAME);
        when(evidenceManagementService.storeDocumentAndGetInfo(eq(TEST_GENERATED_DOCUMENT), eq(TEST_AUTH_TOKEN), eq(A_TEMPLATE_FILE_NAME))).thenReturn(expectedFileUploadResponse);

        GeneratedDocumentInfo generatedDocumentInfo = classUnderTest.generateAndStoreDocument(A_TEMPLATE, new HashMap<>(), TEST_AUTH_TOKEN);

        assertGeneratedDocumentInfoIsAsExpected(generatedDocumentInfo);
        verify(evidenceManagementService).storeDocumentAndGetInfo(eq(TEST_GENERATED_DOCUMENT), eq(TEST_AUTH_TOKEN), eq(A_TEMPLATE_FILE_NAME));
    }

    @Test
    public void givenTemplateNameIsInvalid_whenGenerateAndStoreDocument_thenThrowException() {
        String unknownTemplateName = "unknown-template";
        when(templateConfiguration.getFileNameByTemplateName(unknownTemplateName)).thenThrow(new IllegalArgumentException("Unknown template: " + unknownTemplateName));

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(equalTo("Unknown template: " + unknownTemplateName));

        classUnderTest.generateAndStoreDocument(unknownTemplateName, new HashMap<>(), "some-auth-token");
    }

    @Test
    public void whenStoreDocument_thenProceedAsExpected() {
        when(evidenceManagementService.storeDocumentAndGetInfo(TEST_GENERATED_DOCUMENT, TEST_AUTH_TOKEN, A_TEMPLATE_FILE_NAME)).thenReturn(expectedFileUploadResponse);

        GeneratedDocumentInfo generatedDocumentInfo = classUnderTest.storeDocument(TEST_GENERATED_DOCUMENT, TEST_AUTH_TOKEN, A_TEMPLATE_FILE_NAME);

        assertGeneratedDocumentInfoIsAsExpected(generatedDocumentInfo);
        verify(evidenceManagementService).storeDocumentAndGetInfo(TEST_GENERATED_DOCUMENT, TEST_AUTH_TOKEN, A_TEMPLATE_FILE_NAME);
    }

    @Test
    public void givenPdfGeneratorIsUsed_whenGenerateDocumentWithHtmlCharacters_thenEscapeHtmlCharacters() {
        final Map<String, Object> placeholderMap = new HashMap<>();
        placeholderMap.put("htmlValue", "<b>This should be escaped</b>");

        when(pdfGenerationFactory.getGeneratorService(A_TEMPLATE)).thenReturn(pdfGenerationService);
        when(pdfGenerationService.generate(eq(A_TEMPLATE), any())).thenReturn(TEST_GENERATED_DOCUMENT);
        //TODO - what tests that the formatter is not called? check coverage
        //TODO - should this be done in the documentGeneratorService?

        byte[] generatedDocument = classUnderTest.generateDocument(A_TEMPLATE, placeholderMap);

        assertThat(generatedDocument, equalTo(TEST_GENERATED_DOCUMENT));
        verify(pdfGenerationFactory).getGeneratorService(A_TEMPLATE);
        verify(pdfGenerationService).generate(eq(A_TEMPLATE), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue(), hasEntry("htmlValue", "&lt;b&gt;This should be escaped&lt;/b&gt;"));
    }

    private void assertGeneratedDocumentInfoIsAsExpected(GeneratedDocumentInfo generatedDocumentInfo) {
        assertThat(generatedDocumentInfo.getUrl(), equalTo("someUrl"));
        assertThat(generatedDocumentInfo.getMimeType(), equalTo("someMimeType"));
        assertThat(generatedDocumentInfo.getCreatedOn(), equalTo("someCreatedDate"));
    }

}
