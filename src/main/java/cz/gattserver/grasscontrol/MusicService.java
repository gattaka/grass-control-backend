package cz.gattserver.grasscontrol;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.gattserver.grasscontrol.interfaces.ResultTO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
public class MusicService {

	private final Logger logger = LoggerFactory.getLogger(MusicService.class);

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
		if (path.isEmpty())
			return null;
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
		if (path.isEmpty())
			return Collections.emptyList();
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
		return new ShortItemTO(item.getName(),
				formatPath(item.getPath()), item.isDirectory());
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
					if (dotIndex == -1 || dotIndex == childItem.getName().length())
						return;
					String extension = childItem.getName().substring(dotIndex + 1).toLowerCase();
					if (!acceptedTypes.contains(extension))
						return;
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
		if (item.getName().toLowerCase().contains(searchPhrase))
			results.add(mapAsShort(item));
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
		if (!statusResult.isSuccess())
			return statusResult;

		ResultTO playlistResult = vlcService.sendCommand("playlistResult.json");
		if (!playlistResult.isSuccess())
			return playlistResult;

		JsonObject statusJson = JsonParser.parseString(statusResult.getValue())
				.getAsJsonObject();
		int currentId = statusJson.get("currentplid").getAsInt();

		JsonObject playlistJson = JsonParser.parseString(playlistResult.getValue())
				.getAsJsonObject();
		JsonArray children = playlistJson.get("children").getAsJsonArray();
		JsonArray songs = children.get(0).getAsJsonObject().get("children").getAsJsonArray();
		for (JsonElement song : songs) {
			int id = song.getAsJsonObject().get("id").getAsInt();
			if (id != currentId)
				removeFromPlaylist(id);
		}

		return ResultTO.success("Success", 200);
	}

	public void seek(int position) {
		vlcService.sendCommand("status.json?command=seek&val=" + position);
	}

	public void volume(int position) {
		vlcService.sendCommand("status.json?command=volume&val=" + position);
	}
}