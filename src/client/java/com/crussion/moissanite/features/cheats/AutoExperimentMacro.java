package com.crussion.moissanite.features.cheats;

import com.crussion.moissanite.definitions.UiDefinitions;
import com.crussion.moissanite.util.chat.FeatureChat;
import com.crussion.moissanite.util.input.PlayerInputActions;
import com.crussion.moissanite.util.inventory.GuiClickThrottle;
import com.crussion.moissanite.util.inventory.HotbarItemSearch;
import com.crussion.moissanite.util.rotation.RotationController;
import com.crussion.moissanite.util.scoreboard.ScoreboardAreaMatcher;
import com.crussion.moissanite.util.text.TextNormalizer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AutoExperimentMacro {
	private static final String FEATURE_NAME = "Auto Enchanting";
	private static final String MAIN_TITLE = "experimentation table";
	private static final String CHRONOMATRON_TITLE = "chronomatron";
	private static final String ULTRASEQUENCER_TITLE = "ultrasequencer";
	private static final String SUPERPAIRS_TITLE = "superpairs (";
	private static final String PRIVATE_ISLAND = "private island";
	private static final String YOUR_ISLAND = "your island";
	private static final int TABLE_SEARCH_RADIUS = 8;
	private static final int TABLE_ENTITY_SEARCH_RADIUS = 12;
	private static final int FIND_TABLE_TIMEOUT_TICKS = 80;
	private static final int ROTATE_TIMEOUT_TICKS = 100;
	private static final int OPEN_TIMEOUT_TICKS = 80;
	private static final int MENU_TIMEOUT_TICKS = 120;
	private static final int CLAIM_TIMEOUT_TICKS = 6_000;
	private static final int MACRO_TIMEOUT_TICKS = 12_000;
	private static final int WAIT_AFTER_CLICK_TICKS = 5;
	private static final int WAIT_AFTER_SWAP_TICKS = 2;
	private static final int BOTTLE_CLICK_INTERVAL_TICKS = 4;
	private static final int DEFAULT_SUPERPAIRS_CLICK_DELAY_MS = 1_200;
	private static final int DEFAULT_SUPERPAIRS_AFTER_PAIR_DELAY_MS = 3_000;
	private static final int DEFAULT_SUPERPAIRS_PAIR_CLAIMS = 3;
	private static final int MIN_SUPERPAIRS_DELAY_MS = 0;
	private static final int MAX_SUPERPAIRS_DELAY_MS = 10_000;
	private static final int MIN_SUPERPAIRS_PAIR_CLAIMS = 1;
	private static final int MAX_SUPERPAIRS_PAIR_CLAIMS = 20;
	private static final double SUPERPAIRS_XP_FALLBACK_UNCOVERED_RATIO = 0.90D;
	private static final int RENEW_EXPERIMENTS_LEVEL_COST = 50;
	private static final int TITANIC_LEVEL_THRESHOLD = 110;
	private static final double ROTATION_MULTIPLIER = 0.6D;
	private static final double TABLE_AIM_Y_OFFSET = 1.0D;
	private static final String GRAND_BOTTLE_ID = "GRAND_EXP_BOTTLE";
	private static final String TITANIC_BOTTLE_ID = "TITANIC_EXP_BOTTLE";
	private static final String SKYHANNI_ULTRA_RARE_BOOK_MARKER = "ultra-rare book";
	private static final String[] SUPERPAIRS_EXTRA_CLAIM_TARGETS = {
			"growth vi",
			"protection vi",
			"titanic experience bottle",
			"gold bottle cap"
	};

	private static final ExperimentTier[] CHRONOMATRON_TIERS = {
			ExperimentTier.METAPHYSICAL,
			ExperimentTier.TRANSCENDENT,
			ExperimentTier.SUPREME,
			ExperimentTier.GRAND,
			ExperimentTier.HIGH
	};
	private static final ExperimentTier[] ULTRASEQUENCER_TIERS = {
			ExperimentTier.METAPHYSICAL,
			ExperimentTier.TRANSCENDENT,
			ExperimentTier.SUPREME
	};
	private static final ExperimentTier[] SUPERPAIRS_TIERS = {
			ExperimentTier.METAPHYSICAL,
			ExperimentTier.TRANSCENDENT,
			ExperimentTier.SUPREME,
			ExperimentTier.GRAND,
			ExperimentTier.HIGH,
			ExperimentTier.BEGINNER
	};
	private static final StakeOption[] CHRONOMATRON_STAKES = {
			new StakeOption(ExperimentTier.METAPHYSICAL, 24, 40),
			new StakeOption(ExperimentTier.TRANSCENDENT, 23, 35),
			new StakeOption(ExperimentTier.SUPREME, 22, 30),
			new StakeOption(ExperimentTier.GRAND, 21, 25),
			new StakeOption(ExperimentTier.HIGH, 20, 20)
	};
	private static final StakeOption[] ULTRASEQUENCER_STAKES = {
			new StakeOption(ExperimentTier.METAPHYSICAL, 23, 40),
			new StakeOption(ExperimentTier.TRANSCENDENT, 22, 30),
			new StakeOption(ExperimentTier.SUPREME, 21, 25)
	};
	private static final int[] SUPERPAIRS_BOARD_SLOTS = {
			10, 11, 12, 13, 14, 15, 16,
			19, 20, 21, 22, 23, 24, 25,
			28, 29, 30, 31, 32, 33, 34,
			37, 38, 39, 40, 41, 42, 43
	};
	private static final String[] SKYHANNI_MAX_LEVEL_ENCHANT_TARGETS = {
			"angler vi",
			"bane of arthropods vii",
			"big brain v",
			"blast protection vii",
			"blessing vi",
			"caster vi",
			"cayenne v",
			"chance v",
			"cleave vi",
			"counter-strike v",
			"critical vii",
			"cubism vi",
			"charm vi",
			"corruption v",
			"dedication iv",
			"delicate v",
			"divine gift iii",
			"drain v",
			"efficiency x",
			"ender slayer vii",
			"execute vi",
			"experience v",
			"feather falling x",
			"fire aspect iii",
			"fire protection vii",
			"first strike v",
			"fortune iv",
			"frail vii",
			"ferocious mana x",
			"forest pledge v",
			"giant killer vii",
			"gravity vi",
			"great spook i",
			"green thumb v",
			"growth vii",
			"harvesting vi",
			"hardened mana x",
			"ice cold v",
			"infinite quiver x",
			"lapidary v",
			"lethality vi",
			"life steal v",
			"looting v",
			"luck vii",
			"luck of the sea vii",
			"lure vi",
			"magnet vi",
			"mana steal iii",
			"mana vampire x",
			"overload v",
			"paleontologist v",
			"pesterminator vi",
			"piscary vii",
			"power vii",
			"prismatic v",
			"projectile protection vii",
			"prosecute vi",
			"prosperity v",
			"protection vii",
			"quantum v",
			"quick bite v",
			"rainbow iii",
			"reflection v",
			"rejuvenate v",
			"replenish i",
			"respiration iv",
			"respite v",
			"scavenger vi",
			"scuba v",
			"sharpness vii",
			"small brain v",
			"smarty pants v",
			"smite vii",
			"snipe iv",
			"spiked hook vii",
			"sugar rush iii",
			"sunder vi",
			"smoldering v",
			"stealth i",
			"strong mana x",
			"tabasco iii",
			"thunderbolt vii",
			"thunderlord vii",
			"tidal iii",
			"titan killer vii",
			"transylvanian v",
			"triple-strike v",
			"true protection i",
			"turbo-cacti v",
			"turbo-cane v",
			"turbo-carrot v",
			"turbo-cocoa v",
			"turbo-melon v",
			"turbo-mushrooms v",
			"turbo-potato v",
			"turbo-pumpkin v",
			"turbo-warts v",
			"turbo-wheat v",
			"turbo-rose v",
			"turbo-moonflower v",
			"turbo-sunflower v",
			"vampirism vi",
			"venomous vii",
			"vicious v"
	};

	private static boolean initialized;
	private static boolean running;
	private static MacroStep currentStep = MacroStep.IDLE;
	private static int macroElapsedTicks;
	private static int stepElapsedTicks;
	private static boolean stepStarted;
	private static Vec3 tableAimPoint;
	private static BottlePlan bottlePlan;
	private static int bottlesThrown;
	private static int lastSeenGuiContainerId = -1;
	private static String lastSeenGuiTitle = "";
	private static MacroStep reopenTableNextStep = MacroStep.IDLE;
	private static MacroStep afterBottlesTableStep = MacroStep.IDLE;
	private static final Map<String, Integer> superpairsFirstSlotsByEnchant = new HashMap<>();
	private static final Set<Integer> superpairsKnownHighTierSlots = new HashSet<>();
	private static final Set<Integer> superpairsClickedRevealSlots = new HashSet<>();
	private static final Set<Integer> superpairsIgnoredPrioritySlots = new HashSet<>();
	private static final Set<String> superpairsExhaustedPairKeys = new HashSet<>();
	private static SuperpairsPair activeSuperpairsPair;
	private static int activeSuperpairsPairClicksRemaining;
	private static boolean activeSuperpairsClickFirstNext;
	private static int lastSuperpairsClickTick = -ticksForDelayMs(DEFAULT_SUPERPAIRS_CLICK_DELAY_MS);

	private AutoExperimentMacro() {
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(AutoExperimentMacro::handleClientTick);
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
			if (running) {
				stopInternal("World changed, stopping macro.");
			} else {
				resetState();
			}
		});
	}

	public static boolean isRunning() {
		return running;
	}

	public static void start() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		if (!client.isSameThread()) {
			client.execute(AutoExperimentMacro::start);
			return;
		}
		startInternal(client);
	}

	public static void stop() {
		stop("Stopped from GUI.");
	}

	public static void stop(String reason) {
		Minecraft client = Minecraft.getInstance();
		if (client != null && !client.isSameThread()) {
			String message = reason;
			client.execute(() -> stop(message));
			return;
		}
		stopInternal(reason);
	}

	private static void startInternal(Minecraft client) {
		if (running) {
			sendMessage("Macro is already running.");
			return;
		}
		if (client == null || client.player == null || client.level == null) {
			sendMessage("Cannot start without a loaded world.");
			return;
		}
		if (!isInPrivateIsland()) {
			sendMessage("Not on Private Island.");
			return;
		}

		if (client.screen != null) {
			client.setScreen(null);
		}
		client.player.closeContainer();

		running = true;
		macroElapsedTicks = 0;
		tableAimPoint = null;
		bottlePlan = null;
		bottlesThrown = 0;
		GuiClickThrottle.reset();
		resetSuperpairsState();
		enterStep(MacroStep.FIND_TABLE);
		sendMessage("Macro started.");
	}

	private static void handleClientTick(Minecraft client) {
		if (!running) {
			return;
		}
		if (client == null || client.player == null || client.level == null) {
			stopInternal("Player or world unloaded.");
			return;
		}
		if (!isInPrivateIsland()) {
			stopInternal("Left Private Island.");
			return;
		}

		macroElapsedTicks++;
		stepElapsedTicks++;
		if (macroElapsedTicks > MACRO_TIMEOUT_TICKS) {
			stopInternal("Macro timeout.");
			return;
		}
		trackGuiForClickDelay(client);

		switch (currentStep) {
			case FIND_TABLE -> handleFindTable(client);
			case ROTATE_TO_TABLE -> handleRotateToTable(client, MacroStep.OPEN_TABLE);
			case OPEN_TABLE -> handleOpenTable(client, MacroStep.CHECK_INITIAL_RENEW_EXPERIMENTS);
			case CHECK_INITIAL_RENEW_EXPERIMENTS -> handleInitialRenewExperiments(client);
			case CLICK_CHRONOMATRON -> handleClickMainEntry(client, "chronomatron", MacroStep.SELECT_CHRONOMATRON);
			case SELECT_CHRONOMATRON -> handleSelectTier(
					client,
					CHRONOMATRON_TIERS,
					false,
					MacroStep.WAIT_CHRONOMATRON_REWARD,
					"Chronomatron");
			case WAIT_CHRONOMATRON_REWARD -> handleWaitForReward(
					client,
					CHRONOMATRON_TITLE,
					MacroStep.WAIT_MAIN_AFTER_CHRONOMATRON,
					MacroStep.CLICK_ULTRASEQUENCER);
			case WAIT_MAIN_AFTER_CHRONOMATRON -> handleWaitForMainOrClaim(
					client,
					MacroStep.CLICK_ULTRASEQUENCER);
			case CLICK_ULTRASEQUENCER -> handleClickMainEntry(client, "ultrasequencer", MacroStep.SELECT_ULTRASEQUENCER);
			case SELECT_ULTRASEQUENCER -> handleSelectTier(
					client,
					ULTRASEQUENCER_TIERS,
					false,
					MacroStep.WAIT_ULTRASEQUENCER_REWARD,
					"Ultrasequencer");
			case WAIT_ULTRASEQUENCER_REWARD -> handleWaitForReward(
					client,
					ULTRASEQUENCER_TITLE,
					MacroStep.WAIT_MAIN_AFTER_ULTRASEQUENCER,
					MacroStep.CLICK_SUPERPAIRS);
			case WAIT_MAIN_AFTER_ULTRASEQUENCER -> handleWaitForMainOrClaim(
					client,
					MacroStep.CLICK_SUPERPAIRS);
			case CLICK_SUPERPAIRS -> handleClickMainEntry(client, "superpairs", MacroStep.SELECT_SUPERPAIRS);
			case SELECT_SUPERPAIRS -> handleSelectSuperpairs(client, false);
			case ROTATE_FOR_BOTTLES -> handleRotateForBottles();
			case SWAP_BOTTLES -> handleSwapBottles();
			case THROW_BOTTLES -> handleThrowBottles();
			case WAIT_AFTER_BOTTLES -> handleWaitAfterBottles();
			case ROTATE_BACK_TO_TABLE -> handleRotateToTable(client, MacroStep.REOPEN_TABLE);
			case REOPEN_TABLE -> handleOpenTable(client, afterBottlesTableStep());
			case ROTATE_REOPEN_TABLE -> handleRotateToTable(client, MacroStep.REOPEN_TABLE_DYNAMIC);
			case REOPEN_TABLE_DYNAMIC -> handleOpenTable(client, reopenTableNextStep);
			case CLICK_SUPERPAIRS_AFTER_BOTTLES -> handleClickMainEntry(
					client,
					"superpairs",
					MacroStep.SELECT_SUPERPAIRS_AFTER_BOTTLES);
			case SELECT_SUPERPAIRS_AFTER_BOTTLES -> handleSelectSuperpairs(client, true);
			case SOLVE_SUPERPAIRS -> handleSolveSuperpairs(client);
			case WAIT_AFTER_SUPERPAIRS_CLAIM -> handleWaitAfterSuperpairsClaim(client);
			case CHECK_RENEW_EXPERIMENTS -> handleCheckRenewExperiments(client);
			case WAIT_AFTER_RENEW_EXPERIMENTS -> handleWaitAfterRenewExperiments(client);
			case IDLE -> stopInternal("Macro stopped.");
		}
	}

	private static void handleFindTable(Minecraft client) {
		if (!stepStarted || stepElapsedTicks % 10 == 0) {
			stepStarted = true;
			tableAimPoint = findExperimentationTableAimPoint(client);
			if (tableAimPoint != null) {
				sendMessage("Found table at " + formatVec(tableAimPoint) + ".");
				enterStep(MacroStep.ROTATE_TO_TABLE);
				return;
			}
		}
		if (stepElapsedTicks > FIND_TABLE_TIMEOUT_TICKS) {
			stopInternal("Could not find an Experimentation Table nearby.");
		}
	}

	private static void handleRotateToTable(Minecraft client, MacroStep nextStep) {
		if (tableAimPoint == null) {
			stopInternal("Lost Experimentation Table target.");
			return;
		}
		if (!stepStarted) {
			stepStarted = true;
			boolean rotated = RotationController.rotateTo(
					tableAimPoint.x,
					tableAimPoint.y,
					tableAimPoint.z,
					ROTATION_MULTIPLIER);
			if (!rotated) {
				stopInternal("Failed to start rotation to table.");
				return;
			}
		}
		if (!RotationController.isRotating() && stepElapsedTicks >= WAIT_AFTER_SWAP_TICKS) {
			enterStep(nextStep);
			return;
		}
		if (stepElapsedTicks > ROTATE_TIMEOUT_TICKS) {
			stopInternal("Rotation to table timed out.");
		}
	}

	private static void handleOpenTable(Minecraft client, MacroStep nextStep) {
		if (!stepStarted) {
			stepStarted = true;
			PlayerInputActions.rightClick();
		}
		if (isMainMenu(client)) {
			enterStep(nextStep);
			return;
		}
		if (stepElapsedTicks > WAIT_AFTER_CLICK_TICKS && currentMenu(client) != null) {
			String title = currentTitle(client);
			if (!MAIN_TITLE.equals(title)) {
				closeScreen(client);
				stopInternal("Opened \"" + title + "\" instead of Experimentation Table.");
				return;
			}
		}
		if (stepElapsedTicks > OPEN_TIMEOUT_TICKS) {
			stopInternal("Experimentation Table did not open.");
		}
	}

	private static void handleClickMainEntry(Minecraft client, String entryName, MacroStep nextStep) {
		if (isExperimentEntryMenu(client, entryName)) {
			enterStep(nextStep);
			return;
		}
		if (!isMainMenu(client)) {
			if (stepStarted && currentMenu(client) != null) {
				enterStep(nextStep);
				return;
			}
			if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Expected Experimentation Table menu.");
			}
			return;
		}
		if (isCompletedMainExperiment(currentMenu(client), entryName)) {
			sendMessage(formatExperimentName(entryName) + " is already completed, skipping.");
			enterStep(completedExperimentSkipStep(entryName, nextStep));
			return;
		}
		if (stepStarted) {
			if (stepElapsedTicks >= WAIT_AFTER_CLICK_TICKS) {
				enterStep(nextStep);
			}
			return;
		}

		ChestMenu menu = currentMenu(client);
		int slot = findSlotContaining(menu, entryName);
		if (slot == -1) {
			stopInternal("Could not find " + entryName + " in the table menu.");
			return;
		}
		if (clickSlot(client, menu, slot)) {
			stepStarted = true;
		}
	}

	private static void handleInitialRenewExperiments(Minecraft client) {
		if (!isMainMenu(client)) {
			if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Expected Experimentation Table while checking renewal.");
			}
			return;
		}

		ChestMenu menu = currentMenu(client);
		int renewSlot = findRenewExperimentsSlot(menu);
		if (renewSlot == -1) {
			enterStep(MacroStep.CLICK_CHRONOMATRON);
			return;
		}

		if (prepareRenewalBottlesIfNeeded(client, menu, renewSlot, MacroStep.CHECK_INITIAL_RENEW_EXPERIMENTS)) {
			return;
		}

		if (clickSlot(client, menu, renewSlot)) {
			sendMessage("Renewed experiments before starting macro.");
			enterStep(MacroStep.WAIT_AFTER_RENEW_EXPERIMENTS);
		}
	}

	private static void handleSelectTier(
			Minecraft client,
			ExperimentTier[] tiers,
			boolean allowInsufficientXp,
			MacroStep nextStep,
			String experimentName) {
		if (isExperimentRunningTitle(client, experimentName)) {
			enterStep(nextStep);
			return;
		}

		ChestMenu menu = currentMenu(client);
		if (menu == null) {
			if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Expected " + experimentName + " experiment menu.");
			}
			return;
		}

		int stakeSlot = findHighestStakeSlot(client, menu);
		if (stakeSlot != -1) {
			clickStartOptionOrTimeout(client, menu, stakeSlot, experimentName);
			return;
		}

		TierOption option = findHighestTierOption(menu, tiers, allowInsufficientXp);
		if (option == null) {
			if (stepStarted && isMainMenu(client)) {
				if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
					stopInternal(experimentName + " did not start after selecting an experiment.");
				}
				return;
			}
			if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("No unlocked " + experimentName + " experiment found.");
			}
			return;
		}
		clickStartOptionOrTimeout(client, menu, option.slot(), experimentName);
	}

	private static void handleWaitForReward(
			Minecraft client,
			String experimentTitle,
			MacroStep waitMainStep,
			MacroStep nextMainStep) {
		if (isMainMenu(client)) {
			enterStep(nextMainStep);
			return;
		}

		ChestMenu menu = currentMenu(client);
		if (menu != null) {
			int claimSlot = findClaimSlot(menu);
			if (claimSlot != -1 && canClickOnInterval()) {
				if (clickSlot(client, menu, claimSlot)) {
					enterStep(waitMainStep);
				}
				return;
			}
		}

		String title = currentTitle(client);
		if (!title.isBlank()
				&& !title.contains(experimentTitle)
				&& !title.contains("experiment over")
				&& !title.contains("reward")
				&& !title.contains("claim")) {
			if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Unexpected menu while waiting for reward: " + title + ".");
			}
			return;
		}

		if (stepElapsedTicks > CLAIM_TIMEOUT_TICKS) {
			stopInternal("Timed out waiting for " + experimentTitle + " reward.");
		}
	}

	private static void handleWaitForMainOrClaim(Minecraft client, MacroStep nextStep) {
		if (isMainMenu(client)) {
			enterStep(nextStep);
			return;
		}

		ChestMenu menu = currentMenu(client);
		if (menu == null) {
			if (stepElapsedTicks >= WAIT_AFTER_CLICK_TICKS) {
				sendMessage("Reward claim closed the table, reopening.");
				reopenTableThen(nextStep);
			}
			return;
		}

		if (menu != null) {
			int claimSlot = findClaimSlot(menu);
			if (claimSlot != -1 && canClickOnInterval()) {
				clickSlot(client, menu, claimSlot);
				return;
			}
		}

		if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
			stopInternal("Reward claim did not return to Experimentation Table.");
		}
	}

	private static void reopenTableThen(MacroStep nextStep) {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.player != null && client.level != null && tableAimPoint == null) {
			tableAimPoint = findExperimentationTableAimPoint(client);
		}
		if (tableAimPoint == null) {
			stopInternal("Lost Experimentation Table target while reopening.");
			return;
		}
		reopenTableNextStep = nextStep == null ? MacroStep.IDLE : nextStep;
		enterStep(MacroStep.ROTATE_REOPEN_TABLE);
	}

	private static void handleSelectSuperpairs(Minecraft client, boolean afterBottles) {
		if (isSuperpairsRunningTitle(client)) {
			resetSuperpairsState();
			enterStep(MacroStep.SOLVE_SUPERPAIRS);
			return;
		}

		ChestMenu menu = currentMenu(client);
		if (menu == null) {
			if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Expected Superpairs menu.");
			}
			return;
		}

		TierOption option = findHighestSuperpairsTierOption(menu);
		if (option == null) {
			if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("No unlocked Superpairs experiment found.");
			}
			return;
		}

		int currentLevel = client.player.experienceLevel;
		int requiredLevel = option.tier().requiredLevel();
		if (currentLevel < requiredLevel) {
			if (afterBottles) {
				stopInternal("Still below " + requiredLevel + " XP levels after using bottles.");
				return;
			}
			BottlePlan plan = createBottlePlan(client, requiredLevel);
			if (plan == null) {
				return;
			}
			bottlePlan = plan;
			bottlesThrown = 0;
			afterBottlesTableStep = MacroStep.CLICK_SUPERPAIRS_AFTER_BOTTLES;
			closeScreen(client);
			sendMessage("Need level " + requiredLevel + ", throwing " + plan.count() + " "
					+ plan.type().displayName() + ".");
			enterStep(MacroStep.ROTATE_FOR_BOTTLES);
			return;
		}

		if (!stepStarted && clickSlot(client, menu, option.slot())) {
			stepStarted = true;
		}
		if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
			stopInternal("Superpairs did not start after selecting an experiment.");
		}
	}

	private static void handleSolveSuperpairs(Minecraft client) {
		if (isMainMenu(client)) {
			enterStep(MacroStep.CHECK_RENEW_EXPERIMENTS);
			return;
		}

		ChestMenu menu = currentMenu(client);
		if (menu == null) {
			if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Expected Superpairs board.");
			}
			return;
		}

		String title = currentTitle(client);
		if (title.contains("experiment over") || title.contains("superpairs rewards")) {
			int claimSlot = findClaimSlot(menu);
			if (claimSlot != -1 && canClickSuperpairs()) {
				if (clickSuperpairsSlot(client, menu, claimSlot)) {
					lastSuperpairsClickTick = macroElapsedTicks;
					enterStep(MacroStep.WAIT_AFTER_SUPERPAIRS_CLAIM);
				}
			}
			return;
		}
		if (!isSuperpairsRunningTitle(client)) {
			if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Unexpected Superpairs menu: " + title + ".");
			}
			return;
		}

		if (activeSuperpairsPair != null) {
			clickActiveSuperpairsPair(client, menu);
			return;
		}

		SuperpairsPair pair = scanSuperpairsBoard(menu);
		if (pair != null) {
			startSuperpairsPair(pair);
			clickActiveSuperpairsPair(client, menu);
			return;
		}

		if (isSuperpairsMostlyUncovered(menu) && !hasVisibleSuperpairsPriorityReward(menu)) {
			SuperpairsPair xpPair = findHighestEnchantingXpPair(menu);
			if (xpPair != null) {
				startSuperpairsPair(xpPair);
				clickActiveSuperpairsPair(client, menu);
				return;
			}
		}

		int revealSlot = findNextSuperpairsRevealSlot(menu);
		if (revealSlot != -1 && canClickSuperpairs()) {
			if (clickSuperpairsSlot(client, menu, revealSlot)) {
				lastSuperpairsClickTick = macroElapsedTicks;
				superpairsClickedRevealSlots.add(revealSlot);
			}
			return;
		}

		int fillerSlot = findAnySuperpairsClickableSlot(menu);
		if (fillerSlot != -1 && canClickSuperpairs()) {
			if (clickSuperpairsSlot(client, menu, fillerSlot)) {
				lastSuperpairsClickTick = macroElapsedTicks;
			}
		}
	}

	private static void handleWaitAfterSuperpairsClaim(Minecraft client) {
		if (isMainMenu(client)) {
			enterStep(MacroStep.CHECK_RENEW_EXPERIMENTS);
			return;
		}

		ChestMenu menu = currentMenu(client);
		if (menu == null) {
			if (stepElapsedTicks >= WAIT_AFTER_CLICK_TICKS) {
				reopenTableThen(MacroStep.CHECK_RENEW_EXPERIMENTS);
			}
			return;
		}

		int claimSlot = findClaimSlot(menu);
		if (claimSlot != -1 && canClickSuperpairs()) {
			if (clickSuperpairsSlot(client, menu, claimSlot)) {
				lastSuperpairsClickTick = macroElapsedTicks;
			}
			return;
		}

		if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
			reopenTableThen(MacroStep.CHECK_RENEW_EXPERIMENTS);
		}
	}

	private static void handleCheckRenewExperiments(Minecraft client) {
		if (!isMainMenu(client)) {
			ChestMenu menu = currentMenu(client);
			if (menu == null) {
				if (stepElapsedTicks >= WAIT_AFTER_CLICK_TICKS) {
					reopenTableThen(MacroStep.CHECK_RENEW_EXPERIMENTS);
				}
				return;
			}
			if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
				stopInternal("Expected Experimentation Table while checking renewal.");
			}
			return;
		}

		ChestMenu menu = currentMenu(client);
		int renewSlot = findRenewExperimentsSlot(menu);
		if (renewSlot == -1) {
			finishMacro();
			return;
		}

		if (prepareRenewalBottlesIfNeeded(client, menu, renewSlot, MacroStep.CHECK_RENEW_EXPERIMENTS)) {
			return;
		}

		if (clickSlot(client, menu, renewSlot)) {
			sendMessage("Renewed experiments, restarting macro.");
			enterStep(MacroStep.WAIT_AFTER_RENEW_EXPERIMENTS);
		}
	}

	private static void handleWaitAfterRenewExperiments(Minecraft client) {
		if (isMainMenu(client) && stepElapsedTicks >= WAIT_AFTER_CLICK_TICKS) {
			macroElapsedTicks = 0;
			resetSuperpairsState();
			enterStep(MacroStep.CLICK_CHRONOMATRON);
			return;
		}

		if (currentMenu(client) == null && stepElapsedTicks >= WAIT_AFTER_CLICK_TICKS) {
			reopenTableThen(MacroStep.CLICK_CHRONOMATRON);
			return;
		}

		if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
			stopInternal("Experiment renewal did not return to the table.");
		}
	}

	private static void startSuperpairsPair(SuperpairsPair pair) {
		activeSuperpairsPair = pair;
		int pairClaims = configuredSuperpairsPairClaims();
		activeSuperpairsPairClicksRemaining = pairClaims * 2;
		activeSuperpairsClickFirstNext = true;
		if (pair.xpValue() > 0) {
			sendMessage("Matching enchanting XP pair " + pair.reward() + " " + formatPairClaimCount(pairClaims) + ".");
		} else {
			sendMessage("Matching priority reward " + pair.reward() + " " + formatPairClaimCount(pairClaims) + ".");
		}
	}

	private static void clickActiveSuperpairsPair(Minecraft client, ChestMenu menu) {
		if (activeSuperpairsPair == null || !canClickSuperpairs()) {
			return;
		}

		boolean clickedSecondSlot = !activeSuperpairsClickFirstNext;
		int slot = activeSuperpairsClickFirstNext ? activeSuperpairsPair.firstSlot() : activeSuperpairsPair.secondSlot();
		if (!clickSuperpairsSlot(client, menu, slot)) {
			return;
		}

		lastSuperpairsClickTick = clickedSecondSlot
				? macroElapsedTicks + configuredSuperpairsAfterPairDelayTicks() - configuredSuperpairsClickDelayTicks()
				: macroElapsedTicks;
		activeSuperpairsClickFirstNext = !activeSuperpairsClickFirstNext;
		activeSuperpairsPairClicksRemaining--;
		if (activeSuperpairsPairClicksRemaining <= 0) {
			markSuperpairsPairExhausted(activeSuperpairsPair);
			superpairsKnownHighTierSlots.remove(activeSuperpairsPair.firstSlot());
			superpairsKnownHighTierSlots.remove(activeSuperpairsPair.secondSlot());
			superpairsFirstSlotsByEnchant.remove(activeSuperpairsPair.reward());
			activeSuperpairsPair = null;
		}
	}

	private static void markSuperpairsPairExhausted(SuperpairsPair pair) {
		if (pair == null) {
			return;
		}
		superpairsExhaustedPairKeys.add(superpairsPairKey(pair.reward(), pair.firstSlot(), pair.secondSlot()));
		superpairsIgnoredPrioritySlots.add(pair.firstSlot());
		superpairsIgnoredPrioritySlots.add(pair.secondSlot());
	}

	private static SuperpairsPair scanSuperpairsBoard(ChestMenu menu) {
		if (menu == null) {
			return null;
		}

		for (int slotIndex : SUPERPAIRS_BOARD_SLOTS) {
			if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
				continue;
			}
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}

			ItemStack stack = slot.getItem();
			if (isSuperpairsHiddenTile(stack)) {
				continue;
			}

			String reward = priorityRewardSignature(stack);
			if (reward.isBlank()) {
				continue;
			}
			if (superpairsIgnoredPrioritySlots.contains(slotIndex)) {
				continue;
			}

			Integer firstSlot = superpairsFirstSlotsByEnchant.get(reward);
			if (firstSlot == null) {
				superpairsFirstSlotsByEnchant.put(reward, slotIndex);
				superpairsKnownHighTierSlots.add(slotIndex);
				sendMessage("Found priority reward " + reward + " at slot " + slotIndex + ".");
				continue;
			}
			if (firstSlot != slotIndex) {
				if (!slotMatchesPriorityReward(menu, firstSlot, reward)) {
					superpairsFirstSlotsByEnchant.put(reward, slotIndex);
					superpairsKnownHighTierSlots.remove(firstSlot);
					superpairsKnownHighTierSlots.add(slotIndex);
					continue;
				}
				superpairsKnownHighTierSlots.add(slotIndex);
				if (isSuperpairsPairExhausted(reward, firstSlot, slotIndex)) {
					continue;
				}
				return new SuperpairsPair(reward, firstSlot, slotIndex, 0);
			}
		}

		return null;
	}

	private static boolean slotMatchesPriorityReward(ChestMenu menu, int slotIndex, String reward) {
		if (menu == null || reward == null || slotIndex < 0 || slotIndex >= menu.slots.size()) {
			return false;
		}
		Slot slot = menu.slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return false;
		}
		return reward.equals(priorityRewardSignature(slot.getItem()));
	}

	private static boolean isSuperpairsPairExhausted(String reward, int firstSlot, int secondSlot) {
		return superpairsExhaustedPairKeys.contains(superpairsPairKey(reward, firstSlot, secondSlot));
	}

	private static String superpairsPairKey(String reward, int firstSlot, int secondSlot) {
		int lowSlot = Math.min(firstSlot, secondSlot);
		int highSlot = Math.max(firstSlot, secondSlot);
		return reward + ":" + lowSlot + ":" + highSlot;
	}

	private static boolean hasVisibleSuperpairsPriorityReward(ChestMenu menu) {
		if (menu == null) {
			return false;
		}
		for (int slotIndex : SUPERPAIRS_BOARD_SLOTS) {
			if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
				continue;
			}
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			if (superpairsIgnoredPrioritySlots.contains(slotIndex)) {
				continue;
			}
			ItemStack stack = slot.getItem();
			if (!isSuperpairsHiddenTile(stack) && !priorityRewardSignature(stack).isBlank()) {
				return true;
			}
		}
		return false;
	}

	private static SuperpairsPair findHighestEnchantingXpPair(ChestMenu menu) {
		if (menu == null) {
			return null;
		}

		Map<Integer, Integer> firstSlotByXp = new HashMap<>();
		SuperpairsPair bestPair = null;
		for (int slotIndex : SUPERPAIRS_BOARD_SLOTS) {
			if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
				continue;
			}
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			ItemStack stack = slot.getItem();
			if (isSuperpairsHiddenTile(stack)) {
				continue;
			}

			int xp = enchantingXpAmount(stack);
			if (xp <= 0) {
				continue;
			}

			Integer firstSlot = firstSlotByXp.get(xp);
			if (firstSlot == null) {
				firstSlotByXp.put(xp, slotIndex);
				continue;
			}
			if (firstSlot != slotIndex && (bestPair == null || xp > bestPair.xpValue())) {
				bestPair = new SuperpairsPair(formatXpReward(xp), firstSlot, slotIndex, xp);
			}
		}
		return bestPair;
	}

	private static boolean isSuperpairsMostlyUncovered(ChestMenu menu) {
		if (menu == null) {
			return false;
		}

		int boardSlots = 0;
		int uncoveredSlots = 0;
		for (int slotIndex : SUPERPAIRS_BOARD_SLOTS) {
			if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
				continue;
			}
			boardSlots++;
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem() || !isSuperpairsHiddenTile(slot.getItem())) {
				uncoveredSlots++;
			}
		}
		return boardSlots > 0 && uncoveredSlots >= Math.ceil(boardSlots * SUPERPAIRS_XP_FALLBACK_UNCOVERED_RATIO);
	}

	private static int findNextSuperpairsRevealSlot(ChestMenu menu) {
		if (menu == null) {
			return -1;
		}

		for (int slotIndex : SUPERPAIRS_BOARD_SLOTS) {
			if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
				continue;
			}
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			if (!isSuperpairsHiddenTile(slot.getItem())) {
				continue;
			}
			if (superpairsKnownHighTierSlots.contains(slotIndex)) {
				continue;
			}
			if (superpairsClickedRevealSlots.contains(slotIndex)) {
				continue;
			}
			return slotIndex;
		}

		for (int slotIndex : SUPERPAIRS_BOARD_SLOTS) {
			if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
				continue;
			}
			Slot slot = menu.slots.get(slotIndex);
			if (slot != null && slot.hasItem() && isSuperpairsHiddenTile(slot.getItem())) {
				return slotIndex;
			}
		}
		return -1;
	}

	private static int findAnySuperpairsClickableSlot(ChestMenu menu) {
		if (menu == null) {
			return -1;
		}
		for (int slotIndex : SUPERPAIRS_BOARD_SLOTS) {
			if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
				continue;
			}
			Slot slot = menu.slots.get(slotIndex);
			if (slot != null && slot.hasItem() && !superpairsIgnoredPrioritySlots.contains(slotIndex)) {
				return slotIndex;
			}
		}
		return -1;
	}

	private static boolean canClickSuperpairs() {
		return macroElapsedTicks - lastSuperpairsClickTick >= configuredSuperpairsClickDelayTicks();
	}

	private static void handleRotateForBottles() {
		if (bottlePlan == null) {
			stopInternal("No bottle plan available.");
			return;
		}
		if (!stepStarted) {
			stepStarted = true;
			boolean rotated = RotationController.rotateYawPitch(-180.0D, 90.0D, ROTATION_MULTIPLIER, 1.2D, 1.2D);
			if (!rotated) {
				stopInternal("Failed to rotate for bottles.");
				return;
			}
		}
		if (!RotationController.isRotating() && stepElapsedTicks >= WAIT_AFTER_SWAP_TICKS) {
			enterStep(MacroStep.SWAP_BOTTLES);
			return;
		}
		if (stepElapsedTicks > ROTATE_TIMEOUT_TICKS) {
			stopInternal("Bottle rotation timed out.");
		}
	}

	private static void handleSwapBottles() {
		if (bottlePlan == null) {
			stopInternal("No bottle plan available.");
			return;
		}
		if (!stepStarted) {
			stepStarted = true;
			boolean swapped = HotbarItemSearch.swapHeldItem(bottlePlan.slot());
			if (!swapped) {
				stopInternal("Failed to swap to " + bottlePlan.type().displayName() + ".");
				return;
			}
		}
		if (stepElapsedTicks >= WAIT_AFTER_SWAP_TICKS) {
			enterStep(MacroStep.THROW_BOTTLES);
		}
	}

	private static void handleThrowBottles() {
		if (bottlePlan == null) {
			stopInternal("No bottle plan available.");
			return;
		}
		if (bottlesThrown >= bottlePlan.count()) {
			enterStep(MacroStep.WAIT_AFTER_BOTTLES);
			return;
		}
		if (!stepStarted || stepElapsedTicks % BOTTLE_CLICK_INTERVAL_TICKS == 0) {
			stepStarted = true;
			if (!selectedHotbarSlotMatchesBottle(bottlePlan)) {
				stopInternal("Selected hotbar slot no longer holds " + bottlePlan.type().displayName() + ".");
				return;
			}
			PlayerInputActions.rightClick();
			bottlesThrown++;
		}
	}

	private static void handleWaitAfterBottles() {
		if (stepElapsedTicks >= 20) {
			enterStep(MacroStep.ROTATE_BACK_TO_TABLE);
		}
	}

	private static MacroStep afterBottlesTableStep() {
		return afterBottlesTableStep == MacroStep.IDLE
				? MacroStep.CLICK_SUPERPAIRS_AFTER_BOTTLES
				: afterBottlesTableStep;
	}

	private static boolean prepareRenewalBottlesIfNeeded(
			Minecraft client,
			ChestMenu menu,
			int renewSlot,
			MacroStep retryStep) {
		if (client == null || client.player == null || menu == null || renewSlot < 0 || renewSlot >= menu.slots.size()) {
			return false;
		}
		int requiredLevel = renewRequiredLevel(menu.slots.get(renewSlot).getItem());
		if (requiredLevel <= 0 || client.player.experienceLevel >= requiredLevel) {
			return false;
		}

		BottlePlan plan = createBottlePlan(client, requiredLevel);
		if (plan == null) {
			return true;
		}
		bottlePlan = plan;
		bottlesThrown = 0;
		afterBottlesTableStep = retryStep;
		closeScreen(client);
		sendMessage("Need level " + requiredLevel + " to renew experiments, throwing "
				+ plan.count() + " " + plan.type().displayName() + ".");
		enterStep(MacroStep.ROTATE_FOR_BOTTLES);
		return true;
	}

	private static BottlePlan createBottlePlan(Minecraft client, int targetLevel) {
		BottleType type = targetLevel > TITANIC_LEVEL_THRESHOLD ? BottleType.TITANIC : BottleType.GRAND;
		int slot = findBottleSlot(client, type);
		if (slot == -1) {
			stopInternal("No " + type.displayName() + " found in hotbar.");
			return null;
		}

		int currentXp = currentTotalExperience(client);
		int targetXp = totalExperienceForLevel(targetLevel);
		int neededXp = targetXp - currentXp;
		if (neededXp <= 0) {
			return null;
		}

		int bottleXp = estimatedBottleXp(type);
		int neededBottles = Math.max(1, (int) Math.ceil(neededXp / (double) bottleXp));
		ItemStack stack = client.player.getInventory().getItem(slot);
		if (stack == null || stack.isEmpty() || stack.getCount() < neededBottles) {
			int available = stack == null || stack.isEmpty() ? 0 : stack.getCount();
			stopInternal("Need " + neededBottles + " " + type.displayName()
					+ " in hotbar, found " + available + ".");
			return null;
		}

		return new BottlePlan(type, slot, neededBottles);
	}

	private static Vec3 findExperimentationTableAimPoint(Minecraft client) {
		Vec3 namedEntity = findNamedExperimentationTableEntity(client);
		if (namedEntity != null) {
			BlockPos nearbyTable = findNearestEnchantingTableBlock(client, namedEntity, 4);
			if (nearbyTable != null) {
				return tableAimPoint(nearbyTable);
			}
			return namedEntity.add(0.0D, TABLE_AIM_Y_OFFSET, 0.0D);
		}

		BlockPos tableBlock = findNearestEnchantingTableBlock(client, client.player.position(), TABLE_SEARCH_RADIUS);
		return tableBlock == null ? null : tableAimPoint(tableBlock);
	}

	private static Vec3 tableAimPoint(BlockPos tableBlock) {
		return Vec3.atCenterOf(tableBlock).add(0.0D, TABLE_AIM_Y_OFFSET, 0.0D);
	}

	private static Vec3 findNamedExperimentationTableEntity(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			return null;
		}
		double maxDistSq = TABLE_ENTITY_SEARCH_RADIUS * TABLE_ENTITY_SEARCH_RADIUS;
		Vec3 best = null;
		double bestDistSq = Double.POSITIVE_INFINITY;
		Vec3 playerPos = client.player.position();
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity == null || !entity.isAlive()) {
				continue;
			}
			String name = TextNormalizer.normalize(entity.getName().getString());
			if (!name.contains(MAIN_TITLE)) {
				continue;
			}
			Vec3 pos = entity.position();
			double distSq = pos.distanceToSqr(playerPos);
			if (distSq > maxDistSq || distSq >= bestDistSq) {
				continue;
			}
			best = pos;
			bestDistSq = distSq;
		}
		return best;
	}

	private static BlockPos findNearestEnchantingTableBlock(Minecraft client, Vec3 center, int radius) {
		if (client == null || client.level == null || center == null) {
			return null;
		}

		BlockPos origin = BlockPos.containing(center);
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		BlockPos best = null;
		double bestDistSq = Double.POSITIVE_INFINITY;
		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					mutable.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
					Block block = client.level.getBlockState(mutable).getBlock();
					if (!isEnchantingTableBlock(block)) {
						continue;
					}
					Vec3 blockCenter = Vec3.atCenterOf(mutable);
					double distSq = blockCenter.distanceToSqr(center);
					if (distSq < bestDistSq) {
						best = mutable.immutable();
						bestDistSq = distSq;
					}
				}
			}
		}
		return best;
	}

	private static boolean isEnchantingTableBlock(Block block) {
		if (block == null) {
			return false;
		}
		var key = BuiltInRegistries.BLOCK.getKey(block);
		return key != null && "enchanting_table".equals(key.getPath());
	}

	private static boolean isExperimentRunningTitle(Minecraft client, String experimentName) {
		String title = currentTitle(client);
		if (title.isBlank()) {
			return false;
		}
		String experiment = TextNormalizer.normalize(experimentName);
		return title.startsWith(experiment + " (");
	}

	private static boolean isMainMenu(Minecraft client) {
		return MAIN_TITLE.equals(currentTitle(client));
	}

	private static String currentTitle(Minecraft client) {
		if (client == null || !(client.screen instanceof ContainerScreen containerScreen)) {
			return "";
		}
		return TextNormalizer.normalize(containerScreen.getTitle().getString());
	}

	private static ChestMenu currentMenu(Minecraft client) {
		if (client == null || client.player == null || !(client.player.containerMenu instanceof ChestMenu menu)) {
			return null;
		}
		return menu;
	}

	private static void trackGuiForClickDelay(Minecraft client) {
		ChestMenu menu = currentMenu(client);
		String title = currentTitle(client);
		if (menu == null || title.isBlank()) {
			lastSeenGuiContainerId = -1;
			lastSeenGuiTitle = "";
			return;
		}

		if (menu.containerId != lastSeenGuiContainerId || !title.equals(lastSeenGuiTitle)) {
			lastSeenGuiContainerId = menu.containerId;
			lastSeenGuiTitle = title;
			GuiClickThrottle.reset();
		}
	}

	private static int findSlotContaining(ChestMenu menu, String needle) {
		if (menu == null) {
			return -1;
		}
		String normalizedNeedle = TextNormalizer.normalize(needle);
		int maxSlot = containerSlotCount(menu);
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			if (normalizedStackName(slot.getItem()).contains(normalizedNeedle)) {
				return slotIndex;
			}
		}
		return -1;
	}

	private static TierOption findHighestTierOption(
			ChestMenu menu,
			ExperimentTier[] tiers,
			boolean allowInsufficientXp) {
		if (menu == null || tiers == null) {
			return null;
		}

		for (ExperimentTier tier : tiers) {
			String tierName = TextNormalizer.normalize(tier.displayName());
			int maxSlot = containerSlotCount(menu);
			for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
				Slot slot = menu.slots.get(slotIndex);
				if (slot == null || !slot.hasItem()) {
					continue;
				}
				ItemStack stack = slot.getItem();
				String name = normalizedStackName(stack);
				String text = normalizedStackText(stack);
				if (!name.contains(tierName)) {
					continue;
				}
				if (isTierSlotUsable(stack, text, allowInsufficientXp)) {
					return new TierOption(tier, slotIndex);
				}
			}
		}
		return null;
	}

	private static TierOption findHighestSuperpairsTierOption(ChestMenu menu) {
		if (menu == null) {
			return null;
		}

		int enchantingLevel = configuredEnchantingLevel();
		for (ExperimentTier tier : SUPERPAIRS_TIERS) {
			if (enchantingLevel < superpairsEnchantingRequirement(tier)) {
				continue;
			}
			TierOption option = findTierOption(menu, tier, true);
			if (option != null) {
				return option;
			}
		}
		return null;
	}

	private static TierOption findTierOption(ChestMenu menu, ExperimentTier tier, boolean allowInsufficientXp) {
		if (menu == null || tier == null) {
			return null;
		}

		String tierName = TextNormalizer.normalize(tier.displayName());
		int maxSlot = containerSlotCount(menu);
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			ItemStack stack = slot.getItem();
			String name = normalizedStackName(stack);
			String text = normalizedStackText(stack);
			if (!name.contains(tierName)) {
				continue;
			}
			if (isTierSlotUsable(stack, text, allowInsufficientXp)) {
				return new TierOption(tier, slotIndex);
			}
		}
		return null;
	}

	private static int findHighestStakeSlot(Minecraft client, ChestMenu menu) {
		if (!isStakesTitle(client) || menu == null) {
			return -1;
		}

		int layoutSlot = findHighestConfiguredStakeSlot(client, menu);
		if (layoutSlot != -1) {
			return layoutSlot;
		}

		return -1;
	}

	private static int findHighestConfiguredStakeSlot(Minecraft client, ChestMenu menu) {
		String title = currentTitle(client);
		StakeOption[] stakes = null;
		if (title.startsWith(CHRONOMATRON_TITLE)) {
			stakes = CHRONOMATRON_STAKES;
		} else if (title.startsWith(ULTRASEQUENCER_TITLE)) {
			stakes = ULTRASEQUENCER_STAKES;
		}
		if (stakes == null) {
			return -1;
		}

		int enchantingLevel = configuredEnchantingLevel();
		int maxSlot = containerSlotCount(menu);
		for (StakeOption stake : stakes) {
			if (enchantingLevel < stake.enchantingLevel()) {
				continue;
			}
			int slotIndex = stake.slot();
			if (slotIndex < 0 || slotIndex >= maxSlot) {
				continue;
			}
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			ItemStack stack = slot.getItem();
			String text = normalizedStackText(stack);
			if (isStakeOptionItem(stack) && isTierSlotUsable(stack, text, false)) {
				return slotIndex;
			}
		}
		return -1;
	}

	private static void clickStartOptionOrTimeout(
			Minecraft client,
			ChestMenu menu,
			int slot,
			String experimentName) {
		if (!stepStarted) {
			sendMessage("Selecting " + experimentName + " slot " + slot + ".");
			stepStarted = true;
		}
		if (clickSlot(client, menu, slot)) {
			return;
		}
		if (stepElapsedTicks > MENU_TIMEOUT_TICKS) {
			stopInternal(experimentName + " did not start after selecting an experiment.");
		}
	}

	private static boolean isStakesTitle(Minecraft client) {
		return currentTitle(client).contains("stakes");
	}

	private static boolean isSuperpairsRunningTitle(Minecraft client) {
		return currentTitle(client).startsWith(SUPERPAIRS_TITLE);
	}

	private static boolean isExperimentEntryMenu(Minecraft client, String entryName) {
		String title = currentTitle(client);
		String normalizedEntry = TextNormalizer.normalize(entryName);
		if (title.isBlank() || normalizedEntry.isBlank()) {
			return false;
		}
		if (normalizedEntry.contains("chronomatron")) {
			return title.startsWith(CHRONOMATRON_TITLE);
		}
		if (normalizedEntry.contains("ultrasequencer")) {
			return title.startsWith(ULTRASEQUENCER_TITLE);
		}
		return title.contains(normalizedEntry);
	}

	private static boolean isStakeOptionItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		String path = itemPath(stack);
		if (path.equals("gray_dye") || path.equals("light_gray_dye")) {
			return false;
		}
		return path.endsWith("_dye")
				|| path.equals("ink_sac")
				|| path.equals("cocoa_beans")
				|| path.equals("lapis_lazuli")
				|| path.equals("bone_meal");
	}

	private static boolean isSuperpairsHiddenTile(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		String path = itemPath(stack);
		return path.endsWith("_stained_glass_pane") || path.endsWith("_stained_glass");
	}

	private static String priorityRewardSignature(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}

		String highTierEnchant = highTierEnchantSignature(stack);
		if (!highTierEnchant.isBlank()) {
			return highTierEnchant;
		}

		String text = normalizedStackText(stack);
		for (String target : SUPERPAIRS_EXTRA_CLAIM_TARGETS) {
			if (containsNormalizedPhrase(text, target)) {
				return target;
			}
		}
		if (text.contains(TextNormalizer.normalize(TITANIC_BOTTLE_ID))) {
			return "titanic experience bottle";
		}
		return "";
	}

	private static String highTierEnchantSignature(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}

		String skyHanniSignature = skyHanniUltraRareBookSignature(stack);
		if (!skyHanniSignature.isBlank()) {
			return skyHanniSignature;
		}

		String text = normalizedStackText(stack);
		boolean looksLikeBook = itemPath(stack).equals("enchanted_book") || text.contains("enchanted book");
		if (looksLikeBook) {
			for (String target : SKYHANNI_MAX_LEVEL_ENCHANT_TARGETS) {
				if (text.contains(target)) {
					return target;
				}
			}
		}
		if (text.contains(SKYHANNI_ULTRA_RARE_BOOK_MARKER)) {
			return "skyhanni-ultra-rare:" + text;
		}
		return "";
	}

	private static int enchantingXpAmount(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}

		String text = normalizedStackText(stack);
		int marker = text.indexOf("enchanting xp");
		if (marker == -1) {
			marker = text.indexOf("enchanting exp");
		}
		if (marker == -1) {
			return 0;
		}

		int start = marker - 1;
		while (start >= 0 && Character.isWhitespace(text.charAt(start))) {
			start--;
		}
		if (start < 0) {
			return 0;
		}

		int end = start + 1;
		while (start >= 0) {
			char c = text.charAt(start);
			if (!Character.isDigit(c) && c != '.' && c != ',' && c != 'k' && c != 'm' && c != 'b') {
				break;
			}
			start--;
		}

		String amountText = text.substring(start + 1, end).replace(",", "");
		if (amountText.isBlank()) {
			return 0;
		}

		double multiplier = 1.0D;
		char suffix = amountText.charAt(amountText.length() - 1);
		if (suffix == 'k' || suffix == 'm' || suffix == 'b') {
			amountText = amountText.substring(0, amountText.length() - 1);
			multiplier = switch (suffix) {
				case 'k' -> 1_000.0D;
				case 'm' -> 1_000_000.0D;
				case 'b' -> 1_000_000_000.0D;
				default -> 1.0D;
			};
		}

		try {
			return Math.max(0, (int) Math.round(Double.parseDouble(amountText) * multiplier));
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}

	private static String formatXpReward(int xp) {
		if (xp >= 1_000_000 && xp % 1_000_000 == 0) {
			return (xp / 1_000_000) + "m enchanting xp";
		}
		if (xp >= 1_000 && xp % 1_000 == 0) {
			return (xp / 1_000) + "k enchanting xp";
		}
		return xp + " enchanting xp";
	}

	private static boolean containsNormalizedPhrase(String text, String phrase) {
		if (text == null || text.isBlank() || phrase == null || phrase.isBlank()) {
			return false;
		}
		String normalizedPhrase = TextNormalizer.normalize(phrase);
		int index = text.indexOf(normalizedPhrase);
		while (index != -1) {
			int before = index - 1;
			int after = index + normalizedPhrase.length();
			boolean beforeBoundary = before < 0 || !Character.isLetterOrDigit(text.charAt(before));
			boolean afterBoundary = after >= text.length() || !Character.isLetterOrDigit(text.charAt(after));
			if (beforeBoundary && afterBoundary) {
				return true;
			}
			index = text.indexOf(normalizedPhrase, index + 1);
		}
		return false;
	}

	private static String skyHanniUltraRareBookSignature(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null || lore.lines().isEmpty()) {
			return "";
		}

		List<net.minecraft.network.chat.Component> lines = lore.lines();
		for (int i = 0; i < lines.size(); i++) {
			String line = normalizedLoreLine(lines, i);
			if (!line.contains(SKYHANNI_ULTRA_RARE_BOOK_MARKER)) {
				continue;
			}

			String skyHanniEnchantLine = normalizedLoreLine(lines, i + 2);
			if (isLikelyEnchantNameLine(skyHanniEnchantLine)) {
				return skyHanniEnchantLine;
			}

			for (int j = i + 1; j < lines.size(); j++) {
				String fallbackLine = normalizedLoreLine(lines, j);
				if (isLikelyEnchantNameLine(fallbackLine)) {
					return fallbackLine;
				}
			}
		}
		return "";
	}

	private static String normalizedLoreLine(List<net.minecraft.network.chat.Component> lines, int index) {
		if (lines == null || index < 0 || index >= lines.size()) {
			return "";
		}
		return TextNormalizer.normalize(lines.get(index).getString());
	}

	private static boolean isLikelyEnchantNameLine(String line) {
		if (line == null || line.isBlank()) {
			return false;
		}
		if (line.contains(SKYHANNI_ULTRA_RARE_BOOK_MARKER)
				|| line.contains("enchanted book")
				|| line.contains("click")
				|| line.contains("reward")
				|| line.contains("superpairs")
				|| line.contains("experiment")) {
			return false;
		}
		return true;
	}

	private static boolean isTierSlotUsable(ItemStack stack, String text, boolean allowInsufficientXp) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		if (text.contains("click to play")) {
			return true;
		}
		if (text.contains("not unlocked")
				|| text.contains(" locked")
				|| text.contains("locked!")
				|| text.contains("locked.")
				|| text.contains("on cooldown")
				|| text.contains("cooldown")
				|| text.contains("already completed")
				|| text.contains("unavailable")
				|| text.contains("complete previous")) {
			return false;
		}

		boolean insufficientXp = text.contains("not enough")
				|| text.contains("insufficient")
				|| text.contains("need more experience")
				|| text.contains("need more xp");
		if (insufficientXp && allowInsufficientXp) {
			return true;
		}
		if (stack.is(Items.BARRIER)) {
			return false;
		}
		return !insufficientXp;
	}

	private static int findClaimSlot(ChestMenu menu) {
		if (menu == null) {
			return -1;
		}
		int maxSlot = containerSlotCount(menu);
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			String text = normalizedStackText(slot.getItem());
			if (text.contains("claim reward")
					|| text.contains("claim rewards")
					|| text.contains("click to claim")) {
				return slotIndex;
			}
		}
		String title = currentTitle(Minecraft.getInstance());
		if (title.contains("experiment over") || title.contains("superpairs rewards")) {
			return findExperimentOverClaimSlot(menu);
		}
		return -1;
	}

	private static int findExperimentOverClaimSlot(ChestMenu menu) {
		int maxSlot = containerSlotCount(menu);
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			ItemStack stack = slot.getItem();
			String path = itemPath(stack);
			if (path.equals("redstone_block") || path.equals("barrier")) {
				continue;
			}
			String text = normalizedStackText(stack);
			if (path.contains("player_head")
					|| text.contains("game closed")
					|| text.contains("highest series")
					|| text.contains("rewards")) {
				return slotIndex;
			}
		}
		return -1;
	}

	private static int findRenewExperimentsSlot(ChestMenu menu) {
		if (menu == null) {
			return -1;
		}
		int maxSlot = containerSlotCount(menu);
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			ItemStack stack = slot.getItem();
			if (isRenewExperimentsItem(stack)) {
				return slotIndex;
			}
		}
		return -1;
	}

	private static boolean isRenewExperimentsItem(ItemStack stack) {
		return normalizedStackName(stack).contains("renew experiments")
				|| normalizedStackText(stack).contains("renew experiments");
	}

	private static int renewRequiredLevel(ItemStack stack) {
		String text = normalizedStackText(stack);
		int marker = text.indexOf("xp level");
		if (marker == -1) {
			return text.contains("cannot afford") && isRenewExperimentsItem(stack) ? RENEW_EXPERIMENTS_LEVEL_COST : 0;
		}
		int start = marker - 1;
		while (start >= 0 && Character.isWhitespace(text.charAt(start))) {
			start--;
		}
		int end = start + 1;
		while (start >= 0 && Character.isDigit(text.charAt(start))) {
			start--;
		}
		if (end <= start + 1) {
			return 0;
		}
		try {
			return Integer.parseInt(text.substring(start + 1, end));
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}

	private static boolean hasPendingExperiment(ChestMenu menu) {
		return menuContains(menu, "pending experiment");
	}

	private static boolean menuContains(ChestMenu menu, String needle) {
		if (menu == null) {
			return false;
		}
		String normalizedNeedle = TextNormalizer.normalize(needle);
		int maxSlot = containerSlotCount(menu);
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			if (normalizedStackText(slot.getItem()).contains(normalizedNeedle)) {
				return true;
			}
		}
		return false;
	}

	private static int containerSlotCount(ChestMenu menu) {
		if (menu == null || menu.slots == null) {
			return 0;
		}
		return Mth.clamp(menu.slots.size() - 36, 0, menu.slots.size());
	}

	private static boolean selectedHotbarSlotMatchesBottle(BottlePlan plan) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || plan == null) {
			return false;
		}
		return matchesBottle(client.player.getMainHandItem(), plan.type());
	}

	private static int findBottleSlot(Minecraft client, BottleType type) {
		if (client == null || client.player == null || type == null) {
			return -1;
		}

		Inventory inventory = client.player.getInventory();
		int hotbarSize = Inventory.getSelectionSize();
		for (int slot = 0; slot < hotbarSize; slot++) {
			if (matchesBottle(inventory.getItem(slot), type)) {
				return slot;
			}
		}
		return -1;
	}

	private static boolean matchesBottle(ItemStack stack, BottleType type) {
		if (stack == null || stack.isEmpty() || type == null) {
			return false;
		}
		String text = normalizedStackText(stack);
		return text.contains(TextNormalizer.normalize(type.displayName()))
				|| text.contains(TextNormalizer.normalize(type.skyblockId()));
	}

	private static boolean isCompletedMainExperiment(ChestMenu menu, String entryName) {
		if (menu == null || entryName == null) {
			return false;
		}
		String normalizedEntry = TextNormalizer.normalize(entryName);
		if (!normalizedEntry.contains("chronomatron") && !normalizedEntry.contains("ultrasequencer")) {
			return false;
		}

		int maxSlot = containerSlotCount(menu);
		for (int slotIndex = 0; slotIndex < maxSlot; slotIndex++) {
			Slot slot = menu.slots.get(slotIndex);
			if (slot == null || !slot.hasItem()) {
				continue;
			}
			String text = normalizedStackText(slot.getItem());
			if (!text.contains("experiment completed")) {
				continue;
			}
			int column = slotIndex % 9;
			if (normalizedEntry.contains("chronomatron") && column < 4) {
				return true;
			}
			if (normalizedEntry.contains("ultrasequencer") && column > 4) {
				return true;
			}
		}
		return false;
	}

	private static String formatExperimentName(String entryName) {
		if (entryName == null || entryName.isBlank()) {
			return "Experiment";
		}
		String normalized = TextNormalizer.normalize(entryName);
		if (normalized.contains("chronomatron")) {
			return "Chronomatron";
		}
		if (normalized.contains("ultrasequencer")) {
			return "Ultrasequencer";
		}
		if (normalized.contains("superpairs")) {
			return "Superpairs";
		}
		return entryName;
	}

	private static MacroStep completedExperimentSkipStep(String entryName, MacroStep fallbackStep) {
		String normalized = TextNormalizer.normalize(entryName);
		if (normalized.contains("chronomatron")) {
			return MacroStep.CLICK_ULTRASEQUENCER;
		}
		if (normalized.contains("ultrasequencer")) {
			return MacroStep.CLICK_SUPERPAIRS;
		}
		return fallbackStep;
	}

	private static String itemPath(ItemStack stack) {
		if (stack == null || stack.isEmpty() || stack.getItem() == null) {
			return "";
		}
		var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return key == null ? "" : key.getPath();
	}

	private static String normalizedStackText(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}

		StringBuilder builder = new StringBuilder(256);
		builder.append(stack.getHoverName().getString()).append(' ');
		builder.append(stack.getDisplayName().getString()).append(' ');
		builder.append(stack.getComponentsPatch()).append(' ');

		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (net.minecraft.network.chat.Component line : lore.lines()) {
				builder.append(line.getString()).append(' ');
			}
		}

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData != null && !customData.isEmpty()) {
			builder.append(customData.copyTag());
		}
		return TextNormalizer.normalize(builder.toString());
	}

	private static String normalizedStackName(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}
		return TextNormalizer.normalize(stack.getHoverName().getString() + " " + stack.getDisplayName().getString());
	}

	private static boolean clickSlot(Minecraft client, ChestMenu menu, int slot) {
		return GuiClickThrottle.clickSlot(client, menu, slot);
	}

	private static boolean clickSuperpairsSlot(Minecraft client, ChestMenu menu, int slot) {
		int clickDelayMs = configuredSuperpairsClickDelayMs();
		return GuiClickThrottle.clickSlot(
				client,
				menu,
				slot,
				clickDelayMs,
				clickDelayMs);
	}

	private static boolean canClickOnInterval() {
		return stepElapsedTicks % WAIT_AFTER_CLICK_TICKS == 0;
	}

	private static void closeScreen(Minecraft client) {
		if (client == null || client.player == null) {
			return;
		}
		client.player.closeContainer();
	}

	private static boolean isInPrivateIsland() {
		return ScoreboardAreaMatcher.isInArea(PRIVATE_ISLAND) || ScoreboardAreaMatcher.isInArea(YOUR_ISLAND);
	}

	private static int estimatedBottleXp(BottleType type) {
		int enchantingLevel = configuredEnchantingLevel();
		double multiplier = 1.0D + (enchantingLevel * 0.05D);
		return (int) Math.round(type.baseXp() * multiplier);
	}

	private static int configuredEnchantingLevel() {
		Integer value = UiDefinitions.AUTO_EXPERIMENTS_ENCHANTING_LEVEL.get();
		int configured = value == null ? 60 : value;
		return Mth.clamp(configured, 1, 60);
	}

	private static int configuredSuperpairsClickDelayMs() {
		Integer value = UiDefinitions.AUTO_EXPERIMENTS_SUPERPAIRS_CLICK_DELAY.get();
		int configured = value == null ? DEFAULT_SUPERPAIRS_CLICK_DELAY_MS : value;
		return Mth.clamp(configured, MIN_SUPERPAIRS_DELAY_MS, MAX_SUPERPAIRS_DELAY_MS);
	}

	private static int configuredSuperpairsClickDelayTicks() {
		return ticksForDelayMs(configuredSuperpairsClickDelayMs());
	}

	private static int configuredSuperpairsAfterPairDelayTicks() {
		Integer value = UiDefinitions.AUTO_EXPERIMENTS_SUPERPAIRS_AFTER_PAIR_DELAY.get();
		int configured = value == null ? DEFAULT_SUPERPAIRS_AFTER_PAIR_DELAY_MS : value;
		return ticksForDelayMs(Mth.clamp(configured, MIN_SUPERPAIRS_DELAY_MS, MAX_SUPERPAIRS_DELAY_MS));
	}

	private static int configuredSuperpairsPairClaims() {
		Integer value = UiDefinitions.AUTO_EXPERIMENTS_SUPERPAIRS_PAIR_CLAIMS.get();
		int configured = value == null ? DEFAULT_SUPERPAIRS_PAIR_CLAIMS : value;
		return Mth.clamp(configured, MIN_SUPERPAIRS_PAIR_CLAIMS, MAX_SUPERPAIRS_PAIR_CLAIMS);
	}

	private static int ticksForDelayMs(int delayMs) {
		return Math.max(0, (int) Math.ceil(delayMs / 50.0D));
	}

	private static String formatPairClaimCount(int pairClaims) {
		return pairClaims == 1 ? "once" : pairClaims + " times";
	}

	private static int superpairsEnchantingRequirement(ExperimentTier tier) {
		if (tier == null) {
			return Integer.MAX_VALUE;
		}
		return switch (tier) {
			case METAPHYSICAL -> 50;
			case TRANSCENDENT -> 40;
			case SUPREME -> 30;
			case GRAND -> 25;
			case HIGH -> 20;
			case BEGINNER -> 10;
		};
	}

	private static int currentTotalExperience(Minecraft client) {
		if (client == null || client.player == null) {
			return 0;
		}
		int level = Math.max(0, client.player.experienceLevel);
		int base = totalExperienceForLevel(level);
		int partial = Math.round(client.player.experienceProgress * experienceToNextLevel(level));
		return base + Math.max(0, partial);
	}

	private static int totalExperienceForLevel(int level) {
		int safeLevel = Math.max(0, level);
		if (safeLevel <= 16) {
			return (safeLevel * safeLevel) + (6 * safeLevel);
		}
		if (safeLevel <= 31) {
			return (int) Math.floor((2.5D * safeLevel * safeLevel) - (40.5D * safeLevel) + 360.0D);
		}
		return (int) Math.floor((4.5D * safeLevel * safeLevel) - (162.5D * safeLevel) + 2_220.0D);
	}

	private static int experienceToNextLevel(int level) {
		int safeLevel = Math.max(0, level);
		if (safeLevel <= 15) {
			return (2 * safeLevel) + 7;
		}
		if (safeLevel <= 30) {
			return (5 * safeLevel) - 38;
		}
		return (9 * safeLevel) - 158;
	}

	private static void enterStep(MacroStep step) {
		currentStep = step;
		stepElapsedTicks = 0;
		stepStarted = false;
	}

	private static void finishMacro() {
		resetState();
		sendMessage("Macro finished.");
	}

	private static void stopInternal(String reason) {
		boolean wasRunning = running;
		resetState();
		if (wasRunning) {
			sendMessage(reason == null || reason.isBlank() ? "Macro stopped." : reason);
		}
	}

	private static void resetState() {
		running = false;
		currentStep = MacroStep.IDLE;
		macroElapsedTicks = 0;
		stepElapsedTicks = 0;
		stepStarted = false;
		tableAimPoint = null;
		bottlePlan = null;
		bottlesThrown = 0;
		lastSeenGuiContainerId = -1;
		lastSeenGuiTitle = "";
		reopenTableNextStep = MacroStep.IDLE;
		afterBottlesTableStep = MacroStep.IDLE;
		GuiClickThrottle.reset();
		resetSuperpairsState();
	}

	private static void resetSuperpairsState() {
		superpairsFirstSlotsByEnchant.clear();
		superpairsKnownHighTierSlots.clear();
		superpairsClickedRevealSlots.clear();
		superpairsIgnoredPrioritySlots.clear();
		superpairsExhaustedPairKeys.clear();
		activeSuperpairsPair = null;
		activeSuperpairsPairClicksRemaining = 0;
		activeSuperpairsClickFirstNext = true;
		lastSuperpairsClickTick = -configuredSuperpairsClickDelayTicks();
	}

	private static String formatVec(Vec3 vec) {
		if (vec == null) {
			return "?";
		}
		return String.format(java.util.Locale.ROOT, "%.1f %.1f %.1f", vec.x, vec.y, vec.z);
	}

	private static void sendMessage(String text) {
		FeatureChat.sendPrefixed(FEATURE_NAME, text);
	}

	private enum MacroStep {
		IDLE,
		FIND_TABLE,
		ROTATE_TO_TABLE,
		OPEN_TABLE,
		CHECK_INITIAL_RENEW_EXPERIMENTS,
		CLICK_CHRONOMATRON,
		SELECT_CHRONOMATRON,
		WAIT_CHRONOMATRON_REWARD,
		WAIT_MAIN_AFTER_CHRONOMATRON,
		CLICK_ULTRASEQUENCER,
		SELECT_ULTRASEQUENCER,
		WAIT_ULTRASEQUENCER_REWARD,
		WAIT_MAIN_AFTER_ULTRASEQUENCER,
		CLICK_SUPERPAIRS,
		SELECT_SUPERPAIRS,
		ROTATE_FOR_BOTTLES,
		SWAP_BOTTLES,
		THROW_BOTTLES,
		WAIT_AFTER_BOTTLES,
		ROTATE_BACK_TO_TABLE,
		REOPEN_TABLE,
		ROTATE_REOPEN_TABLE,
		REOPEN_TABLE_DYNAMIC,
		CLICK_SUPERPAIRS_AFTER_BOTTLES,
		SELECT_SUPERPAIRS_AFTER_BOTTLES,
		SOLVE_SUPERPAIRS,
		WAIT_AFTER_SUPERPAIRS_CLAIM,
		CHECK_RENEW_EXPERIMENTS,
		WAIT_AFTER_RENEW_EXPERIMENTS
	}

	private enum ExperimentTier {
		METAPHYSICAL("Metaphysical Experiment", 350),
		TRANSCENDENT("Transcendent Experiment", 200),
		SUPREME("Supreme Experiment", 100),
		GRAND("Grand Experiment", 75),
		HIGH("High Experiment", 50),
		BEGINNER("Beginner Experiment", 25);

		private final String displayName;
		private final int requiredLevel;

		ExperimentTier(String displayName, int requiredLevel) {
			this.displayName = displayName;
			this.requiredLevel = requiredLevel;
		}

		private String displayName() {
			return displayName;
		}

		private int requiredLevel() {
			return requiredLevel;
		}
	}

	private enum BottleType {
		GRAND("Grand Experience Bottle", GRAND_BOTTLE_ID, 1_500),
		TITANIC("Titanic Experience Bottle", TITANIC_BOTTLE_ID, 250_000);

		private final String displayName;
		private final String skyblockId;
		private final int baseXp;

		BottleType(String displayName, String skyblockId, int baseXp) {
			this.displayName = displayName;
			this.skyblockId = skyblockId;
			this.baseXp = baseXp;
		}

		private String displayName() {
			return displayName;
		}

		private String skyblockId() {
			return skyblockId;
		}

		private int baseXp() {
			return baseXp;
		}
	}

	private record StakeOption(ExperimentTier tier, int slot, int enchantingLevel) {
	}

	private record SuperpairsPair(String reward, int firstSlot, int secondSlot, int xpValue) {
	}

	private record TierOption(ExperimentTier tier, int slot) {
	}

	private record BottlePlan(BottleType type, int slot, int count) {
	}
}
