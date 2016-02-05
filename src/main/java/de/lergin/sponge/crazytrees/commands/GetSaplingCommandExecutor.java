package de.lergin.sponge.crazytrees.commands;

import de.lergin.sponge.crazytrees.data.saplingData.CrazySaplingManipulatorBuilder;
import de.lergin.sponge.crazytrees.trees.CrazyTree;
import de.lergin.sponge.crazytrees.trees.CrazyTreeType;
import de.lergin.sponge.crazytrees.util.ConfigHelper;
import de.lergin.sponge.crazytrees.util.TranslationHelper;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.CatalogTypes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.Optional;

public class GetSaplingCommandExecutor implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player player;

        Optional<Player> target = args.getOne("player");
        if (target.isPresent()) {
            player = target.get();
        } else if (src instanceof Player) {
            player = (Player) src;
        }else{
            return CommandResult.empty();
        }


        //create the Data from  the arguments
        ItemStack sapling = ItemStack.of(ItemTypes.SAPLING, (int) args.getOne("amount").orElse(1));


        ConfigurationNode saplingConfig = ConfigHelper.getNode("sapling", "default");
        ConfigurationNode treeTypeConfig = ConfigHelper.getNode("defaultTree");

        CrazyTree.Builder crazyTreeBuilder =
                ((CrazyTreeType) args.getOne("treeType").orElse(
                        CrazyTreeType.valueOf(treeTypeConfig.getNode("treeType").getString("OAK"))
                )).getBuilder();

        crazyTreeBuilder.woodBlock(
                (BlockType) args.getOne("woodBlock").orElse(
                        Sponge.getRegistry().getType(
                                CatalogTypes.BLOCK_TYPE,
                                treeTypeConfig.getNode("woodBlock").getString("minecraft:log")
                        ).get()
                )
        );

        crazyTreeBuilder.leaveBlock(
                (BlockType) args.getOne("leaveBlock").orElse(
                        Sponge.getRegistry().getType(
                                CatalogTypes.BLOCK_TYPE,
                                treeTypeConfig.getNode("leaveBlock").getString("minecraft:leaves")
                        ).get()
                )
        );

        crazyTreeBuilder.treeHeight(
                (Integer) args.getOne("height").orElse(crazyTreeBuilder.getTreeHeightMax()),
                (Integer) args.getOne("height").orElse(crazyTreeBuilder.getTreeHeightMin())
        );

        crazyTreeBuilder.placeBlockUnderTree(false);
        crazyTreeBuilder.replaceBlocks(new ArrayList<>());
        crazyTreeBuilder.groundBlocks(new ArrayList<>());

        CrazyTree crazyTree = crazyTreeBuilder.build();

        sapling.offer(
                new CrazySaplingManipulatorBuilder().setTree(crazyTree).create()
        );

        //only for the good locking
        if(saplingConfig.getNode("treeTypeAsName").getBoolean(true)){
            sapling.offer(Keys.DISPLAY_NAME, TranslationHelper.p(
                    player,
                    "treeType." + crazyTree.getClass().getSimpleName()
            ));
        }

        if(saplingConfig.getNode("enchanted").getBoolean(true))
            sapling.offer(Keys.ITEM_ENCHANTMENTS, new ArrayList<>());


        ArrayList<Text> loreTexts = new ArrayList<>();
        if(saplingConfig.getNode("lore", "woodBlock").getBoolean(true)){
            loreTexts.add(TranslationHelper.p(
                    player,
                    "player.info.wood_block",
                    crazyTree.getWoodBlock().getType()
            ));
        }

        if(saplingConfig.getNode("lore", "leaveBlock").getBoolean(true)){
            loreTexts.add(TranslationHelper.p(
                    player,
                    "player.info.leave_block",
                    crazyTree.getLeaveBlock().getType()
            ));
        }

        if(saplingConfig.getNode("lore", "height").getBoolean(true))
            loreTexts.add(TranslationHelper.p(player, "player.info.height_block", crazyTree.getTreeHeightMax()));


        sapling.offer(Keys.ITEM_LORE, loreTexts);



        Optional<ItemStack> optionalInHandItem = player.getItemInHand();

        if(optionalInHandItem.isPresent()){
            Boolean isOffered = player.getInventory().offer(sapling).getRejectedItems().isEmpty();

            if(!isOffered){
                player.sendMessage(TranslationHelper.p(player, "player.warn.not_enough_space_in_inventory"));
            }
        }else{
            player.setItemInHand(sapling);
        }

        return CommandResult.success();
    }
}
