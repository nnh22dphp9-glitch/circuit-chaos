package de.phlup.circuitchaos.common.model;

public record Range(int minX, int minY, int maxX, int maxY) {

    public Range wide() {
        return new Range(minX - 1, minY - 1, maxX + 1, maxY + 1);
    }

    public Range wideRangeToPosition(Position position) {
        Range range = this;
        if (position.x() == range.minX()) {
            range = new Range(range.minX() - 1, range.minY(), range.maxX(), range.maxY());
        }
        if (position.y() == range.minY()) {
            range = new Range(range.minX(), range.minY() - 1, range.maxX(), range.maxY());
        }
        if (position.x() == range.maxX()) {
            range = new Range(range.minX(), range.minY(), range.maxX() + 1, range.maxY());
        }
        if (position.y() == range.maxY()) {
            range = new Range(range.minX(), range.minY(), range.maxX(), range.maxY() + 1);
        }
        return range;
    }

}
