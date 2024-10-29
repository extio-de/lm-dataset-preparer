package de.extio.lmdatasetprep;

import java.util.Random;
import java.util.UUID;

@SuppressWarnings("serial")
public final class XorShift128Random extends Random {
	
	private long state0;
	
	private long state1;
	
	public XorShift128Random() {
		super();
	}
	
	public XorShift128Random(final long seed) {
		this.setSeed(seed);
	}
	
	public XorShift128Random(final String state) {
		final UUID uuid = UUID.fromString(state);
		this.state0 = uuid.getMostSignificantBits();
		this.state1 = uuid.getLeastSignificantBits();
	}
	
	@Override
	public synchronized void setSeed(final long seed) {
		this.state0 = avalanche(seed == 0 ? -1 : seed);
		this.state1 = avalanche(this.state0);
		this.state0 = avalanche(this.state1);
	}
	
	@Override
	protected int next(final int bits) {
		return (int) (this.nextLong() & (1L << bits) - 1);
	}
	
	@Override
	public long nextLong() {
		long s1 = this.state0;
		final long s0 = this.state1;
		this.state0 = s0;
		s1 ^= s1 << 23;
		return (this.state1 = s1 ^ s0 ^ (s1 >>> 17) ^ (s0 >>> 26)) + s0;
	}
	
	@Override
	public String toString() {
		return new UUID(this.state0, this.state1).toString();
	}
	
	private static long avalanche(long k) {
		k ^= k >>> 33;
		k *= 0xff51afd7ed558ccdL;
		k ^= k >>> 33;
		k *= 0xc4ceb9fe1a85ec53L;
		k ^= k >>> 33;
		
		return k;
	}
}
