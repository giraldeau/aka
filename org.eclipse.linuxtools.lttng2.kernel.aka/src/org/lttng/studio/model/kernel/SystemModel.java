package org.lttng.studio.model.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.lttng.studio.model.kernel.InterruptContext.Context;
import org.lttng.studio.reader.TraceReader;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class SystemModel implements ITraceModel {



	private Task[] swappers;
	private HashMap<Long, Task> tasks; // (tid, task)
	//private Table<Long, Long, FD> fdsTable; // (pid, id, fd)
	private HashMap<Task, FDSet> taskFdSet;
	private BiMap<Inet4Sock, Inet4Sock> sockPeer; // (sock1, sock2) where sock1.isComplement(sock2)
	private Multimap<Task, Inet4Sock> taskSock; // (sock, task) fast lookup
	private long[] current;			// (cpu, tid)
	private int numCpus;
	private boolean isInitialized = false;
	private HashMap<Long, HRTimer> hrtimers;
	private int switchUnkownTask;
	private int dupUnkownFD;
	private int cpu;
	private ArrayList<Stack<InterruptContext>> context; // per-cpu interrupt context stack

	public SystemModel() {
	}

	@Override
	public void init(TraceReader reader) {
		if (isInitialized == false){
			// FIXME: should avoid the model coupling with tracing classes
			switchUnkownTask = 0;
			dupUnkownFD = 0;
			numCpus = reader.getNumCpus();
			tasks = new HashMap<Long, Task>();
			taskFdSet = new HashMap<Task, FDSet>();
			sockPeer = HashBiMap.create();
			taskSock = HashMultimap.create();
			hrtimers = new HashMap<Long, HRTimer>();
			current = new long[numCpus];
			// Swapper task is always present on each CPU
			swappers = new Task[numCpus];
			for (int i = 0; i < numCpus; i++) {
				Task swapper = new Task();
				swapper.setName("swapper/" + i);
				swapper.setPid(0);
				swapper.setTid(0);
				swapper.setPpid(0);
				swappers[i] = swapper;
			}
			// init context
			context = new ArrayList<Stack<InterruptContext>>();
			for (int i = 0; i < numCpus; i++) {
				Stack<InterruptContext> stack = new Stack<InterruptContext>();
				stack.push(new InterruptContext(null, Context.NONE));
				context.add(stack);
			}
			cpu = 0;
		}
		isInitialized = true;
	}

	public Collection<Task> getTasks() {
		return tasks.values();
	}

	public long getCurrentTid(int cpu) {
		return current[cpu];
	}

	public HashMap<Long, HRTimer> getHRTimers() {
		return hrtimers;
	}

	public void setCurrentTid(int cpu, long tid) {
		current[cpu] = tid;
	}

	public void putTask(Task task) {
		tasks.put(task.getTid(), task);
		// prepare fdset
		if (task.isThreadGroupLeader()) {
			taskFdSet.put(task, new FDSet());
		} else {
			Task leader = tasks.get(task.getPid());
			FDSet fds = taskFdSet.get(leader);
			taskFdSet.put(task, fds);
		}
	}

	public Task getTask(long tid) {
		if (tid > 0)
			return tasks.get(tid);
		if (tid == 0)
			return swappers[cpu];
		return null;
	}

	public Task getTaskCpu(int cpu) {
		long currentTid = getCurrentTid(cpu);
		return getTask(currentTid);
	}

	public int getNumCpus() {
		return numCpus;
	}

	public int getCurrentCPU() {
		return cpu;
	}

	public void setCurrentCPU(int cpu) {
		this.cpu = cpu;
	}

	@Override
	public void reset() {
		isInitialized = false;
	}

	/*
	 * FIXME: there seems between FD and Sock
	 * Opportunity to simplify into object with some id per pid
	 */

	/*
	 * FDs management
	 */
	public void addTaskFD(Task task, FD fd) {
		if (task == null)
			return;
		FDSet fdSet = taskFdSet.get(task);
		if (fdSet == null) {
			fdSet = new FDSet();
			taskFdSet.put(task, fdSet);
		}
		fdSet.addFD(fd);
	}

	private FD removeTaskFD(Task task, FD ofd) {
		FDSet fdSet = taskFdSet.get(task);
		if (fdSet != null)
			return fdSet.remove(ofd);
		return null;
	}

	public FD getFD(Task task, long num) {
		return taskFdSet.get(task).getFD(num);
	}

	public Collection<FD> getFDs() {
		HashSet<FD> set = new HashSet<FD>();
		for (FDSet fds: taskFdSet.values()) {
			set.addAll(fds.getFDs());
		}
		return set;
	}

	public void dup2FD(Task task, long oldfd, long newfd) {
		// dup2 does nothing if oldfd == newfd
		if (oldfd == newfd)
			return;
		// Copy oldfd, assign newfd
		FD ofd = getFD(task, oldfd);
		String name = null;
		if (ofd == null) {
			//System.err.println("WARNING: dup2 of unkown fd " + task + " " + oldfd + " " + newfd);
			dupUnkownFD++;
		} else {
			name = ofd.getName();
		}
		FD nfd = new FD(newfd, name);
		removeTaskFD(task, ofd);
		addTaskFD(task, nfd);
		//System.out.println("dup2 " + task + " " + ofd + " " + nfd);
		// TODO: manage sock relationship

	}

	public FDSet getFDSet(Task task) {
		FDSet set = taskFdSet.get(task);
		if (set == null) {
			set = new FDSet();
			taskFdSet.put(task, set);
		}
		return set;
	}

	public void setTaskFDSet(Task task, FDSet childFDs) {
		taskFdSet.put(task, childFDs);
	}

	/*
	 * Socks management
	 */
	public boolean addInetSock(Task task, Inet4Sock sock) {
		return taskSock.put(task, sock);
	}

	public boolean removeInetSock(Task task, Inet4Sock sock) {
		return taskSock.remove(task, sock);
	}

	public boolean removeInetSock(Inet4Sock sock) {
		Task owner = getInetSockTaskOwner(sock);
		return taskSock.remove(owner, sock);
	}

	public boolean removeInetSock(long sk) {
		Inet4Sock sock = getInetSock(sk);
		Task owner = getInetSockTaskOwner(sock);
		return taskSock.remove(owner, sock);
	}

	public Inet4Sock getInetSock(Task task, long sk) {
		Collection<Inet4Sock> set = taskSock.get(task);
		for (Inet4Sock sock: set) {
			if (sock.getSk() == sk)
				return sock;
		}
		return null;
	}

	public Inet4Sock getInetSock(long sk) {
		Collection<Inet4Sock> set = taskSock.values();
		for (Inet4Sock sock: set) {
			if (sock.getSk() == sk)
				return sock;
		}
		return null;
	}

	public Collection<Inet4Sock> getInetSocks() {
		return taskSock.values();
	}

	public BiMap<Inet4Sock, Inet4Sock> getInetSockIndex() {
		return sockPeer;
	}

	public Task getInetSockTaskOwner(Inet4Sock sock) {
		for (Task task: taskSock.keySet()) {
			if (taskSock.get(task).contains(sock))
				return task;
		}
		return null;
	}


	public Task getInetSockTaskOwner(long sk) {
		Inet4Sock sock = getInetSock(sk);
		return getInetSockTaskOwner(sock);
	}

	public void matchPeer(Inet4Sock sock) {
		if (sockPeer.containsKey(sock) || sockPeer.containsValue(sock))
			return;
		for (Inet4Sock peer: taskSock.values()) {
			if (peer.isComplement(sock)) {
				sockPeer.put(peer, sock);
				break;
			}
		}
	}

	public Set<Task> getTaskByNameSuffix(String name) {
		Set<Task> set = new HashSet<Task>();
		for (Task task: tasks.values()) {
			if (task.getName().endsWith(name)) {
				set.add(task);
			}
		}
		return set;
	}

	/*
	public void setInetSockFd(Inet4Sock sock, FD fd) {
		sockFd.put(sock, fd);
	}
	*/

	public Stack<InterruptContext> getInterruptContext(int cpu) {
		return context.get(cpu);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Tasks\n");
		for (Task task: tasks.values()) {
			str.append(String.format("%10d %10d %10d %s\n", task.getPid(), task.getTid(), task.getPpid(), task.getName()));
			FDSet fdSet = taskFdSet.get(task);
			if (fdSet != null) {
				Collection<? extends FD> fDs = fdSet.getFDs();
				if (fDs != null) {
					for (FD fd: fDs) {
						str.append(String.format("\t%d %s\n", fd.getNum(), fd.getName()));
					}
				}
			}
		}
		return str.toString();
	}

	public void incrementSwitchUnkownTask() {
		switchUnkownTask++;
	}

	public int getSwitchUnkowntask() {
		return switchUnkownTask;
	}

	public int getDupUnkownFD() {
		return dupUnkownFD;
	}

}
