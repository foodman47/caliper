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

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import bftsmart.tom.util.Storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Simple server that just acknowledge the reception of a request.
 * adapted by Julien Tinguely, for JSON output
 */
public final class ThroughputLatencyServer extends DefaultRecoverable{
    
    private int interval;
    private int replySize;
    
    private int id;
    private String filename;

    private static PrintStream fileOut;
    private boolean report;
    
    private int iterations = 0;
    private long throughputMeasurementStartTime;
            
    private Storage totalLatency = null;
    private Storage consensusLatency = null;
    private Storage proposeLatency = null;
    private Storage writeLatency = null;
    private Storage acceptLatency = null;
	private ServiceReplica replica;
    private JSONObject results;
    private JSONArray measures;

    /*
     * interval is meant # iterations to summarize results
     */
    @SuppressWarnings("unchecked")
	public ThroughputLatencyServer(int id, int interval, int replySize, String filename, boolean report) {

    	this.report = report;
        this.interval = interval;
        this.replySize = replySize;
        this.id = id;
        this.filename = filename;

        if(report) {
        	results = new JSONObject();
            measures = new JSONArray();

	        totalLatency = new Storage(interval);
	        consensusLatency = new Storage(interval);
	        proposeLatency = new Storage(interval);
	        writeLatency = new Storage(interval);
	        acceptLatency = new Storage(interval);
        }

        replica = new ServiceReplica(id, this, this);
		
        this.throughputMeasurementStartTime = System.currentTimeMillis();
        
    }
    
    @Override
    public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs) {
        byte[][] replies = new byte[commands.length][];
        
        for (int i = 0; i < commands.length; i++) {
            replies[i] = execute(commands[i],msgCtxs[i]);
        }
        
        return replies;
    }
    
    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return execute(command,msgCtx);
    }
    
    @SuppressWarnings("unchecked")
	public byte[] execute(byte[] command, MessageContext msgCtx) {    

        boolean readOnly = false;
        
        iterations++;
        
        if(report) { 
    		if (iterations % interval == 0) {
		        if (msgCtx != null && msgCtx.getFirstInBatch() != null) {
		            
		            readOnly = msgCtx.readOnly;
		                    
		            msgCtx.getFirstInBatch().executedTime = System.nanoTime();
		                        
		            totalLatency.store(msgCtx.getFirstInBatch().executedTime - msgCtx.getFirstInBatch().receptionTime);
		
		            if (readOnly == false) {
		                consensusLatency.store(msgCtx.getFirstInBatch().decisionTime - msgCtx.getFirstInBatch().consensusStartTime);
		                long temp = msgCtx.getFirstInBatch().consensusStartTime - msgCtx.getFirstInBatch().receptionTime;            
		                proposeLatency.store(msgCtx.getFirstInBatch().writeSentTime - msgCtx.getFirstInBatch().consensusStartTime);
		                writeLatency.store(msgCtx.getFirstInBatch().acceptSentTime - msgCtx.getFirstInBatch().writeSentTime);
		                acceptLatency.store(msgCtx.getFirstInBatch().decisionTime - msgCtx.getFirstInBatch().acceptSentTime);
		            } else {       
		                consensusLatency.store(0);          
		                proposeLatency.store(0);
		                writeLatency.store(0);
		                acceptLatency.store(0);   
		            }
		            
		        } else {         
		            consensusLatency.store(0);           
		            proposeLatency.store(0);
		            writeLatency.store(0);
		            acceptLatency.store(0);            
		        }
    		}
        	// for each interval write results values into array
        	// overall thrp for interval
            float tp = interval*1000/(float)(System.currentTimeMillis()-throughputMeasurementStartTime);
			// used for the output measurements that we are interested in
            
            // write input parameters into JSON object
            JSONObject inp = new JSONObject();
    		inp.put("type", "server");
    		inp.put("id", id);
    		inp.put("replySize", replySize);
    		inp.put("interval", interval);
    		results.put("input", inp);
    		// write metrics
    		JSONObject info = new JSONObject();
    		info.put("throughput", "tps");
    		info.put("time", "ns");
    		results.put("metrics", info);
    		
			JSONObject out = new JSONObject();
			out.put("throughput", tp);
			out.put("avg_latency", totalLatency.getAverage(false));
			out.put("consensus_latency", consensusLatency.getAverage(false) );
			out.put("propose_latency", proposeLatency.getAverage(false));
			out.put("write_latency", writeLatency.getAverage(false));
			out.put("accept_latency", acceptLatency.getAverage(false));
			// add interval out to measures
			measures.add(out);
			
			results.put("measures", measures);
			
            throughputMeasurementStartTime = System.currentTimeMillis();
            
            try {
            	fileOut = new PrintStream("./results/server_"+filename+".json");
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

        return new byte[replySize];
    }

	public static void main(String[] args) {
        if(args.length < 5) {
            System.out.println("Usage: ... ThroughputLatencyServer <processId> <reply size> <report> <interval> <filename>");
            System.exit(-1);
        }

        int processId = Integer.parseInt(args[0]);
        int interval = Integer.parseInt(args[3]);
        int replySize = Integer.parseInt(args[1]);
        String filename = args[4];
        boolean report = Boolean.parseBoolean(args[2]);
 
        new ThroughputLatencyServer(processId,interval,replySize, filename, report);        
    }

	@SuppressWarnings("unchecked")
	@Override
	public void installSnapshot(byte[] state) {
		try {
			System.out.println("setState called");
			ByteArrayInputStream bis = new ByteArrayInputStream(state);
			ObjectInput in = new ObjectInputStream(bis);
			//counter =  in.readInt();
			in.close();
			bis.close();
		} catch (Exception e) {
			System.err.println("[ERROR] Error deserializing state: "
					+ e.getMessage());
		}
	}

	@Override
	public byte[] getSnapshot() {
		try {
			System.out.println("getState called");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bos);
			//out.writeInt(counter);
			out.flush();
			bos.flush();
			out.close();
			bos.close();
			return bos.toByteArray();
		} catch (IOException ioe) {
			System.err.println("[ERROR] Error serializing state: "
					+ ioe.getMessage());
			return "ERROR".getBytes();
		}
	}

   
}
