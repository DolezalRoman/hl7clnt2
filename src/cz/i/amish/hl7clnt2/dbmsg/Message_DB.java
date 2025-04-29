package cz.i.amish.hl7clnt2.dbmsg;


import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v23.segment.MRG;
import ca.uhn.hl7v2.model.v23.segment.PID;

/*
 * Created on 29.3.2004
 *
 * $Id: Message_DB.java,v 1.4 2020/02/26 14:44:03 amis Exp $
 * $Log: Message_DB.java,v $
 * Revision 1.4  2020/02/26 14:44:03  amis
 * Migrated to OpenJDK 8.
 *
 * Revision 1.3  2017/12/04 12:39:04  raska
 * ver 2.7.2; klient podporuje upravu rodneho cisla ve zprave (lomitko, bez lomitka, hodnota z databaze)
 *
 * Revision 1.2  2017/04/24 13:44:08  raska
 * vyhozeny komentare z duvodu problemu s cestinou
 *
 * Revision 1.1  2005/10/09 16:41:20  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.4  2005/08/25 08:42:04  raska
 * Doplnena moznost vytvaret dynamicky seznam identifikatoru pacienta (PID-3 a MRG-1) dle nastaveni namespace v property.
 *
 * Revision 1.3  2005/07/15 09:22:16  raska
 * Upravena metoda getStr, vraci SQLException.
 *
 * Revision 1.2  2005/07/11 16:21:09  raska
 * Rozsireni zpravy ORM o segmenty DG1 a OBX, podpora pro prenos diagnoz a vysledku mereni. Odpovidajicim zpusobem rozsireny konfiguracni soubory.
 *
 * Revision 1.1  2005/05/16 12:36:21  raska
 * Zalozeni projektu hl7clnt2 v repository.
 *
 */

public abstract class Message_DB {

	protected final Properties prop;
	private static final Logger logger = Logger.getLogger(Message_DB.class);
	
	protected Message_DB(Properties p) {
		this.prop = p;
	}
	
	protected final String getStr(ResultSet rs, String col) throws SQLException {
		String r = "";
//		try {
			r = rs.getString(col);
			r = (r != null) ? r.trim() : "";
//		} catch (SQLException e) {
//			r = "";
//		}
		try {
			String encoding = this.prop.getProperty("hl7.output.encoding");
			if (encoding != null && encoding.equals("ASCII"))
				return Recode.toAscii(r);
			return r;
		}
		catch (UnsupportedEncodingException e) {
			return r;
		}
	}
	
