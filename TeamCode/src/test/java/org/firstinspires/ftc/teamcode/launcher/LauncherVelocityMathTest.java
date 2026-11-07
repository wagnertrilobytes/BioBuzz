package org.firstinspires.ftc.teamcode.launcher;

import java.util.List;

/**
 * Host-side checks for the launcher gate. This does not compile into the robot
 * app and it does not talk to hardware. Run it with javac and java from the
 * TeamCode tree.
 */
public final class LauncherVelocityMathTest {
    private static int failures;

    public static void main(String[] args) {
        defaultsStayLocked();
        placeholdersDoNotInventRpm();
        specificMotorCanReachReference();
        slowMotorFlagsUnreachableAndCaps();
        missingExternalRatioBlocksFlywheelRpm();
        verifiedOverrideReplacesSeriesNumbers();
        invalidOverrideIsIgnored();
        triggerScalesAndKeepsOppositeSigns();
        negativeRatioFlipsFlywheelButNotMotorCommand();
        hubLimitCapsTicks();
        toggleRequiresReleaseEncodersAndThenZeros();
        heldToggleAtStartDoesNothing();
        stallReturnsToZeroUntilTriggerReleased();
        powerIsNotReportedAsRpm();
        if (failures == 0) {
            System.out.println("LauncherVelocityMathTest: all checks passed");
            return;
        }
        System.out.println("LauncherVelocityMathTest: " + failures + " failed");
        System.exit(1);
    }

    private static void defaultsStayLocked() {
        LauncherVelocityMath.Calibration cal = LauncherVelocityConfig.calibration();
        expect(cal.testReferenceFlywheelRpm == 1500.0, "default reference is 1500");
        expect(Double.isNaN(cal.externalFlywheelRevsPerMotorOutputRev), "external ratio unset");
        expect(Double.isNaN(cal.verifiedTicksPerMotorOutputRev), "verified ticks unset");
        expect(Double.isNaN(cal.verifiedMaxMotorOutputRpm), "verified max unset");
        expect(Double.isNaN(cal.verifiedClosedLoopFraction), "verified fraction unset");
        expect(Double.isNaN(cal.verifiedInternalGearing), "verified gearing unset");
        expect(LauncherVelocityConfig.OUTTAKE1_COMMAND_SIGN == 1, "outtake1 sign");
        expect(LauncherVelocityConfig.OUTTAKE2_COMMAND_SIGN == -1, "outtake2 sign");
        LauncherControlLogic logic = logicFor(goBilda5202(), goBilda5202(), cal);
        logic.primeToggle(false);
        LauncherControlLogic.Output output = logic.update(0, true, 0, 0, 100, 100, 0);
        expect(logic.getMode() == LauncherControlLogic.Mode.POWER, "defaults stay in power mode");
        expect(!output.closedLoop && !output.ensureClosedLoop, "defaults do not command velocity");
        String reference = lineStarting(logic, "Test reference");
        expect(reference.contains("UNKNOWN"), "1500 reachability is unknown without a real limit");
        expect(!reference.contains("UNREACHABLE"), "placeholder 60 RPM must not be reported as 1500 unreachable");
    }

    private static void placeholdersDoNotInventRpm() {
        assertPlaceholderRejected(unspecified());
        assertPlaceholderRejected(goBilda5202());
        assertPlaceholderRejected(goBilda5201());
        assertPlaceholderRejected(ultraplanetary());
    }

    private static void assertPlaceholderRejected(LauncherVelocityMath.MotorTypeSnapshot type) {
        LauncherVelocityMath.Calibration cal = LauncherVelocityConfig.calibration();
        LauncherControlLogic logic = logicFor(type, type, cal);
        logic.primeToggle(false);
        logic.update(0, false, 0, 0, 1000, 1000, 0);
        String measured = lineStarting(logic, type.hardwareName + " measured");
        expect(measured.contains("motor output RPM unavailable"), type.xmlTag + " hides motor RPM");
        expect(measured.contains("flywheel shaft RPM unavailable"), type.xmlTag + " hides flywheel RPM");
        expect(!measured.contains("41.7") && !measured.contains("21.5") && !measured.contains("8.3"),
                type.xmlTag + " measured line does not contain a converted RPM");
        String config = lineStarting(logic, type.hardwareName + " SDK type");
        expect(config.contains("not used"), type.xmlTag + " config says SDK numbers are not used");
    }

