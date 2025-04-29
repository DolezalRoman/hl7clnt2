package cz.i.amish.hl7clnt2.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import cz.i.amish.hl7clnt2.dbmsg.*;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.llp.LLPException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;

/*
 * Created on 15.3.2004
 *
 * $Id: HL7Clnt.java,v 1.5 2020/02/26 14:44:03 amis Exp $
 * $Log: HL7Clnt.java,v $
 * Revision 1.5  2020/02/26 14:44:03  amis
 * Migrated to OpenJDK 8.
 *
 * Revision 1.4  2017/04/24 13:44:07  raska
 * vyhozeny komentare z duvodu problemu s cestinou
 *
 * Revision 1.3  2017/04/24 13:25:27  raska
 * ver. 2.7.0; spojeni verze od Pavla Navrkala, odstraneny warning
 *
 * Revision 1.2  2015/10/22 08:23:21  dolezal
 * Osetreni dvou vlaken clienta.
 *
 * Revision 1.1  2005/10/09 16:41:23  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.5  2005/10/03 11:17:09  raska
 * Doplnena podpora zpravy ADT/A40.
 *
 * Revision 1.4  2005/10/02 08:53:20  dolezal
 * Osetrena udalost A40.
 *
 * Revision 1.3  2005/06/13 08:00:16  raska
 * Opraven zapis do tabulky al7_zpravy, atribut iter, v pripade, ze vzdaleny server neodpovida.
 *
 * Revision 1.2  2005/06/03 12:48:39  raska
 * Doplneno rizeni odesilani zprav podle typu zpravy a udalosti.
 *
 * Revision 1.1  2005/05/20 13:28:02  raska
 * Opraveno jmeno package cz.i.amish.hl7cln2.client.
 *
 * Revision 1.2  2005/05/20 13:17:08  raska
 * Procisteni verze od vsech nanosu FNBR.
 *
 * Revision 1.1  2005/05/16 12:36:29  raska
 * Zalozeni projektu hl7clnt2 v repository.
 *
 */

/**
 * 
 * @author raska
 *
 */

public class HL7Clnt {

	private final Properties prop;
	private HL7ClntDb db;
	private ServerConnection hl7Srv;

	private static final Logger logger = Logger.getLogger(HL7Clnt.class);

	private HL7Clnt(Properties p) {
		prop = p;
	}

	private final void run() throws Exception {

		// Inicializace pripojeni na databazi
		this.db = new HL7ClntDb(this.prop);

		// Vytvoreni spojeni na HL7 server
		this.hl7Srv = new ServerMLLPConnection(this.prop);

		// Zakladni ridici cyklus klienta - jednorazovy pruchod/nekolikanasobny pruchod/nekonecny cyklus

		int loopCount = Integer.valueOf(prop.getProperty("loopcount","0")).intValue(); 
		int loopDelay = Integer.valueOf(prop.getProperty("loopdelay","5")).intValue();

		logger.info("Hlavni smycka (pocet cyklu = "+loopCount+", zpozdeni mezi pruchody = "+loopDelay+")" );

		if (loopCount == 0)
			for (;;) {
				try {
					passThrough();
				} catch (LLPException e) {
					logger.error("LLP Error: ",e);
				} catch (IOException e) {
					logger.error("IO Error: ",e);
				} catch (SQLException e) {
					logger.error("SQL Error: ",e);
				} catch (HL7Exception e) {
					logger.error("HL7 Error: ",e);
				}
				try {
					Thread.sleep(loopDelay * 1000);
				} catch (InterruptedException e1) {
					logger.warn("Interrupted Exception catched, so what else? ... terminate");
					throw e1;
				}
			}
		else 
			for (int i = 0; i < loopCount; i++) {
				try {
					passThrough();
				} catch (LLPException e) {
					logger.error("LLP Error: ",e);
				} catch (IOException e) {
					logger.error("IO Error: ",e);
				} catch (SQLException e) {
					logger.error("SQL Error: ",e);
				} catch (HL7Exception e) {
					logger.error("HL7 Error: ",e);
				}
				if (i < loopCount-1) {
					try {
						Thread.sleep(loopDelay * 1000);
					} catch (InterruptedException e1) {
						logger.warn("Interrupted Exception catched, so what else? ... terminate");
						throw e1;
					}
				}
			}
	}

