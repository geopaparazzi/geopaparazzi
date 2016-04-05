/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.util.xml;

/**
 * @author tag
 * @version $Id: XMLParserNotificationListener.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public interface XMLParserNotificationListener
{
    /**
     * Receives notification events from the parser context.
     *
     * @param notification the notification object containing the notificaton type and data.
     */
    public void notify(XMLParserNotification notification);
}
