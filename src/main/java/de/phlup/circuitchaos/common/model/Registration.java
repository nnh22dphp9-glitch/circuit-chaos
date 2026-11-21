package de.phlup.circuitchaos.common.model;

import lombok.Data;

import java.util.UUID;

@Data
public class Registration {

    private String  gameName;
    private UUID    id;
    private String  url;
    private boolean watchOnly;
    private boolean pull;

}
