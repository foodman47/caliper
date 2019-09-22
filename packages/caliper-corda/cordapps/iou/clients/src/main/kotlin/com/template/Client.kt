package com.template

import com.example.flow.ExampleFlow
import com.example.flow.ExampleFlow.Initiator
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.json.simple.JSONArray
import org.json.simple.JSONObject


/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = Client().main(args)

class Client {

    lateinit var proxy: CordaRPCOps

    companion object {
        val logger = loggerFor<Client>()

    }

    fun main(args: Array<String>) {
        // Create an RPC connection to the node.
        require(args.size == 3) { "Usage: Client <node address> <rpc username> <rpc password>" }
        val nodeAddress = parse(args[0])
        val rpcUsername = args[1]
        val rpcPassword = args[2]
        val client = CordaRPCClient(nodeAddress)
        val proxy = client.start(rpcUsername, rpcPassword).proxy

        // Interact with the node.
        // get network infos
        val notaries = proxy.notaryIdentities()
        val issueRef = OpaqueBytes.of(Byte.MAX_VALUE)

        // parameters to measure
        val batches = arrayOf(1, 5, 10, 50, 100)

        val res = JSONArray()
        for (batch in batches) {

            var nTX = 1000

            // out min, max resp time, trp, simulation time
            var min: Long = 99999
            var max: Long = 0
            var sum: Long = 0

            // record all ms measurements
            var ends = ArrayList<Long>()
            var threads = ArrayList<Thread>(batch)

            val x500Name = CordaX500Name.parse("O=PartyC,L=Paris,C=FR")
            val recipient = proxy.wellKnownPartyFromX500Name(x500Name) as Party
            val notary = notaries.get(0)

            var start1 = System.currentTimeMillis()

            for (j in 0..batch - 1) {
                val thread = Thread {
                    for (i in 0..(nTX / batch - 1)) {
                        val start = System.currentTimeMillis()
                        // start flow and wait for reply
                        val flow = proxy.startFlow(::Initiator, 1, recipient).returnValue.getOrThrow();
                        val end = System.currentTimeMillis() - start
                        logger.info("{}", Thread.currentThread().id.toString() + " " + end)
                        ends.add(end)
                    };
                }
                threads.add(thread)
            }
            // start threads at the same time
            for (j in 0..batch - 1) {
                threads[j].start()
            }
            // join all threads
            for (j in 0..batch - 1) {
                threads[j].join()
            }
            // modify actual nTX
            nTX = ends.size
            // get measures once measurements are done
            var end1 = System.currentTimeMillis() - start1
            val trp = nTX / (end1 / 1e3) // tps
            // compute max, min resp time
            for (i in 0..nTX - 1) {
                sum += ends[i]
                if (ends[i] < min) {
                    min = ends[i]
                }
                if (ends[i] > max) {
                    max = ends[i]
                }
            }
            // compute all metrics and convert them into JSON
            var avg_resp_time = sum / nTX.toLong()
            // record result in json format
            var obj_array = JSONObject()
            // used for input params has reminder
            var inp = JSONObject()
            inp.put("nTX_sent", nTX); inp.put("nThreads", batch);
            // used for the output measurements that we are interested in
            var out = JSONObject()
            out.put("throughput", trp); out.put("avg_resp_time", avg_resp_time);
            out.put("max_resp_time", max); out.put("min_resp_time", min); out.put("simulation_time", end1);
            out.put("success", nTX); out.put("fail", nTX - nTX)
            // used for metrics info
            var info = JSONObject(); info.put("throughput", "tps"); info.put("time", "ms")
            // link the four json objects into main
            obj_array.put("input_params", inp);
            obj_array.put("output_params", out);
            obj_array.put("metrics", info)

            res.add(obj_array)
            logger.info("{}", "Done with size: "+batch)
            logger.info("{}", obj_array)
        }
        logger.info("{}", res)
    }
}