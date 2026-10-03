package de.phlup.circuitchaos.model;

public record Range(int minX, int minY, int maxX, int maxY) {

    public Range wide() {
        return new Range(minX - 1, minY - 1, maxX + 1, maxY + 1);
    }

}
