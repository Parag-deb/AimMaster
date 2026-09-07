package com.aimmaster.model;

/**
 * The three kinds of targets the player can shoot.
 *
 * In the game, each type moves differently:
 * - BIRD flies horizontally across the screen.
 * - BALLOON drifts slowly upward.
 * - FRUIT drops downward with gravity.
 */
public enum TargetType {
    BIRD,
    BALLOON,
    FRUIT
}
