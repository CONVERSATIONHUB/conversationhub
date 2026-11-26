package com.example.conversationHub.domain.enums;

/**
 * How a service is delivered. The label is used on public pages so that the mode is
 * readable without exposing implementation-specific enum names.
 */
public enum ServiceMode {
    IN_PERSON("In person"),
    REMOTE("Remote"),
    BOTH("In person or remote");

    private final String label;

    ServiceMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
