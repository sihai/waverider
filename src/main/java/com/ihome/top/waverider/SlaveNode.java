/**
 * waverider
 *  
 */

package com.ihome.top.waverider;

import com.ihome.top.waverider.command.CommandProvider;
import com.ihome.top.waverider.slave.MasterDeadCallback;

/**
 * <p>
 * 基于Master-Slave结构分布式命令执行框架Slave节点接口
 * </p>
 * 
 * @author <a href="mailto:sihai@taobao.com">sihai</a>
 *
 */
public interface SlaveNode extends Node {

	String SLAVE_COMMAND_PROVIDER_THREAD_NAME = "Waverider-Slave-Command-Provider-Thread";
	String SLAVE_HEART_BEAT_THREAD_NAME_PREFIX = "Waverider-Slave-Heartbeat";
	String SLAVE_COMMAND_DISPATCHE_THREAD_NAME = "Waverider-Slave-Command-Dispatcher-Thread";
	
	/**
	 * 添加命令产生器
	 * @param commandProvider
	 */
	void addCommandProvider(CommandProvider commandProvider);
	
	/**
	 * 
	 * @param commandProviderName
	 */
	//void startCommandProvider(String commandProviderName);
	
	/**
	 * 注册MasterDeadCallback回调, 覆盖原有的注册
	 * @param callback
	 * 		null	 	取消注册
	 * 		非null		覆盖原有注册
	 */
	void registerMasterDeadCallback(MasterDeadCallback callback);
}
