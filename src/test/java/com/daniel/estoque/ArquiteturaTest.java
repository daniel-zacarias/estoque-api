package com.daniel.estoque;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garante a regra central da arquitetura hexagonal (ADR-002):
 * as dependências apontam para o domínio, nunca o contrário.
 */
@AnalyzeClasses(packages = "com.daniel.estoque", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    @ArchTest
    static final ArchRule dominioNaoConheceFrameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.hibernate..", "tools.jackson..", "com.fasterxml..")
            .because("o domínio é Java puro; frameworks ficam nos adaptadores");

    @ArchTest
    static final ArchRule dominioNaoConheceAplicacaoNemAdaptadores = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..", "..config..");

    @ArchTest
    static final ArchRule aplicacaoNaoConheceAdaptadores = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..");

    @ArchTest
    static final ArchRule adaptadoresDeEntradaNaoUsamPersistencia = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.out..");
}
