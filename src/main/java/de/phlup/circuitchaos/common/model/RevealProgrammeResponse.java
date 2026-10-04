package de.phlup.circuitchaos.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RevealProgrammeResponse {

    private int                           phase;
    private List<RevealProgrammeListItem> programme = new ArrayList<>();

}
