# Version 1 teleop, feed, and both launchers

This folder is an addition to team 12820 BioBuzz. It does not replace, disable, or repair the student opmodes already on master.

The shipped settings command no motion. The robot is not ready to drive, intake, feed, or launch until the team fills `Version1Config.java` from a supported bench check and rebuilds. No power, distance, heading, or launcher speed in this folder is a measured result from this robot.

This change is not merged and is not installed on a robot controller.

## Source reviewed

Current master tip reviewed for this package:

`dd23f69bc56a8b58a484ef89b42ba6dfd3ddd8b9`

Commit subject: Update leftEdgeAuto.java

Commit time: 2026-11-07 08:49:42 -0600

`origin/master` matched that commit when this branch was created. The historical commit `7cd0a6ce0a5f64c91cbf1ac416e6d863a836b85c` ("wha", 2026-11-06) is an ancestor. It is not the current tip.

Commits on master after `7cd0a6ce`:

- `2817da3f781c611a139d05987627306976d7ff34` Add outtake and midtake motors with control logic. This commit edits only `haydenTele.java`.
- `c58599f` deletes `what.java`. That file is not on the current tip. Its motor names were `lf`, `lr`, `rf`, and `rr`.
- `19c28e5` updates `rightEdgeAuto.java`.
- `dd23f69` updates `leftEdgeAuto.java`.

There is no Robot Controller configuration XML in this repository. Device names below are names that appear in Java. They are not a confirmed hardware map.

## What the current student files command

`haydenTele.java` is registered as "Boy you want hotdog / USE THIS TELEOP". On the current tip it looks up `frontLeftDrive`, `backLeftDrive`, `frontRightDrive`, `backRightDrive`, `intake`, `outtake1`, `outtake2`, and `midtake`. It sets drive directions front-left REVERSE, back-left REVERSE, front-right FORWARD, back-right FORWARD. It does not call `setDirection` on intake, midtake, outtake1, or outtake2. Gamepad 1 left stick drives, right stick rotates with yaw multiplied by 1.1, and A selects scale 0.25 instead of 1. Strafe uses sign +1. Gamepad 2 right bumper commands intake +1, otherwise the right trigger commands intake at the trigger value times -1. The left trigger commands outtake1 at +trigger and outtake2 at -trigger. The left bumper commands midtake at -1, otherwise midtake is 0. There is no deadband, no emergency-stop latch, no `stop()` override, and no feed interlock. The file still imports vision types and does not use them.

At `7cd0a6ce`, `haydenTele.java` looked up `frontLeft`, `backLeft`, `frontRight`, `backRight`, and `intake`, opened "Webcam 1", and used strafe sign -1. It did not reference outtake or midtake. The report that teleop lacked feed and launcher describes that older file. It does not describe the current tip. The current teleop does command both outtake motors and midtake, at full magnitude, with the signs above.

`testop.java` and `testAuto.java` look up `frontLeft`, `backLeft`, `frontRight`, `backRight`, `intake`, `servo`, and "Webcam 1". `pedro/Constants.java` uses `frontLeft`, `frontRight`, `backLeft`, and `backRight`. `testop.java` uses strafe sign -1. Those names disagree with the current `haydenTele.java` and both edge autos. This package does not choose between the two families.

`leftEdgeAuto.java` is registered as "left park auto". `rightEdgeAuto.java` is registered as "right park auto". Neither is `@Disabled`. Both look up the same eight motor names as current `haydenTele.java`, plus an IMU named `imu`, and both initialize that IMU with logo UP and USB LEFT. After start, `leftEdgeAuto` calls `move(18, 0.5)`, `turn(-90)`, `move(115, 0.5)`, `turn(0)`, `move(40, 0.5)`, `turn(-90)`, `shoot()`, `turn(180)`, `move(40, 0.5)`, `turn(90)`, `move(115, 0.5)`, `turn(180)`, and `move(18, 0.5)`. `rightEdgeAuto` uses 105 where the left file uses 115 on the two long moves. `move` treats distance times 26 as an encoder target and reads only the front-left encoder. `turn` uses `targetAngle - yaw` with no wrap across the -180/180 boundary. `move` does wrap its heading correction, then adds that correction to 0.5, so a wheel command can exceed 1. `shoot` sets outtake1 to 1, outtake2 to -1, and midtake to -1, sleeps 3000 ms, then sets those three to 0. `pickup` is not called by the path. None of those distances, the 26 tick factor, the IMU orientation, or the shoot signs were measured for this package.

