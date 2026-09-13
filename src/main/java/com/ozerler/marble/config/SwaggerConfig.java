package com.ozerler.marble.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
public class SwaggerConfig {

    @Bean
    public OpenAPI marbleErpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ozerler Mermer ERP API")
                        .version("1.0")
                        .description("""
                                REST and MVC endpoints for Ozerler Mermer ERP — physical \
                                traceability, waste management, and dynamic cost accounting.
                                
                                Covered areas:
                                • Authentication and account management (admin login, password change, roles)
                                • Quarry blocks (inventory, factory transfer, sales)
                                • Production orders, slab stock, barcode labels, and scrap
                                • Workshop cut orders
                                • Sales orders and procurement / purchase orders
                                • Projects, site locations, and material consumption
                                • Multi-layer cost accounting and pricing simulation
                                • Genealogy / stone traceability trees
                                • Operational reports (CSV and Excel export)
                                • Global search across blocks, slabs, orders, customers, and suppliers
                                • Digital stone passport, file uploads, and system health
                                """)
                        .contact(new Contact()
                                .name("Ozerler Mermer A.S.")));
    }
}
