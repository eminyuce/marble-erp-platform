package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.MachineType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "machines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class Machine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_unit", nullable = false, length = 30)
    private BusinessUnit businessUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "machine_type", nullable = false, length = 40)
    private MachineType machineType;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
