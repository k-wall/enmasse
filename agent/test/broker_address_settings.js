/*
 * Copyright 2017 Red Hat Inc.
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

var assert = require('assert');
var events = require('events');
var util = require('util');
var BrokerAddressSettings = require('../lib/broker_address_settings.js');

describe('broker address settings', function() {
    var brokerAddressSettings;
    var config = { BROKER_GLOBAL_MAX_SIZE: "64MB" };

    it('get address settings async - returns setting maxSizeBytes calculated from infra', function(done) {
        config.BROKER_GLOBAL_MAX_SIZE="64MB";
        brokerAddressSettings = new BrokerAddressSettings(config);
        brokerAddressSettings.get_address_settings_async({address:'foo',type:'queue', plan: 'small-queue', status: {planStatus: {name: "small-queue", resources: {broker: 0.2}}}}).then(function (result) {
            assert.equal(13421773, result.maxSizeBytes);
            done();
        });
    });

    it('get address settings async - returns setting maxSizeBytes calculated from infra with partitions', function(done) {
        config.BROKER_GLOBAL_MAX_SIZE="64MB";
        brokerAddressSettings = new BrokerAddressSettings(config);
        brokerAddressSettings.get_address_settings_async({address:'foo',type:'queue', plan: 'small-queue', status: {planStatus: {name: "small-queue", partitions: 2, resources: {broker: 0.2}}}}).then(function (result) {
            assert.equal(6710886, result.maxSizeBytes);
            done();
        });
    });

    it('get address settings async - returns setting maxSizeBytes calculated from broker', function(done) {
        config.BROKER_GLOBAL_MAX_SIZE=undefined;
        brokerAddressSettings = new BrokerAddressSettings(config);
        brokerAddressSettings.get_address_settings_async({address:'foo',type:'queue', plan: 'small-queue', status: {planStatus: {name: "small-queue", resources: {broker: 0.1}}}}, Promise.resolve(1000000)).then(function (result) {
            assert.equal(100000, result.maxSizeBytes);
            done();
        });
    });
});