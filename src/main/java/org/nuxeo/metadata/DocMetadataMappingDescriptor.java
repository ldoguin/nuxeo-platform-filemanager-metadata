package org.nuxeo.metadata;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("doc")
public class DocMetadataMappingDescriptor {

    @XNode("@docType")
    protected String docType;


    @XNodeList(value = "mappingId", type = String[].class, componentType = String.class)
    protected String[] mappingId;

    @XNodeList(value = "mapping", type = MetadataMappingDescriptor[].class, componentType = MetadataMappingDescriptor.class)
    protected MetadataMappingDescriptor[] innerMapping;

    public String getDocType() {
        return docType;
    }

    public String[] getMappingId() {
        return mappingId;
    }

    public MetadataMappingDescriptor[] getInnerMapping() {
        return innerMapping;
    }

}