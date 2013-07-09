package org.nuxeo.metadata.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.Serializable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.metadata.TikaDefaultMapper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "nuxeo-platform-filemanager-metadata" })
public class TestTikaDefaultMapper extends TikaDefaultMapper {

    @Inject
    CoreSession session;

    @Test
    public void testXMPmapping() throws Exception {
        DocumentModel doc = createPSDDocumentModel();
        assertEquals("8", doc.getPropertyValue("xmp:BitsPerSample"));
        assertEquals("930", doc.getPropertyValue("xmp:ImageWidth"));
        assertEquals("784", doc.getPropertyValue("xmp:ImageLength"));

        doc = createPDFDocumentModel();
        assertEquals("1", doc.getPropertyValue("xmp:NPages"));
        assertEquals("Writer", doc.getPropertyValue("xmp:CreatorTool"));
    }

    private DocumentModel createPSDDocumentModel() throws Exception {
        return createDocument("application/vnd.adobe.photoshop",
                "data/montagehp.psd");
    }

    private DocumentModel createPDFDocumentModel() throws Exception {
        return createDocument("application/pdf", "data/hello.pdf");
    }

    private DocumentModel createDocument(String mimetype, String filePath)
            throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        File f = FileUtils.getResourceFileFromContext(filePath);
        Blob blob = new FileBlob(f);
        blob.setMimeType(mimetype);
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);
        return doc;

    }
}
