package org.eclipse.slm.aas.clients.submodelrepository;

import org.eclipse.digitaltwin.aas4j.v3.model.DataTypeDefXsd;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultBlob;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultFile;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultLangStringTextType;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultMultiLanguageProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubmodelSanitizerTests {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelSanitizerTests.class);

    @Test
    void sanitizeIdShort_shouldNormalizeInvalidValue() {
        var sanitized = SubmodelSanitizer.sanitizeIdShort("1 bad/id-", LOG);

        assertThat(sanitized).isEqualTo("A1_bad_id_");
        assertThat(SubmodelSanitizer.sanitizeIdShort(null, LOG)).isEqualTo("A_");
    }

    @Test
    void sanitizeSubmodel_shouldHandleCategoryRules() {
        var emptyCategoryElement = new DefaultProperty.Builder()
                .idShort("removeByCategory")
                .valueType(DataTypeDefXsd.STRING)
                .value("ok")
                .category("   ")
                .build();

        var longCategory = "x".repeat(140);
        var longCategoryElement = new DefaultProperty.Builder()
                .idShort("truncateCategory")
                .valueType(DataTypeDefXsd.STRING)
                .value("ok")
                .category(longCategory)
                .build();

        var submodel = submodelWithElements(emptyCategoryElement, longCategoryElement);

        SubmodelSanitizer.sanitizeSubmodel(submodel, LOG);

        assertThat(submodel.getSubmodelElements()).hasSize(1);
        assertThat(submodel.getSubmodelElements().getFirst().getIdShort()).isEqualTo("truncateCategory");
        assertThat(submodel.getSubmodelElements().getFirst().getCategory()).hasSize(128);
    }

    @Test
    void sanitizeSubmodel_shouldValidateNumericPropertyValues() {
        var invalidNumeric = new DefaultProperty.Builder()
                .idShort("invalidNumeric")
                .valueType(DataTypeDefXsd.DOUBLE)
                .value("not-a-number")
                .build();

        var emptyValue = new DefaultProperty.Builder()
                .idShort("emptyValue")
                .valueType(DataTypeDefXsd.DOUBLE)
                .value("   ")
                .build();

        var validNumeric = new DefaultProperty.Builder()
                .idShort("validNumeric")
                .valueType(DataTypeDefXsd.DOUBLE)
                .value("42.5")
                .build();

        var submodel = submodelWithElements(invalidNumeric, emptyValue, validNumeric);

        SubmodelSanitizer.sanitizeSubmodel(submodel, LOG);

        assertThat(submodel.getSubmodelElements()).hasSize(1);
        assertThat(submodel.getSubmodelElements().getFirst().getIdShort()).isEqualTo("validNumeric");
    }

    @Test
    void sanitizeSubmodel_shouldNormalizeMultiLanguageProperty() {
        var validButNeedsNormalization = new DefaultMultiLanguageProperty.Builder()
                .idShort("mlpValid")
                .value(new DefaultLangStringTextType.Builder()
                        .language("DE-de_123")
                        .text("a".repeat(1050))
                        .build())
                .build();

        var invalidText = new DefaultMultiLanguageProperty.Builder()
                .idShort("mlpInvalid")
                .value(new DefaultLangStringTextType.Builder()
                        .language("fr")
                        .text("  ")
                        .build())
                .build();

        var submodel = submodelWithElements(validButNeedsNormalization, invalidText);

        SubmodelSanitizer.sanitizeSubmodel(submodel, LOG);

        assertThat(submodel.getSubmodelElements()).hasSize(1);

        var kept = (org.eclipse.digitaltwin.aas4j.v3.model.MultiLanguageProperty) submodel.getSubmodelElements().getFirst();
        assertThat(kept.getValue()).hasSize(1);
        assertThat(kept.getValue().getFirst().getLanguage()).isEqualTo("de");
        assertThat(kept.getValue().getFirst().getText()).hasSize(1023);
    }

    @Test
    void sanitizeSubmodel_shouldApplyFileAndBlobRules() {
        var keepFile = new DefaultFile.Builder()
                .idShort("fileKeep")
                .contentType("  ")
                .value("v".repeat(2100))
                .build();

        var removeFile = new DefaultFile.Builder()
                .idShort("fileRemove")
                .contentType("text/plain")
                .value(" ")
                .build();

        var keepBlob = new DefaultBlob.Builder()
                .idShort("blobKeep")
                .contentType("")
                .value(new byte[2100])
                .build();

        var removeBlob = new DefaultBlob.Builder()
                .idShort("blobRemove")
                .contentType("application/octet-stream")
                .value(new byte[0])
                .build();

        var submodel = submodelWithElements(keepFile, removeFile, keepBlob, removeBlob);

        SubmodelSanitizer.sanitizeSubmodel(submodel, LOG);

        assertThat(submodel.getSubmodelElements()).hasSize(2);

        var sanitizedFile = (org.eclipse.digitaltwin.aas4j.v3.model.File) submodel.getSubmodelElements().stream()
                .filter(element -> "fileKeep".equals(element.getIdShort()))
                .findFirst()
                .orElseThrow();
        assertThat(sanitizedFile.getContentType()).isEqualTo("application/octet-stream");
        assertThat(sanitizedFile.getValue()).hasSize(2000);

        var sanitizedBlob = (org.eclipse.digitaltwin.aas4j.v3.model.Blob) submodel.getSubmodelElements().stream()
                .filter(element -> "blobKeep".equals(element.getIdShort()))
                .findFirst()
                .orElseThrow();
        assertThat(sanitizedBlob.getContentType()).isEqualTo("application/octet-stream");
        assertThat(sanitizedBlob.getValue()).hasSize(2000);
    }

    @Test
    void sanitizeSubmodel_shouldSanitizeNestedCollectionsRecursively() {
        var nestedInvalid = new DefaultProperty.Builder()
                .idShort("nestedInvalid")
                .valueType(DataTypeDefXsd.STRING)
                .value(" ")
                .build();

        var nestedValid = new DefaultProperty.Builder()
                .idShort("nestedValid")
                .valueType(DataTypeDefXsd.STRING)
                .value("ok")
                .category("c".repeat(160))
                .build();

        var collection = new DefaultSubmodelElementCollection.Builder()
                .idShort("collection")
                .value(List.of(nestedInvalid, nestedValid))
                .build();

        var submodel = submodelWithElements(collection);

        SubmodelSanitizer.sanitizeSubmodel(submodel, LOG);

        var sanitizedCollection = (org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection) submodel.getSubmodelElements().getFirst();
        assertThat(sanitizedCollection.getValue()).hasSize(1);
        assertThat(sanitizedCollection.getValue().getFirst().getIdShort()).isEqualTo("nestedValid");
        assertThat(sanitizedCollection.getValue().getFirst().getCategory()).hasSize(128);
    }

    private DefaultSubmodel submodelWithElements(SubmodelElement... elements) {
        return new DefaultSubmodel.Builder()
                .id("urn:uuid:test-submodel")
                .idShort("testSubmodel")
                .submodelElements(List.of(elements))
                .build();
    }
}

