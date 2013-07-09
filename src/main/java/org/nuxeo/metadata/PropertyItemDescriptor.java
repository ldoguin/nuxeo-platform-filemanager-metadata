package org.nuxeo.metadata;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("propertyItem")
public class PropertyItemDescriptor {

    @XNode("@xpath")
    protected String xpath;

    @XNode("@metadataName")
    protected String metadataName;

    @XNode("@policy")
    protected String policy;

    @XNodeList(value = "fallback/metadata", componentType = String.class, type = String[].class)
    protected String[] fallbackMetadata;

    public String getXpath() {
        return xpath;
    }

    public String getMetadataName() {
        return metadataName;
    }

    public String getPolicy() {
        return policy;
    }

    public String[] getFallbackMetadatas() {
        return fallbackMetadata;
    }

}
