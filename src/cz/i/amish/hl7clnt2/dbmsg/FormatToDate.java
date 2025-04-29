/*
 * $Id: FormatToDate.java,v 1.1 2005/10/09 16:41:19 raska Exp $
 * $Log: FormatToDate.java,v $
 * Revision 1.1  2005/10/09 16:41:19  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.2  2005/07/11 16:19:29  raska
 * Drobne upravy v souvislosti s prechodem na Eclipse3.1.
 *
 * Revision 1.1  2005/05/16 12:36:19  raska
 * Zalozeni projektu hl7clnt2 v repository.
 *
 */

package cz.i.amish.hl7clnt2.dbmsg;

/**
 * Trida podporujici konverzi z retezce 
 * do datoveho typu java.sql.Date
 *
 *  @author dolezal
 *
 */

import java.util.GregorianCalendar;

public class FormatToDate extends java.sql.Date {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static java.util.Date getDate(String datum) {
    // yyyyMMdd
        if ((datum == null) || (datum.length() < 8))
            datum = "19990101"; 
	    String rok = datum.substring(0, 4);
        String mes = datum.substring(4, 6);
        String den = datum.substring(6, 8);
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(GregorianCalendar.YEAR, Integer.parseInt(rok));
        gc.set(GregorianCalendar.MONTH, Integer.parseInt(mes)-1);
        gc.set(GregorianCalendar.DATE, Integer.parseInt(den));
        //System.out.println(gc.getTime()); 
        return gc.getTime();
    }
    /**
     * Metoda formatujici retezec
     *  
     * @param r - nutno zadat retezec ve formatu yyyymmdd
     */
    public FormatToDate(String r) {

        super(getDate(r).getTime());
    }

    public FormatToDate(long time) {

        super(time);
    }
     
    public static void main(String[] args) {

       java.sql.Date d = new FormatToDate("20030228");
       System.out.println(d); 
   }

}