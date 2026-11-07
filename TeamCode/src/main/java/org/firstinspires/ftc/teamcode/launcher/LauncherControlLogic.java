package org.firstinspires.ftc.teamcode.launcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Power-mode versus velocity-mode gate for the two launcher motors.
 * Hardware calls stay outside this class so the stop and toggle rules can be tested
 * without a hub.
 */
public final class LauncherControlLogic {
    public enum Mode {
        POWER,
        VELOCITY
    }

    public static final String DIRECTION_LINE =
            "Command polarity stays outtake1 positive and outtake2 negative, matching power mode. "
                    + "Encoder sign is that same SDK direction frame, including the motor type's CCW flip. "
                    + "It is not a measurement of which flywheel face contacts the game piece. "
                    + "TeleOp does not call setDirection on these motors.";

    public static final class Output {
        public final boolean ensureOpenLoop;
        public final boolean ensureClosedLoop;
        public final boolean closedLoop;
        public final double power1;
        public final double power2;
        public final double velocity1;
        public final double velocity2;

        public Output(boolean ensureOpenLoop, boolean ensureClosedLoop, boolean closedLoop,
                      double power1, double power2, double velocity1, double velocity2) {
            this.ensureOpenLoop = ensureOpenLoop;
            this.ensureClosedLoop = ensureClosedLoop;
            this.closedLoop = closedLoop;
            this.power1 = power1;
            this.power2 = power2;
            this.velocity1 = velocity1;
            this.velocity2 = velocity2;
        }
    }

    private Mode mode = Mode.POWER;
    private boolean prevY;
    private boolean yPrimed;
    private boolean seen1;
    private boolean seen2;
    private boolean suppressUntilRelease;
    private long stallSince1 = -1;
    private long stallSince2 = -1;
    private String status = "Power mode is selected. Velocity stays off until the gate passes and gamepad2 Y is tapped with the left trigger released.";
    private String hardwareBlock;
    private LauncherVelocityMath.MotorTypeSnapshot snapshot1;
    private LauncherVelocityMath.MotorTypeSnapshot snapshot2;
    private LauncherVelocityMath.ResolvedMotor motor1;
    private LauncherVelocityMath.ResolvedMotor motor2;
    private LauncherVelocityMath.Calibration calibration;
    private double lastMeasured1 = Double.NaN;
    private double lastMeasured2 = Double.NaN;
    private double lastCommand1;
    private double lastCommand2;

    public void configure(LauncherVelocityMath.MotorTypeSnapshot first,
                          LauncherVelocityMath.MotorTypeSnapshot second,
                          LauncherVelocityMath.Calibration cal,
                          String hardwareBlock) {
        this.snapshot1 = first;
        this.snapshot2 = second;
        this.calibration = cal;
        this.hardwareBlock = hardwareBlock;
        this.motor1 = LauncherVelocityMath.resolve(first, cal);
        this.motor2 = LauncherVelocityMath.resolve(second, cal);
    }

    public void primeToggle(boolean yHeld) {
        prevY = yHeld;
        yPrimed = true;
    }

    public Mode getMode() {
        return mode;
    }

    public String getStatus() {
        return status;
    }

    public boolean encodersSeen() {
        return seen1 && seen2;
    }

    public Output update(double trigger, boolean yDown, double requestedPower1, double requestedPower2,
                         double measuredTicks1, double measuredTicks2, long nowNanos) {
        if (calibration == null) {
            return openLoop(requestedPower1, requestedPower2);
        }
        lastMeasured1 = measuredTicks1;
        lastMeasured2 = measuredTicks2;
        if (LauncherVelocityMath.encoderMotion(measuredTicks1)) {
            seen1 = true;
        }
        if (LauncherVelocityMath.encoderMotion(measuredTicks2)) {
            seen2 = true;
        }
        boolean rising = yPrimed && yDown && !prevY;
        prevY = yDown;
        yPrimed = true;

        if (suppressUntilRelease) {
            if (LauncherVelocityMath.triggerReleased(trigger)) {
                suppressUntilRelease = false;
                status = "Left trigger released. Power mode is live again.";
            } else {
                return coastZero();
            }
        }

        if (rising && !LauncherVelocityMath.triggerReleased(trigger)) {
            status = "Mode change ignored. Release the left trigger, then tap gamepad2 Y.";
        } else if (rising) {
            if (mode == Mode.VELOCITY) {
                return leaveVelocity();
            }
            String block = velocityBlock();
            if (block != null) {
                status = "Staying in power mode. " + block;
            } else {
                mode = Mode.VELOCITY;
                stallSince1 = -1;
                stallSince2 = -1;
                lastCommand1 = 0;
                lastCommand2 = 0;
                status = "Velocity mode armed at 0. The left trigger scales both motors toward the test reference.";
                return new Output(false, true, true, 0, 0, 0, 0);
            }
        }

        if (mode == Mode.VELOCITY) {
            LauncherVelocityMath.VelocityCommand first = LauncherVelocityMath.command(
                    motor1, calibration, trigger, LauncherVelocityConfig.OUTTAKE1_COMMAND_SIGN);
            LauncherVelocityMath.VelocityCommand second = LauncherVelocityMath.command(
                    motor2, calibration, trigger, LauncherVelocityConfig.OUTTAKE2_COMMAND_SIGN);
            if (faultMatured(first, measuredTicks1, second, measuredTicks2, nowNanos)) {
                mode = Mode.POWER;
                suppressUntilRelease = true;
                stallSince1 = -1;
                stallSince2 = -1;
                lastCommand1 = 0;
                lastCommand2 = 0;
                status = "Velocity fault: encoder ticks stayed near 0 while a velocity was commanded. "
                        + "Launcher set to 0. Release the left trigger before power mode will follow it again.";
                return coastZero();
            }
            lastCommand1 = first.signedTicksPerSecond;
            lastCommand2 = second.signedTicksPerSecond;
            return new Output(false, false, true, 0, 0, first.signedTicksPerSecond, second.signedTicksPerSecond);
        }
        lastCommand1 = 0;
        lastCommand2 = 0;
        return openLoop(requestedPower1, requestedPower2);
    }

