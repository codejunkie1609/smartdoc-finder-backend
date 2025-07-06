package com.smartdocfinder.core.constants;

import java.util.Set;

public class Constants {
        public static final Set<String> ALLOWED_TYPES = Set.of(
                        "text/plain",
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.oasis.opendocument.text",
                        "application/rtf",
                        "text/html",
                        "application/xml",
                        "application/json",
                        "text/markdown",
                        "text/csv",
                        "application/x-yaml",
                        "text/yaml",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
}
