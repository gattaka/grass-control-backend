package cz.gattserver.grasscontrol;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.function.Function;

@Service
public class VLCService {

	private static final Logger logger = LoggerFactory.getLogger(VLCService.class);

	@Value("${vlc.url}")
	private String vlcURL;

	@Value("${vlc.pass}")
	private String vlcPass;

	public String sendCommand(String command) {
		try {
			// Create the URL object
			URL url = new URL(vlcURL + "/requests/" + command);

			// Open connection to the URL
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			// Prepare the Basic Authentication header
			String user = "";
			String auth = user + ":" + vlcPass;
			String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
			connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

			// Set request method
			connection.setRequestMethod("GET"); // You can use POST, PUT, DELETE, etc. depending on the API

			// Get the response code
			int responseCode = connection.getResponseCode();

			// Read the response if needed
			if (responseCode == HttpURLConnection.HTTP_OK) { // 200 OK
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();

				// Output the response
				return response.toString();
			} else {
				logger.warn("VLC connection failed with code {}", responseCode);
			}
		} catch (IOException e) {
			logger.error("VLC connection failed with exception", e);
		}
		throw new RuntimeException("VLC connection failed");
	}
}