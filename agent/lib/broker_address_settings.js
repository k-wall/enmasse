/*
 * Copyright 2018 Red Hat Inc.
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

var log = require("./log.js").logger();
var kubernetes = require('./kubernetes.js');
var myutils = require('./utils.js');
var events = require('events');
var util = require('util');


function BrokerAddressSettings(config) {
    this.config = config || {};
    this.selector = "";
    this.broker_settings = {};
    this.global_max_size = this.read_global_max_size();
    events.EventEmitter.call(this);
}

util.inherits(BrokerAddressSettings, events.EventEmitter);

BrokerAddressSettings.prototype.generate_address_settings = function (address, global_max_size) {
    var planStatus = address.status.planStatus;
    if (planStatus && planStatus.resources && planStatus.resources.broker > 0) {

        var r = planStatus.resources.broker;
        var p = planStatus.partitions;
        var allocation = (r && p) ? (r / p) : (r) ? r : undefined;
        if (allocation) {
            var maxSizeBytes = Math.round(allocation * global_max_size);
            return {
                maxSizeBytes: maxSizeBytes
            };
        }
    }
    log.info('no broker resource required for %s, therefore not applying address settings', address.name);
};

BrokerAddressSettings.prototype.read_global_max_size = function () {
    return this.config.BROKER_GLOBAL_MAX_SIZE ? myutils.parseToBytes(this.config.BROKER_GLOBAL_MAX_SIZE) : 0;
};

BrokerAddressSettings.prototype.get_address_settings_async = function (address, global_max_size_promise) {
    var self = this;
    if (self.global_max_size > 0) {
        return Promise.resolve(self.generate_address_settings(address, self.global_max_size));
    } else if (global_max_size_promise) {
        return global_max_size_promise.then(function (global_max_size) {
            return Promise.resolve(self.generate_address_settings(address, global_max_size));
        }).catch(function (e) {
            log.debug('no global max, therefore not applying address settings %s', e);
            return Promise.reject(e);
        });
    } else {
        log.debug('no global max, therefore not applying address settings');
        return Promise.resolve();
    }
};

module.exports = BrokerAddressSettings;
