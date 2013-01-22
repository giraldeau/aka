package org.lttng.studio.model.kernel;

public class HRTimer {

	public enum HRTimerState {
		INIT, START, CANCEL
	}
	
	private long id;
	private HRTimerState state;
	
	public HRTimer() {
		this(0, HRTimerState.INIT);
	}
	
	public HRTimer(long id, HRTimerState state) {
		setId(id);
		setState(state);
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public HRTimerState getState() {
		return state;
	}
	public void setState(HRTimerState state) {
		this.state = state;
	}
	
}