    public List<String> telemetryLines() {
        List<String> lines = new ArrayList<String>();
        if (calibration == null || motor1 == null || motor2 == null) {
            lines.add("Launcher telemetry unavailable until the motors are bound.");
            return lines;
        }
        String modeName = mode == Mode.VELOCITY ? "VELOCITY" : "POWER";
        lines.add("Launcher " + modeName
                + ". Toggle is gamepad2 Y on a rising edge, and only while the left trigger is released. Default is power mode.");
        lines.add(status);
        lines.add(LauncherVelocityMath.measuredLine(snapshot1.hardwareName, lastMeasured1, motor1, calibration));
        lines.add(LauncherVelocityMath.measuredLine(snapshot2.hardwareName, lastMeasured2, motor2, calibration));
        lines.add(LauncherVelocityMath.referenceLine(motor1, motor2, calibration));
        lines.add(LauncherVelocityMath.externalRatioLine(calibration));
        lines.add(DIRECTION_LINE);
        if (mode == Mode.VELOCITY) {
            lines.add(String.format(Locale.US,
                    "Commanded, not measured: outtake1 %.1f ticks/s, outtake2 %.1f ticks/s. The hub rounds each command to integer ticks/s.",
                    lastCommand1, lastCommand2));
        }
        String armature1 = LauncherVelocityMath.armatureLine(snapshot1.hardwareName, lastMeasured1, motor1);
        String armature2 = LauncherVelocityMath.armatureLine(snapshot2.hardwareName, lastMeasured2, motor2);
        if (armature1 != null) {
            lines.add(armature1);
        }
        if (armature2 != null) {
            lines.add(armature2);
        }
        lines.add(LauncherVelocityMath.configLine(snapshot1, motor1));
        lines.add(LauncherVelocityMath.configLine(snapshot2, motor2));
        return lines;
    }

    private Output leaveVelocity() {
        mode = Mode.POWER;
        stallSince1 = -1;
        stallSince2 = -1;
        lastCommand1 = 0;
        lastCommand2 = 0;
        status = "Launcher returned to power mode at 0.";
        return coastZero();
    }

    private Output coastZero() {
        lastCommand1 = 0;
        lastCommand2 = 0;
        return new Output(true, false, false, 0, 0, 0, 0);
    }

    private Output openLoop(double power1, double power2) {
        return new Output(false, false, false, power1, power2, 0, 0);
    }

    private boolean faultMatured(LauncherVelocityMath.VelocityCommand first, double measured1,
                                 LauncherVelocityMath.VelocityCommand second, double measured2,
                                 long nowNanos) {
        stallSince1 = advanceStall(stallSince1, LauncherVelocityMath.stallCondition(
                first.signedTicksPerSecond, measured1, first.fullScaleTicksPerSecond), nowNanos);
        stallSince2 = advanceStall(stallSince2, LauncherVelocityMath.stallCondition(
                second.signedTicksPerSecond, measured2, second.fullScaleTicksPerSecond), nowNanos);
        return matured(stallSince1, nowNanos) || matured(stallSince2, nowNanos);
    }

    private static long advanceStall(long since, boolean condition, long nowNanos) {
        if (!condition) {
            return -1;
        }
        if (since < 0) {
            return nowNanos;
        }
        return since;
    }

    private static boolean matured(long since, long nowNanos) {
        return since >= 0 && nowNanos - since >= LauncherVelocityMath.STALL_WINDOW_NANOS;
    }

    private String velocityBlock() {
        if (hardwareBlock != null && hardwareBlock.length() > 0) {
            return hardwareBlock;
        }
        StringBuilder block = new StringBuilder();
        append(block, motor1.velocityRejection);
        append(block, motor2.velocityRejection);
        if (LauncherVelocityMath.ratioProblem(calibration) != null) {
            append(block, LauncherVelocityMath.ratioProblem(calibration));
        }
        if (LauncherVelocityMath.referenceProblem(calibration) != null) {
            append(block, LauncherVelocityMath.referenceProblem(calibration));
        }
        if (!seen1 || !seen2) {
            append(block, "both encoders must show motion first. In power mode, rotate each flywheel until measured ticks/sec passes "
                    + String.format(Locale.US, "%.0f", LauncherVelocityMath.ENCODER_MOTION_TICKS_PER_SECOND)
                    + ". A dead encoder must not receive a velocity command.");
        }
        return block.length() == 0 ? null : block.toString();
    }

    private static void append(StringBuilder block, String sentence) {
        if (sentence == null || sentence.length() == 0) {
            return;
        }
        if (block.length() > 0) {
            block.append(' ');
        }
        block.append(sentence);
    }
}
