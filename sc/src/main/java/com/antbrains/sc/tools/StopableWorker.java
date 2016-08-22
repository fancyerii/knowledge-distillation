package com.antbrains.sc.tools;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class StopableWorker implements Stoppable {
	protected volatile boolean bStop = false;
	private CountDownLatch latch = new CountDownLatch(1);

	public void stopMe() {
		bStop = true;
	}

	/**
	 * 完成后一定记得调用该方法
	 */
	public void finished() {
		latch.countDown();
	}

	public void waitFinish(long waitTime) {
		try {
			if (waitTime <= 0)
				latch.await();
			else
				latch.await(waitTime, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {

		}
	}
}
