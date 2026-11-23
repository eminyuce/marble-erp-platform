package com.ozerler.marble.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.model.Customer;
import com.ozerler.marble.repository.CustomerRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaAuditingTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("JPA Auditing populates addUserId, updateUserId, createdDate and updatedDate from SecurityContext")
    @WithMockUser(username = "auditor@ozerler.com")
    void auditingPopulatesAuditFieldsFromSecurityContext() {
        Customer customer = Customer.builder()
                .customerCode("CUST-AUDIT-1")
                .companyName("Audit Test Corp")
                .contactPerson("Ahmet Yılmaz")
                .phone("05551234567")
                .email("ahmet@audittest.com")
                .build();

        Customer saved = customerRepository.saveAndFlush(customer);

        Assertions.assertThat(saved.getId()).isNotNull();
        Assertions.assertThat(saved.getCreatedDate()).isNotNull();
        Assertions.assertThat(saved.getUpdatedDate()).isNotNull();
        Assertions.assertThat(saved.getAddUserId()).isEqualTo("auditor@ozerler.com");
        Assertions.assertThat(saved.getUpdateUserId()).isEqualTo("auditor@ozerler.com");

        // Backward-compatible alias getters
        Assertions.assertThat(saved.getCreatedAt()).isEqualTo(saved.getCreatedDate());
        Assertions.assertThat(saved.getUpdatedAt()).isEqualTo(saved.getUpdatedDate());
    }

    @Test
    @DisplayName("JPA Auditing falls back to system@ozerler.com when no authenticated user is present")
    void auditingFallbackToSystemWhenUnauthenticated() {
        Customer customer = Customer.builder()
                .customerCode("CUST-AUDIT-2")
                .companyName("System Audit Corp")
                .contactPerson("Mehmet Demir")
                .phone("05559876543")
                .email("mehmet@systemtest.com")
                .build();

        Customer saved = customerRepository.saveAndFlush(customer);

        Assertions.assertThat(saved.getId()).isNotNull();
        Assertions.assertThat(saved.getCreatedDate()).isNotNull();
        Assertions.assertThat(saved.getUpdatedDate()).isNotNull();
        Assertions.assertThat(saved.getAddUserId()).isEqualTo("system@ozerler.com");
        Assertions.assertThat(saved.getUpdateUserId()).isEqualTo("system@ozerler.com");
    }
}
