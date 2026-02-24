package com.crussion.moissanite.util.kuudra;

import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.MagmaCube;

public final class KuudraPhaseTracker {
	private static final String KUUDRA_HOLLOW = "Kuudra's Hollow";
	private static final int WORLD_LOAD_RESET_WINDOW_TICKS = 80;

	public static final int PHASE_NONE = -1;
	public static final int PHASE_START = 0;
	public static final int PHASE_SUPPLY = 1;
	public static final int PHASE_BUILD = 2;
	public static final int PHASE_EATEN = 3;
	public static final int PHASE_STUN = 4;
	public static final int PHASE_HIT = 5;
	public static final int PHASE_SKIP = 6;
	public static final int PHASE_DPS = 7;
	public static final int PHASE_END = 8;

	private static boolean initialized;
	private static int phase = PHASE_NONE;
	private static int worldLoadResetTicksRemaining;
	private static MagmaCube kuudraEntity;
	private static long clientTicks;

	private KuudraPhaseTracker() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		reset();

		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
			worldLoadResetTicksRemaining = WORLD_LOAD_RESET_WINDOW_TICKS;
			tryResetForKuudraWorldLoad();
		});

		ClientTickEvents.END_CLIENT_TICK.register(KuudraPhaseTracker::handleClientTick);
	}

	public static int getPhase() {
		return phase;
	}

	public static void reset() {
		phase = PHASE_NONE;
		KuudraPhase.resetAll();
		kuudraEntity = null;
	}

	public static void onSystemChat(Component message) {
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}

		String msg = TextNormalizer.stripFormattingCodes(message == null ? "" : message.getString());
		if (msg.isBlank()) {
			return;
		}

		long now = System.currentTimeMillis();
		long ticks = clientTicks;

		if (msg.contains("[NPC] Elle: Talk with me to begin!")) {
			phase = PHASE_START;
			KuudraPhase.NONE.begin(now, ticks);
			return;
		}
		if (msg.contains("[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!")) {
			phase = PHASE_SUPPLY;
			KuudraPhase.SUPPLIES.begin(now, ticks);
			KuudraPhase.END.start(now, ticks);
			return;
		}
		if (msg.contains("[NPC] Elle: OMG! Great work collecting my supplies!")) {
			phase = PHASE_BUILD;
			KuudraPhase.BUILD.begin(now, ticks);
			return;
		}
		if (msg.contains(
				"[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!")) {
			if (phase != PHASE_HIT) {
				phase = PHASE_EATEN;
				KuudraPhase.EATEN.begin(now, ticks);
			}
			return;
		}
		if (msg.contains("has been eaten by Kuudra!") && !msg.contains("Elle")) {
			if (phase == PHASE_EATEN) {
				phase = PHASE_STUN;
				KuudraPhase.STUN.begin(now, ticks);
			}
			return;
		}
		if (msg.contains("destroyed one of Kuudra's pods!")) {
			if (phase != PHASE_HIT) {
				phase = PHASE_HIT;
				KuudraPhase.DPS.begin(now, ticks);
			}
			return;
		}
		if (msg.contains("[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!")) {
			phase = PHASE_SKIP;
			KuudraPhase.SKIP.begin(now, ticks);
			return;
		}
		if (msg.contains("KUUDRA DOWN!") || msg.contains("DEFEAT")) {
			phase = PHASE_END;
			KuudraPhase.KILL.end(now, ticks);
			KuudraPhase.END.end(now, ticks);
			KuudraPhase.endMissedPhases(now, ticks);
		}
	}

	private static void handleClientTick(Minecraft client) {
		if (worldLoadResetTicksRemaining > 0) {
			worldLoadResetTicksRemaining--;
			tryResetForKuudraWorldLoad();
		}

		clientTicks++;

		if (client == null || client.player == null || client.level == null) {
			kuudraEntity = null;
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			kuudraEntity = null;
			return;
		}
		if (phase == PHASE_SKIP && Math.round(client.player.getY()) < 10L) {
			phase = PHASE_DPS;
			KuudraPhase.KILL.begin(System.currentTimeMillis(), clientTicks);
		}

		kuudraEntity = KuudraEntityFinder.findKuudra(client);
	}

	private static void tryResetForKuudraWorldLoad() {
		if (worldLoadResetTicksRemaining <= 0) {
			return;
		}
		if (!ScoreboardAreaMatcher.isInArea(KUUDRA_HOLLOW)) {
			return;
		}
		reset();
		worldLoadResetTicksRemaining = 0;
	}

	public static MagmaCube getKuudraEntity() {
		return kuudraEntity;
	}

	public static KuudraPhase getPhaseEnum() {
		if (phase == PHASE_NONE || phase == PHASE_START)
			return KuudraPhase.NONE;
		if (phase == PHASE_SUPPLY)
			return KuudraPhase.SUPPLIES;
		if (phase == PHASE_BUILD)
			return KuudraPhase.BUILD;
		if (phase == PHASE_EATEN)
			return KuudraPhase.EATEN;
		if (phase == PHASE_STUN)
			return KuudraPhase.STUN;
		if (phase == PHASE_HIT)
			return KuudraPhase.DPS;
		if (phase == PHASE_SKIP)
			return KuudraPhase.SKIP;
		if (phase == PHASE_DPS)
			return KuudraPhase.KILL;
		if (phase == PHASE_END)
			return KuudraPhase.END;
		return KuudraPhase.NONE;
	}
}