`testAuto.java` is registered as "Auto detection / DONT USE" and is not `@Disabled`. `pedropathingAutoStart.java` is registered as "DONT USE THIS" and is not `@Disabled`. In `pedropathingAutoStart.init`, the follower assignment is commented out and the next line calls `follower.setPose`. This package does not change either file.

## Old autonomous files

These files are still in the project and still have their original annotations:

- `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/leftEdgeAuto.java`
- `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/rightEdgeAuto.java`
- `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/testAuto.java`
- `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedropathingAutoStart.java`
- `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/haydenTele.java`
- `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/testop.java`

`Version1AutoDisabled` does not wrap them, unregister them, or change their commands. Selecting an old auto still runs that old file. The new auto class does not make heading, feed, or park behavior in those files safe.

## Files added

All of these are new. No existing file is modified.

- `Version1Config.java` — names, directions, powers, and confirmation gates
- `Version1Hardware.java` — lookup, zero, direction apply, init report
- `Version1TeleOp.java` — drive, intake, feed, both launchers
- `Version1AutoDisabled.java` — `@Disabled`, sends power 0 only
- `VERSION1_BENCH_NOTES.md` — this guide

The teleop and the disabled auto use FTC SDK classes already on the TeamCode classpath: `LinearOpMode`, `TeleOp`, `Autonomous`, `Disabled`, `DcMotor`, `HardwareMap`, and `Telemetry`. They do not import vision, AprilTag, Pedro Pathing, Pinpoint, OTOS, or a webcam. No Gradle file is changed. No dependency is added.

## How motion stays off

`Version1TeleOp` sets every found motor to 0 during init, at the start of every loop, and in a `finally` block when `runOpMode` exits. `LinearOpMode.stop()` is final in FTC SDK 12.0.0, so this class does not override it. A non-zero command is sent only when every gate for that actuator is open.

Drive is sent only when all of these are true:

- `HARDWARE_MAP_CONFIRMED` is true
- `DRIVE_DIRECTIONS_CONFIRMED` is true
- `POWER_LIMITS_CONFIRMED` is true
- the four drive names are non-blank, unique, and found
- all four drive directions are non-null
- `STRAFE_SIGN` is exactly 1 or exactly -1
- `DRIVE_POWER_SCALE` is greater than 0 and at most 1
- `STICK_DEADBAND` is in the range from 0 inclusive to 0.5 exclusive
- `YAW_GAIN` is a finite number
- emergency stop is not latched
- if A is held, `SLOW_MODE_SCALE` is also greater than 0 and at most 1, otherwise drive is sent as 0 while A is held

Intake is sent only when the hardware-map, mechanism-direction, and power-limit flags are true, the intake name is found and unique, `INTAKE_DIRECTION` is non-null, `INTAKE_POWER` is in (0, 1], the trigger deadband is valid, and emergency stop is off.

Feed uses the same mechanism flags, `FEED_DIRECTION`, and `FEED_POWER`.

Both launchers use the same mechanism flags. One missing launcher, one null launcher direction, or one launcher power outside (0, 1] keeps both launchers at 0.

Feed toward the launcher also requires `LAUNCHER_HOLD_BEFORE_FEED_MS` to be 0 or greater, both launchers to be commanded on that loop, and that hold to have elapsed. The shipped value is -1, so feed toward the launcher stays off. Feed reverse does not use that timer. Reverse exists so a jammed transfer can be run backward after the feed direction is confirmed. Until `FEED_DIRECTION` is set and the mechanism gates are open, reverse is also 0.

Positive mechanism power means the physical direction the team checked: intake pulling in, feed moving toward the launcher, each launcher spinning in the launch direction. Wiring reversal belongs in `setDirection`, not in a second hidden sign. Current `haydenTele.java` commands outtake2 with the opposite sign of outtake1. That is evidence from that file, not a direction this package applies.

## Driver controls

Driver Station name: `V1 TeleOp UNCONFIRMED`, group `Version1`.

