/*
 * ADT_A40 - Slouceni pacienta  
 * 
 * $Id: ADT_A40_DB.java,v 1.2 2017/12/04 12:39:04 raska Exp $
 * $Log: ADT_A40_DB.java,v $
 * Revision 1.2  2017/12/04 12:39:04  raska
 * ver 2.7.2; klient podporuje upravu rodneho cisla ve zprave (lomitko, bez lomitka, hodnota z databaze)
 *
 * Revision 1.1  2005/10/09 16:41:17  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.2  2005/10/03 11:17:11  raska
 * Doplnena podpora zpravy ADT/A40.
 *
 * Revision 1.1  2005/10/02 08:54:22  dolezal
 * Registrace zpracovani udalosti A40 do CVS.
 *
 */

package cz.i.amish.hl7clnt2.dbmsg;
 
import java.util.*;
import java.sql.*;
import java.text.*;

import ca.uhn.hl7v2.*;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v23.message.ADT_A40;


/**
 * Trida podporujici zpravu A40 - Slouceni pacienta 
 * Obsahuje segmenty - MSH, EVN, PID, MRG, PV1
 * 
 * @author dolezal
 */
public class ADT_A40_DB extends Message_DB implements OutboundMessageDb {
	private Connection con;
	private ADT_A40 msg;
	
	/**
	 * Konstruktor s moznosti zadani konf.souboru
	 * 
	 * @param c     - konexe
	 * @param prop  - konfiguracni soubor properties 
	 */
	public ADT_A40_DB(Connection c, Properties prop) {
		super(prop);
		this.con = c;
	}

	/** 
	 * Metoda vraci ovladac na zpracovanou zpravu
     *
     * @return msg - objekt Message    
   	 */
	public Message getMsg() {
		return this.msg;
	}
	
