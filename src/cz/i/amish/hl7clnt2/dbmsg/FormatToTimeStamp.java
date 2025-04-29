/*
 * $Id: FormatToTimeStamp.java,v 1.1 2005/10/09 16:41:20 raska Exp $
 * $Log: FormatToTimeStamp.java,v $
 * Revision 1.1  2005/10/09 16:41:20  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.2  2005/07/11 16:19:29  raska
 * Drobne upravy v souvislosti s prechodem na Eclipse3.1.
 *
 * Revision 1.1  2005/05/16 12:36:20  raska
 * Zalozeni projektu hl7clnt2 v repository.
 *
 */
 
package cz.i.amish.hl7clnt2.dbmsg;

/**
 * Trda podporujici formatovani retezce   
 * do tridy java.sql.Timestamp
 * @author dolezal
 *
 */
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormatToTimeStamp extends java.sql.Timestamp {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static java.util.Date getDate(String datum) {

		if (datum == null) datum = "19000101000000.0000";

		// vzor:		YYYYMMDD[HHMM[SS[.S[S[S[S]]]]]][+/-ZZZZ]
		//                         YYYY     MM      DD       HH       MM      SS     .SSSS            +-ZZZZ       
		String tsRegexPattern = "(\\d{4})(\\d{2})(\\d{2})((\\d{2})(\\d{2})((\\d{2})(\\.\\d+)?)?)?([\\+\\-]\\d{4})?";
		Matcher m = Pattern.compile(tsRegexPattern).matcher(datum);
		if (m.matches()) {
			String rok = m.group(1);
			String mes = m.group(2);
			String den = m.group(3);
			String hod = (m.group(5) != null) ? m.group(5) : "00";
			String min = (m.group(6) != null) ? m.group(6) : "00";
			String sec = (m.group(8) != null) ? m.group(8) : "00";

			GregorianCalendar gc = new GregorianCalendar();
			gc.set(GregorianCalendar.YEAR, Integer.parseInt(rok));
			gc.set(GregorianCalendar.MONTH, Integer.parseInt(mes)-1);
			gc.set(GregorianCalendar.DATE, Integer.parseInt(den));
			int iHod = Integer.parseInt(hod);
			if (iHod > 12){
				gc.set(GregorianCalendar.AM_PM, GregorianCalendar.PM);	
				iHod = iHod - 12;
			} else {
				gc.set(GregorianCalendar.AM_PM, GregorianCalendar.AM);
			}	
			gc.set(GregorianCalendar.HOUR, iHod); 
			gc.set(GregorianCalendar.MINUTE, Integer.parseInt(min));
			gc.set(GregorianCalendar.SECOND, Integer.parseInt(sec));
			gc.set(GregorianCalendar.MILLISECOND, 0);
 
	        return gc.getTime();
		}
		else
			return null;

/*    // yyyyMMddhhmm
        if ((datum == null) || (datum.length() < 12)) 
            datum = "199901010101"; 
	    String rok = datum.substring(0, 4);
        String mes = datum.substring(4, 6);
        String den = datum.substring(6, 8);
        String hod = datum.substring(8, 10);
        String min = datum.substring(10,12);
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(GregorianCalendar.YEAR, Integer.parseInt(rok));
        gc.set(GregorianCalendar.MONTH, Integer.parseInt(mes)-1);
        gc.set(GregorianCalendar.DATE, Integer.parseInt(den));
        int iHod = Integer.parseInt(hod);
        if (iHod > 12){
            gc.set(GregorianCalendar.AM_PM, GregorianCalendar.PM);	
            iHod = iHod - 12;
        } else {
        	gc.set(GregorianCalendar.AM_PM, GregorianCalendar.AM);
        }	
        gc.set(GregorianCalendar.HOUR, iHod); 
        gc.set(GregorianCalendar.MINUTE, Integer.parseInt(min)); 
        //System.out.println(gc.getTime()); 
        return gc.getTime();
*/    }

    /**
     * Metoda formatujici retezec 
     * 
     * @param r - nutno zadat ve formatu yyyyMMddhhmm
     */
    public FormatToTimeStamp(String r) {

        super(getDate(r).getTime());
    }

    public FormatToTimeStamp(long time) {

        super(time);
    }

    public static void main(String[] args) {

		String s[] = {
			"20041130",
			"200411301055",
			"200411302355",
			"20041130235533",
			"19990305+0100",
			"19981231235959.1234-0203",
			null
		};

		for (int i = 0; i < s.length; i++) {
			System.out.println("text: " + s[i] + " timestamp: " + new FormatToTimeStamp(s[i]));
		} 

   }

}