The new auto name is `V1 Auto DISABLED no motion`. `@Disabled` keeps it off the runnable list. The loop would still send 0 if the annotation were removed. It has no distances.

### Gamepad 1

- Left stick Y: forward and back. The SDK reports stick Y negative when the stick is pushed up, and the code negates it so stick up is positive forward.
- Left stick X: strafe, multiplied by `STRAFE_SIGN` after the deadband. 0 blocks the entire drive.
- Right stick X: rotate, multiplied by `YAW_GAIN`, then the four wheel commands are divided by the largest magnitude if any wheel would exceed 1.
- A, held: multiplies the drive scale by `SLOW_MODE_SCALE`. The shipped slow scale is 0.25, matching the factor in `haydenTele.java`. It is a stick scale, not a measured speed. If that scale is outside (0, 1], holding A sends drive 0.
- Back or Share: latch emergency stop.
- Start or Options: one half of the clear chord.
- Unused, and they command nothing: B, X, Y, dpad, both bumpers, both triggers, both stick buttons.

### Gamepad 2

- Left bumper, held: intake in at `INTAKE_POWER`.
- Left trigger: intake out. Magnitude is `INTAKE_POWER` times the trigger after the deadband. Sign is negative.
- Left bumper and left trigger together: intake 0.
- Right bumper, held: both launchers at their configured positive powers. Release sends 0 on the next loop. This is hold-to-run, not a toggle.
- X, held: feed toward the launcher at `FEED_POWER`, and only when the launcher hold allows it.
- B, held: feed reverse at negative `FEED_POWER`, with no launcher requirement.
- X and B together: feed 0.
- Back or Share: latch emergency stop.
- Start or Options: the other half of the clear chord.
- Unused, and they command nothing: Y, dpad, right trigger, both stick axes, both stick buttons.

Intake does not start the feed. Feed does not start the intake. The two launchers are commanded together. This is a two-motor launcher, not a single flywheel and not an arm.

### Priority

1. OpMode stop. `LinearOpMode.stop()` is final in FTC SDK 12.0.0, so these classes do not override it. `runOpMode` leaves its loop when the opmode stops, and the `finally` block sets every motor power to 0. The loop also sets every motor to 0 before reading controls.
2. Emergency stop latch. Back or Share on gamepad 1 or gamepad 2 latches it. While latched, drive, intake, feed, and both launchers are not applied, and the launcher hold timer is cleared.
3. Confirmation gates, missing or duplicate hardware, null directions, and powers outside (0, 1].
4. Opposite buttons on one mechanism. Intake in plus intake out sends 0. Feed toward plus feed reverse sends 0.
5. Feed-toward hold. X does nothing until both launchers are actually being commanded and the configured millisecond hold has elapsed.
6. The single remaining command is sent.

Clear the latch by holding Start or Options on gamepad 1 and Start or Options on gamepad 2 at the same time, with Back and Share released on both pads. `Gamepad` in RobotCore 12.0.0 has `back`, `share`, `start`, and `options` as separate fields. This teleop reads each pair so an Xbox Back/Start pad and a PlayStation Share/Options pad both reach the latch. Clearing the latch does not open a closed gate.

Sticks and triggers inside the deadband become 0. Outside the deadband, the remaining travel is scaled so a full push is still a full command before the power scale. Shipped deadbands are 0.05. That is a software noise floor, not a measured robot value.

Wheel mix, before the power scale, is the FTC sample mecanum mix:

- front left = forward + strafe + yaw
- front right = forward - strafe - yaw
- back left = forward - strafe + yaw
- back right = forward + strafe - yaw

`STRAFE_SIGN` of +1 matches current `haydenTele.java` and that sample. `STRAFE_SIGN` of -1 matches `haydenTele.java` at `7cd0a6ce` and `testop.java`. Leave it at 0 until a supported test picks one.

Every found motor is set to `RUN_WITHOUT_ENCODER`. Encoder ticks are not used for driving or for launcher speed. `ZERO_POWER_BEHAVIOR` is `BRAKE` so a power of 0 asks the hub to brake. That is not a measured stopping distance. Change the constant to `FLOAT` if the team wants coast.

## Init diagnostics

Before start, and again every loop, telemetry shows:

