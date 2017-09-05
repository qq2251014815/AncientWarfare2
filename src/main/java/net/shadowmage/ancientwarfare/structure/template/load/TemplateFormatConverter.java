/*
 Copyright 2012-2013 John Cummens (aka Shadowmage, Shadowmage4513)
 This software is distributed under the terms of the GNU General Public License.
 Please see COPYING for precise license information.

 This file is part of Ancient Warfare.

 Ancient Warfare is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Ancient Warfare is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Ancient Warfare.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.shadowmage.ancientwarfare.structure.template.load;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.*;
import net.shadowmage.ancientwarfare.core.config.AWLog;
import net.shadowmage.ancientwarfare.core.util.StringTools;
import net.shadowmage.ancientwarfare.structure.api.TemplateRule;
import net.shadowmage.ancientwarfare.structure.api.TemplateRuleEntity;
import net.shadowmage.ancientwarfare.structure.block.BlockDataManager;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.build.validation.StructureValidationType;
import net.shadowmage.ancientwarfare.structure.template.plugin.default_plugins.block_rules.*;
import net.shadowmage.ancientwarfare.structure.template.save.TemplateExporter;

import java.io.File;
import java.util.*;


public class TemplateFormatConverter {
    /*
     * cached TE instances to use for writing out of basic NBT data into tag to use for
     * converted rule
     */
    private static TileEntityCommandBlock teCommand = new TileEntityCommandBlock();
    private static TileEntitySkull teSkull = new TileEntitySkull();
    private static TileEntityDropper teDropper = new TileEntityDropper();
    private static TileEntityDispenser teDispenser = new TileEntityDispenser();
    private static TileEntityFurnace teFurnace = new TileEntityFurnace();
    private static TileEntityHopper teHopper = new TileEntityHopper();
    private static TileEntityBrewingStand teBrewingStand = new TileEntityBrewingStand();
    private static TileEntityChest teChest = new TileEntityChest();

    private static HashSet<Block> specialHandledBlocks = new HashSet<>();//just a temp cache to keep track of what blocks to not register with blanket block rule

    static {
        specialHandledBlocks.add(Blocks.STANDING_SIGN);
        specialHandledBlocks.add(Blocks.WALL_SIGN);
        specialHandledBlocks.add(Blocks.IRON_DOOR);
        specialHandledBlocks.add(Blocks.WOODEN_DOOR);
        specialHandledBlocks.add(Blocks.COMMAND_BLOCK);
        specialHandledBlocks.add(Blocks.MOB_SPAWNER);//noop
        specialHandledBlocks.add(Blocks.FURNACE);
        specialHandledBlocks.add(Blocks.LIT_FURNACE);
        specialHandledBlocks.add(Blocks.SKULL);
        specialHandledBlocks.add(Blocks.BREWING_STAND);
        specialHandledBlocks.add(Blocks.CHEST);
        specialHandledBlocks.add(Blocks.DROPPER);
        specialHandledBlocks.add(Blocks.DISPENSER);
        specialHandledBlocks.add(Blocks.HOPPER);
    }

    int lineNumber = -1;

    public StructureTemplate convertOldTemplate(String fileName, List<String> templateLines) {
        lineNumber = -1;
        /*
         * parsed-out data, to be used to construct new template
         */
        List<TemplateRule> parsedRules = new ArrayList<>();
        List<TemplateRuleEntity> parsedEntityRules = new ArrayList<>();
        short[] templateData = null;
        int xSize = 0, ySize = 0, zSize = 0;
        int xOffset = 0, yOffset = 0, zOffset = 0;

        String name = fileName;
        if (name.length() >= 4) {
            name = name.substring(0, name.length() - 4);
        }
        Iterator<String> it = templateLines.iterator();
        List<String> groupedLines = new ArrayList<>();
        String line, lower;
        int parsedLayers = 0;
        int readSizeParams = 0;
        int highestRuleNumber = 0;
        while (it.hasNext() && (line = it.next()) != null) {
            lineNumber++;
            lower = line.toLowerCase(Locale.ENGLISH);
            if (lower.startsWith("xsize=")) {
                xSize = StringTools.safeParseInt("=", line);
                readSizeParams++;
                if (readSizeParams == 3) {
                    templateData = new short[xSize * ySize * zSize];
                }
            } else if (lower.startsWith("ysize=")) {
                ySize = StringTools.safeParseInt("=", line);
                readSizeParams++;
                if (readSizeParams == 3) {
                    templateData = new short[xSize * ySize * zSize];
                }
            } else if (lower.startsWith("zsize=")) {
                zSize = StringTools.safeParseInt("=", line);
                readSizeParams++;
                if (readSizeParams == 3) {
                    templateData = new short[xSize * ySize * zSize];
                }
            } else if (lower.startsWith("verticaloffset=")) {
                yOffset = (StringTools.safeParseInt("=", line));
            } else if (lower.startsWith("xoffset=")) {
                xOffset = StringTools.safeParseInt("=", line);
            } else if (lower.startsWith("zoffset")) {
                zOffset = StringTools.safeParseInt("=", line);
            } else if (lower.startsWith("rule:")) {
                parseTag("rule", it, groupedLines);
                TemplateRule rule = parseOldBlockRule(groupedLines);
                if (rule != null) {
                    if (rule.ruleNumber > highestRuleNumber) {
                        highestRuleNumber = rule.ruleNumber;
                    }
                    parsedRules.add(rule);
                }
                groupedLines.clear();
            } else if (lower.startsWith("layer:")) {
                parseTag("layer", it, groupedLines);
                parseLayer(groupedLines, templateData, parsedLayers, xSize, ySize, zSize);
                parsedLayers++;
                groupedLines.clear();
            } else if (lower.startsWith("entity:")) {
                parseTag("entity", it, groupedLines);
                //NO SUPPORT FOR CARYING ENTITIES OVER FROM OLD VERSION
                groupedLines.clear();
            } else if (lower.startsWith("npc:")) {
                parseTag("npc", it, groupedLines);
                //NO SUPPORT FOR CARYING NPCS OVER FROM OLD VERSION
                groupedLines.clear();
            } else if (lower.startsWith("gate:")) {
                parseTag("gate", it, groupedLines);
                //NO SUPPORT FOR CARYING NPCS OVER FROM OLD VERSION
                groupedLines.clear();
            } else if (lower.startsWith("vehicle:")) {
                parseTag("vehicle", it, groupedLines);
                //NO SUPPORT FOR CARYING VEHICLES OVER FROM OLD VERSION
                groupedLines.clear();
            } else if (lower.startsWith("civic:")) {
                parseTag("civic", it, groupedLines);
                //NO DIRECT SUPPORT FOR CARYING CIVICS OVER FROM OLD VERSION
                groupedLines.clear();
            }
        }

        TemplateRule[] rules = new TemplateRule[highestRuleNumber + 1];
        for (TemplateRule rule : parsedRules) {
            if (rule.ruleNumber >= 1 && rules[rule.ruleNumber] == null) {
                rules[rule.ruleNumber] = rule;
            } else {
                AWLog.logError("error parsing template rules, duplicate rule number detected for: " + rule.ruleNumber);
            }
        }

        TemplateRuleEntity entityRule;
        TemplateRuleEntity[] entityRules = new TemplateRuleEntity[parsedEntityRules.size()];
        for (int i = 0; i < parsedEntityRules.size(); i++) {
            entityRule = parsedEntityRules.get(i);
            entityRule.ruleNumber = i;
            entityRules[i] = entityRule;
        }

        zOffset = zSize - 1 - zOffset;//invert offset to normalize for the new top-left oriented template construction
        StructureTemplate template = new StructureTemplate(name, xSize, ySize, zSize, xOffset, yOffset, zOffset);
        template.setRuleArray(rules);
        template.setEntityRules(entityRules);
        template.setTemplateData(templateData);
        template.setValidationSettings(StructureValidationType.GROUND.getValidator().setDefaults(template));
        TemplateExporter.exportTo(template, new File(TemplateLoader.outputDirectory));
        return template;
    }

    private List<String> parseTag(String tag, Iterator<String> it, List<String> output) {
        String line;
        while (it.hasNext() && (line = it.next()) != null) {
            if (line.toLowerCase(Locale.ENGLISH).startsWith(":end" + tag)) {
                break;
            }
            output.add(line);
        }
        return output;
    }

    private TemplateRule parseOldBlockRule(List<String> lines) {
        int number = 0;
        int id = 0;
        int meta = 0;
        int buildPass = 0;
        for (String line : lines) {
            lineNumber++;
            if (line.toLowerCase(Locale.ENGLISH).startsWith("number=")) {
                number = StringTools.safeParseInt("=", line);
            } else if (line.toLowerCase(Locale.ENGLISH).startsWith("blocks=")) {
                String[] blockLines = StringTools.safeParseString("=", line).split(",");
                String[] blockData = blockLines[0].split("-");
                id = StringTools.safeParseInt(blockData[0]);
                meta = StringTools.safeParseInt(blockData[1]);
            } else if (line.toLowerCase(Locale.ENGLISH).startsWith("order=")) {
                buildPass = StringTools.safeParseInt("=", line);
            }
        }

        Block block = Block.getBlockById(id);
        if (block == null)//skip air block rule (0/null), or non-present blocks
        {
            return null;
        } else if (id > 256) {
            return parseModBlock(block, number, buildPass, meta);
        } else if (specialHandledBlocks.contains(block)) {
            return parseSpecialBlockRule(block, number, buildPass, meta, lines);
        } else {
            TemplateRuleVanillaBlocks rule = new TemplateRuleVanillaBlocks();
            rule.ruleNumber = number;
            rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
            rule.block = block;
            rule.meta = meta;
            rule.buildPass = buildPass;
            return rule;
        }
    }

    private TemplateRule parseSpecialBlockRule(Block block, int number, int buildPass, int meta, List<String> lines) {
        TemplateRuleVanillaBlocks rule = null;
        if (block == Blocks.WOODEN_DOOR || block == Blocks.IRON_DOOR) {
            rule = new TemplateRuleBlockDoors();
            rule.ruleNumber = number;
            rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
            rule.meta = meta;
            rule.buildPass = buildPass;
        }//vanilla door rule
        else if (block == Blocks.STANDING_SIGN || block == Blocks.WALL_SIGN) {
            rule = new TemplateRuleBlockSign();
            rule.ruleNumber = number;
            rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
            rule.meta = meta;
            rule.buildPass = buildPass;
            ((TemplateRuleBlockSign) rule).signContents = new String[]{"", "", "", ""};
        }//vanilla sign rule
        else if (block == Blocks.COMMAND_BLOCK) {
            NBTTagCompound tag = new NBTTagCompound();
            teCommand.writeToNBT(tag);

            rule = new TemplateRuleBlockLogic();
            rule.ruleNumber = number;
            rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
            rule.meta = meta;
            rule.buildPass = buildPass;
            ((TemplateRuleBlockLogic) rule).tag = tag;
        } else if (block == Blocks.MOB_SPAWNER) {
            //NOOP -- no previous spawner-block handling
        }//vanilla spawner rule
        else if (block == Blocks.LIT_FURNACE || block == Blocks.FURNACE) {
            NBTTagCompound tag = new NBTTagCompound();
            teFurnace.writeToNBT(tag);

            rule = new TemplateRuleBlockLogic();
            rule.ruleNumber = number;
            rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
            rule.meta = meta;
            rule.buildPass = buildPass;
            ((TemplateRuleBlockLogic) rule).tag = tag;
        } else if (block == Blocks.SKULL) {
            NBTTagCompound tag = new NBTTagCompound();
            teSkull.writeToNBT(tag);

            rule = new TemplateRuleBlockLogic();
            rule.ruleNumber = number;
            rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
            rule.meta = meta;
            rule.buildPass = buildPass;
            ((TemplateRuleBlockLogic) rule).tag = tag;
        } else if (block == Blocks.BREWING_STAND) {
            NBTTagCompound tag = new NBTTagCompound();
            teBrewingStand.writeToNBT(tag);

            rule = new TemplateRuleBlockLogic();
            rule.ruleNumber = number;
            rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
            rule.meta = meta;
            rule.buildPass = buildPass;
            ((TemplateRuleBlockLogic) rule).tag = tag;
        } else if (block == Blocks.CHEST) {
            NBTTagCompound tag = new NBTTagCompound();
            teChest.writeToNBT(tag);

            rule = new TemplateRuleBlockInventory();
            rule.ruleNumber = number;
            rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
            rule.meta = meta;
            rule.buildPass = buildPass;
            ((TemplateRuleBlockInventory) rule).tag = tag;
            ((TemplateRuleBlockInventory) rule).randomLootLevel = 1;
        }//vanilla chests
        else if (block == Blocks.DISPENSER) {
            NBTTagCompound tag = new NBTTagCompound();
            teDispenser.writeToNBT(tag);

            rule = new TemplateRuleBlockInventory();
            rule.ruleNumber = number;
            rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
            rule.meta = meta;
            rule.buildPass = buildPass;
            ((TemplateRuleBlockInventory) rule).tag = tag;
            ((TemplateRuleBlockInventory) rule).randomLootLevel = 0;
        } else if (block == Blocks.DROPPER) {
            NBTTagCompound tag = new NBTTagCompound();
            teDropper.writeToNBT(tag);

            rule = new TemplateRuleBlockInventory();
            rule.ruleNumber = number;
            rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
            rule.meta = meta;
            rule.buildPass = buildPass;
            ((TemplateRuleBlockInventory) rule).tag = tag;
            ((TemplateRuleBlockInventory) rule).randomLootLevel = 0;
        } else if (block == Blocks.HOPPER) {
            NBTTagCompound tag = new NBTTagCompound();
            teHopper.writeToNBT(tag);

            rule = new TemplateRuleBlockInventory();
            rule.ruleNumber = number;
            rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
            rule.meta = meta;
            rule.buildPass = buildPass;
            ((TemplateRuleBlockInventory) rule).tag = tag;
            ((TemplateRuleBlockInventory) rule).randomLootLevel = 0;
        }
        return rule;
    }

    private TemplateRule parseModBlock(Block block, int number, int buildPass, int meta) {
        TemplateRuleModBlocks rule = new TemplateRuleModBlocks();
        rule.ruleNumber = number;
        rule.blockName = BlockDataManager.INSTANCE.getNameForBlock(block);
        rule.meta = meta;
        return rule;
    }

    private void parseLayer(List<String> lines, short[] templateData, int yLayer, int xSize, int ySize, int zSize) {
        if (templateData == null) {
            throw new IllegalArgumentException("cannot fill data into null template data array");
        }
        int z = 0;
        for (String st : lines) {
            lineNumber++;
            if (st.startsWith("layer:") || st.startsWith(":endlayer")) {
                continue;
            }
            short[] data = StringTools.parseShortArray(st);
            for (int x = 0; x < xSize && x < data.length; x++) {
                templateData[StructureTemplate.getIndex(x, yLayer, z, xSize, ySize, zSize)] = data[x];
            }
            z++;
        }
    }


}
