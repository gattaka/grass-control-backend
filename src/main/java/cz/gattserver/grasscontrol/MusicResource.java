package cz.gattserver.grasscontrol;

import cz.gattserver.grasscontrol.interfaces.VersionTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
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

	@GetMapping(value = "/search")
	List<ShortItemTO> search(@Nullable String searchPhrase) {
		if (searchPhrase == null || searchPhrase.isEmpty())
			return musicService.getRootItems();
		return musicService.getItemsBySearch(searchPhrase);
	}

	@GetMapping(value = "/enqueue")
	void enqueue(@Nullable String path) {
		if (path != null && !path.isEmpty())
			musicService.enqueue(path);
	}

	@GetMapping(value = "/seek")
	void seek(@Nullable int position) {
			musicService.seek(position);
	}

	@GetMapping(value = "/volume")
	void volume(@Nullable int position) {
		musicService.volume(position);
	}

	@GetMapping(value = "/enqueue-and-play")
	void enqueueAndPlay(@Nullable String path) {
		if (path != null && !path.isEmpty())
			musicService.enqueueAndPlay(path);
	}

	@GetMapping(value = "/status")
	String status() {
		return musicService.getStatus();
	}

	@GetMapping(value = "/playlist")
	String playlist() {
		return musicService.getPlaylist();
	}

	@GetMapping(value = "/playFromPlaylist")
	void playFromPlaylist(int id) {
		musicService.playFromPlaylist(id);
	}

	@GetMapping(value = "/removeFromPlaylist")
	void removeFromPlaylist(int id) {
		musicService.removeFromPlaylist(id);
	}

	@GetMapping(value = "/emptyPlaylist")
	void emptyPlaylist() {
		musicService.emptyPlaylist();
	}

	@GetMapping(value = "/emptyPlaylistExceptPlaying")
	void emptyPlaylistExceptPlaying() {
		musicService.emptyPlaylistExceptPlaying();
	}

	@GetMapping(value = "/play")
	void play() {
		musicService.play();
	}

	@GetMapping(value = "/pause")
	void pause() {
		musicService.pause();
	}

	@GetMapping(value = "/stop")
	void stop() {
		musicService.stop();
	}

	@GetMapping(value = "/previous")
	void previous() {
		musicService.previous();
	}

	@GetMapping(value = "/next")
	void next() {
		musicService.next();
	}

	@GetMapping(value = "/loop")
	void loop() {
		musicService.loop();
	}

	@GetMapping(value = "/random")
	void random() {
		musicService.random();
	}

	@GetMapping(value = "/list")
	List<ShortItemTO> list(@Nullable String path) {
		if (path == null || path.isEmpty()) {
			// http://localhost:8080/api/list
			return musicService.getRootItems();
		} else {
			// http://localhost:8080/api/list?path=2000s
			return musicService.getItems(path);
		}
	}
}