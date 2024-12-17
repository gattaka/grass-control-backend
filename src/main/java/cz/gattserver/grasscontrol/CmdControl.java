package cz.gattserver.grasscontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public enum CmdControl {

	INSTANCE;

	private static final Logger logger = LoggerFactory.getLogger(CmdControl.class);

	public static void openVLC() {
		String command = "start vlc";
		ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
		try {
			pb.start();
		} catch (IOException e) {
			logger.error("Command '" + command + "' failed", e);
		}
	}
}