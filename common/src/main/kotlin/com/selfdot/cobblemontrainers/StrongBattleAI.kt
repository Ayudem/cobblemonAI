/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.selfdot.cobblemontrainers

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.battles.interpreter.BattleContext
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI
import com.cobblemon.mod.common.api.moves.Move
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.moves.categories.DamageCategories
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.pokemon.status.Statuses
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.battles.*
import com.cobblemon.mod.common.battles.interpreter.ContextManager
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*
import javax.swing.text.Element
import kotlin.random.Random

/**
 * AI that tries to choose the best move for the given situations. Based off of the Pokemon Trainer Tournament Simulator Github
 * https://github.com/cRz-Shadows/Pokemon_Trainer_Tournament_Simulator/blob/main/pokemon-showdown/sim/examples/Simulation-test-1.ts#L330
 *
 * @since December 15th 2023
 */
// Define the type for the damage multipliers
typealias TypeEffectivenessMap = Map<String, Map<String, Double>>

fun getDamageMultiplier(attackerType: ElementalType, defenderType: ElementalType, defenderAbility: String?): Double {
    val effectiveness = typeEffectiveness[attackerType]?.get(defenderType) ?: 1.0
    if (defenderAbility != null) {
        when (defenderAbility) {
            "wonderguard" -> if (effectiveness <= 1.0) return 0.0
        }
    }
    return effectiveness
}

val typeEffectiveness: Map<ElementalType, Map<ElementalType, Double>> = mapOf(
    ElementalTypes.NORMAL to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 0.5, ElementalTypes.GHOST to 0.0, ElementalTypes.DRAGON to 1.0,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
    ),
    ElementalTypes.FIRE to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 0.5, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 2.0,
        ElementalTypes.ICE to 2.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 2.0, ElementalTypes.ROCK to 0.5, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 0.5,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 2.0, ElementalTypes.FAIRY to 1.0
    ),
    ElementalTypes.WATER to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 2.0, ElementalTypes.WATER to 0.5, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 0.5,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 2.0, ElementalTypes.FLYING to 1.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 2.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 0.5,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 1.0, ElementalTypes.FAIRY to 1.0
    ),
    ElementalTypes.ELECTRIC to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 2.0, ElementalTypes.ELECTRIC to 0.5, ElementalTypes.GRASS to 0.5,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 0.0, ElementalTypes.FLYING to 2.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 0.5,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 1.0, ElementalTypes.FAIRY to 1.0
    ),
    ElementalTypes.GRASS to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 2.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 0.5,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 0.5, ElementalTypes.GROUND to 2.0, ElementalTypes.FLYING to 0.5,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 0.5, ElementalTypes.ROCK to 2.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 0.5,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
    ),
    ElementalTypes.ICE to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 0.5, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 2.0,
        ElementalTypes.ICE to 0.5, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 2.0, ElementalTypes.FLYING to 2.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 2.0,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
    ),
    ElementalTypes.FIGHTING to mapOf(
        ElementalTypes.NORMAL to 2.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
        ElementalTypes.ICE to 2.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 0.5, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 0.5,
        ElementalTypes.PSYCHIC to 0.5, ElementalTypes.BUG to 0.5, ElementalTypes.ROCK to 2.0, ElementalTypes.GHOST to 0.0, ElementalTypes.DRAGON to 1.0,
        ElementalTypes.DARK to 2.0, ElementalTypes.STEEL to 2.0, ElementalTypes.FAIRY to 0.5
    ),
    ElementalTypes.POISON to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 2.0,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 0.5, ElementalTypes.GROUND to 0.5, ElementalTypes.FLYING to 1.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 0.5, ElementalTypes.GHOST to 0.5, ElementalTypes.DRAGON to 1.0,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.0, ElementalTypes.FAIRY to 2.0
    ),
    ElementalTypes.GROUND to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 2.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 2.0, ElementalTypes.GRASS to 0.5,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 2.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 0.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 0.5, ElementalTypes.ROCK to 2.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 1.0,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 2.0, ElementalTypes.FAIRY to 1.0
    ),
    ElementalTypes.FLYING to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 0.5, ElementalTypes.GRASS to 2.0,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 2.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 2.0, ElementalTypes.ROCK to 0.5, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 1.0,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
    ),
    ElementalTypes.PSYCHIC to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 2.0, ElementalTypes.POISON to 2.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
        ElementalTypes.PSYCHIC to 0.5, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 1.0,
        ElementalTypes.DARK to 0.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
    ),
    ElementalTypes.BUG to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 2.0,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 0.5, ElementalTypes.POISON to 0.5, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 0.5,
        ElementalTypes.PSYCHIC to 2.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 0.5, ElementalTypes.DRAGON to 1.0,
        ElementalTypes.DARK to 2.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 0.5
    ),
    ElementalTypes.ROCK to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 2.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
        ElementalTypes.ICE to 2.0, ElementalTypes.FIGHTING to 0.5, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 0.5, ElementalTypes.FLYING to 2.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 2.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 1.0,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
    ),
    ElementalTypes.GHOST to mapOf(
        ElementalTypes.NORMAL to 0.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
        ElementalTypes.PSYCHIC to 2.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 2.0, ElementalTypes.DRAGON to 1.0,
        ElementalTypes.DARK to 0.5, ElementalTypes.STEEL to 1.0, ElementalTypes.FAIRY to 1.0
    ),
    ElementalTypes.DRAGON to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 2.0,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 0.0
    ),
    ElementalTypes.DARK to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 0.5, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
        ElementalTypes.PSYCHIC to 2.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 2.0, ElementalTypes.DRAGON to 1.0,
        ElementalTypes.DARK to 0.5, ElementalTypes.STEEL to 1.0, ElementalTypes.FAIRY to 0.5
    ),
    ElementalTypes.STEEL to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 0.5, ElementalTypes.ELECTRIC to 0.5, ElementalTypes.GRASS to 1.0,
        ElementalTypes.ICE to 2.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 2.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 1.0,
        ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 2.0
    ),
    ElementalTypes.FAIRY to mapOf(
        ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
        ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 2.0, ElementalTypes.POISON to 0.5, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
        ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 2.0,
        ElementalTypes.DARK to 2.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
    )
)

val statusMoves: Map<MoveTemplate?, String> = mapOf(
    Moves.getByName("willowisp") to Statuses.BURN.showdownName,
    Moves.getByName("glare") to Statuses.PARALYSIS.showdownName,
    Moves.getByName("nuzzle") to Statuses.PARALYSIS.showdownName,
    Moves.getByName("stunspore") to Statuses.PARALYSIS.showdownName,
    Moves.getByName("thunderwave") to Statuses.PARALYSIS.showdownName,
    Moves.getByName("darkvoid") to Statuses.SLEEP.showdownName,
    Moves.getByName("hypnosis") to Statuses.SLEEP.showdownName,
    Moves.getByName("lovelykiss") to Statuses.SLEEP.showdownName,
    Moves.getByName("relicsong") to Statuses.SLEEP.showdownName,
    Moves.getByName("sing") to Statuses.SLEEP.showdownName,
    Moves.getByName("sleeppower") to Statuses.SLEEP.showdownName,
    Moves.getByName("spore") to Statuses.SLEEP.showdownName,
    Moves.getByName("yawn") to Statuses.SLEEP.showdownName,
    Moves.getByName("poisongas") to Statuses.POISON.showdownName,
    Moves.getByName("poisonpowder") to Statuses.POISON.showdownName,
    Moves.getByName("toxic") to Statuses.POISON_BADLY.showdownName,
    Moves.getByName("toxicthread") to Statuses.POISON.showdownName,
)

val volatileStatusMoves: Map<MoveTemplate?, String> = mapOf(
    Moves.getByName("chatter") to "confusion",
    Moves.getByName("confuseray") to "confusion",
    Moves.getByName("dynamicpunch") to "confusion",
    Moves.getByName("flatter") to "confusion",
    Moves.getByName("supersonic") to "confusion",
    Moves.getByName("swagger") to "confusion",
    Moves.getByName("sweetkiss") to "confusion",
    Moves.getByName("teeterdance") to "confusion",
    Moves.getByName("curse") to "cursed",
    Moves.getByName("leechseed") to "leech"
)

val boostFromMoves: Map<String, Map<Stat, Int>> = mapOf(
    "bellydrum" to mapOf(Stats.ATTACK to 6),
    "bulkup" to mapOf(Stats.ATTACK to 1, Stats.DEFENCE to 1),
    "clangoroussoul" to mapOf(Stats.ATTACK to 1, Stats.DEFENCE to 1, Stats.SPECIAL_ATTACK to 1, Stats.SPECIAL_DEFENCE to 1, Stats.SPEED to 1),
    "coil" to mapOf(Stats.ATTACK to 1, Stats.DEFENCE to 1, Stats.ACCURACY to 1),
    "dragondance" to mapOf(Stats.ATTACK to 1, Stats.SPEED to 1),
    "extremeevoboost" to mapOf(Stats.ATTACK to 2, Stats.DEFENCE to 2, Stats.SPECIAL_ATTACK to 2, Stats.SPECIAL_DEFENCE to 2, Stats.SPEED to 2),
    "clangoroussoulblaze" to mapOf(Stats.ATTACK to 1, Stats.DEFENCE to 1, Stats.SPECIAL_ATTACK to 1, Stats.SPECIAL_DEFENCE to 1, Stats.SPEED to 1),
    "filletaway" to mapOf(Stats.ATTACK to 2, Stats.SPECIAL_ATTACK to 2, Stats.SPEED to 2),
    "honeclaws" to mapOf(Stats.ATTACK to 1, Stats.ACCURACY to 1),
    "noretreat" to mapOf(Stats.ATTACK to 1, Stats.DEFENCE to 1, Stats.SPECIAL_ATTACK to 1, Stats.SPECIAL_DEFENCE to 1, Stats.SPEED to 1),
    "shellsmash" to mapOf(Stats.ATTACK to 2, Stats.DEFENCE to -1, Stats.SPECIAL_ATTACK to 2, Stats.SPECIAL_DEFENCE to -1, Stats.SPEED to 2),
    "shiftgear" to mapOf(Stats.ATTACK to 1, Stats.SPEED to 2),
    "swordsdance" to mapOf(Stats.ATTACK to 2),
    "tidyup" to mapOf(Stats.ATTACK to 1, Stats.SPEED to 1),
    "victorydance" to mapOf(Stats.ATTACK to 1, Stats.DEFENCE to 1, Stats.SPEED to 1),
    "acidarmor" to mapOf(Stats.DEFENCE to 2),
    "barrier" to mapOf(Stats.DEFENCE to 2),
    "cottonguard" to mapOf(Stats.DEFENCE to 3),
    "defensecurl" to mapOf(Stats.DEFENCE to 1),
    "irondefense" to mapOf(Stats.DEFENCE to 2),
    "shelter" to mapOf(Stats.DEFENCE to 2, Stats.EVASION to 1),
    "stockpile" to mapOf(Stats.DEFENCE to 1, Stats.SPECIAL_DEFENCE to 1),
    "stuffcheeks" to mapOf(Stats.DEFENCE to 2),
    "amnesia" to mapOf(Stats.SPECIAL_DEFENCE to 2),
    "calmmind" to mapOf(Stats.SPECIAL_ATTACK to 1, Stats.SPECIAL_DEFENCE to 1),
    "geomancy" to mapOf(Stats.SPECIAL_ATTACK to 2, Stats.SPECIAL_DEFENCE to 2, Stats.SPEED to 2),
    "nastyplot" to mapOf(Stats.SPECIAL_ATTACK to 2),
    "quiverdance" to mapOf(Stats.SPECIAL_ATTACK to 1, Stats.SPECIAL_DEFENCE to 1, Stats.SPEED to 1),
    "tailglow" to mapOf(Stats.SPECIAL_ATTACK to 3),
    "takeheart" to mapOf(Stats.SPECIAL_ATTACK to 1, Stats.SPECIAL_DEFENCE to 1),
    "agility" to mapOf(Stats.SPEED to 2),
    "autotomize" to mapOf(Stats.SPEED to 2),
    "rockpolish" to mapOf(Stats.SPEED to 2),
    "curse" to mapOf(Stats.ATTACK to 1, Stats.DEFENCE to 1, Stats.SPEED to -1),
    "minimize" to mapOf(Stats.EVASION to 2)
) //

class StrongBattleAI(skill: Int) : BattleAI {

