package com.example.conversationHub;

import com.example.conversationHub.config.HttpsEnforcementFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class HttpsEnforcementFilterTest {

    @Test
    void redirectsPlainHttpToHttpsWhenEnabled() throws Exception {
        MockHttpServletRequest request = request("http");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new HttpsEnforcementFilter(true).doFilter(request, response, new MockFilterChain());

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://localhost:8080/api/public/services?lang=sw");
    }

    @Test
    void acceptsForwardedHttpsAndSecureRequests() throws Exception {
        MockHttpServletRequest forwarded = request("http");
        forwarded.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse forwardedResponse = new MockHttpServletResponse();
        MockFilterChain forwardedChain = new MockFilterChain();

        new HttpsEnforcementFilter(true).doFilter(forwarded, forwardedResponse, forwardedChain);

        assertThat(forwardedResponse.getRedirectedUrl()).isNull();
        assertThat(forwardedChain.getRequest()).isSameAs(forwarded);

        MockHttpServletRequest secure = request("https");
        MockHttpServletResponse secureResponse = new MockHttpServletResponse();
        MockFilterChain secureChain = new MockFilterChain();

        new HttpsEnforcementFilter(true).doFilter(secure, secureResponse, secureChain);

        assertThat(secureResponse.getRedirectedUrl()).isNull();
        assertThat(secureChain.getRequest()).isSameAs(secure);
    }

    @Test
    void passesThroughWhenDisabled() throws Exception {
        MockHttpServletRequest request = request("http");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new HttpsEnforcementFilter(false).doFilter(request, response, chain);

        assertThat(response.getRedirectedUrl()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private static MockHttpServletRequest request(String scheme) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme(scheme);
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setRequestURI("/api/public/services");
        request.setQueryString("lang=sw");
        return request;
    }
}
