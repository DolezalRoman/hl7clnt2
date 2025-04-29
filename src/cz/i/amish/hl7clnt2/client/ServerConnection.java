/*
 * Created on 8.4.2004
 *
 * $Id: ServerConnection.java,v 1.2 2017/04/24 13:44:07 raska Exp $
 * $Log: ServerConnection.java,v $
 * Revision 1.2  2017/04/24 13:44:07  raska
 * vyhozeny komentare z duvodu problemu s cestinou
 *
 * Revision 1.1  2005/10/09 16:41:23  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.1  2005/05/20 13:28:02  raska
 * Opraveno jmeno package cz.i.amish.hl7cln2.client.
 *
 * Revision 1.1  2005/05/16 12:36:29  raska
 * Zalozeni projektu hl7clnt2 v repository.
 *
 */
package cz.i.amish.hl7clnt2.client;

import ca.uhn.hl7v2.model.Message;

/**
 * @author raska
 *
 */
public interface ServerConnection {

	public void connect() throws Exception;
	
	public void disconnect() throws Exception;
	
	public Message send(Message msg) throws Exception;
}
