package com.ozerler.marble.config;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.FactoryOperation;
import com.ozerler.marble.model.FactoryWorkOrder;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.Role;
import com.ozerler.marble.model.User;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CostCenterRepository costCenterRepository;
    private final QuarryRepository quarryRepository;
    private final BlockRepository blockRepository;
    private final MachineRepository machineRepository;
    private final FactoryWorkOrderRepository factoryWorkOrderRepository;
    private final FactoryOperationRepository factoryOperationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking local database seed state...");

        if (roleRepository.count() == 0) {
            initRoles();
        }

        if (userRepository.count() == 0) {
            initUsers();
        } else if (userRepository.findByEmail("admin@eimece.test").isEmpty()) {
            seedEimeceAdmin();
        }

        if (costCenterRepository.count() == 0) {
            initCostCenters();
        }

        if (quarryRepository.count() == 0) {
            initQuarriesAndBlocks();
        }

        if (machineRepository.count() == 0) {
            initFactoryOperations();
        }

        log.info("Local database initialization completed successfully.");
    }

    private void initRoles() {
        log.info("Seeding security roles...");
        List<Role> roles = List.of(
                Role.builder().name("ROLE_ADMIN").description("Sistem Yöneticisi - Tam Yetki").build(),
                Role.builder().name("ROLE_USER").description("Standart Kullanıcı").build(),
                Role.builder().name("ROLE_QUARRY_CHIEF").description("Ocak Şefi / Formeni").build(),
                Role.builder().name("ROLE_FACTORY_MANAGER").description("Fabrika Müdürü").build(),
                Role.builder().name("ROLE_OPERATOR").description("Makine Operatörü").build(),
                Role.builder().name("ROLE_WORKSHOP_CHIEF").description("Atölye Şefi").build(),
                Role.builder().name("ROLE_SITE_ENGINEER").description("Şantiye Şefi / Metraj Mühendisi").build(),
                Role.builder().name("ROLE_FINANCE").description("Maliyet & Finans Uzmanı").build(),
                Role.builder().name("ROLE_SALES").description("Satış & İhracat Sorumlusu").build(),
                Role.builder().name("ROLE_QC").description("Kalite Kontrol Uzmanı").build(),
                Role.builder().name("ROLE_EXECUTIVE").description("Genel Müdür / Şirket Ortağı").build()
        );
        roleRepository.saveAll(roles);
    }

    private void initUsers() {
        log.info("Seeding initial admin user...");
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
        Role execRole = roleRepository.findByName("ROLE_EXECUTIVE").orElseThrow();
        Role factoryRole = roleRepository.findByName("ROLE_FACTORY_MANAGER").orElseThrow();
        Role siteRole = roleRepository.findByName("ROLE_SITE_ENGINEER").orElseThrow();

        String encodedPassword = passwordEncoder.encode("changeit");

        User admin = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(encodedPassword)
                .firstName("Sistem")
                .lastName("Yöneticisi")
                .enabled(true)
                .deleted(false)
                .roles(new HashSet<>(Set.of(adminRole, userRole, execRole)))
                .build();

        User factoryMgr = User.builder()
                .username("factory_mgr")
                .email("fabrika@ozerler.com")
                .password(encodedPassword)
                .firstName("Ahmet")
                .lastName("Kaya")
                .enabled(true)
                .deleted(false)
                .roles(new HashSet<>(Set.of(factoryRole, userRole)))
                .build();

        User siteChief = User.builder()
                .username("site_chief")
                .email("santiye@ozerler.com")
                .password(encodedPassword)
                .firstName("Mehmet")
                .lastName("Demir")
                .enabled(true)
                .deleted(false)
                .roles(new HashSet<>(Set.of(siteRole, userRole)))
                .build();

        User eimeceAdmin = User.builder()
                .username("admin@eimece.test")
                .email("admin@eimece.test")
                .password(passwordEncoder.encode("B2u5c8JB"))
                .firstName("Eimece")
                .lastName("Admin")
                .enabled(true)
                .deleted(false)
                .roles(new HashSet<>(Set.of(adminRole, userRole, execRole, factoryRole, siteRole)))
                .build();

        userRepository.saveAll(List.of(admin, factoryMgr, siteChief, eimeceAdmin));
    }

    private void seedEimeceAdmin() {
        log.info("Seeding admin@eimece.test user...");
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
        Role execRole = roleRepository.findByName("ROLE_EXECUTIVE").orElseThrow();
        Role factoryRole = roleRepository.findByName("ROLE_FACTORY_MANAGER").orElseThrow();
        Role siteRole = roleRepository.findByName("ROLE_SITE_ENGINEER").orElseThrow();

        userRepository.save(User.builder()
                .username("admin@eimece.test")
                .email("admin@eimece.test")
                .password(passwordEncoder.encode("B2u5c8JB"))
                .firstName("Eimece")
                .lastName("Admin")
                .enabled(true)
                .deleted(false)
                .roles(new HashSet<>(Set.of(adminRole, userRole, execRole, factoryRole, siteRole)))
                .build());
    }

    private void initCostCenters() {
        log.info("Seeding cost centers...");
        List<CostCenter> costCenters = List.of(
                CostCenter.builder().code("CC-001").name("Ocak & Çıkarma").monthlyBudget(new BigDecimal("500000.00")).description("Blok çıkarma masrafları").businessUnit(BusinessUnit.QUARRY).build(),
                CostCenter.builder().code("CC-002").name("Fabrika Katrak & Dilimleme").monthlyBudget(new BigDecimal("450000.00")).description("Katrak lamaları, elektrik, kesim").businessUnit(BusinessUnit.FACTORY).build(),
                CostCenter.builder().code("CC-003").name("Cila & Yüzey İşleme").monthlyBudget(new BigDecimal("350000.00")).description("Epoksi reçine, cila").businessUnit(BusinessUnit.FACTORY).build(),
                CostCenter.builder().code("CC-004").name("Atölye & Ebatlama İmalatı").monthlyBudget(new BigDecimal("300000.00")).description("Köprü kesme diskleri, CNC").businessUnit(BusinessUnit.WORKSHOP).build(),
                CostCenter.builder().code("CC-005").name("Şantiye & Montaj").monthlyBudget(new BigDecimal("600000.00")).description("Montaj işçilik puantajı").businessUnit(BusinessUnit.SITE).build(),
                CostCenter.builder().code("CC-006").name("Lojistik & Nakliye").monthlyBudget(new BigDecimal("250000.00")).description("Ocak-fabrika-şantiye nakliye").businessUnit(null).build()
        );
        costCenterRepository.saveAll(costCenters);
    }

    private void initQuarriesAndBlocks() {
        log.info("Seeding quarries and sample blocks...");
        Quarry q1 = Quarry.builder()
                .code("Q-MUG-01")
                .name("Yatağan Beyaz Ocağı")
                .location("Muğla - Yatağan")
                .specificGravity(new BigDecimal("2.70"))
                .licenseNo("MUG-2024-884")
                .build();

        Quarry q2 = Quarry.builder()
                .code("Q-AFY-01")
                .name("Afyon Şeker Ocağı")
                .location("Afyonkarahisar - İscehisar")
                .specificGravity(new BigDecimal("2.72"))
                .licenseNo("AFY-2022-102")
                .build();

        Quarry q3 = Quarry.builder()
                .code("Q-BUR-01")
                .name("Burdur Bej Ocağı")
                .location("Burdur - Karamanlı")
                .specificGravity(new BigDecimal("2.68"))
                .licenseNo("BUR-2023-455")
                .build();

        quarryRepository.saveAll(List.of(q1, q2, q3));

        Block b1 = Block.builder()
                .quarry(q1)
                .blockCode("BLK-2026-00125")
                .extractionDate(LocalDate.now().minusDays(20))
                .widthCm(180)
                .lengthCm(290)
                .heightCm(150)
                .volumeM3(new BigDecimal("7.830"))
                .theoreticalWeightKg(new BigDecimal("21141.00"))
                .actualWeightKg(new BigDecimal("20850.00"))
                .weightDeviationPct(new BigDecimal("-1.37"))
                .stoneType("Muğla Beyaz")
                .colorTone("Ekstra Beyaz Kristalize")
                .qualityGrade(QualityGrade.A)
                .crackLevel(0)
                .status(BlockStatus.FACTORY_STOCK)
                .extractionCost(new BigDecimal("42000.00"))
                .transportCost(new BigDecimal("7200.00"))
                .totalCost(new BigDecimal("49200.00"))
                .notes("Homojen kristal yapıda, çatlaksız ayna bloğu.")
                .build();

        Block b2 = Block.builder()
                .quarry(q2)
                .blockCode("BLK-2026-00126")
                .extractionDate(LocalDate.now().minusDays(15))
                .widthCm(170)
                .lengthCm(280)
                .heightCm(140)
                .volumeM3(new BigDecimal("6.664"))
                .theoreticalWeightKg(new BigDecimal("18126.00"))
                .actualWeightKg(new BigDecimal("18400.00"))
                .weightDeviationPct(new BigDecimal("1.51"))
                .stoneType("Afyon Şeker")
                .colorTone("Açık Krem Damarlı")
                .qualityGrade(QualityGrade.A)
                .crackLevel(1)
                .status(BlockStatus.SAWING)
                .extractionCost(new BigDecimal("38000.00"))
                .transportCost(new BigDecimal("6500.00"))
                .totalCost(new BigDecimal("44500.00"))
                .notes("Kılcal yüzey çatlağı epoksi hattında telafi edilebilir.")
                .build();

        Block b3 = Block.builder()
                .quarry(q3)
                .blockCode("BLK-2026-00127")
                .extractionDate(LocalDate.now().minusDays(5))
                .widthCm(190)
                .lengthCm(310)
                .heightCm(160)
                .volumeM3(new BigDecimal("9.424"))
                .theoreticalWeightKg(new BigDecimal("25256.00"))
                .actualWeightKg(new BigDecimal("25100.00"))
                .weightDeviationPct(new BigDecimal("-0.62"))
                .stoneType("Burdur Bej")
                .colorTone("Homojen Açık Bej")
                .qualityGrade(QualityGrade.EXTRA)
                .crackLevel(0)
                .status(BlockStatus.QUARRY)
                .extractionCost(new BigDecimal("52000.00"))
                .transportCost(BigDecimal.ZERO)
                .totalCost(new BigDecimal("52000.00"))
                .notes("Ocak sahasında sevk bekliyor.")
                .build();

        blockRepository.saveAll(List.of(b1, b2, b3));
    }

    private void initFactoryOperations() {
        log.info("Seeding factory machines, work orders and operations...");
        Machine m1 = Machine.builder()
                .code("M-KAT-01")
                .name("Katrak-01 (80 Lama)")
                .businessUnit(BusinessUnit.FACTORY)
                .machineType(MachineType.GANGSAW)
                .active(true)
                .notes("Ana katrak kesim tezgahı")
                .build();

        Machine m2 = Machine.builder()
                .code("M-KAT-02")
                .name("Katrak-02 (Simel)")
                .businessUnit(BusinessUnit.FACTORY)
                .machineType(MachineType.GANGSAW)
                .active(true)
                .notes("İkinci katrak kesim tezgahı")
                .build();

        Machine m3 = Machine.builder()
                .code("M-ST-01")
                .name("ST Kesim-01")
                .businessUnit(BusinessUnit.FACTORY)
                .machineType(MachineType.ST)
                .active(true)
                .notes("Blok dilme ve şerit kesme")
                .build();

        Machine m4 = Machine.builder()
                .code("M-POL-01")
                .name("Plaka Silim Hattı (Barsanti)")
                .businessUnit(BusinessUnit.FACTORY)
                .machineType(MachineType.SLAB_POLISHING)
                .active(true)
                .notes("16 kafalı plaka silim ve cila")
                .build();

        machineRepository.saveAll(List.of(m1, m2, m3, m4));

        List<Block> blocks = blockRepository.findAll();
        if (blocks.isEmpty()) {
            return;
        }

        Block b1 = blocks.get(0);
        b1.setStatus(BlockStatus.SAWING);
        blockRepository.save(b1);

        FactoryWorkOrder fwo1 = FactoryWorkOrder.builder()
                .orderNo("FWO-2026-001")
                .block(b1)
                .acceptedAt(LocalDate.now().minusDays(2))
                .assignedMachine(m1)
                .status(FactoryWorkOrderStatus.IN_PROGRESS)
                .responsibleName("Ahmet Kaya")
                .notes("2 cm plaka kesim iş emri")
                .build();

        FactoryWorkOrder fwo2 = null;
        if (blocks.size() > 1) {
            Block b2 = blocks.get(1);
            fwo2 = FactoryWorkOrder.builder()
                    .orderNo("FWO-2026-002")
                    .block(b2)
                    .acceptedAt(LocalDate.now().minusDays(1))
                    .assignedMachine(m2)
                    .status(FactoryWorkOrderStatus.ASSIGNED)
                    .responsibleName("Ahmet Kaya")
                    .notes("2 cm plaka kesim iş emri")
                    .build();
        }

        factoryWorkOrderRepository.save(fwo1);
        if (fwo2 != null) {
            factoryWorkOrderRepository.save(fwo2);
        }

        FactoryOperation op1 = FactoryOperation.builder()
                .workOrder(fwo1)
                .processType(FactoryProcessType.GANGSAW_CUTTING)
                .machine(m1)
                .operatorName("Ahmet Usta")
                .inputQuantity(b1.getActualTonnage() != null ? b1.getActualTonnage() : new BigDecimal("20.85"))
                .inputUnit(QuantityUnit.TON)
                .outputQuantity(BigDecimal.ZERO)
                .outputUnit(QuantityUnit.SQUARE_METER)
                .wasteQuantity(BigDecimal.ZERO)
                .wasteUnit(QuantityUnit.TON)
                .status(OperationStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now().minusHours(3))
                .build();

        FactoryOperation op2 = null;
        if (fwo2 != null) {
            Block b2 = blocks.get(1);
            op2 = FactoryOperation.builder()
                    .workOrder(fwo2)
                    .processType(FactoryProcessType.GANGSAW_CUTTING)
                    .machine(m2)
                    .operatorName("Mehmet Usta")
                    .inputQuantity(b2.getActualTonnage() != null ? b2.getActualTonnage() : new BigDecimal("18.40"))
                    .inputUnit(QuantityUnit.TON)
                    .outputQuantity(BigDecimal.ZERO)
                    .outputUnit(QuantityUnit.SQUARE_METER)
                    .wasteQuantity(BigDecimal.ZERO)
                    .wasteUnit(QuantityUnit.TON)
                    .status(OperationStatus.PLANNED)
                    .build();
        }

        factoryOperationRepository.save(op1);
        if (op2 != null) {
            factoryOperationRepository.save(op2);
        }
    }
}
