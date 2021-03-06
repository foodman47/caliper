/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

const CaliperUtils = require('./../../caliper-core/lib/utils/caliper-utils');
const logger = CaliperUtils.getLogger('corda-utils.js');

/**
 * Implements util methods for corda deployment backend. It connects Caliper to Corda via webserver.
 */
class CordaUtils {
    /**
     * Create a new instance of the {CordaUtils} class
     * @param {string} client object reference.
     * @param {string} workspace_root indicates the working directory
     */
    constructor(client, workspace_root) {
        this.client = client;
        this.workspaceroot = workspace_root;
    }

    /**
     * Computes benchmark round defined in caliper configuration file
     * @param {string} args contains the caliper-corda benchmarking configuration
     * @return {object} results contains benchmarking results in JSON format
     * @async
     */
    async computeRounds(args) {
        let rounds = args.length;
        let results = [];
        for (let i = 0; i < rounds; i++) {
            logger.info('Round '+i+' starting...');
            let context = {'host':'localhost', 'port':10050};
            let contractID = null;
            let contractVer = null;
            results.push(await this.client.invokeSmartContract(context, contractID, contractVer, args, i));
        }
        return results;
    }

    /**
     * Invokes gradle to stop all gradle processes at the end of the simulation
     * @async
     */
    static async stopGradleDaemon() {
        let fork = require('child_process').exec;
        logger.info('Stopping gradle deamons');
        fork('./gradlew --stop', {cwd: this.workspaceroot+'/cordapps/finance'});
    }

    /**
     * Save the results into a log file
     * @param {string} results is sent into python to generate a graph representing the round
     * @param {string} filename for output results
     * @async
     */
    async saveResults(results, filename) {
        const fs = require('fs');
        logger.info('Results: '+results);
        fs.writeFile('./packages/caliper-corda/results/'+filename, results);
    }

}

module.exports = CordaUtils;
