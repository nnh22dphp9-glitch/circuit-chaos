package de.phlup.circuitchaos.common.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "circuit-chaos.client")
public class ClientSettings {

    private String  ownUrl;
    private String  serverUrl;
    private boolean allowOthersToStartAGame;
    private boolean sound;
    private String  sslBundle;

    private int    defaultBoardSizeX;
    private int    defaultBoardSizeY;
    private String defaultTheme;
    private float  defaultZoom;

    private List<String> defaultModules;

    private List<String> boardDirectories;

    private HashMap<String, Theme> themes;

    public Collection<Theme> getThemeCollection() {
        return themes.values();
    }

    @Data
    public static class Theme {
        private String  name;
        private String  path;
        private int     openFloorVariants;
        private int     waterVariants;
        private int     explosionVariants;
        private int     glueVariants;
        private int     oilVariants;
        private int     pusherAnimationSteps;
        private int     repulsorAnimationSteps;
        private double  zoomFactor;
        private boolean portalAnimated;
        private int     imageSize;
    }

}
