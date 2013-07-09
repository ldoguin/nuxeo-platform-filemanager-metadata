package org.nuxeo.metadata;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("mapping")
public class MetadataMappingDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("@nxpath")
    protected String nxpath;

    @XNode("@mapper")
    protected String mapperId;

    @XNodeList(value = "mimeTypes/mimeType", componentType = String.class, type = String[].class)
    protected String[] mimeTypes;

    @XNodeList(value = "requirements/schema", componentType = String.class, type = String[].class)
    protected String[] requiredSchema;

    @XNodeList(value = "requirements/facet", componentType = String.class, type = String[].class)
    protected String[] requiredFacets;

    @XNodeList(value = "properties/propertyItem", componentType = PropertyItemDescriptor.class, type = PropertyItemDescriptor[].class )
    protected PropertyItemDescriptor[] properties;

    public String getId() {
        return id;
    }

    public String getNxpath() {
        return nxpath;
    }

    public String getMapperId() {
        return mapperId;
    }

    public String[] getMimeTypes() {
        return mimeTypes;
    }

    public String[] getRequiredSchema() {
        return requiredSchema;
    }

    public String[] getRequiredFacets() {
        return requiredFacets;
    }

    public PropertyItemDescriptor[] getProperties() {
        return properties;
    }

}
