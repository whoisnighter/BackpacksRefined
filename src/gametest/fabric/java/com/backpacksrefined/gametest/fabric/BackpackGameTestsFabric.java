package com.backpacksrefined.gametest.fabric;

import com.backpacksrefined.gametest.BackpackGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Registers {@link BackpackGameTests} with Fabric's game test API, one method a test, each named after
 * the method it calls. Fabric finds this class through the {@code fabric-gametest} entrypoint.
 */
public class BackpackGameTestsFabric {
    private final BackpackGameTests tests = new BackpackGameTests();

    @GameTest
    public void floatsOnStillWater(GameTestHelper helper) {
        tests.floatsOnStillWater(helper);
    }

    @GameTest
    public void standsInWater(GameTestHelper helper) {
        tests.standsInWater(helper);
    }

    @GameTest
    public void placedBackpackKeepsItsItems(GameTestHelper helper) {
        tests.placedBackpackKeepsItsItems(helper);
    }

    @GameTest
    public void brokenBackpackKeepsItsItems(GameTestHelper helper) {
        tests.brokenBackpackKeepsItsItems(helper);
    }

    @GameTest
    public void tiersHaveTheirOwnSize(GameTestHelper helper) {
        tests.tiersHaveTheirOwnSize(helper);
    }

    @GameTest
    public void upgradeKeepsTheItems(GameTestHelper helper) {
        tests.upgradeKeepsTheItems(helper);
    }

    @GameTest
    public void refusesBackpacksAndShulkerBoxes(GameTestHelper helper) {
        tests.refusesBackpacksAndShulkerBoxes(helper);
    }

    @GameTest
    public void storageSavesAndLoads(GameTestHelper helper) {
        tests.storageSavesAndLoads(helper);
    }

    @GameTest
    public void panelStoresAndTakes(GameTestHelper helper) {
        tests.panelStoresAndTakes(helper);
    }
}
