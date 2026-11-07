package org.firstinspires.ftc.teamcode.launcher;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.List;

/**
 * Binds {@code outtake1} and {@code outtake2} to the velocity gate.
 *
 * <p>Power mode writes the same open-loop powers TeleOp already computed.
 * Velocity mode uses {@link DcMotorEx#setVelocity(double)} in ticks per second
 * and {@link DcMotor.RunMode#RUN_USING_ENCODER}. RPM on the driver station comes
 * from {@link DcMotorEx#getVelocity()} and verified ticks per revolution, never
 * from the trigger power.
 */
public final class LauncherVelocityControl {
    private final LauncherControlLogic logic = new LauncherControlLogic();
    private DcMotor motor1;
    private DcMotor motor2;
    private DcMotorEx ex1;
    private DcMotorEx ex2;

    public void bind(DcMotor first, DcMotor second) {
        motor1 = first;
        motor2 = second;
        ex1 = first instanceof DcMotorEx ? (DcMotorEx) first : null;
        ex2 = second instanceof DcMotorEx ? (DcMotorEx) second : null;
        // Keep the SDK default direction. Do not call setDirection here.
        // RUN_WITHOUT_ENCODER is the open-loop mode setPower already depends on.
        coast(motor1);
        coast(motor2);
        String hardwareBlock = null;
        if (ex1 == null || ex2 == null) {
            hardwareBlock = "Velocity mode is unavailable because outtake1 or outtake2 is not a DcMotorEx.";
        }
        logic.configure(snapshot("outtake1", motor1), snapshot("outtake2", motor2),
                LauncherVelocityConfig.calibration(), hardwareBlock);
    }

    public void primeToggle(boolean yHeld) {
        logic.primeToggle(yHeld);
    }

    public void command(double trigger, boolean yDown, double power1, double power2) {
        LauncherControlLogic.Output output = logic.update(
                trigger,
                yDown,
                power1,
                power2,
                readTicks(ex1),
                readTicks(ex2),
                System.nanoTime());
        apply(output);
    }

    public void addTelemetry(Telemetry telemetry) {
        List<String> lines = logic.telemetryLines();
        for (int i = 0; i < lines.size(); i++) {
            telemetry.addLine(lines.get(i));
        }
    }

    public void stopSafe() {
        if (ex1 != null) {
            ex1.setVelocity(0);
        }
        if (ex2 != null) {
            ex2.setVelocity(0);
        }
        coast(motor1);
        coast(motor2);
    }

    private void apply(LauncherControlLogic.Output output) {
        if (output.ensureOpenLoop) {
            // setVelocity after RUN_WITHOUT_ENCODER would throw the hub back into
            // RUN_USING_ENCODER, so the velocity write has to happen first.
            if (ex1 != null) {
                ex1.setVelocity(0);
            }
            if (ex2 != null) {
                ex2.setVelocity(0);
            }
            coast(motor1);
            coast(motor2);
            return;
        }
        if (output.ensureClosedLoop) {
            if (motor1 != null) {
                motor1.setPower(0);
            }
            if (motor2 != null) {
                motor2.setPower(0);
            }
            if (motor1 != null) {
                motor1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            }
            if (motor2 != null) {
                motor2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            }
            if (ex1 != null) {
                ex1.setVelocity(output.velocity1);
            }
            if (ex2 != null) {
                ex2.setVelocity(output.velocity2);
            }
            return;
        }
        if (output.closedLoop) {
            if (ex1 != null) {
                ex1.setVelocity(output.velocity1);
            }
            if (ex2 != null) {
                ex2.setVelocity(output.velocity2);
            }
            return;
        }
        if (motor1 != null) {
            motor1.setPower(output.power1);
        }
        if (motor2 != null) {
            motor2.setPower(output.power2);
        }
    }

    private static void coast(DcMotor motor) {
        if (motor == null) {
            return;
        }
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setPower(0);
    }

    private static double readTicks(DcMotorEx motor) {
        if (motor == null) {
            return Double.NaN;
        }
        try {
            return motor.getVelocity();
        } catch (RuntimeException ignored) {
            return Double.NaN;
        }
    }

    private static LauncherVelocityMath.MotorTypeSnapshot snapshot(String hardwareName, DcMotor motor) {
        if (!(motor instanceof DcMotorEx)) {
            return new LauncherVelocityMath.MotorTypeSnapshot(
                    hardwareName, "", "", Double.NaN, Double.NaN, Double.NaN, Double.NaN, "unavailable");
        }
        MotorConfigurationType type;
        try {
            type = ((DcMotorEx) motor).getMotorType();
        } catch (RuntimeException ignored) {
            type = null;
        }
        if (type == null) {
            return new LauncherVelocityMath.MotorTypeSnapshot(
                    hardwareName, "", "", Double.NaN, Double.NaN, Double.NaN, Double.NaN, "unavailable");
        }
        String orientation = type.getOrientation() == null ? "unknown" : type.getOrientation().toString();
        String name = type.getName() == null ? "" : type.getName();
        String tag = type.getXmlTag() == null ? "" : type.getXmlTag();
        return new LauncherVelocityMath.MotorTypeSnapshot(
                hardwareName,
                name,
                tag,
                type.getTicksPerRev(),
                type.getMaxRPM(),
                type.getGearing(),
                type.getAchieveableMaxRPMFraction(),
                orientation);
    }
}
