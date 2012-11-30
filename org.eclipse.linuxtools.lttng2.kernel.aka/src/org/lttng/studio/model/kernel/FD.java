package org.lttng.studio.model.kernel;

public class FD {

	private long num;
	private String name;

	public FD(long num, String name) {
		setNum(num);
		setName(name);
	}

	public FD(long num) {
		this(num, null);
	}

	public FD(FD fd) {
		this(fd.getNum(), fd.getName());
	}

	public long getNum() {
		return num;
	}

	public void setNum(long num) {
		this.num = num;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
