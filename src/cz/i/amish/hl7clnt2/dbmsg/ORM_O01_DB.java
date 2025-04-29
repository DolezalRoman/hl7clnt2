/*
 * ORM_O01 - Obecny pozadavek
 * 
 * $Id: ORM_O01_DB.java,v 1.4 2020/02/26 14:44:03 amis Exp $
 * $Log: ORM_O01_DB.java,v $
 * Revision 1.4  2020/02/26 14:44:03  amis
 * Migrated to OpenJDK 8.
 *
 * Revision 1.3  2017/12/04 12:39:04  raska
 * ver 2.7.2; klient podporuje upravu rodneho cisla ve zprave (lomitko, bez lomitka, hodnota z databaze)
 *
 * Revision 1.2  2017/04/24 13:25:28  raska
 * ver. 2.7.0; spojeni verze od Pavla Navrkala, odstraneny warning
 *
 * Revision 1.1  2005/10/09 16:41:17  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.6  2005/08/25 08:42:04  raska
 * Doplnena moznost vytvaret dynamicky seznam identifikatoru pacienta (PID-3 a MRG-1) dle nastaveni namespace v property.
 *
 * Revision 1.5  2005/07/20 10:26:45  raska
 * Opraveny chyby v SELECT prikazech, chybely carky za nekterymi sloupci.
 *
 * Revision 1.4  2005/07/15 09:23:11  raska
 * Opraveny jmena sloupcu prijmeni_z a jm_z.
 *
 * Revision 1.3  2005/07/11 16:21:09  raska
 * Rozsireni zpravy ORM o segmenty DG1 a OBX, podpora pro prenos diagnoz a vysledku mereni. Odpovidajicim zpusobem rozsireny konfiguracni soubory.
 *
 * Revision 1.2  2005/06/08 12:36:06  raska
 * Do segmentu PV1 doplneno nacitani informaci o aktualnim a predchozim pracovisti, na kterem paciant lezi.
 *
 * Revision 1.1  2005/05/16 12:36:18  raska
 * Zalozeni projektu hl7clnt2 v repository.
 *
 */

package cz.i.amish.hl7clnt2.dbmsg;

import java.util.*;
import java.util.Date;
import java.sql.*;
import java.text.*;

import ca.uhn.hl7v2.*;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v23.datatype.ST;
import ca.uhn.hl7v2.model.v23.message.ORM_O01;

/**
 * Trida podporujici zpravu O01 - Obecny pozadavek
 * Obsahuje segmenty - MSH, PID, PV1, IN1, AL1, ORC, OBR
 *
 * @author dolezal
 */
public class ORM_O01_DB extends Message_DB implements OutboundMessageDb {
	private Connection con;
	private ORM_O01 msg; 
	
	/**
	 * Konstruktor s moznosti zadani konf.souboru
	 * 
	 * @param c     - konexe
	 * @param prop  - konfiguracni soubor properties 
	 */
	public ORM_O01_DB(Connection c, Properties prop) {
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
	 * Metoda vytvarejici zpravu O01 z tabulky al7_kom a al7_kom_d. 
	 * Tabulky svazany vazbou 1:N sloupci pk a pk_ol7_kom. 
	 * 
	 * @param id - sloupec pk z tabulky al7_kom
	 * @throws SQLException 
     * @throws HL7Exception 
 	 */	
	public void loadFromDb(int id)throws SQLException, HL7Exception {
		
		
		
		msg = new ORM_O01();
		List<String> kodDg = null;
		String S6 = "";
		String zadPrio = "";
		String zadCis  = "";
		String pracZkr = "";
		String pracNaz = "";
		String zadDat = "";
		String rizFak = "";
		String ud  = "";
		String p1 = "";
		String p2 = "";
		String S4;
		String S5;
		String S9 = "";
		String S8;
		String S7 = "";
		String S10 = "";
		String zadZadal = "";
		String zadPrijmeniZ = "";
		String zadJmZ = "";
		String zadIcp = "";
		String zadOdb = "";
		String zadVs = "";      
		String zadPracZkrZ = "";
		
		String vyska = "";
		String vaha = "";
		Date dat_naroz = null;
		
		// select cas_zad_v from al7_kom                                 Trpova fligna pro datetime
		//  where extend(cas_zad_v, year to month) = mdy(11,1,2003)
		
		/*
		 * Formaty data
		 */
		
		SimpleDateFormat fmtTs = new SimpleDateFormat("yyyyMMddHHmm"); 
		SimpleDateFormat fmtDt = new SimpleDateFormat("yyyyMMdd"); 
		SimpleDateFormat fmtDtDot = new SimpleDateFormat("dd.MM.yyyy"); 
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
					p2 = getStr(rsCfgUd,"p2");
				}
			} finally {
				rsCfgUd.close();
			}
			
