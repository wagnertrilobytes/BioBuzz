// 2026 league meet one Nov 7 autonomous programming.
// Fixed motor response, proportional turning, heading drift, and long-distance hang-up bug.
// Made by 12820 Wagner Trilobytes programming team (Modified/Optimized)
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

        imu.resetYaw();

        frontLeftDrive.setDirection(DcMotor.Direction.REVERSE);
        frontRightDrive.setDirection(DcMotor.Direction.FORWARD);
        backRightDrive.setDirection(DcMotor.Direction.FORWARD);
        backLeftDrive.setDirection(DcMotor.Direction.REVERSE);

        telemetry.addData("Status", "Initialized - Yaw Zeroed");
        telemetry.update();

        waitForStart();

        // Autonomous path sequence
        move(18, 0.5);
        turn(-90);
        move(115, 0.5);
        turn(0);
        move(40, 0.5);
        turn(-90);

        shoot();

        turn(180);
        move(40, 0.5);
        turn(90);
        move(115, 0.5);
        turn(180);
        move(18, 0.5);

        while (opModeIsActive()) {
            // Keep looping while op mode is active
        }
    }

    public void shoot() {
        // Open-loop full power. Leave this until launcher velocity control is validated.
        // Do not enable uncalibrated velocity mode in autonomous.
        outtake1.setPower(1);
        outtake2.setPower(-1);
        midtake.setPower(-1);
        sleep(3000);
        midtake.setPower(0);
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
        frontLeftDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontRightDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeftDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backRightDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        int angleTolerance = 2;
        double kP = 0.022;
        double minPower = 0.18;
        double maxPower = 0.45;

        while (opModeIsActive()) {
            YawPitchRollAngles orientation = imu.getRobotYawPitchRollAngles();
            double yaw = orientation.getYaw(AngleUnit.DEGREES);
            double error = targetAngle - yaw;

            if (Math.abs(error) <= angleTolerance) {
                break;
            }

            telemetry.addData("Turning to", targetAngle);
            telemetry.addData("Current Yaw", yaw);
            telemetry.addData("Error", error);
            telemetry.update();

            double turnSpeed = error * kP;

            if (Math.abs(turnSpeed) < minPower) {
                turnSpeed = Math.signum(turnSpeed) * minPower;
            }
            if (Math.abs(turnSpeed) > maxPower) {
                turnSpeed = Math.signum(turnSpeed) * maxPower;
            }

            frontLeftDrive.setPower(-turnSpeed);
            frontRightDrive.setPower(turnSpeed);
            backLeftDrive.setPower(-turnSpeed);
            backRightDrive.setPower(turnSpeed);
        }

        frontLeftDrive.setPower(0);
        frontRightDrive.setPower(0);
        backLeftDrive.setPower(0);
        backRightDrive.setPower(0);

        sleep(300);
    }

    public void move(int distance, double speed) {
        frontLeftDrive.setPower(0);
        frontRightDrive.setPower(0);
        backLeftDrive.setPower(0);
        backRightDrive.setPower(0);

        // Reset encoders
        frontLeftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontRightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backLeftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backRightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // Use RUN_USING_ENCODER instead of RUN_TO_POSITION to prevent hanging
        frontLeftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backLeftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backRightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        int targetTicks = (int)(distance * 26);

        YawPitchRollAngles startOrientation = imu.getRobotYawPitchRollAngles();
        double targetHeading = startOrientation.getYaw(AngleUnit.DEGREES);

        int currentPosition = 0;

        // Loop runs until encoder tick count matches target distance, preventing infinite stalls
        while (opModeIsActive() && Math.abs(currentPosition) < targetTicks) {
            currentPosition = Math.abs(frontLeftDrive.getCurrentPosition());

            YawPitchRollAngles orientation = imu.getRobotYawPitchRollAngles();
            double yaw = orientation.getYaw(AngleUnit.DEGREES);

            telemetry.addData("Path", "Driving to position");
            telemetry.addData("Target Ticks", targetTicks);
            telemetry.addData("Current Ticks", currentPosition);
            telemetry.addData("Current Yaw", yaw);
            telemetry.update();

            // FIXED: Normalize heading error to handle the 180/-180 degree boundary wrap
            double headingError = targetHeading - yaw;
            while (headingError > 180) headingError -= 360;
            while (headingError < -180) headingError += 360;

            double correction = headingError * 0.03;

            double leftPower = speed - correction;
            double rightPower = speed + correction;

            frontLeftDrive.setPower(leftPower);
            backLeftDrive.setPower(leftPower);
            frontRightDrive.setPower(rightPower);
            backRightDrive.setPower(rightPower);
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
