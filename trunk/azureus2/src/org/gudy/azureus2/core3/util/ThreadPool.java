/*
 * File    : ThreadPool.java
 * Created : 21-Nov-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

import java.util.*;

public class 
ThreadPool 
{
	protected static final int	IDLE_LINGER_TIME	= 10000;
	
	protected static final boolean	LOG_WARNINGS	= true;
	protected static final int		WARN_TIME		= 10000;
	
	protected String	name;
	protected int		thread_name_index	= 1;
	
	protected long		execution_limit;
	
	protected Stack		thread_pool;
	protected List		busy;
	
	protected Semaphore	thread_sem;
	
	public
	ThreadPool(
		String	_name,
		int		max_size )
	{
		name	= _name;
		
		thread_sem = new Semaphore( max_size );
		
		thread_pool	= new Stack();
					
		busy		= new ArrayList( max_size );
	}

	public void
	setExecutionLimit(
		long		millis )
	{
		execution_limit	= millis;
	}
	
	public threadPoolWorker
	run(
		Runnable	runnable )
	{
		// System.out.println( "Thread pool:" + name + " - sem = " + thread_sem.getValue());
		
		thread_sem.reserve();
						
		threadPoolWorker allocated_worker;
						
		synchronized( this ){
							
			if ( thread_pool.isEmpty()){
						
				allocated_worker = new threadPoolWorker();	
	
			}else{
								
				allocated_worker = (threadPoolWorker)thread_pool.pop();
			}
			
			if ( runnable instanceof ThreadPoolTask ){
				
				((ThreadPoolTask)runnable).worker = allocated_worker;
			}
			
			allocated_worker.run( runnable );
		}
		
		synchronized( ThreadPool.this ){
			
			long	now = SystemTime.getCurrentTime();
			
			for (int i=0;i<busy.size();i++){
					
				threadPoolWorker	x = (threadPoolWorker)busy.get(i);
			
				long	elapsed = now - x.run_start_time ;
					
				if ( elapsed > ( WARN_TIME * (x.warn_count+1))){
		
					x.warn_count++;
					
					if ( LOG_WARNINGS ){
						
						Debug.out( x.getWorkerName() + ": running, elapsed = " + elapsed + ", state = " + x.state );
					}
					
					if ( execution_limit > 0 && elapsed > execution_limit ){
						
						if ( LOG_WARNINGS ){
							
							Debug.out( x.getWorkerName() + ": interrupting" );
						}
						
						Runnable r = x.runnable;
						
						if ( r instanceof ThreadPoolTask ){
							
							((ThreadPoolTask)r).interruptTask();
							
						}else{
							
							x.worker_thread.interrupt();
						}
					}
				}
			}
		}
		
		return( allocated_worker );
	}
	
	public class
	threadPoolWorker
	{
		protected String	worker_name;
		
		protected Thread	worker_thread;
		
		protected Semaphore my_sem = new Semaphore();
		
		protected Runnable	runnable;
		protected long		run_start_time;
		protected int		warn_count;
		
		protected String	state	= "<none>";
		
		protected
		threadPoolWorker()
		{
			worker_name = name + "[" + (thread_name_index++) +  "]";
			
			worker_thread = new AEThread( worker_name )
				{
					public void 
					run()
					{
						boolean	time_to_die = false;
			
outer:
						while(true){
							
							try{
								while( !my_sem.reserve(IDLE_LINGER_TIME)){
																		
									synchronized( ThreadPool.this ){
										
										if ( runnable == null ){
											
											time_to_die	= true;
											
											thread_pool.remove( threadPoolWorker.this );
																						
											break outer;
										}
									}
								}
								
								try{
									
									synchronized( ThreadPool.this ){
											
										run_start_time	= SystemTime.getCurrentTime();
										warn_count		= 0;
										
										busy.add( threadPoolWorker.this );
									}
									
									runnable.run();
										
								}finally{
																				
									synchronized( ThreadPool.this ){
											
										long	elapsed = SystemTime.getCurrentTime() - run_start_time;
										
										if ( elapsed > WARN_TIME && LOG_WARNINGS ){
											
											Debug.out( getWorkerName() + ": terminated, elapsed = " + elapsed + ", state = " + state );
										}
										
										busy.remove( threadPoolWorker.this );
									}
									
									runnable	= null;
								}
							}catch( Throwable e ){
									
								e.printStackTrace();
											
							}finally{
										
								if ( !time_to_die ){
									
									synchronized( ThreadPool.this ){
															
										thread_pool.push( threadPoolWorker.this );
									}
								
									thread_sem.release();
								}
							}
						}
					}
				};
				
			worker_thread.setDaemon(true);
			
			worker_thread.start();
		}
		
		public void
		setState(
			String	_state )
		{
			//System.out.println( "state = " + _state );
			
			state	= _state;
		}
		
		protected String
		getWorkerName()
		{
			return( worker_name );
		}
		
		protected void
		run(
			Runnable	_runnable )
		{
			runnable	= _runnable;
			
			my_sem.release();
		}
	}
	
	public String
	getName()
	{
		return( name );
	}
}
