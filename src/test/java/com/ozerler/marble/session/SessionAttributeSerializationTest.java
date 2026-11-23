package com.ozerler.marble.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Objects that actually land in the HTTP session must survive Java serialization.
 * Spring Session JDBC stores attribute bytes that way. Redis will use the same bytes.
 */
class SessionAttributeSerializationTest {

    @Test
    @DisplayName("SecurityContext, form draft, and flash strings serialize")
    void sessionPayloadSurvivesJavaSerialization() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext());
        payload.put("preservedForm", formDraft());
        payload.put("preservedFormAction", "/erp/blocks/new");
        payload.put("invalidFields", List.copyOf(List.of("blockCode")));
        payload.put("successMessage", "Blok kaydedildi");

        Map<String, Object> restored = deserialize(serialize(payload));

        SecurityContext context = (SecurityContext) restored.get(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication().getName()).isEqualTo("admin@eimece.test");
        assertThat(context.getAuthentication().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_EXECUTIVE");
        assertThat(context.getAuthentication().getDetails()).isInstanceOf(WebAuthenticationDetails.class);

        @SuppressWarnings("unchecked")
        Map<String, List<String>> form = (Map<String, List<String>>) restored.get("preservedForm");
        assertThat(form.get("customerName")).containsExactly("Özerler");
        assertThat(restored.get("invalidFields")).isEqualTo(List.of("blockCode"));
        assertThat(restored.get("successMessage")).isEqualTo("Blok kaydedildi");
    }

    private static SecurityContext securityContext() {
        UserDetails user = User.withUsername("admin@eimece.test")
                .password("{noop}secret")
                .roles("ADMIN", "EXECUTIVE")
                .build();
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                user,
                user.getPassword(),
                user.getAuthorities());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        authentication.setDetails(new WebAuthenticationDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }

    private static Map<String, List<String>> formDraft() {
        Map<String, List<String>> values = new LinkedHashMap<>();
        values.put("customerName", new ArrayList<>(List.of("Özerler")));
        return values;
    }

    private static byte[] serialize(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        return bytes.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (Map<String, Object>) input.readObject();
        }
    }
}
