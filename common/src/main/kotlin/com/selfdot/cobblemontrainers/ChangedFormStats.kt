package com.selfdot.cobblemontrainers

import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.pokemon.EVs
import com.cobblemon.mod.common.pokemon.IVs
import com.cobblemon.mod.common.pokemon.Nature

/**
 * Temporary workaround for transformed pokemon since battlePokemon.effectedPokemon do not return what we're looking for (I suppose it should be the transformed one
 */

data class NatureBoost(
    val increased: Stat,
    val decreased: Stat
)

private val natureBoosts = mapOf(
    Natures.LONELY to NatureBoost(Stats.ATTACK, Stats.DEFENCE),
    Natures.BRAVE to NatureBoost(Stats.ATTACK, Stats.SPEED),
    Natures.ADAMANT to NatureBoost(Stats.ATTACK, Stats.SPECIAL_ATTACK),
    Natures.NAUGHTY to NatureBoost(Stats.ATTACK, Stats.SPECIAL_DEFENCE),
    Natures.BOLD to NatureBoost(Stats.DEFENCE, Stats.ATTACK),
    Natures.RELAXED to NatureBoost(Stats.DEFENCE, Stats.SPEED),
    Natures.IMPISH to NatureBoost(Stats.DEFENCE, Stats.SPECIAL_ATTACK),
    Natures.LAX to NatureBoost(Stats.DEFENCE, Stats.SPECIAL_DEFENCE),
    Natures.TIMID to NatureBoost(Stats.SPEED, Stats.ATTACK),
    Natures.HASTY to NatureBoost(Stats.SPEED, Stats.DEFENCE),
    Natures.JOLLY to NatureBoost(Stats.SPEED, Stats.SPECIAL_ATTACK),
    Natures.NAIVE to NatureBoost(Stats.SPEED, Stats.SPECIAL_DEFENCE),
    Natures.MODEST to NatureBoost(Stats.SPECIAL_ATTACK, Stats.ATTACK),
    Natures.MILD to NatureBoost(Stats.SPECIAL_ATTACK, Stats.DEFENCE),
    Natures.QUIET to NatureBoost(Stats.SPECIAL_ATTACK, Stats.SPEED),
    Natures.RASH to NatureBoost(Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE),
    Natures.CALM to NatureBoost(Stats.SPECIAL_DEFENCE, Stats.ATTACK),
    Natures.GENTLE to NatureBoost(Stats.SPECIAL_DEFENCE, Stats.DEFENCE),
    Natures.SASSY to NatureBoost(Stats.SPECIAL_DEFENCE, Stats.SPEED),
    Natures.CAREFUL to NatureBoost(Stats.SPECIAL_DEFENCE, Stats.SPECIAL_ATTACK),
    Natures.HARDY to NatureBoost(Stats.ATTACK, Stats.ATTACK),
    Natures.DOCILE to NatureBoost(Stats.DEFENCE, Stats.DEFENCE),
    Natures.SERIOUS to NatureBoost(Stats.SPEED, Stats.SPEED),
    Natures.BASHFUL to NatureBoost(Stats.SPECIAL_ATTACK, Stats.SPECIAL_ATTACK),
    Natures.QUIRKY to NatureBoost(Stats.SPECIAL_DEFENCE, Stats.SPECIAL_DEFENCE)
)

