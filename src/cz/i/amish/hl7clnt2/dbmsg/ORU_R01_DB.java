/*
 * ORU_R01 - Nevyzadane predani vysledku vysetreni
 * 
 * $Id: ORU_R01_DB.java,v 1.5 2019/10/03 06:52:21 dolezal Exp $
 * $Log: ORU_R01_DB.java,v $
 * Revision 1.5  2019/10/03 06:52:21  dolezal
 * Oprava podepisovani popisu v segmentu OBX
 *
 * Revision 1.4  2019/09/26 09:41:43  dolezal
 * Popisy opatreny datem a podpisem ,
 *
 * Revision 1.3  2017/12/04 12:39:06  raska
 * ver 2.7.2; klient podporuje upravu rodneho cisla ve zprave (lomitko, bez lomitka, hodnota z databaze)
 *
 * Revision 1.2  2009/03/13 15:52:40  raska
 * Opraveno nacitani sloupce dat_vstr.
 *
 * Revision 1.1  2005/10/09 16:41:19  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.6  2005/08/25 08:42:05  raska
 * Doplnena moznost vytvaret dynamicky seznam identifikatoru pacienta (PID-3 a MRG-1) dle nastaveni namespace v property.
 *
 * Revision 1.5  2005/07/20 10:26:46  raska
 * Opraveny chyby v SELECT prikazech, chybely carky za nekterymi sloupci.
 *
 * Revision 1.4  2005/07/11 16:19:44  raska
 * Drobne upravy v souvislosti s prechodem na Eclipse3.1.
 *
 * Revision 1.3  2005/06/08 12:36:07  raska
 * Do segmentu PV1 doplneno nacitani informaci o aktualnim a predchozim pracovisti, na kterem paciant lezi.
 *
 * Revision 1.2  2005/05/20 13:17:04  raska
 * Procisteni verze od vsech nanosu FNBR.
 *
 * Revision 1.1  2005/05/16 12:36:20  raska
 * Zalozeni projektu hl7clnt2 v repository.
 *
 */

package cz.i.amish.hl7clnt2.dbmsg;

import java.util.*;
import java.sql.*;
import java.text.*;

import ca.uhn.hl7v2.*;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v23.datatype.TX;
import ca.uhn.hl7v2.model.v23.message.ORU_R01;


/**
 * Trida podporujici zpravu R01 - Nevyzadane predani vysledku vysetreni
 * Obsahuje segmenty - MSH, PID, PV1, OBR, ORC
 * 
 * @author dolezal
 */
public class ORU_R01_DB extends Message_DB implements OutboundMessageDb {
	private Connection con;
	private ORU_R01 msg;
	