			String queryKom = "select pk, cas_zad_v, ic_pac, rod_cis, prijmeni, jm, titul, dat_naroz, " +
			"sex, vyska, vaha, adr_ulice, adr_obec, adr_psc, stat_prisl, telef, cis_pojist, " +  
			"ic_dokladu, dat_prij, cas_prij, typ_pojist, zpoj_kod, alergie, zad_prac_typ,  " +
			"zad_cis, zad_dat_p, zad_cas_p, zad_prio, zad_dat, zad_cas, " +
			"zad_uziv, zad_prac_naz, zad_icp, zad_vs, zad_odb, " +
			"dg_kod1, dg_kod2, dg_kod3, dg_kod4, dg_kod5, udalost, riz_fak,  " +
			"zad_zadal, prijmeni_z, jm_z, " +
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
					
					msg.getMSH().getMessageType().getMessageType().setValue("ORM");
					msg.getMSH().getMessageType().getTriggerEvent().setValue("O01");
					msg.getMSH().getMessageControlID().setValue(getStr(rsKom,"pk"));
					msg.getMSH().getProcessingID().getProcessingID().setValue(prop.getProperty("proc_id"));
					msg.getMSH().getVersionID().setValue(prop.getProperty("ver_id"));
					msg.getMSH().getCharacterSet().setValue(prop.getProperty("char_set"));
					
					
					/*
					 * Segment PID
					 */ 
					
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getSetIDPatientID().setValue("1");
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getPatientIDExternalID().getID().setValue(getStr(rsKom,"ic_pac"));
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getPatientIDExternalID().getAssigningAuthority().getNamespaceID().setValue(prop.getProperty("send_fac_name"));
					
					
					String p_ic_pac =  getStr(rsKom,"ic_pac");
					String p_rod_cis = getRC(rsKom,"rod_cis");
                    setPIDInternalIDs(msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID(),p_ic_pac,p_rod_cis);
					
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getPatientName().getFamilyName().setValue(getStr(rsKom,"prijmeni"));    
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getPatientName().getGivenName().setValue(getStr(rsKom,"jm")); 
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getPatientName().getDegreeEgMD().setValue(getStr(rsKom,"titul")); 
					
					String R1;
					if (rsKom.getDate("dat_naroz") != null)
						R1 = fmtTs.format(rsKom.getDate("dat_naroz"));
					else
						R1 = "";
					
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getDateOfBirth().setValue(R1.trim());  
					
					String  pSex = getStr(rsKom,"sex");
					if (pSex.equals("M")) {
						msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getSex().setValue("M");
					} else if (pSex.equals("Z")) {
						msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getSex().setValue("F");
					} else {
						msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getSex().setValue("O");
					}
					
					String[] trp1 = getStr(rsKom,"telef").split(" ");
					String trp2 = "";
					for (int i = 0; i < trp1.length; i++) {
						trp2.concat(trp1[i]);
					}                    
					
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getPatientAddress(0).getStreetAddress().setValue(getStr(rsKom,"adr_ulice"));
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getPatientAddress(0).getCity().setValue(getStr(rsKom,"adr_obec"));
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getPatientAddress(0).getZipOrPostalCode().setValue(getStr(rsKom,"adr_psc"));
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getPatientAddress(0).getCountry().setValue(getStr(rsKom,"stat_prisl"));
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getPhoneNumberHome(0).setValue(trp2);
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getPID().getSSNNumberPatient().setValue(getStr(rsKom,"cis_pojist"));
					
					
					/*
					 * Segment PV1
					 */
					
