# Launcher velocity control

This note is for the opposed 3-inch flywheel launcher on hardware names `outtake1` and `outtake2`. Those names are the ones already used by `haydenTele`, `leftEdgeAuto`, and `rightEdgeAuto`. The wheel diameter is not used in the math. Surface speed is not calculated.

The match TeleOp is still `Boy you want hotdog / USE THIS TELEOP`. It starts in the same open-loop power mode as before. Velocity mode is optional and stays off until the gate below passes.

`1500` flywheel-shaft RPM is only the initial test reference in `LauncherVelocityConfig.TEST_REFERENCE_FLYWHEEL_SHAFT_RPM`. It is not a scoring speed, not a safety limit, and not a promise that either motor can reach it.

## Driver controls

These gamepad actions are unchanged:

- Gamepad 1 left stick Y: forward. The stick is inverted in code, the same as before.
- Gamepad 1 left stick X: strafe.
- Gamepad 1 right stick X: turn.
- Gamepad 1 A, held: drive power is multiplied by `0.25`. Released: full drive scale.
- Gamepad 2 right trigger: intake power, negative, unless the right bumper is held.
- Gamepad 2 right bumper, held: intake power `1`.
- Gamepad 2 left bumper, held: midtake power `-1`. Released: midtake `0`.
- Gamepad 2 left trigger: launcher. `outtake1` gets `left_trigger * 1`. `outtake2` gets `left_trigger * -1`.

New control, and it is the only new control:

- Gamepad 2 Y, rising edge: toggles launcher velocity mode. The button must go from released to pressed. Holding Y while Start is pressed does not toggle. If the left trigger is at `0.05` or more, the tap is ignored and the current mode keeps running. Release the trigger, then tap Y.

`gamepad2` Y was not read by this TeleOp. A, B, X, the d-pad, and the sticks on gamepad 2 are still unused.

## Power mode and velocity mode

Power mode is the default. The left trigger still writes those two powers with `setPower`. Releasing the trigger still commands `0`.

Velocity mode does not use power as a speed. The left trigger, from 0 to 1, scales the magnitude of a ticks-per-second command toward the test reference:

- motor-output RPM asked for = `1500 / abs(external ratio)`
- that ask is cut down to `max output RPM * closed-loop fraction`, and then to `32767` ticks/s, which is the REV hub velocity limit in SDK 12.0.0
- `outtake1` is commanded positive and `outtake2` negative, the same signs as power mode

The hub rounds the command to integer ticks/s. The screen labels that line `Commanded, not measured`.

Leaving velocity mode, a velocity fault, and opmode stop all write velocity `0`, then `RUN_WITHOUT_ENCODER`, then power `0`. `setVelocity` after the open-loop mode change is avoided on purpose: in this SDK, `setVelocity` puts the motor back into `RUN_USING_ENCODER`.

If a velocity command is large and that encoder stays near 0 ticks/s for 400 ms, velocity mode drops out and holds power at 0 until the left trigger is released. A dead encoder must not be left in closed-loop control, because the hub will keep adding power to chase a speed it cannot see.

TeleOp init also selects `RUN_WITHOUT_ENCODER` and power `0` on the two launcher motors. That is the SDK default `setPower` already used. It keeps a leftover closed-loop mode from turning the old `setPower` calls into a hidden velocity command.

## What the screen is reporting

Measured lines use `DcMotorEx.getVelocity()`, which is encoder ticks per second. They are not derived from the trigger.

- `measured ticks/s` is the raw encoder rate. It is shown even when RPM is unavailable.
- `motor output RPM` = ticks/s `/` ticks per motor-output revolution `*` 60. The FTC motor type's ticks/rev value is defined as output-shaft ticks, so this is motor-output RPM, not a separate pre-gearbox sensor.
- `flywheel shaft RPM` = motor output RPM `*` external ratio. This line stays `unavailable` until that ratio is configured. A missing ratio is never treated as 1.
- `armature RPM`, when it appears, is motor output RPM times the internal gearbox ratio. It is display only. It is not the flywheel and it is not a second encoder.

The sign on those RPM numbers follows the SDK command frame. Positive means the same direction as positive `setPower` after the SDK applies motor direction. TeleOp never calls `setDirection` on `outtake1` or `outtake2`. If the configured motor type's orientation is CCW, the SDK already inverts both power and encoder counts; that inversion existed before this change and it applies to velocity the same way. Opposite signs are how this robot commands the opposed flywheels. The sign is not, by itself, which face of the wheel touches the game piece.

The test-reference line says `UNKNOWN` when the ticks, the closed-loop limit, or the external ratio is missing. It says `UNREACHABLE` only when those inputs exist and `1500` flywheel RPM would need more motor RPM or more ticks/s than the verified limit. The command is then capped. It is not raised to 1500 anyway.

## Why the gate stays shut

Velocity mode turns on only when all of these are true:

