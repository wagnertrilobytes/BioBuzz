package org.firstinspires.ftc.teamcode.version1;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import java.util.List;

/**
 * Version 1 teleop for mecanum drive, intake, transfer feed, and both launchers.
 *
 * The robot is not ready to run from this file until Version1Config is filled
 * from a supported bench check. With the shipped settings every sent power is 0.
 *
 * Controls, highest priority first:
 * 1. Stopping the opmode ends runOpMode(). LinearOpMode.stop() is final in
 *    this SDK, so the finally block sets every motor to 0.
 * 2. Back or Share on either gamepad latches emergency stop. Every motor stays at 0.
 *    Start or Options on both gamepads together, with Back and Share released,
 *    clear the latch. Clearing the latch does not bypass a closed confirmation gate.
 * 3. Closed confirmation gates, missing hardware, duplicate names, unset
 *    directions, and powers outside (0, 1] keep the blocked actuators at 0.
 * 4. Intake in together with intake out sets intake to 0.
 *    Feed toward launcher together with feed reverse sets feed to 0.
 * 5. Feed toward the launcher stays at 0 until both launchers are actually
 *    running and LAUNCHER_HOLD_BEFORE_FEED_MS is a configured hold that has
 *    elapsed. That hold is a timer. It is not a measured launcher speed.
 * 6. One allowed command is then sent.
 *
 * Gamepad 1: left stick forward/back and strafe, right stick rotate,
 * A holds slow scale, Back or Share latches stop, Start or Options is half of the clear chord.
 * Gamepad 2: left bumper intake in, left trigger intake out,
 * right bumper both launchers, X feed toward launcher, B feed reverse,
 * Back or Share latches stop, Start or Options is half of the clear chord.
 * Other buttons are unused and command nothing.
 *
 * Old opmodes are still on the Driver Station. This class does not change them.
 */
@TeleOp(name = "V1 TeleOp UNCONFIRMED", group = "Version1")
public class Version1TeleOp extends LinearOpMode {

    private final Version1Hardware robot = new Version1Hardware();
    private boolean eStopLatched = false;
    private long launcherHoldStartMs = -1L;

    @Override
    public void runOpMode() {
        try {
            robot.init(hardwareMap);
            while (!isStarted() && !isStopRequested()) {
                robot.zeroAll();
                updateEmergencyStop();
                publishTelemetry(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "INIT");
                idle();
            }

            while (opModeIsActive()) {
                robot.zeroAll();
                updateEmergencyStop();

                double frontLeft = 0.0;
                double frontRight = 0.0;
                double backLeft = 0.0;
                double backRight = 0.0;
                double intake = 0.0;
                double feed = 0.0;
                double launcher1 = 0.0;
                double launcher2 = 0.0;

                if (!eStopLatched) {
                    double[] drive = driveCommand();
                    frontLeft = drive[0];
                    frontRight = drive[1];
                    backLeft = drive[2];
                    backRight = drive[3];
                    intake = intakeCommand();
                    double[] launchAndFeed = launcherAndFeedCommand();
                    launcher1 = launchAndFeed[0];
                    launcher2 = launchAndFeed[1];
                    feed = launchAndFeed[2];
                } else {
                    launcherHoldStartMs = -1L;
                }

                robot.command(robot.frontLeft, frontLeft);
                robot.command(robot.frontRight, frontRight);
                robot.command(robot.backLeft, backLeft);
                robot.command(robot.backRight, backRight);
                robot.command(robot.intake, intake);
                robot.command(robot.feed, feed);
                robot.command(robot.launcher1, launcher1);
                robot.command(robot.launcher2, launcher2);

                publishTelemetry(frontLeft, frontRight, backLeft, backRight, intake, feed, launcher1, launcher2, "RUN");
            }
        } finally {
            robot.zeroAll();
        }
    }

    private void updateEmergencyStop() {
        boolean stop1 = gamepad1.back || gamepad1.share;
        boolean stop2 = gamepad2.back || gamepad2.share;
        boolean start1 = gamepad1.start || gamepad1.options;
        boolean start2 = gamepad2.start || gamepad2.options;
        if (stop1 || stop2) {
            eStopLatched = true;
        } else if (eStopLatched && start1 && start2) {
            eStopLatched = false;
        }
    }