					String orgNSpace = prop.getProperty("org_struct_namespace","AMISHORG");
					
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getSetIDPatientVisit().setValue("1");
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getPatientClass().setValue(getStr(rsKom,"zad_prac_typ"));                
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getAssignedPatientLocation().getPointOfCare().setValue(getStr(rsKom,"zad_prac_zkr"));
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getAssignedPatientLocation().getRoom().setValue(getStr(rsKom,"pokoj"));
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getAssignedPatientLocation().getBed().setValue(getStr(rsKom,"luzko"));
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getAssignedPatientLocation().getFacility().getNamespaceID().setValue(orgNSpace);
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getAdmissionType().setValue("R");               
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getPriorPatientLocation().getPointOfCare().setValue(getStr(rsKom,"zad_prac_zkr_z"));
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getPriorPatientLocation().getRoom().setValue(getStr(rsKom,"pokoj_z"));
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getPriorPatientLocation().getBed().setValue(getStr(rsKom,"luzko_z"));
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getPriorPatientLocation().getFacility().getNamespaceID().setValue(orgNSpace);
					
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getVisitNumber().getID().setValue(getStr(rsKom,"ic_dokladu"));
					
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
					
					
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_PV1PV2().getPV1().getAdmitDateTime().setValue(S3);
					
					
					/*
					 * Segment IN1 dodelat test na null polozku
					 */
					
					
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_IN1IN2IN3().getIN1().getSetIDInsurance().setValue("1");
					
					String p_typ_pojist = getStr(rsKom,"typ_pojist"); 
					if (p_typ_pojist.equals("")) 
						p_typ_pojist = "1";
					
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_IN1IN2IN3().getIN1().getInsurancePlanID().getIdentifier().setValue("");
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_IN1IN2IN3().getIN1().getInsurancePlanID().getText().setValue(p_typ_pojist);
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_IN1IN2IN3().getIN1().getInsurancePlanID().getNameOfCodingSystem().setValue(" ");
					
					msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getORM_O01_IN1IN2IN3().getIN1().getInsuranceCompanyID().getID().setValue(getStr(rsKom,"zpoj_kod"));  
					
					/*
					 * Segment AL1   
					 */ 
					
					if (rsKom.getString("alergie") != null & rsKom.getString("alergie") != " " ) {
						msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getAL1().getSetIDAL1().setValue("1");
						msg.getORM_O01_PIDPD1NTEPV1PV2IN1IN2IN3GT1AL1().getAL1().getAllergyCodeMnemonicDescription().getText().setValue(getStr(rsKom,"alergie"));
					}                       
					
					/*
					 * Segment ORC - priprava
					 */ 
					
					/*                    
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getOrderControl().setValue(p1); 
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getPlacerOrderNumber(0).getEntityIdentifier().setValue(getStr(rsKom,"zad_cis"));
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getPlacerOrderNumber(0).getNamespaceID().setValue(prop.getProperty("send_fac_name"));
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getOrderStatus().setValue(p2);
					 */
					
					if (rsKom.getDate("zad_cas_p") != null)
						S5 = fmtTm.format(rsKom.getDate("zad_cas_p"));
					else
						S5 = "0000";
					if (rsKom.getDate("zad_dat_p") != null) {
						S4 = fmtDt.format(rsKom.getDate("zad_dat_p"));
						S6 = S4 + S5 + "00";
					}
					zadPrio = getStr(rsKom,"zad_prio");
					/*
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getQuantityTiming().getStartDateTime().setValue(S6);
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getQuantityTiming().getPriority().setValue(zadPrio);
					 */
					
