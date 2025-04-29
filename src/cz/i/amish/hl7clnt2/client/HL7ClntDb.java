package cz.i.amish.hl7clnt2.client;


import java.io.FileInputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.informix.jdbc.IfmxStatement;

/*
 * Created on 24.3.2004
 *
 * $Id: HL7ClntDb.java,v 1.7 2020/02/26 14:44:03 amis Exp $
 * $Log: HL7ClntDb.java,v $
 * Revision 1.7  2020/02/26 14:44:03  amis
 * Migrated to OpenJDK 8.
 *
 * Revision 1.6  2017/04/24 13:44:07  raska
 * vyhozeny komentare z duvodu problemu s cestinou
 *
 * Revision 1.5  2017/04/24 13:25:27  raska
 * ver. 2.7.0; spojeni verze od Pavla Navrkala, odstraneny warning
 *
 * Revision 1.4  2007/10/04 13:09:32  dolezal
 * Oprava razeni ve vyberu ListStatementu, prechod od casu vzniku udalosti - cas0 k primarnimu klici pk. Duvod - kolize ve zpravach A08.
 *
 * Revision 1.3  2005/10/20 13:27:45  raska
 * Login a password pro pripojeni k db je separatni polozka v property.
 *
 * Revision 1.2  2005/10/19 14:38:14  raska
 * Prechod na JDBC ver.3.0. V URL se nsatvuji promenne pro lock mode a isolation level.
 *
 * Revision 1.1  2005/10/09 16:41:24  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.2  2005/06/03 12:48:39  raska
 * Doplneno rizeni odesilani zprav podle typu zpravy a udalosti.
 *
 * Revision 1.1  2005/05/20 13:28:02  raska
 * Opraveno jmeno package cz.i.amish.hl7cln2.client.
 *
 * Revision 1.1  2005/05/16 12:36:29  raska
 * Zalozeni projektu hl7clnt2 v repository.
 *
 */

/**
 * @author raska
 */

public class HL7ClntDb {

	private final Properties prop;
	private java.sql.Connection con;
	
	private final String ids;
	private final int retransMaxCount;
	private final int statusOffset;
	
	private PreparedStatement getMsgIdListStatement;
	private PreparedStatement getMsgIdStatement;
	private PreparedStatement getMsgTypeStatement;
	private PreparedStatement updateStatusStatement;
	private PreparedStatement updateIterStatement;
	private PreparedStatement insertStatement;
	
	private static final Logger logger = Logger.getLogger(HL7ClntDb.class);
	
	public HL7ClntDb(Properties p) {
		prop = p;
		
		this.ids = p.getProperty("ids","UNKNOWN");
		this.retransMaxCount = Integer.valueOf(p.getProperty("maxmsgretrans","1")).intValue();
		this.statusOffset = Integer.valueOf(p.getProperty("status.offset","0")).intValue();
	}
		
	public void connect() throws Exception {
		Driver driver = (Driver)Class.forName(prop.getProperty("jdbcdriver")).getDeclaredConstructor().newInstance();
		DriverManager.registerDriver(driver);
		
		String url = prop.getProperty("dburl");
		String login = prop.getProperty("dblogin");

		logger.info("Pripojeni na databazi (url='"+url+"',autocommit=off) ...");
		
		con = DriverManager.getConnection(url,login,prop.getProperty("dbpasswd"));
		con.setAutoCommit(false);
		
		this.prepareStatements();
	}
	
	private void prepareStatements() throws SQLException {
		this.getMsgIdListStatement = this.con.prepareStatement("SELECT pk,stav,cas0 FROM al7_zpravy WHERE ids = ? and stav in (" + (1 + statusOffset) + " , " + (9 + statusOffset) + ") and iter < ? ORDER BY stav desc, pk");
		this.getMsgIdStatement = this.con.prepareStatement("SELECT icz FROM al7_zpravy WHERE pk = ?");
		this.getMsgTypeStatement = this.con.prepareStatement("SELECT msh_9 FROM al7_zpravy WHERE pk = ?");
		
		this.updateStatusStatement = this.con.prepareStatement("UPDATE al7_zpravy SET stav = ?, cas1 = extend(current,year to second), iter = iter+1  WHERE pk = ?");
		this.updateIterStatement = this.con.prepareStatement("UPDATE al7_zpravy SET iter = iter+1 WHERE pk = ?");

		//this.insertStatement = this.con.prepareStatement("INSERT INTO al7_zpravy(icz,ids,msh_9,stav,cas0) VALUES (?,?,?,?,extend(current,year to second))");
		this.insertStatement = this.con.prepareStatement("INSERT INTO al7_zpravy(idz,ids,msh_9,stav) VALUES (?,?,?,?)");
	}
	