object TransformedStats {
    private val statsMap = mapOf(
        "palafinhero" to PokemonFormChange(
            listOf(ElementalTypes.WATER),
            PokemonStats(
                hp = 100.0,
                attack = 160.0,
                defense = 97.0,
                specialAttack = 106.0,
                specialDefense = 87.0,
                speed = 100.0
            )
        ),
        "wishiwashischool" to PokemonFormChange(
            listOf(ElementalTypes.WATER),
            PokemonStats(
                hp = 45.0,
                attack = 140.0,
                defense = 130.0,
                specialAttack = 140.0,
                specialDefense = 135.0,
                speed = 30.0
            )
        ),
        "miniormeteor" to PokemonFormChange(
            listOf(ElementalTypes.ROCK, ElementalTypes.FLYING),
            PokemonStats(
                hp = 60.0,
                attack = 60.0,
                defense = 100.0,
                specialAttack = 60.0,
                specialDefense = 100.0,
                speed = 60.0
            )
        ),
        "ashgreninja" to PokemonFormChange( // didn't battle bond disappeared in recent gens ?
            listOf(ElementalTypes.WATER, ElementalTypes.DARK),
            PokemonStats(
                hp = 72.0,
                attack = 145.0,
                defense = 67.0,
                specialAttack = 153.0,
                specialDefense = 71.0,
                speed = 132.0
            )
        ),
        "darmanitanzen" to PokemonFormChange(
            listOf(ElementalTypes.FIRE, ElementalTypes.PSYCHIC),
            PokemonStats(
                hp = 105.0,
                attack = 30.0,
                defense = 105.0,
                specialAttack = 140.0,
                specialDefense = 105.0,
                speed = 55.0
            )
        ),
        "zygardecomplete" to PokemonFormChange(
            listOf(ElementalTypes.DRAGON, ElementalTypes.GROUND),
            PokemonStats(
                hp = 216.0,
                attack = 100.0,
                defense = 121.0,
                specialAttack = 91.0,
                specialDefense = 95.0,
                speed = 85.0
            )
        ),
        "eiscuenoice" to PokemonFormChange(
            listOf(ElementalTypes.ICE),
            PokemonStats(
                hp = 75.0,
                attack = 80.0,
                defense = 70.0,
                specialAttack = 65.0,
                specialDefense = 50.0,
                speed = 130.0
            )
        ),
        "aegislashblade" to PokemonFormChange(
            listOf(ElementalTypes.GHOST, ElementalTypes.STEEL),
            PokemonStats(
                hp = 60.0,
                attack = 150.0,
                defense = 50.0,
                specialAttack = 150.0,
                specialDefense = 50.0,
                speed = 60.0
            )
        ),
        "meloettapirouette" to PokemonFormChange(
            listOf(ElementalTypes.NORMAL, ElementalTypes.FIGHTING),
            PokemonStats(
                hp = 100.0,
                attack = 128.0,
                defense = 90.0,
                specialAttack = 77.0,
                specialDefense = 77.0,
                speed = 128.0
            )
        ),
        "zaciancrowned" to PokemonFormChange( // ignored right now because rusted sword doesn't exist
            listOf(ElementalTypes.FAIRY, ElementalTypes.STEEL),
            PokemonStats(
                hp = 92.0,
                attack = 150.0,
                defense = 115.0,
                specialAttack = 80.0,
                specialDefense = 115.0,
                speed = 148.0
            )
        ),
        "zamazentacrowned" to PokemonFormChange( // same with rusted shield
            listOf(ElementalTypes.FIGHTING, ElementalTypes.STEEL),
            PokemonStats(
                hp = 92.0,
                attack = 130.0,
                defense = 145.0,
                specialAttack = 80.0,
                specialDefense = 145.0,
                speed = 128.0
            )
        ),
        "terapagosterastal" to PokemonFormChange(
            listOf(ElementalTypes.NORMAL),
            PokemonStats(
                hp = 95.0,
                attack = 95.0,
                defense = 110.0,
                specialAttack = 105.0,
                specialDefense = 110.0,
                speed = 85.0
            )
        ),
        "terapagosstellar" to PokemonFormChange( // ignored because no terastalize is not a thing right now
            listOf(ElementalTypes.NORMAL),
            PokemonStats(
                hp = 160.0,
                attack = 105.0,
                defense = 110.0,
                specialAttack = 130.0,
                specialDefense = 110.0,
                speed = 85.0
            )
        )
    )

    // --- Fonctions de calcul internes ---
    private fun calculateHPStat(base: Double, iv: Int, ev: Int, level: Int): Double {
        return ((2 * base + iv + ev / 4) * level / 100) + level + 10
    }

    private fun calculateOtherStat(base: Double, iv: Int, ev: Int, level: Int, nature: Nature, statType: Stat): Double {
        val finalStat = (((2 * base + iv + ev / 4) * level / 100) + 5)
        return natureMultiplied(nature, statType, finalStat)
    }

    private fun natureMultiplied(nature: Nature, statType: Stat, statVal: Double): Double {
        var finalStat = statVal
        if (natureBoosts[nature]?.increased == statType) finalStat*=1.1
        if (natureBoosts[nature]?.decreased == statType) finalStat/=1.1
        return finalStat
    }

    fun getPokemonRealStats(baseStats: PokemonStats, ivs: IVs, evs: EVs, nature: Nature, level: Int): PokemonStats {
        return PokemonStats(
            calculateHPStat(baseStats.hp, ivs.get(Stats.HP)!!, evs.get(Stats.HP)!!, level),
            calculateOtherStat(baseStats.attack, ivs.get(Stats.ATTACK)!!, evs.get(Stats.ATTACK)!!, level, nature, Stats.ATTACK),
            calculateOtherStat(baseStats.specialAttack, ivs.get(Stats.SPECIAL_ATTACK)!!, evs.get(Stats.SPECIAL_ATTACK)!!, level, nature, Stats.SPECIAL_ATTACK),
            calculateOtherStat(baseStats.defense, ivs.get(Stats.DEFENCE)!!, evs.get(Stats.DEFENCE)!!, level, nature, Stats.DEFENCE),
            calculateOtherStat(baseStats.specialDefense, ivs.get(Stats.SPECIAL_DEFENCE)!!, evs.get(Stats.SPECIAL_DEFENCE)!!, level, nature, Stats.SPECIAL_DEFENCE),
            calculateOtherStat(baseStats.speed, ivs.get(Stats.SPEED)!!, evs.get(Stats.SPEED)!!, level, nature, Stats.SPEED),
        )
    }

    fun getForm(formName: String): PokemonFormChange? {
        return statsMap[formName]
    }
}