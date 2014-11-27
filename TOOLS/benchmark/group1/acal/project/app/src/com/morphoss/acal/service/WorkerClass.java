/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.morphoss.acal.service;

import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

import android.os.ConditionVariable;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateTime;

/**
 * This class is designed to have a single worker thread perform jobs supplied
 * by either the service, or an activity. Jobs are prioritised and higher
 * priority jobs a performed first.
 * 
 * After completing a set of jobs, the worker will sleep until woken by a timer
 * which is set for when the worker next thinks it needs to do a job.
 * 
 * If the service or an activity needs to do a job immediately it should call
 * wakeWorker() after adding the job to the queue.
 * 
 * As different jobs may take different lengths of time, there is no guarantee
 * of how long it will take before the worker completes an assigned job. If
 * something needs to be done immediately then it should be done by the context
 * that needs the work done.
 * 
 * The workers constructor is hidden to ensure the invariant |Workers|=1 is
 * maintained. to get a worker use the factory method getInstance()
 * 
 * The time the worker will next awaken, and the time the worker last completed
 * a job are publicly available. This allows an external thread to check if the
 * worker has hung. In the case of a hung worker use the reset() method.
 * 
 * @author Morphoss Ltd
 * 
 */
public class WorkerClass implements Runnable {

	private static WorkerClass			instance			= null;

	public static final String			TAG					= "aCal WorkerClass";

	private TimerTask					myTimerTask;
	private Thread						worker				= null;
	private PriorityQueue<ServiceJob>	jobQueue			= new PriorityQueue<ServiceJob>();
	private ConditionVariable			runWorker			= new ConditionVariable(true);
	private Timer						myTimer;
	private aCalService					context;
	private volatile boolean			interruptSent		= false;
	public static boolean				isRunning			= false;

	/** Public vars */

	public volatile long				timeOfLastAction	= System.currentTimeMillis();
	public volatile long				timeOfNextAction	= System.currentTimeMillis()
																	+ Constants.MAXIMUM_SERVICE_WORKER_DELAY_MS
																	+ Constants.SERVICE_WORKER_GRACE_PERIOD;

	private WorkerClass(aCalService context) {
		this.context = context;
	}

	public synchronized static WorkerClass getInstance(aCalService context) {
		if ( instance == null ) instance = new WorkerClass(context);
		if ( instance.worker == null ) {
			instance.worker = new Thread(instance);
			instance.worker.setPriority(Thread.MIN_PRIORITY);
			instance.worker.start();
		}
		return instance;
	}

	public static WorkerClass getExistingInstance() {
		return instance;
	}

	/**
	 * Add a new job onto the job queue. If the job is already in the queue then
	 * we will replace that one if this is at an earlier time, or ignore it if
	 * it would happen after the pending instance.
	 * 
	 * If the schedule is less than ten days, we treat it as a relative time in
	 * the future, otherwise we will treat it as an absolute time.
	 * 
	 * @param s
	 */
	private synchronized void addJob(ServiceJob s) {
		if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, "Request to add new job " + s);

		// If it's less than ten days assume offset from now.
		if ( s.TIME_TO_EXECUTE < 864000000 ) s.TIME_TO_EXECUTE += System.currentTimeMillis();