    private double[] driveCommand() {
        double[] zeros = new double[] {0.0, 0.0, 0.0, 0.0};
        if (!driveArmed(null)) {
            return zeros;
        }

        double forward = shapeStick(-gamepad1.left_stick_y);
        double strafe = shapeStick(gamepad1.left_stick_x) * Version1Config.STRAFE_SIGN;
        double yaw = shapeStick(gamepad1.right_stick_x) * Version1Config.YAW_GAIN;

        double frontLeft = forward + strafe + yaw;
        double frontRight = forward - strafe - yaw;
        double backLeft = forward - strafe + yaw;
        double backRight = forward + strafe - yaw;

        double max = Math.max(Math.abs(frontLeft), Math.abs(frontRight));
        max = Math.max(max, Math.abs(backLeft));
        max = Math.max(max, Math.abs(backRight));
        if (max > 1.0) {
            frontLeft /= max;
            frontRight /= max;
            backLeft /= max;
            backRight /= max;
        }

        double scale = Version1Config.DRIVE_POWER_SCALE;
        if (gamepad1.a) {
            if (!Version1Config.powerInRange(Version1Config.SLOW_MODE_SCALE)) {
                return zeros;
            }
            scale *= Version1Config.SLOW_MODE_SCALE;
        }
        return new double[] {frontLeft * scale, frontRight * scale, backLeft * scale, backRight * scale};
    }

    private double intakeCommand() {
        if (!intakeArmed(null)) {
            return 0.0;
        }
        boolean intakeIn = gamepad2.left_bumper;
        boolean intakeOut = gamepad2.left_trigger > Version1Config.TRIGGER_DEADBAND;
        if (intakeIn && intakeOut) {
            return 0.0;
        }
        if (intakeIn) {
            return Version1Config.INTAKE_POWER;
        }
        if (intakeOut) {
            return -Version1Config.INTAKE_POWER * shapeTrigger(gamepad2.left_trigger);
        }
        return 0.0;
    }

    /**
     * Returns launcher 1 power, launcher 2 power, and feed power.
     * Feed reverse is allowed without the launchers so a jam can be cleared.
     * Feed toward the launcher requires both launchers to be commanded and the
     * configured hold timer to elapse. The timer does not read wheel speed.
     */
    private double[] launcherAndFeedCommand() {
        double launcher1 = 0.0;
        double launcher2 = 0.0;
        boolean launchersOn = false;
        if (launchersArmed(null) && gamepad2.right_bumper) {
            launcher1 = Version1Config.LAUNCHER_1_POWER;
            launcher2 = Version1Config.LAUNCHER_2_POWER;
            launchersOn = true;
        }

        if (launchersOn) {
            if (launcherHoldStartMs < 0L) {
                launcherHoldStartMs = System.currentTimeMillis();
            }
        } else {
            launcherHoldStartMs = -1L;
        }

        double feed = 0.0;
        if (feedArmed(null)) {
            boolean toward = gamepad2.x;
            boolean reverse = gamepad2.b;
            if (toward && reverse) {
                feed = 0.0;
            } else if (reverse) {
                feed = -Version1Config.FEED_POWER;
            } else if (toward && feedTowardAllowed(launchersOn)) {
                feed = Version1Config.FEED_POWER;
            }
        }
        return new double[] {launcher1, launcher2, feed};
    }

    private boolean feedTowardAllowed(boolean launchersOn) {
        long holdMs = Version1Config.LAUNCHER_HOLD_BEFORE_FEED_MS;
        if (!launchersOn || holdMs < 0L || launcherHoldStartMs < 0L) {
            return false;
        }
        long held = System.currentTimeMillis() - launcherHoldStartMs;
        return held >= holdMs;
    }

    private boolean driveArmed(List<String> reasons) {
        boolean ok = true;
        if (!Version1Config.HARDWARE_MAP_CONFIRMED) {
            ok = addReason(reasons, "Drive blocked: HARDWARE_MAP_CONFIRMED is false");
        }
        if (!Version1Config.DRIVE_DIRECTIONS_CONFIRMED) {
            ok = addReason(reasons, "Drive blocked: DRIVE_DIRECTIONS_CONFIRMED is false");
        }
        if (!Version1Config.POWER_LIMITS_CONFIRMED) {
            ok = addReason(reasons, "Drive blocked: POWER_LIMITS_CONFIRMED is false");
        }
        if (!robot.namesAreUnique) {
            ok = addReason(reasons, "Drive blocked: duplicate hardware names");
        }
        if (robot.frontLeft == null || robot.backLeft == null || robot.frontRight == null || robot.backRight == null) {
            ok = addReason(reasons, "Drive blocked: a drive motor is unset or missing");
        }
        if (!directionsSet(Version1Config.FRONT_LEFT_DIRECTION, Version1Config.BACK_LEFT_DIRECTION,
                Version1Config.FRONT_RIGHT_DIRECTION, Version1Config.BACK_RIGHT_DIRECTION)) {
            ok = addReason(reasons, "Drive blocked: a drive direction is null");
        }
        if (Version1Config.STRAFE_SIGN != 1 && Version1Config.STRAFE_SIGN != -1) {
            ok = addReason(reasons, "Drive blocked: STRAFE_SIGN must be 1 or -1");
        }
        if (!Version1Config.powerInRange(Version1Config.DRIVE_POWER_SCALE)) {
            ok = addReason(reasons, "Drive blocked: DRIVE_POWER_SCALE must be in (0, 1]");
        }
        if (!Version1Config.deadbandInRange(Version1Config.STICK_DEADBAND)) {
            ok = addReason(reasons, "Drive blocked: STICK_DEADBAND must be in [0, 0.5)");
        }
        if (!Version1Config.yawGainInRange(Version1Config.YAW_GAIN)) {
            ok = addReason(reasons, "Drive blocked: YAW_GAIN is not a finite number");
        }
        return ok;
    }

