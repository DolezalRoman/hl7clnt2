package cz.i.amish.hl7clnt2.test;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Properties;

import cz.i.amish.hl7clnt2.dbmsg.ORU_R01_DB;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import junit.framework.TestCase;

public class ORU_R01_DBTest extends TestCase {
	
	private Properties prop;
	private Driver driver;
	private Connection conn;
	private ORU_R01_DB adt;
	private PipeParser pParser;
	
	
	protected void setUp() throws Exception {
		super.setUp();
		prop = new Properties();
		prop.load(new FileInputStream("conf/HL7Clnt_NEM.property"));
		
		driver = (Driver)Class.forName(prop.getProperty("jdbcdriver")).newInstance();
		DriverManager.registerDriver(driver);
		
		String url = prop.getProperty("dburl");
		String login = prop.getProperty("dblogin");
		
		conn = DriverManager.getConnection(url,login,prop.getProperty("dbpasswd"));
		conn.setAutoCommit(false);	
		
		adt = new ORU_R01_DB(conn,prop);
		pParser = new PipeParser();
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void test1() throws Exception {
		
		adt.loadFromDb(699); //bio
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}
	public void test2() throws Exception {
		
		adt.loadFromDb(686); //hem
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}		
	
	public void test3() throws Exception {
		
		adt.loadFromDb(149); //amb
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}
	
	public void test4() throws Exception {
		
		adt.loadFromDb(146); //rtg
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}
	
	public void test5() throws Exception {
		
		adt.loadFromDb(178); //mik
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}
	
	public void test6() throws Exception {
		
		adt.loadFromDb(699); //pat
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}
	
	public void test7() throws Exception {
		
		adt.loadFromDb(686); //pat
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}
	
	public void test8() throws Exception {
		
		adt.loadFromDb(16094); //bio  -- cilove pracoviste oddeleni
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}
	
	public void test9() throws Exception {
		
		adt.loadFromDb(16096); //bio
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}
	
	public void test10() throws Exception {
		
		adt.loadFromDb(16159); //bio
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}
	
	public void test11() throws Exception {
		
		adt.loadFromDb(16161); //bio_conv
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}
	
	public void test12() throws Exception {
		
		adt.loadFromDb(29393); //r01_rtg
		Message msg = adt.getMsg(); 
		String adtMessage = pParser.encode(msg);
		System.out.println(adtMessage);
		
	}
	
}
