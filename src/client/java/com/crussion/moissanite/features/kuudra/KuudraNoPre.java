package com.crussion.moissanite.features.kuudra;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.kuudra.KuudraPhaseTracker;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class KuudraNoPre {
	public static final String NO_EQUALS = "No Equals!";
	public static final String NO_SHOP = "No Shop!";
	public static final String NO_X_CANNON = "No X Cannon!";
	public static final String NO_SLASH = "No Slash!";
	public static final String NO_TRIANGLE = "No Triangle!";
	public static final String NO_X = "No X!";
	public static final String NO_SQUARE = "No Square!";

	public enum PickupSpot {
		SHOP(new Vec3(-81.0, 76.0, -143.0), "Shop"),
		X(new Vec3(-142.5, 77.0, -148.0), "X"),
		X_CANNON(new Vec3(-143.0, 76.0, -125.0), "X Cannon"),
		EQUALS(new Vec3(-65.5, 76.0, -87.5), "Equals"),
		SLASH(new Vec3(-113.5, 77.0, -68.5), "Slash"),
		TRIANGLE(new Vec3(-67.5, 77.0, -122.5), "Triangle"),
		SQUARE(new Vec3(-143.0, 76.0, -80.0), "Square"),
		NONE(new Vec3(0.0, 0.0, 0.0), "None");

		private final Vec3 location;
		private final String displayText;

		PickupSpot(Vec3 location, String displayText) {
			this.location = location;
			this.displayText = displayText;
		}

		public Vec3 getLocation() {
			return location;
		}

		public String getDisplayText() {
			return displayText;
		}
	}

	private static PickupSpot preSpot = PickupSpot.NONE;
	private static PickupSpot missing = PickupSpot.NONE;
	private static final List<Vec3> crates = new ArrayList<>();
	private static boolean initialized;

	private static final Map<String, PickupSpot> PHRASE_TO_SPOT = new HashMap<>();
	static {
		PHRASE_TO_SPOT.put(NO_EQUALS, PickupSpot.EQUALS);
		PHRASE_TO_SPOT.put(NO_SHOP, PickupSpot.SHOP);
		PHRASE_TO_SPOT.put(NO_X_CANNON, PickupSpot.X_CANNON);
		PHRASE_TO_SPOT.put(NO_SLASH, PickupSpot.SLASH);
		PHRASE_TO_SPOT.put(NO_TRIANGLE, PickupSpot.TRIANGLE);
		PHRASE_TO_SPOT.put(NO_X, PickupSpot.X);
		PHRASE_TO_SPOT.put(NO_SQUARE, PickupSpot.SQUARE);
	}

	private KuudraNoPre() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ClientTickEvents.END_CLIENT_TICK.register(KuudraNoPre::onClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
			preSpot = PickupSpot.NONE;
			missing = PickupSpot.NONE;
			crates.clear();
		});
	}

	private static void onClientTick(Minecraft client) {
		if (!Boolean.TRUE.equals(UiDefinitions.NO_PRE.get())) {
			return;
		}
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea("Kuudra's Hollow")
				|| KuudraPhaseTracker.getPhase() > KuudraPhaseTracker.PHASE_SUPPLY) {
			return;
		}

		crates.clear();
		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof Giant)) {
				continue;
			}
			if (entity.getY() >= 67) {
				continue;
			}
			float yawRad = (float) ((entity.getYRot() + 130.0f) * (Math.PI / 180.0));
			double offsetX = 3.7 * Math.cos(yawRad);
			double offsetZ = 3.7 * Math.sin(yawRad);
			double x = entity.getX() + 0.5 + offsetX;
			double z = entity.getZ() + 0.5 + offsetZ;
			crates.add(new Vec3(x, 75, z));
		}
	}

	public static void onSystemChat(Component message) {
		if (!Boolean.TRUE.equals(UiDefinitions.NO_PRE.get())) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea("Kuudra's Hollow")) {
			return;
		}

		String msg = TextNormalizer.stripFormattingCodes(message == null ? "" : message.getString());

		if (msg.equals("[NPC] Elle: Head over to the main platform, I will join you when I get a bite!")) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null) {
				return;
			}
			Vec3 playerLocation = mc.player.position();

			if (PickupSpot.TRIANGLE.getLocation().distanceTo(playerLocation) < 15) {
				preSpot = PickupSpot.TRIANGLE;
			} else if (PickupSpot.X.getLocation().distanceTo(playerLocation) < 30) {
				preSpot = PickupSpot.X;
			} else if (PickupSpot.EQUALS.getLocation().distanceTo(playerLocation) < 15) {
				preSpot = PickupSpot.EQUALS;
			} else if (PickupSpot.SLASH.getLocation().distanceTo(playerLocation) < 15) {
				preSpot = PickupSpot.SLASH;
			} else {
				preSpot = PickupSpot.NONE;
			}

			FeatureChat.sendPrefixed("No Pre", preSpot == PickupSpot.NONE
					? ChatFormatting.RED + "Didn't register your pre-spot because you didn't get there in time."
					: ChatFormatting.GREEN + "Pre-spot: " + preSpot.getDisplayText());
			return;
		}

		if (msg.equals("[NPC] Elle: Not again!") && preSpot != PickupSpot.NONE) {
			boolean pre = false;
			boolean second = false;
			String alert = "";

			for (Vec3 supply : crates) {
				double dPre = preSpot.getLocation().distanceTo(supply);
				if (dPre < 18) {
					pre = true;
				}

				if (preSpot == PickupSpot.TRIANGLE) {
					double dShop = PickupSpot.SHOP.getLocation().distanceTo(supply);
					if (dShop < 18) {
						second = true;
					}
				} else if (preSpot == PickupSpot.X) {
					double dXCannon = PickupSpot.X_CANNON.getLocation().distanceTo(supply);
					if (dXCannon < 16) {
						second = true;
					}
				} else if (preSpot == PickupSpot.SLASH) {
					double dSquare = PickupSpot.SQUARE.getLocation().distanceTo(supply);
					if (dSquare < 20) {
						second = true;
					}
				}
			}

			if (!(second && pre)) {
				if (!pre && preSpot != PickupSpot.NONE) {
					alert = "No " + preSpot.getDisplayText() + "!";
				} else if (!second) {
					switch (preSpot) {
						case TRIANGLE -> alert = NO_SHOP;
						case X -> alert = NO_X_CANNON;
						case SLASH -> alert = NO_SQUARE;
						default -> {
						}
					}
				}

				if (!alert.isEmpty()) {
					Minecraft mc = Minecraft.getInstance();
					if (mc.player != null && mc.player.connection != null) {
						mc.player.connection.sendCommand("pc " + alert);
					}
				}
			}
			return;
		}

		for (Map.Entry<String, PickupSpot> e : PHRASE_TO_SPOT.entrySet()) {
			if (msg.contains(e.getKey())) {
				missing = e.getValue();
				break;
			}
		}
	}

	public static PickupSpot getMissing() {
		return missing;
	}

	public static PickupSpot getPreSpot() {
		return preSpot;
	}
}
