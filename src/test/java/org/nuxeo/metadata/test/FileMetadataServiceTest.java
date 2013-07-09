package org.nuxeo.metadata.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.metadata.FileMetadataService;
import org.nuxeo.metadata.MetadataMappingDescriptor;
import org.nuxeo.metadata.PropertyItemDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "nuxeo-platform-filemanager-metadata",
        "nuxeo-platform-filemanager-metadata-test" })
@LocalDeploy({
        "nuxeo-platform-filemanager-metadata-test:OSGI-INF/metadata-core-contrib.xml",
        "nuxeo-platform-filemanager-metadata-test:OSGI-INF/test-tika-contrib.xml" })
public class FileMetadataServiceTest {

    @Inject
    CoreSession session;

    @Test
    public void testService() throws Exception {
        FileMetadataService serviceInterface = Framework.getService(FileMetadataService.class);
        assertNotNull(serviceInterface);
        DocumentModel file = createFileDocumentModelWithPdf();
        List<MetadataMappingDescriptor> fileMappings = serviceInterface.getMappings(file);
        assertNotNull(fileMappings);
        assertFalse(fileMappings.isEmpty());
        assertEquals(1, fileMappings.size());

        DocumentModel file2 = createFileDocumentModelWithPng();
        fileMappings = serviceInterface.getMappings(file2);
        assertNotNull(fileMappings);
        assertFalse(fileMappings.isEmpty());
        assertEquals(1, fileMappings.size());

        MetadataMappingDescriptor mapping = fileMappings.get(0);
        String[] facets = mapping.getRequiredFacets();
        assertNotNull(facets);
        assertEquals(1, facets.length);
        assertEquals("xmp", facets[0]);
        String[] schemas = mapping.getRequiredSchema();
        assertNotNull(schemas);
        assertEquals(1, schemas.length);
        assertEquals("dublincore", schemas[0]);

        assertEquals("image/png", mapping.getMimeTypes()[0]);
        assertEquals("files:files/item[0]/file", mapping.getNxpath());
        assertEquals("defaultTikaMapper", mapping.getMapperId());
        PropertyItemDescriptor[] properties = mapping.getProperties();
        assertNotNull(properties);
        assertEquals(1, properties.length);

        PropertyItemDescriptor item = properties[0];
        assertNotNull(item);
        assertEquals("title", item.getMetadataName());
        assertEquals("dc:title", item.getXpath());
        assertEquals("sync", item.getPolicy());

    }

    private DocumentModel createFileDocumentModelWithPdf() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        File f = FileUtils.getResourceFileFromContext("data/hello.pdf");
        Blob blob = new FileBlob(f);
        blob.setMimeType("application/pdf");
        doc.setPropertyValue("file:content", (Serializable) blob);
        return doc;
    }

    private DocumentModel createFileDocumentModelWithPng() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "file2", "File2");
        File f = FileUtils.getResourceFileFromContext("data/training.png");
        Blob blob = new FileBlob(f);
        blob.setMimeType("image/png");
        Map<String, Serializable> blobMap = new HashMap<String, Serializable>();
        blobMap.put("file", (Serializable) blob);
        blobMap.put("filename", "training.png");
        List<Map<String, Serializable>> blobs = new ArrayList<Map<String, Serializable>>();
        blobs.add(blobMap);
        doc.setPropertyValue("files:files", (Serializable) blobs);
        return doc;
    }

}
