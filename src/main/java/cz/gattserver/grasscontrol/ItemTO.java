package cz.gattserver.grasscontrol;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ItemTO {

	private String name;
	private Path path;
	private ItemTO parent;
	private boolean directory;
	private List<ItemTO> children = new ArrayList<>();

	public boolean isDirectory() {
		return directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public ItemTO getParent() {
		return parent;
	}

	public void setParent(ItemTO parent) {
		this.parent = parent;
	}

	public List<ItemTO> getChildren() {
		return children;
	}

	public void setChildren(List<ItemTO> children) {
		this.children = children;
	}
}
