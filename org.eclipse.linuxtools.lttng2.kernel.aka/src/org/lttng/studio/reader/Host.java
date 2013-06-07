package org.lttng.studio.reader;

import java.util.UUID;

public class Host {

	private final UUID uuid;
	private final String hostname;
	private final int cpus;

	public Host(UUID uuid, String hostname, int cpus) {
		this.uuid = uuid;
		this.hostname = hostname;
		this.cpus = cpus;
	}

	public Host(String uuidStr, String hostname, int cpus) {
		this(UUID.fromString(uuidStr), hostname, cpus);
	}

	public UUID getUUID() {
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
			return this.uuid.equals(((Host) other).getUUID());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public String toString() {
		return "[" + uuid.toString() + "," + hostname + "," + getNumCpus() + "]";
	}

	public int getNumCpus() {
		return cpus;
	}

}
