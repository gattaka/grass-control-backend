package cz.gattserver.grasscontrol;

import com.google.gson.*;
import cz.gattserver.grasscontrol.interfaces.ResultTO;
import cz.gattserver.grasscontrol.interfaces.TagTO;
import jakarta.annotation.PostConstruct;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
public class MusicService implements ApplicationContextAware {

	private final Logger logger = LoggerFactory.getLogger(MusicService.class);

	private ApplicationContext applicationContext;

	@Autowired
	private VLCService vlcService;

	@Value("${music.path}")
	private String musicPath;

	@Value("${mime.filter}")
	private String mimeFilter;

	private Set<String> acceptedTypes;

	private Path rootPath;
	private List<ItemTO> items;
	private List<ShortItemTO> rootItems;

	@PostConstruct
	public void index() {
		acceptedTypes = new HashSet<>();
		for (String type : mimeFilter.toLowerCase().split(","))
			acceptedTypes.add(type.trim());

		reindex();
	}

	public ItemTO getItem(String path) {
		if (path.isEmpty()) return null;
		if (path.startsWith("/")) path = path.substring(1);
		ItemTO targetItem = null;
		List<ItemTO> list = items;
		for (String part : path.split("/")) {
			for (ItemTO item : list) {
				if (item.getName().equals(part)) {
					targetItem = item;
					list = item.getChildren();
					break;
				}
			}
		}
		return targetItem;
	}

	public List<ShortItemTO> getItems(String path) {
		if (path.isEmpty()) return Collections.emptyList();
		ItemTO targetItem = getItem(path);
		return mapAsShort(targetItem.getChildren());
	}

	public List<ShortItemTO> getRootItems() {
		return rootItems;
	}

	private String formatPath(Path path) {
		List<String> elements = new ArrayList<>();
		for (Path current : path)
			elements.add(current.toString());
		return String.join("/", elements);
	}

	private ShortItemTO mapAsShort(ItemTO item) {
		return new ShortItemTO(item.getName(), formatPath(item.getPath()), item.isDirectory());
	}

	private List<ShortItemTO> mapAsShort(List<ItemTO> items) {
		return items.stream().map(this::mapAsShort).toList();
	}

