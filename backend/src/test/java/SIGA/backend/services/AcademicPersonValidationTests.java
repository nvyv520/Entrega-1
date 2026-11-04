package SIGA.backend.services;

import static SIGA.backend.services.ServiceFixtures.rejects;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AcademicPersonValidationTests {
  @Test
  void normalizesRutAndPreservesLegacyIdentity() {
    assertEquals("12.345.678-K", AcademicPersonValidation.normalizeRut("12345678k", null));
    assertEquals("12345678K", AcademicPersonValidation.compactRut("12.345.678-k"));
    assertEquals("legacy", AcademicPersonValidation.normalizeRut("legacy", "legacy"));
    rejects(400, () -> AcademicPersonValidation.normalizeRut("123456789", "SEED-2026-1"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" 12345678-9", "0-0", "12.34.567-8", "1234567890-1", "12A-4"})
  void rejectsInvalidRut(String value) {
    rejects(400, () -> AcademicPersonValidation.normalizeRut(value, null));
  }

  @Test
  void namesAndEmailsPreserveCapitalizationAndUnchangedLegacyValues() {
    assertEquals("María José", AcademicPersonValidation.normalizeName("María José", null));
    assertEquals("O’Connor", AcademicPersonValidation.normalizeName("O’Connor", null));
    assertEquals("legacy123", AcademicPersonValidation.normalizeName("legacy123", "legacy123"));
    assertEquals(
        "Ana@Institucion.cl", AcademicPersonValidation.normalizeEmail("Ana@Institucion.cl", null));
    assertEquals("old", AcademicPersonValidation.normalizeEmail("old", "old"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" Ana", "Ana ", "Ana123", "Ana  María", "-Ana"})
  void rejectsInvalidNames(String value) {
    rejects(400, () -> AcademicPersonValidation.normalizeName(value, null));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "no-at",
        "a@@b.cl",
        ".a@b.cl",
        "a.@b.cl",
        "a..b@b.cl",
        "a@localhost",
        "a@-b.cl",
        "a@b.c",
        "a@b..cl",
        "a b@b.cl"
      })
  void rejectsInvalidEmail(String value) {
    rejects(400, () -> AcademicPersonValidation.normalizeEmail(value, null));
  }
}