	protected final String getTxt(ResultSet rs, String col) {
		String r = "";
		try {
			byte[] b = rs.getBytes(col);
			if (b != null)
				r = new String(rs.getBytes(col),"ISO-8859-2");
			else
				r = "";
			// r = rs.getString(col);
			r = (r != null) ? r.trim() : "";					
		} catch (SQLException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return r;	
	}
	
	protected final String getRC(ResultSet rs, String col) throws SQLException {
		String res = getStr(rs, col);
		
		if (res != null && res.length() > 0) {
			String modif = this.prop.getProperty("hl7.output.rc_modif");
			if (modif != null) {
				String pure = "";
				for (String c : res.split("/")) {
					pure += c;
				}
				
				if (modif.toUpperCase().startsWith("S")) {
					if (pure != null && pure.length() > 6) {
						res = pure.substring(0, 6) + "/" + pure.substring(6);
					}
				}
				else if (modif.toUpperCase().startsWith("N")) {
					res = pure;
				}
			}
		}
		
		return res;		
	}
	
	protected final String[] computePatientAge(Date bd) {
		String[] age = new String[2];

		if (bd == null)		return null;
		
		long d = (new Date()).getTime() - bd.getTime();
		if (d < 0)	return null;
		
		d = d / 1000 / 3600 / 24; 
		
		if (d > Long.parseLong(prop.getProperty("patient.age.year.threshold","365"))) {
			age[0] = Long.valueOf(d / 365).toString();
			age[1] = prop.getProperty("patient.age.year.unit","year");
		}
		else if (d > Long.parseLong(prop.getProperty("patient.age.month.threshold","90"))) {
			age[0] = Long.valueOf(d / 30).toString();
			age[1] = prop.getProperty("patient.age.month.unit","month");
		}
		else if (d > Long.parseLong(prop.getProperty("patient.age.week.threshold","14"))) {
			age[0] = Long.valueOf(d / 7).toString();
			age[1] = prop.getProperty("patient.age.week.unit","week");
		}
		else {
			age[0] = Long.valueOf(d).toString();
			age[1] = prop.getProperty("patient.age.day.unit","day");
		}
		
		return age;
	}
	
	protected void setPIDInternalIDs(PID pid, String ic_pac, String rc) throws HL7Exception {
		String[] nspaces = prop.getProperty("message.ALL.PID.internal_nspaces","").trim().split(" +");
		int ind = 0;
		for (int i = 0; i < nspaces.length; i++) {
			String nspace = nspaces[i];
			if (nspace.equals("AMISHHL7")) {
                pid.getPatientIDInternalID(ind).getID().setValue(ic_pac);
                pid.getPatientIDInternalID(ind).getAssigningAuthority().getNamespaceID().setValue(nspace);
				ind += 1;
			}
			else if (nspace.equals("RC")) {
                pid.getPatientIDInternalID(ind).getID().setValue(rc);
                pid.getPatientIDInternalID(ind).getAssigningAuthority().getNamespaceID().setValue(nspace);
				ind += 1;
			}
			else if (nspace.equals("RC@ID")) {
				int patIdLen = Integer.valueOf(this.prop.getProperty("patient_id_suffix_length","10")).intValue();
				String o_ic_pac = ic_pac.substring((ic_pac.length() < patIdLen) ? 0 : ic_pac.length()-patIdLen);
				pid.getPatientIDInternalID(ind).getID().setValue(rc + prop.getProperty("patient_id_suffix_sep") + o_ic_pac);
				pid.getPatientIDInternalID(ind).getAssigningAuthority().getNamespaceID().setValue(nspace);
				ind += 1;
			}
			else {
				logger.warn("Unknown PID Namespece Identifier (" + nspace + ") defined for Internal IDs, nothing will be generated for this one!");
			}
		}
	}

	protected void setMRGInternalIDs(MRG mrg, String ic_pac, String rc) throws HL7Exception {
		String[] nspaces = prop.getProperty("message.ADT.MRG.internal_nspaces","").trim().split(" +");
		int ind = 0;
		for (int i = 0; i < nspaces.length; i++) {
			String nspace = nspaces[i];
			if (nspace.equals("AMISHHL7")) {
                mrg.getPriorPatientIDInternal(ind).getID().setValue(ic_pac);
				mrg.getPriorPatientIDInternal(ind).getAssigningAuthority().getNamespaceID().setValue(nspace);
				ind += 1;
			}
			else if (nspace.equals("RC")) {
                mrg.getPriorPatientIDInternal(ind).getID().setValue(rc);
				mrg.getPriorPatientIDInternal(ind).getAssigningAuthority().getNamespaceID().setValue(nspace);
				ind += 1;
			}
			else if (nspace.equals("RC@ID")) {
				int patIdLen = Integer.valueOf(this.prop.getProperty("patient_id_suffix_length","10")).intValue();
				String o_ic_pac = ic_pac.substring((ic_pac.length() < patIdLen) ? 0 : ic_pac.length()-patIdLen);
                mrg.getPriorPatientIDInternal(ind).getID().setValue(rc + prop.getProperty("patient_id_suffix_sep") + o_ic_pac);
				mrg.getPriorPatientIDInternal(ind).getAssigningAuthority().getNamespaceID().setValue(nspace);
				ind += 1;
			}
			else {
				logger.warn("Unknown MRG Namespece Identifier (" + nspace + ") defined for Internal IDs, nothing will be generated for this one!");
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
/*
		Properties prop = new Properties();
		prop.setProperty("patient_id_suffix_length","4");
		
		Message_DB msgdb = new Message_DB(prop);
		
		String[] age = msgdb.computePatientAge(new Date(1970-1900,4-1,7));
		System.out.println("vek: " + age[0] + " " + age[1]);
		
		age = msgdb.computePatientAge(new Date(2003-1900,7-1,11));
		System.out.println("vek: " + age[0] + " " + age[1]);
		age = msgdb.computePatientAge(new Date(2004-1900,7-1,11));
		System.out.println("vek: " + age[0] + " " + age[1]);
		age = msgdb.computePatientAge(new Date(2005-1900,6-1,1));
		System.out.println("vek: " + age[0] + " " + age[1]);
		age = msgdb.computePatientAge(new Date(2005-1900,7-1,3));
		System.out.println("vek: " + age[0] + " " + age[1]);

	
		String s1 = "123";
		String s2 = "123456789";
		
		System.out.println(msgdb.amendPatientID(s1));
		System.out.println(msgdb.amendPatientID(s2));
*/	
		
	}
}
