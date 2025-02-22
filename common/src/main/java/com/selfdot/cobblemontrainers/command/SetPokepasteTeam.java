package com.selfdot.cobblemontrainers.command;

import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.*;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SetPokepasteTeam extends TrainerCommand {
    @Override
    protected int runSubCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String urlString = StringArgumentType.getString(context, "url");
        boolean ignoreErrors = context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals("ignoreErrors"))
                && BoolArgumentType.getBool(context, "ignoreErrors");
        if(urlString.startsWith("https://pokepast.es/") && !urlString.endsWith("/raw")) {
            urlString += "/raw";
        }
        CompletableFuture<String> fileContentFuture = AsyncFileDownloader.downloadFileAsString(urlString);
        fileContentFuture.thenAccept((content) -> {
            List<Pokemon> team = PokePasteParser.parse(content, ignoreErrors);
            trainer.clearTeam();
            for(Pokemon pokemon : team) {
                trainer.addPokemon(pokemon);
            }
            context.getSource().sendMessage(Text.literal("Pokepaste loaded"));
        }).exceptionally(e -> {
            context.getSource().sendMessage(Text.literal("Could not import paste: " + e.getMessage()));
            e.printStackTrace();
            return null;
        });
        return SINGLE_SUCCESS;
    }
}

class PokePasteParser {
    public static List<Pokemon> parse(String paste, boolean ignoreErrors) {
        List<Pokemon> team = new ArrayList<>();
        Pokemon current = null;

        String[] lines = paste.split("\n");
        for (String s : lines) {
            try {
                String line = s.trim();
                if (line.isEmpty()) {
                    current = null;
                    continue;
                }

                if (current == null) {
                    // this line must contain species+form+item
                    String[] firstLineSplit = line.split(" @ ");
                    String fullSpeciesName = firstLineSplit[0].trim().toLowerCase().replaceAll(" ", "");
                    Gender gender = null;
                    if (fullSpeciesName.contains("(m)")) {
                        fullSpeciesName = fullSpeciesName.replaceAll("\\(m\\)", "").trim();
                        gender = Gender.MALE;
                    } else if (fullSpeciesName.contains("(f)")) {
                        fullSpeciesName = fullSpeciesName.replaceAll("\\(f\\)", "").trim();
                        gender = Gender.FEMALE;
                    }
                    String[] nameSplit = fullSpeciesName.split("-");
                    String speciesName = nameSplit[0];
                    String formName = nameSplit.length > 1 ? nameSplit[1] : "";
                    Species species = PokemonSpecies.INSTANCE.getByName(speciesName);
                    if (species == null) {
                        throw new RuntimeException("Unknown Species: " + speciesName);
                    }
                    current = new Pokemon();
                    team.add(current);
                    current.setSpecies(species);
                    if (gender != null) {
                        current.setGender(gender);
                    }
                    if (!formName.isEmpty()) {
                        List<FormData> allForms = species.getForms();
                        FormData found = null;
                        for (FormData form : allForms) {
                            if (form.getName().toLowerCase().equals(formName)) {
                                found = form;
                                break;
                            }
                        }
                        if (found != null) {
                            current.setAspects(Set.of(found.getAspects().toArray(new String[0])));
                        } else {
                            if (ignoreErrors) {
                                System.out.println("Unknown Form: " + formName);
                            }
                            else {
                                throw new RuntimeException("Unknown Form: " + formName);
                            }
                        }
                    }

                    current.setLevel(100);
                    current.getMoveSet().clear();
                    current.setIV(Stats.HP, 31);
                    current.setIV(Stats.ATTACK, 31);
                    current.setIV(Stats.SPECIAL_ATTACK, 31);
                    current.setIV(Stats.DEFENCE, 31);
                    current.setIV(Stats.SPECIAL_DEFENCE, 31);
                    current.setIV(Stats.SPEED, 31);

                    if (firstLineSplit.length > 1) {
                        // has held item
                        String heldItemName = firstLineSplit[1].trim().toLowerCase().replaceAll(" ", "_");
                        Item heldItem = Registries.ITEM.get(Identifier.tryParse("cobblemon:" + heldItemName));
                        if (heldItem == Items.AIR) {
                            if (ignoreErrors) {
                                System.out.println("Unknown Held Item: " + heldItemName);
                            } else {
                                throw new RuntimeException("Unknown Held Item: " + heldItemName);
                            }
                        }
                        current.swapHeldItem(new ItemStack(heldItem), false);
                    }
                } else if (line.startsWith("Level: ")) {
                    int level = Integer.parseInt(line.split(" ")[1]);
                    current.setLevel(level);
                } else if (line.endsWith(" Nature")) {
                    String natureName = line.split(" ")[0].toLowerCase();
                    Nature nature = Natures.INSTANCE.getNature(natureName);
                    if (nature == null) {
                        throw new RuntimeException("Unknown Nature: " + natureName);
                    }
                    current.setNature(nature);
                } else if (line.startsWith("Ability: ")) {
                    String abilityName = line.replaceFirst("Ability: ", "").toLowerCase().replaceAll(" ", "");
                    AbilityTemplate abilityTemplate = Abilities.INSTANCE.get(abilityName);
                    if (abilityTemplate == null) {
                        throw new RuntimeException("Unknown Ability: " + abilityName);
                    }
                    current.setAbility$common(abilityTemplate.create(true));
                } else if (line.startsWith("- ")) {
                    String moveName = line
                            .replaceFirst("- ", "")
                            .toLowerCase()
                            .replaceAll(" ", "")
                            .replaceAll("-", "");
                    MoveTemplate moveTemplate = Moves.INSTANCE.getByName(moveName);
                    if (moveTemplate == null) {
                        throw new RuntimeException("Unknown Move: " + moveName);
                    }
                    current.getMoveSet().add(moveTemplate.create());
                } else if (line.startsWith("EVs: ")) {
                    List<StatValue> stats = parseStats(line.replaceAll("EVs: ", ""));
                    for (StatValue stat : stats) {
                        current.setEV(stat.stat, stat.value);
                    }
                } else if (line.startsWith("IVs: ")) {
                    List<StatValue> stats = parseStats(line.replaceAll("IVs: ", ""));
                    for (StatValue stat : stats) {
                        current.setIV(stat.stat, stat.value);
                    }
                }
            } catch(Exception e) {
                if (ignoreErrors) {
                    System.out.println(e.getMessage());
                } else {
                    throw e;
                }
            }
        }

        return team;
    }

