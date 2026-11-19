# Circuit Chaos – Game Manual (Draft Rules)

> This document brings together the game rules and gameplay conventions established so far in a form that can later serve as a player-facing manual. It describes **rules and gameplay, not the technical implementation**.
>
> Wherever a rule has not yet been conclusively clarified or fully checked, this is explicitly noted in the **Open Issues** section. These points should be decided before the manual is published as a definitive rulebook.

## 1. Game Concept and Objective

In **Circuit Chaos**, players program their robots using movement cards. The robots then execute their programs step by step. During this process, they must take the course, other robots, and the hazards built into the course into account.

Depending on the game mode, players win by reaching the finish or checkpoints, or by eliminating all opposing robots.

### Game Objectives

- **Finish:** Checkpoints must be reached in the specified order. The first player to reach the finish wins.
- **Kill All:** The objective is to eliminate all other robots. In this mode, all checkpoints become starting positions, and the robots are distributed evenly among them.

## 2. Programming and Execution

### Program Registers and Cards

A robot has five program registers, one for each phase. All five registers must be filled. Players may distribute their available program steps freely among these five phases.

Known cards:

- **Move 1, Move 2, Move 3, Move 4:** Move forward by the specified number of spaces.
- **Back-Up / Back-Up 2:** Move backward by one or two spaces.
- **Rotate Left / Rotate Right:** Turn 90 degrees to the left or right.
- **U-Turn:** Turn 180 degrees.

The number of program steps allocated is generally **nine minus the damage taken**, plus the value of the **Extra Program** module, if the robot has it. The Extra Program module starts with an *amount* of 1. Each additional Extra Program module the robot randomly receives at a Pit Stop increases this value by 1.

Program steps that are not assigned to one of the five registers are lost at the end of the round. Only blocked program steps are reused in the next round.

Programs are executed according to their card priorities; the highest priority is executed first. Movement can be affected by obstacles, other robots, or course effects.

### Blocked Program Steps and Destruction

If a robot has more than four damage points, program registers are blocked from back to front: Phase 5 first, then Phase 4, and so on. A blocked program step remains in its register and cannot be replaced with another card. Blocked program steps are reused, so the program remains complete even if the robot is allocated only four or fewer new program steps.

The Extra Program module does not change either the damage threshold for blocking registers or the damage threshold for destruction.

At **ten or more damage points**, the robot is destroyed. The rules for its return depend on the game mode: In Finish mode, it reappears at its Archive Position at the start of the next turn, normally with two damage points. With **Superior Archive Copy**, it returns without damage. In Kill All mode, it is eliminated permanently.

### Power-Down and Program Cards

A Power-Down suspends the robot for the following round. During that round, it can reduce its damage and then receive nine or more program steps again. Apart from this, Power-Down does not have any direct effect on the number of program steps allocated.

### Secrecy and Simultaneous Programming

All players program simultaneously using the program cards allocated to them and their available modules. They do not know the other players' unexecuted program steps or modules unless those modules have been used or have produced a visible effect. They can draw conclusions from previous rounds and visible effects. Some modules, such as **Layered Armor**, can be active without providing any visible indication.

### Sequence of a Round

After all players have programmed, the programs are executed over **five phases**. The following steps are performed in the specified order during each phase:

1. **Reveal program step and determine order:** The program step for the phase is revealed. Robots are sorted by card priority, with the highest priority first. Randomizers can replace a robot's current program step with another randomly selected program step. The three Turn cards no longer have a priority; they are executed at the end, in random order among themselves.
2. **Open trapdoors:** Trapdoors that are active during this phase open. Robots may already fall into the abyss and be destroyed at this point.
3. **Execute program steps:** Robots execute their respective program steps in the established priority order. At the beginning of its program step, a robot may use activated modules to deploy devices, such as mines or bridges. Newly deployed mines are initially inactive. During this step, the robot moves according to its program step.
4. **Move express conveyor belts:** Express conveyor belts move robots and other objects on them; depending on the conveyor belt, their orientation may also change.
5. **Move conveyor belts:** Both normal and express conveyor belts now move and transport the robots and objects on them; their orientation may also change.
6. **Pushers:** Active pushers may push robots one space in the specified direction.
7. **Gears:** Gears rotate robots and other objects standing on them.
8. **Fire beams and lasers:** First, the robots' weapons fire, followed by the course-mounted beams and lasers. The order in which shooters fire follows the same priority order as the program steps.
9. **Checkpoints and mines:** The game checks whether robots have reached checkpoints or the finish. Mines are activated during this step and explode immediately if a robot is standing on one. Proximity mines can also be triggered when a robot is standing on an adjacent space. When a robot reaches the finish, it is declared the winner. If multiple robots reach the finish simultaneously, a tie message is displayed.
10. **Check for a Kill All victory:** In Kill All mode, the winner is recalculated according to the Kill All rules. This can override the previously determined finish winner or result in no winner being declared.

