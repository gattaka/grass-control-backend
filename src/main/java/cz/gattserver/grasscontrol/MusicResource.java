package cz.gattserver.grasscontrol;

import cz.gattserver.grasscontrol.interfaces.VersionTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RequestMapping("api")
@RestController
public class MusicResource {

	@Autowired
	private MusicService musicService;

	@GetMapping(value = "/version")
	VersionTO version() {
		return new VersionTO("1.0");
	}

	@GetMapping(value = "/reindex")
	void reindex() {
		musicService.index();
	}

	@GetMapping(value = "/list")
	List<ShortItemTO> list() {
		return musicService.getItems();
	}
}