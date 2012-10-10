/**
 * waverider
 *  
 */

package com.ihome.top.waverider.demo;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ihome.top.waverider.Node;
import com.ihome.top.waverider.SlaveNode;
import com.ihome.top.waverider.command.Command;
import com.ihome.top.waverider.command.CommandFactory;
import com.ihome.top.waverider.command.CommandHandler;
import com.ihome.top.waverider.command.CommandProvider;
import com.ihome.top.waverider.config.WaveriderConfig;
import com.ihome.top.waverider.master.DefaultMasterNode;
import com.ihome.top.waverider.network.Packet;
import com.ihome.top.waverider.slave.DefaultSlaveNode;


/**
 * DataTransport demo class
 * 
 * 
 * @author raoqiang
 *
 */
public class DataTransport {
	
	private static final Log logger = LogFactory.getLog(DataTransport.class);
	
	private static final Long ECHO = 99L;
	private static final int  DATA_SIZE_BASE = 1024;
	
	public static final int  DATA_SIZE_1K = 0;
	public static final int  DATA_SIZE_2K = 1;
	public static final int  DATA_SIZE_4K = 2;
	public static final int  DATA_SIZE_8K = 3;
	public static final int  DATA_SIZE_16K = 4;
	
	public static final int  DATA_SIZE_128M = 17;
	
	private boolean isMaster = true;
	private int dataSize = (1 << DATA_SIZE_1K) * DATA_SIZE_BASE;
	private WaveriderConfig config = new WaveriderConfig();
	private Node node;
	
	public DataTransport(boolean isMaster, int dataSize) {
		this.isMaster = isMaster;
		this.dataSize = (1 << dataSize) * DATA_SIZE_BASE;
	}
	
	public void run() {
		if(isMaster) {
			runAsMaster();
		} else {
			runAsSlave();
		}
		try{
			Thread.currentThread().join();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void runAsMaster() {
		System.out.println("Run as master model ......");
		node = new DefaultMasterNode(config);
		node.addCommandHandler(ECHO, new CommandHandler() {

			@Override
			public Command handle(Command command) {
				ByteBuffer buffer = command.getPayLoad();
				int size = command.getSize() - Command.getHeaderSize();
				assert(buffer.remaining() == size);
				logger.info(String.format("Receive %d bytes data from slave:%s", size, command.getSession().getSlaveWorker()));
				Packet.dump(buffer);
				byte[] bytes = buffer.array();
				for(int i = 0; i < bytes.length; i++) {
					bytes[i] = (byte)(1- bytes[i]);
				}
				return CommandFactory.createCommand(ECHO, buffer);
			}
		});
		node.init();
		node.start();
	}
	
	private void runAsSlave() {
		System.out.println("Run as slave model ......");
		config.setMasterAddress("127.0.0.1");
		node = new DefaultSlaveNode(config);
		// command 
		((SlaveNode)node).addCommandProvider(new CommandProvider() {
			
			private int count = 0;
			
			@Override
			public String getName() {
				return "SmallDataCommandProvider";
			}

			@Override
			public Command produce() {
				byte[] buffer = new byte[dataSize];
				int pad = count++ % 2;
				for(int i = 0; i < dataSize; i++) {
					buffer[i] = (byte)pad;
				}
				return CommandFactory.createCommand(ECHO, ByteBuffer.wrap(buffer));
			}

			@Override
			public List<Command> produce(long count) {
				throw new UnsupportedOperationException("Not supported");
			}
		});
		
		node.addCommandHandler(ECHO, new CommandHandler() {

			@Override
			public Command handle(Command command) {
				ByteBuffer buffer = command.getPayLoad();
				int size = command.getSize() - Command.getHeaderSize();
				assert(buffer.remaining() == size);
				logger.info(String.format("Receive %d bytes data from master", size));
				Packet.dump(buffer);
				return CommandFactory.createCommand(ECHO, buffer);
			}
		});
		
		node.init();
		node.start();
	}
}
