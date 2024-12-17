package cz.gattserver.grasscontrol;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

		// TODO
		//reindex();
	}

	public ItemTO getItem(String path) {
		if (path.isEmpty())
			return null;
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

	public String getStatus() {
		return vlcService.sendCommand("");
	}

	public void play() {
		vlcService.sendCommand("?command=pl_play");
	}

	public void pause() {
		vlcService.sendCommand("?command=pl_pause");
	}

	public void stop() {
		vlcService.sendCommand("?command=pl_stop");
	}

	public void enqueue(String path) {
		ItemTO item = getItem(path);
		if (item.isDirectory()) {
			// TODO rekurzivní rozbalení a enqueue
		} else {
			vlcService.sendCommand("?command=in_enqueue&input=" +
					rootPath.resolve(item.getPath()).resolve(item.getName()));
		}
	}

	public void enqueueAndPlay(String path) {
		enqueue(path);
		play();
	}
}