package cz.i.amish.hl7clnt2.dbmsg;


import java.sql.SQLException;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;

/*
 * Created on 25.3.2004
 *
 * $Id: OutboundMessageDb.java,v 1.2 2017/04/24 13:44:08 raska Exp $
 * $Log: OutboundMessageDb.java,v $
 * Revision 1.2  2017/04/24 13:44:08  raska
 * vyhozeny komentare z duvodu problemu s cestinou
 *
 * Revision 1.1  2005/10/09 16:41:19  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.1  2005/05/16 12:36:20  raska
 * Zalozeni projektu hl7clnt2 v repository.
 *
 */

/**
 * 
 * @author raska
 */

public interface OutboundMessageDb {
	
	public Message getMsg();
	
	public void loadFromDb(int id) throws SQLException, HL7Exception;
}
