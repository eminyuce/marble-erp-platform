package com.ozerler.marble.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(packages = "com.ozerler.marble", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllersDoNotDependOnRepositories = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule servicesDoNotDependOnControllers = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

    /**
     * The entity package is {@code com.ozerler.marble.model}. Enums and API envelopes live in subpackages
     * and stay available to request parameters. JPA entities themselves must not reach a controller.
     */
    @ArchTest
    static final ArchRule controllersDoNotDependOnEntities = ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("com.ozerler.marble.model");
}