### End of the Round

After the five program phases, the end-of-round effects are processed. Destroyed robots are returned to the course, and damage and Power-Down are handled according to their respective rules. Players then receive new program cards and the next round begins.

The five program phases therefore involve more than just robot movement: During each phase, trapdoors, conveyor belts, pushers, gears, weapons, and mines also take effect in a fixed order.

## 3. Movement, Walls, and Elevation Changes

### Normal Movement

A robot moves in the direction it is facing. Back-Up cards move it in the opposite direction without requiring it to turn around. Turn cards change its orientation.

Walls and other obstacles can prevent movement. Robots can also block or push one another.

### Elevation Changes, Abysses, and Ramps

- A space with a **LEDGE** edge is on the lower side of an elevation change.
- Moving from a lower space to a higher space is not possible without a suitable ramp.
- Moving downhill to a lower space is generally possible and causes **one damage point**.
- Crossing via a ramp does not cause additional damage due to the elevation change.
- The ramp directions **RAMP_UP** and **RAMP_DOWN** differ in their appearance or orientation; crossing a suitable ramp does not cost additional movement and does not cause elevation-related damage.
- Even if a LEDGE edge is overlaid by another wall type, the elevation difference remains relevant to movement.
- Falling into an abyss or through an active trapdoor is fatal.

### One-Way Walls and Repulsor Fields

One-way walls marked green and red are created as matching pairs. The green side allows movement towards the red side; the red side blocks movement in the opposite direction.

**Repulsor fields** are active depending on the phase. During active phases, they repel any movement that would cross the field. They can be crossed during inactive phases.

### Flying and Hovercraft

The Hovercraft module enables flying movement while executing a movement card (Move or Back-Up). During the entire movement – including multi-step cards such as Move-2 – walls and elevation differences do not block flying movement in the same way as normal movement. Once the movement card has been completed, the robot is considered to have landed. It is considered landed during the subsequent events of the same phase, such as the movement of other robots or conveyor belts. Landing on a lower level does not cause damage. Landing in an abyss or on an active trapdoor is fatal.

## 4. Pushing and Ramming Robots

Robots can push other robots. A push can cover multiple spaces and is limited by obstacles and the boundaries of the course.

### Ramming Armor

Ramming Armor causes one damage point to the robot hit on the first collision involving its carrier during a phase. This damage is inflicted even if the hit robot cannot be moved because of an obstacle. In a chain of pushed robots, Ramming Armor does not automatically damage every robot in the chain.

In complex collisions, movements are resolved sequentially; several robots may become part of a chain push.

## 5. Course Elements and Hazards

### Conveyor Belts

Conveyor belts move robots standing on them in the direction of the arrow:

- Normal conveyor belts move a robot by one space.
- Express conveyor belts move it by two spaces. They are processed in two steps: First, only robots on express conveyor belts move; afterwards, express and normal conveyor belts move together. This order matters when movement is blocked by other robots.
- A robot can be moved from one conveyor belt onto another, which may then affect it as well.

There are also three variants that affect a robot's orientation when it moves from one conveyor belt to another:

- **CW (clockwise):** Turn clockwise.
- **CCW (counter-clockwise):** Turn counter-clockwise.
- **CW/CCW:** The direction of rotation depends on the direction from which the robot enters the belt.

If the directions of both conveyor belts match, the robot is not rotated.

Elevation changes, walls, and ramps affect whether transportation is possible. Moving down without a ramp causes one damage point; an ascent that is not permitted is blocked.

### Gears

Gears rotate a robot standing on them by **90°**. Depending on the type of gear, the rotation is clockwise (CW) or counter-clockwise (CCW).

The **Gyroscopic Stabilizer** prevents the robot from being rotated by gears or rotating conveyor belts.

### Pushers

Pushers are active depending on the phase. When active, they extend during the PUSHERS_PUSH step and retract immediately afterwards. They push the robot on their space one space forward, provided the destination space is not blocked. A switch can toggle the active phases of a pusher.

