package org.firstinspires.ftc.teamcode.launcher;

import java.util.Locale;

/**
 * Converts SDK motor-type metadata and team calibration into RPM readings and
 * a clamped velocity command. Power is never turned into an RPM.
 *
 * <p>Motor-output RPM uses encoder ticks per second divided by ticks per motor
 * output revolution. Flywheel-shaft RPM multiplies that by the external ratio.
 * Those are different shafts whenever the flywheel is not 1:1 with the motor output.
 */
public final class LauncherVelocityMath {
    /** REV hub velocity command limit from LynxSetMotorTargetVelocityCommand in SDK 12.0.0. */
    public static final double HUB_VELOCITY_TICKS_PER_SECOND_LIMIT = 32767.0;

    /** Brisk shaft motion, in ticks/sec, that counts as "this encoder is alive." */
    public static final double ENCODER_MOTION_TICKS_PER_SECOND = 15.0;

    /** Left trigger at or above this is still "held" for the mode toggle. */
    public static final double TRIGGER_RELEASED_BELOW = 0.05;

    public static final long STALL_WINDOW_NANOS = 400000000L;

    private LauncherVelocityMath() {
    }

    public static final class Calibration {
        public final double testReferenceFlywheelRpm;
        public final double externalFlywheelRevsPerMotorOutputRev;
        public final double verifiedTicksPerMotorOutputRev;
        public final double verifiedMaxMotorOutputRpm;
        public final double verifiedClosedLoopFraction;
        public final double verifiedInternalGearing;

        public Calibration(double testReferenceFlywheelRpm,
                           double externalFlywheelRevsPerMotorOutputRev,
                           double verifiedTicksPerMotorOutputRev,
                           double verifiedMaxMotorOutputRpm,
                           double verifiedClosedLoopFraction,
                           double verifiedInternalGearing) {
            this.testReferenceFlywheelRpm = testReferenceFlywheelRpm;
            this.externalFlywheelRevsPerMotorOutputRev = externalFlywheelRevsPerMotorOutputRev;
            this.verifiedTicksPerMotorOutputRev = verifiedTicksPerMotorOutputRev;
            this.verifiedMaxMotorOutputRpm = verifiedMaxMotorOutputRpm;
            this.verifiedClosedLoopFraction = verifiedClosedLoopFraction;
            this.verifiedInternalGearing = verifiedInternalGearing;
        }
    }

    /** Values copied from {@code MotorConfigurationType} at init. They are not all safe to use. */
    public static final class MotorTypeSnapshot {
        public final String hardwareName;
        public final String sdkName;
        public final String xmlTag;
        public final double ticksPerRev;
        public final double maxRpm;
        public final double gearing;
        public final double closedLoopFraction;
        public final String orientation;

        public MotorTypeSnapshot(String hardwareName, String sdkName, String xmlTag,
                                 double ticksPerRev, double maxRpm, double gearing,
                                 double closedLoopFraction, String orientation) {
            this.hardwareName = hardwareName == null ? "" : hardwareName;
            this.sdkName = sdkName == null ? "" : sdkName;
            this.xmlTag = xmlTag == null ? "" : xmlTag;
            this.ticksPerRev = ticksPerRev;
            this.maxRpm = maxRpm;
            this.gearing = gearing;
            this.closedLoopFraction = closedLoopFraction;
            this.orientation = orientation == null ? "unknown" : orientation;
        }
    }

    public static final class ResolvedMotor {
        public final String hardwareName;
        public final boolean ticksUsable;
        public final boolean limitUsable;
        public final boolean gearingUsable;
        public final double ticksPerRev;
        public final double maxRpm;
        public final double closedLoopFraction;
        public final double internalGearing;
        public final String ticksSource;
        public final String limitSource;
        public final String gearingSource;
        public final String placeholderReason;
        public final String overrideWarning;
        public final String velocityRejection;