    private static void specificMotorCanReachReference() {
        LauncherVelocityMath.Calibration cal = calibration(1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        LauncherVelocityMath.ResolvedMotor motor = LauncherVelocityMath.resolve(neveRest37(), cal);
        expect(motor.ticksUsable && motor.limitUsable, "specific SDK type is usable");
        expect(Math.abs(motor.ticksPerRev - 44.4) < 1e-9, "uses SDK ticks");
        String reach = LauncherVelocityMath.reachability(motor, cal);
        expect(reach.contains("within the configured closed-loop limit"), "1500 fits NeveRest 3.7 closed-loop limit");
        LauncherVelocityMath.VelocityCommand command = LauncherVelocityMath.command(motor, cal, 1, 1);
        expect(!command.referenceUnreachable, "3.7 command is not flagged unreachable");
        expect(Math.abs(command.fullScaleTicksPerSecond - 1110.0) < 1e-6, "full-scale ticks for 1500 RPM");
    }

    private static void slowMotorFlagsUnreachableAndCaps() {
        LauncherVelocityMath.Calibration cal = calibration(1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        LauncherVelocityMath.ResolvedMotor motor = LauncherVelocityMath.resolve(neveRest60(), cal);
        String reach = LauncherVelocityMath.reachability(motor, cal);
        expect(reach.contains("UNREACHABLE"), "1500 is unreachable on the NeveRest 60 limit");
        expect(reach.contains("89.3"), "cap is 105 * 0.85 motor RPM");
        LauncherVelocityMath.VelocityCommand command = LauncherVelocityMath.command(motor, cal, 1, -1);
        expect(command.referenceUnreachable, "command records unreachable");
        expect(Math.abs(command.fullScaleTicksPerSecond - 2499.0) < 1e-6, "command is capped at the closed-loop ticks");
        expect(command.signedTicksPerSecond < 0, "negative sign is preserved on the capped command");
        expect(Math.abs(command.signedTicksPerSecond) <= command.fullScaleTicksPerSecond + 1e-6, "does not command past the cap");
    }

    private static void missingExternalRatioBlocksFlywheelRpm() {
        LauncherVelocityMath.Calibration cal = LauncherVelocityConfig.calibration();
        LauncherControlLogic logic = logicFor(neveRest37(), neveRest37(), cal);
        logic.primeToggle(false);
        logic.update(1, false, 1, -1, 44.4, -44.4, 0);
        String measured = lineStarting(logic, "outtake1 measured");
        expect(measured.contains("motor output RPM 60.0"), "motor RPM is available from a specific type");
        expect(measured.contains("flywheel shaft RPM unavailable"), "flywheel RPM stays blank without an external ratio");
        LauncherControlLogic.Output blocked = logic.update(0, true, 0, 0, 44.4, -44.4, 1);
        expect(logic.getMode() == LauncherControlLogic.Mode.POWER, "velocity stays off without the external ratio");
        expect(!blocked.closedLoop, "no closed loop without the external ratio");
        expect(logic.getStatus().contains("external flywheel ratio"), "status names the missing ratio");
    }

    private static void verifiedOverrideReplacesSeriesNumbers() {
        LauncherVelocityMath.Calibration cal = calibration(1.0, 384.0, 435.0, 0.85, Double.NaN);
        LauncherControlLogic logic = logicFor(goBilda5202(), goBilda5202(), cal);
        logic.primeToggle(false);
        logic.update(0, false, 0, 0, 384, -384, 0);
        String measured = lineStarting(logic, "outtake1 measured");
        expect(measured.contains("motor output RPM 60.0"), "override ticks produce 60 RPM at 384 ticks/s");
        expect(measured.contains("flywheel shaft RPM 60.0"), "external 1.0 reports the same shaft RPM");
        expect(!measured.contains("8.3"), "series ticks/rev of 2786 are not used");
        String reach = lineStarting(logic, "Test reference");
        expect(reach.contains("UNREACHABLE"), "435 RPM class cannot hold 1500 at fraction 0.85");
        LauncherVelocityMath.VelocityCommand command = LauncherVelocityMath.command(
                LauncherVelocityMath.resolve(goBilda5202(), cal), cal, 1, 1);
        expect(Math.abs(command.fullScaleTicksPerSecond - 2366.4) < 1e-4, "cap uses 384 ticks/rev, not 2786");
    }

    private static void invalidOverrideIsIgnored() {
        LauncherVelocityMath.Calibration cal = calibration(1.0, -5, Double.NaN, Double.NaN, Double.NaN);
        LauncherVelocityMath.ResolvedMotor motor = LauncherVelocityMath.resolve(neveRest37(), cal);
        expect(Math.abs(motor.ticksPerRev - 44.4) < 1e-9, "negative verified ticks are ignored");
        expect(motor.overrideWarning != null && motor.overrideWarning.contains("ignored"), "invalid override is flagged");
    }

    private static void triggerScalesAndKeepsOppositeSigns() {
        LauncherVelocityMath.Calibration cal = calibration(2.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        LauncherControlLogic logic = armed(neveRest37(), neveRest37(), cal);
        LauncherControlLogic.Output half = logic.update(0.5, true, 0.5, -0.5, 100, -100, 5_000_000L);
        expect(half.closedLoop && !half.ensureClosedLoop, "steady velocity does not redo the mode change");
        expect(Math.abs(half.velocity1 - 277.5) < 1e-6, "trigger 0.5 scales the 2:1 motor command");
        expect(Math.abs(half.velocity2 + 277.5) < 1e-6, "outtake2 stays the opposite sign");
        String reach = lineStarting(logic, "Test reference");
        expect(reach.contains("within the configured closed-loop limit"), "external 2:1 makes 1500 a 750 motor-RPM ask");
        LauncherControlLogic.Output stopped = logic.update(0, true, 0, 0, 100, -100, 6_000_000L);
        expect(stopped.velocity1 == 0 && stopped.velocity2 == 0, "released trigger commands zero velocity");
    }

    private static void negativeRatioFlipsFlywheelButNotMotorCommand() {
        LauncherVelocityMath.Calibration cal = calibration(-1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        LauncherVelocityMath.ResolvedMotor motor = LauncherVelocityMath.resolve(neveRest37(), cal);
        double motorRpm = LauncherVelocityMath.motorOutputRpm(44.4, motor);
        double wheel = LauncherVelocityMath.flywheelShaftRpm(motorRpm, cal.externalFlywheelRevsPerMotorOutputRev);
        expect(Math.abs(motorRpm - 60.0) < 1e-9, "motor RPM stays positive with the encoder");
        expect(Math.abs(wheel + 60.0) < 1e-9, "negative external ratio flips the flywheel sign");
        LauncherVelocityMath.VelocityCommand command = LauncherVelocityMath.command(motor, cal, 1, 1);
        expect(command.signedTicksPerSecond > 0, "motor command sign does not follow the belt sign");
        expect(command.commandedFlywheelRpm < 0, "commanded flywheel RPM shows the belt sign");
    }

    private static void hubLimitCapsTicks() {
        LauncherVelocityMath.MotorTypeSnapshot fast = new LauncherVelocityMath.MotorTypeSnapshot(
                "outtake1", "High count", "HighCountMotor", 2000, 10000, 1, 1, "CW");
        LauncherVelocityMath.Calibration cal = calibration(1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        LauncherVelocityMath.VelocityCommand command = LauncherVelocityMath.command(
                LauncherVelocityMath.resolve(fast, cal), cal, 1, 1);
        expect(command.hubLimited && command.referenceUnreachable, "hub tick limit makes 1500 unreachable");
        expect(Math.abs(command.fullScaleTicksPerSecond - 32767.0) < 1e-9, "command stops at 32767 ticks/s");
    }

    private static void toggleRequiresReleaseEncodersAndThenZeros() {
        LauncherVelocityMath.Calibration cal = calibration(1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        LauncherControlLogic logic = logicFor(neveRest37(), neveRest37(), cal);
        logic.primeToggle(false);
        LauncherControlLogic.Output held = logic.update(0.8, true, 0.8, -0.8, 0, 0, 0);
        expect(logic.getMode() == LauncherControlLogic.Mode.POWER, "held trigger does not toggle");
        expect(held.power1 == 0.8 && held.power2 == -0.8, "power mode still applies the existing powers");
        expect(logic.getStatus().contains("Release the left trigger"), "driver is told to release");

        logic.update(0, false, 0, 0, 0, 0, 1);
        LauncherControlLogic.Output noMotion = logic.update(0, true, 0, 0, 0, 0, 2);
        expect(logic.getMode() == LauncherControlLogic.Mode.POWER, "no encoder motion blocks velocity");
        expect(logic.getStatus().contains("both encoders"), "status asks for encoder motion");
        expect(!noMotion.closedLoop, "blocked toggle stays open loop");

        logic.update(0, false, 0, 0, 0, 0, 3);
        logic.update(0, false, 0, 0, 20, -20, 4);
        expect(logic.encodersSeen(), "both encoders recorded motion");
        LauncherControlLogic.Output armed = logic.update(0, true, 0, 0, 20, -20, 5);
        expect(logic.getMode() == LauncherControlLogic.Mode.VELOCITY, "released trigger plus motion enters velocity");
        expect(armed.ensureClosedLoop, "entry selects closed loop");
        expect(armed.velocity1 == 0 && armed.velocity2 == 0, "entry commands zero");

        LauncherControlLogic.Output left = logic.update(0, false, 0, 0, 20, -20, 5);
        expect(logic.getMode() == LauncherControlLogic.Mode.VELOCITY, "releasing Y does not leave velocity");
        expect(left.closedLoop, "still closed loop");
        LauncherControlLogic.Output back = logic.update(0, true, 0.04, -0.04, 20, -20, 6);
        expect(logic.getMode() == LauncherControlLogic.Mode.POWER, "second rising edge returns to power");
        expect(back.ensureOpenLoop, "exit forces open loop");
        expect(back.power1 == 0 && back.power2 == 0, "exit zeros even if the trigger is slightly off zero");
    }

    private static void heldToggleAtStartDoesNothing() {
        LauncherControlLogic logic = logicFor(neveRest37(), neveRest37(),
                calibration(1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        logic.primeToggle(true);
        logic.update(0, true, 0, 0, 40, -40, 0);
        expect(logic.getMode() == LauncherControlLogic.Mode.POWER, "Y held across start is not a rising edge");
    }

    private static void stallReturnsToZeroUntilTriggerReleased() {
        LauncherControlLogic logic = armed(neveRest37(), neveRest37(),
                calibration(1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        long start = 1_000_000L;
        LauncherControlLogic.Output spinning = logic.update(1, true, 1, -1, 0, 0, start);
        expect(logic.getMode() == LauncherControlLogic.Mode.VELOCITY, "stall window has not elapsed");
        expect(spinning.closedLoop && !spinning.ensureOpenLoop, "first stalled sample still commands velocity");
        logic.update(1, true, 1, -1, 0, 0, start + LauncherVelocityMath.STALL_WINDOW_NANOS - 1);
        expect(logic.getMode() == LauncherControlLogic.Mode.VELOCITY, "399ms does not fault");
        LauncherControlLogic.Output fault = logic.update(1, true, 1, -1, 0, 0,
                start + LauncherVelocityMath.STALL_WINDOW_NANOS);
        expect(logic.getMode() == LauncherControlLogic.Mode.POWER, "400ms of dead encoder leaves velocity");
        expect(fault.ensureOpenLoop && fault.power1 == 0 && fault.power2 == 0, "fault output is zero");
        LauncherControlLogic.Output held = logic.update(1, true, 1, -1, 0, 0,
                start + LauncherVelocityMath.STALL_WINDOW_NANOS + 50_000_000L);
        expect(held.ensureOpenLoop && held.power1 == 0 && held.power2 == 0, "trigger held after a fault does not restore power");
        LauncherControlLogic.Output released = logic.update(0, true, 0, 0, 0, 0,
                start + LauncherVelocityMath.STALL_WINDOW_NANOS + 80_000_000L);
        expect(!released.ensureOpenLoop && !released.closedLoop, "release returns to ordinary power mode");
        LauncherControlLogic.Output again = logic.update(1, true, 1, -1, 0, 0,
                start + LauncherVelocityMath.STALL_WINDOW_NANOS + 90_000_000L);
        expect(again.power1 == 1 && again.power2 == -1, "power mode follows the trigger after release");
    }

    private static void powerIsNotReportedAsRpm() {
        LauncherVelocityMath.Calibration cal = calibration(1.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        LauncherControlLogic logic = logicFor(neveRest37(), neveRest37(), cal);
        logic.primeToggle(false);
        LauncherControlLogic.Output output = logic.update(1, false, 1, -1, 0, 0, 0);
        expect(output.power1 == 1 && output.power2 == -1, "open-loop powers pass through");
        expect(!output.closedLoop, "power mode is not closed loop");
        String measured = lineStarting(logic, "outtake1 measured");
        expect(measured.contains("ticks/s 0.0"), "zero encoder reads as zero ticks");
        expect(measured.contains("motor output RPM 0.0"), "zero ticks are zero motor RPM");
        expect(measured.contains("flywheel shaft RPM 0.0"), "zero ticks are zero flywheel RPM");
        expect(!measured.contains("1500"), "power 1 is not displayed as the 1500 RPM reference");
    }

    private static LauncherControlLogic armed(LauncherVelocityMath.MotorTypeSnapshot first,
                                              LauncherVelocityMath.MotorTypeSnapshot second,
                                              LauncherVelocityMath.Calibration cal) {
        LauncherControlLogic logic = logicFor(first, second, cal);
        logic.primeToggle(false);
        logic.update(0, false, 0, 0, 20, -20, 0);
        LauncherControlLogic.Output entered = logic.update(0, true, 0, 0, 20, -20, 1);
        if (logic.getMode() != LauncherControlLogic.Mode.VELOCITY || !entered.ensureClosedLoop) {
            failures++;
            System.out.println("FAIL setup could not arm velocity mode: " + logic.getStatus());
        }
        return logic;
    }

    private static LauncherControlLogic logicFor(LauncherVelocityMath.MotorTypeSnapshot first,
                                                 LauncherVelocityMath.MotorTypeSnapshot second,
                                                 LauncherVelocityMath.Calibration cal) {
        LauncherControlLogic logic = new LauncherControlLogic();
        logic.configure(named("outtake1", first), named("outtake2", second), cal, null);
        return logic;
    }

    private static LauncherVelocityMath.MotorTypeSnapshot named(String hardwareName,
                                                                LauncherVelocityMath.MotorTypeSnapshot source) {
        return new LauncherVelocityMath.MotorTypeSnapshot(hardwareName, source.sdkName, source.xmlTag,
                source.ticksPerRev, source.maxRpm, source.gearing, source.closedLoopFraction, source.orientation);
    }

    private static LauncherVelocityMath.Calibration calibration(double ratio, double ticks, double maxRpm,
                                                                double fraction, double gearing) {
        return new LauncherVelocityMath.Calibration(1500, ratio, ticks, maxRpm, fraction, gearing);
    }

    private static LauncherVelocityMath.MotorTypeSnapshot unspecified() {
        return new LauncherVelocityMath.MotorTypeSnapshot(
                "outtake1", "Unspecified Motor", "Motor", 1440, 165, 52, 0.85, "CW");
    }

    private static LauncherVelocityMath.MotorTypeSnapshot goBilda5202() {
        return new LauncherVelocityMath.MotorTypeSnapshot(
                "outtake1", "GoBILDA 5202/3/4 series", "goBILDA5202SeriesMotor", 2786, 60, 99.5, 0.85, "CCW");
    }

    private static LauncherVelocityMath.MotorTypeSnapshot goBilda5201() {
        return new LauncherVelocityMath.MotorTypeSnapshot(
                "outtake1", "GoBILDA 5201 series", "goBILDA5201SeriesMotor", 1500, 104, 54, 0.85, "CCW");
    }

    private static LauncherVelocityMath.MotorTypeSnapshot ultraplanetary() {
        return new LauncherVelocityMath.MotorTypeSnapshot(
                "outtake1", "Ultraplanetary", "RevRoboticsUltraplanetaryHDHexMotor", 560, 300, 20, 0.85, "CW");
    }

    private static LauncherVelocityMath.MotorTypeSnapshot neveRest37() {
        return new LauncherVelocityMath.MotorTypeSnapshot(
                "outtake1", "NeveRest 3.7 v1 Gearmotor", "NeveRest3.7v1Gearmotor", 44.4, 1784, 3.7, 0.85, "CCW");
    }

    private static LauncherVelocityMath.MotorTypeSnapshot neveRest60() {
        return new LauncherVelocityMath.MotorTypeSnapshot(
                "outtake1", "NeveRest 60 Gearmotor", "NeveRest60Gearmotor", 1680, 105, 60, 0.85, "CCW");
    }

    private static String lineStarting(LauncherControlLogic logic, String prefix) {
        List<String> lines = logic.telemetryLines();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                return lines.get(i);
            }
        }
        failures++;
        System.out.println("FAIL missing telemetry line: " + prefix);
        return "";
    }

    private static void expect(boolean condition, String label) {
        if (!condition) {
            failures++;
            System.out.println("FAIL " + label);
        }
    }
}
