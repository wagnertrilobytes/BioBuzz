//2026 league meet one Nov 7 autonomous programming.
//pedropathing wasn't recognizing a dead wheel so this was a quick fix.
//Uses built in wheel encoders and the imu in order to navigate the field.
//Made by 12820 Wagner Trilobytes programming team
//Hayden, Arthur, Emarie, Derek, Jacob, Dalton.
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@Autonomous(name="left park auto", group = "Linear OpMode")
public class leftEdgeAuto extends LinearOpMode {
    private IMU imu;
    private DcMotor frontLeftDrive = null;
    private DcMotor backLeftDrive = null;
    private DcMotor backRightDrive = null;
    private DcMotor frontRightDrive = null;
    private DcMotor intake = null;
    private DcMotor outtake1 = null;
    private DcMotor outtake2 = null;
    private DcMotor midtake = null;

    @Override
    public void runOpMode() {
        imu = hardwareMap.get(IMU.class, "imu");
        frontLeftDrive = hardwareMap.get(DcMotor.class, "frontLeftDrive");
        backLeftDrive = hardwareMap.get(DcMotor.class, "backLeftDrive");
        frontRightDrive = hardwareMap.get(DcMotor.class, "frontRightDrive");
        backRightDrive = hardwareMap.get(DcMotor.class, "backRightDrive");
        intake = hardwareMap.get(DcMotor.class, "intake");
        outtake1 = hardwareMap.get(DcMotor.class, "outtake1");
        outtake2 = hardwareMap.get(DcMotor.class, "outtake2");
        midtake = hardwareMap.get(DcMotor.class, "midtake");

        RevHubOrientationOnRobot.LogoFacingDirection logoDirection = RevHubOrientationOnRobot.LogoFacingDirection.UP;
        RevHubOrientationOnRobot.UsbFacingDirection  usbDirection  = RevHubOrientationOnRobot.UsbFacingDirection.LEFT;

        IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(logoDirection, usbDirection));
        imu.initialize(parameters);

        // Resets the IMU heading to 0 degrees right at initialization
        imu.resetYaw();

        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        frontRightDrive.setDirection(DcMotor.Direction.FORWARD);
        backRightDrive.setDirection(DcMotor.Direction.FORWARD);
        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);

        telemetry.addData("Status", "Initialized - Yaw Zeroed");
        telemetry.update();

        waitForStart();

        // Start angle is now 0 natively thanks to imu.resetYaw()
        move(18, 0.25);
        turn(-90);
        move(110, 0.25);
        turn(0);
        move(45, 0.25);
        turn(-90);

        // Shoot does what it says (Hint: it shoots)
        shoot();

        while (opModeIsActive()) {
            // Keep looping while op mode is active
        }
    }

    public void shoot() {
        outtake1.setPower(1);
        outtake2.setPower(-1);
        sleep(10000);
        outtake1.setPower(0);
        outtake2.setPower(0);
    }

    public void pickup() {
        intake.setPower(1);
        midtake.setPower(1);
        sleep(1000);
        intake.setPower(0);
        midtake.setPower(0);
    }

    public void turn(int targetAngle) {
        // Set power to off (backup in case of infinite loop causing motors to continuously spin)
        frontLeftDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontRightDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeftDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backRightDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        frontLeftDrive.setPower(0);
        frontRightDrive.setPower(0);
        backLeftDrive.setPower(0);
        backRightDrive.setPower(0);

        int angleTolerance = 2;
        YawPitchRollAngles orientation = imu.getRobotYawPitchRollAngles();
        double yaw = orientation.getYaw(AngleUnit.DEGREES);
        double error = targetAngle - yaw;

        // Turn until within angle tolerance
        while (!(Math.abs(error) < angleTolerance) && opModeIsActive()){
            orientation = imu.getRobotYawPitchRollAngles();
            yaw = orientation.getYaw(AngleUnit.DEGREES);
            error = targetAngle - yaw;

            telemetry.addData("Turning to", targetAngle);
            telemetry.addData("Current Yaw", yaw);
            telemetry.update();

            double turnSpeed = Math.signum(error) * 0.3;

            frontLeftDrive.setPower(-turnSpeed);
            frontRightDrive.setPower(turnSpeed);
            backLeftDrive.setPower(-turnSpeed);
            backRightDrive.setPower(turnSpeed);
        }

        // Stop motors immediately upon reaching target angle
        frontLeftDrive.setPower(0);
        frontRightDrive.setPower(0);
        backLeftDrive.setPower(0);
        backRightDrive.setPower(0);
    }

    public void move(int distance, double speed) {
        // 12.57 inches per revolution | 384.5 ticks per rotation | ~30.6 ticks per inch
        int angleTolerance = 2;

        // Failsafe to stop unwanted power
        frontLeftDrive.setPower(0);
        frontRightDrive.setPower(0);
        backLeftDrive.setPower(0);
        backRightDrive.setPower(0);

        // Reset encoder values back to 0
        frontLeftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontRightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backLeftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backRightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // Turn encoders back on
        frontLeftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backLeftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backRightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Set target positions
        int targetTicks = (int)(distance * 26);
        frontLeftDrive.setTargetPosition(targetTicks);
        frontRightDrive.setTargetPosition(targetTicks);
        backLeftDrive.setTargetPosition(targetTicks);
        backRightDrive.setTargetPosition(targetTicks);

        // Run to position
        frontLeftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        frontRightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        backLeftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        backRightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        frontLeftDrive.setPower(speed);
        frontRightDrive.setPower(speed);
        backLeftDrive.setPower(speed);
        backRightDrive.setPower(speed);

        // Maintain straight heading relative to initial 0° heading while driving
        while (opModeIsActive() && (frontLeftDrive.isBusy() || backRightDrive.isBusy() || backLeftDrive.isBusy() || frontRightDrive.isBusy())) {
            YawPitchRollAngles orientation = imu.getRobotYawPitchRollAngles();
            double yaw = orientation.getYaw(AngleUnit.DEGREES);

            telemetry.addData("Path", "Driving to position");
            telemetry.addData("Current Yaw", yaw);
            telemetry.update();

            // Simple proportional correction if drifting off 0° heading
            if (yaw < 0){
                frontLeftDrive.setPower(speed);
                frontRightDrive.setPower(speed * 1.3);
                backLeftDrive.setPower(speed);
                backRightDrive.setPower(speed * 1.3);
            } else if (yaw > 0){
                frontLeftDrive.setPower(speed * 1.3);
                frontRightDrive.setPower(speed);
                backLeftDrive.setPower(speed * 1.3);
                backRightDrive.setPower(speed);
            } else {
                frontLeftDrive.setPower(speed);
                frontRightDrive.setPower(speed);
                backLeftDrive.setPower(speed);
                backRightDrive.setPower(speed);
            }
        }

        // Stop motors after travel
        frontLeftDrive.setPower(0);
        frontRightDrive.setPower(0);
        backLeftDrive.setPower(0);
        backRightDrive.setPower(0);

        frontLeftDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontRightDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeftDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backRightDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }
}