### Oil

Oil may already be present on the course or may be released by a robot using an Oil Dispenser. Unlike glue, oil is not consumed: Once placed on the course, it remains there permanently – unless it is on a conveyor belt and is moved onto an Abyss space along with the belt.

When a robot enters an oil-covered space, it slides forward in its direction of movement until it reaches a space without oil or its movement is blocked by another effect. Sliding does not consume additional program steps. Example: If a robot executes a Move-2 step and enters an oil-covered space on the first step, it can move a total of three spaces: one space due to the Move step and two additional spaces due to sliding.

If a robot actually comes to a stop on an oil-covered space, it needs an additional movement step to get going; its wheels initially spin. A Hovercraft can move without this starting step.

Multiple oil objects on the same space do not increase the sliding effect.

### Water

Water reduces a robot's movement by one movement step. In water, a robot therefore remains stationary when using Move-1 or Back-Up-1. Hovercraft are not affected by this effect.

Conveyor belts work normally underwater and are not restricted by the slowdown.

Water and **BRAKES** work together: BRAKES reduces Move-2 to one movement step, and water reduces that remaining step to zero.

### Trapdoors and Abysses

An **Abyss** is a space without a floor. A robot that falls into it crashes and is destroyed. Hovercraft can fly over abysses; teleporters can also teleport a robot across an abyss or into one.

A **trapdoor** opens and closes depending on the phase:

- During open phases, the space behaves like an Abyss.
- During closed phases, it behaves like OPEN_FLOOR.

The phases during which a trapdoor is open are displayed in the field's tooltip and are therefore visible to all players.

Mines, glue, and oil can also fall into an abyss if they are pushed there by a conveyor belt. They are then removed from the course. A mine does not explode when this happens.

A switch can toggle the active phases of a trapdoor. The general operation of switches is described in the following section.

### Switches

A switch is connected to a target space. When a non-flying robot moves onto an armed switch space, the switch is triggered: All five entries in the target space's `activeInPhase` array are inverted. Every phase that was previously active becomes inactive, and every previously inactive phase becomes active.

This can be used to toggle the phase-dependent properties of a **trapdoor**, a **pusher**, or a **repulsor field**. After being triggered, a switch is deactivated. Once its space is clear during a subsequent movement event, it is armed again.

### Teleporters

- A teleporter is activated when a robot is standing on it and then executes a movement program step.
- Stepping onto the teleporter space does not trigger teleportation. A teleporter is also not activated by movement caused by another effect, for example when a conveyor belt moves the robot over it.
- The teleporter moves the robot along its current orientation. The distance depends on the original direction of movement or the movement command.
- Teleportation only takes place if the destination space is free of robots and objects. A mine on the destination space therefore blocks teleportation; it is not triggered.
- Teleporting into a space without a floor, or into an abyss, is allowed. A robot can therefore deliberately be sent into an abyss.
- Mobile teleporters may occur during the game. It cannot therefore be assumed that a course always has just one fixed teleporter configuration.

### Portals

All portals work according to the same principle. Their colors are used for visual distinction, particularly in the editor: A crosshair in the same color marks the destination portal or destination space. The destination is not visible during the actual game. Players can therefore only know it from previous games or look it up in the editor; the computer AI knows the destination.

Portals can point to any destination. The destination space does not itself have to contain a portal. Multiple portals of the same color can also be created, for example several portal pairs. There is no restriction to a single pair of the same color as in the board game; course designers can define portal connections freely.

When a robot enters a portal space, it is immediately moved to the destination, provided that no other robot is standing there. If the destination space is blocked by a robot, no teleportation takes place. A robot that is already standing on a portal space, or lands on one, is not teleported either. A portal transfer does not immediately trigger another portal transfer, even if the destination space itself contains a portal. The portal at the destination is only activated after the robot has left that space and enters a portal again during a later movement step. This prevents portal chains and direct back-and-forth teleportation between two portals.

The destination portal or space is considered independently of any hazards there. For example, a course designer can make a portal point to an abyss. The robot is moved there and then falls into the abyss.

### Mines and Proximity Mines

**Mine Layer** and **Proximity Mine Layer** deploy mines on the space occupied by the robot. Both types of mine cause the same damage and have the same damage radius. In addition, a Proximity Mine can be triggered by a robot on an adjacent space.

Damage decreases with distance from the center of the explosion:

|   |   |       |   |   |
|--:|--:|------:|--:|--:|
| 0 | 0 |     1 | 0 | 0 |
| 0 | 1 |     2 | 1 | 0 |
| 1 | 2 | **4** | 2 | 1 |
| 0 | 1 |     2 | 1 | 0 |
| 0 | 0 |     1 | 0 | 0 |

A mine causes 4 damage points on its own space, 2 on orthogonally adjacent spaces, and 1 on diagonally adjacent spaces. No damage is caused outside this 5×5 area.

A mine is triggered when a robot steps onto its space or lands on it. A Proximity Mine is also triggered when a robot is standing on an immediately adjacent space. Newly deployed mines are initially **inactive** and are only activated after the ROBOTS_MOVE step. If a robot merely turns while standing on a mine during its program step, it can trigger the mine itself afterwards.

Mines can also hit the robot that deployed them. If a robot is pushed onto a mine by movement, a pusher, or another effect, or lands on one, it can trigger it. If several mines explode simultaneously, the damage they cause is added together.

Walls block Proximity Mines from being triggered if they are between the mine and the robot. Mines do not trigger chain reactions through the damage caused by another mine explosion.

### Glue

Glue traps a robot and initially has **3 glue points**. While the robot is stuck, each intended movement step is used instead to remove one glue point. The robot must therefore spend at least three movement steps to free itself.

It does not matter whether the steps are taken in one turn (e.g. with Move-3) or over several program steps. Moving backwards also removes glue points. It also makes no difference whether the glue was placed on the course by a Glue Dispenser or was already part of the course.

A Hovercraft can simply fly away from the glue; in this case, the glue has no effect.

Movement caused by other effects, such as pushers or conveyor belts, also removes glue points. Newly deployed glue is initially inactive and is only activated during the TOUCH_CHECKPOINTS step.

### Randomizers

If a robot is standing on a Randomizer, its next program step is replaced with another randomly selected program step.

### Lasers and Beams

Lasers and beams generally fire in the direction the robot is facing. The exception is the **Rear Laser**, which fires backwards. Course-mounted lasers and beams fire in the direction in which they are oriented on the course.

For a weapon mounted on a robot, the first point hit is the next space in front of the robot; for a course-mounted weapon, it is the space on which the weapon itself is mounted. The shot then continues to the edge of the course.

A shot is stopped by a robot it hits (exception: **High Power Laser**). The current code check also shows that shots are blocked if there is a SOLID wall, ONE_WAY_RED wall, a repulsor, a LEDGE, or a RAMP_UP on the shooter's side, or a SOLID wall or repulsor on the target side. A ONE_WAY_GREEN wall on the shooter's side does not block the shot. The High Power Laser has special handling and can continue the beam past the first target to a second target, even across an obstacle. Elevation differences are deliberately ignored when shooting: A LEDGE does not stop a shot. However, a robot on a different elevation can still be hit and stop the shot.

The weapons have the following effects:

| Weapon | Effect |
|---|---|
| **Main Laser** / **Single Course-Mounted Laser** | Causes 1 damage point on a hit. |
| **Double Barrel Laser** / **Double Course-Mounted Laser** | Causes 2 damage points on a hit. |
| **Triple Course-Mounted Laser** | Causes 3 damage points on a hit. |
| **High Power Laser** | Causes 1 damage point to the first target hit. The shot passes through that target and can hit a second target, which also receives 1 damage point. |
| **Rear Laser** | Fires backwards and causes 1 damage point on a hit, just like the Main Laser. |
| **Exchange Beam** | Swaps the firing robot's position with the target's position. Causes no damage. |
| **Pressure Beam** / **Course-Mounted Pressure Beam** | Pushes the target one space further. Causes no damage. |
| **Tractor Beam** / **Course-Mounted Tractor Beam** | Pulls the target one space towards the shooter. Causes no damage. |
| **Spin Left Beam** / **Course-Mounted Spin-Left Beam** | Rotates the target 90 degrees counter-clockwise. Causes no damage. |
| **Spin Right Beam** / **Course-Mounted Spin-Right Beam** | Rotates the target 90 degrees clockwise. Causes no damage. |

Course-mounted weapons are fixed to the course and fire during a phase after the robots' weapons. There is no course-mounted Exchange Beam, as its point of origin would have to move.

## 6. Damage, Destruction, and Repair

Robots accumulate damage from hazards and attacks. What happens when a robot is destroyed depends on the game mode:

