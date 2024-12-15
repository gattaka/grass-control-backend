package cz.gattserver.grasscontrol;

public class ShortItemTO {

	private String name;
	private String path;
	private boolean directory;

	public ShortItemTO(String name, String path, boolean directory) {
		this.name = name;
		this.path = path;
		this.directory = directory;
	}

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

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
