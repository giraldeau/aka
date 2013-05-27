package org.lttng.studio.model.kernel;

public class Host {

	private final String hostname;
	private final long uuid;

	public Host(long uuid, String hostname) {
		this.hostname = hostname;
		this.uuid = uuid;
	}

	public long getUuid() {
		return uuid;
	}

	public String getHostname() {
		return hostname;
	}

}