- **Finish:** The robot is eliminated only for the remainder of the current round. At the start of the next round, it returns to its Archive Position – the last checkpoint or Pit Stop it reached – and remains in the game. It normally returns with two damage points. The **Superior Archive Copy** module can reduce this damage to zero.
- **Kill All:** A destroyed robot is permanently eliminated from the game.

### Power-Down

When a Power-Down occurs, the already programmed program is still executed in full. The suspension and the resetting or loss of the existing damage points only take effect in the following round.

### Power-Down Shield

The Power-Down Shield is active during a Power-Down and blocks direct laser hits. Protection is consumed direction by direction: Four directional shield points are available in each phase, and a hit consumes the protection from the corresponding direction. It does not protect against push or fall damage.

### Pit Stop

A Pit Stop repairs a maximum of **one damage point** and also gives the robot a randomly selected new module.

## 7. Modules and Weapons

Modules are either available from the beginning or are awarded after reaching a Pit Stop at the end of the round (after Phase 5). Depending on the module, it is either active immediately or taken into account during programming.

Some modules have **Ammunition** (also called *amount*). These modules can be acquired multiple times; each additional acquisition increases the ammunition by the module type's standard value (*default amount*). The current standard values in the code are: Bridge Projector 3, Glue Dispenser 2, Mine Layer 2, Mobile Teleporter 1, Mobile Randomizer 1, Oil Dispenser 2, Proximity Mine Layer 3, Layered Armor 4, and Extra Program 1. Modules without ammunition are either present or absent. Such a module cannot be awarded again at a Pit Stop.

### Weapons

Weapons are selected during programming. Only one weapon can be active at a time. The following weapons do not have ammunition:

- **Main Laser**
- **Double Barrel Laser**
- **High Power Laser**
- **Rear Laser**
- **Exchange Beam**
- **Pressure Beam**
- **Spin Left Beam**
- **Spin Right Beam**
- **Tractor Beam**

The **High Power Laser** can shoot through the first robot it hits and hit another robot behind it.

### Movement Weapon: Ramming Armor

**Ramming Armor** inflicts one damage point on the first robot its carrier collides with or rams during each phase. This also applies if the robot being hit cannot be moved because of a wall or another obstacle. Ramming Armor has no ammunition.

### Dispensers

Dispensers leave a device or object on the space occupied by the robot. The **Bridge Projector** places its bridge on the space in front of the robot instead. Dispensers have ammunition, which can have a value between 1 and 3.

The dispensers are:

- **Bridge Projector**
- **Glue Dispenser**
- **Oil Dispenser**
- **Mine Layer**
- **Proximity Mine Layer**
- **Mobile Teleporter**
- **Mobile Randomizer**

### Movement-Modifying Modules

These modules do not have ammunition:

- **Hovercraft:** Enables flying movement.
- **Extra Program:** Increases the number of program steps allocated. See the Programming section for details.
- **Overdrive:** Allows the **Move 4** program card to be dealt.
- **Reverse Drive:** Allows the **Back-Up 2** program card to be dealt.
- **Brakes:** Allows a program step to be shortened by one movement unit, potentially bringing it to a complete stop.
- **Gyroscopic Stabilizer:** Prevents the robot from being rotated by gears or conveyor belts.

### Defensive Modules

- **Layered Armor:** Has 4 ammunition by default and absorbs up to four direct damage events handled by the protection system (e.g. lasers). Other types of damage, such as mine explosions or fall damage, bypass this protection.
- **Shield:** Protects against a direct laser hit from the selected direction. A successful block prevents the hit and consumes the Shield for that phase.
- **Power-Down Shield:** Is active during a Power-Down and blocks direct laser hits. Protection is consumed direction by direction: Four directional shield points are available in each phase, and a hit consumes the protection from the corresponding direction. It does not protect against push or fall damage.

### Mechanical Arm

The **Mechanical Arm** allows a checkpoint to be reached from an adjacent space. It has no ammunition.

### Other Modules

- **Superior Archive Copy:** Reduces the damage taken when returning to the Archive Position to zero.

## 8. Checkpoints and Victory

In the normal racing mode, checkpoints must be reached in the specified order. The first player to reach the finish wins. The Pit Stop can be used for repairs during the race.

In **Kill All** mode, the winner is the player who eliminates all other robots. In this mode, all checkpoints become starting positions, and the robots are distributed evenly among them. **Multiple starting positions can also be used in normal Finish mode.**
