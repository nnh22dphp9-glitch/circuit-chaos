package de.phlup.circuitchaos.common.model;

import lombok.Data;

@Data
public class Registration {

    private String  gameName;
    private String  id;
    private String  url;
    private boolean watchOnly;
    private boolean pull;

}
