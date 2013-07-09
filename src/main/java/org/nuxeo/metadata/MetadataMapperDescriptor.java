package org.nuxeo.metadata;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("mapper")
public class MetadataMapperDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("@class")
    private Class<MetadataMapper> adapterClass;

    public String getId() {
        return id;
    }

    public MetadataMapper getMapper() throws InstantiationException,
            IllegalAccessException {
        return adapterClass.newInstance();
    }

}
