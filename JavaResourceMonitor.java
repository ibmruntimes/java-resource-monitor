/*******************************************************************************
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 *
 * (c) Copyright IBM Corp. 2017, 2017 All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/
import java.lang.instrument.*;
import javax.management.*;
import com.ibm.lang.management.*;
import java.lang.management.ManagementFactory;
import java.util.Scanner;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.io.BufferedWriter;
import java.io.IOException;

enum ParseResult {
        OPTION_OK ("ok"),
	OPTION_MISSING_VALUE ("missing value"),
        OPTION_INVALID_VALUE ("invalid value"),
        OPTION_UNRECOGNIZED ("unrecognized");

        private String message;
        ParseResult(String msg) {
                message = msg;
        }

        public String toString() {
                return message;
        }
}

public class JavaResourceMonitor {
	// list of valid options
	final static String FREQUENCY = "frequency";
	final static String DURATION = "duration";

	// create an array of all valid options
	final static String options[] = { FREQUENCY, DURATION };

	// variables for the options
	static long frequency = 1000; // in milliseconds
	static long duration = 0; // in seconds; 0 means profile for ever

        public static void printUsage() {
                System.out.println("Usage: java -javaagent:<agent.jar>=[" + FREQUENCY + "=<msecs>][," + DURATION + "=<secs>]");
        }

	public static void agentmain(String agentArgs) {
		premain(agentArgs);	
	}

	public static void premain(String agentArgs) {
		MBeanServer mbs = null;
		ObjectName objName = null;
		JvmCpuMonitorMXBean mbean = null;

		ParseResult result = parseArgs(agentArgs);
		if (result != ParseResult.OPTION_OK) {
			printUsage();
			return;
                }

		System.out.print("Using sampling frequency of " + frequency + " ms");
		if (duration != 0) {
			System.out.println(" for " + duration + " secs");
		}
		System.out.println();

		try {
			objName = new ObjectName("com.ibm.lang.management:type=JvmCpuMonitor");
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
			return;
		}

		try {
			mbs = ManagementFactory.getPlatformMBeanServer();
			if (mbs.isRegistered(objName) != true) {
				System.err.println("JvmCpuMonitorMXBean is not registered. " + "Cannot Proceed");
				return;
			}
			mbean = JMX.newMXBeanProxy(mbs, objName, JvmCpuMonitorMXBean.class);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		startMonitor(mbean);
	}

	public static ParseResult parseArgs(String agentArgs) {
		ParseResult parseResult = ParseResult.OPTION_OK;

		if (agentArgs != null) {
			Scanner scanner = new Scanner(agentArgs).useDelimiter(",");
			while (scanner.hasNext()) {
				int i = 0;
				String name = null;
				String value = null;
				String option = scanner.next();

				if (option.indexOf('=') != -1) {
					String optionParts[] = option.split("=");
					name = optionParts[0];
					if (optionParts.length > 1) {
						value = optionParts[1];
					}
				} else {
					name = option;
				}
				for (i = 0; i < options.length; i++) {
					if (name.equals(options[i])) {
						switch (name) {
						case FREQUENCY:
							if (value != null) {
								try {
									frequency = Long.parseLong(option.substring(options[i].length()));
								} catch (NumberFormatException e) {
									parseResult = ParseResult.OPTION_INVALID_VALUE;
								}
							} else {
								parseResult = ParseResult.OPTION_MISSING_VALUE;
							}
							break;
						case DURATION:
							if (value != null) {
								try {
									duration = Long.parseLong(option.substring(options[i].length()));
								} catch (NumberFormatException e) {
									parseResult = ParseResult.OPTION_INVALID_VALUE;
								}
								break;
							} else {
								parseResult = ParseResult.OPTION_MISSING_VALUE;
							}
						}
					}
					if (parseResult != ParseResult.OPTION_OK) {
						break;
					}
				}
				if (i == options.length) {
					parseResult = ParseResult.OPTION_UNRECOGNIZED;
				}
				if (parseResult != ParseResult.OPTION_OK) {
					System.out.println("Error in parsing option \"" + option + "\" : " + parseResult);
					break;
				}
			}
		}
		return parseResult;
	}

	public static void startMonitor(JvmCpuMonitorMXBean mbean) {
		Thread monitorThread = null;
		try {
			monitorThread = new Thread(new ThreadMonitor(mbean, frequency, duration));
			monitorThread.setDaemon(true);
			monitorThread.start();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		return;
	}
}

class ThreadMonitor implements Runnable {
        JvmCpuMonitorMXBean mbean;
	long frequency;
	long duration;

        public ThreadMonitor(JvmCpuMonitorMXBean bean, long frequency, long duration) {
                mbean = bean;
		this.frequency = frequency;
		this.duration = duration;
        }

        public void run() {
		mbean.setThreadCategory(Thread.currentThread().getId(), "Resource-Monitor");
		try {
	                dumpJvmCpuMonitor();
		} catch (Exception e) {
			e.printStackTrace();
		}
        }

        private void dumpJvmCpuMonitor() throws IOException
        {
		JvmCpuMonitorInfo jcmInfo1 = new JvmCpuMonitorInfo();
		JvmCpuMonitorInfo jcmInfo2 = new JvmCpuMonitorInfo();
		JvmCpuMonitorInfo currJcmInfo = null;
		JvmCpuMonitorInfo prevJcmInfo = null;
		long adiff = 0;
		long tdiff = 0;
		long sdiff = 0;
		long othdiff = 0;
		long gcdiff = 0;
		long jitdiff = 0;
		long timestamp = 0;
		long firstTimestamp = 0;
		long applicationCpuTime = 0;
		long resourceMonitorCpuTime = 0;
		long systemCpuTime = 0;
		long gcCpuTime = 0;
		long jitCpuTime = 0;
		long totalCpuTime = 0;
		float apc = 0;
		float tpc = 0;
		float spc = 0;
		float gcpc = 0;
		float jitpc = 0;
		float othpc = 0;
		long startTime = 0;
		float time = 0;
		int i = 0;
		String out = null;
		BufferedWriter writer = null;
	
		try {
			writer = Files.newBufferedWriter(FileSystems.getDefault().getPath("cpulogs"), StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			writer = Files.newBufferedWriter(FileSystems.getDefault().getPath("cpulogs"), StandardOpenOption.TRUNCATE_EXISTING);
		}
		out = String.format("%16s %14s %10s %14s %10s %14s %10s [%10s %10s %10s]\n", "Timestamp(s)", "R-Mon(us)", "R-Mon(%)", "App(us)", "App(%)", "System(us)", "System(%)", "GC(%)", "JIT(%)", "Others(%)");
		writer.write(out);

		startTime = System.nanoTime();
		do {
                        if ((i % 2) == 0) {
                                jcmInfo1 = mbean.getThreadsCpuUsage(jcmInfo1);
                                currJcmInfo = jcmInfo1;
                                prevJcmInfo = jcmInfo2;
                        } else {
                                jcmInfo2 = mbean.getThreadsCpuUsage(jcmInfo2);
                                currJcmInfo = jcmInfo2;
                                prevJcmInfo = jcmInfo1;
                        }

                        timestamp = currJcmInfo.getTimestamp();
                        applicationCpuTime = currJcmInfo.getApplicationCpuTime();
                        resourceMonitorCpuTime = currJcmInfo.getResourceMonitorCpuTime();
                        systemCpuTime = currJcmInfo.getSystemJvmCpuTime();
			gcCpuTime = currJcmInfo.getGcCpuTime();
			jitCpuTime = currJcmInfo.getJitCpuTime();

                        if (i > 0) {
				time = (float)(timestamp - firstTimestamp) / (float)1000000; // convert to seconds 
                                adiff = applicationCpuTime - prevJcmInfo.getApplicationCpuTime();
                                tdiff = resourceMonitorCpuTime - prevJcmInfo.getResourceMonitorCpuTime();
                                sdiff = systemCpuTime - prevJcmInfo.getSystemJvmCpuTime();
				totalCpuTime = sdiff + tdiff + adiff;
				apc = ((float) adiff / (float) totalCpuTime) * 100;
				tpc = ((float) tdiff / (float) totalCpuTime) * 100;
				spc = ((float) sdiff / (float) totalCpuTime) * 100;

				if (sdiff > 0) {
					gcpc = ((float) (gcCpuTime - prevJcmInfo.getGcCpuTime()) / (float) sdiff) * 100;
					jitpc = ((float) (jitCpuTime - prevJcmInfo.getJitCpuTime()) / (float) sdiff) * 100;
					othdiff = systemCpuTime-(gcCpuTime+jitCpuTime) - (prevJcmInfo.getSystemJvmCpuTime()-(prevJcmInfo.getGcCpuTime()+prevJcmInfo.getJitCpuTime()));
					othpc = ((float) othdiff / (float) sdiff) * 100;
					out = String.format("%16.3f %14d %9.2f%% %14d %9.2f%% %14d %9.2f%% [%9.2f%% %9.2f%% %9.2f%%]\n",
								time, tdiff, tpc, adiff, apc, sdiff, spc, gcpc, jitpc, othpc);
				} else {
					out = String.format("%16.3f %14d %9.2f%% %14d %9.2f%% %14d %9.2f%% [%10s %10s %10s]\n",
								time, tdiff, tpc, adiff, apc, sdiff, spc, "-", "-", "-");
				}
                        } else {
				firstTimestamp = timestamp;
				out = String.format("%16.3f %14d %10s %14d %10s %14d %10s [%10s %10s %10s]\n",
							time, tdiff, "-", adiff, "-", sdiff, "-", "-", "-", "-");
			}
			writer.write(out);
			writer.flush();

                        try {
                                Thread.sleep(frequency);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			i += 1;
		} while ((duration == 0) || (System.nanoTime() - startTime) / 1000000L < (duration * 1000));
		writer.close();
	}
}
