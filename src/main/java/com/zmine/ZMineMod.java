package com.zmine;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zmine.network.SyncThirstPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

@SuppressWarnings("null")
public class ZMineMod implements ModInitializer {
	public static final String MOD_ID = "zmine";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Inicializando Mod ZMine (Apocalipse Zumbi)...");
		ZMineWorldGen.registerWorldGen();
		PayloadTypeRegistry.clientboundPlay().register(SyncThirstPayload.TYPE, SyncThirstPayload.CODEC);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
