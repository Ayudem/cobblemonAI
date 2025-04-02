package com.selfdot.cobblemontrainers
import com.cobblemon.mod.common.api.abilities.Ability
import com.cobblemon.mod.common.api.moves.categories.DamageCategory
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.pokemon.Pokemon
import java.util.*

/* List of all necessary informations to take a decision in battle */
// I made it pretty standard so it can be used more easily elsewhere

data class Field(
    val weather:String? = null,
    val terrain:String? = null,
    val trickRoom:Boolean
)

data class PokemonStats(
    val hp:Double,
    val attack:Double,
    val specialAttack:Double,
    val defense:Double,
    val specialDefense:Double,
    val speed:Double
)

data class PokemonStatBoosts(
    val accuracy:Double,
    val attack:Double,
    val specialAttack:Double,
    val defense:Double,
    val specialDefense:Double,
    val speed:Double
)

data class ActivePokemonMove(
    val name:String,
    val power:Double,
    var type:ElementalType,
    val damageCategory:DamageCategory,
    val accuracy:Double,
    val currentPP:Int,
    val priority:Int,
    var disabled:Boolean
)

data class ActivePokemon(
    val name: String,
    val weight:Float,
    val level:Int,
    val stats:PokemonStats,
    val statBoosts:PokemonStatBoosts,
    val types:Iterable<ElementalType>,
    var currentHp:Double,
    val ability:String,
    val item:String,
    val status:String?,
    val volatileStatus:List<String>,
    val moveSet:List<ActivePokemonMove>,
    val trapped:Boolean,
    val uuid: UUID
)

data class Side(
    val owner:SideOwner,
    val hazards:List<String>,
    val screen:List<String>,
    val tailwind:Boolean,
    val team:List<ActivePokemon>,
    val pokemon:ActivePokemon
)

data class BattleState(
    val field:Field,
    val playerSide:Side,
    val npcSide:Side
)

data class DamageHeal(
    var damage: Double,
    var heal: Double
)
data class MoveValue(
    val move: ActivePokemonMove,
    val value: DamageHeal
)

data class Battle1v1State(
    val turnsToKillPlayer:Int,
    val turnsToKillNpc:Int,
    val npcWins:Boolean,
    val npcIsQuicker: Boolean,
    val playerMostProbableMove:ActivePokemonMove,
    val npcMovesInfos:BattleMovesInfos
)

data class BattleMovesInfos(
    val killerMoves:List<ActivePokemonMove>,
    val damagingMoves:List<ActivePokemonMove>,
    val notDamagingMoves:List<ActivePokemonMove>,
    val usedMove:MoveValue
)

data class ActorCurrentHp(
    var pokemon:Double,
    var substitute: Double,
    var disguise: Double,
    val isNpc: Boolean
)

data class PreviousMoves(
    var player: String?,
    var npc: String?
)

data class BattleTracker(
    val pokemons:MutableMap<UUID, PokemonTracker>,
    var previousPlayerPokemon: UUID?,
    var previousNpcPokemon: UUID?,
    var deathNumber: DeathNumber,
    val previousMoves: PreviousMoves,
    var npcForcedSwitch: UUID?,
)

data class PokemonTracker(
    var item: String?,
    var ability: String?,
    var transform: Transform?,
    var disguiseBroken: Boolean,
    var isAHero: Boolean,
    var takenHit: Int,
)

data class Transform(
    val pokemon: Pokemon,
    val statBoosts: PokemonStatBoosts
)

data class DeathNumber(
    var npc: Int,
    var player: Int
)

data class Switch(
    var uuid: UUID?,
    var move: ActivePokemonMove?,
    var isUrgent: Boolean
)

enum class ResetProperty {
    ITEM,
    ABILITY,
    TRANSFORM,
    DISGUISEBROKEN,
    TAKENHIT
}

enum class SideOwner {
    PLAYER,
    NPC
}