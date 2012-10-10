package com.ihome.top.waverider.slave;

import org.junit.Test;

import com.ihome.top.waverider.WaveriderBaseTestCase;

public class SlaveNodeTest extends WaveriderBaseTestCase {

	private DefaultSlaveNode slave;
	
	@Test
	public void test() throws Exception {
		slave.init();
		slave.start();
		
		Thread.currentThread().join();
	}
	
	public void setSlave(DefaultSlaveNode slave) {
		this.slave = slave;
	}

}