        public ResolvedMotor(String hardwareName, boolean ticksUsable, boolean limitUsable,
                             boolean gearingUsable, double ticksPerRev, double maxRpm,
                             double closedLoopFraction, double internalGearing,
                             String ticksSource, String limitSource, String gearingSource,
                             String placeholderReason, String overrideWarning,
                             String velocityRejection) {
            this.hardwareName = hardwareName;
            this.ticksUsable = ticksUsable;
            this.limitUsable = limitUsable;
            this.gearingUsable = gearingUsable;
            this.ticksPerRev = ticksPerRev;
            this.maxRpm = maxRpm;
            this.closedLoopFraction = closedLoopFraction;
            this.internalGearing = internalGearing;
            this.ticksSource = ticksSource;
            this.limitSource = limitSource;
            this.gearingSource = gearingSource;
            this.placeholderReason = placeholderReason;
            this.overrideWarning = overrideWarning;
            this.velocityRejection = velocityRejection;
        }
    }

    public static final class VelocityCommand {
        public final double signedTicksPerSecond;
        public final double fullScaleTicksPerSecond;
        public final double commandedMotorOutputRpm;
        public final double commandedFlywheelRpm;
        public final boolean referenceUnreachable;
        public final boolean hubLimited;
        public final String detail;

        public VelocityCommand(double signedTicksPerSecond, double fullScaleTicksPerSecond,
                               double commandedMotorOutputRpm, double commandedFlywheelRpm,
                               boolean referenceUnreachable, boolean hubLimited, String detail) {
            this.signedTicksPerSecond = signedTicksPerSecond;
            this.fullScaleTicksPerSecond = fullScaleTicksPerSecond;
            this.commandedMotorOutputRpm = commandedMotorOutputRpm;
            this.commandedFlywheelRpm = commandedFlywheelRpm;
            this.referenceUnreachable = referenceUnreachable;
            this.hubLimited = hubLimited;
            this.detail = detail == null ? "" : detail;
        }
    }

    /**
     * SDK 12.0.0 motor xml tags whose published ticks and max RPM belong to one
     * stand-in gearbox, not to every motor a team might have selected.
     * A later SDK type with its own xml tag is not on this list.
     */
    public static String placeholderReason(String xmlTag, String sdkName) {
        String tag = xmlTag == null ? "" : xmlTag;
        String name = sdkName == null ? "" : sdkName;
        if ("Motor".equals(tag) || "Unspecified Motor".equalsIgnoreCase(name)) {
            return "Unspecified Motor is a placeholder. SDK 12.0.0 fills it with historical Tetrix numbers, not this robot's motor.";
        }
        if ("goBILDA5202SeriesMotor".equals(tag)) {
            return "GoBILDA 5202/3/4 series is one dropdown for many gearboxes. SDK 12.0.0 stores the 60 RPM class numbers for that whole series.";
        }
        if ("goBILDA5201SeriesMotor".equals(tag)) {
            return "GoBILDA 5201 series is one dropdown for more than one spur ratio. Its stored ticks and max RPM are not this robot's gearbox.";
        }
        if ("RevRoboticsUltraplanetaryHDHexMotor".equals(tag)) {
            return "Ultraplanetary in SDK 12.0.0 stores a single cartridge ratio. The cartridges stacked on this motor are not in that entry.";
        }
        return null;
    }