					if (rsKom.getDate("zad_cas") != null)
						S8 = fmtTm.format(rsKom.getDate("zad_cas"));
					else
						S8 = "0000";
					if (rsKom.getDate("zad_dat") != null) {
						S7 = fmtDt.format(rsKom.getDate("zad_dat"));
						S9 = S7 + S8;
						S10 = fmtDtDot.format(rsKom.getDate("zad_dat"));
					}                        
					/*                       
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getDateTimeOfTransaction().setValue(S9);
					 */ 
					
					
					zadZadal = getStr(rsKom,"zad_zadal");
					zadPrijmeniZ = getStr(rsKom,"prijmeni_z");
					zadJmZ = getStr(rsKom,"jm_z");
					zadIcp = getStr(rsKom,"zad_icp");
					zadOdb = getStr(rsKom,"zad_odb");
					zadVs = getStr(rsKom,"zad_vs");
					
					/*
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getOrderingProvider(0).getIDNumber().setValue(zadZadal);
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getOrderingProvider(0).getFamilyName().setValue(zadPrijmeniZ);
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getOrderingProvider(0).getGivenName().setValue(zadJmZ);
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getOrderingProvider(0).getMiddleInitialOrName().setValue(zadIcp);
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getOrderingProvider(0).getSuffixEgJRorIII().setValue(zadOdb);
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getOrderingProvider(0).getPrefixEgDR().setValue(zadVs);
					 
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getOrderingProvider(0).getAssigningAuthority().getNamespaceID().setValue(prop.getProperty("send_fac_name"));
					 */
					pracZkr = getStr(rsKom,"zad_prac_zkr");
					pracNaz = getStr(rsKom,"zad_prac_naz");
					/* 
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getEnteringOrganization().getIdentifier().setValue(pracZkr);
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getEnteringOrganization().getText().setValue(pracNaz);
					 msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG().getORC().getEnteringOrganization().getNameOfCodingSystem().setValue(prop.getProperty("send_fac_name"));
					 */                      
					
					
					/*
					 * Priprava segmentu OBR
					 */ 
					
					kodDg = new ArrayList<String>();
					String kodDg1 = getStr(rsKom,"dg_kod1");
					String kodDg2 = getStr(rsKom,"dg_kod2");
					String kodDg3 = getStr(rsKom,"dg_kod3");
					String kodDg4 = getStr(rsKom,"dg_kod4");
					String kodDg5 = getStr(rsKom,"dg_kod5");
					
					if (kodDg1.length() > 0)	kodDg.add(kodDg1);
					if (kodDg2.length() > 0)	kodDg.add(kodDg2);
					if (kodDg3.length() > 0)	kodDg.add(kodDg3);
					if (kodDg4.length() > 0)	kodDg.add(kodDg4);
					if (kodDg5.length() > 0)	kodDg.add(kodDg5);
					
					zadDat = S10;
					rizFak = getStr(rsKom,"riz_fak");
					zadCis = getStr(rsKom,"zad_cis"); 
					zadPracZkrZ = getStr(rsKom,"zad_prac_zkr_z");