	/** 
	 * Metoda vytvarejici zpravu A40 z tabulky al7_kom. 
	 * Zpracovava se pouze jeden radek tabulky.
	 * 
	 * @param id - sloupec pk z tabulky al7_kom
	 * @throws SQLException 
     * @throws HL7Exception 
  	 */
	public void loadFromDb(int id) throws SQLException, HL7Exception {
		
		msg = new ADT_A40();
		
		
		String queryKom = "select pk, cas_zad_v, ic_pac, rod_cis, prijmeni, jm, titul, dat_naroz, " +
                          "sex, adr_ulice, adr_obec, adr_psc, stat_prisl, telef, cis_pojist, " +  
                          "ic_dokladu, dat_prij, cas_prij, typ_pojist, zpoj_kod, zad_prac_typ, zad_prio, " +
                          "zad_prac_zkr, zad_prac_zkr_z, pokoj, luzko, pokoj_z, luzko_z, " +
						  "rod_cis_z, ic_pac_z, prijmeni_z, jm_z " +
		                  "from al7_kom where pk = " + id;
 
        
            Statement stmt = con.createStatement();
            try {
                 ResultSet rsKom = stmt.executeQuery(queryKom);
                 try {
                      while (rsKom.next()) {
                      	
                        /*
                         * Formaty data
                         */
                 
                         SimpleDateFormat fmtTs = new SimpleDateFormat("yyyyMMddHHmm"); 
                         SimpleDateFormat fmtDt = new SimpleDateFormat("yyyyMMdd"); 
                         SimpleDateFormat fmtTm = new SimpleDateFormat("HHmm"); 

                        /* 
                         * Segment MSH 
                         */
                         
                         msg.getMSH().getFieldSeparator().setValue(prop.getProperty("field_sep")); 
                         msg.getMSH().getEncodingCharacters().setValue(prop.getProperty("encode_char"));
                         msg.getMSH().getSendingApplication().getNamespaceID().setValue(prop.getProperty("send_app_name"));
                         msg.getMSH().getSendingFacility().getNamespaceID().setValue(prop.getProperty("send_fac_name"));
                         msg.getMSH().getSendingFacility().getUniversalID().setValue(prop.getProperty("send_fac_id"));
                         msg.getMSH().getSendingFacility().getUniversalIDType().setValue(prop.getProperty("send_fac_id_type"));
                         msg.getMSH().getReceivingApplication().getNamespaceID().setValue(prop.getProperty("recive_app_name"));
                         msg.getMSH().getReceivingFacility().getNamespaceID().setValue(prop.getProperty("recive_fac_name"));
                         
                         String casZad;
                         if (rsKom.getTimestamp("cas_zad_v")!= null)
                         	casZad = fmtTs.format(rsKom.getTimestamp("cas_zad_v"));
                         else
                         	casZad = "";
                         msg.getMSH().getDateTimeOfMessage().setValue(casZad);
                         
                         msg.getMSH().getMessageType().getMessageType().setValue("ADT");
                         msg.getMSH().getMessageType().getTriggerEvent().setValue("A40");
                         msg.getMSH().getMessageControlID().setValue(getStr(rsKom,"pk"));
                         msg.getMSH().getProcessingID().getProcessingID().setValue(prop.getProperty("proc_id"));
                         msg.getMSH().getVersionID().setValue(prop.getProperty("ver_id"));
                         msg.getMSH().getCharacterSet().setValue(prop.getProperty("char_set"));
  

                        /*
                         * Segment EVN
                         */
                         
                         msg.getEVN().getEventTypeCode().setValue("A40");
                         msg.getEVN().getRecordedDateTime().setValue(casZad);
                        

                        /*
                         * Segment PID
                         */ 
                         
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getSetIDPatientID().setValue("1");
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getPatientIDExternalID().getID().setValue(getStr(rsKom,"ic_pac"));
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getPatientIDExternalID().getAssigningAuthority().getNamespaceID().setValue(prop.getProperty("send_fac_name"));

                         String p_ic_pac =  getStr(rsKom,"ic_pac");
                         String p_rod_cis = getRC(rsKom,"rod_cis");
                         setPIDInternalIDs(msg.getADT_A40_PIDPD1MRGPV1().getPID(),p_ic_pac,p_rod_cis);
                         
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getPatientName().getFamilyName().setValue(getStr(rsKom,"prijmeni"));    
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getPatientName().getGivenName().setValue(getStr(rsKom,"jm")); 
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getPatientName().getDegreeEgMD().setValue(getStr(rsKom,"titul")); 
                         
                         String R1;
                         if (rsKom.getDate("dat_naroz") != null)
                         	R1 = fmtTs.format(rsKom.getDate("dat_naroz"));
                         else
                         	R1 = "";
                          msg.getADT_A40_PIDPD1MRGPV1().getPID().getDateOfBirth().setValue(R1.trim());  
              
                         String  pSex = getStr(rsKom,"sex");
                         if (pSex.equals("M")) {
                         	msg.getADT_A40_PIDPD1MRGPV1().getPID().getSex().setValue("M");
                         } else if (pSex.equals("Z")) {
                         	msg.getADT_A40_PIDPD1MRGPV1().getPID().getSex().setValue("F");
                         } else {
                         	msg.getADT_A40_PIDPD1MRGPV1().getPID().getSex().setValue("O");
                         }

                         String[] trp1 = getStr(rsKom,"telef").trim().split(" ");
                         String trp2 = "";
                         for (int i = 0; i < trp1.length; i++) {
                         	trp2.concat(trp1[i]);
                         }                      
                         
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getPatientAddress(0).getStreetAddress().setValue(getStr(rsKom,"adr_ulice"));
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getPatientAddress(0).getCity().setValue(getStr(rsKom,"adr_obec"));
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getPatientAddress(0).getZipOrPostalCode().setValue(getStr(rsKom,"adr_psc"));
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getPatientAddress(0).getCountry().setValue(getStr(rsKom,"stat_prisl"));
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getPhoneNumberHome(0).setValue(trp2);
                         msg.getADT_A40_PIDPD1MRGPV1().getPID().getSSNNumberPatient().setValue(getStr(rsKom,"cis_pojist"));
                         
                        /*
                         * Segment MRG
                         */

                         String p_ic_pac_z =  getStr(rsKom,"ic_pac_z");
                         String p_rod_cis_z = getRC(rsKom,"rod_cis_z");
                         setMRGInternalIDs(msg.getADT_A40_PIDPD1MRGPV1().getMRG(),p_ic_pac_z,p_rod_cis_z);
    					  
                         msg.getADT_A40_PIDPD1MRGPV1().getMRG().getPriorPatientIDExternal().getID().setValue(p_ic_pac_z);
 					     msg.getADT_A40_PIDPD1MRGPV1().getMRG().getPriorPatientIDExternal().getAssigningAuthority().getNamespaceID().setValue(prop.getProperty("send_fac_name"));
 					                           
 					     msg.getADT_A40_PIDPD1MRGPV1().getMRG().getPriorPatientName().getID().setValue(getStr(rsKom,"prijmeni_z"));
                         msg.getADT_A40_PIDPD1MRGPV1().getMRG().getPriorPatientName().getCheckDigit().setValue(getStr(rsKom,"jm_z"));
                                                 
                        /*
                         * Segment PV1
                         */

                         msg.getADT_A40_PIDPD1MRGPV1().getPV1().getSetIDPatientVisit().setValue("1");
                         msg.getADT_A40_PIDPD1MRGPV1().getPV1().getPatientClass().setValue(getStr(rsKom,"zad_prac_typ"));                
                         msg.getADT_A40_PIDPD1MRGPV1().getPV1().getAdmissionType().setValue("R");               
                         msg.getADT_A40_PIDPD1MRGPV1().getPV1().getVisitNumber().getID().setValue(getStr(rsKom,"ic_dokladu"));

                         String S3 = "";
                         String S1;
                         String S2;
                         if (rsKom.getDate("cas_prij") != null)
                         	S2 = fmtTm.format(rsKom.getDate("cas_prij"));
                         else
                         	S2 = "0000";
                         if (rsKom.getDate("dat_prij") != null) {
                         	S1 = fmtDt.format(rsKom.getDate("dat_prij"));
                             S3 = S1 + S2;
                         }
                         
                         msg.getADT_A40_PIDPD1MRGPV1().getPV1().getAdmitDateTime().setValue(S3);
                                                            
                     }
                } finally {
                    rsKom.close();
                }
            } finally {
                stmt.close();
           }
	}
}

		