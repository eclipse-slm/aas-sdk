package org.eclipse.slm.aas.clients.submodelrepository;

import org.eclipse.digitaltwin.aas4j.v3.model.Blob;
import org.eclipse.digitaltwin.aas4j.v3.model.DataTypeDefXsd;
import org.eclipse.digitaltwin.aas4j.v3.model.File;
import org.eclipse.digitaltwin.aas4j.v3.model.LangStringTextType;
import org.eclipse.digitaltwin.aas4j.v3.model.MultiLanguageProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class SubmodelSanitizer {

    private static final Pattern ID_SHORT_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*[a-zA-Z0-9_]+$");
    private static final int MAX_CATEGORY_LENGTH = 128;
    private static final int MAX_LANG_STRING_TEXT_LENGTH = 1023;
    private static final int MAX_CONTENT_TYPE_LENGTH = 100;
    private static final int MAX_FILE_OR_BLOB_VALUE_LENGTH = 2000;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private SubmodelSanitizer() {
        // Utility class
    }

    public static String sanitizeIdShort(String idShort, Logger log) {
        if (idShort == null || idShort.isBlank()) {
            return "A_";
        }

        var original = idShort;
        var sanitized = idShort.replaceAll("[^a-zA-Z0-9_-]", "_");

        if (!Character.isLetter(sanitized.charAt(0))) {
            sanitized = "A" + sanitized;
        }

        if (!Character.isLetterOrDigit(sanitized.charAt(sanitized.length() - 1)) && sanitized.charAt(sanitized.length() - 1) != '_') {
            sanitized = sanitized.substring(0, sanitized.length() - 1) + "_";
        }

        if (sanitized.length() == 1) {
            sanitized = sanitized + "_";
        }

        if (!ID_SHORT_PATTERN.matcher(sanitized).matches()) {
            sanitized = "A_";
        }

        if (!original.equals(sanitized)) {
            log.warn("idShort '{}' has been '{}' normalized, to match expected format.", idShort, sanitized);
        }

        return sanitized;
    }

    public static void sanitizeSubmodel(Submodel submodel, Logger log) {
        if (submodel == null || submodel.getSubmodelElements() == null) {
            return;
        }

        var sanitizedElements = sanitizeSubmodelElements(submodel.getSubmodelElements(), submodel.getIdShort(), log);
        submodel.setSubmodelElements(sanitizedElements);
    }

    private static List<SubmodelElement> sanitizeSubmodelElements(List<SubmodelElement> submodelElements, String pathPrefix, Logger log) {
        if (submodelElements == null) {
            return List.of();
        }

        return submodelElements.stream()
                .filter(submodelElement -> sanitizeSubmodelElement(submodelElement, pathPrefix, log))
                .toList();
    }

    private static boolean sanitizeSubmodelElement(SubmodelElement submodelElement, String pathPrefix, Logger log) {
        var elementPath = buildElementPath(pathPrefix, submodelElement);

        if (!sanitizeCategory(submodelElement, elementPath, log)) {
            return false;
        }

        if (submodelElement instanceof Property property) {
            return sanitizeProperty(property, elementPath, log);
        }

        if (submodelElement instanceof MultiLanguageProperty multiLanguageProperty) {
            return sanitizeMultiLanguageProperty(multiLanguageProperty, elementPath, log);
        }

        if (submodelElement instanceof File file) {
            return sanitizeFile(file, elementPath, log);
        }

        if (submodelElement instanceof Blob blob) {
            return sanitizeBlob(blob, elementPath, log);
        }

        if (submodelElement instanceof SubmodelElementCollection collection) {
            var nestedElements = sanitizeSubmodelElements(collection.getValue(), elementPath, log);
            collection.setValue(nestedElements);
        }

        return true;
    }

    private static boolean sanitizeCategory(SubmodelElement submodelElement, String elementPath, Logger log) {
        var category = submodelElement.getCategory();
        if (category == null) {
            return true;
        }

        if (category.isBlank()) {
            log.warn("SubmodelElement '{}' removed: category is empty.", elementPath);
            return false;
        }

        if (category.length() > MAX_CATEGORY_LENGTH) {
            submodelElement.setCategory(category.substring(0, MAX_CATEGORY_LENGTH));
            log.warn("SubmodelElement '{}' category truncated to {} characters.", elementPath, MAX_CATEGORY_LENGTH);
        }

        return true;
    }

    private static boolean sanitizeProperty(Property property, String elementPath, Logger log) {
        var value = property.getValue();
        if (value == null || value.isBlank()) {
            log.warn("SubmodelElement '{}' removed: Property value is empty.", elementPath);
            return false;
        }

        var valueType = property.getValueType();
        if (isNumericValueType(valueType) && !isValidNumericValue(value, valueType)) {
            log.warn("SubmodelElement '{}' removed: Property value '{}' cannot be parsed as {}.", elementPath, value, valueType);
            return false;
        }

        return true;
    }

    private static boolean sanitizeMultiLanguageProperty(MultiLanguageProperty multiLanguageProperty, String elementPath, Logger log) {
        var langStrings = multiLanguageProperty.getValue();
        if (langStrings == null || langStrings.isEmpty()) {
            log.warn("SubmodelElement '{}' removed: LangString value list is empty.", elementPath);
            return false;
        }

        for (LangStringTextType langString : langStrings) {
            if (langString == null) {
                log.warn("SubmodelElement '{}' removed: LangString entry is null.", elementPath);
                return false;
            }

            langString.setLanguage(normalizeLanguage(langString.getLanguage()));

            var text = langString.getText();
            if (text == null || text.isBlank()) {
                log.warn("SubmodelElement '{}' removed: LangString text is empty.", elementPath);
                return false;
            }

            if (text.length() > MAX_LANG_STRING_TEXT_LENGTH) {
                langString.setText(text.substring(0, MAX_LANG_STRING_TEXT_LENGTH));
                log.warn("SubmodelElement '{}' LangString text truncated to {} characters.", elementPath, MAX_LANG_STRING_TEXT_LENGTH);
            }
        }

        return true;
    }

    private static boolean sanitizeFile(File file, String elementPath, Logger log) {
        file.setContentType(normalizeContentType(file.getContentType(), elementPath, log));

        var value = file.getValue();
        if (value == null || value.isBlank()) {
            log.warn("SubmodelElement '{}' removed: File value/path is empty.", elementPath);
            return false;
        }

        if (value.length() > MAX_FILE_OR_BLOB_VALUE_LENGTH) {
            file.setValue(value.substring(0, MAX_FILE_OR_BLOB_VALUE_LENGTH));
            log.warn("SubmodelElement '{}' File value/path truncated to {} characters.", elementPath, MAX_FILE_OR_BLOB_VALUE_LENGTH);
        }

        return true;
    }

    private static boolean sanitizeBlob(Blob blob, String elementPath, Logger log) {
        blob.setContentType(normalizeContentType(blob.getContentType(), elementPath, log));

        var value = blob.getValue();
        if (value == null || value.length == 0) {
            log.warn("SubmodelElement '{}' removed: Blob value is empty.", elementPath);
            return false;
        }

        if (value.length > MAX_FILE_OR_BLOB_VALUE_LENGTH) {
            blob.setValue(Arrays.copyOf(value, MAX_FILE_OR_BLOB_VALUE_LENGTH));
            log.warn("SubmodelElement '{}' Blob value truncated to {} bytes.", elementPath, MAX_FILE_OR_BLOB_VALUE_LENGTH);
        }

        return true;
    }

    private static String normalizeLanguage(String language) {
        if (language == null) {
            return "en";
        }

        var lettersOnly = language.replaceAll("[^a-zA-Z]", "").toLowerCase();
        if (lettersOnly.length() < 2) {
            return "en";
        }

        return lettersOnly.substring(0, 2);
    }

    private static String normalizeContentType(String contentType, String elementPath, Logger log) {
        if (contentType == null || contentType.isBlank()) {
            log.warn("SubmodelElement '{}' contentType empty, default '{}' applied.", elementPath, DEFAULT_CONTENT_TYPE);
            return DEFAULT_CONTENT_TYPE;
        }

        if (contentType.length() > MAX_CONTENT_TYPE_LENGTH) {
            log.warn("SubmodelElement '{}' contentType truncated to {} characters.", elementPath, MAX_CONTENT_TYPE_LENGTH);
            return contentType.substring(0, MAX_CONTENT_TYPE_LENGTH);
        }

        return contentType;
    }

    private static boolean isNumericValueType(DataTypeDefXsd valueType) {
        if (valueType == null) {
            return false;
        }

        var typeName = valueType.name();
        return typeName.contains("INT")
                || typeName.contains("INTEGER")
                || typeName.contains("DECIMAL")
                || typeName.contains("DOUBLE")
                || typeName.contains("FLOAT")
                || typeName.contains("LONG")
                || typeName.contains("SHORT")
                || typeName.contains("BYTE");
    }

    private static boolean isValidNumericValue(String value, DataTypeDefXsd valueType) {
        var typeName = valueType.name();

        try {
            if (typeName.contains("FLOAT") || typeName.contains("DOUBLE") || typeName.contains("DECIMAL")) {
                Double.parseDouble(value);
                return true;
            }

            if (typeName.contains("UNSIGNED")) {
                var parsed = Long.parseLong(value);
                return parsed >= 0;
            }

            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String buildElementPath(String pathPrefix, SubmodelElement submodelElement) {
        var idShort = submodelElement.getIdShort() == null || submodelElement.getIdShort().isBlank()
                ? "<missing-idShort>"
                : submodelElement.getIdShort();

        if (pathPrefix == null || pathPrefix.isBlank()) {
            return idShort;
        }

        return pathPrefix + "." + idShort;
    }
}

