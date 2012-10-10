/**
 * waverider
 *  
 */

package com.ihome.top.waverider.slave;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ihome.top.waverider.SlaveNode;
import com.ihome.top.waverider.State;
import com.ihome.top.waverider.command.Command;
import com.ihome.top.waverider.command.CommandDispatcher;
import com.ihome.top.waverider.command.CommandFactory;
import com.ihome.top.waverider.command.CommandHandler;
import com.ihome.top.waverider.command.CommandProvider;
import com.ihome.top.waverider.command.SampleCommandDispatcher;
import com.ihome.top.waverider.command.SlaveHeartbeatCommandHandler;
import com.ihome.top.waverider.common.WaveriderThreadFactory;
import com.ihome.top.waverider.config.WaveriderConfig;
import com.ihome.top.waverider.master.MasterState;
import com.ihome.top.waverider.network.DefaultNetWorkClient;
import com.ihome.top.waverider.network.NetWorkClient;
import com.ihome.top.waverider.slave.failure.slave.DefaultMasterFailureHandler;
import com.ihome.top.waverider.slave.failure.slave.DefaultMasterFailureMonitor;
import com.ihome.top.waverider.slave.failure.slave.MasterFailureMonitor;

/**
 * <p>
 * 系统默认的网络命令执行Slave节点
 * </p>
 * 
 * @author <a href="mailto:sihai@taobao.com">sihai</a>
 *
 */
public class DefaultSlaveNode implements SlaveNode {
	
	private static final Log logger = LogFactory.getLog(DefaultSlaveNode.class);
	
	// Config
	private WaveriderConfig config;											// 配置信息
	
	// Command provide
	private List<CommandProvider> commandProviderList;						// 命令发生器列表
	//private Map<String, Boolean> commaandProviderName2IsReadyMap;			// 命令是否就绪
	//private Thread commandProviderThread;									// 命令发生器执行器
	private ScheduledExecutorService commandProviderExecutor;				// 命令发生器执行器
	//private ReentrantLock commandProviderRunLock;							//
	//private Condition commandProviderRunCondition;						//
	//private boolean isCommandProviderRun;									//
	private volatile MasterDeadCallback callback;							// 上层注册的监听Slave离开回调

	// Command execute
	private CommandDispatcher commandDispatcher;							// 命令分发器
	private Thread commandDispatchThread;									// 命令分发线程
	
	// Failure monitor
	private MasterFailureMonitor masterFailureMonitor;
	
	// Heartbeat
	private AtomicLong stateIDGenrator = new AtomicLong(0);
	private ScheduledExecutorService heartbeatScheduler;
	
	// Network
	private NetWorkClient netWorkClient;
	
	public DefaultSlaveNode(WaveriderConfig config) {
		this.config = config;
		this.commandProviderList = new LinkedList<CommandProvider>();
		//this.commandProviderName2IsReadyMap = new HashMap<String, Boolean>();
		//this.commandProviderRunLock = new ReentrantLock();
		//this.commandProviderRunCondition = this.commandProviderRunLock.newCondition();
		//this.isCommandProviderRun = false;
		this.commandDispatcher = new SampleCommandDispatcher();
	}
	
	@Override
	public boolean init() {
		commandDispatcher.addCommandHandler(0L, new SlaveHeartbeatCommandHandler(this));
		netWorkClient = new DefaultNetWorkClient(config.getMasterAddress(), config.getPort());
		if(!netWorkClient.init()) {
			return false;
		}
		masterFailureMonitor = new DefaultMasterFailureMonitor(new DefaultMasterFailureHandler(this), MasterFailureMonitor.DEFAULT_FAILURE_MONITOR_INTERVAL, MasterFailureMonitor.DEFAULT_FAILURE_MONITOR_WAIT_MASTER_STATE_TIME_OUT);
		if(!masterFailureMonitor.init()) {
			return false;
		}
		//commandProviderThread = new Thread(new CommandProviderTask(), "Top-Task-Scheduler-Slave-Command-Provider-Thread");
		//commandProviderThread.setDaemon(true);
		commandProviderExecutor = Executors.newScheduledThreadPool(1, new WaveriderThreadFactory("Top-Task-Scheduler-Slave-Command-Provider", null, true));
		commandDispatchThread = new Thread(new CommandDispatchTask(), SLAVE_COMMAND_DISPATCHE_THREAD_NAME);
		commandDispatchThread.setDaemon(true);
		// heart beat
		heartbeatScheduler = Executors.newScheduledThreadPool(1, new WaveriderThreadFactory(SLAVE_HEART_BEAT_THREAD_NAME_PREFIX, null, true));
		return true;
	}

