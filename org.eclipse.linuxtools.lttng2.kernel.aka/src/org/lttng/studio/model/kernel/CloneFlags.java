package org.lttng.studio.model.kernel;

public class CloneFlags {

	/*
	 * cloning flags from /usr/include/linux/sched.h
	 */
	public static long CSIGNAL =				0x000000ff;      /* signal mask to be sent at exit */
	public static long CLONE_VM =      		0x00000100;      /* set if VM shared between processes */
	public static long CLONE_FS =     		0x00000200;      /* set if fs info shared between processes */
	public static long CLONE_FILES =    		0x00000400;      /* set if open files shared between processes */
	public static long CLONE_SIGHAND =   		0x00000800;      /* set if signal handlers and blocked signals shared */
	public static long CLONE_PTRACE =  		0x00002000;      /* set if we want to let tracing continue on the child too */
	public static long CLONE_VFORK = 			0x00004000;      /* set if the parent wants the child to wake it up on mm_release */
	public static long CLONE_PARENT = 		0x00008000;      /* set if we want to have the same parent as the cloner */
	public static long CLONE_THREAD = 		0x00010000;      /* Same thread group? */
	public static long CLONE_NEWNS = 			0x00020000;      /* New namespace group? */
	public static long CLONE_SYSVSEM = 		0x00040000;      /* share system V SEM_UNDO semantics */
	public static long CLONE_SETTLS = 		0x00080000;      /* create a new TLS for the child */
	public static long CLONE_PARENT_SETTID = 	0x00100000;      /* set the TID in the parent */
	public static long CLONE_CHILD_CLEARTID =	0x00200000;      /* clear the TID in the child */
	public static long CLONE_DETACHED =		0x00400000;      /* Unused, ignored */
	public static long CLONE_UNTRACED =		0x00800000;      /* set if the tracing process can't force CLONE_PTRACE on this clone */
	public static long CLONE_CHILD_SETTID =	0x01000000;      /* set the TID in the child */
	/* 0x02000000 was previously the unused CLONE_STOPPED (Start in stopped state)
	   and is now available for re-use. */
	public static long CLONE_NEWUTS =  		0x04000000;      /* New utsname group? */
	public static long CLONE_NEWIPC =  		0x08000000;      /* New ipcs */
	public static long CLONE_NEWUSER =  		0x10000000;      /* New user namespace */
	public static long CLONE_NEWPID =			0x20000000;      /* New pid namespace */
	public static long CLONE_NEWNET =  		0x40000000;      /* New network namespace */
	public static long CLONE_IO = 			0x80000000;      /* Clone io context */

	public static boolean isFlagSet(long bitfield, long flag) {
		return (bitfield & flag) != 0;
	}

}