    private val offensiveSwitchMoves = listOf("uturn", "voltswitch", "flipturn")
    private val switchMoves = listOf("uturn", "voltswitch", "flipturn", "batonpass", "partingshot", "teleport", "shedtail", "chillyreception")
    private val flinch30Moves = listOf("airslash", "astonish", "bite", "doubleironbash", "headbutt", "heartstamp", "iciclecrash", "ironhead", "rockslide", "rollingkick", "skyattack", "stomp", "zingzap", "steamroller", "snore")
    private val flinch20Moves = listOf("darkpulse", "dragonrush", "waterfall")
    private val flinch10Moves = listOf("boneclub", "extrasensory", "firefang", "hyperfang", "icefang", "thunderfang")
    private val offensiveUtilityMoves = listOf("direclaw", "knockoff", "nuzzle", "dragontail", "stoneaxe", "ceaselessedge", "fakeout")
    private val contactMoves = listOf("tackle","scratch","pound","slash","cut","furyswipes","rapidspin","megapunch","firepunch","thunderpunch","icepunch","dizzypunch","machpunch","cometpunch","dynamicpunch","closecombat","crosschop","doublekick","highjumpkick","jumpkick","lowkick","rollingkick","triplekick","bite","crunch","hyperfang","superfang","headbutt","hornattack","furyattack","horndrill","drillpeck","peck","pluck","wingattack","fly","dive","dig","bodyslam","doubleedge","takedown","flareblitz","bravebird","volttackle","wildcharge","headcharge","woodhammer","headsmash","aquatail","irontail","dragontail","shadowpunch","shadowclaw","nightslash","psychocut","xscissor","uturn","dragonclaw","dragonrush","outrage","falseswipe","leafblade","sacredsword","secretsword","meteormash","bulletpunch","drainpunch","hammerarm","poweruppunch","skyuppercut","suckerpunch","throatchop","darkestlariat","wickedblow","axekick","ragingbull","aquastep","accelerock","armthrust","astralbarrage","attackorder","barbbarrage","behemothblade","bittermalice","bleakwindstorm","ceaselessedge","collisioncourse","combattorque","crushgrip","darkpulse","direclaw","dragonenergy","dragonascent","dragonclaw","dragonrush","esperwing","filletaway","flamecharge","flipturn","freezeshock","gigatonhammer","glaiverush","grassyglide","gravapple","heartstamp","icehammer","icespinner","iceshard","iciclecrash","iciclespear","infernalparade","ivycudgel","jetpunch","kowtowcleave","lastrespects","liquidation","lunge","magicaltorque","mightycleave","mortalspin","mountaingale","muddywater","nuzzle","obstruct","orderup","populationbomb","pounce","psychofangs","psyshieldbash","ragingfury","razorshell","rockblast","ruination","saltcure","sandsearstorm","savagevoltage","seamitarsu","shadowforce","shedtail","shellsidearm","shelter","skittersmack","slam","smackdown","solarblade","spicyextract","spiritbreak","springtidestorm","stoneaxe","stoneedge","stormrush","supercellslam","surgingstrikes","swordsdance","takeheart","terablast","thunderouskick","torchsong","torment","triplearrows","tripledive","tropkick","upperhand","victorydance","wavecrash","wickedtorque","wildboltstorm","zenheadbutt") // omg there are so much, thanks deepseek for this line
    private val selfDamage1to4Moves = listOf("doubleedge","takedown","submission","wildcharge","headcharge")
    private val selfDamage1to3Moves = listOf("flareblitz","bravebird","volttackel","woodhammer","wavecrash")
    private val selfDamage1to2Moves = listOf("headsmash","lightofruin")
    private val babyUnboostMove = listOf("growl", "tailwhip", "leer", "withdraw")
    private val lifeStealMoves75 = listOf("drainingkiss", "oblivionwing")
    private val lifeStealMoves50 = listOf("absorb","megadrain","gigadrain","leechlife","drainpunch","hornleech","paraboliccharge","bitterblade","dreameater")
    private val opponentDependingMoves = listOf("counter", "mirrorcoat", "metalburst","suckerpunch","thunderclap")
    private val multiHitMovesStandard = listOf("armthrust" ,"barrage" ,"bonerush" ,"bulletseed" ,"cometpunch" ,"doubleslap" ,"furyattack" ,"furyswipes" ,"iciclespear" ,"pinmissile" ,"rockblast" ,"scaleshot" ,"spikecannon" ,"tailslap" , "watershuriken")
    private val multiHitMoves2Hits = listOf("bonemerang" ,"doublehit" ,"doubleironbash" ,"doublekick" ,"dragondarts" ,"dualchop" ,"dualwingbeat" ,"geargrind" ,"twinbeam" ,"twineedle")
    private val multiHitMoves3Hits = listOf("surgingstrikes","tripledive")
    private val bulletMoves = listOf("acidspray","aurasphere","beakblast","bulletseed","electroball","energyball","focusblast","gyroball","mistball","pollenpuff","pyroball","rockblast","rockwrecker","seedbomb","syrupbomb","shadowball","sludgebomb","weatherball","zapcannon")
    private val soundMoves = listOf("alluringvoice","boomburst","bugbuzz","chatter","clangingscales","clangoroussoul","clangoroussoulblaze","confide","disarmingvoice","echoedvoice","eeriespell","grasswhistle","growl","healbell","howl","hypervoice","metalsound","nobleroar","overdrive","partingshot","perishsong","psychicnoise","relicsong","roar","round","screech","shadowpanic","sing","snarl","snore","sparklingaria","supersonic","torchsong","uproar")
    private val punchMoves = listOf("bulletpunch","cometpunch","dizzypunch","doubleironbash","drainpunch","dynamicpunch","firepunch","focuspunch","hammerarm","headlongrush","icehammer","icepunch","jetpunch","machpunch","megapunch","meteormash","plasmafists","poweruppunch","ragefist","shadowpunch","skyuppercut","surgingstrikes","thunderpunch","wickedblow")
    private val alwaysCritMoves = listOf("flowertrick", "frostbreath", "stormthrow", "surgingstrike", "wickedblow")
    private val levelDamageMoves = listOf("seismictoss", "nightshade", "psywave")
    private val halfLifeDamageMoves = listOf("superfang", "naturesmadness", "ruination")
    private val screenMoves = listOf("protect", "lightscreen", "auroraveil")
    private val entryHazards = listOf("spikes", "stealthrock", "stickyweb", "toxicspikes", "stoneaxe", "ceaselessedge")
    private val nonOffensiveEntryHazards = listOf("spikes", "stealthrock", "stickyweb", "toxicspikes")
    private val antiHazardsMoves = listOf("rapidspin", "defog", "tidyup", "courtchange", "mortalspin")
    private val antiBoostMoves = listOf("slearsmog","haze", "whirlwind", "roar", "dragontail")
    private val lowPrioAntiBoostMoves = listOf("whirlwind", "roar", "dragontail")
    private val protectMoves = listOf("detect", "protect", "banefulbunker", "burningbulwark", "kingsshield", "obstruct", "silktrap", "spikyshield")
    private val pivotMoves = listOf("uturn","flipturn", "partingshot", "batonpass", "chillyreception","shedtail", "voltswitch", "teleport")
    private val offensivePivotMoves = listOf("uturn","flipturn", "voltswitch")
    private val setupMoves = setOf("tailwind", "trickroom", "auroraveil", "lightscreen", "reflect")
    private val selfRecoveryMoves = listOf("healorder", "milkdrink", "recover", "rest", "roost", "slackoff", "softboiled", "strengthsap")
    private val itemManipulationMoves = listOf("knockoff", "corrosivegas","covet", "embargo", "switcheroo", "trick", "thief")
    private val choiceItems = listOf("choicescarf", "choicespecs", "choiceband")
    private val weatherSetupMoves = mapOf(
        "chillyreception" to "Snow",
        "hail" to "Hail",
        "raindance" to "RainDance",
        "sandstorm" to "Sandstorm",
        "snowscape" to "Snow",
        "sunnyday" to "SunnyDay"
    )
    private val hapinessPower = 102.0 // supposed power of return and frustration
    private val turnAliveToBoost = 4 // chosen completely arbitrary. Number of turn the pokemon need to be able to survive to use a boost move without other thinking

    // used to keep informations about stuff in battle we can't get with activeBattlePokemon (or maybe I just don't know how)
    private val battleTracker = BattleTracker(
        mutableMapOf<UUID, PokemonTracker>(),
        null,
        null,
        DeathNumber(0,0), // the actual number of death, not just the number of dead pokemons
        PreviousMoves(null, null),
        null)

    // get all used information about a pokemon moveset
    private fun getActivePokemonMoveSet(pokemon: Pokemon?, request: ShowdownActionRequest?): List<ActivePokemonMove> {
        if (pokemon != null) {
            return pokemon.moveSet.mapIndexed { index, move ->
                val disabled = request?.active?.getOrNull(0)?.moves?.getOrNull(index)?.disabled ?: false
                ActivePokemonMove(
                    move.name,
                    move.power,
                    move.type,
                    move.damageCategory,
                    move.accuracy,
                    move.currentPp,
                    move.template.priority,
                    disabled
                )
            }
        } else return emptyList()
    }

    private fun getStatsBoosts(contextManager: ContextManager): PokemonStatBoosts {
        return PokemonStatBoosts(
            ((contextManager.get(BattleContext.Type.BOOST)?.count { it.id == "acc" }
                ?: 0) - (contextManager.get(BattleContext.Type.UNBOOST)?.count { it.id == "acc" }
                ?: 0)).toDouble(),
            ((contextManager.get(BattleContext.Type.BOOST)?.count { it.id == "atk" }
                ?: 0) - (contextManager.get(BattleContext.Type.UNBOOST)?.count { it.id == "atk" }
                ?: 0)).toDouble(),
            ((contextManager.get(BattleContext.Type.BOOST)?.count { it.id == "spa" }
                ?: 0) - (contextManager.get(BattleContext.Type.UNBOOST)?.count { it.id == "spa" }
                ?: 0)).toDouble(),
            ((contextManager.get(BattleContext.Type.BOOST)?.count { it.id == "def" }
                ?: 0) - (contextManager.get(BattleContext.Type.UNBOOST)?.count { it.id == "def" }
                ?: 0)).toDouble(),
            ((contextManager.get(BattleContext.Type.BOOST)?.count { it.id == "spd" }
                ?: 0) - (contextManager.get(BattleContext.Type.UNBOOST)?.count { it.id == "spd" }
                ?: 0)).toDouble(),
            ((contextManager.get(BattleContext.Type.BOOST)?.count { it.id == "spe" }
                ?: 0) - (contextManager.get(BattleContext.Type.UNBOOST)?.count { it.id == "spe" }
                ?: 0)).toDouble(),
        )
    }

    // get all used informations about an active pokemon
    private fun getActivePokemon(battlePokemon: BattlePokemon?, request: ShowdownActionRequest?): ActivePokemon {
        if (battlePokemon != null) {
            var item = battlePokemon.originalPokemon.heldItem().item.translationKey.split(".").last().trim()
            var ability = battlePokemon.originalPokemon.ability.name
            var trapped = false

            if (battleTracker.pokemons[battlePokemon.uuid] != null) {
                val tracker = battleTracker.pokemons[battlePokemon.uuid]!!
                if (tracker.item != null) item = tracker.item!!
                if (tracker.ability != null) ability = tracker.ability!!
                if (tracker.transform != null) return getTransformedPokemon(battlePokemon, request, tracker.transform!!)
            }
            if (request != null)
                request.active?.forEach { if (it.trapped) trapped = true }

            return ActivePokemon(
                battlePokemon.originalPokemon.species.name,
                battlePokemon.originalPokemon.species.weight,
                battlePokemon.originalPokemon.level,
                PokemonStats(
                    battlePokemon.originalPokemon.getStat(Stats.HP).toDouble(),
                    battlePokemon.originalPokemon.getStat(Stats.ATTACK).toDouble(),
                    battlePokemon.originalPokemon.getStat(Stats.SPECIAL_ATTACK).toDouble(),
                    battlePokemon.originalPokemon.getStat(Stats.DEFENCE).toDouble(),
                    battlePokemon.originalPokemon.getStat(Stats.SPECIAL_DEFENCE).toDouble(),
                    battlePokemon.originalPokemon.getStat(Stats.SPEED).toDouble(),
                ),
                getStatsBoosts(battlePokemon.contextManager),
                battlePokemon.originalPokemon.types,
                battlePokemon.originalPokemon.currentHealth.toDouble(),
                ability,
                item,
                battlePokemon.originalPokemon.status?.status?.showdownName,
                battlePokemon.contextManager.get(BattleContext.Type.VOLATILE)?.map { it.id } ?: emptyList(),
                getActivePokemonMoveSet(battlePokemon.originalPokemon, request),
                trapped,
                battlePokemon.uuid
            )
        } else return emptyPokemon()
    }

    // get all used informations about a pokemon when transformed has been used
    private fun getTransformedPokemon(battlePokemon: BattlePokemon?, request: ShowdownActionRequest?, transformation: Transform): ActivePokemon {

        // TODO : other stats changes after transformation won't be handled, need to get them in contextManager and make some additions
        if (battlePokemon != null) {
            var item = battlePokemon.originalPokemon.heldItem().item.translationKey.split(".").last().trim()
            var ability = transformation.pokemon.ability.name
            var trapped = false

            if (request != null)
                request.active?.forEach { if (it.trapped) trapped = true }

            return ActivePokemon(
                transformation.pokemon.species.name,
                transformation.pokemon.species.weight,
                battlePokemon.originalPokemon.level,
                PokemonStats(
                    battlePokemon.originalPokemon.getStat(Stats.HP).toDouble(),
                    transformation.pokemon.getStat(Stats.ATTACK).toDouble(),
                    transformation.pokemon.getStat(Stats.SPECIAL_ATTACK).toDouble(),
                    transformation.pokemon.getStat(Stats.DEFENCE).toDouble(),
                    transformation.pokemon.getStat(Stats.SPECIAL_DEFENCE).toDouble(),
                    transformation.pokemon.getStat(Stats.SPEED).toDouble(),
                ),
                transformation.statBoosts,
                transformation.pokemon.types,
                battlePokemon.originalPokemon.currentHealth.toDouble(), // not handled
                transformation.pokemon.ability.name,
                battlePokemon.originalPokemon.heldItem().item.translationKey.split(".").last().trim(),
                battlePokemon.originalPokemon.status?.status?.showdownName,
                battlePokemon.contextManager.get(BattleContext.Type.VOLATILE)?.map { it.id } ?: emptyList(),
                getActivePokemonMoveSet(transformation.pokemon, request),
                trapped,
                battlePokemon.uuid
            )
        } else return emptyPokemon()
    }

    // return an empty pokemon because it's really annoying to allow a player active pokemon to be null
    private fun emptyPokemon(): ActivePokemon {
        return ActivePokemon(
            "",
            0F,
            1,
            PokemonStats(0.0,0.0,0.0,0.0,0.0,0.0),
            PokemonStatBoosts(0.0,0.0,0.0,0.0,0.0,0.0),
            emptyList(),
            0.0,
            "",
            "",
            null,
            emptyList(),
            emptyList(),
            false,
            UUID.randomUUID()
        )
    }

    // get all used infomation about a non-active pokemon in a team
    private fun getAvailableTrainerTeam(pokemonList: MutableList<BattlePokemon>, activePokemon: BattlePokemon?):List<ActivePokemon> {

        val pokemonTeam = mutableListOf<ActivePokemon>()
        pokemonList.forEach { battlePokemon ->
            if (battlePokemon.uuid != activePokemon?.uuid && battlePokemon.originalPokemon.currentHealth > 0)
                pokemonTeam.add(getActivePokemon(battlePokemon, null))
        }
        return pokemonTeam
    }

    // get all relevant informations about the state of battle
    private fun getBattleInfos(activeBattlePokemon: ActiveBattlePokemon): BattleState {
        val battle = activeBattlePokemon.battle

        val p1Actor = battle.side1.actors.first()
        val p2Actor = battle.side2.actors.first()

        val activePlayerBattlePokemon = p1Actor.activePokemon[0].battlePokemon
        val activeNPCBattlePokemon = p2Actor.activePokemon[0].battlePokemon

        val playerPokemon = activePlayerBattlePokemon?.originalPokemon
        val npcPokemon = activeNPCBattlePokemon?.originalPokemon

        var playerTeam: List<ActivePokemon> = emptyList()
        var npcTeam: List<ActivePokemon> = emptyList()

        return BattleState(
            Field(
                battle.contextManager.get(BattleContext.Type.WEATHER)?.firstOrNull()?.id,
                battle.contextManager.get(BattleContext.Type.TERRAIN)?.firstOrNull()?.id,
                battle.contextManager.get(BattleContext.Type.ROOM)?.iterator()?.next()?.id != null
            ),
            Side( // player side
                SideOwner.PLAYER,
                battle.side1.contextManager.get(BattleContext.Type.HAZARD)?.map{it.id} ?: emptyList(),
                battle.side1.contextManager.get(BattleContext.Type.SCREEN)?.map{it.id} ?: emptyList(),
                battle.contextManager.get(BattleContext.Type.TAILWIND)?.iterator()?.next()?.id != null,
                getAvailableTrainerTeam(p1Actor.pokemonList, activePlayerBattlePokemon),
                getActivePokemon(activePlayerBattlePokemon, p1Actor.request)
            ),
            Side( // npc side
                SideOwner.NPC,
                battle.side2.contextManager.get(BattleContext.Type.HAZARD)?.map{it.id} ?: emptyList(),
                battle.side2.contextManager.get(BattleContext.Type.SCREEN)?.map{it.id} ?: emptyList(),
                battle.contextManager.get(BattleContext.Type.TAILWIND)?.iterator()?.next()?.id != null,
                getAvailableTrainerTeam(p2Actor.pokemonList, activeNPCBattlePokemon),
                getActivePokemon(activeNPCBattlePokemon, p2Actor.request)
            ),
        )
    }