    public static ResolvedMotor resolve(MotorTypeSnapshot sdk, Calibration cal) {
        String placeholder = placeholderReason(sdk.xmlTag, sdk.sdkName);
        String warning = overrideWarning(cal);
        boolean verifiedTicks = positiveFinite(cal.verifiedTicksPerMotorOutputRev);
        boolean verifiedMax = positiveFinite(cal.verifiedMaxMotorOutputRpm);
        boolean verifiedFraction = fractionUsable(cal.verifiedClosedLoopFraction);
        boolean verifiedGear = positiveFinite(cal.verifiedInternalGearing);

        double ticks = Double.NaN;
        String ticksSource = "none";
        if (verifiedTicks) {
            ticks = cal.verifiedTicksPerMotorOutputRev;
            ticksSource = "team verified";
        } else if (placeholder == null && positiveFinite(sdk.ticksPerRev)) {
            ticks = sdk.ticksPerRev;
            ticksSource = "SDK motor type";
        }

        double maxRpm = Double.NaN;
        double fraction = Double.NaN;
        String limitSource = "none";
        if (verifiedMax && verifiedFraction) {
            maxRpm = cal.verifiedMaxMotorOutputRpm;
            fraction = cal.verifiedClosedLoopFraction;
            limitSource = "team verified";
        } else if (placeholder == null && !verifiedMax && !hasNumber(cal.verifiedClosedLoopFraction)
                && positiveFinite(sdk.maxRpm) && fractionUsable(sdk.closedLoopFraction)) {
            maxRpm = sdk.maxRpm;
            fraction = sdk.closedLoopFraction;
            limitSource = "SDK motor type";
        } else if (verifiedMax && !verifiedFraction) {
            limitSource = "incomplete";
        }

        double gearing = Double.NaN;
        String gearingSource = "none";
        if (verifiedGear) {
            gearing = cal.verifiedInternalGearing;
            gearingSource = "team verified";
        } else if (placeholder == null && positiveFinite(sdk.gearing)) {
            gearing = sdk.gearing;
            gearingSource = "SDK motor type";
        }

        boolean ticksUsable = positiveFinite(ticks);
        boolean limitUsable = positiveFinite(maxRpm) && fractionUsable(fraction);
        boolean gearingUsable = positiveFinite(gearing);
        String rejection = null;
        if (!ticksUsable || !limitUsable) {
            if ("incomplete".equals(limitSource)) {
                rejection = sdk.hardwareName + ": verified max RPM is set but the closed-loop fraction is not a number from 0 to 1, excluding 0.";
            } else if (!ticksUsable && placeholder != null) {
                rejection = sdk.hardwareName + ": " + placeholder;
            } else if (!limitUsable && placeholder != null) {
                rejection = sdk.hardwareName + ": closed-loop RPM limit is not verified. " + placeholder;
            } else if (!ticksUsable && !limitUsable) {
                rejection = sdk.hardwareName + ": ticks per output revolution and the closed-loop RPM limit are not verified.";
            } else if (!ticksUsable) {
                rejection = sdk.hardwareName + ": ticks per output revolution are not verified.";
            } else {
                rejection = sdk.hardwareName + ": closed-loop RPM limit is not verified.";
            }
        }
        return new ResolvedMotor(sdk.hardwareName, ticksUsable, limitUsable, gearingUsable,
                ticks, maxRpm, fraction, gearing, ticksSource, limitSource, gearingSource,
                placeholder, warning, rejection);
    }

    public static VelocityCommand command(ResolvedMotor motor, Calibration cal, double trigger, int sign) {
        int direction = sign < 0 ? -1 : 1;
        double scale = trigger;
        if (scale < 0) {
            scale = 0;
        } else if (scale > 1) {
            scale = 1;
        }
        if (!motor.ticksUsable || !motor.limitUsable || ratioProblem(cal) != null || referenceProblem(cal) != null) {
            return new VelocityCommand(0, 0, 0, 0, false, false, "velocity command withheld");
        }
        double requestedMotorRpm = Math.abs(cal.testReferenceFlywheelRpm / cal.externalFlywheelRevsPerMotorOutputRev);
        double rpmCap = motor.maxRpm * motor.closedLoopFraction;
        boolean rpmShort = requestedMotorRpm > rpmCap + 0.05;
        double rpmAllowed = Math.min(requestedMotorRpm, rpmCap);
        double ticksForRpm = rpmAllowed / 60.0 * motor.ticksPerRev;
        boolean hubShort = ticksForRpm > HUB_VELOCITY_TICKS_PER_SECOND_LIMIT;
        double ticksAllowed = Math.min(ticksForRpm, HUB_VELOCITY_TICKS_PER_SECOND_LIMIT);
        double signedTicks = direction * scale * ticksAllowed;
        double signedMotorRpm = direction * (ticksAllowed / motor.ticksPerRev * 60.0) * scale;
        double signedFlywheel = signedMotorRpm * cal.externalFlywheelRevsPerMotorOutputRev;
        StringBuilder detail = new StringBuilder();
        if (rpmShort) {
            detail.append(String.format(Locale.US,
                    "needs %.1f motor-output RPM for %.0f flywheel RPM at external ratio %s; closed-loop limit is %.1f motor-output RPM (%.1f flywheel RPM)",
                    requestedMotorRpm, cal.testReferenceFlywheelRpm, formatRatio(cal.externalFlywheelRevsPerMotorOutputRev),
                    rpmCap, rpmCap * Math.abs(cal.externalFlywheelRevsPerMotorOutputRev)));
        }
        if (hubShort) {
            if (detail.length() > 0) {
                detail.append("; ");
            }
            detail.append(String.format(Locale.US,
                    "asks for %.0f ticks/s, above the REV hub limit of %.0f ticks/s",
                    ticksForRpm, HUB_VELOCITY_TICKS_PER_SECOND_LIMIT));
        }
        return new VelocityCommand(signedTicks, ticksAllowed, signedMotorRpm, signedFlywheel,
                rpmShort || hubShort, hubShort, detail.toString());
    }

