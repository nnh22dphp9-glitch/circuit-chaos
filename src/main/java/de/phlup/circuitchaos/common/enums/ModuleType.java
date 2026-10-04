package de.phlup.circuitchaos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public enum ModuleType {

    BRIDGE_PROJECTOR("Bridge Projector", "bridge-projector", false, false, 3),
    GLUE_DISPENSER("Glue Dispenser", "glue-dispenser", false, false, 2),
    HOVERCRAFT("Hovercraft", "hovercraft", true, false),
    MECHANICAL_ARM("Mechanical Arm", "mechanical-arm", true, false),
    MINE_LAYER("Mine Layer", "mine-layer", false, false, 2),
    MOBILE_TELEPORTER("Mobile Teleporter", "mobile-teleporter", false, false, 1),
    MOBILE_RANDOMIZER("Mobile Randomizer", "mobile-randomizer", false, false, 1),
    OIL_DISPENSER("Oil Dispenser", "oil-dispenser", false, false, 2),
    PROXIMITY_MINE_LAYER("Proximity Mine Layer", "proximity-mine-layer", false, false, 3),

    MAIN_LASER("Main Laser", "main-laser", true, true),
    DOUBLE_BARREL_LASER("Double Barrel Laser", "double-barrel-laser", false, true),
    HIGH_POWER_LASER("High Power Laser", "high-power-laser", false, true),
    RAMMING_ARMOR("Ramming Armor", "ramming-armor", false, true),
    REAR_LASER("Rear Laser", "rear-laser", false, true),
    EXCHANGE_BEAM("Exchange Beam", "exchange-beam", false, true),
    PRESSURE_BEAM("Pressure Beam", "pressure-beam", false, true),
    SPIN_LEFT_BEAM("Spin Left Beam", "spin-left-beam", false, true),
    SPIN_RIGHT_BEAM("Spin Right Beam", "spin-right-beam", false, true),
    TRACTOR_BEAM("Tractor Beam", "tractor-beam", false, true),

    EXTRA_PROGRAM("Extra Program", "extra-program", true, false, 1),
    LAYERED_ARMOR("Layered Armor", "layered-armor", true, false, 4),
    OVERDRIVE("Overdrive", "overdrive", true, false),
    POWER_DOWN_SHIELD("Power Down Shield", "power-down-shield", true, false),
    REVERSE_DRIVE("Reverse Drive", "reverse-drive", true, false),
    SUPERIOR_ARCHIVE_COPY("Superior Archive Copy", "superior-archive-copy", true, false),

    BRAKES("Brakes", "brakes", false, false),
    GYROSCOPIC_STABILIZER("Gyroscopic Stabilizer", "gyroscopic-stabilizer", true, false),
    SHIELD("Shield", "shield", true, false),
    ;

    private final String  moduleName;
    private final String  picture;
    private final boolean defaultActiveInPhase;
    private final boolean weapon;
    private       int     ammunition;

}