	public void reindex() {
		rootPath = Paths.get(musicPath);
		ItemTO rootItem = new ItemTO();
		rootItem.setName("");
		rootItem.setPath(Path.of(""));
		try {
			list(rootItem);
			items = rootItem.getChildren();
			rootItems = mapAsShort(rootItem.getChildren());
		} catch (IOException e) {
			logger.error("Nezdařilo se naindexovat kořenový adresář {}, {}", musicPath, e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private void list(ItemTO parentItem) throws IOException {
		Path parentPath = parentItem.getPath().resolve(parentItem.getName());
		Path parentFullPath = rootPath.resolve(parentPath);
		try (Stream<Path> files = Files.list(parentFullPath)) {
			files.forEach(f -> {
				ItemTO childItem = new ItemTO();
				childItem.setName(f.getFileName().toString());
				childItem.setPath(parentPath);
				childItem.setParent(parentItem);

				Path childFullPath = parentFullPath.resolve(childItem.getName());

				logger.info("Indexuji: {}", childFullPath);

				if (Files.isDirectory(childFullPath)) {
					childItem.setDirectory(true);
					try {
						list(childItem);
					} catch (IOException e) {
						logger.warn("Nezdařilo se naindexovat adresář {}, {}", childFullPath, e.getMessage(), e);
					}
				} else {
					int dotIndex = childItem.getName().lastIndexOf(".");
					if (dotIndex == -1 || dotIndex == childItem.getName().length()) return;
					String extension = childItem.getName().substring(dotIndex + 1).toLowerCase();
					if (!acceptedTypes.contains(extension)) return;
				}

				childItem.setParent(parentItem);
				parentItem.getChildren().add(childItem);
			});
		}
	}

	public List<ShortItemTO> getItemsBySearch(String searchPhrase) {
		List<ShortItemTO> results = new ArrayList<>();
		ItemTO rootItem = new ItemTO();
		rootItem.setName("");
		rootItem.setChildren(items);
		search(searchPhrase.toLowerCase(), rootItem, results);
		return results;
	}

	private void search(String searchPhrase, ItemTO item, List<ShortItemTO> results) {
		if (item.getName().toLowerCase().contains(searchPhrase)) results.add(mapAsShort(item));
		for (ItemTO childItem : item.getChildren())
			search(searchPhrase, childItem, results);
	}

	public ResultTO getStatus() {
		return vlcService.sendCommand("status.json");
	}

	public ResultTO getPlaylist() {
		return vlcService.sendCommand("playlist.json");
	}

	public void play() {
		vlcService.sendCommand("status.json?command=pl_play");
	}

	public void pause() {
		vlcService.sendCommand("status.json?command=pl_pause");
	}

	public void stop() {
		vlcService.sendCommand("status.json?command=pl_stop");
	}

	public void previous() {
		vlcService.sendCommand("status.json?command=pl_previous");
	}

	public void next() {
		vlcService.sendCommand("status.json?command=pl_next");
	}

	public void loop() {
		vlcService.sendCommand("status.json?command=pl_loop");
	}

	public void random() {
		vlcService.sendCommand("status.json?command=pl_random");
	}

	// Když se do vlc přidá 1 soubor a 1 adresář, VLC mězi ně pro shuffle rozdělí pravděpodobnost 50:50 na spuštění --
	// dokud nepadne adresář, "nerozbalí" ho na jednotlivé soubory a mezi ně znovu rozpočítá pravděpodobnost. První
	// soubor se tedy spouští často opakovaně než losování konečně padne na adresář
	private void preparePath(ItemTO item, Consumer<String> consumer) {
		if (item.isDirectory()) {
			for (ItemTO child : item.getChildren())
				preparePath(child, consumer);
		} else {
			String param = rootPath.resolve(item.getPath()).resolve(item.getName()).toString();
			param = "file:///" + URLEncoder.encode(param, StandardCharsets.UTF_8);
			// VLC má vadu a nebere URL mezery jako '+', zvládá jen '%20'
			param = param.replace("+", "%20");
			consumer.accept(param);
		}
	}

	public void enqueue(String path) {
		preparePath(getItem(path), s -> vlcService.sendCommand("status.json?command=in_enqueue&input=" + s));
	}

	public void enqueueAndPlay(String path) {
		preparePath(getItem(path), s -> vlcService.sendCommand("status.json?command=in_play&input=" + s));
	}

	public void playFromPlaylist(int id) {
		vlcService.sendCommand("status.json?command=pl_play&id=" + id);
	}

	public void removeFromPlaylist(int id) {
		vlcService.sendCommand("status.json?command=pl_delete&id=" + id);
	}

	public void emptyPlaylist() {
		vlcService.sendCommand("status.json?command=pl_empty");
	}

	public ResultTO emptyPlaylistExceptPlaying() {
		ResultTO statusResult = vlcService.sendCommand("status.json");
		if (!statusResult.isSuccess()) return statusResult;

		ResultTO playlistResult = getPlaylist();
		if (!playlistResult.isSuccess()) return playlistResult;

		JsonObject statusJson = JsonParser.parseString(statusResult.getValue()).getAsJsonObject();
		int currentId = statusJson.get("currentplid").getAsInt();

		JsonObject playlistJson = JsonParser.parseString(playlistResult.getValue()).getAsJsonObject();
		JsonArray children = playlistJson.get("children").getAsJsonArray();
		JsonArray songs = children.get(0).getAsJsonObject().get("children").getAsJsonArray();
		for (JsonElement song : songs) {
			int id = song.getAsJsonObject().get("id").getAsInt();
			if (id != currentId) removeFromPlaylist(id);
		}

		return ResultTO.success("Success", 200);
	}

	public void seek(int position) {
		vlcService.sendCommand("status.json?command=seek&val=" + position);
	}

	public void volume(int position) {
		vlcService.sendCommand("status.json?command=volume&val=" + position);
	}

	private String getUriById(int id) {
		ResultTO playlistResult = getPlaylist();
		if (!playlistResult.isSuccess()) throw new IllegalStateException("Nezdařilo se získat VLC playlist");

		JsonObject playlistJson = JsonParser.parseString(playlistResult.getValue()).getAsJsonObject();
		JsonArray children = playlistJson.get("children").getAsJsonArray();
		JsonArray songs = children.get(0).getAsJsonObject().get("children").getAsJsonArray();
		for (JsonElement song : songs) {
			JsonObject songObject = song.getAsJsonObject();
			if (songObject.get("id").getAsInt() == id) {
				String uri = songObject.get("uri").getAsString();
				return uri;
			}
		}

		throw new IllegalStateException("Nezdařilo se nalézt položku playlistu s id " + id);
	}

	private File getFileByVLCUri(String uri) {
		String prefix = "file:///";
		if (uri.startsWith(prefix)) uri = uri.substring(prefix.length());
		return new File(uri);
	}

	public TagTO readTag(int id) {
		String uri = getUriById(id);
		File file = getFileByVLCUri(uri);

		try {
			// https://www.jthink.net/jaudiotagger/examples_read.jsp
			AudioFile f = AudioFileIO.read(file);
			Tag tag = f.getTag();

			TagTO tagTO = new TagTO();

			if (tag != null) {
				tagTO.setTitle(tag.getFirst(FieldKey.TITLE));
				tagTO.setArtist(tag.getFirst(FieldKey.ARTIST));
				tagTO.setAlbum(tag.getFirst(FieldKey.ALBUM));
				tagTO.setYear(tag.getFirst(FieldKey.YEAR));
				tagTO.setTrack(tag.getFirst(FieldKey.TRACK));
				tagTO.setComposer(tag.getFirst(FieldKey.COMPOSER));
			}

			return tagTO;
		} catch (Exception e) {
			if (e instanceof InvalidAudioFrameException) return new TagTO();
			throw new RuntimeException(e);
		}
	}

	private void writeTagWithJaudiotagger(File file, TagTO tagTO) throws CannotReadException, TagException,
			InvalidAudioFrameException, ReadOnlyFileException, IOException, CannotWriteException {
		// https://www.jthink.net/jaudiotagger/examples.jsp
		AudioFile f = AudioFileIO.read(file);
		Tag tag = f.getTagOrCreateDefault();
		f.setTag(tag);

		if (tagTO.getAlbum() != null)
			tag.setField(FieldKey.ALBUM, tagTO.getAlbum());
		if (tagTO.getArtist() != null)
			tag.setField(FieldKey.ARTIST, tagTO.getArtist());
		if (tagTO.getYear() != null)
			tag.setField(FieldKey.YEAR, tagTO.getYear());
		if (tagTO.getTrack() != null)
			tag.setField(FieldKey.TRACK, tagTO.getTrack());
		if (tagTO.getComposer() != null)
			tag.setField(FieldKey.COMPOSER, tagTO.getComposer());
		if (tagTO.getTitle() != null)
			tag.setField(FieldKey.TITLE, tagTO.getTitle());

		f.commit();
	}

	public void writeTag(int id, TagTO tag) {
		String uri = getUriById(id);
		File file = getFileByVLCUri(uri);

		if (!file.exists())
			throw new IllegalStateException("Soubor neexistuje");

		try {
			writeTagWithJaudiotagger(file, tag);
		} catch (Exception e) {
			throw new RuntimeException("Problém se zápisem souboru", e);
		}
	}

	// https://www.baeldung.com/spring-boot-shutdown
	public void shutdown() {
		int exitCode = SpringApplication.exit(applicationContext, () -> 0);
		System.exit(exitCode);
	}

	public void startVLC() {
		String command = "start vlc";
		ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
		try {
			pb.start();
		} catch (IOException e) {
			logger.error("Command '" + command + "' failed", e);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}