    /** Measured motor-output RPM. Returns NaN when ticks/rev are not verified or the read is not finite. */
    public static double motorOutputRpm(double measuredTicksPerSecond, ResolvedMotor motor) {
        if (motor == null || !motor.ticksUsable || !Double.isFinite(measuredTicksPerSecond)) {
            return Double.NaN;
        }
        return measuredTicksPerSecond / motor.ticksPerRev * 60.0;
    }

    /** Measured flywheel-shaft RPM. Returns NaN until both the motor RPM and the external ratio exist. */
    public static double flywheelShaftRpm(double motorOutputRpm, double externalRatio) {
        if (!Double.isFinite(motorOutputRpm) || !ratioUsable(externalRatio)) {
            return Double.NaN;
        }
        return motorOutputRpm * externalRatio;
    }

    public static double armatureRpm(double motorOutputRpm, ResolvedMotor motor) {
        if (!Double.isFinite(motorOutputRpm) || motor == null || !motor.gearingUsable) {
            return Double.NaN;
        }
        return motorOutputRpm * motor.internalGearing;
    }

    public static boolean ratioUsable(double externalRatio) {
        return Double.isFinite(externalRatio) && externalRatio != 0.0;
    }

    public static String ratioProblem(Calibration cal) {
        double ratio = cal.externalFlywheelRevsPerMotorOutputRev;
        if (Double.isNaN(ratio)) {
            return "external flywheel ratio is not configured";
        }
        if (!ratioUsable(ratio)) {
            return "external flywheel ratio is not a finite non-zero number";
        }
        return null;
    }

    public static String referenceProblem(Calibration cal) {
        if (!positiveFinite(cal.testReferenceFlywheelRpm)) {
            return "test reference flywheel RPM is not a positive finite number";
        }
        return null;
    }

    public static String reachability(ResolvedMotor motor, Calibration cal) {
        if (motor == null || !motor.ticksUsable || !motor.limitUsable) {
            return "UNKNOWN (motor ticks/rev or closed-loop limit is not verified)";
        }
        if (ratioProblem(cal) != null) {
            return "UNKNOWN (external flywheel ratio is not verified)";
        }
        if (referenceProblem(cal) != null) {
            return "UNKNOWN (test reference is not a positive finite RPM)";
        }
        VelocityCommand atFullTrigger = command(motor, cal, 1.0, 1);
        if (atFullTrigger.referenceUnreachable) {
            return "UNREACHABLE (" + atFullTrigger.detail + ")";
        }
        return "within the configured closed-loop limit";
    }

