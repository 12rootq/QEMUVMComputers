package com.sun.org.apache.xml.internal.resolver.tools;

import com.sun.org.apache.xml.internal.resolver.CatalogManager;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class CatalogResolver implements EntityResolver, URIResolver {
    private final CatalogManager catalogManager;

    public CatalogResolver() {
        this(new CatalogManager());
    }

    public CatalogResolver(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    public CatalogManager getCatalogManager() {
        return catalogManager;
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) {
        return null;
    }

    @Override
    public Source resolve(String href, String base) {
        return null;
    }
}
