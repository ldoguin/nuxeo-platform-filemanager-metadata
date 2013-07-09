package org.nuxeo.metadata.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.metadata.TikaDefaultMapper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;
import com.lowagie.text.pdf.PdfReader;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor",
        "nuxeo-platform-filemanager-metadata",
        "nuxeo-platform-filemanager-metadata-test" })
@LocalDeploy({ "nuxeo-platform-filemanager-metadata-test:OSGI-INF/exiftool-metadata-contrib.xml" })
public class TestExifToolMapper extends TikaDefaultMapper {

    private static final String HELLO_WORLD = "Hello World";
    @Inject
    CoreSession session;

    @Test
    public void testXMPmapping() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        CommandAvailability ca = cles.getCommandAvailability("exiftool-read");
        if (!ca.isAvailable()) {
            System.out.println("exiftool is not available, skipping test");
            return;
        }

        DocumentModel doc = createPSDDocumentModel();
        assertEquals("8", doc.getPropertyValue("xmp:BitsPerSample"));
        assertEquals("930", doc.getPropertyValue("xmp:ImageWidth"));
        assertEquals("784", doc.getPropertyValue("xmp:ImageLength"));

        doc = createPDFDocumentModel();
        assertEquals("1", doc.getPropertyValue("xmp:NPages"));
        assertEquals("Writer", doc.getPropertyValue("xmp:CreatorTool"));
        // test write metadata to file
        doc.setPropertyValue("dc:title", HELLO_WORLD);
        session.saveDocument(doc);
        Blob b = (Blob) doc.getPropertyValue("file:content");
        PdfReader reader = new PdfReader(b.getStream());
        String title = (String) reader.getInfo().get("Title");
        assertEquals(HELLO_WORLD, title);
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