    private boolean intakeArmed(List<String> reasons) {
        boolean ok = mechanismCommon(reasons, "Intake");
        if (robot.intake == null) {
            ok = addReason(reasons, "Intake blocked: motor unset or missing");
        }
        if (Version1Config.INTAKE_DIRECTION == null) {
            ok = addReason(reasons, "Intake blocked: INTAKE_DIRECTION is null");
        }
        if (!Version1Config.powerInRange(Version1Config.INTAKE_POWER)) {
            ok = addReason(reasons, "Intake blocked: INTAKE_POWER must be in (0, 1]");
        }
        if (!Version1Config.deadbandInRange(Version1Config.TRIGGER_DEADBAND)) {
            ok = addReason(reasons, "Intake blocked: TRIGGER_DEADBAND must be in [0, 0.5)");
        }
        return ok;
    }

    private boolean feedArmed(List<String> reasons) {
        boolean ok = mechanismCommon(reasons, "Feed");
        if (robot.feed == null) {
            ok = addReason(reasons, "Feed blocked: motor unset or missing");
        }
        if (Version1Config.FEED_DIRECTION == null) {
            ok = addReason(reasons, "Feed blocked: FEED_DIRECTION is null");
        }
        if (!Version1Config.powerInRange(Version1Config.FEED_POWER)) {
            ok = addReason(reasons, "Feed blocked: FEED_POWER must be in (0, 1]");
        }
        return ok;
    }

    private boolean launchersArmed(List<String> reasons) {
        boolean ok = mechanismCommon(reasons, "Launcher");
        if (robot.launcher1 == null || robot.launcher2 == null) {
            ok = addReason(reasons, "Launcher blocked: one or both launcher motors are unset or missing");
        }
        if (Version1Config.LAUNCHER_1_DIRECTION == null || Version1Config.LAUNCHER_2_DIRECTION == null) {
            ok = addReason(reasons, "Launcher blocked: a launcher direction is null");
        }
        if (!Version1Config.powerInRange(Version1Config.LAUNCHER_1_POWER)
                || !Version1Config.powerInRange(Version1Config.LAUNCHER_2_POWER)) {
            ok = addReason(reasons, "Launcher blocked: both launcher powers must be in (0, 1]");
        }
        return ok;
    }

    private boolean mechanismCommon(List<String> reasons, String label) {
        boolean ok = true;
        if (!Version1Config.HARDWARE_MAP_CONFIRMED) {
            ok = addReason(reasons, label + " blocked: HARDWARE_MAP_CONFIRMED is false");
        }
        if (!Version1Config.MECHANISM_DIRECTIONS_CONFIRMED) {
            ok = addReason(reasons, label + " blocked: MECHANISM_DIRECTIONS_CONFIRMED is false");
        }
        if (!Version1Config.POWER_LIMITS_CONFIRMED) {
            ok = addReason(reasons, label + " blocked: POWER_LIMITS_CONFIRMED is false");
        }
        if (!robot.namesAreUnique) {
            ok = addReason(reasons, label + " blocked: duplicate hardware names");
        }
        return ok;
    }

    private boolean directionsSet(DcMotor.Direction a, DcMotor.Direction b, DcMotor.Direction c, DcMotor.Direction d) {
        return a != null && b != null && c != null && d != null;
    }

    private static String flag(boolean value) {
        return value ? "true" : "false";
    }

    private boolean addReason(List<String> reasons, String reason) {
        if (reasons != null) {
            reasons.add(reason);
        }
        return false;
    }

