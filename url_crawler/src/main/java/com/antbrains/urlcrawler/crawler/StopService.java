package com.antbrains.urlcrawler.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

public class StopService extends Thread {
	protected static Logger logger = Logger.getLogger(StopService.class);
	private int port;
	private Stoppable stoppable;
	private long waitTime;

	public StopService(int port, Stoppable stoppable, long waitTime) {
		this.port = port;
		this.stoppable = stoppable;
		this.waitTime = waitTime;
	}

	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			logger.info("stopMe at port: " + port);
			while (true) {
				Socket socket = serverSocket.accept();
				InputStream sin = socket.getInputStream();
				BufferedReader inputReader = new BufferedReader(new InputStreamReader(sin, "UTF8"));
				OutputStream sout = socket.getOutputStream();
				BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(sout, "UTF8"));
				String cmd = inputReader.readLine();
				if (cmd.equals("stop")) {
					if (stoppable != null) {
						stoppable.stopMe();
						stoppable.waitFinish(waitTime);
					}
					outputWriter.write("stop success\n");
					outputWriter.flush();
					sin.close();
					sout.close();
					break;
				} else {
					outputWriter.write("unknown command: " + cmd + "\n");
					outputWriter.flush();
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {

					}
					sin.close();
					sout.close();
				}
			}

			serverSocket.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}
}
