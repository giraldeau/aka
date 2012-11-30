package org.lttng.studio.model.kernel;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class Inet4Sock {

	private long sk;
	protected int saddr;
	protected int daddr;
	protected int sport;
	protected int dport;
	private boolean isSet;
	private long startTime;
	private long endTime;
	// FIXME: Add create timestamp to make it uniq

	public Inet4Sock(long sk) {
		this.sk = sk;
		this.isSet = false;
	}
	public Inet4Sock() {
		this(0);
	}
	public long getSk() {
		return sk;
	}

	public void setSk(long sk) {
		this.sk = sk;
	}

	public void setInet(int saddr, int daddr, int sport, int dport) {
		this.saddr = saddr;
		this.daddr = daddr;
		this.sport = sport;
		this.dport = dport;
		isSet = true;
	}

	public void setInet(Inet4Sock sock) {
		this.saddr = sock.saddr;
		this.daddr = sock.daddr;
		this.sport = sock.sport;
		this.dport = sock.dport;
		isSet = true;
	}

	public boolean isComplement(Inet4Sock other) {
		if (other == null)
			return false;
		if (this.isSet && other.isSet &&
				this.daddr == other.saddr && this.saddr == other.daddr &&
				this.sport == other.dport && this.dport == other.sport)
			return true;
		return false;
	}

	public boolean isSame(Inet4Sock other) {
		if (other == null)
			return false;
		if (this.isSet && other.isSet &&
				this.saddr == other.saddr && this.daddr == other.daddr &&
				this.sport == other.sport && this.dport == other.dport)
			return true;
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (obj instanceof Inet4Sock) {
			Inet4Sock other = (Inet4Sock) obj;
			if (this.sk == other.sk)
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		HashFunction hf = Hashing.goodFastHash(32);
		Hasher hasher = hf.newHasher()
				.putLong(sk);
		return hasher.hash().asInt();
	}

	@Override
	public String toString() {
		return String.format("[0x%s, %s:%d => %s:%d]", Long.toHexString(sk),
				ipquad(saddr), sport, ipquad(daddr), dport);
	}
	private String ipquad(int ip) {
		byte[] a = new byte[] {
				(byte) (ip),
				(byte) (ip << 8),
				(byte) (ip << 16),
				(byte) (ip << 24)
		};
		return String.format("%d.%d.%d.%d", a[0], a[1], a[2], a[3]);
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

}
