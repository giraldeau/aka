package org.lttng.studio.model.kernel;

import java.util.UUID;

public class Host {

	private final String hostname;
	private final UUID uuid;

	public Host(UUID uuid, String hostname) {
		this.hostname = hostname;
		this.uuid = uuid;
	}

	public Host(String uuidStr, String hostname) {
		this(UUID.fromString(uuidStr), hostname);
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getHostname() {
		return hostname;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other instanceof Host) {
			return ((Host) other).uuid.equals(this.uuid);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

}