    // simulate a 1v1 between the 2 active pokemons with their most probable offensive move and get informations (most importantly the winner)
    private fun get1v1Result(field:Field, playerSide:Side, npcSide:Side): Battle1v1State {
        // we continue the fight even if a pokemon die to estimate how many turns the other one could survive. we stop calculation after 10 turns max
        val maxTurns = 10

        // not real current hp, only during the simulation
        val npcCurrentHp = ActorCurrentHp(
            npcSide.pokemon.currentHp,
            if (npcSide.pokemon.volatileStatus.contains("substitute")) npcSide.pokemon.stats.hp/4 else 0.0,
            if (npcSide.pokemon.ability == "disguise" && (battleTracker.pokemons[npcSide.pokemon.uuid] == null || battleTracker.pokemons[npcSide.pokemon.uuid]!!.disguiseBroken == false )) 1.0 else 0.0,
            true
        )
        val playerCurrentHp = ActorCurrentHp(
            playerSide.pokemon.currentHp,
            if (playerSide.pokemon.volatileStatus.contains("substitute")) playerSide.pokemon.stats.hp/4 else 0.0,
            if (playerSide.pokemon.ability == "disguise" && (battleTracker.pokemons[playerSide.pokemon.uuid] == null || battleTracker.pokemons[playerSide.pokemon.uuid]!!.disguiseBroken == false )) 1.0 else 0.0,
            false
        )

        // substitute start 1v1 simulation with full life
        // TODO eventually : find the information about substitute currentLife and set substituteHp correctly

        val npcIsQuicker = selectedIsQuicker(field, npcSide, playerSide)

        lateinit var npcMoves: BattleMovesInfos
        lateinit var playerMoves: BattleMovesInfos

        lateinit var npcEndTurnDamageHeal: DamageHeal
        lateinit var playerEndTurnDamageHeal: DamageHeal

        lateinit var playerMostProbableMove: ActivePokemonMove
        lateinit var npcMovesInfo: BattleMovesInfos

        var firstIteration = true
        var weHaveAWinner = false

        var turnsToKillNpc = 0
        var turnsToKillPlayer = 0
        var npcAlive = true
        var playerAlive = true

        var npcWins = true

        // one of the pokemon die
        fun pokemonDead(isNpc:Boolean) {
            if(!weHaveAWinner) {
                weHaveAWinner = true
                npcWins = !isNpc
                //if (isNpc) println("player wins")
                //else println("npc wins")
            }
            if (isNpc) npcAlive = false
            else playerAlive = false
        }

        // if a substitute is killed with a multi-hit move, we suppose the move has trample (mtg ref) to get a better estimation of real damages
        // if a pokemon is full life and has sturdy or a focus sash, he is put to 1 life
        fun doMoveDamage(move: MoveValue, currentHp: ActorCurrentHp, attacker: ActivePokemon, defender: ActivePokemon) {
            val fullLifeBeforeDamage = (currentHp.pokemon == defender.stats.hp)

            if (currentHp.substitute > 0 && attacker.ability != "infiltrator") {
                currentHp.substitute -= move.value.damage
                if (currentHp.substitute < 0 && move.move.name in (multiHitMovesStandard + multiHitMoves2Hits + multiHitMoves3Hits))
                    currentHp.pokemon += currentHp.substitute
            } else if (currentHp.disguise > 0) {
                currentHp.disguise -= move.value.damage
                if (currentHp.disguise < 0) { // we can be more precise than with a substitute so we are
                    when (move.move.name) {
                        in multiHitMovesStandard -> currentHp.pokemon -= 4.0*move.value.damage/5.0 // approximation supposing attacker has skill link or dice
                        in multiHitMoves2Hits -> currentHp.pokemon -= move.value.damage/2.0
                        in multiHitMoves3Hits -> currentHp.pokemon -= 2.0*move.value.damage/3.0
                    }
                }
            }
            else currentHp.pokemon -= move.value.damage

            if (fullLifeBeforeDamage && currentHp.pokemon <= 0.0 && (defender.ability == "sturdy" || defender.item == "focussash") && move.move.name !in (multiHitMovesStandard + multiHitMoves2Hits + multiHitMoves3Hits))
                currentHp.pokemon = 1.0
        }

        fun doAttack(move: MoveValue, currentHp: ActorCurrentHp, attacker: ActivePokemon, defender: ActivePokemon) {
            doMoveDamage(move, currentHp, attacker, defender)
            if (currentHp.pokemon <= 0) pokemonDead(currentHp.isNpc)
            currentHp.pokemon += move.value.heal
        }

        // we make a first check here in case a pokemon switched and died during switch (making them survive exactly 0 turns)
        if (playerCurrentHp.pokemon <= 0) pokemonDead(false)
        if (npcCurrentHp.pokemon <= 0) pokemonDead(true)

        //println("BATTLE START")
        //println(npcSide.pokemon.name+" vs "+playerSide.pokemon.name)
        //println("--------------------------------------")
        // each iteration represent a turn
        while ((npcAlive || playerAlive) && turnsToKillNpc < maxTurns && turnsToKillPlayer < maxTurns) {
            // ONE MORE TURN ALIVE
            //println("TURN START")
            if (npcAlive) turnsToKillNpc += 1
            if (playerAlive) turnsToKillPlayer += 1

            // TAKING DECISION
            playerMoves = mostProbableOffensiveMove(field, playerSide, npcSide, playerCurrentHp.pokemon, npcCurrentHp.pokemon,!npcIsQuicker,false)
            npcMoves = mostProbableOffensiveMove(field, npcSide, playerSide, npcCurrentHp.pokemon, playerCurrentHp.pokemon,npcIsQuicker,false)
            //println("[ATTACK] player do "+playerMoves.usedMove.value.damage+" damages and "+playerMoves.usedMove.value.heal+" heal with move: "+playerMoves.usedMove.move.name)
            //println("[ATTACK] npc do "+npcMoves.usedMove.value.damage+" damages and "+npcMoves.usedMove.value.heal+" heal with move: "+npcMoves.usedMove.move.name)

            // Selecting the next most probable move selected by player for next Real turn
            if (firstIteration) {
                playerMostProbableMove = playerMoves.usedMove.move
                npcMovesInfo = npcMoves
                firstIteration = false
            }

            // LETS FIGHT !!
            if (selectedAttackFirst(field, npcSide, playerSide, npcMoves.usedMove.move, playerMoves.usedMove.move)) { // NPC ATTACKS FIRST
                doAttack(npcMoves.usedMove, playerCurrentHp, npcSide.pokemon, playerSide.pokemon) // NPC ATTACKS
                doAttack(playerMoves.usedMove, npcCurrentHp, playerSide.pokemon, npcSide.pokemon) // PLAYER ATTACKS
            } else { // PLAYER ATTACKS FIRST
                doAttack(playerMoves.usedMove, npcCurrentHp, playerSide.pokemon, npcSide.pokemon) // PLAYER ATTACKS
                doAttack(npcMoves.usedMove, playerCurrentHp, npcSide.pokemon, playerSide.pokemon) // NPC ATTACKS
            }

            // RESIDUAL DAMAGES
            npcEndTurnDamageHeal = damageAndHealEndTurn(field, npcSide.pokemon, playerSide.pokemon)
            playerEndTurnDamageHeal = damageAndHealEndTurn(field, playerSide.pokemon, npcSide.pokemon)
            npcCurrentHp.pokemon-=npcEndTurnDamageHeal.damage-npcEndTurnDamageHeal.heal
            playerCurrentHp.pokemon-=playerEndTurnDamageHeal.damage-playerEndTurnDamageHeal.heal
            //println("[RESIDUAL] player take "+playerEndTurnDamageHeal.damage+" damages and "+playerEndTurnDamageHeal.heal+" heal by residual effects")
            //println("[RESIDUAL] npc take "+npcEndTurnDamageHeal.damage+" damages and "+npcEndTurnDamageHeal.heal+" heal by residual effects")

            if (playerCurrentHp.pokemon <= 0) pokemonDead(false)
            if (npcCurrentHp.pokemon <= 0) pokemonDead(true)
        }

        //println("player will survive "+turnsToKillPlayer+" turns")
        //println("npc will survive "+turnsToKillNpc+" turns")

        return Battle1v1State (
            turnsToKillPlayer,
            turnsToKillNpc,
            npcWins,
            selectedIsQuicker(field, npcSide, playerSide),
            playerMostProbableMove,
            npcMovesInfo
        )
    }

    // simulate a 1v1 but starting with a switch of at least one pokemon
    private fun get1v1ResultWithSwitch(field:Field, playerSide:Side, npcSide:Side, otherNpcPokemon:ActivePokemon?, otherPlayerPokemon:ActivePokemon?, moveOnSwitch:ActivePokemonMove?): Battle1v1State {
        // TODO : handle when switched pokemon arrive on field with a substitute (with shed tail or baton pass)

        var newPlayerSide = playerSide
        var newNpcSide = npcSide
        lateinit var damageOnSwitch: DamageHeal

        if (otherPlayerPokemon != null) {
            newPlayerSide = Side(
                SideOwner.PLAYER,
                playerSide.hazards,
                playerSide.screen,
                playerSide.tailwind,
                playerSide.team,
                otherPlayerPokemon
            )
        }
        if (otherNpcPokemon != null) {
            newNpcSide = Side(
                SideOwner.NPC,
                npcSide.hazards,
                npcSide.screen,
                npcSide.tailwind,
                npcSide.team,
                otherNpcPokemon
            )
        }

        if (otherPlayerPokemon != null && moveOnSwitch != null) {
            damageOnSwitch = getDamageOnSwitch(field, newPlayerSide, newNpcSide, moveOnSwitch)
            newPlayerSide.pokemon.currentHp -= damageOnSwitch.damage
            newNpcSide.pokemon.currentHp += damageOnSwitch.heal
        }
        if (otherNpcPokemon != null && moveOnSwitch != null) {
            damageOnSwitch = getDamageOnSwitch(field, newNpcSide, newPlayerSide, moveOnSwitch)
            newNpcSide.pokemon.currentHp -= damageOnSwitch.damage
            newPlayerSide.pokemon.currentHp += damageOnSwitch.heal
        }

        // residual damages
        val npcEndTurnDamageHeal = damageAndHealEndTurn(field, npcSide.pokemon, playerSide.pokemon)
        val playerEndTurnDamageHeal = damageAndHealEndTurn(field, playerSide.pokemon, npcSide.pokemon)
        newNpcSide.pokemon.currentHp-=npcEndTurnDamageHeal.damage-npcEndTurnDamageHeal.heal
        newPlayerSide.pokemon.currentHp-=playerEndTurnDamageHeal.damage-playerEndTurnDamageHeal.heal

        return get1v1Result(field, newPlayerSide, newNpcSide)
    }

    // return the moveId probable move used by pokemon
    private fun mostProbableOffensiveMove(field: Field, attackerSide: Side, defenderSide: Side, attackerCurrentHp: Double, defenderCurrentHp: Double, attackerIsQuicker: Boolean, ignoreOpponentMove: Boolean): BattleMovesInfos {

        // The value of offensive move is represented by addition of damage and self-heal it provides
        // if a move can kill immediatly, it's selected
        val killerMoves = mutableListOf<ActivePokemonMove>()
        val damagingMoves = mutableListOf<ActivePokemonMove>()
        val notDamagingMoves = mutableListOf<ActivePokemonMove>()

        var opponentProbableMove: MoveValue? = null
        var highestDamageHeal = DamageHeal(
            0.0,
            0.0
        )
        lateinit var damageHeal: DamageHeal
        var bestMove = attackerSide.pokemon.moveSet.get(0)
        var effectiveHeal: Double // can't heal more than full hp
        var flinch = 0.0

        for (move in getEnabledMoves(attackerSide.pokemon.moveSet)) {
            if (!move.disabled) {
                // if we have a move with damage dependings on opponent move (like counter), we recalculate their most probable move here
                if (!ignoreOpponentMove && opponentProbableMove == null) {
                    opponentProbableMove = mostProbableOffensiveMove(
                        field,
                        defenderSide,
                        attackerSide,
                        defenderCurrentHp,
                        attackerCurrentHp,
                        !attackerIsQuicker,
                        true
                    ).usedMove
                    flinch = when (opponentProbableMove.move.name) {
                        in flinch30Moves -> 0.3
                        in flinch20Moves -> 0.2
                        in flinch10Moves -> 0.1
                        else -> 0.0
                    }
                }

                damageHeal = damageAndHealDoneByMove(
                    field,
                    attackerSide,
                    defenderSide,
                    move,
                    attackerIsQuicker,
                    flinch,
                    opponentProbableMove
                )

                // if we find a killing move with priority, we stop immediatly
                if ((attackerIsQuicker || move.priority > 0) && damageHeal.damage >= defenderCurrentHp) {
                    killerMoves.add(move)
                    highestDamageHeal = DamageHeal(damageHeal.damage, damageHeal.heal)
                    bestMove = move
                }

                if (damageHeal.heal > attackerSide.pokemon.stats.hp - attackerCurrentHp)
                    effectiveHeal = attackerSide.pokemon.stats.hp - attackerCurrentHp
                else
                    effectiveHeal = damageHeal.heal

                if (damageHeal.damage + effectiveHeal > 0.0) damagingMoves.add(move)
                else notDamagingMoves.add(move)

                if (damageHeal.damage + effectiveHeal >= highestDamageHeal.damage + highestDamageHeal.heal) {
                    if (killerMoves.isEmpty()) { // if no killer move found, highest damage is the best move
                        highestDamageHeal = DamageHeal(damageHeal.damage, effectiveHeal)
                        bestMove = move
                    }
                }
            }
        }
        return BattleMovesInfos(
            killerMoves,
            damagingMoves,
            notDamagingMoves,
            MoveValue(
                bestMove,
                highestDamageHeal
            )
        )
    }