	private final void passThrough() throws Exception {

		logger.info("Jeden pruchod clienta ... ");

		// Pripojeni na databazi
		this.db.connect();

		try {
			List<Integer> ids = this.db.getMsgIdList();
			logger.info("Pocet zprav pripravenych ke zpracovani: " + ids.size());

			if (ids.size() > 0) {
				// Napojeni na vzdaleny sever
				this.hl7Srv.connect();
				String updateDisabledMessages = prop.getProperty("update.disabled.messages","yes").toLowerCase();

				try {
					for (int i = 0; i < ids.size(); i++) {
						int pk = ((Integer)ids.get(i)).intValue();
						int msgId = this.db.getMsgId(pk);
						String msgType = this.db.getMsgType(pk);

						String enable = prop.getProperty("message.event." + msgType.toUpperCase() + ".enable","no").toLowerCase();
						if (!enable.equals("yes")) {
							if (updateDisabledMessages.equals("yes")) { 
								this.db.updateStatus(pk,11);
								this.db.commit();
							}
							continue;
						}

						OutboundMessageDb dbMsg = null;

						if (msgType.equals("ADT^A01")) {
							dbMsg = new ADT_A01_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ADT^A02")) {
							dbMsg = new ADT_A02_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ADT^A03")) {
							dbMsg = new ADT_A03_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ADT^A04")) {
							dbMsg = new ADT_A04_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ADT^A05")) {
							dbMsg = new ADT_A05_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ADT^A08")) {
							dbMsg = new ADT_A08_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ADT^A11")) {
							dbMsg = new ADT_A11_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ADT^A12")) {
							dbMsg = new ADT_A12_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ADT^A13")) {
							dbMsg = new ADT_A13_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ADT^A18")) {
							dbMsg = new ADT_A18_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ADT^A40")) {
							dbMsg = new ADT_A40_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ORM^O01")) {
							dbMsg = new ORM_O01_DB(this.db.getDbConnection(),this.prop);
						}
						if (msgType.equals("ORU^R01")) {
							dbMsg = new ORU_R01_DB(this.db.getDbConnection(),this.prop);
						}

						if (dbMsg == null) {
							logger.warn("Neznamy typ zpravy (" + msgType + ")!");
						}
						else {
							Message msgIn;
							try {
								dbMsg.loadFromDb(msgId);

								msgIn = this.hl7Srv.send(dbMsg.getMsg());
							} catch (Exception e) {
								this.db.incrementIter(pk);
								this.db.commit();
								throw e;
							}

							if (msgIn != null) { 
								Terser t = new Terser(msgIn);
								String ack = t.get("MSA-1");

								if (ack.compareTo("AA") == 0) {
									this.db.updateStatus(pk,7);
									this.db.commit();
								}
								else if (ack.compareTo("AE") == 0) {
									this.db.updateStatus(pk,8);
									this.db.commit();
								}
								else if (ack.compareTo("AR") == 0) {
									this.db.updateStatus(pk,9);
									this.db.commit();
								}
								else {
									logger.warn("NEZNAMY TYP ACK (" + ack +")!");
								}
							}
							else {
								this.db.incrementIter(pk);
								this.db.commit();
								logger.warn("Zadna odpoved na zpravu ...");
							}
						}
					}
				} finally {
					this.hl7Srv.disconnect();
				}
			}			
		} finally {
			this.db.close();
		}
	}

	public static void main(String[] args) throws Exception {

		if (args.length > 1) {
			System.err.println("usage: HL7Clnt [property file]");
			System.exit(1);
		}

		// Vytvoreni property objektu a nacteni ze souboru
		String propFileName = (args.length == 1) ? args[0] : "conf/HL7Clnt.property";

		FileInputStream propFile = new FileInputStream(propFileName);

		Properties prop = new Properties();
		prop.load(propFile);

		// Inicializace logovani
		if (prop.containsKey("logconfig")) {
			org.apache.log4j.xml.DOMConfigurator.configure(prop.getProperty("logconfig"));
		}

		logger.info("Start aplikace ...");


		try {
			try {
				HL7Clnt clnt = new HL7Clnt(prop);
				clnt.run();
			}
			catch (Exception e) {
				logger.error("Exception: ",e);
				// throw e;
			}
		} catch (RuntimeException e) {
			logger.error("RuntimeException: ",e);
			e.printStackTrace();
		}

		logger.info("Ukonceni aplikace ...");
	}
}
