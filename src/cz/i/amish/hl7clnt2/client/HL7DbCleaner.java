/**
 * 
 */
package cz.i.amish.hl7clnt2.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * @author raska
 *
 */
public class HL7DbCleaner {
	
	private static final Logger logger = Logger.getLogger(HL7DbCleaner.class);
	private Properties prop;

	public HL7DbCleaner(String propFileName) throws IOException {
		super();

		FileInputStream propFile = new FileInputStream(propFileName);
		
		prop = new Properties();
		prop.load(propFile);

		if (prop.containsKey("logconfig")) {
			org.apache.log4j.xml.DOMConfigurator.configure(prop.getProperty("logconfig"));
		}
		
	}

	private void run() {
		logger.info("Start cisteni databaze ...");
		try {
			try {
				cleanUpDb();
			}
			catch (Exception e) {
				logger.error("Exception: ",e);
			}
		} catch (RuntimeException e) {
			logger.error("RuntimeException: ",e);
			e.printStackTrace();
		}
		logger.info("Konec cisteni databaze ...");	
	}

	private void cleanUpDb() throws Exception {
		Driver driver = (Driver)Class.forName(prop.getProperty("jdbcdriver")).getDeclaredConstructor().newInstance();
		DriverManager.registerDriver(driver);
		
		String url = prop.getProperty("dburl");
		String login = prop.getProperty("dblogin");
		
		Connection con = DriverManager.getConnection(url,login,prop.getProperty("dbpasswd"));
		con.setAutoCommit(false);

		logger.info("Pripojeni na databazi (uzivatel='"+login+"',url='"+url+"',autocommit=off) ...");
		
		int sustainDays = Integer.valueOf(prop.getProperty("db.cleaner.message.sustain","30")).intValue();
		String stat = prop.getProperty("db.cleaner.message.status");
		logger.info("Parametry pro mazani: drzet = " + sustainDays + " dnu,  stavy = " + stat);
		
		int count = 0;
		
		String sql = "select pk from al7_zpravy where extend(cas0,year to day) < (today - " + sustainDays + ")";
		if (stat != null) {
			sql += " and stav in (" + stat +")";
		}
		
		List<Integer> keys = new ArrayList<Integer>();
		ResultSet rs = con.createStatement().executeQuery(sql);
		try {
			while (rs.next()) {
				keys.add(Integer.valueOf(rs.getInt("pk")));
			}
		} finally {
			rs.close();
		}
		
		PreparedStatement delSt1 = con.prepareStatement("delete from al7_kom where pk = (select icz from al7_zpravy where pk = ?)");
		PreparedStatement delSt2 = con.prepareStatement("delete from al7_zpravy where pk = ?");
		try {
			for (Iterator<Integer> i = keys.iterator(); i.hasNext();) {
				int pk = ((Integer) i.next()).intValue();
				delSt1.setInt(1, pk);
				delSt1.executeUpdate();

				delSt2.setInt(1, pk);
				delSt2.executeUpdate();

				con.commit();
				logger.debug("Zprava byla vymazana: pk = " + pk);
				count++;
			}
		} finally {
			delSt1.close();
			delSt2.close();
		}
		
		logger.info("Smazano celkem zprav: " + count);
		con.close();
	}

	public static void main(String[] args) throws IOException {

		if (args.length > 1) {
			System.err.println("usage: HL7DbCleaner [property file]");
			System.exit(1);
		}
		
		String propFileName = (args.length == 1) ? args[0] : "conf/HL7Clnt.property";
		
		new HL7DbCleaner(propFileName).run();
	}

}