- phase INIT or RUN
- emergency-stop state and the clear chord
- Drive, Intake, Feed, and Launchers as ARMED or BLOCKED
- the four confirmation flags
- configured strafe sign, hold milliseconds, yaw gain, slow scale, and the five power numbers
- feed-toward status
- the powers actually sent this loop
- raw drive sticks
- one map line per motor: `NAME UNSET`, `FOUND "name"`, or `MISSING "name"`
- whether a direction was applied

With the shipped file, expect every actuator BLOCKED, every sent power 0.00, every name `NAME UNSET`, and all four flags false. If a typed name is absent from the active configuration, the line says MISSING and that actuator stays blocked. `hardwareMap.get` failure is caught per motor so the rest of the report still prints.

Read "Configured power" to confirm the numbers in the running build. A Driver Station session started before a rebuild still shows the previous numbers.

## Fill in Version1Config

Edit only `Version1Config.java`. Restart the opmode after each rebuild. Order:

1. Open the Driver Station configuration and copy motor names exactly, including capitals. Type them into `FRONT_LEFT_NAME`, `BACK_LEFT_NAME`, `FRONT_RIGHT_NAME`, `BACK_RIGHT_NAME`, `INTAKE_NAME`, `FEED_NAME`, `LAUNCHER_1_NAME`, and `LAUNCHER_2_NAME`. Leave a string empty if the configuration does not show that device. Do not invent a name to make the opmode start.
2. Candidate strings seen in current Java, for comparison only: drive `frontLeftDrive` or `frontLeft` and the matching back and right names; intake `intake`; feed `midtake`; launchers `outtake1` and `outtake2`.
3. On a supported robot, use the motor test control in the configuration screen, one motor at a time. Set each `DcMotor.Direction` so positive power is forward on that drive wheel, intake in, feed toward the launcher, and launch spin on each launcher. Leave a direction null if you have not watched that motor.
4. Type power magnitudes the team is willing to command on a supported robot. Use a number greater than 0 and at most 1. Leave 0 to keep that actuator off. This guide has no tested power to copy.
5. Set `STRAFE_SIGN` to 1 or -1 only after stick-left and stick-right move the supported robot the way the driver expects.
6. Set `LAUNCHER_HOLD_BEFORE_FEED_MS` to 0 or a positive millisecond count only when the team wants feed-toward to become available after that timer. 0 means no extra wait. It does not mean the launcher is at speed. There is no speed sensor in this opmode. Leave -1 to keep feed-toward off.
7. Set `HARDWARE_MAP_CONFIRMED`, `DRIVE_DIRECTIONS_CONFIRMED`, `MECHANISM_DIRECTIONS_CONFIRMED`, and `POWER_LIMITS_CONFIRMED` to true only for the checks that are actually done. You can type names and numbers first, leave the booleans false, and use telemetry to read them back while every sent power stays 0.

`YAW_GAIN` ships at 1. Current `haydenTele.java` uses 1.1. Change it only if the team wants that extra rotate gain. The wheel commands are still normalized to at most 1 before the drive scale.

## Installation the team does later

This branch does not install the app and does not merge into master.

1. Use branch `cursor/version1-teleop-feed-launcher-8bd9`. Do not merge it as part of this delivery.
2. Android Studio: open this project and let the existing Gradle files build TeamCode. OnBotJava: copy the four `.java` files into `org/firstinspires/ftc/teamcode/version1/` and build there. Copying the markdown is optional. It is not compiled.
3. Install the robot controller app with the team's normal cable or wireless workflow.
4. Select the active configuration whose names match `Version1Config`.
5. Choose `V1 TeleOp UNCONFIRMED`. Do not assume it replaced "Boy you want hotdog / USE THIS TELEOP". The old opmode is still in the list.
6. The disabled auto does not appear as a runnable opmode while `@Disabled` is present.

## Bench check

No step below was executed on the robot for this delivery. Do these with the robot supported so wheels and mechanisms cannot touch the floor or a person, with a person at the Driver Station.

