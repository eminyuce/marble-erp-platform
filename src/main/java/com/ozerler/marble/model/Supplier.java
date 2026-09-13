package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.SupplierType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class Supplier extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_code", nullable = false, unique = true, length = 50)
    private String supplierCode;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    @Column(name = "tax_number", length = 20)
    private String taxNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_type", nullable = false, length = 50)
    @Builder.Default
    private SupplierType supplierType = SupplierType.CONSUMABLE;

    @Column(columnDefinition = "TEXT")
    private String notes;

}
