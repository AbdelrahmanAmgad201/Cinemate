package org.example.backend.hateSpeech;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class HateSpeechServiceTest {

    private RestTemplate restTemplate;
    private HateSpeechService hateSpeechService;
    private static final String TEST_URL = "http://test-hate-speech-api.com/analyze";

    @BeforeEach
    void setup() {
        restTemplate = mock(RestTemplate.class);
        hateSpeechService = new HateSpeechService(restTemplate);
        ReflectionTestUtils.setField(hateSpeechService, "url", TEST_URL);
    }

    // =============== AnalyzeText - Positive Cases ===============
    @Test
    void testAnalyzeText_ReturnsTrue_WhenHateSpeechDetected() {
        String hatefulText = "This is offensive content";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(true, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        boolean result = hateSpeechService.analyzeText(hatefulText);

        assertTrue(result);
        verify(restTemplate).postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class));
    }

    @Test
    void testAnalyzeText_ReturnsFalse_WhenNoHateSpeechDetected() {
        String cleanText = "This is a nice message";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        boolean result = hateSpeechService.analyzeText(cleanText);

        assertFalse(result);
        verify(restTemplate).postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class));
    }

    // =============== Request Construction Tests ===============
    @Test
    void testAnalyzeText_SendsCorrectHeaders() {
        String text = "Test message";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(TEST_URL), httpEntityCaptor.capture(), eq(Boolean.class)))
                .thenReturn(mockResponse);

        hateSpeechService.analyzeText(text);

        HttpEntity<String> capturedEntity = httpEntityCaptor.getValue();
        HttpHeaders headers = capturedEntity.getHeaders();

        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
    }

    @Test
    void testAnalyzeText_SendsCorrectJsonBody() {
        String text = "Test message";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(TEST_URL), httpEntityCaptor.capture(), eq(Boolean.class)))
                .thenReturn(mockResponse);

        hateSpeechService.analyzeText(text);

        HttpEntity<String> capturedEntity = httpEntityCaptor.getValue();
        String body = capturedEntity.getBody();

        assertEquals("{\"text\":\"Test message\"}", body);
    }

    @Test
    void testAnalyzeText_EscapesQuotesInText() {
        String textWithQuotes = "This is a \"quoted\" message";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(TEST_URL), httpEntityCaptor.capture(), eq(Boolean.class)))
                .thenReturn(mockResponse);

        hateSpeechService.analyzeText(textWithQuotes);

        HttpEntity<String> capturedEntity = httpEntityCaptor.getValue();
        String body = capturedEntity.getBody();

        // Verify quotes are escaped
        assertEquals("{\"text\":\"This is a \\\"quoted\\\" message\"}", body);
        assertTrue(body.contains("\\\""));
    }

    @Test
    void testAnalyzeText_HandlesMultipleQuotes() {
        String textWithMultipleQuotes = "\"Hello\" \"World\" \"Test\"";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(TEST_URL), httpEntityCaptor.capture(), eq(Boolean.class)))
                .thenReturn(mockResponse);

        hateSpeechService.analyzeText(textWithMultipleQuotes);

        HttpEntity<String> capturedEntity = httpEntityCaptor.getValue();
        String body = capturedEntity.getBody();

        assertEquals("{\"text\":\"\\\"Hello\\\" \\\"World\\\" \\\"Test\\\"\"}", body);
    }

    // =============== Edge Cases ===============
    @Test
    void testAnalyzeText_EmptyString() {
        String emptyText = "";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        boolean result = hateSpeechService.analyzeText(emptyText);

        assertFalse(result);
        verify(restTemplate).postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class));
    }

    @Test
    void testAnalyzeText_VeryLongText() {
        String longText = "a".repeat(10000);
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        boolean result = hateSpeechService.analyzeText(longText);

        assertFalse(result);
        verify(restTemplate).postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class));
    }

    @Test
    void testAnalyzeText_SpecialCharacters() {
        String textWithSpecialChars = "Hello! @#$%^&*() {}[]|\\:;<>?,./~`";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(TEST_URL), httpEntityCaptor.capture(), eq(Boolean.class)))
                .thenReturn(mockResponse);

        hateSpeechService.analyzeText(textWithSpecialChars);

        HttpEntity<String> capturedEntity = httpEntityCaptor.getValue();
        String body = capturedEntity.getBody();

        assertNotNull(body);
        assertTrue(body.contains("Hello!"));
    }

    @Test
    void testAnalyzeText_WithNewlines() {
        String textWithNewlines = "Line 1\nLine 2\nLine 3";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        boolean result = hateSpeechService.analyzeText(textWithNewlines);

        assertFalse(result);
        verify(restTemplate).postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class));
    }

    @Test
    void testAnalyzeText_WithTabs() {
        String textWithTabs = "Column1\tColumn2\tColumn3";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        boolean result = hateSpeechService.analyzeText(textWithTabs);

        assertFalse(result);
        verify(restTemplate).postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class));
    }

    @Test
    void testAnalyzeText_UnicodeCharacters() {
        String textWithUnicode = "Hello 世界 🌍 مرحبا";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        boolean result = hateSpeechService.analyzeText(textWithUnicode);

        assertFalse(result);
        verify(restTemplate).postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class));
    }

    // =============== Response Handling Tests ===============
    @Test
    void testAnalyzeText_NullResponseBody_ThrowsNullPointerException() {
        String text = "Test message";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        // The service will throw NullPointerException when trying to unbox null Boolean
        assertThrows(NullPointerException.class, () -> hateSpeechService.analyzeText(text));
    }

    @Test
    void testAnalyzeText_DifferentHttpStatus() {
        String text = "Test message";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(true, HttpStatus.CREATED);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        boolean result = hateSpeechService.analyzeText(text);

        assertTrue(result);
    }

    // =============== Exception Handling Tests ===============
    @Test
    void testAnalyzeText_RestClientException() {
        String text = "Test message";

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenThrow(new RestClientException("Connection failed"));

        assertThrows(RestClientException.class, () -> hateSpeechService.analyzeText(text));
    }

    @Test
    void testAnalyzeText_NetworkTimeout() {
        String text = "Test message";

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenThrow(new RestClientException("Timeout"));

        assertThrows(RestClientException.class, () -> hateSpeechService.analyzeText(text));
    }

    @Test
    void testAnalyzeText_ServerError() {
        String text = "Test message";

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenThrow(new RestClientException("500 Internal Server Error"));

        assertThrows(RestClientException.class, () -> hateSpeechService.analyzeText(text));
    }

    // =============== URL Configuration Tests ===============
    @Test
    void testAnalyzeText_UsesCorrectUrl() {
        String text = "Test message";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        hateSpeechService.analyzeText(text);

        verify(restTemplate).postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class));
    }

    @Test
    void testAnalyzeText_WithDifferentUrl() {
        String differentUrl = "http://different-api.com/check";
        ReflectionTestUtils.setField(hateSpeechService, "url", differentUrl);

        String text = "Test message";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(differentUrl), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        hateSpeechService.analyzeText(text);

        verify(restTemplate).postForEntity(eq(differentUrl), any(HttpEntity.class), eq(Boolean.class));
    }

    // =============== Multiple Calls Tests ===============
    @Test
    void testAnalyzeText_MultipleCallsIndependent() {
        String text1 = "First message";
        String text2 = "Second message";

        ResponseEntity<Boolean> response1 = new ResponseEntity<>(true, HttpStatus.OK);
        ResponseEntity<Boolean> response2 = new ResponseEntity<>(false, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(response1)
                .thenReturn(response2);

        boolean result1 = hateSpeechService.analyzeText(text1);
        boolean result2 = hateSpeechService.analyzeText(text2);

        assertTrue(result1);
        assertFalse(result2);
        verify(restTemplate, times(2)).postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class));
    }

    @Test
    void testAnalyzeText_ConsecutiveCalls() {
        String text = "Same message";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(true, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(mockResponse);

        boolean result1 = hateSpeechService.analyzeText(text);
        boolean result2 = hateSpeechService.analyzeText(text);
        boolean result3 = hateSpeechService.analyzeText(text);

        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
        verify(restTemplate, times(3)).postForEntity(eq(TEST_URL), any(HttpEntity.class), eq(Boolean.class));
    }

    // =============== JSON Escaping Tests ===============
    @Test
    void testAnalyzeText_HandlesBackslash() {
        String textWithBackslash = "Path: C:\\Users\\Admin";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(TEST_URL), httpEntityCaptor.capture(), eq(Boolean.class)))
                .thenReturn(mockResponse);

        hateSpeechService.analyzeText(textWithBackslash);

        HttpEntity<String> capturedEntity = httpEntityCaptor.getValue();
        String body = capturedEntity.getBody();

        assertNotNull(body);
        assertTrue(body.contains("Path:"));
    }

    @Test
    void testAnalyzeText_OnlyQuotes() {
        String onlyQuotes = "\"\"\"";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(TEST_URL), httpEntityCaptor.capture(), eq(Boolean.class)))
                .thenReturn(mockResponse);

        hateSpeechService.analyzeText(onlyQuotes);

        HttpEntity<String> capturedEntity = httpEntityCaptor.getValue();
        String body = capturedEntity.getBody();

        assertEquals("{\"text\":\"\\\"\\\"\\\"\"}", body);
    }

    @Test
    void testAnalyzeText_MixedQuotesAndText() {
        String mixed = "He said \"Hello\" and she replied \"Hi\"";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);

        ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(TEST_URL), httpEntityCaptor.capture(), eq(Boolean.class)))
                .thenReturn(mockResponse);

        hateSpeechService.analyzeText(mixed);

        HttpEntity<String> capturedEntity = httpEntityCaptor.getValue();
        String body = capturedEntity.getBody();

        assertTrue(body.contains("\\\"Hello\\\""));
        assertTrue(body.contains("\\\"Hi\\\""));
    }
}