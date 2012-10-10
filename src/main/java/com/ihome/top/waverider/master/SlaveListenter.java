/**
 * waverider
 * 
 */

package com.ihome.top.waverider.master;

import com.ihome.top.waverider.SlaveWorker;

/**
 * <p>
 * Slave离开回调上层
 * </p>
 * 
 * @author <a href="mailto:sihai@taobao.com">sihai</a>
 *
 */
public interface SlaveListenter {
	
	/**
	 * 指定的Slave加入
	 * @param slave
	 */
	void joined(SlaveWorker slave);
	
	/**
	 * 指定Slave离开
	 * @param slave
	 */
	void left(SlaveWorker slave);
}
