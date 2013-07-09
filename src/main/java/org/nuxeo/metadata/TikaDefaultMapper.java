package org.nuxeo.metadata;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.utils.BlobsExtractor;

public class TikaDefaultMapper implements MetadataMapper {

    public final BlobsExtractor blobExtractor = new BlobsExtractor();

    @Override
    public void mapProperties(DocumentModel doc, Blob blob,
            MetadataMappingDescriptor mappingDescriptor, CoreSession session)
            throws ClientException, IOException {
        String nxPath = mappingDescriptor.getNxpath();
        if (nxPath != null && !"".equals(nxPath)) {
            Property p = doc.getProperty(nxPath);
            if (p != null) {
                if (p.isDirty()) {
                    readProperties(doc, blob, mappingDescriptor);
                    // ignore write prop
                    return;
                } else {
                    writeChangesToFile(doc, blob, mappingDescriptor);
                }
            }
        } else {
            // No specified XPath, we assume it's the bh mapper
            // is the property holding the bh dirty?
            try {
                for (Property prop : blobExtractor.getBlobsProperties(doc)) {
                    if (prop.isDirty()) {
                        Blob b = prop.getValue(Blob.class);
                        if (b.equals(blob)) {
                            // we found the property in the BlobHolder
                            readProperties(doc, blob, mappingDescriptor);
                            return;
                        }
                    }
                }
                writeChangesToFile(doc, blob, mappingDescriptor);
            } catch (Exception e) {
                throw new IOException("Error while extracting dirty blob properties");
            }
        }

    }

    private void writeChangesToFile(DocumentModel doc, Blob b,
            MetadataMappingDescriptor mappingDescriptor) throws ClientException {
        List<String> dirtyProperties = getDirtyPropertiesXPath(doc);
        Metadata meta = new Metadata();

        for (PropertyItemDescriptor property : mappingDescriptor.getProperties()) {
            if (dirtyProperties.contains(property.getXpath())
                    && property.getPolicy().equals("sync")) {
                // write this property to the file
                String propertyValue = (String) doc.getPropertyValue(property.getXpath());
                meta.set(property.getMetadataName(), propertyValue);
            }
        }
        Tika tika = new Tika();

    }

    private void readProperties(DocumentModel doc, Blob blob,
            MetadataMappingDescriptor mappingDescriptor) throws IOException,
            PropertyException, ClientException {
        String[] requiredFacets = mappingDescriptor.getRequiredFacets();
        for (String facet : requiredFacets) {
            if (!doc.hasFacet(facet)) {
                doc.addFacet(facet);
            }
        }
        Tika tika = new Tika();
        Metadata metadataParser = new Metadata();
        InputStream stream = new BufferedInputStream(blob.getStream());
        tika.parse(stream, metadataParser);

        for (PropertyItemDescriptor property : mappingDescriptor.getProperties()) {
            String metadata = metadataParser.get(property.getMetadataName());
            if (metadata != null && !"".equals(metadata)) {
                Property p = doc.getProperty(property.getXpath());
                p.setValue(metadata);
            }
        }
    }

    public List<String> getDirtyPropertiesXPath(DocumentModel source)
            throws ClientException {
        List<String> dirtyPropertiesName = new ArrayList<String>();
        DocumentPart[] docParts = source.getParts();
        for (DocumentPart docPart : docParts) {
            Iterator<Property> dirtyChildrenIterator = docPart.getDirtyChildren();
            while (dirtyChildrenIterator.hasNext()) {
                Property property = dirtyChildrenIterator.next();
                if (!property.isContainer() && property.isDirty()) {
                    dirtyPropertiesName.add(docPart.getName() + ":"
                            + property.getField().getName().getLocalName());
                } else {
                    List<Property> dirtyProps = addChildrenDirtyProperties(
                            property, new ArrayList<Property>());
                    for (Property dirtyProperty : dirtyProps) {
                        dirtyPropertiesName.add(docPart.getName() + ":"
                                + dirtyProperty.getPath().substring(1));
                    }
                }
            }
        }
        return dirtyPropertiesName;
    }

    private List<Property> addChildrenDirtyProperties(Property property,
            List<Property> dirtyProperties) {
        if (!property.isContainer() && property.isDirty()) {
            dirtyProperties.add(property);
            return dirtyProperties;
        } else {
            Iterator<Property> dirtyChildrenIterator = property.getDirtyChildren();
            while (dirtyChildrenIterator.hasNext()) {
                Property chilProperty = dirtyChildrenIterator.next();
                dirtyProperties = addChildrenDirtyProperties(chilProperty,
                        dirtyProperties);
            }
            return dirtyProperties;
        }
    }
}