    public static List<StatValue> parseStats(String str) {
        List<StatValue> stats = new ArrayList<>();
        String[] split = str.split(" / ");
        for (String s : split) {
            String[] cur = s.split(" ");
            String name = cur[1];
            int val = Integer.parseInt(cur[0]);
            switch (name) {
                case "HP" -> stats.add(new StatValue(Stats.HP, val));
                case "Atk" -> stats.add(new StatValue(Stats.ATTACK, val));
                case "SpA" -> stats.add(new StatValue(Stats.SPECIAL_ATTACK, val));
                case "Def" -> stats.add(new StatValue(Stats.DEFENCE, val));
                case "SpD" -> stats.add(new StatValue(Stats.SPECIAL_DEFENCE, val));
                case "Spe" -> stats.add(new StatValue(Stats.SPEED, val));
                default -> throw new RuntimeException("Unknown Stat: " + name);
            }
        }
        return stats;
    }
}

class StatValue {
    Stat stat;
    int value;

    public StatValue(Stat stat, int value) {
        this.stat = stat;
        this.value = value;
    }
}

 class AsyncFileDownloader {
    public static CompletableFuture<String> downloadFileAsString(String urlString) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder content = new StringBuilder();

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Set HTTP request method
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                // Read the file content using InputStreamReader
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");  // Append the line to the content
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }

            return content.toString();
        });
    }
}