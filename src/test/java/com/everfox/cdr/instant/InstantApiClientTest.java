/**
 * Copyright 2026 Everfox
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.everfox.cdr.instant;

import org.junit.jupiter.api.Test;

import com.everfox.cdr.MimeType;
import com.everfox.cdr.Region;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

class InstantApiClientTest {

    @Test
    void testConfigWithRegion() {
        InstantApiConfig config = new InstantApiConfig("test-key", Region.US_WEST_2);

        assertEquals("test-key", config.getApiKey());
        assertEquals("https://us-west-2.aws.instant.cdr.everfox.com/v1", config.getBaseUrl().toString());
    }

    @Test
    void testConfigWithCustomUrl() {
        InstantApiConfig config = new InstantApiConfig("test-key", URI.create("https://custom.example.com"), 5, 30);

        assertEquals("test-key", config.getApiKey());
        assertEquals("https://custom.example.com", config.getBaseUrl().toString());
        assertEquals(5, config.getConnectTimeoutSeconds());
        assertEquals(30, config.getRequestTimeoutSeconds());
    }

    @Test
    void testRequestCreation() {
        byte[] data = "test data".getBytes();
        InstantApiRequest request = new InstantApiRequest(
                data,
                MimeType.PDF,
                MimeType.PDF
        );

        assertArrayEquals(data, request.getFileData());
        assertEquals("application/pdf", request.getContentType());
        assertEquals("application/pdf", request.getAcceptType());
        assertNull(request.getOptions());
    }

    @Test
    void testRequestOptionsJson() {
        RequestOptions options = new RequestOptions();
        options.setReporting(RequestOptions.ReportFormat.FULL);

        RequestOptions.RiskOptions risks = new RequestOptions.RiskOptions();
        risks.setAllowMacros(false);
        risks.setAllowExecutables(false);
        options.setRisks(risks);

        String json = options.toJson();
        assertNotNull(json);
        assertTrue(json.contains("FULL"));
        assertTrue(json.contains("allowMacros"));
    }
}
