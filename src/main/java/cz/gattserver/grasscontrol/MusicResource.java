package cz.gattserver.grasscontrol;

import cz.gattserver.grasscontrol.interfaces.ResultTO;
import cz.gattserver.grasscontrol.interfaces.TagTO;
import cz.gattserver.grasscontrol.interfaces.VersionTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import java.net.URLDecoder;
import java.util.List;

@RequestMapping("api")
@RestController
public class MusicResource {

	@Autowired
	private MusicService musicService;

	private String getCleanDynamicPath(HttpServletRequest request) {
		String pattern = (String) request.getAttribute(
				HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		String restOfTheUrl = (String) request.getAttribute(
				HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		// -2 kvůli **
		String rawURL = restOfTheUrl.substring(pattern.length() - 2);
		return URLDecoder.decode(rawURL);
	}

	@GetMapping(value = "/version")
	VersionTO version() {
		return new VersionTO("2.0");
	}

	@GetMapping(value = "/reindex")
	void reindex() {
		musicService.index();
	}

	@GetMapping(path = "/search/{searchPhrase}")
	List<ShortItemTO> search(@PathVariable String searchPhrase) {
		return musicService.getItemsBySearch(searchPhrase);
	}

	/**
	 * Parametrem je escapovaná cesta, která ale kvůli Tomcatu nesmí mít escapované znaky lomítek (/ = %2F)
	 */
	@GetMapping(path = "/enqueue/**")
	void enqueue(HttpServletRequest request) {
		musicService.enqueue(getCleanDynamicPath(request));
	}

	@GetMapping(path = "/seek/{position}")
	void seek(@PathVariable int position) {
		musicService.seek(position);
	}

	@GetMapping(path = "/volume/{position}")
	void volume(@PathVariable int position) {
		musicService.volume(position);
	}

	/**
	 * Parametrem je escapovaná cesta, která ale kvůli Tomcatu nesmí mít escapované znaky lomítek (/ = %2F)
	 */
	@GetMapping(path = "/enqueue-and-play/**")
	void enqueueAndPlay(HttpServletRequest request) {
		musicService.enqueueAndPlay(getCleanDynamicPath(request));
	}

	@GetMapping(path = "/read-tag/{id}")
	TagTO readTag(@PathVariable int id) {
		return musicService.readTag(id);
	}

	@PutMapping(path = "/write-tag/{id}")
	void writeTag(@RequestBody TagTO tag, @PathVariable int id) {
		musicService.writeTag(id, tag);
	}

	@GetMapping(value = "/status")
	String status(HttpServletResponse response) {
		ResultTO resultTO = musicService.getStatus();
		if (resultTO.isSuccess())
			return resultTO.getValue();
		response.setStatus(resultTO.getStatus());
		return resultTO.getMessage();
	}

	@GetMapping(value = "/start-vlc")
	void startVLC() {
		musicService.startVLC();
	}

	@GetMapping(value = "/shutdown")
	void shutdown() {
		musicService.shutdown();
	}

	@GetMapping(value = "/playlist")
	String playlist(HttpServletResponse response) {
		ResultTO resultTO = musicService.getPlaylist();
		if (resultTO.isSuccess())
			return resultTO.getValue();
		response.setStatus(resultTO.getStatus());
		return resultTO.getMessage();
	}

	@GetMapping(path = "/play-from-playlist/{id}")
	void playFromPlaylist(@PathVariable int id) {
		musicService.playFromPlaylist(id);
	}

	@GetMapping(path = "/remove-from-playlist/{id}")
	void removeFromPlaylist(@PathVariable int id) {
		musicService.removeFromPlaylist(id);
	}

	@GetMapping(value = "/empty-playlist")
	void emptyPlaylist() {
		musicService.emptyPlaylist();
	}

	@GetMapping(value = "/empty-playlist-except-playing")
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

	/**
	 * Parametrem je escapovaná cesta, která ale kvůli Tomcatu nesmí mít escapované znaky lomítek (/ = %2F)
	 */
	@GetMapping(path = "/list/**")
	List<ShortItemTO> list(HttpServletRequest request) {
		return musicService.getItems(getCleanDynamicPath(request));
	}

	@GetMapping(path = "/list-all")
	List<ShortItemTO> listAll() {
		return musicService.getRootItems();
	}
}