    public static String configLine(MotorTypeSnapshot sdk, ResolvedMotor motor) {
        StringBuilder line = new StringBuilder();
        line.append(sdk.hardwareName)
                .append(" SDK type '").append(sdk.sdkName)
                .append("' [").append(sdk.xmlTag)
                .append("] orientation ").append(sdk.orientation)
                .append(". ");
        if (motor.placeholderReason != null) {
            line.append(motor.placeholderReason).append(" ");
            line.append("SDK lists ticks/rev ").append(formatNumber(sdk.ticksPerRev))
                    .append(", max RPM ").append(formatNumber(sdk.maxRpm))
                    .append(", gearing ").append(formatNumber(sdk.gearing))
                    .append(", fraction ").append(formatNumber(sdk.closedLoopFraction))
                    .append("; those values are not used for RPM or velocity. ");
        }
        if (motor.ticksUsable) {
            line.append("Using ticks/rev ").append(formatNumber(motor.ticksPerRev))
                    .append(" (").append(motor.ticksSource).append("). ");
        } else {
            line.append("Ticks/rev not used. ");
        }
        if (motor.limitUsable) {
            line.append("Using max output RPM ").append(formatNumber(motor.maxRpm))
                    .append(" times closed-loop fraction ").append(formatNumber(motor.closedLoopFraction))
                    .append(" (").append(motor.limitSource).append("). ");
        } else {
            line.append("Closed-loop limit not used. ");
        }
        if (motor.overrideWarning != null) {
            line.append(motor.overrideWarning);
        }
        return line.toString();
    }

    public static String measuredLine(String hardwareName, double measuredTicksPerSecond,
                                      ResolvedMotor motor, Calibration cal) {
        double motorRpm = motorOutputRpm(measuredTicksPerSecond, motor);
        double wheelRpm = flywheelShaftRpm(motorRpm, cal.externalFlywheelRevsPerMotorOutputRev);
        String motorWhy = motor.ticksUsable ? "" : "ticks/rev not verified";
        String wheelWhy;
        if (!Double.isFinite(motorRpm)) {
            wheelWhy = "motor output RPM unavailable";
        } else if (ratioProblem(cal) != null) {
            wheelWhy = ratioProblem(cal);
        } else {
            wheelWhy = "";
        }
        return hardwareName
                + " measured ticks/s " + formatMeasurement(measuredTicksPerSecond)
                + " | motor output RPM " + formatRpm(motorRpm, motorWhy)
                + " | flywheel shaft RPM " + formatRpm(wheelRpm, wheelWhy);
    }

    public static String armatureLine(String hardwareName, double measuredTicksPerSecond, ResolvedMotor motor) {
        double motorRpm = motorOutputRpm(measuredTicksPerSecond, motor);
        double armature = armatureRpm(motorRpm, motor);
        if (!Double.isFinite(armature) || Math.abs(motorRpm) < 0.5) {
            return null;
        }
        return hardwareName + " armature RPM " + formatNumber(armature)
                + " = motor output RPM times internal gearing " + formatNumber(motor.internalGearing)
                + " (" + motor.gearingSource + "). Not flywheel RPM and not a second encoder.";
    }

    public static String externalRatioLine(Calibration cal) {
        String problem = ratioProblem(cal);
        if (problem != null) {
            return "External gear ratio: NOT CONFIGURED. Flywheel shaft RPM is not reported. " + problem + ".";
        }
        return "External gear ratio: " + formatRatio(cal.externalFlywheelRevsPerMotorOutputRev)
                + " flywheel-shaft revolutions per motor-output revolution (team configured). "
                + "Flywheel RPM = motor output RPM x this ratio, sign included.";
    }

    public static String referenceLine(ResolvedMotor first, ResolvedMotor second, Calibration cal) {
        String shown = positiveFinite(cal.testReferenceFlywheelRpm)
                ? String.format(Locale.US, "%.0f", cal.testReferenceFlywheelRpm)
                : "invalid";
        return "Test reference " + shown
                + " flywheel-shaft RPM is an initial test reference, not a scoring speed and not a safety guarantee. "
                + first.hardwareName + ": " + reachability(first, cal) + ". "
                + second.hardwareName + ": " + reachability(second, cal) + ".";
    }