					vaha = getStr(rsKom,"vaha");
					vyska = getStr(rsKom,"vyska");
					dat_naroz = rsKom.getDate("dat_naroz");

				}
			} finally {
				rsKom.close();
			}
			
			
			String queryKomD = "select zad_cis, zkr_vstr, naz_vstr, ic_vstr, modalita, dat_vstr " +
			"from al7_kom_d where pk_al7_kom = " + id;               
			
			ResultSet rsKomD = stmt.executeQuery(queryKomD);
			int j = -1;
			try {
				while (rsKomD.next()) {
					
					j = j + 1;
					
					/*
					 * Segment ORC
					 */
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderControl().setValue(p1); 
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getPlacerOrderNumber(0).getEntityIdentifier().setValue(zadCis);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getPlacerOrderNumber(0).getNamespaceID().setValue(prop.getProperty("send_fac_name"));
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderStatus().setValue(p2);
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getQuantityTiming().getStartDateTime().setValue(S6);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getQuantityTiming().getPriority().setValue(zadPrio);
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getDateTimeOfTransaction().setValue(S9);
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getIDNumber().setValue(zadZadal);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getFamilyName().setValue(zadPrijmeniZ);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getGivenName().setValue(zadJmZ);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getMiddleInitialOrName().setValue(zadIcp);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getSuffixEgJRorIII().setValue(zadOdb);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getPrefixEgDR().setValue(zadVs);
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getAssigningAuthority().getNamespaceID().setValue(prop.getProperty("send_fac_name"));
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getEnteringOrganization().getIdentifier().setValue(pracZkr);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getEnteringOrganization().getText().setValue(pracNaz);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getEnteringOrganization().getNameOfCodingSystem().setValue(prop.getProperty("send_fac_name"));
					
					
					
					/*
					 * Segment OBR
					 */
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getSetIDObservationRequest().setValue(Integer.toString(j+1));
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getPlacerOrderNumber(0).getEntityIdentifier().setValue(zadCis);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getPlacerOrderNumber(0).getNamespaceID().setValue(prop.getProperty("send_fac_name"));
					
					String pZkrVstr = getStr(rsKomD,"zkr_vstr");
					String pZadCis =  getStr(rsKomD,"zad_cis");
//					String pIcVstr =  getStr(rsKomD,"ic_vstr");
					String orderNumber = pZadCis;
					String serviceId = pZkrVstr;
					/* Olomouc */
					//if (prop.getProperty("order").equals("ol")){
					String orderSep = prop.getProperty("order_number_sep");
					if (orderSep!= null) {                     
						orderNumber = pZkrVstr + orderSep + pZadCis;      
						serviceId   =  pZkrVstr + orderSep + pZadCis; 
					} else {
						orderNumber = pZadCis;      
						serviceId   = pZkrVstr; 
					}    
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getFillerOrderNumber().getEntityIdentifier().setValue(orderNumber);
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getUniversalServiceIdentifier().getIdentifier().setValue(serviceId);
					String nazVstr = getStr(rsKomD,"naz_vstr");
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getUniversalServiceIdentifier().getText().setValue(nazVstr);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getUniversalServiceIdentifier().getNameOfCodingSystem().setValue(prop.getProperty("send_fac_name"));
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getObservationDateTime().setValue(S6);
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getDangerCode().getText().setValue(rizFak);
					
					if (prop.getProperty("message.ORM.diag.clininfo","no").equals("yes")) {
						String dgSep = prop.getProperty("message.ORM.diag.clininfo.separator"," "); 
						String sumDg = "";
						for (Iterator<String> dg = kodDg.iterator(); dg.hasNext(); ) {
							if (sumDg.length() > 0)	sumDg += dgSep;
							sumDg += (String)dg.next();
						}
						msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getRelevantClinicalInformation().setValue(sumDg);
					}    	 
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getOrderingProvider(0).getIDNumber().setValue(pracZkr);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getOrderingProvider(0).getFamilyName().setValue(pracNaz);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getOrderingProvider(0).getAssigningAuthority().getNamespaceID().setValue(prop.getProperty("send_fac_name"));
					
					String placeField1 = prop.getProperty("place_field_1");
					if (placeField1.equals("date"))
						msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getPlacerField1().setValue(zadDat);
					else
						msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getPlacerField1().setValue(getStr(rsKomD,"modalita"));
					
					String placeField2 = prop.getProperty("place_field_2");
					if (placeField2 != null)
						msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getPlacerField2().setValue(zadPracZkrZ);
					
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getQuantityTiming().getStartDateTime().setValue(S6);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getQuantityTiming().getPriority().setValue(zadPrio);
					
					String S11;
					if (rsKomD.getDate("dat_vstr") != null)
						S11 = fmtTs.format(rsKomD.getDate("dat_vstr"));
					else
						S11 = "";                
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getOBR().getScheduledDateTime().setValue(S11);

					/*
					 * Segment DG1
					 */
					if (prop.getProperty("message.ORM.diag.DG1","no").equals("yes")) {
						String codsys = prop.getProperty("message.ORM.diag.DG1.codsys");
						String type = prop.getProperty("message.ORM.diag.DG1.type","A");
						int seq = 0;
						for (Iterator<String> dg = kodDg.iterator(); dg.hasNext(); ) {
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getDG1(seq).getSetIDDiagnosis().setValue(Integer.valueOf(seq+1).toString());
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getDG1(seq).getDiagnosisCode().getIdentifier().setValue((String)dg.next());
							if (codsys != null)
								msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getDG1(seq).getDiagnosisCode().getNameOfCodingSystem().setValue(codsys);
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getDG1(seq).getDiagnosisType().setValue(type);
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getDG1(seq).getDiagnosisPriority().setValue(Integer.valueOf(seq+1).toString());
							seq += 1;
						}
					}

					/*
					 * Segment OBX
					 */
					if (prop.getProperty("message.ORM.result.OBX","no").equals("yes")) {
						int seq = 0;

						// vaha pacienta
						if (vaha.length() > 0) {
							ST val = new ST(vaha);
							String ident = prop.getProperty("message.ORM.result.OBX.weight.ident","WEIGHT");
							String unit = prop.getProperty("message.ORM.result.OBX.weight.unit","kg");
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getSetIDOBX().setValue(Integer.valueOf(seq+1).toString());
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getValueType().setValue("ST");
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getObservationIdentifier().getIdentifier().setValue(ident);
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getObservationValue(0).setData(val);
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getUnits().getIdentifier().setValue(unit);
							seq += 1;
						}

						// vyska pacienta
						if (vyska.length() > 0) {
							ST val = new ST(vyska);
							String ident = prop.getProperty("message.ORM.result.OBX.height.ident","HEIGHT");
							String unit = prop.getProperty("message.ORM.result.OBX.height.unit","cm");
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getSetIDOBX().setValue(Integer.valueOf(seq+1).toString());
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getValueType().setValue("ST");
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getObservationIdentifier().getIdentifier().setValue(ident);
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getObservationValue(0).setData(val);
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getUnits().getIdentifier().setValue(unit);
							seq += 1;
						}
						
						// vek pacienta
						String[] vek = computePatientAge(dat_naroz);
						if (vek != null) {
							ST val = new ST(vek[0]);
							String ident = prop.getProperty("message.ORM.result.OBX.age.ident","AGE");
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getSetIDOBX().setValue(Integer.valueOf(seq+1).toString());
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getValueType().setValue("ST");
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getObservationIdentifier().getIdentifier().setValue(ident);
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getObservationValue(0).setData(val);
							msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORM_O01_OBRRQDRQ1RXOODSODTNTEDG1OBXNTE().getORM_O01_OBXNTE(seq).getOBX().getUnits().getIdentifier().setValue(vek[1]);
							seq += 1;
						}
					}
				}
				if (j == -1) {
					
					j = j + 1;
					
					/*
					 * Segment ORC
					 */
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderControl().setValue(p1); 
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getPlacerOrderNumber(0).getEntityIdentifier().setValue(zadCis);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getPlacerOrderNumber(0).getNamespaceID().setValue(prop.getProperty("send_fac_name"));
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderStatus().setValue(p2);
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getQuantityTiming().getStartDateTime().setValue(S6);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getQuantityTiming().getPriority().setValue(zadPrio);
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getDateTimeOfTransaction().setValue(S9);
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getIDNumber().setValue(zadZadal);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getFamilyName().setValue(zadPrijmeniZ);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getGivenName().setValue(zadJmZ);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getMiddleInitialOrName().setValue(zadIcp);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getSuffixEgJRorIII().setValue(zadOdb);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getPrefixEgDR().setValue(zadVs);
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getOrderingProvider(0).getAssigningAuthority().getNamespaceID().setValue(prop.getProperty("send_fac_name"));
					
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getEnteringOrganization().getIdentifier().setValue(pracZkr);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getEnteringOrganization().getText().setValue(pracNaz);
					msg.getORM_O01_ORCOBRRQDRQ1RXOODSODTNTEDG1OBXNTECTIBLG(j).getORC().getEnteringOrganization().getNameOfCodingSystem().setValue(prop.getProperty("send_fac_name"));
					
				}
				
			} finally {
				rsKomD.close();
			}
		} finally {
			stmt.close();
		}                        		   
		
	}
}