	@Override
	public boolean start() {
		while (true) {
			try {
				netWorkClient.start();
				break;
			} catch (Exception e) {
				logger.error(e);
				if (e.getCause() instanceof IOException || e.getCause() instanceof ConnectException) {
					try {
						logger.error("Can not connect to master , sleep 60s, then try again");
						Thread.sleep(60 * 1000);
					} catch (InterruptedException ex) {
						logger.error("OOPS：Exception：", ex);
						Thread.currentThread().interrupt();
					}
					continue;
				}
			}
		}

		masterFailureMonitor.start();
		//commandProviderThread.start();
		commandProviderExecutor.scheduleAtFixedRate(new CommandProviderTask(), 0, config.getSlaveCommandProduceInterval(), TimeUnit.SECONDS);
		commandDispatchThread.start();
		heartbeatScheduler.scheduleAtFixedRate(new HeartbeatTask(), WaveriderConfig.WAVERIDER_DEFAULT_HEART_BEAT_INTERVAL, WaveriderConfig.WAVERIDER_DEFAULT_HEART_BEAT_INTERVAL, TimeUnit.SECONDS);
		
		return true;
	}

	@Override
	public boolean stop() {
		netWorkClient.stop();
		masterFailureMonitor.stop();
		//commandProviderThread.interrupt();
		commandProviderExecutor.shutdown();
		commandDispatchThread.interrupt();
		heartbeatScheduler.shutdown();
		return true;
	}

	@Override
	public boolean restart() {
		return true;
	}
	
	@Override
	public void addCommandHandler(Long command, CommandHandler handler) {
		if(command == null || command.equals(0L)) {
			throw new IllegalArgumentException("command must not be null or 0");
		}
		commandDispatcher.addCommandHandler(command, handler);
	}
	
	@Override
	public State gatherStatistics() {
		SlaveState slaveState = new SlaveState();
		slaveState.setId(stateIDGenrator.addAndGet(1));
		slaveState.setIsMasterCandidate(config.isMasterCandidate());
		
		return slaveState;
	}
	
	@Override
	public void acceptStatistics(State state) {
		//logger.info(new StringBuilder("Slave Accept Master state : ").append(((MasterState)state).toString()).toString());
		masterFailureMonitor.process((MasterState)state);
	}
	
	/**
	 * 分发命令
	 * @throws Exception
	 */
	private void _process_() throws IOException, InterruptedException {
		
		Command command = netWorkClient.receive();
		if(command != null) {
			Command resultCommand = commandDispatcher.dispatch(command);
			command.getPayLoad().clear();
			if(resultCommand != null) {
				netWorkClient.send(resultCommand);
			}
		}
	}
	
	private void _heartbeat_() throws Exception {
		SlaveState slaveState  = (SlaveState)gatherStatistics();
		netWorkClient.send(CommandFactory.createHeartbeatCommand(slaveState.toByteBuffer()));
		logger.debug("Slave send one heartbeat command to Master");
	}
	
	// 收集所有的CommandProvider产生的命令, 并通netWorkClient过发送出去
	private class CommandProviderTask implements Runnable {

		@Override
		public void run() {
			int total = 0;
			Iterator<CommandProvider> iterator = null;
			CommandProvider commandProvider = null;
			Command command = null;
			total = 0;
			iterator = commandProviderList.iterator();
			while(iterator.hasNext()) {
				commandProvider = iterator.next();
					command = commandProvider.produce();
					if (command != null) {
						try {
							netWorkClient.send(command);
						} catch (InterruptedException e) {
							logger.error("OOPS：Exception：", e);
							e.printStackTrace();
							Thread.currentThread().interrupt();
						}
						total++;
					}
			}
		}
	}
	
	// 命令分发执行
	private class CommandDispatchTask implements Runnable{
		@Override
		public void run() {
			logger.info("Waverider-Slave-Command-Dispatch-Thread started");
			while(!Thread.currentThread().isInterrupted()){
				try{
					_process_();
				} catch(InterruptedException e) {
					e.printStackTrace();
					logger.error("OOPS：Exception：", e);
					Thread.currentThread().interrupt();
				} catch(IOException e){
					e.printStackTrace();
					logger.error("OOPS：Exception：", e);
				} catch(Exception e){
					e.printStackTrace();
					logger.error("OOPS：Exception：", e);
				}
			}
			logger.info("Waverider-Slave-Command-Dispatch-Thread stoped");
		}
	}
	
	private class HeartbeatTask implements Runnable {
		public void run() {
			try {
				_heartbeat_();
			} catch(InterruptedException e) {
				logger.error("OOPS：Exception：", e);
				e.printStackTrace();
				Thread.currentThread().interrupt();
			} catch(Exception e) {
				logger.error(e);
				e.printStackTrace();
			}
		}
	}

	@Override
	public void addCommandProvider(CommandProvider commandProvider) {
		commandProviderList.add(commandProvider);
		//commandProviderName2IsReadyMap.put(commandProvider.getName(), false);
	}
	
	@Override
	public void registerMasterDeadCallback(MasterDeadCallback callback) {
		this.callback = callback;
	}
	
	/*@Override
	public void startCommandProvider(String commandProviderName) {
		try {
			commandProviderRunLock.lock();
			isCommandProviderRun = true;
			commandProviderName2IsReadyMap.put(commandProviderName, true);
			commandProviderRunCondition.signalAll();
		} finally {
			commandProviderRunLock.unlock();
		}
	}*/

	public void setNetWorkClient(NetWorkClient netWorkClient) {
		this.netWorkClient = netWorkClient;
	}
	
	public void setMasterFailureMonitor(MasterFailureMonitor masterFailureMonitor) {
		this.masterFailureMonitor = masterFailureMonitor;
	}
}
