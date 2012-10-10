package com.ihome.top.waverider.session;

import java.nio.channels.SocketChannel;

import com.ihome.top.waverider.SlaveWorker;
import com.ihome.top.waverider.command.CommandDispatcher;

/**
 * 
 * @author raoqiang
 *
 */
public abstract class SessionFactory {
	
	public static DefaultSession newSession(Long id, SocketChannel channel, int inBufferSize, int outBufferSize, SlaveWorker slaveWorker, CommandDispatcher commandDispatcher) {
		DefaultSession session = new DefaultSession(id, inBufferSize, outBufferSize);
		session.withChannel(channel).withSlaveWorker(slaveWorker).withCommandDispatcher(commandDispatcher);
		return session;
	}
}
