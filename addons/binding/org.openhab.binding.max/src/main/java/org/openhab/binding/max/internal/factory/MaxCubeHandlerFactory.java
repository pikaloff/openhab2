/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.max.internal.factory;

import java.util.Hashtable;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.max.MaxBinding;
import org.openhab.binding.max.config.MaxCubeBridgeConfiguration;
import org.openhab.binding.max.internal.discovery.MaxDeviceDiscoveryService;
import org.openhab.binding.max.internal.handler.MaxCubeBridgeHandler;
import org.openhab.binding.max.internal.handler.MaxCubeHandler;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MaxCubeHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Marcel Verpaalen - Initial contribution
 */

public class MaxCubeHandlerFactory extends BaseThingHandlerFactory {

	private Logger logger = LoggerFactory.getLogger(MaxCubeHandlerFactory.class);
	private ServiceRegistration<?> discoveryServiceReg;


	@Override
	public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
			ThingUID thingUID, ThingUID bridgeUID) {

		if (MaxBinding.CUBEBRIDGE_THING_TYPE.equals(thingTypeUID)) {
			ThingUID cubeBridgeUID = getBridgeThingUID(thingTypeUID, thingUID, configuration);
			return super.createThing(thingTypeUID, configuration, cubeBridgeUID, null);
		}
		if (supportsThingType(thingTypeUID)) {
			ThingUID deviceUID = getMaxCubeDeviceUID(thingTypeUID, thingUID, configuration, bridgeUID);
			return super.createThing(thingTypeUID, configuration, deviceUID , bridgeUID);
		}
		throw new IllegalArgumentException("The thing type " + thingTypeUID
				+ " is not supported by the binding.");
	}

	@Override
	public boolean supportsThingType(ThingTypeUID thingTypeUID) {
		return MaxBinding.SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
	}


	private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID, ThingUID thingUID,
			Configuration configuration) {
		if (thingUID == null) {
			String SerialNumber = (String) configuration.get(MaxBinding.SERIAL_NUMBER);
			thingUID = new ThingUID(thingTypeUID, SerialNumber);
		}
		return thingUID;
	}

	private ThingUID getMaxCubeDeviceUID(ThingTypeUID thingTypeUID, ThingUID thingUID,
			Configuration configuration , ThingUID bridgeUID ) {
		String SerialNumber = (String) configuration.get(MaxBinding.SERIAL_NUMBER);

		if (thingUID == null) {
			thingUID = new ThingUID(thingTypeUID, SerialNumber, bridgeUID.getId());
		}
		return thingUID;
	}



	private void registerDeviceDiscoveryService(MaxCubeBridgeHandler maxCubeBridgeHandler) {
		MaxDeviceDiscoveryService discoveryService = new MaxDeviceDiscoveryService(maxCubeBridgeHandler);
		discoveryService.activate();
		this.discoveryServiceReg = bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>());
	}

	@Override
	protected void removeHandler(ThingHandler thingHandler) {
		if(this.discoveryServiceReg!=null) {
			MaxDeviceDiscoveryService service = (MaxDeviceDiscoveryService) bundleContext.getService(discoveryServiceReg.getReference());
			service.deactivate();
			discoveryServiceReg.unregister();
			discoveryServiceReg = null;
		}
		super.removeHandler(thingHandler);
	}

	@Override
	protected ThingHandler createHandler(Thing thing) {
		if (thing.getThingTypeUID().equals(MaxBinding.CUBEBRIDGE_THING_TYPE)) {
			MaxCubeBridgeHandler handler = new MaxCubeBridgeHandler((Bridge) thing);
			registerDeviceDiscoveryService(handler);
			return handler;
		} else if (supportsThingType(thing.getThingTypeUID())) {
			return new MaxCubeHandler(thing);            
		} else {
			logger.debug("ThingHandler not found for {}" , thing.getThingTypeUID());
			return null;
		}
	}

}