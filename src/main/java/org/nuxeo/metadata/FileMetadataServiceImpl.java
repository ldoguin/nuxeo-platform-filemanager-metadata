/**
 * 
 */

package org.nuxeo.metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author ldoguin
 * @since 5.7
 */
public class FileMetadataServiceImpl extends DefaultComponent implements
        FileMetadataService {

    /**
     * List of Mappers
     */
    protected Map<String, MetadataMapper> mappers;

    /**
     * Every mapping
     */
    protected Map<String, MetadataMappingDescriptor> mappings;

    /**
     * Every mappings by mimeType
     */
    protected Map<String, MetadataMappingDescriptor> mimeTypeMappings;

    /**
     * Every mappings withouth specific nxpath, use blob holder instead
     */    
    protected Map<String, MetadataMappingDescriptor> bhMapping;

    /**
     * Three level mapping registry.
     * First key is document type
     * Second key is nxPath to the blob
     * Third key is mime type
     */
    protected Map<String, Map<String, Map<String, MetadataMappingDescriptor>>> nxPathMapping;

    @Override
    public void activate(ComponentContext context) throws Exception {
        mappings = new HashMap<String, MetadataMappingDescriptor>();
        mimeTypeMappings = new HashMap<String, MetadataMappingDescriptor>();
        mappers = new HashMap<String, MetadataMapper>();
        bhMapping = new HashMap<String, MetadataMappingDescriptor>();
        nxPathMapping = new HashMap<String, Map<String, Map<String, MetadataMappingDescriptor>>>();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("mapper")) {
            if (contribution instanceof MetadataMapperDescriptor) {
                registerMapper((MetadataMapperDescriptor) contribution);
            }
        } else if (extensionPoint.equals("mapping")) {
            if (contribution instanceof MetadataMappingDescriptor) {
                registerMapping((MetadataMappingDescriptor) contribution);
            }
        } else if (extensionPoint.equals("docMapping")) {
            if (contribution instanceof DocMetadataMappingDescriptor) {
                registerDocMapping((DocMetadataMappingDescriptor) contribution);
            }
        }
    }

    private void registerMapping(MetadataMappingDescriptor contribution) {
        mappings.put(contribution.getId(), contribution);
        String[] mimetypes = contribution.getMimeTypes();
        for (String mimeType : mimetypes) {
            mimeTypeMappings.put(mimeType, contribution);
        }
    }

    private void registerDocMapping(DocMetadataMappingDescriptor contribution) {
        String docType = contribution.docType;
        MetadataMappingDescriptor[] innerMappings = contribution.getInnerMapping();
        for (MetadataMappingDescriptor metadataMappingDescriptor : innerMappings) {
            addMappingToRegistries(docType, metadataMappingDescriptor);
        }
        String[] mappingIds = contribution.getMappingId();
        for (String string : mappingIds) {
            addMappingToRegistries(docType, mappings.get(string));
        }
    }

    private void addMappingToRegistries(String docType,
            MetadataMappingDescriptor metadataMappingDescriptor) {
        String nxPath = metadataMappingDescriptor.getNxpath();
        if (nxPath != null && !"".equals(nxPath)) {
            Map<String, Map<String, MetadataMappingDescriptor>> docNxPathMapping = nxPathMapping.get(docType);
            if (docNxPathMapping == null) {
                docNxPathMapping = new HashMap<String, Map<String, MetadataMappingDescriptor>>();
            }
            Map<String, MetadataMappingDescriptor> mimeTypeToMapper = docNxPathMapping.get(nxPath);
            if (mimeTypeToMapper == null) {
                mimeTypeToMapper = new HashMap<String, MetadataMappingDescriptor>();
                docNxPathMapping.put(nxPath, mimeTypeToMapper);
            }
            String[] mimeTypes = metadataMappingDescriptor.getMimeTypes();
            for (String mimeType : mimeTypes) {
                mimeTypeToMapper.put(mimeType, metadataMappingDescriptor);
            }
            nxPathMapping.put(docType, docNxPathMapping);
        } else {
            String[] mimeTypes = metadataMappingDescriptor.getMimeTypes();
            for (String mimeType : mimeTypes) {
                String id = docType + mimeType;
                bhMapping.put(id, metadataMappingDescriptor);
            }
        }
    }

    private void registerMapper(MetadataMapperDescriptor contribution)
            throws InstantiationException, IllegalAccessException {
        mappers.put(contribution.id, contribution.getMapper());
    }

    @Override
    public List<MetadataMappingDescriptor> getMappings(DocumentModel doc)
            throws ClientException {
        String docType = doc.getType();
        List<MetadataMappingDescriptor> mappings = new ArrayList<MetadataMappingDescriptor>();
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            Blob blob = bh.getBlob();
            if (blob != null) {
                String blobMimeType = blob.getMimeType();
                String bhId = docType + blobMimeType;
                MetadataMappingDescriptor bhMapper = bhMapping.get(bhId);
                if (bhMapper != null) {
                    mappings.add(bhMapper);
                }
            }
        }
        Map<String, Map<String, MetadataMappingDescriptor>> nxPathDocMapping = nxPathMapping.get(docType);
        if (nxPathDocMapping != null) {
            for (String nxPath : nxPathDocMapping.keySet()) {
                Blob blob = (Blob) doc.getPropertyValue(nxPath);
                if (blob != null) {
                    String blobMimeType = blob.getMimeType();
                    MetadataMappingDescriptor bhMapper = nxPathDocMapping.get(
                            nxPath).get(blobMimeType);
                    if (bhMapper != null) {
                        mappings.add(bhMapper);
                    }
                }
            }
        }
        return mappings;
    }

    @Override
    public void mapMetadata(DocumentModel doc, CoreSession coreSession)
            throws ClientException, IOException {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        Blob blob = bh.getBlob();
        String blobMimeType = blob.getMimeType();
        String docType = doc.getType();
        String bhId = docType + blobMimeType;
        MetadataMappingDescriptor bhMapper = bhMapping.get(bhId);
        if (bhMapper != null) {
            mapProperties(doc, blob, bhMapper, coreSession);
        }

        Map<String, Map<String, MetadataMappingDescriptor>> nxPathDocMapping = nxPathMapping.get(docType);
        if (nxPathDocMapping != null) {
            for (String nxPath : nxPathDocMapping.keySet()) {
                Blob b = (Blob) doc.getPropertyValue(nxPath);
                if (b != null) {
                    String bMimeType = b.getMimeType();
                    bhMapper = nxPathDocMapping.get(nxPath).get(bMimeType);
                    if (bhMapper != null) {
                        mapProperties(doc, b, bhMapper, coreSession);
                    }
                }
            }
        }
    }

    private void mapProperties(DocumentModel doc, Blob blob,
            MetadataMappingDescriptor metadataMappingDescriptor,
            CoreSession coreSession) throws ClientException, IOException {
        String mapper = metadataMappingDescriptor.getMapperId();
        MetadataMapper mapperInstance = mappers.get(mapper);
        mapperInstance.mapProperties(doc, blob, metadataMappingDescriptor,
                coreSession);
    }

    public Map<String, MetadataMapper> getMappers() {
        return mappers;
    }

}