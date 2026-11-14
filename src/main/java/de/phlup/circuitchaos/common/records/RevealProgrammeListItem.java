package de.phlup.circuitchaos.common.records;

import de.phlup.circuitchaos.common.enums.ModuleType;

public record RevealProgrammeListItem(String name, String imageIconPath, Programme programme, ModuleType weapon,
                                      int damage) {

}