    // looking for a relevant utility move before using an offensive move
    private fun usableUtilityMove(field: Field, attackerSide: Side, defenderSide: Side, attackerTurnsToLive: Int, defenderTurnsToLive: Int, attackerWins: Boolean, opponentMove: ActivePokemonMove, previousAttackerMove: ActivePokemonMove?, previousDefenderMove: ActivePokemonMove?): ActivePokemonMove? {
        // TODO check number of heavy duty boots and ungrounded (and alive pokemon) on teams to mitigate usage of hazard and anti-hazard
        // TODO check number of poison/steel/ungrounded opponents to mitigate usage of toxic spikes
        // TODO usage of boost moves could be smarter (probably)

        // TODO also : OMG organise this shit better, it's so unreadable !

        val attackerResidualDamage = damageAndHealEndTurn(field, attackerSide.pokemon, defenderSide.pokemon)
        var defenderResidualDamage = damageAndHealEndTurn(field, defenderSide.pokemon, attackerSide.pokemon)
        val expectedTurnsToPlay = expectedTurnsToPlay(attackerTurnsToLive, selectedIsQuicker(field, attackerSide, defenderSide))

        for (move in getEnabledMoves(attackerSide.pokemon.moveSet)) {
            if (move.damageCategory == DamageCategories.STATUS || move.name in offensiveUtilityMoves) {
                when (move.name) {
                    "fakeout" -> if (!pokemonHasType(defenderSide.pokemon, ElementalTypes.GHOST) && battleTracker.previousNpcPokemon != attackerSide.pokemon.uuid) return move
                    // we protect if opponent residual damagevalue is higher than our. we always protect if it's kingsshield
                    in protectMoves -> {
                        if (previousAttackerMove?.name !in protectMoves) {
                            if (move.name == "kingsshield") return move
                            if (previousAttackerMove?.name == "wish"
                                || (defenderResidualDamage.damage - defenderResidualDamage.heal) > (attackerResidualDamage.damage - attackerResidualDamage.heal))
                                return move
                        }
                    }
                    // if pokemon has anti-boost move, they use it when cumulative opponent boost is 2 or more
                    in antiBoostMoves -> {
                        if (defenderSide.pokemon.statBoosts.run { attack+specialAttack+defense+specialDefense+speed } >= 2.0) {
                            when (move.name) {
                                in lowPrioAntiBoostMoves -> {
                                    if (expectedTurnsToPlay >= 2) {
                                        when (move.name) {
                                            "dragontail" -> if (!pokemonHasType(defenderSide.pokemon, ElementalTypes.FAIRY)) return move
                                            else -> return move
                                        }
                                    }
                                }
                                else -> if (expectedTurnsToPlay >= 1) return move
                            }
                        }
                    }
                    // we wish if previous move is not wish
                    "wish" -> if (previousAttackerMove?.name != "wish")
                        if (expectedTurnsToPlay >= 1)
                            return move
                    // we provoc if opponent has 2 status moves or more
                    "taunt" -> if (expectedTurnsToPlay >= 2) {
                        if (!defenderSide.pokemon.volatileStatus.contains("taunt")
                            && canUseStatusMove(field, move, attackerSide.pokemon, defenderSide.pokemon)
                            && defenderSide.pokemon.moveSet.count { it.damageCategory == DamageCategories.STATUS} >= 2)
                            return move
                    }
                    // we encore if opponent has 1 status move or more, we use encore half of the time
                    // if we're quicker and opponent used a status move on previous turn, we use encore 90% of the time
                    "encore" -> if (expectedTurnsToPlay >= 2) {
                        if (defenderSide.pokemon.item !in choiceItems
                            && !defenderSide.pokemon.volatileStatus.contains("encore")
                            && canUseStatusMove(field, move, attackerSide.pokemon, defenderSide.pokemon)) {
                            if (defenderSide.pokemon.moveSet.count { it.damageCategory == DamageCategories.STATUS} >= 1 && xChanceOn100(50))
                                return move
                            if (selectedIsQuicker(field, attackerSide, defenderSide) && previousDefenderMove?.damageCategory == DamageCategories.STATUS && xChanceOn100(90))
                                return move
                        }
                    }
                    // if entry hazard of a type isn't on field, we put it
                    in entryHazards -> {
                        if (expectedTurnsToPlay >= 1) {
                            if (canUseStatusMove(field, move, attackerSide.pokemon, defenderSide.pokemon) && !pokemonHasMove(defenderSide.pokemon, antiHazardsMoves)) {
                                when (move.name) {
                                    "stealthrock" -> if (!defenderSide.hazards.contains("stealthrock") && !pokemonHasMove(defenderSide.pokemon, antiHazardsMoves)) return move
                                    "stoneaxe" -> if (!defenderSide.hazards.contains("stealthrock") && !pokemonHasMove(defenderSide.pokemon, antiHazardsMoves)) return move
                                    "spikes" -> if (defenderSide.hazards.count { it == "spikes" } < 3) return move
                                    "ceaselessedge" -> if (defenderSide.hazards.count { it == "spikes" } < 3) return move
                                    "stickyweb" -> if (!defenderSide.hazards.contains("stickyweb") && !pokemonHasMove(defenderSide.pokemon, antiHazardsMoves)) return move
                                    "toxicspikes" -> if (defenderSide.hazards.count { it == "toxicspikes" } < 2 && !pokemonHasMove(defenderSide.pokemon, antiHazardsMoves)) return move
                                }
                            }
                        }
                    }
                    // we setup screens if they're not setup
                    in screenMoves -> {
                        if (expectedTurnsToPlay >= 1) {
                            when (move.name) {
                                "auroraveil" -> if (!attackerSide.screen.contains("auroraveil") && field.weather == "snow") return move
                                "protect" -> if (!attackerSide.screen.contains("protect") && defenderSide.pokemon.stats.attack > defenderSide.pokemon.stats.specialAttack) return move
                                "lightscreen" -> if (!attackerSide.screen.contains("lightscreen") && defenderSide.pokemon.stats.specialAttack > defenderSide.pokemon.stats.attack) return move
                            }
                        }
                    }
                    // if hazards are on attacker side, he try to remove them
                    in antiHazardsMoves -> {
                        if (expectedTurnsToPlay >= 1) {
                            if (attackerSide.hazards.isNotEmpty()) {
                                if (!(move.name == "rapidspin" && pokemonHasType(defenderSide.pokemon, ElementalTypes.GHOST))
                                    && !(move.name == "courtchange" && (defenderSide.hazards.isNotEmpty() || attackerSide.screen.isNotEmpty()))) {
                                        return move
                                }
                            }
                        }
                    }
                    // we heal if it's our last move before dying and hp is not full
                    in selfRecoveryMoves -> {
                        if (expectedTurnsToPlay >= 1) {
                            when (move.name) {
                                "strengthsap" -> if (defenderSide.pokemon.stats.attack > defenderSide.pokemon.stats.specialAttack
                                    && (attackerSide.pokemon.currentHp/attackerSide.pokemon.stats.hp < 0.7 || defenderSide.pokemon.statBoosts.attack >= -1))
                                        return move
                                else -> if (lastMoveBeforeDying(field, attackerSide, defenderSide, attackerTurnsToLive, move, opponentMove)
                                    && attackerSide.pokemon.currentHp < attackerSide.pokemon.stats.hp)
                                        return move
                            }
                        }
                    }
                    // we substitute if opponent has status moves or we can sometimes try to substitute if we win fight (because opponent could switch)
                    "substitute" -> {
                        if (expectedTurnsToPlay >= 2) {
                            if (!attackerSide.pokemon.volatileStatus.contains("substitute")) {
                                if (attackerWins) {
                                    if (xChanceOn100(50)) return move
                                }
                                if (xChanceOn100(
                                        countMoveWithDamage(
                                            field,
                                            defenderSide,
                                            attackerSide,
                                            attackerSide.pokemon.stats.hp / 4,
                                            false
                                        ) * 25
                                    )
                                ) return move
                            }
                        }
                    }
                    // we healing wish if it's our last move before dying and hp is not full
                    in listOf("healingwish", "lunardance") -> {
                        if (lastMoveBeforeDying(field, attackerSide, defenderSide, attackerTurnsToLive, move, opponentMove)
                                && attackerSide.pokemon.currentHp < attackerSide.pokemon.stats.hp)
                                return move
                    }
                    // if opponent has an item, we try to remove/exchange it
                    // we suppose trick/switcheroo are used to give a scarf
                    in itemManipulationMoves -> {
                        if (expectedTurnsToPlay >= 2) {
                            if (canUseStatusMove(field, move, attackerSide.pokemon, defenderSide.pokemon)) {
                                if (defenderSide.pokemon.item != "") {
                                    when (move.name) {
                                        in listOf(
                                            "switcheroo",
                                            "trick"
                                        ) -> if (attackerSide.pokemon.item in choiceItems && defenderSide.pokemon.item !in choiceItems) return move

                                        else -> return move
                                    }
                                }
                            }
                        }
                    }
                    // we use a status move if opponent doesn't have status
                    in statusMoves.keys.mapNotNull { it?.name } -> {
                        if (expectedTurnsToPlay >= 2 && xChanceOn100(80)) {
                            if (defenderSide.pokemon.status == null && canUseStatusMove(field, move, attackerSide.pokemon, defenderSide.pokemon)) {
                                when (statusMoves[Moves.getByName(move.name)]) {
                                    Statuses.BURN.showdownName -> if (defenderSide.pokemon.ability !in listOf("waterveil","thermalexchange") && !pokemonHasType(defenderSide.pokemon, ElementalTypes.FIRE)) return move
                                    Statuses.PARALYSIS.showdownName -> {
                                        if (defenderSide.pokemon.ability != "limber" && !pokemonHasType(defenderSide.pokemon, ElementalTypes.ELECTRIC)) {
                                            when (move.name) {
                                                "stunpowder" -> if (!isPowderProtected(defenderSide.pokemon)) return move
                                                in listOf("thunderwave", "nuzzle") -> if (!pokemonHasType(defenderSide.pokemon, ElementalTypes.GROUND)) return move
                                                else -> return move
                                            }
                                        }
                                    }
                                    Statuses.SLEEP.showdownName -> {
                                        if (defenderSide.pokemon.ability !in listOf("insomnia", "vitalspirit", "sweetveil"))
                                        when (move.name) {
                                            in listOf("sleeppowder", "spore") -> if (!isPowderProtected(defenderSide.pokemon)) return move
                                            "yawn" -> if (previousAttackerMove?.name != "yawn") return move
                                            else -> return move
                                        }
                                    }
                                    in listOf(Statuses.POISON.showdownName, Statuses.POISON_BADLY.showdownName) -> {
                                        if (defenderSide.pokemon.ability !in listOf("immunity","pastelveil") && (attackerSide.pokemon.ability == "corrosion" || !(pokemonHasType(defenderSide.pokemon, ElementalTypes.POISON) || pokemonHasType(defenderSide.pokemon, ElementalTypes.STEEL)))) {
                                            when (move.name) {
                                                "poisonpowder" -> if (!isPowderProtected(defenderSide.pokemon)) return move
                                                else -> return move
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // we use a volatile status move if opponent doesn't already have the effect
                    in volatileStatusMoves.keys.mapNotNull { it?.name } -> {
                        if (expectedTurnsToPlay >= 2 && xChanceOn100(80)) {
                            if (canUseStatusMove(field, move, attackerSide.pokemon, defenderSide.pokemon)) {
                                when (statusMoves[Moves.getByName(move.name)]) {
                                    "confusion" -> if (!(defenderSide.pokemon.volatileStatus.contains("confusion"))) {
                                        when (move.name) {
                                            "dynamicpunch" -> if (!pokemonHasType(defenderSide.pokemon, ElementalTypes.GHOST)) return move
                                            else return move
                                        }
                                    }
                                    // curse has ghost is used brainlessly because I don't know how it could be used better ^_^
                                    "cursed" -> if (pokemonHasType(attackerSide.pokemon, ElementalTypes.GHOST) && !(defenderSide.pokemon.volatileStatus.contains("cursed"))) return move
                                    "leech" -> if (pokemonHasType(defenderSide.pokemon, ElementalTypes.GRASS) && !(defenderSide.pokemon.volatileStatus.contains("leech"))) return move
                                }
                            }
                        }
                    }
                    // we boost speed if we're slower, else we boost while we have 2 less boosts than the opponent in a given stat/counterstat (like attack vs defense)
                    // kind of simplistic for now, maybe I'll try to make it better if I have another idea
                    // TODO handle dodge and accuracy boosts
                    in boostFromMoves.keys -> {
                        if (expectedTurnsToPlay >= 2) {
                            if (canUseBoostMove(move, attackerSide.pokemon)) {
                                if (attackerTurnsToLive > 1) {
                                    if (boostCanGoHigher(attackerSide.pokemon, 6)) {
                                        if (attackerTurnsToLive >= turnAliveToBoost) {
                                            if (pokemonHasMove(defenderSide.pokemon, antiBoostMoves)) {
                                                if (xChanceOn100(50)) return move
                                            }
                                            return move
                                        }
                                        if (move.name in boostFromMoves.filter { (_, statMap) -> statMap.containsKey(Stats.SPEED) }) {
                                            if (!selectedIsQuicker(
                                                    field,
                                                    attackerSide,
                                                    defenderSide
                                                ) || attackerSide.pokemon.statBoosts.speed <= 0
                                            ) return move
                                        }
                                        if (move.name in boostFromMoves.filter { (_, statMap) -> statMap.containsKey(Stats.ATTACK) }) {
                                            if (attackerSide.pokemon.statBoosts.attack - defenderSide.pokemon.statBoosts.defense < 2) return move
                                        }
                                        if (move.name in boostFromMoves.filter { (_, statMap) -> statMap.containsKey(Stats.SPECIAL_ATTACK) }) {
                                            if (attackerSide.pokemon.statBoosts.specialAttack - defenderSide.pokemon.statBoosts.specialDefense < 2) return move
                                        }
                                        if (move.name in boostFromMoves.filter { (_, statMap) -> statMap.containsKey(Stats.DEFENCE) }) {
                                            if (attackerSide.pokemon.statBoosts.defense - defenderSide.pokemon.statBoosts.attack < 2) return move
                                        }
                                        if (move.name in boostFromMoves.filter { (_, statMap) -> statMap.containsKey(Stats.SPECIAL_DEFENCE) }) {
                                            if (attackerSide.pokemon.statBoosts.specialDefense - defenderSide.pokemon.statBoosts.specialAttack < 2) return move
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // just for the lvl 5 battle against rival
                    // TODO just done for fun, would be better to extend it to usage of any unboost move
                    in babyUnboostMove -> if (boostCanGoHigher(attackerSide.pokemon, 4)) {
                        if (expectedTurnsToPlay >= 2) {
                            if (attackerSide.pokemon.statBoosts.attack + attackerSide.pokemon.statBoosts.defense - defenderSide.pokemon.statBoosts.attack - defenderSide.pokemon.statBoosts.defense < 1) return move
                        }
                    }
                    // we destiny bond 80% of the time if we're gonna die
                    "destinybond" -> {
                        if (lastMoveBeforeDying(field, attackerSide, defenderSide, attackerTurnsToLive, move, opponentMove) && previousAttackerMove?.name != "destinybond") {
                            if(xChanceOn100(80))
                                return move
                        }
                    }
                }
            }
        }
        return null
    }

    private fun getDamageOnSwitch(field: Field, switcherSide: Side, attackerSide: Side?, takenMove: ActivePokemonMove?): DamageHeal{
        // TODO add residual damage
        var hazardDamage = 0.0
        var moveValue = DamageHeal(0.0,0.0)

        // spikes
        if (isGrounded(switcherSide.pokemon)) {
            when(switcherSide.hazards.count {it == "spikes"}) {
                1 -> hazardDamage += switcherSide.pokemon.stats.hp/8
                2 -> hazardDamage += switcherSide.pokemon.stats.hp/6
                3 -> hazardDamage += switcherSide.pokemon.stats.hp/4
            }
        }
        //stealth rocks
        if (switcherSide.hazards.contains("stealthrock")) {
            var stealthRockWeakness = 1.0
            for (type in switcherSide.pokemon.types) {
                stealthRockWeakness *= getDamageMultiplier(ElementalTypes.ROCK, type, null)
            }
            hazardDamage += switcherSide.pokemon.stats.hp / 8 * stealthRockWeakness
        }

        // if switched before opponent move, we take the damage
        if (attackerSide != null && takenMove != null) {
            moveValue = damageAndHealDoneByMove(field, attackerSide, switcherSide, takenMove,false, 0.0,null)
        }
        return DamageHeal(
            hazardDamage+moveValue.damage,
            moveValue.heal
        )
    }

    private fun damageAndHealDoneByMove(field: Field, attackerSide: Side, defenderSide: Side, move: ActivePokemonMove, attackerIsQuicker: Boolean, flinched: Double, opponentMove: MoveValue?): DamageHeal {

        /* ------- TODO-LIST ------------
        // maybe add more chance of crit as modifier
        // spit-up -> NOPE !
        // beat-up -> MEGA NOPE !!!
        // fling -> AHAHAHAHAHAHAHAHAHAHA nobody plays fling anyway

        // Tester un peu voir si on a des degats calculés à peu près corrects
         */
        val nodamage = DamageHeal(0.0,0.0)

        // calculated first to immediatly stop calculation if defender is immune
        move.type = adjustMoveType(field, move)
        val damageTypeMultiplier = getDamageTypeMultiplier(field, attackerSide, defenderSide, move)

        // cases for 0 damage
        when {
            attackerSide.pokemon.status == "slp" -> return nodamage // attacker asleep, we suppose they won't awake this turn
            move.damageCategory == DamageCategories.STATUS -> return nodamage // status move doesn't deal damages
            damageTypeMultiplier == 0.0 -> return nodamage // defender immune to move
        }

        // fixed damage
        val fixedDamage = getFixedDamage(attackerSide, defenderSide, move, opponentMove)
        if (fixedDamage != null) return fixedDamage

        // real stats with boost
        val attackerBoostedStats = getBoostedStats(attackerSide.pokemon, defenderSide.pokemon)
        val defenderBoostedStats = getBoostedStats(defenderSide.pokemon, attackerSide.pokemon)

        val critical = if (move.name in alwaysCritMoves) 1.5 else 1.0 // crits added only for 100% crit moves
        val movePower = getMovePower(attackerSide,defenderSide,move,attackerBoostedStats,defenderBoostedStats) // move power
        val statRatio = getStatRatio(attackerSide,defenderSide,move,attackerBoostedStats,defenderBoostedStats, critical > 1.0) // ratio attack/defense

        val burn = if (attackerSide.pokemon.status == "burn" && move.damageCategory == DamageCategories.PHYSICAL && move.name != "facade" && attackerSide.pokemon.ability != "guts") 0.5 else 1.0 // burn attack reduction
        val paralysis = if (attackerSide.pokemon.status == "par") 0.75 else 1.0 // if the attacker is paralyzed, they won't attack 1/4 of the time so we reduce the expected damage accordingly
        val confusion = if (attackerSide.pokemon.volatileStatus.contains("confusion")) 0.666 else 1.0 // if the attacker has confusion, they attack themselves 1/3 of the time so we reduce the expected damage accordingly

        var damage = (((((2 * attackerSide.pokemon.level) / 5 ) + 2) * movePower * statRatio) / 50 + 2)
        damage *= getWeatherMultiplier(field, attackerSide, defenderSide, move) // WEATHER
        damage *= getTerrainMultiplier(field, attackerSide, defenderSide, move) // TERRAIN
        damage *= getScreenMultiplier(attackerSide, defenderSide, move) // SCREENS
        damage *= getStabMultiplier(attackerSide.pokemon, move) // STAB
        damage *= getItemDamageMultiplier(field, attackerSide, defenderSide, move, damageTypeMultiplier) // ITEMS
        damage *= getMultiHitMultiplier(attackerSide, move) // MULTI-HIT MOVES
        damage *= getAccuracyMultiplier(field, attackerSide, defenderSide, move) // ACCURACY
        damage *= getOtherDamageMultiplier(field, attackerSide, defenderSide, move, opponentMove?.move) // Other multipliers
        damage *= getFlinchMultiplier(attackerSide, defenderSide, flinched, !attackerIsQuicker); // Flinch
        damage *= damageTypeMultiplier // TYPE
        damage *= burn // attack reduced by 0.5
        damage *= critical
        damage *= paralysis // user attacks only 3/4 times
        damage *= confusion // user attacks only 2/4 times

        var heal = getHealFromMove(attackerSide, move, damage)
        heal -= getSelfDamageFromMove(attackerSide, defenderSide, move, damage)
        return DamageHeal(damage,heal)
    }

    private fun damageAndHealEndTurn(field: Field, pokemon: ActivePokemon, opponent: ActivePokemon): DamageHeal {
        // TODO toxic damages
        var damage = 0.0
        var heal = 0.0

        var bindDamage = 1/8

        when (pokemon.status) {
            "brn" -> damage+=pokemon.stats.hp/16
            "psn" -> {
                if (pokemon.ability == "poisonheal") heal+=pokemon.stats.hp/8
                else damage+=pokemon.stats.hp/8
            }
        }
        when (field.weather) {
            "raining" -> if (pokemon.ability == "dryskin") heal+=pokemon.stats.hp/8
            "sunny" -> {
                when (pokemon.ability) {
                    "dryskin" -> damage+=pokemon.stats.hp/8
                    "solarpower" -> damage+=pokemon.stats.hp/8
                }
            }
            "sandstorm" -> {
                if (!pokemonHasType(pokemon, ElementalTypes.STEEL) && !pokemonHasType(pokemon, ElementalTypes.GROUND) && !pokemonHasType(pokemon, ElementalTypes.ROCK)) damage+=pokemon.stats.hp/16
            }
        }
        when (pokemon.item) {
            "leftovers" -> heal+=pokemon.stats.hp/16
            "blacksludge" -> {
                if (!pokemonHasType(pokemon, ElementalTypes.POISON))
                    heal+=pokemon.stats.hp/16
                else damage+=pokemon.stats.hp/16
            }
        }
        if (opponent.volatileStatus.contains("leech")) heal+=opponent.stats.hp/8
        if (pokemon.volatileStatus.contains("leech")) damage+=opponent.stats.hp/8
        if (pokemon.volatileStatus.contains("cursed")) damage+=opponent.stats.hp/4
        if (pokemon.volatileStatus.contains("aquaring")) heal+=opponent.stats.hp/16
        if (pokemon.volatileStatus.contains("ingrain")) heal+=opponent.stats.hp/16
        if (pokemon.volatileStatus.contains("clamp")) damage+=opponent.stats.hp/16

        if (opponent.item == "bindingband") bindDamage = 1/6
        if (pokemon.volatileStatus.contains("bind")) damage+=opponent.stats.hp*bindDamage
        if (pokemon.volatileStatus.contains("wrap")) damage+=opponent.stats.hp*bindDamage
        if (pokemon.volatileStatus.contains("firespin")) damage+=opponent.stats.hp*bindDamage
        if (pokemon.volatileStatus.contains("sandtomb")) damage+=opponent.stats.hp*bindDamage
        if (pokemon.volatileStatus.contains("whirlpool")) damage+=opponent.stats.hp*bindDamage
        if (pokemon.volatileStatus.contains("magmastorm")) damage+=opponent.stats.hp*bindDamage
        if (pokemon.volatileStatus.contains("infestation")) damage+=opponent.stats.hp*bindDamage

        return DamageHeal(damage, heal)
    }

    private fun getBoostedStats (pokemon: ActivePokemon, opponent: ActivePokemon): PokemonStats {
        var attackMultiplier = 1.0
        var defenseMultiplier = 1.0
        var specialAttackMultiplier = 1.0
        var specialDefenseMultiplier = 1.0

        when (opponent.ability) {
            "unaware" -> return pokemon.stats
            "tabletsofruin" -> if(pokemon.ability != "tabletsofruin") attackMultiplier *= 0.75
            "swordofruin" -> if(pokemon.ability != "swordofruin") defenseMultiplier *= 0.75
            "vesselofruin" -> if(pokemon.ability != "vesselofruin") specialAttackMultiplier *= 0.75
            "beadsofruin" -> if(pokemon.ability != "beadsofruin") specialDefenseMultiplier *= 0.75
        }
        return  PokemonStats (
            pokemon.stats.hp,
            pokemon.stats.attack*(1+pokemon.statBoosts.attack/2)*attackMultiplier,
            pokemon.stats.specialAttack*(1+pokemon.statBoosts.specialAttack/2)*specialAttackMultiplier,
            pokemon.stats.defense*(1+pokemon.statBoosts.defense/2)*defenseMultiplier,
            pokemon.stats.specialDefense*(1+pokemon.statBoosts.specialDefense/2)*specialDefenseMultiplier,
            pokemon.stats.speed*(1+pokemon.statBoosts.speed/2)
        )
    }

    private fun adjustMoveType(field: Field, move: ActivePokemonMove): ElementalType {
        val type = move.type
        when {
            move.name == "weatherball" -> {
                when {
                    field.weather == "sunny" -> move.type = ElementalTypes.FIRE
                    field.weather == "raining" -> move.type = ElementalTypes.WATER
                    field.weather == "snow" -> move.type = ElementalTypes.ICE
                    field.weather == "sandstorm" -> move.type = ElementalTypes.ROCK
                }
            }
            move.name == "terrainpulse" -> {
                when {
                    field.terrain == "electricterrain" -> move.type = ElementalTypes.ELECTRIC
                    field.terrain == "grassyterrain" -> move.type = ElementalTypes.GRASS
                    field.terrain == "mistyterrain" -> move.type = ElementalTypes.FAIRY
                    field.terrain == "psychicterrain" -> move.type = ElementalTypes.PSYCHIC
                }
            }
        }
        return type
    }

    private fun getFixedDamage(attackerSide: Side, defenderSide: Side, move: ActivePokemonMove, opponentFirstMove: MoveValue?): DamageHeal? {
        var damage = 0.0
        var heal = 0.0

        when (move.name){
            "dragonrage" -> damage = 40.0
            "sonicboom" -> damage = 20.0
            in levelDamageMoves -> damage = attackerSide.pokemon.level.toDouble()
            in halfLifeDamageMoves -> damage = defenderSide.pokemon.currentHp/2.0
            "painsplit" -> {
                damage = defenderSide.pokemon.currentHp - (defenderSide.pokemon.currentHp + attackerSide.pokemon.currentHp)/2.0
                heal = attackerSide.pokemon.currentHp - (defenderSide.pokemon.currentHp + attackerSide.pokemon.currentHp)/2.0
            }
            "endeavor" -> {
                if (defenderSide.pokemon.currentHp > attackerSide.pokemon.currentHp)
                    damage = (defenderSide.pokemon.currentHp - attackerSide.pokemon.currentHp).toDouble()
            }
            "counter" -> {
                if (opponentFirstMove != null && opponentFirstMove.move.damageCategory == DamageCategories.PHYSICAL)
                    damage = opponentFirstMove.value.damage*2.0
            }
            "mirrorcoat" -> {
                if (opponentFirstMove != null && opponentFirstMove.move.damageCategory == DamageCategories.SPECIAL)
                    damage = opponentFirstMove.value.damage*2.0
            }
            "metalburst" -> {
                if (opponentFirstMove != null)
                    damage = opponentFirstMove.value.damage*1.5
            }
            else -> return null
        }
        return DamageHeal(damage,heal)
    }

    private fun getDamageTypeMultiplier(field: Field, attackerSide: Side, defenderSide: Side, move: ActivePokemonMove): Double {
        // todo : wonderguard, soundproof, bulletproof
        var damageTypeMultiplier = 1.0 // calculated early to stop immediatly if defender is immune
        for (type in defenderSide.pokemon.types) {
            damageTypeMultiplier *= getDamageMultiplier(move.type, type, defenderSide.pokemon.ability)
        }
        when (move.type) {
            ElementalTypes.GROUND ->
                if (!isGrounded(defenderSide.pokemon)) damageTypeMultiplier *= 0
            ElementalTypes.WATER ->
                when {
                    defenderSide.pokemon.ability in listOf("dryskin", "waterabsorb") -> damageTypeMultiplier *= -0.25
                    defenderSide.pokemon.ability == "stormdrain" -> damageTypeMultiplier *= 0
                }
            ElementalTypes.ELECTRIC ->
                when {
                    defenderSide.pokemon.ability == "voltabsorb" -> damageTypeMultiplier *= -0.25
                    defenderSide.pokemon.ability in listOf("lightningrod", "motordrive") -> damageTypeMultiplier *= 0
                }
            ElementalTypes.GRASS -> if (defenderSide.pokemon.ability == "sapsipper") damageTypeMultiplier *= 0
            ElementalTypes.FIRE -> if (defenderSide.pokemon.ability in listOf("flashfire","thermalexchange")) damageTypeMultiplier *= 0
            ElementalTypes.GHOST -> if (defenderSide.pokemon.ability == "purifyingsalt") damageTypeMultiplier *= 0.5
        }
        when (attackerSide.pokemon.ability) {
            "tintedlens" -> if (damageTypeMultiplier > 0.0 && damageTypeMultiplier < 1.0) damageTypeMultiplier = 1.0
        }
        when (defenderSide.pokemon.ability) {
            in listOf("solidrock","filter", "prismarmor") -> if (damageTypeMultiplier > 1.0) damageTypeMultiplier *= 0.75
        }
        return damageTypeMultiplier
    }

    private fun getMovePower(attackerSide: Side, defenderSide: Side, move: ActivePokemonMove, attackerBoostedStats:PokemonStats, defenderBoostedStats:PokemonStats): Double {
        // move power
        val movePower:Double = when {
            move.name == "ragingfist" -> 50.0 + 50.0*getNumberOfHitTaken(attackerSide.pokemon)
            move.name == "lastrespects" -> 50.0 + 50.0*getDeadAlliesNumber(attackerSide)
            move.name in listOf("return", "frustration") -> hapinessPower
            move.name == "magnitude" -> 71.0
            move.name in listOf("eruption", "waterspout") -> 150*attackerSide.pokemon.currentHp/attackerSide.pokemon.stats.hp
            move.name in listOf("crushgrip", "wringout") -> 120*attackerSide.pokemon.currentHp/attackerSide.pokemon.stats.hp
            move.name == "hardpress" -> 100*attackerSide.pokemon.currentHp/attackerSide.pokemon.stats.hp
            move.name == "gyroball" -> 25*defenderBoostedStats.speed/attackerBoostedStats.speed
            move.name in listOf("powertrip", "storedpower") -> 20.0 + 20.0*with(attackerSide.pokemon.statBoosts) {attack+specialAttack+defense+specialDefense+speed}
            move.name == "punishment" -> 60.0 + 20.0*with(attackerSide.pokemon.statBoosts) {attack+specialAttack+defense+specialDefense+speed}
            move.name == "present" -> 54.0 - defenderSide.pokemon.stats.hp/4
            move.name in listOf("heavyslam","heatcrash") ->
                when {
                    attackerSide.pokemon.weight/defenderSide.pokemon.weight > 5 -> 120.0
                    attackerSide.pokemon.weight/defenderSide.pokemon.weight > 4 -> 100.0
                    attackerSide.pokemon.weight/defenderSide.pokemon.weight > 3 -> 80.0
                    attackerSide.pokemon.weight/defenderSide.pokemon.weight > 2 -> 60.0
                    else -> 40.0
                }
            move.name in listOf("grassknot","lowkick") ->
                when {
                    defenderSide.pokemon.weight < 10 -> 20.0
                    defenderSide.pokemon.weight < 25 -> 40.0
                    defenderSide.pokemon.weight < 50 -> 60.0
                    defenderSide.pokemon.weight < 100 -> 80.0
                    defenderSide.pokemon.weight < 200 -> 100.0
                    else -> 120.0
                }
            move.name == "electroball" ->
                when {
                    attackerBoostedStats.speed/defenderBoostedStats.speed > 4 -> 150.0
                    attackerBoostedStats.speed/defenderBoostedStats.speed > 3 -> 120.0
                    attackerBoostedStats.speed/defenderBoostedStats.speed > 2 -> 80.0
                    attackerBoostedStats.speed/defenderBoostedStats.speed > 1 -> 60.0
                    else -> 40.0
                }
            move.name == "flail" ->
                when {
                    attackerSide.pokemon.currentHp/attackerSide.pokemon.stats.hp > 0.688 -> 20.0
                    attackerSide.pokemon.currentHp/attackerSide.pokemon.stats.hp > 0.354 -> 40.0
                    attackerSide.pokemon.currentHp/attackerSide.pokemon.stats.hp > 0.208 -> 80.0
                    attackerSide.pokemon.currentHp/attackerSide.pokemon.stats.hp > 0.104 -> 100.0
                    attackerSide.pokemon.currentHp/attackerSide.pokemon.stats.hp > 0.042 -> 150.0
                    else -> 200.0
                }
            move.name == "trumpcard" ->
                when {
                    move.currentPP == 4 -> 50.0
                    move.currentPP == 3 -> 60.0
                    move.currentPP == 2 -> 80.0
                    move.currentPP == 1 -> 200.0
                    else -> 40.0
                }
            else -> move.power
        }
        return movePower
    }

    private fun getStatRatio (attackerSide: Side, defenderSide: Side, move: ActivePokemonMove, attackerBoostedStats:PokemonStats, defenderBoostedStats:PokemonStats, isCritical:Boolean): Double {
        // if crit, boosted defenses are ignored
        val defenderDef = when {
            isCritical -> defenderSide.pokemon.stats.defense
            else -> defenderBoostedStats.defense
        }
        val defenderSpd = when {
            isCritical -> defenderSide.pokemon.stats.specialDefense
            else -> defenderBoostedStats.specialDefense
        }

        return when {
            move.name in listOf("psyshock","psystrike","secretsword") -> attackerBoostedStats.specialAttack/defenderDef
            move.name == "bodypress" -> attackerBoostedStats.defense/defenderDef
            move.name == "foulplay" -> defenderBoostedStats.defense/defenderDef
            move.damageCategory == DamageCategories.PHYSICAL -> attackerBoostedStats.attack/defenderDef
            move.damageCategory == DamageCategories.SPECIAL -> attackerBoostedStats.specialAttack/defenderSpd
            else -> 1.0
        }
    }

    private fun getWeatherMultiplier (field: Field, attackerSide: Side, defenderSide: Side, move:ActivePokemonMove): Double {
        var weatherMultiplier = 1.0
        when {
            // Sunny Weather
            field.weather == "sunny" -> {
                when {
                    move.type == ElementalTypes.FIRE -> weatherMultiplier *= 1.5
                    move.name == "hydrosteam" -> weatherMultiplier *= 1.5
                    move.type == ElementalTypes.WATER && move.name != "hydrosteam" -> weatherMultiplier *= 0.5
                }
                when (attackerSide.pokemon.ability) {
                    "solarpower" -> if (move.damageCategory == DamageCategories.SPECIAL) weatherMultiplier *= 1.5
                    "orichalcumpulse" -> weatherMultiplier *= 1.3
                }
                if (move.name == "weatherball") weatherMultiplier *= 2
            }
            // Rainy Weather
            field.weather == "raining" -> {
                when {
                    move.type == ElementalTypes.WATER -> weatherMultiplier *= 1.5
                    move.type == ElementalTypes.FIRE -> weatherMultiplier *= 0.5
                }
                if (move.name == "weatherball")
                    weatherMultiplier *= 2
            }
            // Snowy Weather
            field.weather == "snow" -> {
                // ice +1.5 def in snow
                if (defenderSide.pokemon.types.contains(ElementalTypes.ICE) && move.damageCategory == DamageCategories.PHYSICAL)
                    weatherMultiplier *= 0.666

                if (attackerSide.pokemon.ability == "sandforce" && move.type in listOf(ElementalTypes.ROCK, ElementalTypes.GROUND, ElementalTypes.STEEL))
                    weatherMultiplier *= 1.3
                if (move.name == "weatherball")
                    weatherMultiplier *= 2
            }

            // Sandy Weather
            field.weather == "sandstorm" -> {
                // rock +1.5 spedef in sandstorm
                if (defenderSide.pokemon.types.contains(ElementalTypes.ROCK) && move.damageCategory == DamageCategories.SPECIAL)
                    weatherMultiplier *= 0.666
                if (move.name == "weatherball")
                    weatherMultiplier *= 2
            }
        }
        return weatherMultiplier
    }

    private fun getTerrainMultiplier (field: Field, attackerSide: Side, defenderSide: Side, move:ActivePokemonMove): Double {
        var terrainMultiplier = 1.0
        if (isGrounded(defenderSide.pokemon)) {
            when {
                field.terrain == "electricterrain" -> {
                    if (move.type == ElementalTypes.ELECTRIC) terrainMultiplier *= 1.5
                    if (attackerSide.pokemon.ability == "hadronengine" && move.damageCategory == DamageCategories.SPECIAL) terrainMultiplier *= 1.333
                    if (move.name == "risingvoltage") terrainMultiplier *= 2
                    if (move.name == "terrainpulse") terrainMultiplier *= 2
                }
                field.terrain == "grassyterrain" -> {
                    if (move.type == ElementalTypes.GRASS) terrainMultiplier *= 1.5
                    if (move.name in listOf("earthquake", "magnitude", "bulldoze")) terrainMultiplier *= 0.5
                    if (defenderSide.pokemon.ability == "grasspelt" && move.damageCategory == DamageCategories.PHYSICAL) terrainMultiplier *= 0.666
                    if (move.name == "terrainpulse") terrainMultiplier *= 2
                }
                field.terrain == "mistyterrain" -> {
                    if (move.type == ElementalTypes.DRAGON) terrainMultiplier *= 0.5
                    if (move.name == "mistyexplosion") terrainMultiplier *= 1.5
                    if (move.name == "terrainpulse") terrainMultiplier *= 2
                }
                field.terrain == "psychicterrain" -> {
                    if (move.type == ElementalTypes.PSYCHIC) terrainMultiplier *= 1.5
                    if (move.name == "expandingforce" && isGrounded(attackerSide.pokemon)) terrainMultiplier *= 1.5
                    if (move.priority > 0) terrainMultiplier = 0.0
                    if (move.name == "terrainpulse") terrainMultiplier *= 2
                }
            }
        }
        return terrainMultiplier
    }

    private fun getStabMultiplier (pokemon: ActivePokemon, move: ActivePokemonMove): Double {
        return when {
            move.type in pokemon.types ->
                when {
                    pokemon.ability == "adaptability" -> 2.0
                    else -> 1.5
                }
            else -> 1.0
        }
    }

    private fun getScreenMultiplier (attackerSide: Side, defenderSide: Side, move: ActivePokemonMove): Double{
        if (attackerSide.pokemon.ability != "infiltrator") {
            return when {
                defenderSide.screen.contains("protect") && move.damageCategory == DamageCategories.PHYSICAL -> 0.5
                defenderSide.screen.contains("lightscreen") && move.damageCategory == DamageCategories.SPECIAL -> 0.5
                defenderSide.screen.contains("auroraveil")  -> 0.5
                else -> 1.0
            }
        } else return 1.0
    }

    private fun getItemDamageMultiplier (field: Field, attackerSide: Side, defenderSide: Side, move: ActivePokemonMove, typeMultiplier: Double): Double{
        var itemMultiplier = 1.0
        when {
            attackerSide.pokemon.item == "choiceband" && move.damageCategory == DamageCategories.PHYSICAL -> itemMultiplier *= 1.5
            attackerSide.pokemon.item == "choicespecs" && move.damageCategory == DamageCategories.SPECIAL -> itemMultiplier *= 1.5
            attackerSide.pokemon.item == "lifeorb" -> itemMultiplier += 1.3
            attackerSide.pokemon.item == "expertbelt" && typeMultiplier > 1.0 -> itemMultiplier *= 1.2

            attackerSide.pokemon.item == "lightball" && attackerSide.pokemon.name == "pikachu" -> itemMultiplier *= 1.5
            attackerSide.pokemon.item == "souldew" && attackerSide.pokemon.name in listOf("latios", "latias") && move.type in listOf(ElementalTypes.PSYCHIC, ElementalTypes.DRAGON) -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "adamantorb" && attackerSide.pokemon.name == "dialga" && move.type in listOf(ElementalTypes.STEEL, ElementalTypes.DRAGON) -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "lustrousorb" && attackerSide.pokemon.name == "palkia" && move.type in listOf(ElementalTypes.WATER, ElementalTypes.DRAGON) -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "griseousorb" && attackerSide.pokemon.name == "giratina" && move.type in listOf(ElementalTypes.GHOST, ElementalTypes.DRAGON) -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "thickclub" && attackerSide.pokemon.name == "marowak" && move.damageCategory == DamageCategories.PHYSICAL -> itemMultiplier *= 2.0

            attackerSide.pokemon.item == "watergem" && move.type == ElementalTypes.WATER -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "firegem" && move.type == ElementalTypes.FIRE -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "grassgem" && move.type == ElementalTypes.GRASS -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "electricgem" && move.type == ElementalTypes.ELECTRIC -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "icegem" && move.type == ElementalTypes.ICE -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "psychicgem" && move.type == ElementalTypes.PSYCHIC -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "ghostgem" && move.type == ElementalTypes.GHOST -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "darkgem" && move.type == ElementalTypes.DARK -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "flyinggem" && move.type == ElementalTypes.FLYING -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "buggem" && move.type == ElementalTypes.BUG -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "rockgem" && move.type == ElementalTypes.ROCK -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "steelgem" && move.type == ElementalTypes.STEEL -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "normalgem" && move.type == ElementalTypes.NORMAL -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "fightinggem" && move.type == ElementalTypes.FIGHTING -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "groundgem" && move.type == ElementalTypes.GROUND -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "dragongem" && move.type == ElementalTypes.DRAGON -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "fairygem" && move.type == ElementalTypes.FAIRY -> itemMultiplier *= 1.3
            attackerSide.pokemon.item == "poisongem" && move.type == ElementalTypes.POISON -> itemMultiplier *= 1.3

            attackerSide.pokemon.item == "mysticwater" && move.type == ElementalTypes.WATER -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "charcoal" && move.type == ElementalTypes.FIRE -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "miracleseed" && move.type == ElementalTypes.GRASS -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "magnet" && move.type == ElementalTypes.ELECTRIC -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "nevermeltice" && move.type == ElementalTypes.ICE -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "twistedspoon" && move.type == ElementalTypes.PSYCHIC -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "spelltag" && move.type == ElementalTypes.GHOST -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "blackglasses" && move.type == ElementalTypes.DARK -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "sharpbeak" && move.type == ElementalTypes.FLYING -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "silverpowder" && move.type == ElementalTypes.BUG -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "hardstone" && move.type == ElementalTypes.ROCK -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "metalcoat" && move.type == ElementalTypes.STEEL -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "silkscarf" && move.type == ElementalTypes.NORMAL -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "blackbelt" && move.type == ElementalTypes.FIGHTING -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "softsand" && move.type == ElementalTypes.GROUND -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "dragonfang" && move.type == ElementalTypes.DRAGON -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "fairyfeather" && move.type == ElementalTypes.FAIRY -> itemMultiplier *= 1.2
            attackerSide.pokemon.item == "poisonbarb" && move.type == ElementalTypes.POISON -> itemMultiplier *= 1.2
        }
        when {
            defenderSide.pokemon.item == "assaultvest" && move.damageCategory == DamageCategories.SPECIAL -> itemMultiplier *= 0.666
            defenderSide.pokemon.item == "eviolite" -> itemMultiplier *= 0.666 // no need to check if not fully evolved because who the fuck would use eviolite on fully evolved pokemon
        }
        return itemMultiplier
    }

    private fun getMultiHitMultiplier (attackerSide: Side, move: ActivePokemonMove): Double{
        return when (move.name) {
            in multiHitMovesStandard ->
                when {
                    attackerSide.pokemon.ability == "skilllink" -> 5.0
                    attackerSide.pokemon.item == "loadeddice" -> 4.5
                    else -> 3.1
                }
            in multiHitMoves2Hits -> 2.0
            in multiHitMoves3Hits -> 3.0
            in listOf("tripleaxel", "triplekick") ->
                when {
                    attackerSide.pokemon.ability == "skilllink" -> 6.0
                    attackerSide.pokemon.item == "loadeddice" -> 6.0
                    attackerSide.pokemon.item == "widelens" -> 5.9
                    else -> 4.7
                }
            "populationbomb" ->
                when {
                    attackerSide.pokemon.ability == "skilllink" -> 10.0
                    attackerSide.pokemon.item == "loadeddice" -> 7.0 // supposed to hit 7 times on average
                    attackerSide.pokemon.item == "widelens" -> 9.5
                    else -> 5.9
                }
            else -> 1.0
        }
    }

    private fun getAccuracyMultiplier (field: Field, attackerSide: Side, defenderSide: Side, move: ActivePokemonMove): Double{
        // TODO handle cantmiss moves
        // TODO handle accuracy buffs/debuffs

        var accuracy = when {
            move.name in listOf("tripleaxel", "triplekick", "populationbomb") -> 1.0 // damage variation from accuracy already calculated in getMultiHitMultiplier
            attackerSide.pokemon.item == "widelens" -> move.accuracy*1.1
            attackerSide.pokemon.ability == "compoundeyes" -> move.accuracy*1.3
            field.weather == "snow" && defenderSide.pokemon.ability == "snowcloak" -> move.accuracy*0.8
            field.weather == "sandstorm" && defenderSide.pokemon.ability == "sandveil" -> move.accuracy*0.8
            else -> move.accuracy
        }
        if (accuracy > 1.0) accuracy = 1.0
        return accuracy
    }

    private fun getFlinchMultiplier(attackerSide: Side, defenderSide: Side, flinch: Double, canBeFlinched: Boolean): Double {
        var flinchChance = flinch
        if (attackerSide.pokemon.ability == "serenegrace") flinchChance *= 2.0
        if (defenderSide.pokemon.ability == "innerfocus") flinchChance *= 0
        if (canBeFlinched)  return 1.0-flinch
        else return 1.0
    }

    private fun getOtherDamageMultiplier (field: Field, attackerSide: Side, defenderSide: Side, move: ActivePokemonMove, opponentMove: ActivePokemonMove?): Double{
        // TODO : last resort, maybe one day
        // TODO : focus punch, maybe a little sooner
        var multiplier = 1.0

        when (attackerSide.pokemon.ability) {
            "supremeoverlord" -> multiplier *= 1.0+0.1*getDeadAlliesNumber(attackerSide)
            "hugepower" -> if (move.damageCategory == DamageCategories.PHYSICAL) multiplier *= 2.0
            "technician" -> if (move.power <= 60.0) multiplier *= 1.5
            "sheerforce" -> multiplier *= 1.3 // too many moves to handle, we'll suppose sheer force is always active
            "ironfist" -> if (move.name in punchMoves) multiplier *= 1.2
            "waterbubble" -> if (move.type == ElementalTypes.WATER) multiplier *= 2.0
            "transistor" -> if (move.type == ElementalTypes.ELECTRIC) multiplier *= 1.5
            "guts" -> if (attackerSide.pokemon.status != null && move.damageCategory == DamageCategories.PHYSICAL) multiplier *= 1.5
        }
        when (defenderSide.pokemon.ability) {
            "soundproof" -> if (move.name in soundMoves) multiplier = 0.0
            "bulletproof" -> if (move.name in bulletMoves) multiplier = 0.0
            "waterbubble" -> if (move.type == ElementalTypes.FIRE) multiplier *= 0.5
            "furcoat" -> if (move.damageCategory == DamageCategories.PHYSICAL) multiplier *= 0.5
            "thickfat" -> if (move.type in listOf(ElementalTypes.ICE, ElementalTypes.FIRE)) multiplier *= 0.5
            "fluffy" -> {
                if (move.damageCategory == DamageCategories.PHYSICAL) multiplier *= 0.5
                if (move.type == ElementalTypes.FIRE) multiplier *= 2.0
            }
            in listOf("multiscale", "shadowShield") -> if (defenderSide.pokemon.currentHp.toDouble() == defenderSide.pokemon.stats.hp) multiplier *= 0.5
        }
        when (move.name) {
            "fishiousrend" -> if (selectedIsQuicker(field, attackerSide, defenderSide) || opponentMove == null) multiplier *= 2.0
            "synchronoise" -> if (attackerSide.pokemon.types.intersect(defenderSide.pokemon.types).isEmpty()) multiplier *= 0.0
            "acrobatics" -> if (attackerSide.pokemon.item == "") multiplier*= 2.0
            "knockoff" -> if (defenderSide.pokemon.item != "") multiplier *= 1.5
            "facade" -> if (attackerSide.pokemon.status != null) multiplier *= 2.0
            "hex" -> if (defenderSide.pokemon.status != null) multiplier *= 2.0
            in listOf("suckerpunch", "thunderclap") -> if (opponentMove == null || opponentMove.damageCategory == DamageCategories.STATUS) multiplier *= 0.0
        }

        return multiplier
    }

    private fun getHealFromMove(attackerSide: Side, move: ActivePokemonMove, damage:Double): Double {
        var heal = 0.0
        when (move.name) {
            in lifeStealMoves50 -> heal = damage*0.5
            in lifeStealMoves75 -> heal = damage*0.75
        }
        if (attackerSide.pokemon.item == "bigroot") heal*=1.3

        return heal
    }

    private fun getSelfDamageFromMove(attackerSide: Side, defenderSide: Side, move: ActivePokemonMove, damage:Double): Double {
        var selfDamage = 0.0
        if (attackerSide.pokemon.ability != "rockhead") {
            when (move.name) {
                in selfDamage1to4Moves -> selfDamage += damage/4
                in selfDamage1to3Moves -> selfDamage += damage/3
                in selfDamage1to2Moves -> selfDamage += damage/2
            }
        }
        if (attackerSide.pokemon.item == "lifeorb") selfDamage += attackerSide.pokemon.stats.hp/10
        if (defenderSide.pokemon.item == "rockyhelmet" && move.name in contactMoves) selfDamage += attackerSide.pokemon.stats.hp/6
        if (defenderSide.pokemon.ability == "ironbarbs" && move.name in contactMoves) selfDamage += attackerSide.pokemon.stats.hp/8

        /* if (attackerSide.pokemon.volatileStatus.contains("confusion"))
            TODO confusion damage (a 40 damage attack to itself)
         */

        return selfDamage
    }

    private fun getRealSpeed(field: Field, pokemon: ActivePokemon, opponent: ActivePokemon): Double {
        // TODO Unburden

        val boostedStats = getBoostedStats(pokemon, opponent)
        var speed = boostedStats.speed

        when (pokemon.ability) {
            "swiftswim" -> if (field.weather == "raining") speed*=2.0
            "sandrush" -> if (field.weather == "sandstorm") speed*=2.0
            "chlorophyll" -> if (field.weather == "sunny") speed*=2.0
            "slushrush" -> if (field.weather == "snow") speed*=2.0
            "surgesurfer" -> if (field.terrain == "electricfield") speed*=2.0
            "quickfeet" -> if (pokemon.status != null) speed*=1.5
        }

        when (pokemon.item) {
            "choicescarf" -> speed*=1.5
            "ironball" -> speed*=0.5
        }

        if (pokemon.status == "par") speed*=0.5

        return speed
    }

    private fun selectedIsQuicker (field: Field, selectedSide: Side, otherSide: Side): Boolean {
        val selectedSpeed = getRealSpeed(field, selectedSide.pokemon, otherSide.pokemon)
        val otherSpeed = getRealSpeed(field, otherSide.pokemon, selectedSide.pokemon)
        return selectedSpeed > otherSpeed
    }

    // check if selectedSide attacks first
    private fun selectedAttackFirst (field: Field, selectedSide: Side, otherSide: Side, selectedMove: ActivePokemonMove, otherMove: ActivePokemonMove): Boolean{
        // TODO (or not TODO) handle speed tie

        when {
            selectedMove.priority > otherMove.priority -> return true
            otherMove.priority > selectedMove.priority -> return false
            else -> {
                val selectedSpeed = getRealSpeed(field, selectedSide.pokemon, otherSide.pokemon)
                val otherSpeed = getRealSpeed(field, otherSide.pokemon, selectedSide.pokemon)

                if (field.trickRoom) return !selectedIsQuicker(field,selectedSide,otherSide)
                else return selectedIsQuicker(field,selectedSide,otherSide)
            }
        }
    }

    private fun canSwitch (request:ShowdownActionRequest?, pokemon: ActivePokemon, opponentPokemon: ActivePokemon): Boolean {
        request?.active?.forEach { if (it.trapped) return false }
        when (opponentPokemon.ability) {
            "shadowtag" -> if (pokemon.ability != "shadowtag" && !pokemonHasType(pokemon, ElementalTypes.GHOST)) return false
            "arenatrap" -> if (!isGrounded(pokemon) && !pokemonHasType(pokemon, ElementalTypes.GHOST)) return false
            "magnetpull" -> if (pokemonHasType(pokemon, ElementalTypes.STEEL) && !pokemonHasType(pokemon, ElementalTypes.GHOST)) return false
        }
        return true
    }

    private fun pokemonHasType(pokemon: ActivePokemon, lookedType: ElementalType): Boolean {
        for (type in pokemon.types) {
            if (type == lookedType) return true
        }
        return false
    }

    private fun isGrounded (pokemon: ActivePokemon): Boolean { // TODO : add magnet-rize (it's a volatile ?)
        return !(pokemon.types.contains(ElementalTypes.FLYING) || pokemon.ability == "levitate" || pokemon.item == "airbaloon")
    }

    private fun isPowderProtected (pokemon: ActivePokemon): Boolean {
        if (pokemonHasType(pokemon, ElementalTypes.GRASS) || pokemon.item == "safetygoogles" || pokemon.ability == "overcoat") return true
        else return false
    }

    private fun boostCanGoHigher(pokemon: ActivePokemon, maxBoost: Int): Boolean {
        if (pokemon.statBoosts.attack < maxBoost && pokemon.statBoosts.specialAttack < maxBoost && pokemon.statBoosts.defense < maxBoost && pokemon.statBoosts.specialDefense < maxBoost && pokemon.statBoosts.speed < maxBoost)
            return true
        else
            return false
    }

    private fun getCumulatedBoosts(pokemon: ActivePokemon): Double {
        return pokemon.statBoosts.attack + pokemon.statBoosts.defense + pokemon.statBoosts.speed + pokemon.statBoosts.specialAttack + pokemon.statBoosts.specialDefense
    }

    private fun canUseStatusMove(field: Field, move: ActivePokemonMove, attacker: ActivePokemon, target: ActivePokemon): Boolean {
        // prevent using hazard or status if it's switched back
        if (move.damageCategory == DamageCategories.STATUS && (target.ability== "magicbounce" || target.volatileStatus.contains("magiccoat")))
            return false

        // prevent using status or volatile status against a clone or a gholdengo
        if (!(move.name in entryHazards) && (target.volatileStatus.contains("substitute") || target.ability == "goodasgold"))
            return false

        // prevent using status against a target that is immune or will cure it too easily
        if (target.ability in listOf("comatose", "purifyingsalt", "naturalcure")
            || (target.ability == "hydration" && field.weather == "raining")
            || (target.ability == "leafguard" && field.weather == "sunny"))
            return false


        return true
    }

    private fun canUseBoostMove(move: ActivePokemonMove, attacker: ActivePokemon): Boolean {
        if (move.damageCategory == DamageCategories.STATUS && attacker.volatileStatus.contains("taunt")) return false
        else return true
    }

    // TODO : remove this and replace by findPokemonMove(stuff) != null
    private fun pokemonHasMove(pokemon: ActivePokemon, move: String): Boolean {
        if (move in pokemon.moveSet.map {it.name}) return true
        else return false
    }

    private fun pokemonHasMove(pokemon: ActivePokemon, moveList: List<String>): Boolean {
        if (pokemon.moveSet.map {it.name}.intersect(moveList.toSet()).isNotEmpty()) return true
        else return false
    }

    private fun findPokemonMove(pokemon: ActivePokemon, move: String): ActivePokemonMove? {
        pokemon.moveSet. forEach {
            if (it.name == move) return it
        }
        return null
    }

    private fun findPokemonMove(pokemon: ActivePokemon, moveList: List<String>): ActivePokemonMove? {
        pokemon.moveSet. forEach {
            if (it.name in moveList) return it
        }
        return null
    }

    private fun pokemonHasBoostMove(pokemon: ActivePokemon): Boolean {
        pokemon.moveSet.forEach {
            if (boostFromMoves.containsKey(it.name)) return true
        }
        return false
    }

    private fun lastMoveBeforeDying (field: Field, attackerSide: Side, defenderSide: Side, turnsToLive: Int, move: ActivePokemonMove, opponentMove:ActivePokemonMove): Boolean {
        if ((!selectedAttackFirst(field, attackerSide, defenderSide,move,opponentMove) && turnsToLive == 2)
            || (selectedAttackFirst(field, attackerSide, defenderSide,move,opponentMove) && turnsToLive == 1))
                return true
        else return false
    }

    private fun npcAttacksFirst(fightResult: Battle1v1State, move: ActivePokemonMove?): Boolean {
        lateinit var usedMove: ActivePokemonMove
        if (move != null) usedMove = move
            else usedMove = fightResult.npcMovesInfos.usedMove.move

        if (usedMove.priority > fightResult.playerMostProbableMove.priority) return true
        else if (usedMove.priority < fightResult.playerMostProbableMove.priority) return false
        else return fightResult.npcIsQuicker
    }

    private fun countMoveWithDamage(field: Field, attackerSide: Side, defenderSide: Side, damage: Double, moreOrLess:Boolean): Int {
        // more if moreOrLess is true, less if false
        var counter = 0
        lateinit var moveDamage: DamageHeal
        for (move in attackerSide.pokemon.moveSet) {
            moveDamage = damageAndHealDoneByMove(field, attackerSide, defenderSide, move, false, 0.0, null)
            if (moreOrLess && moveDamage.damage > damage) counter += 1
            if (!moreOrLess && moveDamage.damage < damage) counter += 1
        }
        return counter
    }

    private fun expectedTurnsToPlay(pokemonTurnsToLive: Int, pokemonIsQuicker: Boolean): Int {
        var turnsToPlay = pokemonTurnsToLive
        if (!pokemonIsQuicker) turnsToPlay -= 1
        return turnsToPlay
    }

    private fun getNumberOfHitTaken(pokemon: ActivePokemon): Int {
        if (battleTracker.pokemons[pokemon.uuid] != null) return battleTracker.pokemons[pokemon.uuid]!!.takenHit
        else return 0
    }

    private fun xChanceOn100(x: Int): Boolean {
        return Random.nextDouble() < x / 100.0
    }

    private fun updateBattleTracker(battle: PokemonBattle) {

        var seenP1Adamage = false
        var seenP2Adamage = false

        var uuid: String
        var newItem: String
        var newAbility: String
        var transformTarget: String
        var faintPlayer: String

        val battleLogs = battle.battleLog
        val logLines = battleLogs[battleLogs.size - 2].lines()
        val endItemLines = logLines.filter { it.contains("-enditem") }
        val itemLines = logLines.filter { it.contains("-item") }
        val endAbilityLines = logLines.filter { it.contains("-endability") }
        val abilityLines = logLines.filter { it.contains("-ability") }
        val transformLines = logLines.filter { it.contains("-transform") }
        val faintLines = logLines.filter { it.contains("|faint|") }
        var disguiseBrokenLines = logLines.filter { it.contains(Regex("\\|detailschange\\|.*Mimikyu-Busted")) }
        var damageTakenLines = logLines.filter { it.contains("-damage") }

        endItemLines.forEach {
            uuid = it.split('|')[2].split(":")[1].trim()
            updateBattleTrackerLine(UUID.fromString(uuid),ResetProperty.ITEM, "", battle)
        }

        itemLines.forEach {
            uuid = it.split('|')[2].split(":")[1].trim()
            newItem = it.split('|')[3].replace(" ","").lowercase()
            updateBattleTrackerLine(UUID.fromString(uuid),ResetProperty.ITEM, newItem, battle)
        }

        endAbilityLines.forEach {
            uuid = it.split('|')[2].split(":")[1].trim()
            updateBattleTrackerLine(UUID.fromString(uuid),ResetProperty.ABILITY, "", battle)
        }

        abilityLines.forEach {
            uuid = it.split('|')[2].split(":")[1].trim()
            newAbility = it.split('|')[3].replace(" ","").lowercase()
            updateBattleTrackerLine(UUID.fromString(uuid),ResetProperty.ABILITY, newAbility, battle)
        }

        transformLines.forEach {
            uuid = it.split('|')[2].split(":")[1].trim()
            transformTarget = it.split('|')[3].split(":")[1].trim()
            updateBattleTrackerLine(UUID.fromString(uuid),ResetProperty.TRANSFORM, transformTarget, battle)
        }

        faintLines.forEach {
            faintPlayer = it.split('|')[2].split(":")[0].trim()
            if (faintPlayer == "p1a") battleTracker.deathNumber.player++
            else if (faintPlayer == "p2a") battleTracker.deathNumber.npc++
        }

        disguiseBrokenLines.forEach {
            uuid = it.split('|')[2].split(":")[1].trim()
            updateBattleTrackerLine(UUID.fromString(uuid),ResetProperty.DISGUISEBROKEN, "", battle)
        }

        damageTakenLines.forEach {
            uuid = it.split('|')[2].split(":")[1].trim()
            if (it.contains("p1a") && !seenP1Adamage) {
                updateBattleTrackerLine(UUID.fromString(uuid),ResetProperty.TAKENHIT, "", battle)
                seenP1Adamage = true
            }
            if (it.contains("p2a") && !seenP2Adamage) {
                updateBattleTrackerLine(UUID.fromString(uuid),ResetProperty.TAKENHIT, "", battle)
                seenP2Adamage = true
            }
        }
    }

    private fun updateBattleTrackerLine(uuid: UUID, property: ResetProperty, value: String, battle: PokemonBattle) {
        if (battleTracker.pokemons[uuid] == null)
            battleTracker.pokemons[uuid] = PokemonTracker(null, null, null, false, false, 0)
        when (property) {
            ResetProperty.ITEM -> battleTracker.pokemons[uuid]!!.item = value
            ResetProperty.ABILITY -> battleTracker.pokemons[uuid]!!.ability = value
            ResetProperty.TRANSFORM -> {
                var copiedPokemon: Transform? = null
                if (battle.side1.activePokemon.get(0).battlePokemon?.uuid == UUID.fromString(value)) {
                    copiedPokemon = Transform(
                        battle.side1.activePokemon.get(0).battlePokemon!!.originalPokemon,
                        getStatsBoosts(battle.side1.activePokemon.get(0).battlePokemon!!.contextManager)
                    )
                } else if (battle.side2.activePokemon.get(0).battlePokemon?.uuid == UUID.fromString(value)) {
                    copiedPokemon = Transform(
                        battle.side2.activePokemon.get(0).battlePokemon!!.originalPokemon,
                        getStatsBoosts(battle.side2.activePokemon.get(0).battlePokemon!!.contextManager)
                    )
                }
                battleTracker.pokemons[uuid]!!.transform = copiedPokemon
            }
            ResetProperty.DISGUISEBROKEN -> battleTracker.pokemons[uuid]!!.disguiseBroken = true
            ResetProperty.TAKENHIT -> battleTracker.pokemons[uuid]!!.takenHit++
        }
    }

    private fun getMoveFromShowdownMoveSet(moveset: ShowdownMoveset, name: String): InBattleMove? {
        // TODO: this shouldn't return null ever, can we return struggle instead ?
        for (move in moveset.moves) {
            if (move.id == name) return move
        }
        return null
    }

    private fun getEnabledMoves(moveset: List<ActivePokemonMove>): List<ActivePokemonMove> {
        val enabledMove = mutableListOf<ActivePokemonMove>()
        moveset.forEach {
            if (!it.disabled)
                enabledMove.add(it)
        }
        return enabledMove
    }

    private fun getDeadAlliesNumber(side: Side): Int {
        return if (side.owner == SideOwner.PLAYER) battleTracker.deathNumber.player
        else if (side.owner == SideOwner.NPC) battleTracker.deathNumber.npc
        else 0
    }

    private fun shouldUsePivotMove(field: Field, playerSide: Side, npcSide: Side, fightResult: Battle1v1State): ActivePokemonMove? {
        val pivotMove = findPokemonMove(npcSide.pokemon, pivotMoves)
        if (pivotMove != null) {
            when (pivotMove.name) {
                "teleport" -> if (fightResult.turnsToKillNpc >= 2) return pivotMove
                "shedTail" -> if (npcSide.pokemon.currentHp > npcSide.pokemon.stats.hp/2) {
                    if (npcAttacksFirst(fightResult, pivotMove)
                        || npcSide.pokemon.currentHp - damageAndHealDoneByMove(field, playerSide, npcSide, fightResult.playerMostProbableMove, true, 0.0, null).damage > npcSide.pokemon.stats.hp/2)
                        return pivotMove
                }
                else -> {
                    if (npcAttacksFirst(fightResult, pivotMove) || fightResult.turnsToKillNpc >= 2) {
                        when (pivotMove.name) {
                            in offensivePivotMoves -> if (getDamageTypeMultiplier(field, npcSide, playerSide, pivotMove) > 0) return pivotMove
                            in listOf("batonpass", "chillyreception") -> return pivotMove
                            "partingshot" -> if (playerSide.pokemon.ability !in listOf("soundproof", "goodasgold")) return pivotMove
                        }
                    }
                }
            }
        }
        return null
    }

    private fun standardSwitches(field: Field, playerSide: Side, npcSide: Side, fightResult: Battle1v1State): List<Switch> {
        val availableSwitches = mutableListOf<Switch>()
        val pivotMove = shouldUsePivotMove(field, playerSide, npcSide, fightResult)


        for (pokemon in npcSide.team) {
            if (get1v1ResultWithSwitch(field, playerSide, npcSide, pokemon, null, fightResult.playerMostProbableMove).npcWins)
                availableSwitches.add(Switch(pokemon.uuid,pivotMove, false))
        }
        return availableSwitches
    }

    // search for an anti-setup pokemon
    private fun antiSetupSwitches(field: Field, playerSide: Side, npcSide: Side, fightResult: Battle1v1State): List<Switch> {
        val availableSwitches = mutableListOf<Switch>()
        val pivotMove = shouldUsePivotMove(field, playerSide, npcSide, fightResult)
        lateinit var tryToFightAfterSwitch: Battle1v1State

        for (pokemon in npcSide.team) {
            if (pokemonHasMove(pokemon, antiBoostMoves)) {
                tryToFightAfterSwitch = get1v1ResultWithSwitch(field, playerSide, npcSide, pokemon, null, fightResult.playerMostProbableMove)
                if (tryToFightAfterSwitch.turnsToKillNpc > 1 || tryToFightAfterSwitch.npcIsQuicker)
                    availableSwitches.add(Switch(pokemon.uuid,pivotMove, true))
            }
        }
        return availableSwitches
    }

    // look for a setup pokemon first and for a winner one after
    private fun shedTailSwitches(field: Field, playerSide: Side, npcSide: Side, fightResult: Battle1v1State): List<Switch> {
        val availableSwitches = mutableListOf<Switch>()
        val shedTail = shouldUsePivotMove(field, playerSide, npcSide, fightResult)

        for (pokemon in npcSide.team) {
            if (pokemonHasBoostMove(pokemon)) {
                if (get1v1ResultWithSwitch(field, playerSide, npcSide, pokemon, null, fightResult.playerMostProbableMove).npcWins)
                    availableSwitches.add(Switch(pokemon.uuid, shedTail, false))
            }
        }
        return if (availableSwitches.isNotEmpty()) availableSwitches
        else standardSwitches(field, npcSide, playerSide, fightResult)
    }

    private fun switchStrategy(field: Field, playerSide: Side, npcSide: Side, fightResult: Battle1v1State, forceSwitch: Boolean): Switch {
        var availableSwitches:List<Switch>
        var move:ActivePokemonMove? = null

        // both pokemon dead at the same turn
        // if both pokemons are gone, we switch randomly before calculating anything
        if (npcSide.pokemon.name == "" && playerSide.pokemon.name == "") {
            return Switch(npcSide.team.random().uuid, move, false)
        }

        // antiboost security,
        if (getCumulatedBoosts(playerSide.pokemon) > 0 && !pokemonHasMove(npcSide.pokemon, antiBoostMoves)) {
            availableSwitches = antiSetupSwitches(field, playerSide, npcSide, fightResult)
            if (availableSwitches.isNotEmpty()) return availableSwitches.random()
        }

        // palafin switch
        if (npcSide.pokemon.ability == "zerotohero" && (battleTracker.pokemons[npcSide.pokemon.uuid] == null || !battleTracker.pokemons[npcSide.pokemon.uuid]!!.isAHero)) {
            availableSwitches = standardSwitches(field, playerSide, npcSide, fightResult)
            if (availableSwitches.isNotEmpty()) return availableSwitches.random()
            else if (npcSide.team.isNotEmpty()) return Switch(npcSide.team.random().uuid, null, false) // we switch anyway, we don't want to keep a zero palafin active
        }

        // shed tail move
        if (pokemonHasMove(npcSide.pokemon, "shedtail")) {
            availableSwitches = shedTailSwitches(field, playerSide, npcSide, fightResult)
            if (availableSwitches.isNotEmpty() && availableSwitches.random().move != null) return availableSwitches.random()
        }

        // offensive proactive move
        if (pokemonHasMove(npcSide.pokemon, pivotMoves)) {
            availableSwitches = standardSwitches(field, playerSide, npcSide, fightResult)
            if (availableSwitches.isNotEmpty() && availableSwitches.random().move != null && xChanceOn100(50)) return availableSwitches.random()
        }

        // fight lost
        if (!fightResult.npcWins) {
            availableSwitches = standardSwitches(field, playerSide, npcSide, fightResult)
            if (availableSwitches.isNotEmpty()) return availableSwitches.random()
        }

        // no relevant switch found
        return if (forceSwitch && npcSide.team.isNotEmpty()) Switch(npcSide.team.random().uuid, null, false)
        else Switch(null, null, false)
    }

    private fun emptySlotSwitchStrategy(field: Field, playerSide: Side, npcSide: Side): UUID {
        val availableSwitches = mutableListOf<UUID>()

        // if opponent pokemon is dead, we don't calculate anything and just send a random pokemon
        if (playerSide.pokemon.name == "")
            return npcSide.team.random().uuid

        for (pokemon in npcSide.team) {
            if (get1v1ResultWithSwitch(field, playerSide, npcSide, pokemon, null, null).npcWins)
                availableSwitches.add(pokemon.uuid)
        }

        // forced switch because no pokemon on field
        return if (availableSwitches.isEmpty()) npcSide.team.random().uuid
        else availableSwitches.random()
    }

    // check if one of the trainers switched to reset abilities
    private fun checkPreviousPokemonForReset(playerPokemon: BattlePokemon?, npcPokemon: BattlePokemon?) {
        if (battleTracker.previousPlayerPokemon != playerPokemon?.uuid) resetTrackerAbility(playerPokemon?.uuid, playerPokemon)
        if (battleTracker.previousNpcPokemon != npcPokemon?.uuid) resetTrackerAbility(npcPokemon?.uuid, npcPokemon)
    }

    private fun updatePreviousPokemon(playerPokemon: ActivePokemon?, npcPokemon: ActivePokemon?){
        battleTracker.previousPlayerPokemon = playerPokemon?.uuid
        battleTracker.previousNpcPokemon = npcPokemon?.uuid
    }

    // use this and rename it if I/You have to reset more stuff in battleTracker when switched out (just need abilities right now)
    private fun resetTrackerAbility(pokemonUUID: UUID?, pokemonInfo: BattlePokemon?) {
        if (battleTracker.pokemons[pokemonUUID] != null)
            battleTracker.pokemons[pokemonUUID]!!.ability = null
    }

    private fun destinyBondWarning(battle1v1State: Battle1v1State): Boolean {
        if (battleTracker.previousMoves.player?.name == "destinybond" && !battle1v1State.npcIsQuicker) return true
        else return false
    }

    // old function definition
    //override fun choose(request: ShowdownActionRequest, active: ActivePokemon, moves: List<MoveChoice>, canDynamax: Boolean, possibleMoves: List<Move>): ShowdownActionResponse {
    override fun choose(activeBattlePokemon: ActiveBattlePokemon, moveset: ShowdownMoveset?, forceSwitch: Boolean): ShowdownActionResponse {
        try {
            // get the current battle and set it as a variable
            val battle = activeBattlePokemon.battle
            val p1Actor = battle.side1.actors.first()
            val p2Actor = battle.side2.actors.first()
            val activePlayerBattlePokemon = p1Actor.activePokemon[0].battlePokemon
            val activeNPCBattlePokemon = p2Actor.activePokemon[0].battlePokemon

            checkPreviousPokemonForReset(activePlayerBattlePokemon, activeNPCBattlePokemon)
            updateBattleTracker(battle)
            val battleState = getBattleInfos(activeBattlePokemon)

            // if we're dead, we look for a good switch, if we can't find one, we switch randomly
            if (forceSwitch || activeBattlePokemon.isGone()) {
                if (battleTracker.npcForcedSwitch != null) {
                    val forcedSwitch = battleTracker.npcForcedSwitch!!
                    battleTracker.npcForcedSwitch = null
                    return SwitchActionResponse(forcedSwitch)
                } else return SwitchActionResponse(emptySlotSwitchStrategy(battleState.field, battleState.playerSide, battleState.npcSide))
            }

            //println(battle.battleLog)
            //println(battleState.npcSide.pokemon.stats.toString())
            //println(activeBattlePokemon.battlePokemon?.statChanges.toString())

            // we should get to here only if we have a pokemon on each side of the board
            val fightResult = get1v1Result(battleState.field, battleState.playerSide, battleState.npcSide)
            val utilityMove = usableUtilityMove(battleState.field, battleState.npcSide, battleState.playerSide, fightResult.turnsToKillNpc, fightResult.turnsToKillPlayer, fightResult.npcWins, fightResult.playerMostProbableMove, battleTracker.previousMoves.npc, battleTracker.previousMoves.player)
            val chosenSwitch = switchStrategy(battleState.field, battleState.playerSide, battleState.npcSide, fightResult, false)
            updatePreviousPokemon(battleState.playerSide.pokemon, battleState.npcSide.pokemon)

            // if we can kill immediatly
            if (fightResult.npcMovesInfos.killerMoves.isNotEmpty() && fightResult.npcWins && !destinyBondWarning(fightResult)) {
                // we use a killing move at random
                return chooseMove(getMoveFromShowdownMoveSet(moveset!!, fightResult.npcMovesInfos.killerMoves.random().name)!!, activeBattlePokemon)
            }

            // if we determined the switch is urgent and can switch, we do
            if (canSwitch(p2Actor.request, battleState.npcSide.pokemon, battleState.playerSide.pokemon) && chosenSwitch.uuid != null && chosenSwitch.isUrgent) {
                if (chosenSwitch.move != null) {
                    battleTracker.npcForcedSwitch = chosenSwitch.uuid!!
                    return chooseMove(getMoveFromShowdownMoveSet(moveset!!, chosenSwitch.move!!.name)!!, activeBattlePokemon)
                } else return SwitchActionResponse(chosenSwitch.uuid!!)
            }

            // we check for important utility moves before deciding if we attack or switch
            if (utilityMove != null) return chooseMove(getMoveFromShowdownMoveSet(moveset!!, utilityMove.name)!!, activeBattlePokemon)

            // if we can switch and we determined we should, we do
            if (canSwitch(p2Actor.request, battleState.npcSide.pokemon, battleState.playerSide.pokemon) && chosenSwitch.uuid != null) {
                if (chosenSwitch.move != null) {
                    battleTracker.npcForcedSwitch = chosenSwitch.uuid!!
                    return chooseMove(getMoveFromShowdownMoveSet(moveset!!, chosenSwitch.move!!.name)!!, activeBattlePokemon)
                } else return SwitchActionResponse(chosenSwitch.uuid!!)
            }

            // if opponent used destiny bond, we use a 0-damage move or try to switch
            if (destinyBondWarning(fightResult)) {
                if (fightResult.npcMovesInfos.notDamagingMoves.isNotEmpty())
                    return chooseMove(getMoveFromShowdownMoveSet(moveset!!, fightResult.npcMovesInfos.notDamagingMoves.random().name)!!, activeBattlePokemon)
                else {
                    if (chosenSwitch.uuid != null) return SwitchActionResponse(chosenSwitch.uuid!!)
                }
            }

            // if we win the 1v1 or can't find a relevant switch, we fight
            if (xChanceOn100(80) || battleState.npcSide.pokemon.status == "slp") // a bit weird here but if pokemon is asleep, damagingMoves will always be empty, so we won't try another damaging move because we have none
                return chooseMove(getMoveFromShowdownMoveSet(moveset!!, fightResult.npcMovesInfos.usedMove.move.name)!!, activeBattlePokemon)
            else {
                if (fightResult.npcMovesInfos.damagingMoves.isNotEmpty())
                    return chooseMove(getMoveFromShowdownMoveSet(moveset!!, fightResult.npcMovesInfos.damagingMoves.random().name)!!, activeBattlePokemon)
            }

            println("This AI is shit and couldn't know what to do")
            /* if we get here, the pokemon has no more useful move, we force a switch */
            val nothingToDoSwitch = switchStrategy(battleState.field, battleState.playerSide, battleState.npcSide, fightResult, true)
            return if (nothingToDoSwitch.uuid != null) SwitchActionResponse(nothingToDoSwitch.uuid!!)
            else chooseMove(getMoveFromShowdownMoveSet(moveset!!, battleState.npcSide.pokemon.moveSet.random().name)!!, activeBattlePokemon)
            //return PassActionResponse


            // en gros pour switch faut return SwitchActionResponse(switchTo.uuid)
            // et pour attaquer faut return chooseMove(move, activeBattlePokemon) move c'est un InBattleMove et activeBattlePokemon c'est un ActiveBattlePokemon
            // return PassActionResponse pour rien faire
            // return MoveActionResponse("struggle") quand on a pas le choix ?

        } catch (e: Exception) {
            e.printStackTrace()
            activeBattlePokemon.battle.players.forEach { it.sendMessage(Text.literal(
                Formatting.RED.toString() +
                "An error occurred in the Strong Trainer AI, please report this to the devs."
            )) }
            return PassActionResponse
        }
    }

    private fun chooseMove(move: InBattleMove, activeBattlePokemon: ActiveBattlePokemon): MoveActionResponse {
        val target = if (move.mustBeUsed()) null else move.target.targetList(activeBattlePokemon)
        if (target == null)
            return MoveActionResponse(move.id)
        else {
            val chosenTarget = target.filter { !it.isAllied(activeBattlePokemon) }.randomOrNull() ?: target.random()
            return MoveActionResponse(move.id, (chosenTarget as ActiveBattlePokemon).getPNX())
        }
    }
}
