package cz.gattserver.grasscontrol;

import cz.gattserver.grasscontrol.interfaces.VersionTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class MusicResource {

	@Value("${music.path}")
	private String musicPath;

	@GetMapping(value = "/version")
	VersionTO version() {
		return new VersionTO("1.0");
	}

}