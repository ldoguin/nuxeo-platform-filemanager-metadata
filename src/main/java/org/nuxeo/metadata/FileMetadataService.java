package org.nuxeo.metadata;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface FileMetadataService {
    
    List<MetadataMappingDescriptor> getMappings(DocumentModel doc) throws ClientException;

    void mapMetadata(DocumentModel doc, CoreSession coreSession)
            throws ClientException, IOException;
}