1. Both motors are `DcMotorEx`.
2. Both have a usable ticks/rev and a usable closed-loop output-RPM limit.
3. The external flywheel ratio is a finite non-zero number.
4. The test reference is a positive finite RPM.
5. Each encoder has already moved at least 15 ticks/s during this TeleOp. Spin each flywheel by hand, or with a short power-mode pull, until both measured tick lines move. Then release the trigger and tap Y.
6. The left trigger is released at the moment of the tap.

There is no robot configuration file in this repository. The motor type is whatever was selected on the Robot Controller. SDK 12.0.0 was inspected for this project. These configured types are rejected, because their stored numbers are a stand-in gearbox and would otherwise look like a real measurement:

- `Motor` / `Unspecified Motor`. The SDK comment says this uses historical Tetrix numbers (`1440` ticks/rev, gearing `52`, `165` RPM).
- `goBILDA5202SeriesMotor` / `GoBILDA 5202/3/4 series`. The SDK stores the 60 RPM class (`2786` ticks/rev, gearing `99.5`, `60` RPM) for the whole series.
- `goBILDA5201SeriesMotor` / `GoBILDA 5201 series`. One spur ratio stands in for the series.
- `RevRoboticsUltraplanetaryHDHexMotor`. One cartridge ratio stands in for whatever stack is on the motor.

The screen prints those SDK numbers and says they are not used. A specific SDK type, such as one NeveRest or HD Hex entry whose xml tag is not in that list, can be used, and its name is printed so it can be checked against the motor on the robot. A new SDK type with its own xml tag is not rejected by the list above. The printed name still has to match the physical motor.

Do not copy the series numbers into the verified fields. That would make the screen show an RPM the encoder did not measure for this gearbox, and it can mark 1500 unreachable only because the placeholder says 60 RPM.

## Calibration

Edit `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/launcher/LauncherVelocityConfig.java`. Leave a field as `Double.NaN` until it is known. An invalid number is ignored and called out on the screen.

1. Identify the motor on each launcher port from the hardware, not from the series dropdown. Both overrides apply to both motors, so the two launcher motors need the same verified numbers.
2. Set `VERIFIED_TICKS_PER_MOTOR_OUTPUT_REV` to encoder counts per revolution of the motor output shaft. That count already includes the motor's own gearbox. It does not include a belt or gear outside the motor.
3. Set `VERIFIED_MAX_MOTOR_OUTPUT_RPM` to the no-load output RPM for that same motor.
4. Set `VERIFIED_CLOSED_LOOP_RPM_FRACTION` to the share of that no-load RPM you are willing to command in closed loop. Specific SDK types use `0.85` when you are relying on the SDK entry itself. This file does not assume `0.85` for a hand-entered motor. Use a value greater than 0 and at most 1.
5. Set `EXTERNAL_FLYWHEEL_REVS_PER_MOTOR_OUTPUT_REV` to flywheel-shaft revolutions per motor-output revolution. Count teeth, or mark both shafts and turn one by hand. Use `1.0` only if the flywheel is mounted on the motor output shaft and you have confirmed that. Use a negative number for a crossed belt. This is not the internal gearbox ratio.
6. `VERIFIED_INTERNAL_GEARING` is optional and only feeds the armature RPM line. Armature revolutions per output revolution. Leave it unset if you do not need that line.
7. Deploy, run TeleOp, and read the SDK type lines. Confirm the screen is using the team-verified numbers you entered, or a specific SDK type you recognize. Confirm the test-reference line. If it says `UNREACHABLE`, the cap on that line is the most the code will command. Do not treat 1500 as reachable.
8. With the trigger released, rotate each flywheel until both `measured ticks/s` lines move by at least 15. Tap gamepad 2 Y. The mode line should change to `VELOCITY` and the command should still be 0.
9. Pull the left trigger a small amount. Both commanded tick lines should grow with opposite signs, and both measured tick lines should move. Let go. Both commands should return to 0. Tap Y again, with the trigger released, to return to power mode.
10. Check the physical spin direction against the old power-mode behavior before using velocity mode in a match. The code keeps the old signs; it does not re-decide which wheel faces the game piece.

Autonomous is unchanged except for a comment on the full-power `shoot()` commands in `leftEdgeAuto` and `rightEdgeAuto`. Those commands stay `outtake1` power `1` and `outtake2` power `-1`. Do not turn on velocity mode there until the TeleOp gate has been validated on the robot.

No PIDF coefficients are set. Velocity mode uses the hub's existing coefficients for `RUN_USING_ENCODER`. They are not tuned for this launcher.

## Not validated on hardware

`LauncherVelocityMathTest` checks the gate, the 1500 cap, the placeholder motor types, and the rule that trigger power is not displayed as RPM. `./gradlew assembleDebug` only compiles the app. No Control Hub, encoder, or flywheel was connected. A successful build does not mean the flywheel is calibrated or safe to run at 1500 RPM.
