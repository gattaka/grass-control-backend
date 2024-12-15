package cz.gattserver.grasscontrol;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class MusicService {

	private final Logger logger = LoggerFactory.getLogger(MusicService.class);

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

	public List<ShortItemTO> getItems() {
		return rootItems;
	}

	public void reindex() {
		rootPath = Paths.get(musicPath);
		ItemTO rootItem = new ItemTO();
		rootItem.setPath(Path.of(""));
		try {
			list(rootItem);
			items = rootItem.getChildren();
			rootItems = new ArrayList<>();
			rootItem.getChildren().forEach(item -> rootItems.add(new ShortItemTO(item.getName(),
					item.getPath().toString(), item.isDirectory())));
		} catch (IOException e) {
			logger.error("Nezdařilo se naindexovat kořenový adresář " + musicPath, e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private void list(ItemTO item) throws IOException {
		Files.list(rootPath.resolve(item.getPath())).forEach(f -> {
			ItemTO childItem = new ItemTO();
			childItem.setName(f.getFileName().toString());
			childItem.setPath(item.getPath().resolve(childItem.getName()));

			Path fullPath = rootPath.resolve(childItem.getPath());

			logger.info("Indexuji: " + fullPath);

			if (Files.isDirectory(fullPath)) {
				childItem.setDirectory(true);
				try {
					list(childItem);
				} catch (IOException e) {
					logger.warn("Nezdařilo se naindexovat adresář " + fullPath, e.getMessage(), e);
				}
			} else {
				int dotIndex = childItem.getName().lastIndexOf(".");
				if (dotIndex == -1 || dotIndex == childItem.getName().length())
					return;
				String extension = childItem.getName().substring(dotIndex + 1).toLowerCase();
				if (!acceptedTypes.contains(extension))
					return;
			}

			childItem.setParent(item);
			item.getChildren().add(childItem);
		});
	}
}
