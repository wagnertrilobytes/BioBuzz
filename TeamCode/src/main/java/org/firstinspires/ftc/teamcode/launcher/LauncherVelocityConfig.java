package org.firstinspires.ftc.teamcode.launcher;

/**
 * Numbers the team must verify before launcher velocity mode can run.
 *
 * <p>Setup, the gamepad2 Y toggle, and the calibration procedure are in
 * {@code docs/launcher-velocity-control.md}.
 *
 * <p>This repository has no Robot Controller configuration file. Do not type a
 * motor name, ticks-per-revolution, or gear ratio in here from memory. Leave a
 * field as {@link Double#NaN} until it has been measured or copied from the
 * nameplate of the motor that is actually on {@code outtake1} and {@code outtake2}.
 * Velocity mode stays off while the required fields are unset, and TeleOp keeps
 * the existing open-loop power behavior.
 *
 * <p>{@link #TEST_REFERENCE_FLYWHEEL_SHAFT_RPM} is only a starting test target.
 * It is not a scoring speed and it is not a statement that the flywheel is safe
 * or able to reach that speed.
 */
public final class LauncherVelocityConfig {
    private LauncherVelocityConfig() {
    }

    /**
     * Command sign for {@code outtake1}. This matches the existing TeleOp line
     * {@code gamepad2.left_trigger * 1}. Do not change one without the other.
     */
    public static final int OUTTAKE1_COMMAND_SIGN = 1;

    /**
     * Command sign for {@code outtake2}. This matches the existing TeleOp line
     * {@code gamepad2.left_trigger * -1}. Do not change one without the other.
     */
    public static final int OUTTAKE2_COMMAND_SIGN = -1;

    /** Initial flywheel-shaft test reference, in RPM. Not a scoring or safety guarantee. */
    public static final double TEST_REFERENCE_FLYWHEEL_SHAFT_RPM = 1500.0;

    /**
     * Flywheel-shaft revolutions for each revolution of the motor output shaft.
     * Direct drive is 1.0 only after someone confirms the wheel is on that shaft.
     * A crossed belt is negative. {@link Double#NaN} means this is not configured,
     * flywheel RPM is not shown, and velocity mode stays off.
     */
    public static final double EXTERNAL_FLYWHEEL_REVS_PER_MOTOR_OUTPUT_REV = Double.NaN;

    /**
     * Encoder ticks per revolution of the motor output shaft, internal gearbox
     * included, external flywheel ratio not included. {@link Double#NaN} means
     * "use the Robot Controller motor type only when that type is one specific
     * gearbox." Series placeholders are never used as this number.
     */
    public static final double VERIFIED_TICKS_PER_MOTOR_OUTPUT_REV = Double.NaN;

    /** No-load motor output-shaft RPM. {@link Double#NaN} means it is not verified. */
    public static final double VERIFIED_MAX_MOTOR_OUTPUT_RPM = Double.NaN;

    /**
     * Share of {@link #VERIFIED_MAX_MOTOR_OUTPUT_RPM} that a closed-loop command
     * may use. The SDK's own specific motor types use 0.85, but this field does
     * not assume 0.85. Leave it {@link Double#NaN} until the limit is chosen.
     */
    public static final double VERIFIED_CLOSED_LOOP_RPM_FRACTION = Double.NaN;

    /**
     * Armature revolutions per motor-output revolution. Display only. It is not
     * used to command the motor and it is not the flywheel ratio.
     */
    public static final double VERIFIED_INTERNAL_GEARING = Double.NaN;

    public static LauncherVelocityMath.Calibration calibration() {
        return new LauncherVelocityMath.Calibration(
                TEST_REFERENCE_FLYWHEEL_SHAFT_RPM,
                EXTERNAL_FLYWHEEL_REVS_PER_MOTOR_OUTPUT_REV,
                VERIFIED_TICKS_PER_MOTOR_OUTPUT_REV,
                VERIFIED_MAX_MOTOR_OUTPUT_RPM,
                VERIFIED_CLOSED_LOOP_RPM_FRACTION,
                VERIFIED_INTERNAL_GEARING);
    }
}