	/**
	 * Konstruktor s moznosti zadani konf.souboru
	 * 
	 * @param c     - konexe
	 * @param prop  - konfiguracni soubor properties 
	 */
	public ORU_R01_DB(Connection c, Properties prop) {
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
	 * Metoda vytvarejici zpravu R01 z tabulky al7_kom, al7_kom_d a al7_kom_b. 
	 * Tabulky svazany vazbou 1:N sloupci pk_ol7_kom na tabulku al7_kom.
	 * Zpracovana pouze jedna udalost z tabulky al7_kom podle spouce pk. 
	 * 
	 * @param id - sloupec pk z tabulky al7_kom
	 * @throws SQLException 
     * @throws HL7Exception 
 	 */	
	public void loadFromDb(int id)throws SQLException, HL7Exception {
		
	    msg = new ORU_R01();
        String sumDg = "";
        String S6 = "";
        String zadPrio = "";
        String zadCis = " ";
        String pracZkr = "";
        String pracNaz = "";
        String obIdent = "";
        String ud  = "";        
        String p1 = "";
//        String p2 = "";
                
       /*
        * Formaty data
        */
                
        SimpleDateFormat fmtTs = new SimpleDateFormat("yyyyMMddHHmm"); 
        SimpleDateFormat fmtDt = new SimpleDateFormat("yyyyMMdd"); 
        SimpleDateFormat fmtTm = new SimpleDateFormat("HHmm"); 
                        
                                
            Statement stmt = con.createStatement();
            try {
            
            	String queryKomUd = "select udalost " +
                                    "from al7_kom where pk = " + id; 
                ResultSet rsKomUd = stmt.executeQuery(queryKomUd);
                try {
                    while (rsKomUd.next()) { 
            	        ud = getStr(rsKomUd,"udalost");
                    }    
                } finally {
                    rsKomUd.close();
                }                
                
                String queryCfgUd = "select p1, p2 " +
                                    "from al7_cfg_ud where udalost = '" + ud + "'";               

                ResultSet rsCfgUd = stmt.executeQuery(queryCfgUd);
                try {
                    while (rsCfgUd.next()) {
		                p1 = getStr(rsCfgUd,"p1");
//		                p2 = getStr(rsCfgUd,"p2");
                  	}
                 } finally {
                       rsCfgUd.close();
                   }
                 

                 String queryKom = "select pk, cas_zad_v, ic_pac, rod_cis, prijmeni, jm, titul, dat_naroz, " +
                                   "sex, adr_ulice, adr_obec, adr_psc, stat_prisl, telef, cis_pojist, " +  
                                   "ic_dokladu, dat_prij, cas_prij, typ_pojist, zpoj_kod, alergie, zad_prac_typ,  " +
			                       "zad_cis, zad_dat_p, zad_cas_p, zad_prio, zad_dat, zad_cas, " +
                                   "zad_uziv, zad_prac_naz, zad_icp, zad_vs, zad_odb, " +
			                       "dg_kod1, dg_kod2, dg_kod3, dg_kod4, dg_kod5,  " +
			                       "zad_prac_zkr, zad_prac_zkr_z, pokoj, luzko, pokoj_z, luzko_z " +
	                               "from al7_kom where pk = " + id;
 
                 ResultSet rsKom = stmt.executeQuery(queryKom);
                 try {
                      while (rsKom.next()) {
                      	
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
			
                        msg.getMSH().getMessageType().getMessageType().setValue("ORU");
                        msg.getMSH().getMessageType().getTriggerEvent().setValue("R01");
                        msg.getMSH().getMessageControlID().setValue(getStr(rsKom,"pk"));
                        msg.getMSH().getProcessingID().getProcessingID().setValue(prop.getProperty("proc_id"));
                        msg.getMSH().getVersionID().setValue(prop.getProperty("ver_id"));
                        msg.getMSH().getCharacterSet().setValue(prop.getProperty("char_set"));
 

                       /*
                        * Segment PID
                        */ 
                        
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getSetIDPatientID().setValue("1");
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getPatientIDExternalID().getID().setValue(getStr(rsKom,"ic_pac"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getPatientIDExternalID().getAssigningAuthority().getNamespaceID().setValue(prop.getProperty("send_fac_name"));
                      

                        String p_ic_pac =  getStr(rsKom,"ic_pac");
                        String p_rod_cis = getRC(rsKom,"rod_cis");
                        setPIDInternalIDs(msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID(),p_ic_pac,p_rod_cis);

                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getPatientName().getFamilyName().setValue(getStr(rsKom,"prijmeni"));    
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getPatientName().getGivenName().setValue(getStr(rsKom,"jm")); 
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getPatientName().getDegreeEgMD().setValue(getStr(rsKom,"titul")); 

                        String R1;
                        if (rsKom.getDate("dat_naroz") != null)
                        	R1 = fmtTs.format(rsKom.getDate("dat_naroz"));
                            //GregorianCalendar gc = new GregorianCalendar();
                        else
                        	R1 = "";
                        
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getDateOfBirth().setValue(R1.trim());  
             
                        String  pSex = getStr(rsKom,"sex");
                        if (pSex.equals("M")) {
                        	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getSex().setValue("M");
                        } else if (pSex.equals("Z")) {
                        	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getSex().setValue("F");
                        } else {
                        	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getSex().setValue("O");
                        }
                        String[] trp1 = getStr(rsKom,"telef").trim().split(" ");
                        String trp2 = "";
                        for (int i = 0; i < trp1.length; i++) {
                        	trp2.concat(trp1[i]);
                        }                    
                        
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getPatientAddress(0).getStreetAddress().setValue(getStr(rsKom,"adr_ulice"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getPatientAddress(0).getCity().setValue(getStr(rsKom,"adr_obec"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getPatientAddress(0).getZipOrPostalCode().setValue(getStr(rsKom,"adr_psc"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getPatientAddress(0).getCountry().setValue(getStr(rsKom,"stat_prisl"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getPhoneNumberHome(0).setValue(trp2);
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getPID().getSSNNumberPatient().setValue(getStr(rsKom,"cis_pojist"));
                        
                       
                       /*
                        * Segment PV1
                        */
 
                        String orgNSpace = prop.getProperty("org_struct_namespace","AMISHORG");
                        
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getSetIDPatientVisit().setValue("1");
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getPatientClass().setValue(getStr(rsKom,"zad_prac_typ"));                
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getAssignedPatientLocation().getPointOfCare().setValue(getStr(rsKom,"zad_prac_zkr"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getAssignedPatientLocation().getRoom().setValue(getStr(rsKom,"pokoj"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getAssignedPatientLocation().getBed().setValue(getStr(rsKom,"luzko"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getAssignedPatientLocation().getFacility().getNamespaceID().setValue(orgNSpace);
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getAdmissionType().setValue("R");               
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getPriorPatientLocation().getPointOfCare().setValue(getStr(rsKom,"zad_prac_zkr_z"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getPriorPatientLocation().getRoom().setValue(getStr(rsKom,"pokoj_z"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getPriorPatientLocation().getBed().setValue(getStr(rsKom,"luzko_z"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getPriorPatientLocation().getFacility().getNamespaceID().setValue(orgNSpace);
                        
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getVisitNumber().getID().setValue(getStr(rsKom,"ic_dokladu"));

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
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI().getORU_R01_PIDPD1NTEPV1PV2().getORU_R01_PV1PV2().getPV1().getAdmitDateTime().setValue(S3);
 
 
                       /*
                        * Priprava segmentu OBR
                        */                                   
 
                        String S4;
                        String S5;
                        if (rsKom.getDate("zad_cas_p") != null)
                        	S5 = fmtTm.format(rsKom.getDate("zad_cas_p"));
                        else
                        	S5 = "0000";
                        if (rsKom.getDate("zad_dat_p") != null) {
                        	S4 = fmtDt.format(rsKom.getDate("zad_dat_p"));
                            S6 = S4 + S5;
                        }
                        zadPrio = getStr(rsKom,"zad_prio");
                        zadCis  = getStr(rsKom,"zad_cis");
                        pracZkr = getStr(rsKom,"zad_prac_zkr");
                        pracNaz = getStr(rsKom,"zad_prac_naz");                        
           
                                             
                        String kodDg1 = getStr(rsKom,"dg_kod1");
                        String kodDg2 = getStr(rsKom,"dg_kod2");
                        String kodDg3 = getStr(rsKom,"dg_kod3");
                        String kodDg4 = getStr(rsKom,"dg_kod4");
                        String kodDg5 = getStr(rsKom,"dg_kod5");
                        String dgSep = prop.getProperty("diagnoze_sep"); 
                        String dgBeginChar = prop.getProperty("diagnoze_begin_char"); 
                        String dgEndChar   = prop.getProperty("diagnoze_end_char"); 
                        if (dgSep == null) {
                        	dgSep = " ";
                        }
                       
                        sumDg = kodDg1;
                        if (kodDg2!=null && !kodDg2.equals("")) {
                        	sumDg = sumDg + dgSep + kodDg2;
                        }	
                        if (kodDg3!=null && !kodDg3.equals("")) {
                        	sumDg = sumDg + dgSep + kodDg3;
                        }
                        if (kodDg4!=null && !kodDg4.equals("")) {
                        	sumDg = sumDg + dgSep + kodDg4;
                        }
                        if (kodDg5!=null && !kodDg5.equals("")) {
                        	sumDg = sumDg + dgSep + kodDg5;
                        }
                                                             
                        if (dgBeginChar != null) {
                        	sumDg = dgBeginChar + sumDg;
                        }
                        if (dgEndChar != null) {
                        	sumDg = sumDg +  dgEndChar;
                        }
                                         }
                } finally {
                      rsKom.close();
                  }

                String queryKomD = "select zad_cis, zkr_vstr, naz_vstr, ic_vstr, modalita, dat_vstr " +
                                   "from al7_kom_d where pk_al7_kom = " + id;               
                
                ResultSet rsKomD = stmt.executeQuery(queryKomD);
                int j = 0;
                int m = 0; 
                try {
                        while (rsKomD.next()) {
                       	
                       	m = j + 1;
                       	/*
                       	 * Segment OBR
                       	 */
                       	
                       	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getSetIDObservationRequest().setValue(Integer.toString(m));
                       	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getPlacerOrderNumber(0).getEntityIdentifier().setValue(zadCis);
                       	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getPlacerOrderNumber(0).getNamespaceID().setValue(prop.getProperty("send_fac_name"));

                        String pZkrVstr = getStr(rsKomD,"zkr_vstr");
                        String pZadCis =  getStr(rsKomD,"zad_cis");
                        String pIcVstr =  getStr(rsKomD,"ic_vstr");
                        String orderNumber = pZadCis;
                        String serviceId = pZkrVstr;
                        /* Priprava OBX */
                        obIdent = pZkrVstr + "@" + pIcVstr;
                     
                        String orderSep = prop.getProperty("order_number_sep");
                        if (orderSep!= null) {                     
                      	    orderNumber = pZkrVstr + orderSep + pZadCis;      
                            serviceId   = pZkrVstr + orderSep + pZadCis; 
                            obIdent     = pZkrVstr + orderSep + pIcVstr;
                        } else {
                      	    orderNumber = pZadCis;      
                            serviceId   = pZkrVstr; 
                            obIdent     = pIcVstr;
                        }    
 	
                       	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getFillerOrderNumber().getEntityIdentifier().setValue(orderNumber);
                       
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getUniversalServiceIdentifier().getIdentifier().setValue(serviceId);
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getUniversalServiceIdentifier().getText().setValue(getStr(rsKomD,"naz_vstr"));
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getUniversalServiceIdentifier().getNameOfCodingSystem().setValue(prop.getProperty("send_fac_name"));
 
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getRelevantClinicalInformation().setValue(sumDg.trim());

                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getOrderingProvider(0).getIDNumber().setValue(pracZkr);
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getOrderingProvider(0).getFamilyName().setValue(pracNaz);
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getOrderingProvider(0).getAssigningAuthority().getNamespaceID().setValue(prop.getProperty("send_fac_name"));

                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getPlacerField1().setValue(getStr(rsKomD,"modalita"));
                        
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getQuantityTiming().getStartDateTime().setValue(S6);
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getQuantityTiming().getPriority().setValue(zadPrio);
                        
                        String S11;
                        if (rsKomD.getDate("dat_vstr") != null)
                        	S11 = fmtTs.format(rsKomD.getDate("dat_vstr"));
                        else
                        	S11 = "";                
                        
                        msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(j).getORU_R01_ORCOBRNTEOBXNTECTI().getOBR().getScheduledDateTime().setValue(S11);
    
                        j = j + 1;
                       }
                } 
                finally {
                        rsKomD.close();
                }
                    
 
                String queryKomB = "select pk, text, popsal, dat_pop, cas_pop " +
                                   "from al7_kom_b where pk_al7_kom = " + id;               
                
                ResultSet rsKomB = stmt.executeQuery(queryKomB);
                
                int k = 0;
                int l = 0;
                String S15 = "";
                String S16 = "";
    			
                try {
                       while (rsKomB.next()) {
                       	
                       	/*
                       	 * Segment OBX
                       	 */
                       	l = k + 1;
                       	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(k).getORU_R01_ORCOBRNTEOBXNTECTI().getORU_R01_OBXNTE().getOBX().getSetIDOBX().setValue(Integer.toString(l));
                       	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(k).getORU_R01_ORCOBRNTEOBXNTECTI().getORU_R01_OBXNTE().getOBX().getValueType().setValue("TX");
                       	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(k).getORU_R01_ORCOBRNTEOBXNTECTI().getORU_R01_OBXNTE().getOBX().getObservationIdentifier().getIdentifier().setValue(obIdent);

                     	S15 = getTxt(rsKomB,"text")+ "\n\n  Podpis: " + rsKomB.getDate("dat_pop") +  " " + rsKomB.getTime("cas_pop");
                        S16 = getStr(rsKomB,"popsal");

                    // 	TX txtType = new TX();
                    // 	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(k).getORU_R01_ORCOBRNTEOBXNTECTI().getORU_R01_OBXNTE().getOBX().getObservationValue(0).setData(txtType);  
                    //    txtType.setValue(S15 + " " + S16);
   
                    //	txtType.setValue(S15);
                    // 	txtType.setValue(getTxt(rsKomB,"text")+ "\n\n  Podpis: " + rsKomB.getDate("dat_pop") +  " " + rsKomB.getTime("cas_pop")+ " "  + getStr(rsKomB,"popsal") );
                    // 	txtType.setValue(getTxt(rsKomB,"text"));
                    // 	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(k).getORU_R01_ORCOBRNTEOBXNTECTI().getORU_R01_OBXNTE().getOBX().getObservationValue(0).setData(rsKomB.getString("text"));                       	

                     	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(k).getORU_R01_ORCOBRNTEOBXNTECTI().getORU_R01_OBXNTE().getOBX().getObservResultStatus().setValue(p1);
                     	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(k).getORU_R01_ORCOBRNTEOBXNTECTI().getORU_R01_OBXNTE().getOBX().getResponsibleObserver().getIDNumber().setValue(getStr(rsKomB,"popsal"));
                    //  S16 = getStr(rsKomB,"popsal");
                    // 	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(k).getORU_R01_ORCOBRNTEOBXNTECTI().getORU_R01_OBXNTE().getOBX().getResponsibleObserver().getIDNumber().setValue(S16);
                                           
                       	k = k + 1;
                       }
                  } 
                  finally {
					rsKomB.close();
				}
	
				String queryPamZamId = "select tjp " + "from pam_zam_id where uziv = "
						+ "\"" + S16 + "\"";
	
				ResultSet rsPamZamId = stmt.executeQuery(queryPamZamId);

				String S17 = "";
				
	            try {
	            	
					while (rsPamZamId.next()) {
												
						S17 = getStr(rsPamZamId,"tjp");
                      	TX txtType = new TX();
                      	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(k-1).getORU_R01_ORCOBRNTEOBXNTECTI().getORU_R01_OBXNTE().getOBX().getObservationValue(0).setData(txtType);  
                        txtType.setValue(S15 + " " + S17);
                     	msg.getORU_R01_PIDPD1NTEPV1PV2ORCOBRNTEOBXNTECTI(k-1).getORU_R01_ORCOBRNTEOBXNTECTI().getORU_R01_OBXNTE().getOBX().getResponsibleObserver().getIDNumber().setValue(S16);

					}
				} finally {
					rsPamZamId.close();
				}
      
              } 
              finally {
                    stmt.close();
              }                        		   
          }

}

