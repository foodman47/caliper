/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package bftsmart.demo.benchmark;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import bftsmart.tom.ServiceProxy;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.util.Storage;

import java.io.PrintStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Example client that updates a BFT replicated service (a counter).
 * adapted by Julien Tinguely
 */
public class ThroughputLatencyClient {
	
    public static int initId = 0;
    private static int numThreads;
    
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws IOException {
        if (args.length < 7) {
            System.out.println("Usage: ... ThroughputLatencyClient <process id> <num. threads> <number of transactions> "
            		+ "<request size> <timeout> <read only?> <DoS?>");
            System.exit(-1);
        }
        initId = Integer.parseInt(args[0]);
        numThreads = Integer.parseInt(args[1]);
        int nTXs = Integer.parseInt(args[2]);
        int requestSize = Integer.parseInt(args[3]);
        int timeout = Integer.parseInt(args[4]);
        boolean readOnly = Boolean.parseBoolean(args[5]);
        boolean dos = Boolean.parseBoolean(args[6]);

        Client[] c = new Client[numThreads];
        
        for(int i=0; i<numThreads; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(ThroughputLatencyClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            System.out.println("Launching client " + (initId+i));
            c[i] = new ThroughputLatencyClient.Client(initId+i,nTXs,requestSize,timeout,readOnly, dos);
        }

        for(int i=0; i<numThreads; i++) {
            c[i].start();
        }
          
        for(int i=0; i<numThreads; i++) {
            try {
                c[i].join();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
        }

        System.exit(0);
    }

    static class Client extends Thread {

        int id;
        int nTXs;
        int requestSize;
        int timeout;

        boolean readOnly;
        boolean dos;
        ServiceProxy proxy;
        byte[] request;
        
        private static PrintStream fileOut;
        
        private JSONObject results = new JSONObject();
        private JSONArray measures = new JSONArray();

        
        @SuppressWarnings("unchecked")
		public Client(int id, int nTXs, int requestSize, int timeout, boolean readOnly,  boolean dos) {
            super("Client "+id);
        
            this.id = id;
            this.nTXs = nTXs;
            this.requestSize = requestSize;
            this.timeout = timeout;
            this.readOnly = readOnly;
            this.proxy = new ServiceProxy(id);
            this.request = new byte[this.requestSize];
            this.dos = dos;

        }

        @SuppressWarnings("unchecked")
		public void run() {
            //ServiceProxy proxy = new ServiceProxy(id);
            //proxy.setInvokeTimeout(1);
            long throughputMeasurementStartTime = System.currentTimeMillis();

            byte[] reply;
            int reqId;
            int req = 0;

            Storage st = new Storage(nTXs);

            System.out.println("Executing experiment for " + nTXs + " ops");

            for (int i = 0; i < nTXs; i++, req++) {
                long last_send_instant = System.nanoTime();
                if (dos) {
                    reqId = proxy.generateRequestId((readOnly) ? TOMMessageType.UNORDERED_REQUEST : TOMMessageType.ORDERED_REQUEST); 
                    proxy.TOMulticast(request, reqId, (readOnly) ? TOMMessageType.UNORDERED_REQUEST : TOMMessageType.ORDERED_REQUEST); 

                } else {
                	if(readOnly) {
                		reply = proxy.invokeUnordered(request);
                	} else {
                		reply = proxy.invokeOrdered(request);
                	}
                }
                st.store(System.nanoTime() - last_send_instant);

                if (timeout > 0) {
                    //sleeps timeout ms before sending next request
                    try {
						Thread.sleep(timeout);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }                   

            }           
            writeReportJSON(throughputMeasurementStartTime, st);      
        }

		@SuppressWarnings("unchecked")
		private void writeReportJSON(long throughputMeasurementStartTime, Storage st) {
			float tp = (float)(nTXs*timeout*1000/(float)(System.currentTimeMillis()-throughputMeasurementStartTime));

        	// for each timeout write results values into array
        	// overall thrp for timeout
            tp = nTXs*1000/(float)(System.currentTimeMillis()-throughputMeasurementStartTime);
			// used for the output measurements that we are interested in
            
            // write input parameters into JSON object
            JSONObject inp = new JSONObject();
    		inp.put("type", "client");
    		inp.put("id", id);
    		inp.put("nTXs", nTXs);
    		inp.put("requestSize", requestSize);
    		inp.put("timeout", timeout);
    		inp.put("readOnly", readOnly);
    		inp.put("dos", dos);
    		results.put("input", inp);
    		// write metrics
    		JSONObject info = new JSONObject();
    		info.put("throughput", "tps");
    		info.put("time", "ns");
    		results.put("metrics", info);
    		// output
			JSONObject out = new JSONObject();
			out.put("throughput", tp);
			out.put("avg_latency", st.getAverage(false));
			out.put("std_dev", st.getDP(false));
			out.put("avg_latency_limit", st.getAverage(true));
			out.put("std_dev_limit", st.getDP(true));
			out.put("max_latency", st.getMax(false));		

			// add timeout out to measures
			measures.add(out);
			
			results.put("measures", measures);
			
            throughputMeasurementStartTime = System.currentTimeMillis();
            
            try {
            	fileOut = new PrintStream("./results/client_"+this.id+"_"+numThreads+"_"+this.requestSize+".json");
    		} catch (FileNotFoundException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		System.setOut(fileOut);
    		System.out.println(results.toJSONString());  
            PrintStream consoleStream = new PrintStream(
                    new FileOutputStream(FileDescriptor.out));
        	System.setOut(consoleStream);
			
		}
    }
}
