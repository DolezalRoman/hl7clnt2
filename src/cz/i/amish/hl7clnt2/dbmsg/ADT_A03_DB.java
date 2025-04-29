/*
 * ADT_A01- Propusteni pacienta (stacionarni) 
 *
 * $Id: ADT_A03_DB.java,v 1.2 2017/12/04 12:39:06 raska Exp $
 * $Log: ADT_A03_DB.java,v $
 * Revision 1.2  2017/12/04 12:39:06  raska
 * ver 2.7.2; klient podporuje upravu rodneho cisla ve zprave (lomitko, bez lomitka, hodnota z databaze)
 *
 * Revision 1.1  2005/10/09 16:41:18  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.7  2005/08/25 08:42:04  raska
 * Doplnena moznost vytvaret dynamicky seznam identifikatoru pacienta (PID-3 a MRG-1) dle nastaveni namespace v property.
 *
 * Revision 1.6  2005/07/20 10:26:44  raska
 * Opraveny chyby v SELECT prikazech, chybely carky za nekterymi sloupci.
 *
 * Revision 1.5  2005/07/11 16:19:28  raska
 * Drobne upravy v souvislosti s prechodem na Eclipse3.1.
 *
 * Revision 1.4  2005/06/16 07:39:47  dolezal
 * Oprava puvodniho umistrni v segmentu PV1.
 *
 * Revision 1.3  2005/06/09 09:39:23  dolezal
 * Uprava segmentu PV1 - nynejsi a puvodni umisteni pacienta
 *
 * Revision 1.2  2005/06/08 12:36:07  raska
 * Do segmentu PV1 doplneno nacitani informaci o aktualnim a predchozim pracovisti, na kterem paciant lezi.
 *
 * Revision 1.1  2005/05/16 12:36:18  raska
 * Zalozeni projektu hl7clnt2 v repository.
 *
 */

package cz.i.amish.hl7clnt2.dbmsg;

import java.util.*;
import java.sql.*;
import java.text.*;

import ca.uhn.hl7v2.*;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v23.message.ADT_A03;


/**
 * Trida podporujici zpravu A03 - Propusteni pacienta (stacionarni)
 * Obsahuje segmenty - MSH, EVN, PID, PV1
 * 
 * @author dolezal
 */
public class ADT_A03_DB extends Message_DB implements OutboundMessageDb {
	private Connection con;
	private ADT_A03 msg;
	
