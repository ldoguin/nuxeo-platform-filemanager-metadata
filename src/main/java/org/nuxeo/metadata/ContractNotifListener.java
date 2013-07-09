/**
 * 
 */

package org.nuxeo.metadata;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author ldoguin
 */
public class ContractNotifListener implements EventListener {

    private static final String QUERY_CONTRACTS = "Select * From Contract WHERE ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted' AND dc:expired = DATE '%s'";

    public void handleEvent(Event event) throws ClientException {
        CoreSession coreSession = event.getContext().getCoreSession();
        Calendar expirationDate  = Calendar.getInstance();
        expirationDate.add(Calendar.DATE, 30);
        Date now = expirationDate.getTime();
        String date = new SimpleDateFormat("yyyy-MM-dd").format(now);
        String query = String.format(QUERY_CONTRACTS, date);
        DocumentModelList contracts = coreSession.query(query);

        EventService eventService;
        try {
            eventService = Framework.getService(EventService.class);
            for (DocumentModel contract : contracts) {
                DocumentEventContext ctx = new DocumentEventContext(
                        coreSession, coreSession.getPrincipal(), contract);
                Event contractExpiredEvent = ctx.newEvent("contractExpired");
                eventService.fireEvent(event);
            }
        } catch (Exception e) {
            throw new RuntimeException("could not get the EventService", e);
        }
    }

}