		if ( jobQueue.contains(s) ) {
			ServiceJob existing = null;

			// If a job that is equivelent already exists, overwrite iff new job
			// happens sooner
			for (ServiceJob sj : jobQueue) {
				if ( sj.equals(s) ) {
					existing = sj;
					break;
				}
			}
			if ( s.TIME_TO_EXECUTE < existing.TIME_TO_EXECUTE ) {
				if ( Constants.LOG_DEBUG ) {
					Log.println(Constants.LOGD, TAG, "New job " + s);
					Log.println(Constants.LOGD, TAG, "Replaces old " + existing);
				}
				jobQueue.remove(existing);
				jobQueue.add(s);
			}
			else {
				if ( Constants.LOG_DEBUG ) {
					Log.println(Constants.LOGD, TAG, "Old job " + existing);
					Log.println(Constants.LOGD, TAG, "Eclipses new " + s);
				}
				return;
			}

		}
		if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, "New job added " + s);
		jobQueue.add(s);
	}

	
	/**
	 * Add the ServiceJob and wake up the run queue
	 * 
	 * @param s
	 */
	public synchronized void addJobAndWake(ServiceJob s) {
		try {
			addJob(s);
			this.runWorker.open();
		}
		catch( Exception e ) {
			Log.e(TAG,Log.getStackTraceString(e));
		}
	}

	/**
	 * Add a whole bunch of ServiceJobs and then wake up the run queue
	 * 
	 * @param s
	 */
	public void addJobsAndWake(Collection<ServiceJob> jobs) {
		for (ServiceJob sj : jobs) {
			if ( sj != null ) addJob(sj);
		}
		this.runWorker.open();
	}

	/**
	 * Add a whole bunch of ServiceJobs and then wake up the run queue
	 * 
	 * @param s
	 */
	public void addJobsAndWake(ServiceJob[] jobs) {
		for (ServiceJob sj : jobs) {
			if ( sj != null ) addJob(sj);
		}
		this.runWorker.open();
	}

	private synchronized ServiceJob getJob() {
		if ( jobQueue.isEmpty() ) {
			runWorker.close();
			return null;
		}
		ServiceJob sj = jobQueue.peek();
		long time = System.currentTimeMillis();
		if ( time < sj.TIME_TO_EXECUTE ) return null; // Job not ready yet
		else
			return jobQueue.poll();
	}

	public void resetWorker() {
		this.interruptSent = true;
		worker.interrupt();
		Log.i(TAG, "Resetting worker thread.");
		this.worker = null;
		instance.worker = new Thread(instance);
		instance.worker.start();
	}

	public void killWorker() {
		this.interruptSent = true;
		if ( worker != null ) worker.interrupt();
		this.worker = null;
		runWorker.open();
	}

	private void destroyTimers() {
		// Destroy all timer activity
		if ( myTimerTask != null ) {
			myTimerTask.cancel();
			myTimerTask = null;
		}
		if ( myTimer != null ) {
			myTimer.cancel();
			myTimer.purge();
			myTimer = null;
		}
		else {
			if ( Constants.LOG_VERBOSE ) Log.v(TAG, "Asked to destroy timers, but no timers are set!");
		}
	}

	public void run() {
		WorkerClass.isRunning = true;
		this.interruptSent = false;
		try {
			while ( worker == Thread.currentThread() ) {
				if ( Constants.LOG_VERBOSE ) Log.v(TAG, "Worker thread awake, processing queue of " + jobQueue.size()
						+ " jobs.");
				// Remove all timers.
				this.destroyTimers();

				// Iterate through remaining jobs. If we run out our condition
				// variable is closed automatically.
				ServiceJob job;
				while ( (job = getJob()) != null ) {
					if (job.getDescription() == null) {
						Log.w(TAG, "Description of job in class "+job.getClass()+" is returning null.");
					}
					
					Log.i(TAG, "Executing job " + job.getDescription());
//					if ( Constants.debugHeap)
//						AcalDebug.heapDebug(TAG, "Run Job "+job.getDescription().replace(':','-'));

					job.run(this.context);
					timeOfLastAction = System.currentTimeMillis();
				}
				if ( Constants.LOG_VERBOSE ) Log.v(TAG, "Finished processing jobs. Scheduling next wakeup call.");
				this.setWakeupCall();
				runWorker.block();
			}
		}
		catch ( Exception e ) {
			if ( e instanceof InterruptedException && this.interruptSent ) {
				if ( Constants.LOG_DEBUG ) Log.d(TAG, "Worker class interrupted intentionally.");
			}
			else {
				Log.e(TAG, "Worker class unexpected exception: " + e.getMessage());
				Log.e(TAG, Log.getStackTraceString(e));
				resetWorker();
			}
		}
		finally {
			this.destroyTimers();
			WorkerClass.isRunning = false;
		}
	}

	private void setWakeupCall() {
		// Tell the service when it should wait till before assuming we've
		// crashed
		this.timeOfNextAction = System.currentTimeMillis() + (Constants.MAXIMUM_SERVICE_WORKER_DELAY_MS)
				+ Constants.SERVICE_WORKER_GRACE_PERIOD;
		long timeTillNext = Constants.MAXIMUM_SERVICE_WORKER_DELAY_MS;
		synchronized( jobQueue ) {
			if ( Constants.LOG_VERBOSE ) Log.v(TAG, "Sleeping with " + jobQueue.size() + " jobs on hold.");
			if ( !jobQueue.isEmpty() ) {
				this.timeOfNextAction = jobQueue.peek().TIME_TO_EXECUTE + Constants.SERVICE_WORKER_GRACE_PERIOD;
				timeTillNext = Math.max(0, jobQueue.peek().TIME_TO_EXECUTE - System.currentTimeMillis());
			}
		}

		if ( Constants.LOG_VERBOSE ) {
			AcalDateTime when = AcalDateTime.getUTCInstance();
			Log.println(Constants.LOGV, TAG,
					"Next checking jobQueue in " + (timeTillNext / 1000) + " seconds at "+when.setMillis(timeOfNextAction).fmtIcal());
		}

		runWorker.close();

		// Set 'wake up' timer. Should always be the LAST thing done.
		myTimer = new Timer("aCal Service Timer", true);
		myTimerTask = new WakeUpTimer();
		myTimer.schedule(myTimerTask, timeTillNext);

	}

	private class WakeUpTimer extends TimerTask {
		public void run() {
			runWorker.open();
		}
	}
	
}
