package de.phlup.circuitchaos.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
public class GameItemsWrapper {

    private Set<GameItem> items;

}
