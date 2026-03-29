package com.sun.org.apache.xml.internal.resolver;

public class CatalogManager {
    private boolean ignoreMissingProperties;
    private boolean useStaticCatalog = true;
    private final Catalog catalog = new Catalog();

    public void setIgnoreMissingProperties(boolean ignoreMissingProperties) {
        this.ignoreMissingProperties = ignoreMissingProperties;
    }

    public boolean getIgnoreMissingProperties() {
        return ignoreMissingProperties;
    }

    public void setUseStaticCatalog(boolean useStaticCatalog) {
        this.useStaticCatalog = useStaticCatalog;
    }

    public boolean getUseStaticCatalog() {
        return useStaticCatalog;
    }

    public Catalog getCatalog() {
        return catalog;
    }
}