    private void publishTelemetry(double frontLeft, double frontRight, double backLeft, double backRight,
                                  double intake, double feed, double launcher1, double launcher2, String phase) {
        telemetry.addData("Phase", phase);
        if ("INIT".equals(phase)) {
            telemetry.addData("Commands", "not applied until start");
        }
        telemetry.addData("Priority", "opmode stop > e-stop > gates > conflict > feed hold > command");
        telemetry.addData("E-stop", eStopLatched ? "LATCHED all motors 0" : "off");
        telemetry.addData("Clear e-stop", "hold Start or Options on both pads; release Back and Share");
        telemetry.addData("Drive", driveArmed(null) ? "ARMED" : "BLOCKED");
        telemetry.addData("Intake", intakeArmed(null) ? "ARMED" : "BLOCKED");
        telemetry.addData("Feed", feedArmed(null) ? "ARMED" : "BLOCKED");
        telemetry.addData("Launchers", launchersArmed(null) ? "ARMED" : "BLOCKED");
        telemetry.addData("Flags", "map %s driveDir %s mechDir %s power %s",
                flag(Version1Config.HARDWARE_MAP_CONFIRMED),
                flag(Version1Config.DRIVE_DIRECTIONS_CONFIRMED),
                flag(Version1Config.MECHANISM_DIRECTIONS_CONFIRMED),
                flag(Version1Config.POWER_LIMITS_CONFIRMED));
        telemetry.addData("Configured", "strafe %d holdMs %d yaw %.2f slow %.2f",
                Version1Config.STRAFE_SIGN,
                Version1Config.LAUNCHER_HOLD_BEFORE_FEED_MS,
                Version1Config.YAW_GAIN,
                Version1Config.SLOW_MODE_SCALE);
        telemetry.addData("Configured power", "drive %.2f in %.2f feed %.2f L1 %.2f L2 %.2f",
                Version1Config.DRIVE_POWER_SCALE,
                Version1Config.INTAKE_POWER,
                Version1Config.FEED_POWER,
                Version1Config.LAUNCHER_1_POWER,
                Version1Config.LAUNCHER_2_POWER);
        if (!feedArmed(null)) {
            telemetry.addData("Feed toward", "blocked because feed is blocked");
        } else if (Version1Config.LAUNCHER_HOLD_BEFORE_FEED_MS < 0L) {
            telemetry.addData("Feed toward", "blocked until LAUNCHER_HOLD_BEFORE_FEED_MS is set");
        } else if (launcherHoldStartMs < 0L) {
            telemetry.addData("Feed toward", "blocked until both launchers are commanded");
        } else {
            long held = System.currentTimeMillis() - launcherHoldStartMs;
            telemetry.addData("Feed toward", "hold " + held + " / " + Version1Config.LAUNCHER_HOLD_BEFORE_FEED_MS + " ms");
        }
        if (gamepad1.a && !Version1Config.powerInRange(Version1Config.SLOW_MODE_SCALE)) {
            telemetry.addData("Slow", "A held and SLOW_MODE_SCALE invalid, drive sent 0");
        }
        if (gamepad2.left_bumper && gamepad2.left_trigger > Version1Config.TRIGGER_DEADBAND) {
            telemetry.addData("Intake", "in and out together, sent 0");
        }
        if (gamepad2.x && gamepad2.b) {
            telemetry.addData("Feed", "toward and reverse together, sent 0");
        }
        telemetry.addData("Sent drive", "FL %.2f FR %.2f BL %.2f BR %.2f", frontLeft, frontRight, backLeft, backRight);
        telemetry.addData("Sent mech", "in %.2f feed %.2f L1 %.2f L2 %.2f", intake, feed, launcher1, launcher2);
        telemetry.addData("Sticks", "y %.2f x %.2f yaw %.2f",
                gamepad1.left_stick_y, gamepad1.left_stick_x, gamepad1.right_stick_x);
        robot.addMapReport(telemetry);
        telemetry.update();
    }

    private double shapeStick(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        if (value > 1.0) {
            value = 1.0;
        }
        if (value < -1.0) {
            value = -1.0;
        }
        double deadband = Version1Config.STICK_DEADBAND;
        double magnitude = Math.abs(value);
        if (magnitude < deadband) {
            return 0.0;
        }
        return Math.copySign((magnitude - deadband) / (1.0 - deadband), value);
    }

    private double shapeTrigger(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        if (value > 1.0) {
            value = 1.0;
        }
        if (value < 0.0) {
            value = 0.0;
        }
        double deadband = Version1Config.TRIGGER_DEADBAND;
        if (value < deadband) {
            return 0.0;
        }
        return (value - deadband) / (1.0 - deadband);
    }
}
