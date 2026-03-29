package com.sun.org.apache.xml.internal.resolver;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Catalog {
    private final List<URL> parsedCatalogs = new ArrayList<>();

    public void parseCatalog(URL catalogUrl) throws IOException {
        if (catalogUrl != null) {
            parsedCatalogs.add(catalogUrl);
        }
    }

    public List<URL> getParsedCatalogs() {
        return Collections.unmodifiableList(parsedCatalogs);
    }
}
