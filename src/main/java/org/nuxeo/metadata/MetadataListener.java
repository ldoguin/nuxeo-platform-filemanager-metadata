/**
 * 
 */

package org.nuxeo.metadata;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.runtime.api.Framework;

/**
 * @author ldoguin
 */
public class MetadataListener implements EventListener {

    public final BlobsExtractor blobExtractor = new BlobsExtractor();

    public void handleEvent(Event event) throws ClientException {

        if (event.getContext() instanceof DocumentEventContext) {
            DocumentEventContext context = (DocumentEventContext) event.getContext();
            DocumentModel doc = context.getSourceDocument();
            CoreSession coreSession = context.getCoreSession();
            try {
                FileMetadataService fms = Framework.getService(FileMetadataService.class);
                fms.mapMetadata(doc, coreSession);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

}
