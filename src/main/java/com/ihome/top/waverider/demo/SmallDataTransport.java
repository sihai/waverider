/**
 * waverider
 *  
 */

package com.ihome.top.waverider.demo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * SmallDataTransport class
 * 
 * data size 16KB
 * 
 * java waverider SmallDataTransport -mode master
 * java waverider SmallDataTransport -mode slave
 * 
 * 
 * @author raoqiang
 *
 */
public class SmallDataTransport {
	
	private static final Log logger = LogFactory.getLog(SmallDataTransport.class);
	
	private static final String MASTER_MODE = "master";
	private static final String SLAVE_MODE = "slave";
	private static final String ARG_MODE_KEY = "-mode";
	private static final int MIN_ARGS_LENGTH = 2;
	
	public static void showUsage() {
		System.out.println("waverider");
		System.out.println("	Usage:");
		System.out.println("			Run as master mode:");
		System.out.println("				java -jar waverider.jar SmallDataTransport -mode master");
		System.out.println("			Run as slave mode:");
		System.out.println("				java -jar waverider.jar SmallDataTransport -mode slave");
	}
	
	public static void main(String[] args) {
		if(args.length < MIN_ARGS_LENGTH) {
			showUsage();
			return;
		}
		
		if(!ARG_MODE_KEY.equals(args[0])) {
			showUsage();
			return;
		}
		
		if(MASTER_MODE.equals(args[1])) {
			runAsMaster();
		} else if(SLAVE_MODE.equals(args[1])) {
			runAsSlave();
		} else {
			showUsage();
			return;
		}
		
		try{
			Thread.currentThread().join();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void runAsMaster() {
		new DataTransport(true, DataTransport.DATA_SIZE_16K).run();
	}
	
	public static void runAsSlave() {
		new DataTransport(false, DataTransport.DATA_SIZE_16K).run();
	}
}