1. Write down the configuration names and the physical result of each configuration-screen motor test.
2. Enter only those results in `Version1Config.java`.
3. Leave the four confirmation booleans false. Build, install, and start `V1 TeleOp UNCONFIRMED`. Confirm telemetry shows FOUND for each name you entered, BLOCKED on every actuator, and sent powers 0.00 while sticks and buttons are pressed.
4. Enter powers and directions. Leave the booleans false. Rebuild and confirm "Configured power" matches the file and sent powers stay 0.00.
5. Set only the booleans for checks you completed. Rebuild. On the supported robot, arm one actuator at a time.
6. Press Back or Share. Confirm the e-stop line says LATCHED and every sent power is 0.00 while sticks and mechanism buttons are held. Release that button. Confirm it stays latched. Hold Start or Options on both pads. Confirm the latch clears. Confirm closed gates still send 0.
7. Confirm intake in and intake out together send intake 0. Confirm X and B together send feed 0. Confirm X sends feed 0 until both launchers are commanded and the hold line reaches the configured milliseconds. Confirm releasing the right bumper sends both launcher powers to 0.
8. Floor driving, real game pieces, and any autonomous path wait until that supported check matches the telemetry. This package has no autonomous distances to enable.

## Unresolved until the team verifies them

- Active configuration name for each of the eight motors
- Which drive-name family the controller uses
- `DcMotor.Direction` for each of the eight motors
- `STRAFE_SIGN`
- `DRIVE_POWER_SCALE`, `INTAKE_POWER`, `FEED_POWER`, `LAUNCHER_1_POWER`, `LAUNCHER_2_POWER`
- `LAUNCHER_HOLD_BEFORE_FEED_MS`
- All four confirmation booleans, which ship false
- IMU orientation, encoder scale, and path distances in the old autos
- Whether BRAKE is the stop behavior the team wants
- Any launcher speed. This opmode cannot report one.

## Build check

No Gradle file in the repo was edited. `local.properties` was created only on the build machine with `sdk.dir=/tmp/android-sdk`. That file is listed in `.gitignore` and is not part of this change. The Android SDK used for the check is outside the repo: command-line tools, `platforms;android-34`, `build-tools;35.0.0`, and `platform-tools` under `/tmp/android-sdk`. JDK is OpenJDK 21.0.12.1. The wrapper distribution is Gradle 9.1.0. Android Gradle Plugin 8.13.2 and `compileSdk 34` come from the existing project files.

Working directory: `/workspace`

Environment for each command:

`ANDROID_HOME=/tmp/android-sdk`

`ANDROID_SDK_ROOT=/tmp/android-sdk`

First command, before `stop()` was removed:

`./gradlew :TeamCode:compileDebugJavaWithJavac --stacktrace`

Result: `BUILD FAILED in 30s`. The compiler reported two errors, both in the new files: `stop()` in `Version1TeleOp` and `Version1AutoDisabled` cannot override `stop()` in `LinearOpMode` because the overridden method is final. The existing student files were not the errors. Those two overrides were removed. Zeroing stays in the `finally` block.

Second command, on the Java in this change after that removal:

`./gradlew :TeamCode:compileDebugJavaWithJavac --stacktrace`

Result: `BUILD SUCCESSFUL in 821ms`. Exit code 0. Warnings, not failures: Java compiler 21 says source and target value 8 are obsolete. That source level is set by the existing `build.common.gradle`. It was not changed.

Third command:

`./gradlew :TeamCode:assembleDebug --stacktrace`

Result: `BUILD SUCCESSFUL in 33s`. Exit code 0. 52 actionable tasks: 26 executed, 26 up-to-date. Output file: `TeamCode/build/outputs/apk/debug/TeamCode-debug.apk` (55061610 bytes). `stripDebugDebugSymbols` reported it could not strip `libEasyOpenCV.so`, `libRobotCore.so`, `libapriltag.so`, `libc++_shared.so`, `libopencv_java4.so`, and `libturbojpeg.so`, and packaged them as they are. Those libraries come from dependencies already declared in `build.dependencies.gradle`, including FTC Vision. This change does not add a dependency and the new opmodes do not reference vision. NDK `21.3.6528147` from `build.common.gradle` was not installed for this check. `assembleDebug` still completed.

Gradle also printed that deprecated features were used and that this build is incompatible with Gradle 10. That message comes from the existing project build.

This compile and package result does not show that a robot moved, that a hardware name exists on the controller, or that a direction is right. A compile that stubs the FTC SDK was not used.