	/**
	 * Konstruktor s moznosti zadani konf.souboru
	 * 
	 * @param c     - konexe
	 * @param prop  - konfiguracni soubor properties 
	 */
	public ADT_A03_DB(Connection c, Properties prop) {
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
	 * Metoda vytvarejici zpravu A03 z tabulky al7_kom. 
	 * Zpracovava se pouze jeden radek tabulky.
	 * 
	 * @param id - sloupec pk z tabulky al7_kom
	 * @throws SQLException 
     * @throws HL7Exception 
	 */
	public void loadFromDb(int id) throws SQLException, HL7Exception {
		
		msg = new ADT_A03();
		
        String queryKom = "select pk, cas_zad_v, ic_pac, rod_cis, prijmeni, jm, titul, dat_naroz, " +
                          "sex, adr_ulice, adr_obec, adr_psc, stat_prisl, telef, cis_pojist, " +  
                          "ic_dokladu, dat_prij, cas_prij, typ_pojist, zpoj_kod, zad_prac_typ, zad_prio, " +
                          "zad_prac_zkr, zad_prac_zkr_z, pokoj, luzko, pokoj_z, luzko_z, " +
		                  "dat_prop, cas_prop " +
						  "from al7_kom where pk = " + id;
 
        // select cas_zad_v from al7_kom                                 Trpova fligna pro datetime
        //  where extend(cas_zad_v, year to month) = mdy(11,1,2003)

        
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
                        msg.getMSH().getMessageType().getTriggerEvent().setValue("A03");
                        msg.getMSH().getMessageControlID().setValue(getStr(rsKom,"pk"));
                        msg.getMSH().getProcessingID().getProcessingID().setValue(prop.getProperty("proc_id"));
                        msg.getMSH().getVersionID().setValue(prop.getProperty("ver_id"));
                        msg.getMSH().getCharacterSet().setValue(prop.getProperty("char_set"));
 
 
                       /*
                        * Segment EVN
                        */
                        msg.getEVN().getEventTypeCode().setValue("A03");
                        msg.getEVN().getRecordedDateTime().setValue(casZad);
                       

                       /*
                        * Segment PID
                        */ 
                        
                        msg.getPID().getSetIDPatientID().setValue("1");
                        msg.getPID().getPatientIDExternalID().getID().setValue(getStr(rsKom,"ic_pac"));
                        msg.getPID().getPatientIDExternalID().getAssigningAuthority().getNamespaceID().setValue(prop.getProperty("send_fac_name"));
                        
 
                        String p_ic_pac =  getStr(rsKom,"ic_pac");
                        String p_rod_cis = getRC(rsKom,"rod_cis");
                        setPIDInternalIDs(msg.getPID(),p_ic_pac,p_rod_cis);
                     
                        msg.getPID().getPatientName().getFamilyName().setValue(getStr(rsKom,"prijmeni"));    
                        msg.getPID().getPatientName().getGivenName().setValue(getStr(rsKom,"jm")); 
                        msg.getPID().getPatientName().getDegreeEgMD().setValue(getStr(rsKom,"titul")); 
                        
                        String R1;
                        if (rsKom.getDate("dat_naroz") != null)
                        	R1 = fmtTs.format(rsKom.getDate("dat_naroz"));
                        else
                        	R1 = "";
                         msg.getPID().getDateOfBirth().setValue(R1.trim());  
             
                        String  pSex = getStr(rsKom,"sex");
                        if (pSex.equals("M")) {
                        	msg.getPID().getSex().setValue("M");
                        } else if (pSex.equals("Z")) {
                        	msg.getPID().getSex().setValue("F");
                        } else {
                        	msg.getPID().getSex().setValue("O");
                        }

                        String[] trp1 = getStr(rsKom,"telef").trim().split(" ");
                        String trp2 = "";
                        for (int i = 0; i < trp1.length; i++) {
                        	trp2.concat(trp1[i]);
                        }                      
                        
                        msg.getPID().getPatientAddress(0).getStreetAddress().setValue(getStr(rsKom,"adr_ulice"));
                        msg.getPID().getPatientAddress(0).getCity().setValue(getStr(rsKom,"adr_obec"));
                        msg.getPID().getPatientAddress(0).getZipOrPostalCode().setValue(getStr(rsKom,"adr_psc"));
                        msg.getPID().getPatientAddress(0).getCountry().setValue(getStr(rsKom,"stat_prisl"));
                        msg.getPID().getPhoneNumberHome(0).setValue(trp2);
                        msg.getPID().getSSNNumberPatient().setValue(getStr(rsKom,"cis_pojist"));
                        
                       
                        
                        
                       /*
                        * Segment PV1
                        */
                        String orgNSpace = prop.getProperty("org_struct_namespace","AMISHORG");
                        
                        String zadPracZkrZ = getStr(rsKom,"zad_prac_zkr_z");
                        
                        msg.getPV1().getSetIDPatientVisit().setValue("1");
                        msg.getPV1().getPatientClass().setValue(getStr(rsKom,"zad_prac_typ"));                
                        msg.getPV1().getAssignedPatientLocation().getPointOfCare().setValue(getStr(rsKom,"zad_prac_zkr"));
                        msg.getPV1().getAssignedPatientLocation().getRoom().setValue(getStr(rsKom,"pokoj"));
                        msg.getPV1().getAssignedPatientLocation().getBed().setValue(getStr(rsKom,"luzko"));
                        msg.getPV1().getAssignedPatientLocation().getFacility().getNamespaceID().setValue(orgNSpace);
                        msg.getPV1().getAdmissionType().setValue("R");               

                        if (zadPracZkrZ.equals("")) {}
                        else {
                         	msg.getPV1().getPriorPatientLocation().getPointOfCare().setValue(getStr(rsKom,"zad_prac_zkr_z"));
                            msg.getPV1().getPriorPatientLocation().getRoom().setValue(getStr(rsKom,"pokoj_z"));
                            msg.getPV1().getPriorPatientLocation().getBed().setValue(getStr(rsKom,"luzko_z"));
                            msg.getPV1().getPriorPatientLocation().getFacility().getNamespaceID().setValue(orgNSpace);
                         }
                        
                        msg.getPV1().getVisitNumber().getID().setValue(getStr(rsKom,"ic_dokladu"));

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
                        
                        msg.getPV1().getAdmitDateTime().setValue(S3);
  

                        String S31 = "";
                        String S11;
                        String S21;
                        if (rsKom.getDate("cas_prop") != null)
                        	S21 = fmtTm.format(rsKom.getDate("cas_prop"));
                        else
                        	S21 = "0000";
                        if (rsKom.getDate("dat_prop") != null) {
                        	S11 = fmtDt.format(rsKom.getDate("dat_prop"));
                            S31 = S11 + S21;
                        }
                        msg.getPV1().getDischargeDateTime().setValue(S31);
                           
                     // Terser t = new Terser(msg);
                     // t.set("MSH-1", rsMsh.getString("msh_1"));
                     // t.set("MSH-2", rsMsh.getString("msh_2"));
                     // t.set("MSH-3", rsMsh.getString("msh_3"));
                     // t.set("MSH-5", rsMsh.getString("msh_5"));
                     //	t.set("MSH-6", rsMsh.getString("msh_6"));
                     //	t.set("MSH-7", rsMsh.getString("msh_7"));
                     //	t.set("MSH-9", rsMsh.getString("msh_9"));
                     //	t.set("MSH-10", rsMsh.getString("msh_10"));
                     //	t.set("MSH-11", rsMsh.getString("msh_11"));
                     //	t.set("MSH-12", rsMsh.getString("msh_12"));
                     //	t.set("MSH-18", rsMsh.getString("msh_18"));

                     }
                } finally {
                    rsKom.close();
                }
            } finally {
                stmt.close();
           }
            
     }
}
