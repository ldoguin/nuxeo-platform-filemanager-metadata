package org.nuxeo.metadata;

import java.io.IOException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface MetadataMapper {

    void mapProperties(DocumentModel doc, Blob blob,
            MetadataMappingDescriptor mappingDescriptor, CoreSession coreSession)
            throws ClientException, IOException;

}