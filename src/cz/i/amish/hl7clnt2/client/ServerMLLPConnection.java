/*
 * Created on 7.4.2004
 *
 * $Id: ServerMLLPConnection.java,v 1.3 2020/02/26 14:44:03 amis Exp $
 * $Log: ServerMLLPConnection.java,v $
 * Revision 1.3  2020/02/26 14:44:03  amis
 * Migrated to OpenJDK 8.
 *
 * Revision 1.2  2017/04/24 13:44:06  raska
 * vyhozeny komentare z duvodu problemu s cestinou
 *
 * Revision 1.1  2005/10/09 16:41:23  raska
 * Zavedeni do nove repository.
 *
 * Revision 1.2  2005/07/11 16:18:50  raska
 * Drobne upravy v souvislosti s prechodem na Eclipse3.1.
 *
 * Revision 1.1  2005/05/20 13:28:02  raska
 * Opraveno jmeno package cz.i.amish.hl7cln2.client.
 *
 * Revision 1.1  2005/05/16 12:36:29  raska
 * Zalozeni projektu hl7clnt2 v repository.
 *
 */
package cz.i.amish.hl7clnt2.client;

import java.net.Socket;
import java.util.Properties;

import org.apache.log4j.Logger;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.llp.MinLowerLayerProtocol;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;

/**
 * @author raska
 */
public class ServerMLLPConnection implements ServerConnection {
	
	private final Properties prop;

	private Connection mllpConnection;
	private String host;
	private int port;
	
	private static final Logger logger = Logger.getLogger(ServerMLLPConnection.class);
	
	public ServerMLLPConnection(Properties p) {
		this.prop = p;

		this.host = prop.getProperty("llp.mllp.host","localhost");
		this.port = Integer.valueOf(prop.getProperty("llp.mllp.port","3111")).intValue();
		
		logger.info("Definovano MLLP spojeni na HL7 server (host="+host+",port="+port+") ...");		
	}
	
	public void connect() throws Exception {
		
		Socket socket = new Socket(host,port);
		MinLowerLayerProtocol mllp = new MinLowerLayerProtocol();
		PipeParser parser = new PipeParser();

		this.mllpConnection = new Connection(parser,mllp,socket);
		
		logger.info("Pripojeni na vzdaleny server ...");
	}

	public void disconnect() throws Exception {
		this.mllpConnection.close();		
		logger.info("Odpojeni od vzdaleneho serveru ...");
	}
	
	public Message send(Message msg) throws Exception {
		try {
			return this.mllpConnection.getInitiator().sendAndReceive(msg);
		} catch (HL7Exception e) {
			logger.error("Exception: ",e);
			return null;
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		// Nejdrive se vyzkousi spojeni na MLLP server:
		
		Properties p = new Properties();
//		p.put("llp.mllp.host","pc100");
//		p.put("llp.mllp.port","3111");
		p.put("llp.mllp.host","192.168.244.45");
		p.put("llp.mllp.port","3326");
		
		
		String inputMessage = 	"MSH|^~\\&|AMIS*H Rengen|RTG^RTG@i.cz|BROKER|MITRA|200404231440||ORU^R01|442|P|2.3||||||8859/1\r" +
								"PID|1|606466^^^RTG|221101/035@606466||CEJNAR^JOSEF||202211010000|O|||Skolska 318^^Predmerice^^50302\r" +
								"PV1|1|O||R|||||||||||||||560448|||||||||||||||||||||||||200404220000\r" +
								"OBR|1|8065^RTG|8065^RTG|pl@8065^rtg hrudniku = plice vleze^RTG|||||||||R000|||^^^^^^^^RTG\r" +
								"OBX|1|TX|pl@322||... ������������������������؊�� ...||||||F|||||pokorny";
//		String ackMessageString	= "MSH|^~\\&|foo|foo||foo|200108151718||ACK^A01^ACK|1|D|2.4|\rMSA|AA\r";
		
		PipeParser pipeParser = new PipeParser();
		Message msgOut = pipeParser.parse(inputMessage);

		System.out.println("Pripojeni MLLP na vzdaleny HL7 server ...");
		ServerMLLPConnection c = new ServerMLLPConnection(p);	
		c.connect();
		
		System.out.println("ZPRAVA: " + inputMessage);
		
		Message msgIn = c.send(msgOut);
		
		System.out.println("ODPOVED: " + pipeParser.encode(msgIn));
		
		System.out.println("Odpojeni od vzdaleneho serveru ...");
		c.disconnect();
	}
}