	private void closeStatements() throws SQLException {
		this.getMsgIdListStatement.close();
		this.getMsgIdStatement.close();
		this.getMsgTypeStatement.close();
		
		this.updateStatusStatement.close();
		
		this.insertStatement.close();
	}
	
	public void close() {
		logger.info("Odpojeni od databaze ...");
		try {
			this.closeStatements();
			con.close();
		} catch (SQLException e) {
			logger.error("Close exception",e);
		}
	}
	
	public Connection getDbConnection() {
		return this.con;
	}
	
	public int newMsg(String idz, String ids, String type, int st) throws SQLException {
		
		this.insertStatement.setString(1,idz);
		this.insertStatement.setString(2,ids);
		this.insertStatement.setString(3,type);
		this.insertStatement.setInt(4, statusOffset + 1);
		
		if (this.insertStatement.executeUpdate() != 1)
			throw new SQLException("Insert do tabulky al7_zpravy nevratil pocet 1!");
		
		int pk = ((IfmxStatement)this.insertStatement).getSerial();
		
		this.updateStatus(pk,st);
		
		return pk;
	}
	
	public List<Integer> getMsgIdList() throws SQLException {
		List<Integer> ids = new ArrayList<Integer>();
		
		this.getMsgIdListStatement.setString(1,this.ids);
		this.getMsgIdListStatement.setInt(2,this.retransMaxCount);
		
		ResultSet rs = this.getMsgIdListStatement.executeQuery();
		try {
			while (rs.next()) {
				ids.add(Integer.valueOf(rs.getInt("pk")));
			}
		}
		finally {
			rs.close();
		}
		return ids;
	}
	
	public int getMsgId(int pk) throws SQLException {
		int id = -1;
		
		this.getMsgIdStatement.setInt(1,pk);
		
		ResultSet rs = this.getMsgIdStatement.executeQuery();
		try {
			if (rs.next())	id = rs.getInt("icz");
		}
		finally {
			rs.close();
		}
		if (id < 0)
			throw new SQLException("Nezname ICZ pro pk = " + pk + "!");
		return id;
	}
	
	public String getMsgType(int pk) throws SQLException {
		String type = null;
		
		this.getMsgTypeStatement.setInt(1,pk);
		
		ResultSet rs = this.getMsgTypeStatement.executeQuery();
		try {
			if (rs.next())	type = rs.getString("msh_9").trim();
		}
		finally {
			rs.close();
		}
		if (type == null)
			throw new SQLException("Nezname MSH_9 pro pk = " + pk + "!");
		return type;
	}
	
	public void updateStatus(int pk, int st ) throws SQLException {
		this.updateStatusStatement.setInt(1,st + statusOffset);
		this.updateStatusStatement.setInt(2,pk);
		if (this.updateStatusStatement.executeUpdate() != 1)
			throw new SQLException("Update stavu v tabulce zprav: nebyl zmenen prave jeden radek!");
	}
	
	public void incrementIter(int pk) throws SQLException {
		this.updateIterStatement.setInt(1,pk);
		if (this.updateIterStatement.executeUpdate() != 1)
			throw new SQLException("Update poctu iteraci v tabulce zprav: nebyl zmenen prave jeden radek!");
	}
	
	public void commit() throws SQLException {
		con.commit();
	}
	
	public void rollback() throws SQLException {
		con.rollback();
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
		
		HL7ClntDb db = new HL7ClntDb(prop);
		db.connect();
		
/*		List c = db.getMsgIdList();
		
		int id;
		for (int i= 0; i < c.size(); i++) {
			id = ((Integer)c.get(i)).intValue();
			System.out.println(id + "," + db.getMsgId(id) + " : " + db.getMsgType(((Integer)c.get(i)).intValue()));
		}
		
//		for (int i = 0; i < c.size(); i++)
//			db.updateStatus(((Integer)c.get(i)).intValue(),2);
//		
//		db.commit();
		
		System.out.println("NOVA ZPRAVA: " + db.newMsg(111,"TRPASLIK","ORU^O01",3));
		db.commit();
*/
		
		CallableStatement proc = db.getDbConnection().prepareCall("{ call al7_kom_t(?) }");
		// proc.registerOutParameter(1,Types.INTEGER);
		
		proc.setInt(1,10);
		
		ResultSet rs = proc.executeQuery();
		while (rs.next())
			System.out.println(rs.getInt(1) + ":" + rs.getString(2));
		
		db.close();
		
	}
}
