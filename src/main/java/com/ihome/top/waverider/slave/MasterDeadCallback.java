/**
 * waverider
 * 
 */

package com.ihome.top.waverider.slave;

import com.ihome.top.waverider.SlaveWorker;

/**
 * <p>
 * Slave离开回调上层
 * </p>
 * 
 * @author <a href="mailto:sihai@taobao.com">sihai</a>
 *
 */
public interface MasterDeadCallback {
	
	/**
	 * Master挂掉通知
	 */
	void callback();
}
