package cz.gattserver.grasscontrol.interfaces;

public class ResultTO {

	private String value = null;
	private int status;
	private String message;
	private boolean success;

	private ResultTO() {
	}

	public static ResultTO success(String value, int code) {
		ResultTO result = new ResultTO();
		result.value = value;
		result.status = code;
		result.success = true;
		return result;
	}

	public static ResultTO fail(int code, String message) {
		ResultTO result = new ResultTO();
		result.status = code;
		result.message = message;
		result.success = false;
		return result;
	}

	public String getValue() {
		return value;
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public boolean isSuccess() {
		return success;
	}
}