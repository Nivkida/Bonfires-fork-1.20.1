package wehavecookies56.bonfires.datagen;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import wehavecookies56.bonfires.Bonfires;
import wehavecookies56.bonfires.setup.BlockSetup;
import wehavecookies56.bonfires.setup.ItemSetup;

import java.util.function.Consumer;

public class Recipes extends RecipeProvider {

    DataGenerator generator;

    public Recipes(DataGenerator generator) {
        super(generator.getPackOutput());
        this.generator = generator;
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> recipeConsumer) {

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, BlockSetup.ash_block.get())
                .requires(ItemSetup.ash_pile.get(), 9)
                .group(Bonfires.modid)
                .unlockedBy("has_ash_pile", InventoryChangeTrigger.TriggerInstance.hasItems(ItemSetup.ash_pile.get()))
                .save(recipeConsumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BlockSetup.ash_bone_pile.get())
                .pattern("BBB")
                .pattern("AAA")
                .define('A', ItemSetup.ash_pile.get())
                .group(Bonfires.modid)
                .unlockedBy("has_ash_pile", InventoryChangeTrigger.TriggerInstance.hasItems(ItemSetup.ash_pile.get()))
                .save(recipeConsumer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemSetup.ash_pile.get(), 9)
                .requires(BlockSetup.ash_block.get())
                .group(Bonfires.modid)
                .unlockedBy("has_ash_block", InventoryChangeTrigger.TriggerInstance.hasItems(BlockSetup.ash_block.get()))
                .save(recipeConsumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemSetup.coiled_sword.get())
                .pattern("OLO")
                .pattern("FSF")
                .pattern("OAO")
                .define('O', Tags.Items.OBSIDIAN)
                .define('L', Items.LAVA_BUCKET)
                .define('F', Items.FIRE_CHARGE)
                .define('S', Items.DIAMOND_SWORD)
                .define('A', ItemSetup.ash_pile.get())
                .group(Bonfires.modid)
                .unlockedBy("has_ash_bone_pile", InventoryChangeTrigger.TriggerInstance.hasItems(BlockSetup.ash_bone_pile.get()))
                .save(recipeConsumer);

    }
}