    public static double stallCommandThreshold(double fullScaleTicksPerSecond) {
        double full = Math.abs(fullScaleTicksPerSecond);
        if (!Double.isFinite(full) || full <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        if (full < 50.0) {
            return 0.5 * full;
        }
        return Math.max(50.0, 0.25 * full);
    }

    public static boolean stallCondition(double commandedTicks, double measuredTicks, double fullScaleTicks) {
        double trip = stallCommandThreshold(fullScaleTicks);
        if (!Double.isFinite(trip) || Math.abs(commandedTicks) < trip) {
            return false;
        }
        if (!Double.isFinite(measuredTicks)) {
            return true;
        }
        double nearZero = Math.min(ENCODER_MOTION_TICKS_PER_SECOND, Math.abs(commandedTicks) * 0.1);
        return Math.abs(measuredTicks) < nearZero;
    }

    public static boolean encoderMotion(double measuredTicksPerSecond) {
        return Double.isFinite(measuredTicksPerSecond)
                && Math.abs(measuredTicksPerSecond) >= ENCODER_MOTION_TICKS_PER_SECOND;
    }

    public static boolean triggerReleased(double trigger) {
        return trigger < TRIGGER_RELEASED_BELOW;
    }

    private static String overrideWarning(Calibration cal) {
        StringBuilder warning = new StringBuilder();
        warn(warning, "VERIFIED_TICKS_PER_MOTOR_OUTPUT_REV", cal.verifiedTicksPerMotorOutputRev, true);
        warn(warning, "VERIFIED_MAX_MOTOR_OUTPUT_RPM", cal.verifiedMaxMotorOutputRpm, true);
        warn(warning, "VERIFIED_CLOSED_LOOP_RPM_FRACTION", cal.verifiedClosedLoopFraction, false);
        warn(warning, "VERIFIED_INTERNAL_GEARING", cal.verifiedInternalGearing, true);
        if (!Double.isNaN(cal.externalFlywheelRevsPerMotorOutputRev) && !ratioUsable(cal.externalFlywheelRevsPerMotorOutputRev)) {
            warning.append("EXTERNAL_FLYWHEEL_REVS_PER_MOTOR_OUTPUT_REV is set but is not a finite non-zero number, so it is ignored. ");
        }
        if (!Double.isNaN(cal.testReferenceFlywheelRpm) && !positiveFinite(cal.testReferenceFlywheelRpm)) {
            warning.append("TEST_REFERENCE_FLYWHEEL_SHAFT_RPM is not a positive finite number. ");
        }
        return warning.length() == 0 ? null : warning.toString();
    }

    private static void warn(StringBuilder warning, String field, double value, boolean positive) {
        if (Double.isNaN(value)) {
            return;
        }
        boolean ok = positive ? positiveFinite(value) : fractionUsable(value);
        if (!ok) {
            warning.append(field).append(" is set but is not usable, so it is ignored. ");
        }
    }

    private static boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0;
    }

    private static boolean hasNumber(double value) {
        return !Double.isNaN(value);
    }

    private static boolean fractionUsable(double value) {
        return Double.isFinite(value) && value > 0 && value <= 1.0;
    }

    private static String formatRpm(double rpm, String whyUnavailable) {
        if (Double.isFinite(rpm)) {
            return String.format(Locale.US, "%.1f", rpm);
        }
        if (whyUnavailable == null || whyUnavailable.length() == 0) {
            return "unavailable";
        }
        return "unavailable (" + whyUnavailable + ")";
    }

    private static String formatMeasurement(double ticksPerSecond) {
        if (!Double.isFinite(ticksPerSecond)) {
            return "not read";
        }
        return String.format(Locale.US, "%.1f", ticksPerSecond);
    }

    public static String formatNumber(double value) {
        if (!Double.isFinite(value)) {
            return "n/a";
        }
        return String.format(Locale.US, "%.4f", value);
    }

    private static String formatRatio(double value) {
        return String.format(Locale.US, "%.4f", value);
    }
}
