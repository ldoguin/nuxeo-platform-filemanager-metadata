package org.nuxeo.metadata;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

public class ExifToolMapper implements MetadataMapper {

    public static final Log log = LogFactory.getLog(ExifToolMapper.class);

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
                    readProperties(doc, blob, mappingDescriptor, session);
                    // ignore writeChangestoFile
                    return;
                } else {
                    writeChangesToFile(doc, blob, mappingDescriptor, nxPath,
                            session);
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
                            readProperties(doc, blob, mappingDescriptor,
                                    session);
                            return;
                        }
                    }
                }
                writeChangesToFile(doc, blob, mappingDescriptor, null, session);
            } catch (Exception e) {
                throw new IOException(
                        "Error while extracting dirty blob properties");
            }
        }

    }

    private void writeChangesToFile(DocumentModel doc, Blob blob,
            MetadataMappingDescriptor mappingDescriptor, String nxPath,
            CoreSession session) throws ClientException, IOException {
        StringBuilder sb = new StringBuilder();
        for (PropertyItemDescriptor property : mappingDescriptor.getProperties()) {
            Property p = doc.getProperty(property.getXpath());
            if (p.isDirty() && property.getPolicy().equals("sync")) {
                // write this property to the file
                sb.append('-');
                sb.append(property.getMetadataName());
                sb.append('=');
                sb.append("\\\\'");
                sb.append(p.getValue());
                sb.append("\\\\'");
                sb.append(' ');
            }
        }
        String tagList = sb.toString();

        CommandLineExecutorService cles;
        try {
            cles = Framework.getService(CommandLineExecutorService.class);
        } catch (Exception e) {
            throw new RuntimeException(
                    "ComandLineExecutorService was not found.", e);
        }

        CommandAvailability ca = cles.getCommandAvailability("exiftool-write");
        if (!ca.isAvailable()) {
            log.warn("Attempted to use exiftool but did not find it. ");
            return;
        }
        File file = makeFile(blob);
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("tagList", tagList);
        params.addNamedParameter("inFilePath", file);
        try {
            cles.execCommand("exiftool-write", params);
        } catch (CommandNotAvailable e) {
            throw new RuntimeException("Command exiftool is not available.", e);
        }
        Blob fileBlob = new FileBlob(file);
        if (nxPath == null) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            bh.setBlob(fileBlob);
        } else {
            doc.setPropertyValue(nxPath, (Serializable) fileBlob);
        }
    }

    private void readProperties(DocumentModel doc, Blob blob,
            MetadataMappingDescriptor mappingDescriptor, CoreSession session)
            throws IOException, PropertyException, ClientException {
        CommandLineExecutorService cles;
        try {
            cles = Framework.getService(CommandLineExecutorService.class);
        } catch (Exception e) {
            throw new RuntimeException(
                    "ComandLineExecutorService was not found.", e);
        }

        CommandAvailability ca = cles.getCommandAvailability("exiftool-read");
        if (!ca.isAvailable()) {
            log.warn("Attempted to use exiftool but did not find it. ");
            return;
        }
        CmdParameters params = new CmdParameters();
        File file = makeFile(blob);
        params.addNamedParameter("inFilePath", file);
        try {
            ExecResult er = cles.execCommand("exiftool-read", params);
            if (!er.isSuccessful()) {
                log.error("There was an error executing the following command: "
                        + er.getCommandLine());
                return;
            }
            // check facets availability
            String[] requiredFacets = mappingDescriptor.getRequiredFacets();
            for (String facet : requiredFacets) {
                if (!doc.hasFacet(facet)) {
                    doc.addFacet(facet);
                }
            }

            StringBuilder sb = new StringBuilder();
            for (String line : er.getOutput()) {
                sb.append(line);
            }
            String jsonOutput = sb.toString();
            ObjectMapper jacksonMapper = new ObjectMapper();
            JsonNode jsArray = jacksonMapper.readTree(jsonOutput);
            JsonNode jsonObject = jsArray.get(0);
            for (PropertyItemDescriptor property : mappingDescriptor.getProperties()) {
                JsonNode node = jsonObject.get(property.getMetadataName());
                if (node == null) {
                    // try fallback
                    for (String fallbackMetadata: property.fallbackMetadata) {
                        node = jsonObject.get(fallbackMetadata);
                        if (node != null) {
                            break;
                        }
                    }
                }
                if (node != null) {
                    String metadata = node.getValueAsText();
                    if (metadata != null && !"".equals(metadata)) {
                        Property p = doc.getProperty(property.getXpath());
                        p.setValue(metadata);
                    }
                }
            }

        } catch (CommandNotAvailable e) {
            throw new RuntimeException("Command exiftool is not available.", e);
        }
    }

    protected File makeFile(Blob blob) throws IOException {
        File sourceFile = getFileFromBlob(blob);
        if (sourceFile == null) {
            String filename = blob.getFilename();
            sourceFile = File.createTempFile(filename, ".tmp");
            blob.transferTo(sourceFile);
            Framework.trackFile(sourceFile, this);
        }
        return sourceFile;
    }

    protected File getFileFromBlob(Blob blob) {
        if (blob instanceof FileBlob) {
            return ((FileBlob) blob).getFile();
        } else if (blob instanceof SQLBlob) {
            StreamSource source = ((SQLBlob) blob).getBinary().getStreamSource();
            return ((FileSource) source).getFile();
        }
        return null;
    }
}
