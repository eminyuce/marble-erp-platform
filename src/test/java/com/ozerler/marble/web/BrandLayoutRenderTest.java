package com.ozerler.marble.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * The product name should appear once in the mega menu brand row (above the search
 * field, below the dark user header), with the version kept visible there. The top
 * bar and footer must not repeat the brand name.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BrandLayoutRenderTest {

    private static final String BRAND = "Özerler Mermer ERP";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("dashboard chrome shows the brand once in the mega menu and keeps the version")
    void dashboardShowsBrandOnceAndKeepsVersion() throws Exception {
        String html = mockMvc.perform(MockMvcRequestBuilders.get("/admin/dashboard"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String topbar = extract(html, "class=\"admin-topbar\"", "</header>");
        String megaHeader = extract(html, "<header class=\"admin-mega-header\">", "</header>");
        String megaBrand = extract(html, "class=\"admin-mega-brand\"", "id=\"adminSidebarSearchContainer\"");
        String footer = extract(html, "<footer class=\"admin-app-footer\">", "</footer>");

        Assertions.assertEquals(0, count(topbar, BRAND), "top bar should not show the brand name:\n" + topbar);
        Assertions.assertFalse(topbar.contains("admin-topbar-version"), topbar);
        Assertions.assertTrue(topbar.contains("admin-topbar-mark"), topbar);
        Assertions.assertEquals(0, count(megaHeader, BRAND),
                "dark mega header should not contain the brand:\n" + megaHeader);
        Assertions.assertEquals(1, count(megaBrand, BRAND),
                "mega menu body should show the brand once above search:\n" + megaBrand);
        Assertions.assertTrue(megaBrand.contains("admin-mega-brand-version"), megaBrand);
        Assertions.assertTrue(megaBrand.contains("1.0.13"), megaBrand);
        Assertions.assertTrue(footer.contains("1.0.13"), footer);
        Assertions.assertEquals(0, count(footer, BRAND), "footer should not repeat the brand:\n" + footer);
        Assertions.assertTrue(footer.contains("Sürüm"), footer);
        Assertions.assertEquals(1, count(stripTitle(html), BRAND),
                "visible page chrome should contain the brand once");
        Assertions.assertTrue(html.contains("name=\"robots\""), html);
        Assertions.assertTrue(html.contains("noindex, nofollow"), html);
        Assertions.assertTrue(html.contains("/favicon.ico?v="), html);
        Assertions.assertTrue(html.contains("/apple-touch-icon.png?v="), html);
        Assertions.assertTrue(html.contains("/site.webmanifest?v="), html);
        Assertions.assertTrue(html.contains("theme-color"), html);
        Assertions.assertTrue(html.contains("#5A0048"), html);
        Assertions.assertTrue(html.contains("/icons/android-chrome-48x48.png"), html);
        Assertions.assertTrue(html.contains("/brand/logo-ozerler.png"), html);
    }

    @Test
    @DisplayName("login page does not repeat the product name in the guide header")
    void loginPageDoesNotDuplicateGuideBrand() throws Exception {
        String html = mockMvc.perform(MockMvcRequestBuilders.get("/account/adminlogin/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assertions.assertFalse(html.contains("admin-login__product"),
                "login guide eyebrow should not repeat the brand");
        Assertions.assertTrue(html.contains("admin-login__brand"), html);
        Assertions.assertTrue(html.contains("Sürüm"), html);
        Assertions.assertTrue(html.contains("1.0.13"), html);
        Assertions.assertTrue(html.contains("/brand/logo-ozerler.png"), html);
        Assertions.assertTrue(html.contains("name=\"robots\""), html);
        Assertions.assertTrue(html.contains("noindex, nofollow"), html);
        Assertions.assertTrue(html.contains("/apple-touch-icon.png?v="), html);
        Assertions.assertTrue(html.contains("/site.webmanifest?v="), html);
        Assertions.assertFalse(html.contains("admin-login__panel-product"),
                "login panel should not repeat the brand name");
    }

    private static String stripTitle(String html) {
        return html.replaceAll("(?is)<title>.*?</title>", "");
    }

    private static String extract(String html, String startToken, String endToken) {
        int start = html.indexOf(startToken);
        Assertions.assertTrue(start >= 0, "missing " + startToken);
        int end = html.indexOf(endToken, start);
        if (end < 0) {
            return html.substring(start);
        }
        return html.substring(start, end);
    }

    private static int count(String text, String needle) {
        int count = 0;
        int from = 0;
        while (true) {
            int index = text.indexOf(needle, from);
            if (index < 0) {
                return count;
            }
            count++;
            from = index + needle.length();
        }
    }
}
