/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

var v8 = require('v8');
var log = require("../lib/log.js").logger();
var AddressSource = require('../lib/internal_address_source.js');
var AddressSpacePlanSource = require('../lib/internal_addressspaceplan_source.js');
var AddressPlansSource = require('../lib/internal_addressplan_source.js');
var BrokerAddressSettings = require('../lib/broker_address_settings.js');
var ConsoleServer = require('../lib/console_server.js');
var kubernetes = require('../lib/kubernetes.js');
var Ragent = require('../lib/ragent.js');
var tls_options = require('../lib/tls_options.js');
var myutils = require('../lib/utils.js');

function bind_event(source, event, target, method) {
    source.on(event, target[method || event].bind(target));
}

function start(env) {
    kubernetes.is_openshift().then((openshift) => {
        kubernetes.get_messaging_route_hostname(env).then(function (result) {
            if (result !== undefined) env.MESSAGING_ROUTE_HOSTNAME = result;
            env.ADDRESS_SPACE_PREFIX = env.ADDRESS_SPACE + ".";
            var address_space_plan_source = new AddressSpacePlanSource(env);
            var address_plans_source = new AddressPlansSource(env);
            var address_source = new AddressSource(env);
            var address_settings = new BrokerAddressSettings(env);

            var console_server = new ConsoleServer(env, openshift);
            bind_event(address_source, 'addresses_defined', console_server.addresses);

            var server_promise = console_server.listen(env);
            server_promise.then(() => {
                if (env.ADDRESS_SPACE_TYPE === 'brokered') {
                    bind_event(address_source, 'addresses_defined', console_server.metrics);
                    console_server.listen_health(env);
                    var event_logger = env.ENABLE_EVENT_LOGGER === 'true' ? kubernetes.post_event : undefined;
                    var bc = require('../lib/broker_controller.js').create_agent(event_logger, address_settings.get_address_settings_async.bind(address_settings));
                    bind_event(bc, 'address_stats_retrieved', console_server.addresses, 'update_existing');
                    bind_event(bc, 'connection_stats_retrieved', console_server.connections, 'set');
                    bind_event(bc, 'address_stats_retrieved', address_source, 'check_status');
                    bind_event(address_source, 'addresses_defined', bc);
                    bind_event(address_plans_source, 'addressplans_defined', address_source, 'check_address_plans');
                    bind_event(address_space_plan_source, 'addressspaceplan_defined', address_source, 'check_address_plans');

                    bc.connect(tls_options.get_client_options({
                        host: env.BROKER_SERVICE_HOST, port: env.BROKER_SERVICE_PORT, username: 'console',
                        idle_time_out: 'AMQP_IDLE_TIMEOUT' in process.env ? process.env.AMQP_IDLE_TIMEOUT : 300000
                    }));

                    address_space_plan_source.start();
                    address_plans_source.start(address_space_plan_source);
                } else {
                    //assume standard address space for now
                    var StandardStats = require('../lib/standard_stats.js');
                    var stats = new StandardStats();
                    stats.init(console_server);

                    var ragent = new Ragent();
                    ragent.disable_connectivity = true;
                    bind_event(address_source, 'addresses_ready', ragent, 'sync_addresses');

                    ragent.broker_address_settings = address_settings.get_address_settings_async.bind(address_settings);

                    bind_event(address_source, 'addresses_ready', ragent, 'sync_addresses');
                    bind_event(address_source, 'addresses_plans_changed', ragent, 'sync_address_addressplan');
                    ragent.start_listening(env);
                    ragent.listen_health({HEALTH_PORT:8888});
                }

                address_source.start(address_plans_source);

                process.on('SIGTERM', function () {
                    log.info('Shutdown started');
                    var exitHandler = function () {
                        process.exit(0);
                    };
                    var timeout = setTimeout(exitHandler, 2000);

                    console_server.close(function() {
                        log.info("Console server closed");
                        clearTimeout(timeout);
                        exitHandler();
                    });
                });

                setInterval(() => {
                    log.info("Heap statistics : %j", v8.getHeapStatistics());
                }, 60000);
            }).catch((e) => {log.error("Failed to listen ", e)})

        });
    }).catch((e) => {log.error("Failed to check for openshift", e)});
}

if (require.main === module) {
    start(process.env);
} else {
    module.exports.start = start;
}
